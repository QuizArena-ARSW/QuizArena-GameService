package com.quizarena.juego.controlador;

import com.quizarena.juego.modelo.InvitacionSala;
import com.quizarena.juego.modelo.Opcion;
import com.quizarena.juego.modelo.Pregunta;
import com.quizarena.juego.modelo.Sala;
import com.quizarena.juego.servicio.GestorSalas;
import com.quizarena.juego.servicio.RepositorioInvitacionesRedis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitacionSalaControllerTest {

    @Mock GestorSalas gestorSalas;
    @Mock RepositorioInvitacionesRedis repositorioInvitaciones;
    InvitacionSalaController controller;

    UUID creador = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new InvitacionSalaController(gestorSalas, repositorioInvitaciones);
    }

    private Sala salaDe(UUID idCreador) {
        Pregunta p = new Pregunta("2+2", List.of(new Opcion("4", true)), 20);
        return new Sala("ABC123", List.of(p), 10, "banco-1", "Matemáticas", idCreador);
    }

    private MockHttpServletRequest requestConUsuario(UUID idUsuario) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        if (idUsuario != null) req.setAttribute("idUsuario", idUsuario.toString());
        return req;
    }

    @Test
    void invitarSinSesionDevuelve401() {
        ResponseEntity<?> resp = controller.invitar("ABC123", Map.of("idAmigo", "x"), requestConUsuario(null));

        assertThat(resp.getStatusCode().value()).isEqualTo(401);
        verifyNoInteractions(repositorioInvitaciones);
    }

    @Test
    void invitarSinIdAmigoDevuelve400() {
        ResponseEntity<?> resp = controller.invitar("ABC123", Map.of(), requestConUsuario(creador));

        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void invitarASalaInexistenteDevuelve404() {
        when(gestorSalas.buscarSala("ABC123")).thenReturn(null);

        ResponseEntity<?> resp = controller.invitar("ABC123", Map.of("idAmigo", "amigo-1"), requestConUsuario(creador));

        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void soloElCreadorDeLaSalaPuedeInvitar() {
        UUID otroUsuario = UUID.randomUUID();
        when(gestorSalas.buscarSala("ABC123")).thenReturn(salaDe(creador));

        ResponseEntity<?> resp = controller.invitar("ABC123", Map.of("idAmigo", "amigo-1"), requestConUsuario(otroUsuario));

        assertThat(resp.getStatusCode().value()).isEqualTo(403);
        verifyNoInteractions(repositorioInvitaciones);
    }

    @Test
    void unaSalaSinCreadorNoAdmiteInvitaciones() {
        when(gestorSalas.buscarSala("ABC123")).thenReturn(salaDe(null));

        ResponseEntity<?> resp = controller.invitar("ABC123", Map.of("idAmigo", "amigo-1"), requestConUsuario(creador));

        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void elCreadorPuedeInvitarYQuedaGuardadaLaInvitacion() {
        when(gestorSalas.buscarSala("ABC123")).thenReturn(salaDe(creador));

        ResponseEntity<?> resp = controller.invitar("ABC123",
                Map.of("idAmigo", "amigo-1", "nombreInvitador", "Juan"), requestConUsuario(creador));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        ArgumentCaptor<InvitacionSala> captor = ArgumentCaptor.forClass(InvitacionSala.class);
        verify(repositorioInvitaciones).agregar(eq("amigo-1"), captor.capture());
        assertThat(captor.getValue().getCodigo()).isEqualTo("ABC123");
        assertThat(captor.getValue().getNombreInvitador()).isEqualTo("Juan");
    }

    @Test
    void invitarUsaUnNombrePorDefectoSiNoSeEnvia() {
        when(gestorSalas.buscarSala("ABC123")).thenReturn(salaDe(creador));

        controller.invitar("ABC123", Map.of("idAmigo", "amigo-1"), requestConUsuario(creador));

        ArgumentCaptor<InvitacionSala> captor = ArgumentCaptor.forClass(InvitacionSala.class);
        verify(repositorioInvitaciones).agregar(any(), captor.capture());
        assertThat(captor.getValue().getNombreInvitador()).isEqualTo("Un amigo");
    }

    @Test
    void misInvitacionesSinSesionDevuelve401() {
        ResponseEntity<?> resp = controller.misInvitaciones(requestConUsuario(null));

        assertThat(resp.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void misInvitacionesDevuelveLasDelUsuario() {
        InvitacionSala inv = new InvitacionSala("ABC123", "Matemáticas", "Juan", System.currentTimeMillis());
        when(repositorioInvitaciones.listar(creador.toString())).thenReturn(List.of(inv));

        ResponseEntity<?> resp = controller.misInvitaciones(requestConUsuario(creador));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat((List<?>) resp.getBody()).hasSize(1);
    }
}
