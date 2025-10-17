# Multi-stage build for optimal image size

# Stage 1: Build frontend
FROM node:20-alpine AS frontend-builder

WORKDIR /app/frontend

# Copy frontend package files
COPY frontend/package*.json ./
RUN npm ci --only=production

# Copy frontend source
COPY frontend/ ./

# Build frontend
RUN npm run build:prod

# Stage 2: Build backend
FROM maven:3.9.4-eclipse-temurin-21-alpine AS backend-builder

WORKDIR /app/backend

# Copy backend pom.xml and download dependencies (for better caching)
COPY backend/pom.xml ./
RUN mvn dependency:go-offline -B

# Copy backend source
COPY backend/src ./src

# Copy frontend build to backend static resources
COPY --from=frontend-builder /app/frontend/dist/texas-holdem-frontend/ ./src/main/resources/static/

# Build backend
RUN mvn clean package -DskipTests

# Stage 3: Runtime image
FROM eclipse-temurin:21-jre-alpine

# Add system packages for observability
RUN apk --no-cache add curl

# Create non-root user for security
RUN addgroup -g 1001 -S truholdem && \
    adduser -S truholdem -u 1001 -G truholdem

WORKDIR /app

# Copy the built JAR from backend builder stage
COPY --from=backend-builder /app/backend/target/truholdem-*.jar app.jar

# Copy application configuration
COPY backend/src/main/resources/application*.yml ./

# Change ownership to non-root user
RUN chown -R truholdem:truholdem /app

# Switch to non-root user
USER truholdem

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=production"

# Run the application
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
