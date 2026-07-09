FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21.0.11_10-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar
USER spring
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
