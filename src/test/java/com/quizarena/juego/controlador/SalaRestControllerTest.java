package com.quizarena.juego.controlador;

import com.quizarena.juego.modelo.Opcion;
import com.quizarena.juego.modelo.Pregunta;
import com.quizarena.juego.modelo.Sala;
import com.quizarena.juego.servicio.GestorSalas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class SalaRestControllerTest {

    @Mock GestorSalas gestorSalas;
    SalaRestController controller;

    @BeforeEach
    void setUp() {
        controller = new SalaRestController(gestorSalas);
    }

    private Sala salaDeEjemplo() {
        Pregunta p = new Pregunta("2+2", List.of(new Opcion("4", true), new Opcion("5", false)), 20);
        return new Sala("ABC123", List.of(p), 10, "banco-1", "Matematicas", UUID.randomUUID());
    }

    @Test
    void crearSalaDevuelveElCodigoCuandoElBancoEsValido() {
        UUID idUsuario = UUID.randomUUID();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute("idUsuario", idUsuario.toString());
        when(gestorSalas.crearSala(eq("banco-1"), eq(idUsuario))).thenReturn(salaDeEjemplo());

        ResponseEntity<?> resp = controller.crearSala(Map.of("idBanco", "banco-1"), req);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat((Map<String, Object>) resp.getBody()).containsEntry("codigo", "ABC123");
    }

    @Test
    void crearSalaSinUsuarioAutenticadoPasaIdCreadorNulo() {
        MockHttpServletRequest req = new MockHttpServletRequest(); // sin atributo idUsuario (invitado)
        when(gestorSalas.crearSala(eq("banco-1"), isNull())).thenReturn(salaDeEjemplo());

        ResponseEntity<?> resp = controller.crearSala(Map.of("idBanco", "banco-1"), req);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void crearSalaSinIdBancoDaBadRequest() {
        ResponseEntity<?> resp = controller.crearSala(Map.of(), new MockHttpServletRequest());

        assertThat(resp.getStatusCode().value()).isEqualTo(400);
        assertThat((Map<String, Object>) resp.getBody()).containsEntry("error", "Falta el idBanco");
    }

    @Test
    void crearSalaConBancoInexistenteDevuelveElErrorDeIdentidad() {
        when(gestorSalas.crearSala(eq("banco-fantasma"), any()))
                .thenThrow(new RuntimeException("Banco no encontrado"));

        ResponseEntity<?> resp = controller.crearSala(Map.of("idBanco", "banco-fantasma"), new MockHttpServletRequest());

        assertThat(resp.getStatusCode().value()).isEqualTo(400);
        assertThat(((Map<?, ?>) resp.getBody()).get("error").toString()).contains("Banco no encontrado");
    }

    @Test
    void crearSalaDemoNoRequiereBanco() {
        when(gestorSalas.crearSalaDemo(any())).thenReturn(salaDeEjemplo());

        Map<String, Object> resp = controller.crearSalaDemo(new MockHttpServletRequest());

        assertThat(resp).containsEntry("codigo", "ABC123");
    }

    @Test
    void consultarSalaDevuelveSuEstado() {
        when(gestorSalas.buscarSala("ABC123")).thenReturn(salaDeEjemplo());

        Map<String, Object> resp = controller.consultarSala("ABC123");

        assertThat(resp).containsEntry("codigo", "ABC123").containsEntry("jugadores", 0);
    }

    @Test
    void consultarSalaInexistenteDevuelveError() {
        when(gestorSalas.buscarSala("NOEXISTE")).thenReturn(null);

        Map<String, Object> resp = controller.consultarSala("NOEXISTE");

        assertThat(resp).containsEntry("error", "Sala no encontrada");
    }
}
