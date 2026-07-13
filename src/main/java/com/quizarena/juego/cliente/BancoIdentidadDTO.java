package com.quizarena.juego.cliente;

import java.util.List;

/**
 * Representa la respuesta de Identidad al pedir un banco (GET /api/bancos/{id}).
 * Solo mapeamos los campos que el Servicio de Juego necesita.
 * Los records permiten que Jackson (JSON) los llene automaticamente.
 */
public record BancoIdentidadDTO(
        String id,
        String nombre,
        String materia,
        List<PreguntaIdentidadDTO> preguntas
) {
    public record PreguntaIdentidadDTO(
            String id,
            String enunciado,
            String tipo,
            int tiempoLimiteSegundos,
            List<OpcionIdentidadDTO> opciones
    ) {}

    public record OpcionIdentidadDTO(
            String id,
            String texto,
            boolean esCorrecta
    ) {}
}
