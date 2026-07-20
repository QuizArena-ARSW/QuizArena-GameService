package com.quizarena.juego.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizarena.juego.modelo.mensajes.EventoChat;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Guarda el historial de chat de cada sala en Redis, como una lista aparte
 * del blob de la sala (RepositorioSalasRedis): el chat no es parte del
 * estado de juego, solo vive mientras la sala existe.
 *
 * Clave usada: sala:{codigo}:chat -> lista de EventoChat (JSON), recortada a
 * los ultimos MAX_MENSAJES para no crecer sin limite. Mismo TTL que la sala
 * (6h), renovado en cada mensaje nuevo.
 */
@Repository
public class RepositorioChatRedis {

    private static final String PREFIJO = "sala:";
    private static final String SUFIJO = ":chat";
    private static final Duration TTL = Duration.ofHours(6);
    private static final int MAX_MENSAJES = 50;

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RepositorioChatRedis(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    public void agregar(String codigo, EventoChat mensaje) {
        try {
            String clave = clave(codigo);
            redis.opsForList().rightPush(clave, mapper.writeValueAsString(mensaje));
            redis.opsForList().trim(clave, -MAX_MENSAJES, -1);
            redis.expire(clave, TTL);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo guardar el mensaje de chat en Redis", e);
        }
    }

    public List<EventoChat> historial(String codigo) {
        List<String> crudos = redis.opsForList().range(clave(codigo), 0, -1);
        if (crudos == null) return List.of();
        List<EventoChat> mensajes = new ArrayList<>();
        for (String json : crudos) {
            try {
                mensajes.add(mapper.readValue(json, EventoChat.class));
            } catch (Exception ignorado) {
                // mensaje corrupto/ilegible: se omite, no debe tumbar el historial completo
            }
        }
        return mensajes;
    }

    private String clave(String codigo) {
        return PREFIJO + codigo + SUFIJO;
    }
}
