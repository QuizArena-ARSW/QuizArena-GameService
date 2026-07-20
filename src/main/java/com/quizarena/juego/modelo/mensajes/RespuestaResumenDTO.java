package com.quizarena.juego.modelo.mensajes;

/**
 * Una fila del resumen final: una pregunta, lo que el jugador respondio (o
 * null si no alcanzo a responder) y cual era la opcion correcta.
 */
public record RespuestaResumenDTO(
        String idPregunta,
        String enunciado,
        String idOpcionElegida,
        String textoElegida,
        boolean correcta,
        String textoCorrecta
) {}
