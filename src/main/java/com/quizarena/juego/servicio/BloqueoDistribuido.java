package com.quizarena.juego.servicio;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Bloqueo distribuido sobre Redis.
 *
 * PROBLEMA QUE RESUELVE: con varias instancias del Servicio de Juego, dos
 * respuestas de jugadores distintos podrian procesarse AL MISMO TIEMPO en
 * instancias distintas y corromper el puntaje (condicion de carrera).
 *
 * Este bloqueo garantiza que solo UNA instancia modifique una sala a la vez,
 * usando la operacion atomica SETNX de Redis. Es lo que hace que la HU-09
 * (respuestas concurrentes sin condiciones de carrera) siga cumpliendose
 * cuando el servicio escala horizontalmente.
 */
@Component
public class BloqueoDistribuido {

    private static final Duration TTL_BLOQUEO = Duration.ofSeconds(5);   // se libera solo si algo falla
    private static final int INTENTOS = 50;
    private static final long ESPERA_MS = 20;

    private final StringRedisTemplate redis;

    public BloqueoDistribuido(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Ejecuta la accion mientras se tiene el bloqueo exclusivo de esa clave. */
    public <T> T conBloqueo(String clave, Supplier<T> accion) {
        String llave = "lock:" + clave;
        String valor = UUID.randomUUID().toString();   // identifica a este dueno

        boolean adquirido = adquirir(llave, valor);
        if (!adquirido) {
            throw new IllegalStateException("No se pudo obtener el bloqueo de " + clave);
        }
        try {
            return accion.get();
        } finally {
            liberar(llave, valor);
        }
    }

    private boolean adquirir(String llave, String valor) {
        for (int i = 0; i < INTENTOS; i++) {
            // SETNX atomico: solo escribe si la clave NO existe
            Boolean ok = redis.opsForValue().setIfAbsent(llave, valor, TTL_BLOQUEO);
            if (Boolean.TRUE.equals(ok)) return true;
            try {
                Thread.sleep(ESPERA_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void liberar(String llave, String valor) {
        // Solo borra el bloqueo si sigue siendo NUESTRO (evita liberar el de otro)
        String actual = redis.opsForValue().get(llave);
        if (valor.equals(actual)) {
            redis.delete(llave);
        }
    }
}
