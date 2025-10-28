# Neo4flix Recommendation Service

A sophisticated movie recommendation microservice built with Spring Boot, Neo4j, and advanced machine learning algorithms.

## Overview

The Recommendation Service generates personalized movie recommendations using four different algorithms:
- **Collaborative Filtering**: Recommends movies based on user similarity
- **Content-Based Filtering**: Recommends movies based on genres, directors, and actors
- **Popularity-Based**: Recommends highly-rated popular movies
- **Hybrid**: Combines all three algorithms for optimal recommendations

## Features

- **Multiple Recommendation Algorithms**
  - Collaborative filtering using cosine similarity
  - Content-based filtering with genre, director, and actor analysis
  - Popularity-based recommendations using IMDB-style weighted ratings
  - Hybrid algorithm combining all approaches

- **Real-Time Updates**
  - Kafka event consumers for rating changes
  - Automatic recommendation refresh on user activity
  - Cache management for performance

- **Advanced Capabilities**
  - User similarity detection
  - Similar movie discovery
  - Genre-based filtering
  - Recommendation tracking (clicks, views)
  - Batch recommendation generation

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Database**: Neo4j (Graph Database)
- **Cache**: Caffeine
- **Message Queue**: Apache Kafka
- **Service Discovery**: Netflix Eureka
- **Security**: JWT Authentication
- **Documentation**: Swagger/OpenAPI
- **Build Tool**: Maven

## Quick Start

### Running Locally

1. **Ensure Neo4j and Kafka are running**:
   ```bash
   docker-compose up neo4j kafka -d
   ```

2. **Build the service**:
   ```bash
   mvn clean package -DskipTests
   ```

3. **Run the service**:
   ```bash
   java --add-opens java.base/java.lang=ALL-UNNAMED \
        --add-opens java.base/java.util=ALL-UNNAMED \
        -jar target/recommendation-service-1.0.0.jar
   ```

4. **Access the service**:
   - API: http://localhost:9083/api/v1/recommendations
   - Health: http://localhost:9083/actuator/health
   - Swagger UI: http://localhost:9083/swagger-ui.html

### Docker Deployment

```bash
docker build -t neo4flix/recommendation-service .
docker run -p 9083:9083 \
  -e NEO4J_URI=bolt://neo4j:7687 \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  neo4flix/recommendation-service
```

## API Endpoints

### Get Recommendations

#### Generate Personalized Recommendations
```bash
POST /api/v1/recommendations/generate
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

{
  "userId": "user123",
  "limit": 20,
  "algorithm": "hybrid",
  "minRating": 3.5
}
```

#### Get Similar Movies
```bash
GET /api/v1/recommendations/similar/{movieId}?limit=10
```

Example:
```bash
curl http://localhost:9083/api/v1/recommendations/similar/tt0111161?limit=5
```

#### Get Trending Recommendations
```bash
GET /api/v1/recommendations/trending?limit=10
Authorization: Bearer <JWT_TOKEN>
```

#### Get Recommendations by Genre
```bash
GET /api/v1/recommendations/genre/Action?limit=10
Authorization: Bearer <JWT_TOKEN>
```

#### Get New User Recommendations
```bash
GET /api/v1/recommendations/new-user?limit=10
```

### User Interactions

#### Track User Interaction
```bash
POST /api/v1/recommendations/interactions
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

{
  "userId": "user123",
  "movieId": "tt0111161",
  "action": "view|click|watch|rate",
  "value": 4.5
}
```

#### Mark Recommendation as Clicked
```bash
POST /api/v1/recommendations/clicked?movieId=tt0111161
Authorization: Bearer <JWT_TOKEN>
```

### Analytics

#### Get Recommendation Statistics
```bash
GET /api/v1/recommendations/user/{userId}/stats
```

#### Find Similar Users
```bash
GET /api/v1/recommendations/user/{userId}/similar-users?limit=10
```

#### Get Available Algorithms
```bash
GET /api/v1/recommendations/algorithms
```

Response:
```json
["collaborative", "content", "popular", "hybrid"]
```

## Recommendation Algorithms

### 1. Collaborative Filtering

Finds users with similar taste and recommends movies they've rated highly.

**Algorithm**:
- Calculates user similarity using cosine similarity
- Considers users with at least 3 common rated movies
- Weights recommendations by similarity score

**Query Example**:
```cypher
MATCH (u1:User)-[r1:RATED]->(m:Movie)<-[r2:RATED]-(u2:User)
WHERE u1.id = $userId AND similarity > 0.3
MATCH (u2)-[r3:RATED]->(m2:Movie)
WHERE NOT EXISTS((u1)-[:RATED]->(m2))
RETURN m2 ORDER BY avg(r3.rating * similarity) DESC
```

### 2. Content-Based Filtering

Recommends movies similar to ones the user has rated highly.

**Features Analyzed**:
- **Genres**: Movies in preferred genres
- **Directors**: Movies by favorite directors
- **Actors**: Movies with favorite actors

