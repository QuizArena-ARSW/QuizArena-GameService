package com.quizarena.juego.eventos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica los eventos de la partida a traves de REDIS PUB/SUB (Fase 5).
 *
 * PROBLEMA QUE RESUELVE: el broker STOMP de Spring solo entrega mensajes a los
 * clientes conectados a ESA instancia. Si el jugador A esta en la instancia 1 y
 * el jugador B en la 2, publicar localmente dejaria a B sin recibir la pregunta.
 *
 * SOLUCION: en vez de entregar directo, se publica el evento en un canal de
 * Redis. TODAS las instancias estan suscritas a ese canal, y cada una lo
 * reenvia a sus propios clientes. Asi la difusion en tiempo real funciona
 * aunque los jugadores de una sala esten repartidos entre instancias distintas.
 */
@Component
public class PublicadorEventos {

    public static final String CANAL = "quizarena:eventos";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public PublicadorEventos(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    /** Difunde un objeto (se envia como JSON) a un destino STOMP. */
    public void publicar(String destino, Object cuerpo) {
        enviar(new EventoSalaWrapper(destino, cuerpo, true));
    }

    /** Difunde texto plano (ej. el id del jugador en su canal personal). */
    public void publicarTexto(String destino, String texto) {
        enviar(new EventoSalaWrapper(destino, texto, false));
    }

    private void enviar(EventoSalaWrapper w) {
        try {
            String cuerpo = w.esJson()
                    ? mapper.writeValueAsString(w.cuerpo())
                    : String.valueOf(w.cuerpo());
            EventoSala evento = new EventoSala(w.destino(), cuerpo, w.esJson());
            redis.convertAndSend(CANAL, mapper.writeValueAsString(evento));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo publicar el evento", e);
        }
    }

    private record EventoSalaWrapper(String destino, Object cuerpo, boolean esJson) {}
}
