package com.quizarena.juego.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuracion del canal de tiempo real (WebSocket + STOMP).
 *
 * Aqui se define:
 *  - El endpoint por el que los clientes abren la conexion WebSocket.
 *  - El "broker" que reparte los mensajes a los canales (topics).
 *  - El prefijo de los mensajes que ENTRAN desde los clientes.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Los clientes se conectan aqui: http://localhost:8081/ws-juego
        // withSockJS() agrega compatibilidad para navegadores/redes sin WebSocket.
        registry.addEndpoint("/ws-juego")
                .setAllowedOriginPatterns("*")   // en produccion: restringe a tu dominio
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /topic/...  -> canales a los que los jugadores se SUSCRIBEN para recibir
        //                (ej. /topic/sala/ABC123 : todo lo que pasa en esa sala)
        registry.enableSimpleBroker("/topic");

        // /app/...   -> prefijo de los mensajes que los jugadores ENVIAN al servidor
        //                (ej. /app/sala/ABC123/responder)
        registry.setApplicationDestinationPrefixes("/app");
    }
}
