# QNHRM - Railway production container
FROM maven:3.9.16-eclipse-temurin-17 AS builder

WORKDIR /build

# Cache dependencies before copying the full source tree.
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src

# Build the executable Spring Boot JAR.
RUN mvn -B -DskipTests clean package

# Small production runtime image.
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /build/target/qnhrm-0.0.1-SNAPSHOT.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-XX:+ExitOnOutOfMemoryError"

# Railway provides PORT; Spring Boot reads it from application.properties.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
