# ==============================================================================
# CampusFind Backend - Production Multi-Stage Dockerfile
# Base: Eclipse Temurin OpenJDK 21 on Alpine Linux
# ==============================================================================

# -------------------------------------------------------------
# Stage 1: Build Spring Boot JAR with Maven
# -------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and project descriptor for caching
COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw ./
RUN chmod +x ./mvnw

# Pre-fetch dependencies
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and embedded static assets
COPY src ./src

# Build production JAR (skipping tests; tests executed in CI workflow)
RUN ./mvnw clean package -DskipTests -B

# -------------------------------------------------------------
# Stage 2: Minimal Production JRE Runtime
# -------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner

# Create secure non-root user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy executable jar from builder stage
COPY --from=builder /build/target/campusfind-backend-*.jar app.jar

# Ensure proper permissions
RUN chown -R appuser:appgroup /app

USER appuser

# Production environment configurations:
# - Uses host-provided $PORT (e.g. Render, Railway)
# - Container memory awareness with 75% max RAM limit
ENV PORT=8081 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

EXPOSE ${PORT}

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:${PORT}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
