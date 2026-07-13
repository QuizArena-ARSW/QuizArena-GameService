package com.quizarena.juego.datos;

import com.quizarena.juego.modelo.Opcion;
import com.quizarena.juego.modelo.Pregunta;

import java.util.List;

/**
 * Banco de preguntas "quemado" (hardcoded) solo para la Fase 1.
 *
 * En la Fase 2 esto se reemplaza: el Servicio de Juego pedira las preguntas
 * reales al Servicio de Identidad por REST. Por ahora, esto nos permite
 * probar todo el flujo de tiempo real sin depender de nada externo.
 */
public class BancoPreguntasDemo {

    public static List<Pregunta> preguntasEjemplo() {
        return List.of(
                new Pregunta(
                        "¿Que patron de arquitectura usa QuizArena?",
                        List.of(
                                new Opcion("Monolito", false),
                                new Opcion("Microservicios", true),
                                new Opcion("Cliente pesado", false),
                                new Opcion("Peer to peer", false)
                        ),
                        20
                ),
                new Pregunta(
                        "¿Que protocolo se usa para el tiempo real?",
                        List.of(
                                new Opcion("FTP", false),
                                new Opcion("SMTP", false),
                                new Opcion("WebSocket / STOMP", true),
                                new Opcion("HTTP polling", false)
                        ),
                        15
                ),
                new Pregunta(
                        "¿Que se usa como estado compartido entre instancias?",
                        List.of(
                                new Opcion("Redis", true),
                                new Opcion("Un archivo de texto", false),
                                new Opcion("Variables globales", false),
                                new Opcion("Cookies", false)
                        ),
                        15
                )
        );
    }
}
