# QuizArena · Servicio de Juego en tiempo real

Esqueleto andante (Fase 1) del microservicio core de QuizArena.
Implementa el flujo de tiempo real de extremo a extremo: crear sala → unirse →
recibir preguntas sincronizadas → responder → ver el marcador en vivo.

> En esta fase el estado vive **en memoria** (`ConcurrentHashMap`) y las preguntas
> son de ejemplo (quemadas). Eso es intencional: el objetivo es probar lo más
> riesgoso (el tiempo real con WebSocket/STOMP) antes que nada.

---

## Requisitos previos

- **Java 17** o superior (`java -version` para comprobar)
- **Maven** (o usa el wrapper de tu IDE)
- **Docker Desktop** (para PostgreSQL y Redis)
- **IntelliJ IDEA** (recomendado)

---

## Fase 0 — Preparar el entorno

1. **Levanta PostgreSQL y Redis** con Docker (desde la carpeta del proyecto):

   ```bash
   docker compose up -d
   ```

   Comprueba que están corriendo:

   ```bash
   docker ps
   ```

   Deberías ver `quizarena-postgres-juego` y `quizarena-redis`.

   > En la Fase 1 todavía no usamos la base de datos ni Redis; los dejamos
   > listos para las fases siguientes. El servicio arranca igual sin ellos.

2. **Abre el proyecto en IntelliJ**: File → Open → selecciona la carpeta
   `servicio-juego`. IntelliJ detectará el `pom.xml` y descargará las
   dependencias automáticamente (necesitas conexión a internet la primera vez).

---

## Fase 1 — Ejecutar el esqueleto andante

1. **Arranca el servicio.** Dos opciones:

   - Desde IntelliJ: abre `ServicioJuegoApplication.java` y pulsa ▶ (Run).
   - Desde la terminal:

     ```bash
     mvn spring-boot:run
     ```

   El servicio queda escuchando en **http://localhost:8081**.

2. **Abre el cliente de prueba** en el navegador:

   ```
   http://localhost:8081/test.html
   ```

3. **Simula una partida con varios jugadores:**

   - En la **pestaña 1**: pulsa *Crear sala*. Se genera un código (ej. `ABC123`).
     Escribe un apodo y pulsa *Unirme*.
   - Abre **2 o 3 pestañas más** con la misma URL. En cada una, escribe el
     **mismo código** y un apodo distinto, y pulsa *Unirme*.
   - Verás que la **lista de jugadores se actualiza en vivo** en todas las
     pestañas (eso es el tiempo real funcionando).
   - En cualquier pestaña, pulsa *Iniciar partida*. La **misma pregunta aparece
     simultáneamente** en todas.
   - Cada jugador responde haciendo clic en una opción. El **marcador se
     actualiza en vivo** para todos, ponderando acierto y rapidez.
   - Pulsa *Siguiente pregunta* para avanzar. Al terminar las 3 preguntas,
     se muestra el ranking final.

Si ves todo esto funcionando, **ya probaste lo más difícil del proyecto**. ✅

---

## ¿Qué hace cada parte?

```
src/main/java/com/quizarena/juego/
├── ServicioJuegoApplication.java      # arranque
├── config/
│   └── WebSocketConfig.java           # configura WebSocket + STOMP
├── modelo/                            # entidades del dominio
│   ├── Sala, Jugador, Pregunta, Opcion, EstadoSala
│   └── mensajes/                      # DTOs que viajan por WebSocket (records)
├── servicio/
│   ├── GestorSalas.java               # crea y administra salas (en memoria)
│   └── MotorJuego.java                # lógica autoritativa: puntajes
├── controlador/
│   ├── SalaRestController.java        # REST: crear/consultar sala
│   └── JuegoWebSocketController.java  # tiempo real: unirse, responder, etc.
└── datos/
    └── BancoPreguntasDemo.java        # preguntas de ejemplo (Fase 1)
```

### Los canales STOMP

- Los clientes **se suscriben** a `/topic/sala/{codigo}` para recibir todo lo
  que pasa en la sala (lista de jugadores, preguntas, marcador, fin).
- Los clientes **envían** a `/app/sala/{codigo}/...` (unirse, iniciar, responder,
  siguiente). El servidor procesa y **publica** el resultado en el topic, de modo
  que todos los jugadores lo reciben a la vez.

---

## Próximos pasos (siguientes fases)

- **Fase 2:** reemplazar `BancoPreguntasDemo` por una llamada REST al Servicio de
  Identidad para traer bancos reales; persistir resultados en PostgreSQL.
- **Fase 3:** poner el API Gateway delante y validar JWT.
- **Fase 4:** construir el frontend en React (reemplaza a `test.html`).
- **Fase 5:** mover el estado de las salas a **Redis** para escalar a varias
  instancias; agregar observabilidad (métricas y logs).
- **Fase 6:** desplegar en Azure (Container Apps + Static Web Apps + Cache for Redis).
