package com.quizarena.juego.eventos;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Escucha el canal de Redis y reenvia cada evento a los clientes WebSocket
 * conectados A ESTA instancia.
 *
 * Junto con PublicadorEventos forma el "puente" que permite que la difusion en
 * tiempo real funcione entre varias instancias del Servicio de Juego.
 */
@Component
public class SuscriptorEventos {

    private static final Logger log = LoggerFactory.getLogger(SuscriptorEventos.class);

    private final RedisMessageListenerContainer container;
    private final SimpMessagingTemplate messaging;
    private final ObjectMapper mapper;

    public SuscriptorEventos(RedisMessageListenerContainer container,
                             SimpMessagingTemplate messaging,
                             ObjectMapper mapper) {
        this.container = container;
        this.messaging = messaging;
        this.mapper = mapper;
    }

    @PostConstruct
    public void suscribirse() {
        container.addMessageListener((mensaje, patron) -> {
            try {
                EventoSala evento = mapper.readValue(new String(mensaje.getBody()), EventoSala.class);

                if (evento.esJson()) {
                    // Se vuelve a convertir a objeto para que Spring lo serialice
                    // correctamente como JSON (si se enviara el String, quedaria
                    // doblemente codificado y el cliente no lo entenderia).
                    Object cuerpo = mapper.readValue(evento.cuerpo(), Object.class);
                    messaging.convertAndSend(evento.destino(), cuerpo);
                } else {
                    messaging.convertAndSend(evento.destino(), evento.cuerpo());
                }
            } catch (Exception e) {
                log.warn("No se pudo procesar un evento de Redis: {}", e.getMessage());
            }
        }, new ChannelTopic(PublicadorEventos.CANAL));

        log.info("Suscrito al canal de eventos de Redis: {}", PublicadorEventos.CANAL);
    }
}