**Weights**:
- Genre similarity: 40%
- Director match: 40%
- Actor match: 20%

### 3. Popularity-Based

Recommends highly-rated movies using IMDB's weighted rating formula.

**Formula**:
```
WR = (v / (v + m)) * R + (m / (v + m)) * C
Where:
- WR = Weighted Rating
- v = number of votes
- m = minimum votes threshold
- R = average rating
- C = mean rating across all movies
```

### 4. Hybrid Algorithm

Combines all three algorithms with weighted scoring.

**Weights**:
- Collaborative: 50%
- Content: 30%
- Popularity: 20%

## Configuration

### Application Properties

```yaml
spring:
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: password

  kafka:
    bootstrap-servers: localhost:29092
    consumer:
      group-id: recommendation-service-group

  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=30m

server:
  port: 9083

recommendation:
  algorithms:
    collaborative:
      min-common-ratings: 3
      min-similarity: 0.3
      weight: 0.5
    content:
      weight: 0.3
    popularity:
      min-rating: 3.5
      min-rating-count: 10
      weight: 0.2
```

## Database Schema

### Neo4j Graph Structure

```
(User)-[:RATED]->(Movie)-[:IN_GENRE]->(Genre)
(Director)-[:DIRECTED]->(Movie)
(Actor)-[:ACTED_IN]->(Movie)
(User)-[:RECOMMENDED]->(Movie)
```

### Node Properties

**Movie**:
- id: String (IMDB ID)
- title: String
- releaseYear: Integer
- imdbRating: Double
- imdbVotes: Integer

**User**:
- id: String
- username: String
- email: String

**Genre, Director, Actor**:
- id/name: String

## Kafka Integration

### Consumed Topics

| Topic | Purpose |
|-------|---------|
| rating.created | Refresh recommendations when user rates a movie |
| rating.updated | Update recommendations on rating changes |
| rating.deleted | Recalculate recommendations on rating removal |

### Event Structure

```json
{
  "userId": "user123",
  "movieId": "tt0111161",
  "rating": 4.5,
  "timestamp": "2025-10-10T15:00:00Z"
}
```

## Performance Optimization

### Caching Strategy

- **Recommendation Cache**: 30 minutes TTL
- **User Similarity Cache**: 1 hour TTL
- **Movie Metadata Cache**: 24 hours TTL

### Query Optimization

- **Indexes**:
  ```cypher
  CREATE INDEX movie_title_index FOR (m:Movie) ON (m.title)
  CREATE INDEX movie_rating_index FOR (m:Movie) ON (m.imdbRating)
  CREATE CONSTRAINT movie_id_unique FOR (m:Movie) REQUIRE m.id IS UNIQUE
  ```

- **Batch Processing**: Generate recommendations in batches of 100 users

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Manual Testing with cURL

**Test Popular Movies**:
```bash
curl http://localhost:9083/api/v1/recommendations/new-user?limit=5
```

**Test Similar Movies**:
```bash
curl http://localhost:9083/api/v1/recommendations/similar/tt0111161?limit=5
```

## Monitoring

### Health Check
```bash
curl http://localhost:9083/actuator/health
```

### Metrics
```bash
curl http://localhost:9083/actuator/metrics
```

### Prometheus Metrics
```bash
curl http://localhost:9083/actuator/prometheus
```

## Error Handling

The service returns standardized error responses:

```json
{
  "timestamp": "2025-10-10T15:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid algorithm specified",
  "path": "/api/v1/recommendations/generate"
}
```

## Security

- **JWT Authentication**: Required for user-specific endpoints
- **CORS**: Configured for frontend access
- **Role-Based Access**: USER and ADMIN roles supported

## Deployment

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| NEO4J_URI | Neo4j connection URI | bolt://localhost:7687 |
| NEO4J_USERNAME | Neo4j username | neo4j |
| NEO4J_PASSWORD | Neo4j password | password |
| KAFKA_BOOTSTRAP_SERVERS | Kafka servers | localhost:29092 |
| JWT_SECRET | JWT signing secret | - |
| SERVER_PORT | Service port | 9083 |

### Docker Compose

```yaml
recommendation-service:
  image: neo4flix/recommendation-service:latest
  ports:
    - "9083:9083"
  environment:
    - NEO4J_URI=bolt://neo4j:7687
    - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
  depends_on:
    - neo4j
    - kafka
```

## Troubleshooting

### Common Issues

**1. Service fails to start**
- Ensure Neo4j and Kafka are running
- Check Java version (requires Java 17+)
- Verify JVM arguments are set

**2. No recommendations generated**
- Verify database has movies and ratings
- Check algorithm configuration
- Review service logs

**3. Kafka connection errors**
- Verify Kafka is running on correct port
- Check network connectivity
- Review consumer group configuration

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

MIT License - See LICENSE file for details

## Support

For issues and questions:
- GitHub Issues: [neo4flix/issues](https://github.com/neo4flix/issues)
- Documentation: [docs.neo4flix.com](https://docs.neo4flix.com)
