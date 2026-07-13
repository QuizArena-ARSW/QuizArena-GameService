package com.quizarena.juego.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Configuracion de Redis (Fase 5).
 *
 * - StringRedisTemplate: para leer/escribir el estado de las salas (como JSON).
 * - RedisMessageListenerContainer: para el PUB/SUB entre instancias, que hace
 *   que un evento publicado por una instancia llegue a los jugadores conectados
 *   a CUALQUIER otra instancia.
 *
 * Nota: NO definimos un ObjectMapper propio; se usa el que Spring Boot ya
 * configura por defecto (asi conservamos sus ajustes).
 */
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory factory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        return container;
    }
}
