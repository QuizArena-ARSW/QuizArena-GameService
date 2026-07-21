package com.quizarena.juego.controlador;

import com.quizarena.juego.cliente.ClienteIdentidad;
import com.quizarena.juego.eventos.PublicadorEventos;
import com.quizarena.juego.metricas.MetricasJuego;
import com.quizarena.juego.modelo.EstadoSala;
import com.quizarena.juego.modelo.Jugador;
import com.quizarena.juego.modelo.Opcion;
import com.quizarena.juego.modelo.Pregunta;
import com.quizarena.juego.modelo.Sala;
import com.quizarena.juego.modelo.mensajes.EventoChat;
import com.quizarena.juego.modelo.mensajes.EventoFin;
import com.quizarena.juego.modelo.mensajes.EventoLista;
import com.quizarena.juego.modelo.mensajes.EventoMarcador;
import com.quizarena.juego.modelo.mensajes.EventoPregunta;
import com.quizarena.juego.modelo.mensajes.MensajeChatRequest;
import com.quizarena.juego.modelo.mensajes.RespuestaRequest;
import com.quizarena.juego.modelo.mensajes.SiguienteRequest;
import com.quizarena.juego.modelo.mensajes.UnirseRequest;
import com.quizarena.juego.servicio.GestorSalas;
import com.quizarena.juego.servicio.MotorJuego;
import com.quizarena.juego.servicio.RepositorioChatRedis;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * El controlador de tiempo real es el corazon del sistema: aqui se decide
 * que se difunde a los jugadores en cada evento (unirse, iniciar, responder,
 * avanzar de ronda, chat). Se prueba con TODAS sus dependencias externas
 * mockeadas (Redis, metricas, cliente HTTP a Identidad) pero usando objetos
 * REALES de Sala/Jugador/Pregunta y un MotorJuego real, para verificar la
 * logica de negocio de punta a punta sin necesitar Redis ni un servidor
 * levantado.
 */
@ExtendWith(MockitoExtension.class)
class JuegoWebSocketControllerTest {

    private static final String CODIGO = "ABC123";

    @Mock GestorSalas gestorSalas;
    @Mock PublicadorEventos publicador;
    @Mock ClienteIdentidad clienteIdentidad;
    @Mock MetricasJuego metricas;
    @Mock RepositorioChatRedis repositorioChat;

    JuegoWebSocketController controller;

    @BeforeEach
    void setUp() {
        controller = new JuegoWebSocketController(
                gestorSalas, new MotorJuego(), publicador, clienteIdentidad, metricas, repositorioChat);
        // gestorSalas.modificar aplica el cambio sobre el objeto Sala real y lo
        // "guarda" devolviendolo — simula fielmente leer/mutar/guardar en Redis.
        lenient().when(gestorSalas.modificar(eq(CODIGO), any())).thenAnswer(inv -> {
            Sala sala = salaActual;
            if (sala == null) return null;
            Consumer<Sala> cambio = inv.getArgument(1);
            cambio.accept(sala);
            return sala;
        });
    }

    private Sala salaActual;

    private Sala salaConDosPreguntas() {
        Opcion correctaP1 = new Opcion("Madrid", true);
        Opcion incorrectaP1 = new Opcion("Paris", false);
        Pregunta p1 = new Pregunta("Capital de España", List.of(correctaP1, incorrectaP1), 20);
        Pregunta p2 = new Pregunta("2 + 2", List.of(new Opcion("4", true), new Opcion("5", false)), 20);
        salaActual = new Sala(CODIGO, List.of(p1, p2), 10, "banco-1", "Cultura General", UUID.randomUUID());
        return salaActual;
    }

    // ---- unirse ----

