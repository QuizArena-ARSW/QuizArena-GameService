package com.quizarena.juego.servicio;

import com.quizarena.juego.cliente.ClienteIdentidad;
import com.quizarena.juego.cliente.DatosBanco;
import com.quizarena.juego.modelo.Opcion;
import com.quizarena.juego.modelo.Pregunta;
import com.quizarena.juego.modelo.Sala;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestorSalasTest {

    @Mock ClienteIdentidad clienteIdentidad;
    @Mock RepositorioSalasRedis repositorio;
    @Mock BloqueoDistribuido bloqueo;

    GestorSalas gestorSalas;

    @BeforeEach
    void setUp() {
        gestorSalas = new GestorSalas(clienteIdentidad, repositorio, bloqueo);
        lenient().when(bloqueo.conBloqueo(anyString(), any())).thenAnswer(inv -> {
            Supplier<?> accion = inv.getArgument(1);
            return accion.get();
        });
    }

    private DatosBanco datosDeEjemplo() {
        Pregunta p = new Pregunta("2+2", List.of(new Opcion("4", true)), 20);
        return new DatosBanco("banco-1", "Matemáticas", List.of(p));
    }

    @Test
    void crearSalaPideElBancoAIdentidadYLaGuarda() {
        when(clienteIdentidad.obtenerDatosBanco("banco-1")).thenReturn(datosDeEjemplo());
        when(repositorio.existe(anyString())).thenReturn(false);
        UUID idCreador = UUID.randomUUID();

        Sala sala = gestorSalas.crearSala("banco-1", idCreador);

        assertThat(sala.getCodigo()).hasSize(6);
        assertThat(sala.getMateria()).isEqualTo("Matemáticas");
        assertThat(sala.getIdCreador()).isEqualTo(idCreador);
        verify(repositorio).guardar(sala);
    }

    @Test
    void crearSalaGeneraUnCodigoQueNoChoqueConUnoExistente() {
        when(clienteIdentidad.obtenerDatosBanco("banco-1")).thenReturn(datosDeEjemplo());
        // El primer codigo generado "ya existe"; el segundo, no.
        when(repositorio.existe(anyString())).thenReturn(true, false);

        Sala sala = gestorSalas.crearSala("banco-1", UUID.randomUUID());

        assertThat(sala.getCodigo()).hasSize(6);
        verify(repositorio, times(2)).existe(anyString());
    }

    @Test
    void crearSalaDemoNoConsultaAIdentidad() {
        when(repositorio.existe(anyString())).thenReturn(false);

        Sala sala = gestorSalas.crearSalaDemo(null);

        assertThat(sala.getMateria()).isEqualTo("Demo");
        assertThat(sala.getPreguntas()).isNotEmpty();
        verifyNoInteractions(clienteIdentidad);
    }

    @Test
    void buscarSalaDelegaEnElRepositorio() {
        Sala sala = new Sala("ABC123", List.of(), 10, null, "Demo", null);
        when(repositorio.buscar("ABC123")).thenReturn(sala);

        assertThat(gestorSalas.buscarSala("ABC123")).isSameAs(sala);
    }

    @Test
    void modificarAplicaElCambioBajoElBloqueoYGuarda() {
        Sala sala = new Sala("ABC123", List.of(), 10, null, "Demo", null);
        when(repositorio.buscar("ABC123")).thenReturn(sala);

        Sala resultado = gestorSalas.modificar("ABC123", Sala::iniciar);

        assertThat(resultado).isSameAs(sala);
        assertThat(sala.getEstado().name()).isEqualTo("EN_CURSO");
        verify(repositorio).guardar(sala);
        verify(bloqueo).conBloqueo(eq("sala:ABC123"), any());
    }

    @Test
    void modificarDevuelveNuloSiLaSalaNoExiste() {
        when(repositorio.buscar("NOEXISTE")).thenReturn(null);

        Sala resultado = gestorSalas.modificar("NOEXISTE", Sala::iniciar);

        assertThat(resultado).isNull();
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void cerrarSalaDelegaEnElRepositorio() {
        gestorSalas.cerrarSala("ABC123");

        verify(repositorio).eliminar("ABC123");
    }

    @Test
    void salasActivasDelegaEnElRepositorio() {
        when(repositorio.contarActivas()).thenReturn(3L);

        assertThat(gestorSalas.salasActivas()).isEqualTo(3L);
    }
}
