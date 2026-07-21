package com.quizarena.juego.controlador;

import com.quizarena.juego.modelo.InvitacionSala;
import com.quizarena.juego.modelo.Sala;
import com.quizarena.juego.servicio.GestorSalas;
import com.quizarena.juego.servicio.RepositorioInvitacionesRedis;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Invitar a un amigo a una sala activa. Solo quien creo la sala puede invitar.
 *
 * No hay Spring Security en este servicio: la identidad del llamador viene del
 * atributo "idUsuario" que deja FiltroJwtOpcional (null si no habia token
 * valido), por eso se recibe HttpServletRequest en vez de Authentication.
 */
@RestController
@RequestMapping("/api/salas")
public class InvitacionSalaController {

    private static final String CLAVE_ERROR = "error";

    private final GestorSalas gestorSalas;
    private final RepositorioInvitacionesRedis repositorioInvitaciones;

    public InvitacionSalaController(GestorSalas gestorSalas, RepositorioInvitacionesRedis repositorioInvitaciones) {
        this.gestorSalas = gestorSalas;
        this.repositorioInvitaciones = repositorioInvitaciones;
    }

    @PostMapping("/{codigo}/invitaciones")
    public ResponseEntity<Object> invitar(@PathVariable String codigo,
                                     @RequestBody Map<String, String> body,
                                     HttpServletRequest req) {
        String idUsuario = (String) req.getAttribute("idUsuario");
        if (idUsuario == null) {
            return ResponseEntity.status(401).body(Map.of(CLAVE_ERROR, "Se requiere iniciar sesion"));
        }

        String idAmigo = body.get("idAmigo");
        if (idAmigo == null || idAmigo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(CLAVE_ERROR, "Falta el idAmigo"));
        }

        Sala sala = gestorSalas.buscarSala(codigo);
        if (sala == null) {
            return ResponseEntity.status(404).body(Map.of(CLAVE_ERROR, "Sala no encontrada"));
        }
        if (sala.getIdCreador() == null || !sala.getIdCreador().toString().equals(idUsuario)) {
            return ResponseEntity.status(403).body(Map.of(CLAVE_ERROR, "Solo quien creo la sala puede invitar"));
        }

        String nombreInvitador = body.getOrDefault("nombreInvitador", "Un amigo");
        repositorioInvitaciones.agregar(idAmigo,
                new InvitacionSala(codigo, sala.getMateria(), nombreInvitador, System.currentTimeMillis()));

        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/invitaciones")
    public ResponseEntity<Object> misInvitaciones(HttpServletRequest req) {
        String idUsuario = (String) req.getAttribute("idUsuario");
        if (idUsuario == null) {
            return ResponseEntity.status(401).body(List.of());
        }
        return ResponseEntity.ok(repositorioInvitaciones.listar(idUsuario));
    }
}
