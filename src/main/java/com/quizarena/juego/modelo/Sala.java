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

    /** Quien creo la sala (puede ser null: salas creadas antes de esta version, o demos sin token). */
    private final UUID idCreador;

    public Sala(String codigo, List<Pregunta> preguntas, int maxJugadores,
                String idBanco, String materia, UUID idCreador) {
        this(UUID.randomUUID().toString(), codigo, EstadoSala.ESPERANDO, 0, maxJugadores,
                idBanco, materia, new LinkedHashMap<>(), preguntas, -1, idCreador);
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
                @JsonProperty("indicePreguntaActual") int indicePreguntaActual,
                @JsonProperty("idCreador") UUID idCreador) {
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
        this.idCreador = idCreador;
    }

    /**
     * Agrega un jugador, o si ya estaba (recarga de pagina, reconexion) lo
     * reutiliza en vez de duplicarlo: conserva su id y su puntaje acumulado.
     */
    public Jugador agregarJugador(String apodo, String idUsuario) {
        Jugador existente = buscarJugador(apodo, idUsuario);
        if (existente != null) {
            existente.reconectar();
            return existente;
        }
        Jugador jugador = new Jugador(apodo, idUsuario);
        jugadores.put(jugador.getId(), jugador);
        return jugador;
    }

    private Jugador buscarJugador(String apodo, String idUsuario) {
        boolean autenticado = idUsuario != null && !idUsuario.isBlank();
        for (Jugador j : jugadores.values()) {
            boolean jAutenticado = j.getIdUsuario() != null && !j.getIdUsuario().isBlank();
            if (autenticado && jAutenticado) {
                if (idUsuario.equals(j.getIdUsuario())) return j;
            } else if (!autenticado && !jAutenticado) {
                if (apodo.equals(j.getApodo())) return j;
            }
        }
        return null;
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
    public UUID getIdCreador() { return idCreador; }

    @JsonIgnore
    public int getTotalRondas() { return preguntas.size(); }

    @JsonIgnore
    public List<Jugador> getListaJugadores() { return new ArrayList<>(jugadores.values()); }
}
