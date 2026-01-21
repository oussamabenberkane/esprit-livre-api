# Multi-stage build for Esprit Livre API
# Stage 1: Build
FROM eclipse-temurin:17-jdk-focal AS builder

WORKDIR /app

# Build arguments
ARG MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
ARG SKIP_TESTS=true

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -Pprod -DskipTests=${SKIP_TESTS} -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-focal

LABEL maintainer="oussamabenberkane.pro@gmail.com"
LABEL description="Esprit Livre API - Book E-commerce Platform"

WORKDIR /app

# Install curl and ca-certificates for health checks and HTTPS
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r espritlivre && useradd -r -g espritlivre espritlivre

# Copy the built JAR from builder stage
COPY --from=builder /app/target/esprit-livre-*.jar app.jar

# Create directories for media files and logs
RUN mkdir -p /app/media /app/logs && chown -R espritlivre:espritlivre /app

# Declare volumes for persistent data
VOLUME ["/app/media", "/app/logs"]

# Switch to non-root user
USER espritlivre

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/management/health/liveness || exit 1

# JVM options optimized for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:+UseG1GC \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=prod"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
