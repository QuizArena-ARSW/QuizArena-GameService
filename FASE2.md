# Fase 2 · Conexión completa entre microservicios

Los dos microservicios ya se comunican en AMBOS sentidos:
- **Pieza A:** Juego pide bancos a Identidad (GET /api/bancos/{id}).
- **Pieza B:** Juego guarda resultados en Identidad (POST /api/historial).

## Pieza B — Guardar resultados en el historial

Al terminar una partida, el Servicio de Juego guarda el resultado de cada
jugador CON CUENTA en el historial del Servicio de Identidad.

### Qué cambió
- modelo/Jugador.java — ahora guarda el idUsuario (id real de la cuenta).
- modelo/mensajes/UnirseRequest.java — incluye idUsuario.
- modelo/Sala.java — guarda idBanco y materia (para el registro).
- cliente/RegistroPartidaDTO.java — datos que se envian a Identidad.
- cliente/DatosBanco.java — datos del banco al crear la sala.
- cliente/ClienteIdentidad.java — nuevo metodo guardarResultado(...).
- controlador/JuegoWebSocketController.java — al finalizar, guarda resultados.
- static/test.html — nuevo paso 0: Login (obtiene el idUsuario real).

### Flujo ahora
1. El jugador hace LOGIN en Identidad -> obtiene su idUsuario (dentro del token).
2. Al UNIRSE a la sala, envia apodo + idUsuario.
3. Al TERMINAR la partida, el Servicio de Juego ordena por puntaje y llama a
   POST /api/historial por cada jugador (posicion 1 = primero).
4. El resultado queda en la tabla registro_partida de Identidad.

### Como probarlo
1. Los dos servicios corriendo (Juego 8081, Identidad 8082).
2. Reemplaza el proyecto, reinicia el Servicio de Juego, abre el test.html
   y haz Ctrl+F5.
3. Paso 0 - Login: correo y clave vienen precargados (juan@mail.com / secreta123).
   Pulsa Login. Debe aparecer "OK Juan (id ...)".
4. Crea sala real con tu banco, unete y JUEGA HASTA EL FINAL (pulsa "Siguiente
   pregunta" hasta ver "FIN DE LA PARTIDA").
5. Verifica el historial en PowerShell (reemplaza el id por el tuyo):

   Invoke-RestMethod -Uri "http://localhost:8082/api/historial/TU_ID_USUARIO"

   Debe devolver el registro (puntaje, posicion, materia, fecha).
   Si da 403, permite el GET de /api/historial/** en el SecurityConfig de
   Identidad (igual que hiciste con los bancos), o pasa un token.

### Prueba realista (opcional)
Registra 2-3 cuentas mas en Identidad (correos distintos) y haz login con cada
una en pestanas distintas. Asi cada jugador tendra su propio registro.

## Con esto la Fase 2 esta COMPLETA
Siguiente: Fase 3 (API Gateway) o Sprint 2 (frontend en React).
