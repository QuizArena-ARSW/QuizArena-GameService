package com.quizarena.juego.modelo.mensajes;

import java.util.List;

/** Evento: la lista de jugadores conectados se actualizo. */
public record EventoLista(String tipo, List<EventoJugador> jugadores) {
    public EventoLista(List<EventoJugador> jugadores) {
        this("JUGADORES", jugadores);
    }
}
