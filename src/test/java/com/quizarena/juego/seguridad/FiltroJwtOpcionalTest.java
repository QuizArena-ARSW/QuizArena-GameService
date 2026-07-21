package com.quizarena.juego.seguridad;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * A diferencia del filtro del Gateway, este NUNCA rechaza la peticion: solo
 * identifica al usuario si el token es valido, y sigue la cadena siempre.
 */
class FiltroJwtOpcionalTest {

    private static final String SECRETO = "clave-secreta-de-pruebas-de-al-menos-32-bytes-de-largo";

    private FiltroJwtOpcional filtro() {
        return new FiltroJwtOpcional(SECRETO);
    }

    private String tokenValido(String idUsuario) {
        SecretKey clave = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(idUsuario)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(clave)
                .compact();
    }

    @Test
    void sinCabeceraDeAutorizacionSigueLaCadenaSinIdentidad() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filtro().doFilterInternal(request, response, chain);

        assertThat(request.getAttribute("idUsuario")).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void conTokenValidoPopulaElIdUsuarioYSigueLaCadena() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenValido("id-de-usuario-123"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filtro().doFilterInternal(request, response, chain);

        assertThat(request.getAttribute("idUsuario")).isEqualTo("id-de-usuario-123");
        verify(chain).doFilter(request, response);
    }

    @Test
    void conTokenInvalidoNoRechazaSoloSigueSinIdentidad() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-invalido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filtro().doFilterInternal(request, response, chain);

        assertThat(request.getAttribute("idUsuario")).isNull();
        assertThat(response.getStatus()).isEqualTo(200); // nunca lo marca como rechazado
        verify(chain).doFilter(request, response);
    }
}
