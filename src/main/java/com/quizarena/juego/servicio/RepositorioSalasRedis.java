package com.quizarena.juego.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizarena.juego.modelo.Sala;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Set;

/**
 * Guarda y recupera las salas en REDIS (Fase 5).
 *
 * Antes: las salas vivian en un ConcurrentHashMap dentro de UNA instancia.
 * Ahora: viven en Redis, una memoria compartida a la que acceden TODAS las
 * instancias del Servicio de Juego. Ese es el cambio que hace real el
 * escalado horizontal.
 *
 * Claves usadas:
 *   sala:{codigo}     -> el estado completo de la sala, en JSON
 *   salas:activas     -> conjunto con los codigos de las salas vivas (para metricas)
 */
@Repository
public class RepositorioSalasRedis {

    private static final String PREFIJO = "sala:";
    private static final String SET_ACTIVAS = "salas:activas";
    private static final Duration TTL_SALA = Duration.ofHours(6); // se limpia sola

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RepositorioSalasRedis(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    public void guardar(Sala sala) {
        try {
            String json = mapper.writeValueAsString(sala);
            redis.opsForValue().set(PREFIJO + sala.getCodigo(), json, TTL_SALA);
            redis.opsForSet().add(SET_ACTIVAS, sala.getCodigo());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo guardar la sala en Redis", e);
        }
    }

    /** Devuelve la sala, o null si no existe. */
    public Sala buscar(String codigo) {
        String json = redis.opsForValue().get(PREFIJO + codigo);
        if (json == null) return null;
        try {
            return mapper.readValue(json, Sala.class);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer la sala desde Redis", e);
        }
    }

    public boolean existe(String codigo) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIJO + codigo));
    }

    public void eliminar(String codigo) {
        redis.delete(PREFIJO + codigo);
        redis.opsForSet().remove(SET_ACTIVAS, codigo);
    }

    /** Cuantas salas hay activas (para la metrica). */
    public long contarActivas() {
        Long n = redis.opsForSet().size(SET_ACTIVAS);
        return n != null ? n : 0L;
    }

    public Set<String> codigosActivos() {
        return redis.opsForSet().members(SET_ACTIVAS);
    }
}
