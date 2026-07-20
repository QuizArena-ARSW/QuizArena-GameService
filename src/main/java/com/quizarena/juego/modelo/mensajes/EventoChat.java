package com.quizarena.juego.modelo.mensajes;

/** Evento: un mensaje de chat dentro de una sala. */
public record EventoChat(String tipo, String idJugador, String apodo, String texto, long fecha) {
    public EventoChat(String idJugador, String apodo, String texto, long fecha) {
        this("CHAT", idJugador, apodo, texto, fecha);
    }
}
