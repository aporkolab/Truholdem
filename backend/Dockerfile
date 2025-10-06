FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests -Dspring-boot.repackage.excludeDevtools=true

# Rename WAR to a known name (Spring Boot creates truholdem-VERSION.war)
RUN cp /app/target/truholdem-*.war /app/target/app.war && \
    ls -la /app/target/app.war

# Production stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN addgroup --system truholdem && adduser --system --group truholdem

# Copy the war file from builder stage
COPY --from=builder /app/target/app.war app.war

# Create logs directory
RUN mkdir -p /app/logs && chown -R truholdem:truholdem /app

# Switch to non-root user
USER truholdem

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD curl -sf http://localhost:8080/api/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.war"]
