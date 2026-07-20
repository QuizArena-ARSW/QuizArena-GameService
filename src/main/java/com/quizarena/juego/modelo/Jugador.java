package com.quizarena.juego.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Un jugador de una sala. Serializable a JSON para guardarse en Redis. */
public class Jugador {

    private final String id;
    private final String idUsuario;
    private final String apodo;
    private int puntajeTotal;
    private boolean conectado;

    /** idPregunta -> idOpcion elegida. Sirve para el resumen al terminar la partida. */
    private final Map<String, String> respuestas;

    public Jugador(String apodo, String idUsuario) {
        this(UUID.randomUUID().toString(), idUsuario, apodo, 0, true, new LinkedHashMap<>());
    }

    @JsonCreator
    public Jugador(@JsonProperty("id") String id,
                   @JsonProperty("idUsuario") String idUsuario,
                   @JsonProperty("apodo") String apodo,
                   @JsonProperty("puntajeTotal") int puntajeTotal,
                   @JsonProperty("conectado") boolean conectado,
                   @JsonProperty("respuestas") Map<String, String> respuestas) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.apodo = apodo;
        this.puntajeTotal = puntajeTotal;
        this.conectado = conectado;
        // jugadores serializados antes de esta version no tienen "respuestas" en su JSON de Redis
        this.respuestas = respuestas != null ? respuestas : new LinkedHashMap<>();
    }

    public void sumarPuntaje(int puntos) { this.puntajeTotal += puntos; }
    public void marcarDesconectado() { this.conectado = false; }
    public void reconectar() { this.conectado = true; }
    public void registrarRespuesta(String idPregunta, String idOpcion) { respuestas.put(idPregunta, idOpcion); }

    public String getId() { return id; }
    public String getIdUsuario() { return idUsuario; }
    public String getApodo() { return apodo; }
    public int getPuntajeTotal() { return puntajeTotal; }
    public boolean isConectado() { return conectado; }
    public Map<String, String> getRespuestas() { return respuestas; }
}
