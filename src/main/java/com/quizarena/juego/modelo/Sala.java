package com.quizarena.juego.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Una sala de juego.
 *
 * CAMBIO DE LA FASE 5: el estado de la sala ya NO vive en la memoria local del
 * servicio, sino en REDIS. Por eso esta clase debe poder convertirse a JSON y
 * reconstruirse desde JSON sin perder ningun dato (ids incluidos).
 *
 * Esto es lo que permite que VARIAS INSTANCIAS del Servicio de Juego compartan
 * la misma partida: todas leen y escriben la sala en Redis.
 */
public class Sala {

    private final String id;
    private final String codigo;
    private EstadoSala estado;
    private int rondaActual;
    private final int maxJugadores;

    private final String idBanco;
    private final String materia;

    private final Map<String, Jugador> jugadores;
    private final List<Pregunta> preguntas;
    private int indicePreguntaActual;

    public Sala(String codigo, List<Pregunta> preguntas, int maxJugadores,
                String idBanco, String materia) {
        this(UUID.randomUUID().toString(), codigo, EstadoSala.ESPERANDO, 0, maxJugadores,
                idBanco, materia, new LinkedHashMap<>(), preguntas, -1);
    }

    @JsonCreator
    public Sala(@JsonProperty("id") String id,
                @JsonProperty("codigo") String codigo,
                @JsonProperty("estado") EstadoSala estado,
                @JsonProperty("rondaActual") int rondaActual,
                @JsonProperty("maxJugadores") int maxJugadores,
                @JsonProperty("idBanco") String idBanco,
                @JsonProperty("materia") String materia,
                @JsonProperty("jugadores") Map<String, Jugador> jugadores,
                @JsonProperty("preguntas") List<Pregunta> preguntas,
                @JsonProperty("indicePreguntaActual") int indicePreguntaActual) {
        this.id = id;
        this.codigo = codigo;
        this.estado = estado;
        this.rondaActual = rondaActual;
        this.maxJugadores = maxJugadores;
        this.idBanco = idBanco;
        this.materia = materia;
        this.jugadores = jugadores != null ? jugadores : new LinkedHashMap<>();
        this.preguntas = preguntas;
        this.indicePreguntaActual = indicePreguntaActual;
    }

    public Jugador agregarJugador(String apodo, String idUsuario) {
        Jugador jugador = new Jugador(apodo, idUsuario);
        jugadores.put(jugador.getId(), jugador);
        return jugador;
    }

    public void removerJugador(String idJugador) { jugadores.remove(idJugador); }

    @JsonIgnore
    public boolean estaLlena() { return jugadores.size() >= maxJugadores; }

    /** Avanza a la siguiente pregunta. Devuelve null si ya no hay mas. */
    public Pregunta siguientePregunta() {
        indicePreguntaActual++;
        if (indicePreguntaActual >= preguntas.size()) return null;
        rondaActual = indicePreguntaActual + 1;
        return preguntas.get(indicePreguntaActual);
    }

    @JsonIgnore
    public Pregunta getPreguntaActual() {
        if (indicePreguntaActual < 0 || indicePreguntaActual >= preguntas.size()) return null;
        return preguntas.get(indicePreguntaActual);
    }

    public void iniciar() { this.estado = EstadoSala.EN_CURSO; }
    public void finalizar() { this.estado = EstadoSala.FINALIZADA; }

    public String getId() { return id; }
    public String getCodigo() { return codigo; }
    public EstadoSala getEstado() { return estado; }
    public int getRondaActual() { return rondaActual; }
    public int getMaxJugadores() { return maxJugadores; }
    public String getIdBanco() { return idBanco; }
    public String getMateria() { return materia; }
    public Map<String, Jugador> getJugadores() { return jugadores; }
    public List<Pregunta> getPreguntas() { return preguntas; }
    public int getIndicePreguntaActual() { return indicePreguntaActual; }

    @JsonIgnore
    public int getTotalRondas() { return preguntas.size(); }

    @JsonIgnore
    public List<Jugador> getListaJugadores() { return new ArrayList<>(jugadores.values()); }
}
