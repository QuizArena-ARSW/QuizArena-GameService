package com.quizarena.juego.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/** Un jugador de una sala. Serializable a JSON para guardarse en Redis. */
public class Jugador {

    private final String id;
    private final String idUsuario;
    private final String apodo;
    private int puntajeTotal;
    private boolean conectado;

    public Jugador(String apodo, String idUsuario) {
        this(UUID.randomUUID().toString(), idUsuario, apodo, 0, true);
    }

    @JsonCreator
    public Jugador(@JsonProperty("id") String id,
                   @JsonProperty("idUsuario") String idUsuario,
                   @JsonProperty("apodo") String apodo,
                   @JsonProperty("puntajeTotal") int puntajeTotal,
                   @JsonProperty("conectado") boolean conectado) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.apodo = apodo;
        this.puntajeTotal = puntajeTotal;
        this.conectado = conectado;
    }

    public void sumarPuntaje(int puntos) { this.puntajeTotal += puntos; }
    public void marcarDesconectado() { this.conectado = false; }
    public void reconectar() { this.conectado = true; }

    public String getId() { return id; }
    public String getIdUsuario() { return idUsuario; }
    public String getApodo() { return apodo; }
    public int getPuntajeTotal() { return puntajeTotal; }
    public boolean isConectado() { return conectado; }
}
