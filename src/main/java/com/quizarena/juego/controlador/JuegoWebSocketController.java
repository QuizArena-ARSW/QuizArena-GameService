package com.quizarena.juego.controlador;

import com.quizarena.juego.cliente.ClienteIdentidad;
import com.quizarena.juego.cliente.RegistroPartidaDTO;
import com.quizarena.juego.eventos.PublicadorEventos;
import com.quizarena.juego.metricas.MetricasJuego;
import com.quizarena.juego.modelo.*;
import com.quizarena.juego.modelo.mensajes.*;
import com.quizarena.juego.servicio.GestorSalas;
import com.quizarena.juego.servicio.MotorJuego;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Comparator;
import java.util.List;

/**
 * Controlador del TIEMPO REAL (Fase 5).
 *
 * Cambios respecto a la Fase 2:
 *  - El estado de la sala se lee/escribe en REDIS, no en memoria local.
 *  - Las modificaciones pasan por gestorSalas.modificar(...), que aplica un
 *    BLOQUEO DISTRIBUIDO (evita condiciones de carrera entre instancias).
 *  - Los eventos se difunden por REDIS PUB/SUB, para que lleguen a los
 *    jugadores conectados a CUALQUIER instancia.
 *  - Se registran METRICAS de latencia, concurrencia y KPIs.
 */
@Controller
public class JuegoWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(JuegoWebSocketController.class);

    private final GestorSalas gestorSalas;
    private final MotorJuego motorJuego;
    private final PublicadorEventos publicador;
    private final ClienteIdentidad clienteIdentidad;
    private final MetricasJuego metricas;

    public JuegoWebSocketController(GestorSalas gestorSalas, MotorJuego motorJuego,
                                    PublicadorEventos publicador,
                                    ClienteIdentidad clienteIdentidad,
                                    MetricasJuego metricas) {
        this.gestorSalas = gestorSalas;
        this.motorJuego = motorJuego;
        this.publicador = publicador;
        this.clienteIdentidad = clienteIdentidad;
        this.metricas = metricas;
    }

    // ---- 1. Un jugador se une ----
    @MessageMapping("/sala/{codigo}/unirse")
    public void unirse(@DestinationVariable String codigo, @Payload UnirseRequest req) {
        final Jugador[] nuevo = new Jugador[1];

        Sala sala = gestorSalas.modificar(codigo, s -> {
            nuevo[0] = s.agregarJugador(req.apodo(), req.idUsuario());
        });
        if (sala == null || nuevo[0] == null) return;

        metricas.jugadorEntro();
        log.info("evento=jugador_unido sala={} apodo={} jugadores={}",
                codigo, req.apodo(), sala.getJugadores().size());

        // Su id personal (texto plano) y la lista actualizada para toda la sala
        publicador.publicarTexto("/topic/sala/" + codigo + "/jugador/" + nuevo[0].getApodo(),
                nuevo[0].getId());
        publicador.publicar("/topic/sala/" + codigo, new EventoLista(jugadoresEvento(sala)));
    }

    // ---- 2. Iniciar la partida ----
    @MessageMapping("/sala/{codigo}/iniciar")
    public void iniciar(@DestinationVariable String codigo) {
        Sala sala = gestorSalas.modificar(codigo, Sala::iniciar);
        if (sala == null) return;
        metricas.partidaIniciada();
        log.info("evento=partida_iniciada sala={} jugadores={}", codigo, sala.getJugadores().size());
        avanzar(codigo);
    }

    // ---- 3. Responder (aqui se mide la latencia) ----
    @MessageMapping("/sala/{codigo}/responder")
    public void responder(@DestinationVariable String codigo, @Payload RespuestaRequest req) {
        Timer.Sample muestra = metricas.iniciarMedicion();
        final boolean[] acerto = new boolean[1];

        // El bloqueo distribuido garantiza que dos instancias no procesen
        // respuestas de la misma sala al mismo tiempo (HU-09).
        Sala sala = gestorSalas.modificar(codigo, s -> {
            Pregunta p = s.getPreguntaActual();
            acerto[0] = p != null && p.esCorrecta(req.idOpcion());
            motorJuego.registrarRespuesta(s, req.idJugador(), req.idOpcion(), req.tiempoRespuestaMs());
        });

        metricas.terminarMedicion(muestra);
        if (sala == null) return;

        metricas.respuestaProcesada(acerto[0]);
        log.info("evento=respuesta sala={} correcta={} ronda={}", codigo, acerto[0], sala.getRondaActual());

        publicador.publicar("/topic/sala/" + codigo, new EventoMarcador(ranking(sala)));
    }

    // ---- 4. Siguiente pregunta ----
    @MessageMapping("/sala/{codigo}/siguiente")
    public void siguiente(@DestinationVariable String codigo) {
        avanzar(codigo);
    }

    // ===================== Auxiliares =====================

    /** Avanza a la siguiente pregunta, o finaliza la partida si no hay mas. */
    private void avanzar(String codigo) {
        final Pregunta[] preguntaRef = new Pregunta[1];

        Sala sala = gestorSalas.modificar(codigo, s -> {
            preguntaRef[0] = s.siguientePregunta();
            if (preguntaRef[0] == null) s.finalizar();
        });
        if (sala == null) return;

        Pregunta pregunta = preguntaRef[0];

        if (pregunta == null) {
            // Fin de la partida
            metricas.partidaFinalizada();
            log.info("evento=partida_finalizada sala={} jugadores={}", codigo, sala.getJugadores().size());
            guardarResultados(sala);
            publicador.publicar("/topic/sala/" + codigo, new EventoFin(ranking(sala)));
            return;
        }

        List<EventoOpcion> opciones = pregunta.getOpciones().stream()
                .map(o -> new EventoOpcion(o.getId(), o.getTexto()))
                .toList();

        publicador.publicar("/topic/sala/" + codigo, new EventoPregunta(
                sala.getRondaActual(), sala.getTotalRondas(),
                pregunta.getId(), pregunta.getEnunciado(),
                opciones, pregunta.getTiempoLimiteSegundos()
        ));
    }

    /** Guarda el resultado de cada jugador con cuenta en el historial. */
    private void guardarResultados(Sala sala) {
        List<Jugador> clasificados = sala.getListaJugadores().stream()
                .sorted(Comparator.comparingInt(Jugador::getPuntajeTotal).reversed())
                .toList();

        for (int i = 0; i < clasificados.size(); i++) {
            Jugador j = clasificados.get(i);
            if (j.getIdUsuario() == null || j.getIdUsuario().isBlank()) continue;
            try {
                clienteIdentidad.guardarResultado(new RegistroPartidaDTO(
                        j.getIdUsuario(), sala.getIdBanco(), sala.getMateria(),
                        j.getPuntajeTotal(), i + 1));
            } catch (Exception e) {
                log.warn("evento=error_historial jugador={} motivo={}", j.getApodo(), e.getMessage());
            }
        }
    }

    private List<EventoJugador> jugadoresEvento(Sala sala) {
        return sala.getListaJugadores().stream()
                .map(j -> new EventoJugador(j.getId(), j.getApodo(), j.getPuntajeTotal(), j.isConectado()))
                .toList();
    }

    private List<EventoJugador> ranking(Sala sala) {
        return sala.getListaJugadores().stream()
                .sorted(Comparator.comparingInt(Jugador::getPuntajeTotal).reversed())
                .map(j -> new EventoJugador(j.getId(), j.getApodo(), j.getPuntajeTotal(), j.isConectado()))
                .toList();
    }
}
