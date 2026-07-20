package com.quizarena.juego.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuracion del canal de tiempo real (WebSocket + STOMP).
 *
 * CAMBIO DE LA FASE 6: los origenes permitidos ya NO estan escritos a mano;
 * se leen de una variable de entorno (quizarena.cors.origenes). En local vale
 * "*"; en Azure se pone la URL real del frontend.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String[] origenesPermitidos;

    public WebSocketConfig(@Value("${quizarena.cors.origenes}") String origenes) {
        this.origenesPermitidos = origenes.split(",");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-juego")
                .setAllowedOriginPatterns(origenesPermitidos)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
