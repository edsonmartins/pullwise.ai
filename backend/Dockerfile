# Multi-stage build for Pullwise Backend
# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .

# Download dependencies (will be cached if pom.xml doesn't change)
RUN apk add --no-cache curl && \
    curl -o /app/mvnw https://raw.githubusercontent.com/takari/maven-wrapper/mvnw/3.9.9/mvnw && \
    chmod +x /app/mvnw && \
    ./mvnw dependency:go-offline -B

# Copy source and build
COPY src ./src

RUN ./mvnw clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for health check
RUN apk add --no-cache curl

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run application with optimized JVM settings
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dserver.port=8080", \
    "-jar", \
    "app.jar"]

# JVM options can be overridden via JAVA_OPTS
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
