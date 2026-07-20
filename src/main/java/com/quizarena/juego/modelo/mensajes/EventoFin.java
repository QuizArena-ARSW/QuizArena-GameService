package com.quizarena.juego.modelo.mensajes;

import java.util.List;
import java.util.Map;

/**
 * Evento: la partida termino, con el ranking final y el resumen pregunta por
 * pregunta de cada jugador (idJugador -> lista de respuestas). Cada cliente
 * solo lee su propia entrada del mapa para mostrar su repaso personal.
 */
public record EventoFin(String tipo, List<EventoJugador> rankingFinal,
                        Map<String, List<RespuestaResumenDTO>> resumenPorJugador) {
    public EventoFin(List<EventoJugador> rankingFinal, Map<String, List<RespuestaResumenDTO>> resumenPorJugador) {
        this("FIN", rankingFinal, resumenPorJugador);
    }
}
