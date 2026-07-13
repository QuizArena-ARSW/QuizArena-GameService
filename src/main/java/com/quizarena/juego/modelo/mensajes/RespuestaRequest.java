package com.quizarena.juego.modelo.mensajes;

/** Mensaje que envia un jugador al responder una pregunta. */
public record RespuestaRequest(
        String idJugador,
        String idOpcion,
        long tiempoRespuestaMs
) {}
