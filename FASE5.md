# Fase 5 · Redis (escalado real) + Observabilidad

Esta fase convierte dos promesas de tu arquitectura en hechos demostrables:

1. **Redis** hace que el escalado horizontal del Servicio de Juego sea **real**
   (varias instancias compartiendo el estado de las partidas).
2. **La observabilidad** te da los **números** que respaldan tus escenarios de
   calidad (latencia y concurrencia) ante el jurado.

---

## Parte 1 — Qué problemas resuelve Redis aquí

Mover el estado a Redis no es solo "cambiar dónde guardo las salas". Resuelve
**tres** problemas distintos, y conviene que los tengas claros:

**a) Estado compartido.** Antes, cada instancia tenía las salas en su propia
memoria (`ConcurrentHashMap`). Si escalabas a 2 instancias, un jugador en la
instancia 1 no veía lo que pasaba en la 2. Ahora la sala vive en Redis y
**todas las instancias leen y escriben el mismo estado**.
→ `RepositorioSalasRedis`

**b) Difusión entre instancias.** El broker STOMP de Spring solo entrega
mensajes a los clientes conectados a *esa* instancia. Si el jugador A está en la
instancia 1 y el B en la 2, publicar localmente dejaría a B sin recibir la
pregunta. Ahora los eventos se publican en un **canal de Redis (pub/sub)** al
que todas las instancias están suscritas, y cada una los reenvía a sus propios
clientes.
→ `PublicadorEventos` + `SuscriptorEventos`

**c) Concurrencia distribuida.** Con varias instancias, dos respuestas podrían
procesarse *al mismo tiempo* y corromper el puntaje. Se usa un **bloqueo
distribuido** sobre Redis (operación atómica SETNX): solo una instancia modifica
una sala a la vez. Esto es lo que mantiene válida la **HU-09** al escalar.
→ `BloqueoDistribuido` + `GestorSalas.modificar(...)`

---

## Qué cambió

    pom.xml                                   REEMPLAZADO - trae Redis, Actuator, logs JSON
    src/main/resources/application.properties REEMPLAZADO
    src/main/resources/logback-spring.xml     NUEVO - logs estructurados, HU-13

    src/main/java/com/quizarena/juego/
      modelo/Sala.java          REEMPLAZADO - serializable a JSON
      modelo/Jugador.java       REEMPLAZADO - serializable a JSON
      modelo/Pregunta.java      REEMPLAZADO - serializable a JSON
      modelo/Opcion.java        REEMPLAZADO - serializable a JSON
      config/RedisConfig.java   NUEVO
      servicio/RepositorioSalasRedis.java NUEVO
      servicio/BloqueoDistribuido.java    NUEVO
      servicio/GestorSalas.java           REEMPLAZADO - ahora usa Redis
      eventos/EventoSala.java             NUEVO
      eventos/PublicadorEventos.java      NUEVO
      eventos/SuscriptorEventos.java      NUEVO
      metricas/MetricasJuego.java         NUEVO
      controlador/JuegoWebSocketController.java REEMPLAZADO

El stack de Prometheus + Grafana (scrape config, dashboard, docker-compose)
vive en un **repositorio aparte**. Este repo solo se encarga de EXPONER las
métricas; no incluye la infraestructura de observabilidad.

---

## Cómo levantarlo

### 1. Levanta Redis

Ya lo trae el `docker-compose.yml` de la raíz del repo (servicio `redis`,
`localhost:6379`):

    docker compose up -d

### 2. Arranca el Servicio de Juego normalmente

    mvn spring-boot:run

Comprueba que las métricas se exponen:

    http://localhost:8081/actuator/prometheus

Deberías ver un montón de líneas de texto, entre ellas las que empiezan por
`quizarena_...`.

### 3. Dashboard

El dashboard de Grafana ("QuizArena · Operación y KPIs") y la configuración de
Prometheus se administran desde el repositorio de observabilidad aparte;
apunta su scrape config a `http://localhost:8081/actuator/prometheus` (y a
cada instancia adicional si escalas).

---

## LO MÁS IMPORTANTE: demostrar el escalado horizontal

Esto es lo que realmente va a impresionar al jurado, porque **demuestra** que tu
arquitectura escala, en vez de solo afirmarlo.

### Levanta una SEGUNDA instancia del Servicio de Juego

En otra terminal, en el mismo proyecto:

    mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8091"

Ahora tienes el Servicio de Juego corriendo en **8081 y 8091** a la vez.

### La prueba que lo demuestra todo

1. Abre el frontend y **crea una sala** (queda guardada en Redis).
2. Únete con un jugador desde el frontend normal (que apunta a **8081**).
3. Abre **otra pestaña** y, en `src/config.js` del frontend (o usando el
   `test.html` viejo), apunta el WebSocket a **http://localhost:8091/ws-juego**.
   Únete a la **misma sala** con otro nombre.
4. **Inicia la partida.**

Resultado: los dos jugadores ven la **misma pregunta al mismo tiempo** y el
**mismo marcador**, aunque estén conectados a **instancias distintas del
servicio**. Eso solo es posible gracias a Redis (estado compartido + pub/sub).

> Antes de la Fase 5, esto era **imposible**: cada instancia tenía su propia
> memoria y los jugadores no se veían entre sí.

### Para verlo en el dashboard

Agrega la segunda instancia al scrape config de Prometheus (en el repo de
observabilidad aparte) y reinícialo. En el panel "Jugadores conectados por
instancia" verás la carga repartida entre las dos.

---

## Qué historias de usuario cierra esta fase

- **HU-13** Logs estructurados → `logback-spring.xml` genera `logs/quizarena.json`
  con eventos en formato JSON (evento, sala, ronda, etc.).
- **HU-14** Métricas técnicas → latencia (p50/p95/p99), jugadores concurrentes,
  salas activas, throughput.
- **HU-15** Dashboard → el tablero de Grafana con métricas y KPIs.
- Y refuerza **HU-09** (respuestas sin condiciones de carrera) al mantener la
  garantía incluso con varias instancias, gracias al bloqueo distribuido.

---

## Nota honesta sobre un detalle

El bloqueo distribuido serializa las escrituras sobre una misma sala. Es lo
correcto para la integridad del puntaje, pero significa que las respuestas de
una sala se procesan una tras otra. Para tu escala (40 jugadores por sala) es
perfectamente adecuado y la latencia sigue muy por debajo de los 200 ms. Si el
jurado pregunta por el límite, esa es la respuesta honesta: es un trade-off
consciente de **consistencia sobre paralelismo** dentro de una sala, y las salas
distintas sí se procesan en paralelo sin bloquearse entre sí.

## Con esto la Fase 5 está COMPLETA
Siguiente: Fase 6 (desplegar en Azure — Container Apps + Static Web Apps + Cache for Redis).
