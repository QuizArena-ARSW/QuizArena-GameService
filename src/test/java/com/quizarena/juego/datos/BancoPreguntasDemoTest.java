package com.quizarena.juego.datos;

import com.quizarena.juego.modelo.Opcion;
import com.quizarena.juego.modelo.Pregunta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** El banco demo debe quedar siempre jugable: cada pregunta con exactamente una opcion correcta. */
class BancoPreguntasDemoTest {

    @Test
    void devuelveVariasPreguntasBienFormadas() {
        List<Pregunta> preguntas = BancoPreguntasDemo.preguntasEjemplo();

        assertThat(preguntas).isNotEmpty();
        for (Pregunta p : preguntas) {
            assertThat(p.getEnunciado()).isNotBlank();
            assertThat(p.getTiempoLimiteSegundos()).isPositive();
            long correctas = p.getOpciones().stream().filter(Opcion::isEsCorrecta).count();
            assertThat(correctas).isEqualTo(1);
            assertThat(p.getOpciones().size()).isGreaterThanOrEqualTo(2);
        }
    }
}
