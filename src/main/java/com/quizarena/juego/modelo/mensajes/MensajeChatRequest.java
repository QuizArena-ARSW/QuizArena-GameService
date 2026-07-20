package com.quizarena.juego.modelo.mensajes;

/** Mensaje que envia un jugador al escribir en el chat de la sala. */
public record MensajeChatRequest(String idJugador, String texto) {}
