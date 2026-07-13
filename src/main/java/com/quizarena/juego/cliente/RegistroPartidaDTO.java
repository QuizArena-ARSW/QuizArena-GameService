package com.quizarena.juego.cliente;

/**
 * Datos que el Servicio de Juego envia a Identidad para guardar el resultado
 * de un jugador al terminar la partida (POST /api/historial).
 */
public record RegistroPartidaDTO(
        String idUsuario,
        String idBanco,
        String materia,
        int puntajeObtenido,
        int posicionFinal
) {}
