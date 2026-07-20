# QuizArena · Servicio de Juego en tiempo real

Microservicio core de QuizArena: crear sala → unirse → recibir preguntas
sincronizadas → responder → ver el marcador en vivo → resumen final. Además
del juego, concentra el **chat de sala** y las **invitaciones a partida**,
porque son funcionalidades acopladas al ciclo de vida de la sala en tiempo
real (ver "Por qué está todo en un solo servicio" más abajo).

El estado de las salas vive en **Redis**, no en memoria: el servicio puede
correr en varias instancias a la vez compartiendo el mismo estado (probado
con 8081 + 8091 simultáneas — ver `FASE5.md`).

---

## Requisitos previos

- **Java 17** o superior
- **Maven**
- **Docker Desktop** (para PostgreSQL y Redis) o el `docker-compose.full.yml`
  del repo `QuizArena-Infra`
- El **Servicio de Identidad** corriendo (para traer bancos reales y guardar
  el historial)

---

## Puesta en marcha (local, suelto)

1. Levanta PostgreSQL y Redis:

   ```bash
   docker compose up -d
   ```

2. Arranca el servicio (desde IntelliJ ejecuta
   `ServicioJuegoApplication`, o por terminal):

   ```bash
   mvn spring-boot:run
   ```

   Queda escuchando en **http://localhost:8081**.

> Alternativa recomendada: levantar todo el sistema junto con
> `docker compose -f docker-compose.full.yml up --build` desde
> `QuizArena-Infra` — evita tener que arrancar cada pieza a mano.

---

## Qué hace cada parte

```
src/main/java/com/quizarena/juego/
├── ServicioJuegoApplication.java
├── config/
│   └── WebSocketConfig.java            # STOMP sobre WebSocket/SockJS en /ws-juego
├── seguridad/
│   └── FiltroJwtOpcional.java          # valida el JWT si viene, nunca rechaza la peticion
├── modelo/                             # entidades del dominio (serializables a JSON para Redis)
│   ├── Sala, Jugador, Pregunta, Opcion, EstadoSala, InvitacionSala
│   └── mensajes/                       # DTOs que viajan por WebSocket/REST (records)
│       ├── EventoLista, EventoPregunta, EventoMarcador, EventoFin
│       ├── EventoChat, MensajeChatRequest
│       └── RespuestaResumenDTO, SiguienteRequest
├── servicio/
│   ├── GestorSalas.java                # crea/administra salas, con bloqueo distribuido
│   ├── MotorJuego.java                 # logica autoritativa: puntajes, registra respuestas
│   ├── RepositorioSalasRedis.java      # estado de las salas en Redis
│   ├── RepositorioChatRedis.java       # historial de chat por sala en Redis (TTL 6h, tope 50 msj)
│   ├── RepositorioInvitacionesRedis.java  # invitaciones pendientes por usuario (TTL 2h)
│   └── BloqueoDistribuido.java         # SETNX atomico: evita condiciones de carrera entre instancias
├── eventos/
│   ├── PublicadorEventos.java          # publica en el canal pub/sub de Redis
│   └── SuscriptorEventos.java          # reenvia a los clientes STOMP conectados a esta instancia
├── cliente/
│   └── ClienteIdentidad.java           # REST hacia Identidad: trae bancos, guarda historial
├── metricas/
│   └── MetricasJuego.java              # contadores/timers para Prometheus
└── controlador/
    ├── SalaRestController.java         # REST: crear/consultar sala
    ├── InvitacionSalaController.java   # REST: invitar a un amigo a la sala activa
    └── JuegoWebSocketController.java   # tiempo real: unirse, iniciar, responder, siguiente, chat
```

### Los canales STOMP

- Los clientes **se suscriben** a `/topic/sala/{codigo}` para recibir todo lo
  que pasa en la sala (lista de jugadores, preguntas, marcador, fin) y a
  `/topic/sala/{codigo}/chat` para los mensajes de chat.
- Cada jugador tiene además dos sub-topics personales, de entrega **una sola
  vez** al unirse: `/topic/sala/{codigo}/jugador/{apodo}` (su id de jugador)
  y `/topic/sala/{codigo}/jugador/{apodo}/chat-historial` (los últimos
  mensajes de chat, para no perderse la conversación si entra tarde).
- Los clientes **envían** a `/app/sala/{codigo}/...` (`unirse`, `iniciar`,
  `responder`, `siguiente`, `chat`). El servidor procesa y **publica** el
  resultado en el topic correspondiente.

### Por qué chat e invitaciones están en este servicio y no en Identidad

Ambos son **efímeros y ligados a la vida de la sala**: el chat vive en Redis
con TTL de 6h (igual que la sala) y nunca se persiste en Postgres; las
invitaciones expiran a las 2h. Meterlos en Identidad habría significado una
llamada REST cruzada por cada mensaje de chat, además de mezclar datos
permanentes (cuentas) con datos de vida corta. La amistad en sí (permanente)
sí vive en Identidad — solo la invitación puntual a una sala vive aquí.

### Redis: qué problema resuelve cada pieza

Ver `FASE5.md` para el detalle completo (estado compartido, pub/sub entre
instancias, bloqueo distribuido) y cómo levantar dos instancias a la vez
para demostrar el escalado horizontal.

---

## Seguridad

`FiltroJwtOpcional` valida el JWT en cada petición **sin rechazar nunca la
petición** si falta o es inválido: solo popula el id de usuario cuando el
token es válido. Esto es lo que permite saber quién creó una sala (para
poder invitar amigos) sin exigir sesión para jugar como invitado. El
secreto (`JWT_SECRET`) debe ser idéntico al de Identidad y el Gateway, y
**no tiene valor por defecto** — si falta la variable de entorno, el
servicio no arranca (evita validar tokens con una clave pública conocida).

---

## Historias de usuario que cierra este servicio

HU-06 a HU-11 (salas, tiempo real, puntaje, marcador, resumen), HU-13 a
HU-15 (logs, métricas, dashboard) y HU-17 (chat), más las invitaciones a
sala del sistema de amigos. Ver `FASE2.md` y `FASE5.md` para el detalle de
cómo se construyeron la integración con Identidad y el escalado con Redis.
