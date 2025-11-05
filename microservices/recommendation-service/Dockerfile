FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy pre-built JAR from local target folder
COPY target/*.jar app.jar

# Create a non-root user
RUN addgroup -S appuser && adduser -S appuser -G appuser
RUN chown -R appuser:appuser /app
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:9083/actuator/health || exit 1

EXPOSE 9083

ENTRYPOINT ["java", "-jar", "app.jar"]