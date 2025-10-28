# Neo4flix - Movie Recommendation Engine

A microservices-based movie recommendation system built with Neo4j, Spring Boot, Angular, and Docker.

## Architecture

- **Database**: Neo4j with Graph Data Science library
- **Backend**: Spring Boot microservices
- **Frontend**: Angular web application
- **Deployment**: Docker containers
- **Authentication**: JWT with OAuth2 support

## Services

1. **Registry Service** (Port 8761) - Eureka service discovery
2. **Movie Service** (Port 8081) - Movie CRUD operations and basic recommendations
3. **User Service** (Port 8082) - User management and authentication
4. **Rating Service** (Port 8083) - Movie rating operations
5. **Recommendation Service** (Port 8084) - Advanced recommendation algorithms
6. **Gateway Service** (Port 8080) - API Gateway and routing
7. **Frontend** (Port 4200) - Angular web application
8. **Neo4j Database** (Ports 7474/7687) - Graph database

## Quick Start

1. **Clone and navigate to project**:
   ```bash
   cd neo4flix
   ```

2. **Initialize backend services** (Recommended):
   ```bash
   ./scripts/init-backend.sh
   ```

   Or start all services manually:
   ```bash
   docker-compose up -d
   ```

3. **Access the application**:
   - Frontend: http://localhost:4200
   - API Gateway: http://localhost:8080
   - Service Registry: http://localhost:8761
   - Neo4j Browser: http://localhost:7474 (neo4j/password)

4. **Check system health**:
   ```bash
   ./scripts/health-check.sh
   ```

4. **Load sample data**:
   ```bash
   ./scripts/load-sample-data.sh
   ```

## Features

### Core Functionality
- User registration and authentication with JWT
- Movie search by title, genre, release date
- Movie rating system
- Personalized recommendations
- Watchlist management
- Social sharing of recommendations

### Security
- JWT authentication with refresh tokens
- Two-factor authentication (2FA)
- HTTPS/SSL support
- Password complexity requirements
- OAuth2 integration ready

### Recommendation Engine
- Content-based filtering
- Collaborative filtering
- Hybrid recommendation algorithms
- Neo4j Graph Data Science integration
- Real-time recommendation updates

## API Documentation

Once services are running, API documentation is available at:
- http://localhost:8080/swagger-ui.html

## Development

### Prerequisites
- Docker and Docker Compose
- Java 17+ (for local development)
- Node.js 18+ (for frontend development)
- Maven 3.8+

### Local Development Setup
```bash
# Start only Neo4j for local development
docker-compose up neo4j -d

# Run microservices locally with maven
cd microservices/movie-service && mvn spring-boot:run
# Repeat for other services...

# Run frontend locally
cd frontend && npm start
```

## Testing

```bash
# Run all tests
./scripts/run-tests.sh

# Run specific service tests
cd microservices/movie-service && mvn test
```

## Monitoring and Health Checks

- Health checks: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/metrics
- Service discovery via Spring Cloud Gateway

## Configuration

Environment variables can be configured in `docker-compose.yml` or `.env` file:

- `NEO4J_AUTH`: Neo4j credentials
- `JWT_SECRET`: JWT signing secret
- `OAUTH_CLIENT_ID`: OAuth client configuration
- `SSL_ENABLED`: Enable HTTPS

## Data Model

The Neo4j graph contains:
- **User** nodes with authentication data
- **Movie** nodes with metadata (title, genre, release date, etc.)
- **Rating** relationships between users and movies
- **Genre**, **Actor**, **Director** nodes with relationships to movies
- **SIMILAR_TO** relationships for movie recommendations

## Contributing

1. Fork the repository
2. Create feature branch
3. Add tests for new features
4. Submit pull request

## License

MIT License - see LICENSE file for details.