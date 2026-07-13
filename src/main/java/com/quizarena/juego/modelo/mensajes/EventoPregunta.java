package com.quizarena.juego.modelo.mensajes;

import java.util.List;

/** Evento: una pregunta nueva. Se difunde a todos al mismo tiempo. */
public record EventoPregunta(
        String tipo,
        int ronda,
        int totalRondas,
        String idPregunta,
        String enunciado,
        List<EventoOpcion> opciones,
        int tiempoLimiteSegundos
) {
    public EventoPregunta(int ronda, int totalRondas, String idPregunta,
                          String enunciado, List<EventoOpcion> opciones, int tiempoLimite) {
        this("PREGUNTA", ronda, totalRondas, idPregunta, enunciado, opciones, tiempoLimite);
    }
}
