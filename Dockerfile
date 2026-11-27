# Etapa 1: Compilación usando OpenJDK 24
FROM eclipse-temurin:24-jdk-alpine AS build
WORKDIR /app

# Copiar wrapper y archivos de configuración de Gradle
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Dar permisos de ejecución
RUN chmod +x ./gradlew

# Copiar el código fuente completo
COPY src src

# Compilar empaquetando el JAR final (excluyendo tests para acelerar el deploy)
RUN ./gradlew bootJar --no-daemon -x test

# Etapa 2: Imagen final para ejecución en producción
FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

# Copiar el archivo JAR generado
COPY --from=build /app/build/libs/*.jar app.jar

# Exponer el puerto
EXPOSE 8080

# Iniciar la aplicación enlazando la variable dinámica $PORT de Render. La zona horaria define el "hoy" del
# dashboard y de los reportes; por defecto es la de Lima (se cambia con QLIC_TIMEZONE)
ENTRYPOINT ["sh", "-c", "java -Duser.timezone=${QLIC_TIMEZONE:-America/Lima} -Dserver.port=${PORT:-8080} -jar app.jar"]