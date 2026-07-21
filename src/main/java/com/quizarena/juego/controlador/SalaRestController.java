package com.quizarena.juego.controlador;

import com.quizarena.juego.modelo.Sala;
import com.quizarena.juego.servicio.GestorSalas;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST para operaciones que NO son de tiempo real:
 * crear una sala y consultar su estado. (Estas pasan por el API Gateway).
 */
@RestController
@RequestMapping("/api/salas")

public class SalaRestController {

    private static final String CLAVE_ERROR = "error";
    private static final String CLAVE_CODIGO = "codigo";
    private static final String CLAVE_ESTADO = "estado";
    private static final String CLAVE_TOTAL_RONDAS = "totalRondas";

    private final GestorSalas gestorSalas;

    public SalaRestController(GestorSalas gestorSalas) {
        this.gestorSalas = gestorSalas;
    }

    /**
     * Crea una sala a partir de un banco REAL (pide sus preguntas a Identidad).
     * El cliente envia: { "idBanco": "37d30806-..." }
     */
    @PostMapping
    public ResponseEntity<Object> crearSala(@RequestBody Map<String, String> body, HttpServletRequest req) {
        String idBanco = body.get("idBanco");
        if (idBanco == null || idBanco.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, "Falta el idBanco"));
        }
        try {
            Sala sala = gestorSalas.crearSala(idBanco, idCreadorDe(req));
            return ResponseEntity.ok(resumenBasico(sala));
        } catch (Exception e) {
            // Si Identidad no responde o el banco no existe, avisamos claramente
            return ResponseEntity.badRequest().body(Map.of(
                    CLAVE_ERROR, "No se pudo crear la sala: " + e.getMessage()));
        }
    }

    /**
     * Crea una sala DEMO con preguntas de ejemplo (sin depender de Identidad).
     * Util para probar el tiempo real rapido. POST /api/salas/demo
     */
    @PostMapping("/demo")
    public Map<String, Object> crearSalaDemo(HttpServletRequest req) {
        Sala sala = gestorSalas.crearSalaDemo(idCreadorDe(req));
        return resumenBasico(sala);
    }

    /** El creador es quien trae un token valido (FiltroJwtOpcional); null si no hay sesion (p.ej. demo anonimo). */
    private UUID idCreadorDe(HttpServletRequest req) {
        String idUsuario = (String) req.getAttribute("idUsuario");
        return idUsuario != null ? UUID.fromString(idUsuario) : null;
    }

    /** Consulta el estado de una sala. */
    @GetMapping("/{codigo}")
    public Map<String, Object> consultarSala(@PathVariable String codigo) {
        Sala sala = gestorSalas.buscarSala(codigo);
        if (sala == null) {
            return Map.of(CLAVE_ERROR, "Sala no encontrada");
        }
        Map<String, Object> resumen = resumenBasico(sala);
        resumen.put("jugadores", sala.getJugadores().size());
        resumen.put("ronda", sala.getRondaActual());
        return resumen;
    }

    /** Campos comunes (codigo, estado, totalRondas) que devuelven varios endpoints. */
    private Map<String, Object> resumenBasico(Sala sala) {
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put(CLAVE_CODIGO, sala.getCodigo());
        resumen.put(CLAVE_ESTADO, sala.getEstado().name());
        resumen.put(CLAVE_TOTAL_RONDAS, sala.getTotalRondas());
        return resumen;
    }
}
