# Multi-stage build for OpenLedger API
FROM maven:3.9-eclipse-temurin-21 AS build

# Copy source code
COPY pom.xml .
COPY openledger-common ./openledger-common
COPY openledger-integration ./openledger-integration
COPY openledger-api ./openledger-api

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

# Create app user
RUN addgroup --system openledger && adduser --system --group openledger

# Copy the built JAR
COPY --from=build openledger-api/target/openledger-api-*.jar /app/openledger-api.jar

# Change ownership
RUN chown -R openledger:openledger /app

USER openledger

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/openledger-api.jar"]
