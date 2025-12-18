# Multi-stage build for OpenLedger Backend

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-20 AS builder

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:20-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 openledger && \
    adduser -u 1001 -G openledger -s /bin/sh -D openledger

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown -R openledger:openledger /app

# Switch to non-root user
USER openledger

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
