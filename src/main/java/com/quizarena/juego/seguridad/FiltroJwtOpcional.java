package com.quizarena.juego.seguridad;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * A diferencia del filtro de Identidad, este NUNCA rechaza una peticion:
 * este servicio no tiene Spring Security (no hay roles ni login aqui), asi
 * que un token ausente o invalido simplemente deja la peticion sin identidad
 * (atributo "idUsuario" = null). Los endpoints que de verdad necesitan saber
 * quien llama (crear/consultar invitaciones) verifican ese atributo ellos
 * mismos y responden 401 si hace falta.
 *
 * Se usa el mismo JWT_SECRET compartido con Identidad y el Gateway: este
 * servicio tiene ingress externo (el navegador abre el WebSocket directo),
 * asi que sus endpoints REST tambien son alcanzables sin pasar por el
 * Gateway y deben validar el token por su cuenta.
 */
@Component
public class FiltroJwtOpcional extends OncePerRequestFilter {

    private final SecretKey clave;

    public FiltroJwtOpcional(@Value("${quizarena.jwt.secret}") String secret) {
        this.clave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            try {
                String token = cabecera.substring(7);
                String idUsuario = Jwts.parser().verifyWith(clave).build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject();
                request.setAttribute("idUsuario", idUsuario);
            } catch (Exception e) {
                // Token ausente/invalido/expirado: seguimos sin identidad, no se rechaza.
            }
        }
        filterChain.doFilter(request, response);
    }
}
