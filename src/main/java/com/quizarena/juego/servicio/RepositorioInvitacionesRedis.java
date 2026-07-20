package com.quizarena.juego.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizarena.juego.modelo.InvitacionSala;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Guarda las invitaciones a sala pendientes de cada usuario en Redis, con el
 * mismo StringRedisTemplate que RepositorioSalasRedis.
 *
 * Clave usada: invitaciones:{idUsuarioDestino} -> hash de codigo -> InvitacionSala (JSON)
 * TTL generoso (2h) sobre el hash completo: mas simple que trackear el TTL
 * exacto de cada sala individualmente, y de sobra para una invitacion puntual.
 */
@Repository
public class RepositorioInvitacionesRedis {

    private static final String PREFIJO = "invitaciones:";
    private static final Duration TTL = Duration.ofHours(2);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final RepositorioSalasRedis repositorioSalas;

    public RepositorioInvitacionesRedis(StringRedisTemplate redis, ObjectMapper mapper,
                                         RepositorioSalasRedis repositorioSalas) {
        this.redis = redis;
        this.mapper = mapper;
        this.repositorioSalas = repositorioSalas;
    }

    public void agregar(String idUsuarioDestino, InvitacionSala invitacion) {
        try {
            String clave = PREFIJO + idUsuarioDestino;
            String json = mapper.writeValueAsString(invitacion);
            redis.opsForHash().put(clave, invitacion.getCodigo(), json);
            redis.expire(clave, TTL);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo guardar la invitacion en Redis", e);
        }
    }

    /** Lista las invitaciones vigentes del usuario, descartando (y limpiando) las de salas que ya no existen. */
    public List<InvitacionSala> listar(String idUsuario) {
        String clave = PREFIJO + idUsuario;
        Map<Object, Object> entradas = redis.opsForHash().entries(clave);
        List<InvitacionSala> vigentes = new ArrayList<>();
        for (Map.Entry<Object, Object> entrada : entradas.entrySet()) {
            String codigo = (String) entrada.getKey();
            if (!repositorioSalas.existe(codigo)) {
                redis.opsForHash().delete(clave, codigo);
                continue;
            }
            try {
                vigentes.add(mapper.readValue((String) entrada.getValue(), InvitacionSala.class));
            } catch (Exception e) {
                redis.opsForHash().delete(clave, codigo);
            }
        }
        return vigentes;
    }

    public void quitar(String idUsuario, String codigo) {
        redis.opsForHash().delete(PREFIJO + idUsuario, codigo);
    }
}
