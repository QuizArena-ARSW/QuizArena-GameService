package com.quizarena.juego.modelo.mensajes;

/** Representacion ligera de un jugador para enviar al cliente. */
public record EventoJugador(String id, String apodo, int puntaje, boolean conectado) {}
