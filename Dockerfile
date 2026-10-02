# Etapa 1: Compilación del proyecto
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copiar archivos de configuración de Gradle
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Dar permisos de ejecución al wrapper
RUN chmod +x ./gradlew

# Descargar dependencias
RUN ./gradlew dependencies --no-daemon

# Copiar el código fuente y compilar excluyendo tests
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Etapa 2: Imagen final ligera para ejecución
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar el JAR generado desde la etapa de compilación
COPY --from=build /app/build/libs/*.jar app.jar

# Exponer el puerto por defecto de Spring Boot
EXPOSE 8080

# Comando de arranque adaptado a la variable PORT de Render
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]