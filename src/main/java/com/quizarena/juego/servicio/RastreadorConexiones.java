package com.quizarena.juego.servicio;

import com.quizarena.juego.metricas.MetricasJuego;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mantiene "jugadores conectados" como un numero EN VIVO, no un contador que
 * solo sube: registra que sesiones de WebSocket tienen un jugador unido a una
 * sala, y resta cuando esa sesion se desconecta (Spring publica
 * SessionDisconnectEvent automaticamente al cerrarse el socket, sin importar
 * el motivo: cerrar la pestana, perder la conexion, etc.).
 */
@Component
public class RastreadorConexiones {

    private final MetricasJuego metricas;
    private final Set<String> sesionesConJugador = ConcurrentHashMap.newKeySet();

    public RastreadorConexiones(MetricasJuego metricas) {
        this.metricas = metricas;
    }

    /** Se llama cuando un jugador se une a una sala con exito. */
    public void registrarConexion(String idSesionWebSocket) {
        if (idSesionWebSocket != null && sesionesConJugador.add(idSesionWebSocket)) {
            metricas.jugadorEntro();
        }
    }

    @EventListener
    public void alDesconectarse(SessionDisconnectEvent evento) {
        if (sesionesConJugador.remove(evento.getSessionId())) {
            metricas.jugadorSalio();
        }
    }
}
