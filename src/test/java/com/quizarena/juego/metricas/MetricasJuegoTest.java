package com.quizarena.juego.metricas;

import com.quizarena.juego.servicio.RepositorioSalasRedis;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Las metricas son la evidencia que respalda los escenarios de calidad
 * (latencia, concurrencia) ante el jurado -- si no se registran bien, esa
 * evidencia no existe. Se usa un registro real en memoria (SimpleMeterRegistry)
 * en vez de mockear Micrometer, para verificar que los contadores de verdad
 * suben.
 */
@ExtendWith(MockitoExtension.class)
class MetricasJuegoTest {

    @Mock RepositorioSalasRedis repositorio;
    SimpleMeterRegistry registry;
    MetricasJuego metricas;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metricas = new MetricasJuego(registry, repositorio);
        metricas.registrar();
    }

    @Test
    void jugadorEntroYSalioActualizanElContadorDeConectados() {
        metricas.jugadorEntro();
        metricas.jugadorEntro();
        metricas.jugadorSalio();

        assertThat(registry.get("quizarena.jugadores.conectados").gauge().value()).isEqualTo(1.0);
    }

    @Test
    void partidaIniciadaYFinalizadaIncrementanSusContadores() {
        metricas.partidaIniciada();
        metricas.partidaIniciada();
        metricas.partidaFinalizada();

        assertThat(registry.get("quizarena.partidas.iniciadas").counter().count()).isEqualTo(2.0);
        assertThat(registry.get("quizarena.partidas.finalizadas").counter().count()).isEqualTo(1.0);
    }

    @Test
    void respuestaProcesadaCuentaTotalesYCorrectasPorSeparado() {
        metricas.respuestaProcesada(true);
        metricas.respuestaProcesada(false);
        metricas.respuestaProcesada(true);

        assertThat(registry.get("quizarena.respuestas.total").counter().count()).isEqualTo(3.0);
        assertThat(registry.get("quizarena.respuestas.correctas").counter().count()).isEqualTo(2.0);
    }

    @Test
    void iniciarYTerminarMedicionRegistranLaLatencia() {
        Timer.Sample muestra = metricas.iniciarMedicion();
        metricas.terminarMedicion(muestra);

        assertThat(registry.get("quizarena.respuesta.latencia").timer().count()).isEqualTo(1);
    }

    @Test
    void elGaugeDeSalasActivasLeeElRepositorio() {
        when(repositorio.contarActivas()).thenReturn(7L);

        double valor = registry.get("quizarena.salas.activas").gauge().value();

        assertThat(valor).isEqualTo(7.0);
    }
}
