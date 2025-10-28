# Multi-stage build for Neo4flix User Service
FROM maven:latest AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Create user for security
RUN addgroup --system --gid 1001 neo4flix && \
    adduser --system --uid 1001 --gid 1001 neo4flix

# Copy built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership
RUN chown -R neo4flix:neo4flix /app

# Switch to non-root user
USER neo4flix

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]