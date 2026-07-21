package com.quizarena.juego.controlador;

import com.quizarena.juego.cliente.ClienteIdentidad;
import com.quizarena.juego.cliente.RegistroPartidaDTO;
import com.quizarena.juego.eventos.PublicadorEventos;
import com.quizarena.juego.metricas.MetricasJuego;
import com.quizarena.juego.modelo.*;
import com.quizarena.juego.modelo.mensajes.*;
import com.quizarena.juego.servicio.GestorSalas;
import com.quizarena.juego.servicio.MotorJuego;
import com.quizarena.juego.servicio.RepositorioChatRedis;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private static final int TEXTO_CHAT_MAX = 300;

    private final GestorSalas gestorSalas;
    private final MotorJuego motorJuego;
    private final PublicadorEventos publicador;
    private final ClienteIdentidad clienteIdentidad;
    private final MetricasJuego metricas;
    private final RepositorioChatRedis repositorioChat;

    public JuegoWebSocketController(GestorSalas gestorSalas, MotorJuego motorJuego,
                                    PublicadorEventos publicador,
                                    ClienteIdentidad clienteIdentidad,
                                    MetricasJuego metricas,
                                    RepositorioChatRedis repositorioChat) {
        this.gestorSalas = gestorSalas;
        this.motorJuego = motorJuego;
        this.publicador = publicador;
        this.clienteIdentidad = clienteIdentidad;
        this.metricas = metricas;
        this.repositorioChat = repositorioChat;
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
                saneado(codigo), saneado(req.apodo()), sala.getJugadores().size());

        // Su id personal (texto plano) y la lista actualizada para toda la sala
        publicador.publicarTexto("/topic/sala/" + codigo + "/jugador/" + nuevo[0].getApodo(),
                nuevo[0].getId());
        publicador.publicar("/topic/sala/" + codigo, new EventoLista(jugadoresEvento(sala)));

        // Historial de chat existente, solo para quien se acaba de unir (no se
        // difunde a toda la sala: cada quien lo recibe una vez, al conectarse).
        publicador.publicar("/topic/sala/" + codigo + "/jugador/" + nuevo[0].getApodo() + "/chat-historial",
                repositorioChat.historial(codigo));
    }

    // ---- 2. Iniciar la partida ----
    @MessageMapping("/sala/{codigo}/iniciar")
    public void iniciar(@DestinationVariable String codigo) {
        Sala sala = gestorSalas.modificar(codigo, Sala::iniciar);
        if (sala == null) return;
        metricas.partidaIniciada();
        log.info("evento=partida_iniciada sala={} jugadores={}", saneado(codigo), sala.getJugadores().size());
        avanzar(codigo, null);
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
        log.info("evento=respuesta sala={} correcta={} ronda={}", saneado(codigo), acerto[0], sala.getRondaActual());

        publicador.publicar("/topic/sala/" + codigo, new EventoMarcador(ranking(sala)));
    }

    // ---- 4. Siguiente pregunta ----
    // El cliente lo dispara manualmente (boton) o automaticamente cuando se
    // le acaba el tiempo. Como cualquier jugador puede disparar el avance
    // automatico, "ronda" permite ignorar duplicados: si dos jugadores llegan
    // a 0 casi al mismo tiempo, solo el primero realmente avanza la sala.
    @MessageMapping("/sala/{codigo}/siguiente")
    public void siguiente(@DestinationVariable String codigo, @Payload SiguienteRequest req) {
        avanzar(codigo, req != null ? req.ronda() : null);
    }

    // ---- 5. Chat de la sala ----
    // No pasa por gestorSalas.modificar: el chat no es parte del estado de
    // juego (no necesita el bloqueo distribuido), vive en su propia clave de
    // Redis con el mismo ciclo de vida que la sala (ver RepositorioChatRedis).
    @MessageMapping("/sala/{codigo}/chat")
    public void chat(@DestinationVariable String codigo, @Payload MensajeChatRequest req) {
        if (req.texto() == null || req.texto().isBlank()) return;

        Sala sala = gestorSalas.buscarSala(codigo);
        if (sala == null) return;

        Jugador jugador = sala.getJugadores().get(req.idJugador());
        if (jugador == null) return;

        String texto = req.texto().strip();
        if (texto.length() > TEXTO_CHAT_MAX) texto = texto.substring(0, TEXTO_CHAT_MAX);

        EventoChat mensaje = new EventoChat(jugador.getId(), jugador.getApodo(), texto, System.currentTimeMillis());
        repositorioChat.agregar(codigo, mensaje);
        publicador.publicar("/topic/sala/" + codigo + "/chat", mensaje);
    }

    // ===================== Auxiliares =====================

    /** Avanza a la siguiente pregunta, o finaliza la partida si no hay mas. */
    private void avanzar(String codigo, Integer rondaEsperada) {
        final Pregunta[] preguntaRef = new Pregunta[1];
        final boolean[] avanzo = new boolean[] { true };

        Sala sala = gestorSalas.modificar(codigo, s -> {
            if (rondaEsperada != null && s.getRondaActual() != rondaEsperada) {
                avanzo[0] = false;
                return;
            }
            preguntaRef[0] = s.siguientePregunta();
            if (preguntaRef[0] == null) s.finalizar();
        });
        if (sala == null || !avanzo[0]) return;

        Pregunta pregunta = preguntaRef[0];

        if (pregunta == null) {
            // Fin de la partida
            metricas.partidaFinalizada();
            log.info("evento=partida_finalizada sala={} jugadores={}", saneado(codigo), sala.getJugadores().size());
            guardarResultados(sala);
            publicador.publicar("/topic/sala/" + codigo, new EventoFin(ranking(sala), resumenPorJugador(sala)));
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

    /** Arma, para cada jugador, la lista de sus respuestas pregunta por pregunta. */
    private Map<String, List<RespuestaResumenDTO>> resumenPorJugador(Sala sala) {
        Map<String, List<RespuestaResumenDTO>> resultado = new LinkedHashMap<>();
        for (Jugador jugador : sala.getListaJugadores()) {
            List<RespuestaResumenDTO> resumen = sala.getPreguntas().stream()
                    .map(pregunta -> resumenDeUnaPregunta(pregunta, jugador))
                    .toList();
            resultado.put(jugador.getId(), resumen);
        }
        return resultado;
    }

    private RespuestaResumenDTO resumenDeUnaPregunta(Pregunta pregunta, Jugador jugador) {
        String idElegida = jugador.getRespuestas().get(pregunta.getId());
        Opcion correcta = pregunta.getOpciones().stream()
                .filter(Opcion::isEsCorrecta).findFirst().orElse(null);
        Opcion elegida = idElegida == null ? null : pregunta.getOpciones().stream()
                .filter(o -> o.getId().equals(idElegida)).findFirst().orElse(null);

        return new RespuestaResumenDTO(
                pregunta.getId(),
                pregunta.getEnunciado(),
                elegida != null ? elegida.getId() : null,
                elegida != null ? elegida.getTexto() : null,
                elegida != null && elegida.isEsCorrecta(),
                correcta != null ? correcta.getTexto() : null
        );
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

    /**
     * Quita saltos de linea de un valor que viene del cliente (codigo de
     * sala, apodo) antes de escribirlo en el log: sin esto, alguien podria
     * inyectar lineas de log falsas (ej. un apodo con un "\n" seguido de un
     * evento inventado).
     */
    private static String saneado(String valor) {
        return valor == null ? null : valor.replace("\n", "").replace("\r", "");
    }
}
