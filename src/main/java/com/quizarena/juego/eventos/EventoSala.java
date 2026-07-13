package com.quizarena.juego.eventos;

/**
 * Sobre que viaja por Redis entre instancias.
 *
 * @param destino   destino STOMP al que hay que entregar (ej. /topic/sala/ABC123)
 * @param cuerpo    contenido (JSON serializado, o texto plano)
 * @param esJson    true si el cuerpo es un objeto JSON; false si es texto plano
 */
public record EventoSala(String destino, String cuerpo, boolean esJson) {}
