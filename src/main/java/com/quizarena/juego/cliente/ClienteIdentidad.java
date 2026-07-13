package com.quizarena.juego.cliente;

import com.quizarena.juego.modelo.Opcion;
import com.quizarena.juego.modelo.Pregunta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Cliente REST hacia el Servicio de Identidad.
 *
 * Punto de comunicacion entre microservicios: NO tocamos la base de datos de
 * Identidad, solo consumimos su API.
 *  - obtenerDatosBanco: pide un banco (GET /api/bancos/{id})
 *  - guardarResultado:  registra el resultado de un jugador (POST /api/historial)
 */
@Component
public class ClienteIdentidad {

    private final RestClient restClient;
    private final String urlIdentidad;

    public ClienteIdentidad(RestClient restClient,
                            @Value("${quizarena.identidad.url}") String urlIdentidad) {
        this.restClient = restClient;
        this.urlIdentidad = urlIdentidad;
    }

    /** Pide un banco a Identidad y devuelve sus datos + preguntas convertidas. */
    public DatosBanco obtenerDatosBanco(String idBanco) {
        BancoIdentidadDTO banco = restClient.get()
                .uri(urlIdentidad + "/api/bancos/{id}", idBanco)
                .retrieve()
                .body(BancoIdentidadDTO.class);

        if (banco == null || banco.preguntas() == null || banco.preguntas().isEmpty()) {
            throw new IllegalArgumentException("El banco no existe o no tiene preguntas: " + idBanco);
        }

        List<Pregunta> preguntas = banco.preguntas().stream()
                .map(this::convertirPregunta)
                .toList();

        return new DatosBanco(banco.id(), banco.materia(), preguntas);
    }

    /** Guarda el resultado de un jugador en el historial de Identidad. */
    public void guardarResultado(RegistroPartidaDTO registro) {
        restClient.post()
                .uri(urlIdentidad + "/api/historial")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registro)
                .retrieve()
                .toBodilessEntity();
    }

    private Pregunta convertirPregunta(BancoIdentidadDTO.PreguntaIdentidadDTO dto) {
        List<Opcion> opciones = dto.opciones().stream()
                .map(o -> new Opcion(o.texto(), o.esCorrecta()))
                .toList();
        return new Pregunta(dto.enunciado(), opciones, dto.tiempoLimiteSegundos());
    }
}
