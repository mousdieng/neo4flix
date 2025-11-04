# Runtime image for Registry Service
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Neo4flix Team"
LABEL description="Registry Service for Neo4flix Microservices"

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy pre-built JAR from local target folder
COPY target/registry-service-*.jar app.jar

# Expose port
EXPOSE 8761

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8761/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]