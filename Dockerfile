# ==========================================
# Etapa 1: Build con Java 25 y Maven
# ==========================================
FROM eclipse-temurin:25-jdk AS builder

# Copiar Maven directamente desde la imagen oficial
COPY --from=maven:3.9.9-eclipse-temurin-21 /usr/share/maven /usr/share/maven
ENV PATH="/usr/share/maven/bin:${PATH}"

WORKDIR /build

# Copiar configuración de dependencias primero para aprovechar la caché de capas Docker
COPY pom.xml .

# Descargar dependencias para cachear capas si pom.xml no cambia
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Compilar empaquetando el JAR omitiendo tests usando caché de Maven
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -B

# ==========================================
# Etapa 2: Imagen final de ejecución
# ==========================================
FROM eclipse-temurin:25-jre

WORKDIR /app

# Crear usuario sin privilegios por seguridad
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copiar el JAR generado desde la etapa de construcción
COPY --from=builder /build/target/*.jar app.jar

# Puerto configurado por defecto en application.yml (49180)
ENV PORT=49180
EXPOSE 49180

# Variables de entorno JVM y optimizaciones para contenedor
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]

