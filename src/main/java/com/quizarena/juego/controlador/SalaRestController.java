package com.quizarena.juego.controlador;

import com.quizarena.juego.modelo.Sala;
import com.quizarena.juego.servicio.GestorSalas;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para operaciones que NO son de tiempo real:
 * crear una sala y consultar su estado. (Estas pasan por el API Gateway).
 */
@RestController
@RequestMapping("/api/salas")

public class SalaRestController {

    private final GestorSalas gestorSalas;

    public SalaRestController(GestorSalas gestorSalas) {
        this.gestorSalas = gestorSalas;
    }

    /**
     * Crea una sala a partir de un banco REAL (pide sus preguntas a Identidad).
     * El cliente envia: { "idBanco": "37d30806-..." }
     */
    @PostMapping
    public ResponseEntity<?> crearSala(@RequestBody Map<String, String> body) {
        String idBanco = body.get("idBanco");
        if (idBanco == null || idBanco.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Falta el idBanco"));
        }
        try {
            Sala sala = gestorSalas.crearSala(idBanco);
            return ResponseEntity.ok(Map.of(
                    "codigo", sala.getCodigo(),
                    "estado", sala.getEstado().name(),
                    "totalRondas", sala.getTotalRondas()
            ));
        } catch (Exception e) {
            // Si Identidad no responde o el banco no existe, avisamos claramente
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "No se pudo crear la sala: " + e.getMessage()));
        }
    }

    /**
     * Crea una sala DEMO con preguntas de ejemplo (sin depender de Identidad).
     * Util para probar el tiempo real rapido. POST /api/salas/demo
     */
    @PostMapping("/demo")
    public Map<String, Object> crearSalaDemo() {
        Sala sala = gestorSalas.crearSalaDemo();
        return Map.of(
                "codigo", sala.getCodigo(),
                "estado", sala.getEstado().name(),
                "totalRondas", sala.getTotalRondas()
        );
    }

    /** Consulta el estado de una sala. */
    @GetMapping("/{codigo}")
    public Map<String, Object> consultarSala(@PathVariable String codigo) {
        Sala sala = gestorSalas.buscarSala(codigo);
        if (sala == null) {
            return Map.of("error", "Sala no encontrada");
        }
        return Map.of(
                "codigo", sala.getCodigo(),
                "estado", sala.getEstado().name(),
                "jugadores", sala.getJugadores().size(),
                "ronda", sala.getRondaActual(),
                "totalRondas", sala.getTotalRondas()
        );
    }
}
