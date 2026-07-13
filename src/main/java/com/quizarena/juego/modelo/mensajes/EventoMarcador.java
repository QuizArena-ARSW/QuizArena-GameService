package com.quizarena.juego.modelo.mensajes;

import java.util.List;

/** Evento: el marcador actualizado (jugadores ordenados por puntaje). */
public record EventoMarcador(String tipo, List<EventoJugador> ranking) {
    public EventoMarcador(List<EventoJugador> ranking) {
        this("MARCADOR", ranking);
    }
}
