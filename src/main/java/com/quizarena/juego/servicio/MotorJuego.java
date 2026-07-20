package com.quizarena.juego.servicio;

import com.quizarena.juego.modelo.Jugador;
import com.quizarena.juego.modelo.Pregunta;
import com.quizarena.juego.modelo.Sala;
import org.springframework.stereotype.Service;

/**
 * Motor de juego AUTORITATIVO: la unica fuente de verdad de la partida.
 *
 * Calcula puntajes y controla el avance de rondas. Que esta logica viva
 * solo en el servidor es lo que evita trampas: el cliente nunca decide
 * su propio puntaje.
 */
@Service
public class MotorJuego {

    private static final int PUNTOS_BASE = 1000;

    /**
     * Calcula el puntaje de una respuesta correcta, ponderando la rapidez.
     * Mas rapido = mas puntos. Una respuesta incorrecta vale 0.
     *
     * @param correcta        si la respuesta fue correcta
     * @param tiempoRespuesta tiempo que tardo el jugador (ms)
     * @param tiempoLimiteMs  tiempo limite de la pregunta (ms)
     */
    public int calcularPuntaje(boolean correcta, long tiempoRespuesta, long tiempoLimiteMs) {
        if (!correcta) {
            return 0;
        }
        if (tiempoLimiteMs <= 0) {
            return PUNTOS_BASE;
        }
        // Factor de rapidez: 1.0 si responde al instante, baja hacia 0.5 al agotar el tiempo
        double factorRapidez = 1.0 - (0.5 * Math.min(tiempoRespuesta, tiempoLimiteMs) / (double) tiempoLimiteMs);
        return (int) Math.round(PUNTOS_BASE * factorRapidez);
    }

    /**
     * Procesa la respuesta de un jugador: valida, calcula puntaje y lo suma.
     * Devuelve los puntos ganados en esta ronda.
     */
    public int registrarRespuesta(Sala sala, String idJugador, String idOpcion, long tiempoRespuestaMs) {
        Pregunta pregunta = sala.getPreguntaActual();
        Jugador jugador = sala.getJugadores().get(idJugador);
        if (pregunta == null || jugador == null) {
            return 0;
        }
        boolean correcta = pregunta.esCorrecta(idOpcion);
        long tiempoLimiteMs = pregunta.getTiempoLimiteSegundos() * 1000L;
        int puntos = calcularPuntaje(correcta, tiempoRespuestaMs, tiempoLimiteMs);
        jugador.sumarPuntaje(puntos);
        jugador.registrarRespuesta(pregunta.getId(), idOpcion);
        return puntos;
    }
}
