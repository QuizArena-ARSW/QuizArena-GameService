package com.quizarena.juego.modelo;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre la logica de reconexion (recargar la pagina no debe duplicar al
 * jugador ni perder su puntaje) y el control de capacidad de la sala.
 */
class SalaTest {

    private Sala salaDeUnCupo() {
        Pregunta p = new Pregunta("2+2", List.of(new Opcion("4", true), new Opcion("5", false)), 20);
        return new Sala("ABC123", List.of(p), 2, "banco-1", "Matematicas", UUID.randomUUID());
    }

    @Test
    void agregarJugadorNuevoAutenticadoLoCreaConectado() {
        Sala sala = salaDeUnCupo();

        Jugador jugador = sala.agregarJugador("Juan", "id-usuario-1");

        assertThat(sala.getJugadores()).hasSize(1);
        assertThat(jugador.isConectado()).isTrue();
        assertThat(jugador.getIdUsuario()).isEqualTo("id-usuario-1");
    }

    @Test
    void agregarJugadorConElMismoIdUsuarioReconectaEnVezDeDuplicar() {
        Sala sala = salaDeUnCupo();
        Jugador original = sala.agregarJugador("Juan", "id-usuario-1");
        original.sumarPuntaje(500);
        original.marcarDesconectado();

        Jugador reconectado = sala.agregarJugador("Juan (reconectando)", "id-usuario-1");

        assertThat(sala.getJugadores()).hasSize(1); // no se duplico
        assertThat(reconectado.getId()).isEqualTo(original.getId());
        assertThat(reconectado.getPuntajeTotal()).isEqualTo(500); // conserva el puntaje
        assertThat(reconectado.isConectado()).isTrue();
    }

    @Test
    void agregarJugadorAnonimoConElMismoApodoReconectaEnVezDeDuplicar() {
        Sala sala = salaDeUnCupo();
        Jugador original = sala.agregarJugador("Juan", null); // sin cuenta (invitado)

        Jugador reconectado = sala.agregarJugador("Juan", null);

        assertThat(sala.getJugadores()).hasSize(1);
        assertThat(reconectado.getId()).isEqualTo(original.getId());
    }

    @Test
    void unJugadorAnonimoNoSeConfundeConUnoAutenticadoAunqueCompartanApodo() {
        Sala sala = new Sala("ABC123", List.of(), 10, "banco-1", "Materia", UUID.randomUUID());
        Jugador autenticado = sala.agregarJugador("Juan", "id-usuario-1");

        Jugador anonimo = sala.agregarJugador("Juan", null);

        assertThat(sala.getJugadores()).hasSize(2);
        assertThat(anonimo.getId()).isNotEqualTo(autenticado.getId());
    }

    @Test
    void estaLlenaCuandoSeAlcanzaElMaximoDeJugadores() {
        Sala sala = salaDeUnCupo(); // maxJugadores = 2
        assertThat(sala.estaLlena()).isFalse();

        sala.agregarJugador("Juan", null);
        assertThat(sala.estaLlena()).isFalse();

        sala.agregarJugador("Ana", null);
        assertThat(sala.estaLlena()).isTrue();
    }

    @Test
    void removerJugadorLoQuitaDeLaSala() {
        Sala sala = salaDeUnCupo();
        Jugador jugador = sala.agregarJugador("Juan", null);

        sala.removerJugador(jugador.getId());

        assertThat(sala.getJugadores()).isEmpty();
    }

    @Test
    void getTotalRondasReflejaLaCantidadDePreguntas() {
        Sala sala = salaDeUnCupo();
        assertThat(sala.getTotalRondas()).isEqualTo(1);
    }

    @Test
    void getPreguntaActualEsNulaAntesDeIniciarLaPartida() {
        Sala sala = salaDeUnCupo();
        assertThat(sala.getPreguntaActual()).isNull();
    }
}
