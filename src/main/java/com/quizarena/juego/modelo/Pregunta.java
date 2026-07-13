package com.quizarena.juego.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

/** Una pregunta de la trivia. Serializable a JSON para guardarse en Redis. */
public class Pregunta {

    private final String id;
    private final String enunciado;
    private final List<Opcion> opciones;
    private final int tiempoLimiteSegundos;

    public Pregunta(String enunciado, List<Opcion> opciones, int tiempoLimiteSegundos) {
        this(UUID.randomUUID().toString(), enunciado, opciones, tiempoLimiteSegundos);
    }

    @JsonCreator
    public Pregunta(@JsonProperty("id") String id,
                    @JsonProperty("enunciado") String enunciado,
                    @JsonProperty("opciones") List<Opcion> opciones,
                    @JsonProperty("tiempoLimiteSegundos") int tiempoLimiteSegundos) {
        this.id = id;
        this.enunciado = enunciado;
        this.opciones = opciones;
        this.tiempoLimiteSegundos = tiempoLimiteSegundos;
    }

    @JsonIgnore
    public boolean esCorrecta(String idOpcion) {
        return opciones.stream()
                .filter(Opcion::isEsCorrecta)
                .anyMatch(o -> o.getId().equals(idOpcion));
    }

    public String getId() { return id; }
    public String getEnunciado() { return enunciado; }
    public List<Opcion> getOpciones() { return opciones; }
    public int getTiempoLimiteSegundos() { return tiempoLimiteSegundos; }
}
