# Runtime image for Rating Service
FROM eclipse-temurin:17-jre-alpine

# Create app directory
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy pre-built JAR from local target folder
COPY target/rating-service-*.jar app.jar

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Expose port
EXPOSE 9082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:9082/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]