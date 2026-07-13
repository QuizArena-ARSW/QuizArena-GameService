package com.quizarena.juego.modelo.mensajes;

/**
 * Mensaje que envia un jugador para unirse a una sala.
 * idUsuario es el id real de su cuenta en Identidad (necesario para el historial).
 */
public record UnirseRequest(String apodo, String idUsuario) {}