    @Test
    void unirseAgregaAlJugadorYDifundeLosEventosCorrectos() {
        Sala sala = salaConDosPreguntas();
        when(repositorioChat.historial(CODIGO)).thenReturn(List.of());

        controller.unirse(CODIGO, new UnirseRequest("Juan", null));

        assertThat(sala.getJugadores()).hasSize(1);
        verify(metricas).jugadorEntro();
        verify(publicador).publicarTexto(eq("/topic/sala/" + CODIGO + "/jugador/Juan"), anyString());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publicador, times(2)).publicar(anyString(), captor.capture());
        EventoLista lista = (EventoLista) captor.getAllValues().stream()
                .filter(EventoLista.class::isInstance).findFirst().orElseThrow();
        assertThat(lista.jugadores()).hasSize(1);
    }

    @Test
    void unirseNoHaceNadaSiLaSalaNoExiste() {
        salaActual = null;

        controller.unirse(CODIGO, new UnirseRequest("Juan", null));

        verifyNoInteractions(publicador, metricas);
    }

    // ---- iniciar ----

    @Test
    void iniciarMarcaLaSalaEnCursoYPublicaLaPrimeraPregunta() {
        Sala sala = salaConDosPreguntas();

        controller.iniciar(CODIGO);

        assertThat(sala.getEstado()).isEqualTo(EstadoSala.EN_CURSO);
        verify(metricas).partidaIniciada();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publicador).publicar(eq("/topic/sala/" + CODIGO), captor.capture());
        EventoPregunta evento = (EventoPregunta) captor.getValue();
        assertThat(evento.ronda()).isEqualTo(1);
        assertThat(evento.totalRondas()).isEqualTo(2);
        assertThat(evento.enunciado()).isEqualTo("Capital de España");
    }

    // ---- responder ----

    @Test
    void responderCalculaPuntajeYPublicaElMarcadorActualizado() {
        Sala sala = salaConDosPreguntas();
        sala.iniciar();
        sala.siguientePregunta(); // posiciona la pregunta 1 como actual
        Jugador jugador = sala.agregarJugador("Juan", null);
        String idOpcionCorrecta = sala.getPreguntaActual().getOpciones().stream()
                .filter(Opcion::isEsCorrecta).findFirst().orElseThrow().getId();
        when(metricas.iniciarMedicion()).thenReturn(mock(Timer.Sample.class));

        controller.responder(CODIGO, new RespuestaRequest(jugador.getId(), idOpcionCorrecta, 0));

        assertThat(jugador.getPuntajeTotal()).isEqualTo(1000); // respuesta correcta e instantanea
        verify(metricas).respuestaProcesada(true);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publicador).publicar(eq("/topic/sala/" + CODIGO), captor.capture());
        EventoMarcador marcador = (EventoMarcador) captor.getValue();
        assertThat(marcador.ranking()).hasSize(1);
        assertThat(marcador.ranking().get(0).puntaje()).isEqualTo(1000);
    }

    // ---- siguiente ----

    @Test
    void siguienteAvanzaALaSiguientePreguntaCuandoLaRondaCoincide() {
        Sala sala = salaConDosPreguntas();
        sala.iniciar();
        sala.siguientePregunta(); // ronda 1 actual

        controller.siguiente(CODIGO, new SiguienteRequest(1));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publicador).publicar(eq("/topic/sala/" + CODIGO), captor.capture());
        EventoPregunta evento = (EventoPregunta) captor.getValue();
        assertThat(evento.ronda()).isEqualTo(2);
        assertThat(evento.enunciado()).isEqualTo("2 + 2");
    }

    @Test
    void siguienteIgnoraUnaPeticionParaUnaRondaQueYaPaso() {
        Sala sala = salaConDosPreguntas();
        sala.iniciar();
        sala.siguientePregunta();
        sala.siguientePregunta(); // ya esta en ronda 2

        controller.siguiente(CODIGO, new SiguienteRequest(1)); // pide avanzar la ronda 1 (vieja)

        verifyNoInteractions(publicador);
    }

    @Test
    void siguienteEnLaUltimaPreguntaFinalizaLaPartidaYGuardaElHistorial() {
        Opcion correcta = new Opcion("4", true);
        Pregunta unicaPregunta = new Pregunta("2 + 2", List.of(correcta, new Opcion("5", false)), 20);
        salaActual = new Sala(CODIGO, List.of(unicaPregunta), 10, "banco-1", "Matemáticas", UUID.randomUUID());
        Sala sala = salaActual;
        sala.iniciar();
        sala.siguientePregunta();
        Jugador jugador = sala.agregarJugador("Juan", "id-usuario-real");
        jugador.registrarRespuesta(unicaPregunta.getId(), correcta.getId());
        jugador.sumarPuntaje(1000);

        controller.siguiente(CODIGO, new SiguienteRequest(1));

        assertThat(sala.getEstado()).isEqualTo(EstadoSala.FINALIZADA);
        verify(clienteIdentidad).guardarResultado(any());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publicador).publicar(eq("/topic/sala/" + CODIGO), captor.capture());
        EventoFin fin = (EventoFin) captor.getValue();
        assertThat(fin.rankingFinal()).hasSize(1);
        assertThat(fin.resumenPorJugador()).containsKey(jugador.getId());
        assertThat(fin.resumenPorJugador().get(jugador.getId())).hasSize(1);
        assertThat(fin.resumenPorJugador().get(jugador.getId()).get(0).correcta()).isTrue();
    }

    // ---- chat ----

    @Test
    void chatIgnoraMensajesVaciosOEnBlanco() {
        controller.chat(CODIGO, new MensajeChatRequest("cualquiera", "   "));

        verifyNoInteractions(repositorioChat, publicador);
        verify(gestorSalas, never()).buscarSala(any());
    }

    @Test
    void chatIgnoraAUnJugadorDesconocido() {
        Sala sala = salaConDosPreguntas();
        when(gestorSalas.buscarSala(CODIGO)).thenReturn(sala);

        controller.chat(CODIGO, new MensajeChatRequest("id-que-no-existe", "hola"));

        verifyNoInteractions(repositorioChat, publicador);
    }

    @Test
    void chatGuardaYDifundeElMensajeYLoTruncaSiEsMuyLargo() {
        Sala sala = salaConDosPreguntas();
        Jugador jugador = sala.agregarJugador("Juan", null);
        when(gestorSalas.buscarSala(CODIGO)).thenReturn(sala);
        String textoLargo = "a".repeat(400);

        controller.chat(CODIGO, new MensajeChatRequest(jugador.getId(), textoLargo));

        ArgumentCaptor<EventoChat> captorMensaje = ArgumentCaptor.forClass(EventoChat.class);
        verify(repositorioChat).agregar(eq(CODIGO), captorMensaje.capture());
        assertThat(captorMensaje.getValue().texto()).hasSize(300);
        verify(publicador).publicar(eq("/topic/sala/" + CODIGO + "/chat"), any(EventoChat.class));
    }
}
