# ==========================================================
# Dockerfile para los microservicios Spring Boot de QuizArena
# (sirve igual para Juego, Identidad y Gateway)
#
# MULTI-ETAPA: primero compila con Maven, luego copia solo el .jar a una
# imagen ligera. Asi la imagen final no lleva Maven ni el codigo fuente.
# ==========================================================

# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Primero solo el pom: aprovecha la cache de Docker.
# Si el pom no cambia, no vuelve a descargar las dependencias.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuario sin privilegios (buena practica de seguridad)
RUN addgroup -S quizarena && adduser -S quizarena -G quizarena

# logback-spring.xml escribe en logs/quizarena.json: el directorio debe existir
# y pertenecer al usuario sin privilegios antes de arrancar la app.
RUN mkdir -p /app/logs && chown -R quizarena:quizarena /app

USER quizarena

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
