package com.quizarena.juego.servicio;

import com.quizarena.juego.modelo.Jugador;
import com.quizarena.juego.modelo.Opcion;
import com.quizarena.juego.modelo.Pregunta;
import com.quizarena.juego.modelo.Sala;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MotorJuego es la unica fuente de verdad del puntaje: el cliente nunca
 * decide su propio puntaje. Estas pruebas verifican el algoritmo de
 * ponderacion acierto+rapidez (HU-09) sin necesidad de Redis ni de un
 * servidor levantado.
 */
class MotorJuegoTest {

    private MotorJuego motor;

    @BeforeEach
    void setUp() {
        motor = new MotorJuego();
    }

    @Test
    void respuestaIncorrectaValeCero() {
        assertThat(motor.calcularPuntaje(false, 0, 20_000)).isZero();
        assertThat(motor.calcularPuntaje(false, 19_999, 20_000)).isZero();
    }

    @Test
    void respuestaCorrectaInstantaneaDaElMaximoDePuntos() {
        int puntos = motor.calcularPuntaje(true, 0, 20_000);
        assertThat(puntos).isEqualTo(1000);
    }

    @Test
    void respuestaCorrectaJustoAlAgotarElTiempoDaLaMitad() {
        int puntos = motor.calcularPuntaje(true, 20_000, 20_000);
        assertThat(puntos).isEqualTo(500);
    }

    @Test
    void respuestaCorrectaAMitadDeTiempoDaPuntajeIntermedio() {
        int puntos = motor.calcularPuntaje(true, 10_000, 20_000);
        assertThat(puntos).isEqualTo(750);
    }

    @Test
    void tiempoDeRespuestaQueExcedeElLimiteSeAcotaAlLimite() {
        // Un retraso de red no debe dar MENOS puntos que responder justo al limite.
        int enElLimite = motor.calcularPuntaje(true, 20_000, 20_000);
        int pasadoDelLimite = motor.calcularPuntaje(true, 45_000, 20_000);
        assertThat(pasadoDelLimite).isEqualTo(enElLimite);
    }

    @Test
    void sinTiempoLimiteConfiguradoDaElPuntajeBaseCompleto() {
        assertThat(motor.calcularPuntaje(true, 5_000, 0)).isEqualTo(1000);
    }

    @Test
    void registrarRespuestaCorrectaSumaPuntajeYQuedaEnElHistorialDelJugador() {
        Opcion correcta = new Opcion("Madrid", true);
        Opcion incorrecta = new Opcion("Paris", false);
        Pregunta pregunta = new Pregunta("Capital de España", List.of(correcta, incorrecta), 20);
        Sala sala = salaConPreguntaActual(pregunta);
        Jugador jugador = new Jugador("Juan", null);
        sala.getJugadores().put(jugador.getId(), jugador);

        int puntos = motor.registrarRespuesta(sala, jugador.getId(), correcta.getId(), 0);

        assertThat(puntos).isEqualTo(1000);
        assertThat(jugador.getPuntajeTotal()).isEqualTo(1000);
        assertThat(jugador.getRespuestas()).containsEntry(pregunta.getId(), correcta.getId());
    }

    @Test
    void registrarRespuestaIncorrectaNoSumaPuntajePeroQuedaRegistrada() {
        Opcion correcta = new Opcion("Madrid", true);
        Opcion incorrecta = new Opcion("Paris", false);
        Pregunta pregunta = new Pregunta("Capital de España", List.of(correcta, incorrecta), 20);
        Sala sala = salaConPreguntaActual(pregunta);
        Jugador jugador = new Jugador("Juan", null);
        sala.getJugadores().put(jugador.getId(), jugador);

        int puntos = motor.registrarRespuesta(sala, jugador.getId(), incorrecta.getId(), 3_000);

        assertThat(puntos).isZero();
        assertThat(jugador.getPuntajeTotal()).isZero();
        assertThat(jugador.getRespuestas()).containsEntry(pregunta.getId(), incorrecta.getId());
    }

    @Test
    void jugadorDesconocidoNoRompeElMotorYDevuelveCero() {
        Opcion correcta = new Opcion("Madrid", true);
        Pregunta pregunta = new Pregunta("Capital de España", List.of(correcta), 20);
        Sala sala = salaConPreguntaActual(pregunta);

        int puntos = motor.registrarRespuesta(sala, "id-que-no-existe", correcta.getId(), 0);

        assertThat(puntos).isZero();
    }

    @Test
    void salaSinPreguntaActualDevuelveCero() {
        Sala sala = new Sala("ABC123", List.of(), 10, null, "Demo", UUID.randomUUID());
        Jugador jugador = new Jugador("Juan", null);
        sala.getJugadores().put(jugador.getId(), jugador);

        int puntos = motor.registrarRespuesta(sala, jugador.getId(), "cualquiera", 0);

        assertThat(puntos).isZero();
    }

    private Sala salaConPreguntaActual(Pregunta pregunta) {
        Sala sala = new Sala("ABC123", List.of(pregunta), 10, null, "Demo", UUID.randomUUID());
        sala.siguientePregunta();
        return sala;
    }
}
