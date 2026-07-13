package com.quizarena.juego.metricas;

import com.quizarena.juego.servicio.RepositorioSalasRedis;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Metricas de QuizArena (HU-14, HU-15).
 *
 * Expuestas en /actuator/prometheus para que Prometheus las recolecte y
 * Grafana las muestre en un dashboard. Son la EVIDENCIA que respalda los
 * escenarios de calidad ante el jurado: sin medirlos, la latencia y la
 * concurrencia son solo una promesa.
 *
 * Metricas TECNICAS:
 *   quizarena.respuesta.latencia    tiempo que tarda el servidor en procesar una respuesta
 *   quizarena.jugadores.conectados  jugadores activos en este momento
 *   quizarena.salas.activas         salas vivas (leidas de Redis)
 *
 * Metricas de NEGOCIO (KPIs):
 *   quizarena.partidas.iniciadas    cuantas partidas se han empezado
 *   quizarena.partidas.finalizadas  cuantas se completaron
 *   quizarena.respuestas.total      respuestas procesadas
 *   quizarena.respuestas.correctas  respuestas acertadas
 */
@Component
public class MetricasJuego {

    private final MeterRegistry registry;
    private final RepositorioSalasRedis repositorio;

    private final AtomicInteger jugadoresConectados = new AtomicInteger(0);

    private Counter partidasIniciadas;
    private Counter partidasFinalizadas;
    private Counter respuestasTotal;
    private Counter respuestasCorrectas;
    private Timer latenciaRespuesta;

    public MetricasJuego(MeterRegistry registry, RepositorioSalasRedis repositorio) {
        this.registry = registry;
        this.repositorio = repositorio;
    }

    @PostConstruct
    public void registrar() {
        partidasIniciadas = Counter.builder("quizarena.partidas.iniciadas")
                .description("Partidas iniciadas").register(registry);
        partidasFinalizadas = Counter.builder("quizarena.partidas.finalizadas")
                .description("Partidas completadas").register(registry);
        respuestasTotal = Counter.builder("quizarena.respuestas.total")
                .description("Respuestas procesadas").register(registry);
        respuestasCorrectas = Counter.builder("quizarena.respuestas.correctas")
                .description("Respuestas acertadas").register(registry);

        latenciaRespuesta = Timer.builder("quizarena.respuesta.latencia")
                .description("Tiempo de procesamiento de una respuesta")
                .publishPercentiles(0.5, 0.95, 0.99)   // p50, p95, p99
                .register(registry);

        // Jugadores conectados ahora mismo
        Gauge.builder("quizarena.jugadores.conectados", jugadoresConectados, AtomicInteger::get)
                .description("Jugadores conectados en este momento")
                .register(registry);

        // Salas activas (se lee de Redis: refleja TODAS las instancias)
        Gauge.builder("quizarena.salas.activas", repositorio, RepositorioSalasRedis::contarActivas)
                .description("Salas activas en el sistema")
                .register(registry);
    }

    public void jugadorEntro() { jugadoresConectados.incrementAndGet(); }
    public void jugadorSalio() { jugadoresConectados.decrementAndGet(); }
    public void partidaIniciada() { partidasIniciadas.increment(); }
    public void partidaFinalizada() { partidasFinalizadas.increment(); }

    public void respuestaProcesada(boolean correcta) {
        respuestasTotal.increment();
        if (correcta) respuestasCorrectas.increment();
    }

    /** Mide cuanto tarda el servidor en procesar una respuesta. */
    public Timer.Sample iniciarMedicion() {
        return Timer.start(registry);
    }

    public void terminarMedicion(Timer.Sample muestra) {
        muestra.stop(latenciaRespuesta);
    }
}
