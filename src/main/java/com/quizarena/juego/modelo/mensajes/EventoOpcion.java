package com.quizarena.juego.modelo.mensajes;

/** Opcion enviada al cliente: NO incluye si es correcta (eso es secreto). */
public record EventoOpcion(String id, String texto) {}
