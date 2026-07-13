package com.quizarena.juego.modelo;

/** Estados por los que pasa una sala durante su ciclo de vida. */
public enum EstadoSala {
    ESPERANDO,   // creada, esperando que se unan jugadores
    EN_CURSO,    // partida en marcha
    FINALIZADA   // termino
}
