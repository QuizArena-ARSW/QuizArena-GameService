package com.quizarena.juego.eventos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Publica los eventos en el canal de pub/sub de Redis para que todas las
 * instancias del servicio los reenvien a sus clientes STOMP. Se usa un
 * ObjectMapper real (para verificar el JSON de verdad) y se mockea solo
 * StringRedisTemplate (no hay Redis levantado en esta prueba).
 */
@ExtendWith(MockitoExtension.class)
class PublicadorEventosTest {

    @Mock StringRedisTemplate redis;
    ObjectMapper mapper;
    PublicadorEventos publicador;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        publicador = new PublicadorEventos(redis, mapper);
    }

    private record CuerpoDePrueba(String mensaje) {}

    @Test
    void publicarEnviaElCuerpoComoJsonAlCanalDeRedis() throws Exception {
        publicador.publicar("/topic/sala/ABC123", new CuerpoDePrueba("hola"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redis).convertAndSend(eq(PublicadorEventos.CANAL), captor.capture());

        JsonNode evento = mapper.readTree(captor.getValue());
        assertThat(evento.get("destino").asText()).isEqualTo("/topic/sala/ABC123");
        assertThat(evento.get("esJson").asBoolean()).isTrue();
        JsonNode cuerpo = mapper.readTree(evento.get("cuerpo").asText());
        assertThat(cuerpo.get("mensaje").asText()).isEqualTo("hola");
    }

    @Test
    void publicarTextoEnviaElTextoPlanoSinSerializarloComoJson() throws Exception {
        publicador.publicarTexto("/topic/sala/ABC123/jugador/Juan", "id-jugador-1");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redis).convertAndSend(eq(PublicadorEventos.CANAL), captor.capture());

        JsonNode evento = mapper.readTree(captor.getValue());
        assertThat(evento.get("esJson").asBoolean()).isFalse();
        assertThat(evento.get("cuerpo").asText()).isEqualTo("id-jugador-1");
    }
}
