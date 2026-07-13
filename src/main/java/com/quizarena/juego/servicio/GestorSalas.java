package com.quizarena.juego.servicio;

import com.quizarena.juego.cliente.ClienteIdentidad;
import com.quizarena.juego.cliente.DatosBanco;
import com.quizarena.juego.datos.BancoPreguntasDemo;
import com.quizarena.juego.modelo.Sala;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.function.Consumer;

/**
 * Administra las salas. En la Fase 5 el estado pasa a vivir en REDIS.
 *
 * Patron clave: para MODIFICAR una sala se usa `modificar(...)`, que hace
 * leer -> cambiar -> guardar bajo un BLOQUEO DISTRIBUIDO. Asi ninguna otra
 * instancia puede tocar esa sala al mismo tiempo, evitando condiciones de
 * carrera en el puntaje.
 */
@Service
public class GestorSalas {

    private static final String CARACTERES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int LONGITUD_CODIGO = 6;
    private static final int MAX_JUGADORES = 40;

    private final SecureRandom random = new SecureRandom();
    private final ClienteIdentidad clienteIdentidad;
    private final RepositorioSalasRedis repositorio;
    private final BloqueoDistribuido bloqueo;

    public GestorSalas(ClienteIdentidad clienteIdentidad,
                       RepositorioSalasRedis repositorio,
                       BloqueoDistribuido bloqueo) {
        this.clienteIdentidad = clienteIdentidad;
        this.repositorio = repositorio;
        this.bloqueo = bloqueo;
    }

    /** Crea una sala con las preguntas reales del banco (las pide a Identidad). */
    public Sala crearSala(String idBanco) {
        DatosBanco datos = clienteIdentidad.obtenerDatosBanco(idBanco);
        String codigo = generarCodigoUnico();
        Sala sala = new Sala(codigo, datos.preguntas(), MAX_JUGADORES,
                datos.idBanco(), datos.materia());
        repositorio.guardar(sala);
        return sala;
    }

    /** Crea una sala con preguntas de ejemplo (sin depender de Identidad). */
    public Sala crearSalaDemo() {
        String codigo = generarCodigoUnico();
        Sala sala = new Sala(codigo, BancoPreguntasDemo.preguntasEjemplo(), MAX_JUGADORES,
                null, "Demo");
        repositorio.guardar(sala);
        return sala;
    }

    /** Lee una sala (sin bloquear). Devuelve null si no existe. */
    public Sala buscarSala(String codigo) {
        return repositorio.buscar(codigo);
    }

    /**
     * Modifica una sala de forma SEGURA entre instancias:
     * toma el bloqueo, lee el estado fresco de Redis, aplica el cambio y guarda.
     */
    public Sala modificar(String codigo, Consumer<Sala> cambio) {
        return bloqueo.conBloqueo("sala:" + codigo, () -> {
            Sala sala = repositorio.buscar(codigo);
            if (sala == null) return null;
            cambio.accept(sala);
            repositorio.guardar(sala);
            return sala;
        });
    }

    public void cerrarSala(String codigo) {
        repositorio.eliminar(codigo);
    }

    public long salasActivas() {
        return repositorio.contarActivas();
    }

    private String generarCodigoUnico() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder(LONGITUD_CODIGO);
            for (int i = 0; i < LONGITUD_CODIGO; i++) {
                sb.append(CARACTERES.charAt(random.nextInt(CARACTERES.length())));
            }
            codigo = sb.toString();
        } while (repositorio.existe(codigo));
        return codigo;
    }
}
