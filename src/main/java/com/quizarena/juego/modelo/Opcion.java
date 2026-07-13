package com.quizarena.juego.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Una opcion de respuesta.
 * Preparada para serializarse a JSON (Redis): el constructor @JsonCreator
 * permite reconstruirla conservando su id original.
 */
public class Opcion {

    private final String id;
    private final String texto;
    private final boolean esCorrecta;

    public Opcion(String texto, boolean esCorrecta) {
        this(UUID.randomUUID().toString(), texto, esCorrecta);
    }

    @JsonCreator
    public Opcion(@JsonProperty("id") String id,
                  @JsonProperty("texto") String texto,
                  @JsonProperty("esCorrecta") boolean esCorrecta) {
        this.id = id;
        this.texto = texto;
        this.esCorrecta = esCorrecta;
    }

    public String getId() { return id; }
    public String getTexto() { return texto; }
    public boolean isEsCorrecta() { return esCorrecta; }
}
