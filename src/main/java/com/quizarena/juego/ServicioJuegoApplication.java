package com.quizarena.juego;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del Servicio de Juego en tiempo real de QuizArena.
 * Arranca con: mvn spring-boot:run   (o ejecutando esta clase desde IntelliJ)
 */
@SpringBootApplication
public class ServicioJuegoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServicioJuegoApplication.class, args);
    }
}
