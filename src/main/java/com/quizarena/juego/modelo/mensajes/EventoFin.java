package com.quizarena.juego.modelo.mensajes;

import java.util.List;

/** Evento: la partida termino, con el ranking final. */
public record EventoFin(String tipo, List<EventoJugador> rankingFinal) {
    public EventoFin(List<EventoJugador> rankingFinal) {
        this("FIN", rankingFinal);
    }
}
