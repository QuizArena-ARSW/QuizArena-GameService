package com.quizarena.juego.servicio;

import com.quizarena.juego.metricas.MetricasJuego;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * "Jugadores conectados" debe ser un numero EN VIVO: sube cuando alguien se
 * une, baja cuando esa misma sesion de WebSocket se desconecta (sin importar
 * el motivo: Spring publica SessionDisconnectEvent automaticamente).
 */
@ExtendWith(MockitoExtension.class)
class RastreadorConexionesTest {

    @Mock MetricasJuego metricas;
    RastreadorConexiones rastreador;

    @BeforeEach
    void setUp() {
        rastreador = new RastreadorConexiones(metricas);
    }

    private SessionDisconnectEvent desconexionDe(String idSesion) {
        Message<byte[]> mensajeVacio = MessageBuilder.withPayload(new byte[0]).build();
        return new SessionDisconnectEvent(this, mensajeVacio, idSesion, CloseStatus.NORMAL);
    }

    @Test
    void registrarConexionSumaAlContador() {
        rastreador.registrarConexion("sesion-1");

        verify(metricas).jugadorEntro();
    }

    @Test
    void desconectarUnaSesionRegistradaRestaDelContador() {
        rastreador.registrarConexion("sesion-1");

        rastreador.alDesconectarse(desconexionDe("sesion-1"));

        verify(metricas).jugadorSalio();
    }

    @Test
    void desconectarUnaSesionQueNuncaSeUnioNoRestaNada() {
        rastreador.alDesconectarse(desconexionDe("sesion-fantasma"));

        verify(metricas, never()).jugadorSalio();
    }

    @Test
    void desconectarLaMismaSesionDosVecesSoloRestaUnaVez() {
        rastreador.registrarConexion("sesion-1");

        rastreador.alDesconectarse(desconexionDe("sesion-1"));
        rastreador.alDesconectarse(desconexionDe("sesion-1"));

        verify(metricas, org.mockito.Mockito.times(1)).jugadorSalio();
    }

    @Test
    void unirseDosVecesConLaMismaSesionSoloSumaUnaVez() {
        rastreador.registrarConexion("sesion-1");
        rastreador.registrarConexion("sesion-1");

        verify(metricas, org.mockito.Mockito.times(1)).jugadorEntro();
    }
}
