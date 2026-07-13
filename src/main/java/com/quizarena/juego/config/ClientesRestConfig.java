package com.quizarena.juego.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configura el cliente HTTP con el que el Servicio de Juego llama por REST
 * al Servicio de Identidad. La URL base se lee de application.properties
 * (asi es facil cambiarla en cada entorno, ej. en Azure).
 */
@Configuration
public class ClientesRestConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
}
