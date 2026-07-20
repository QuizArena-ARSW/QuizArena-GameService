package com.quizarena.juego.modelo.mensajes;

/**
 * Payload opcional de /siguiente. Si viene "ronda", el servidor solo avanza
 * si la sala sigue en esa ronda: evita saltarse preguntas cuando varios
 * jugadores disparan el avance automatico (fin del tiempo) casi al mismo tiempo.
 */
public record SiguienteRequest(Integer ronda) {}
