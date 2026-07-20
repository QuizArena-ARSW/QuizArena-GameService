package com.quizarena.juego.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Invitacion a unirse a una sala activa, enviada por su creador a un amigo. */
public class InvitacionSala {

    private final String codigo;
    private final String materia;
    private final String nombreInvitador;
    private final long fecha;

    @JsonCreator
    public InvitacionSala(@JsonProperty("codigo") String codigo,
                           @JsonProperty("materia") String materia,
                           @JsonProperty("nombreInvitador") String nombreInvitador,
                           @JsonProperty("fecha") long fecha) {
        this.codigo = codigo;
        this.materia = materia;
        this.nombreInvitador = nombreInvitador;
        this.fecha = fecha;
    }

    public String getCodigo() { return codigo; }
    public String getMateria() { return materia; }
    public String getNombreInvitador() { return nombreInvitador; }
    public long getFecha() { return fecha; }
}
