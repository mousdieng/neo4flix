# Neo4j Graph Data Science (GDS) Implementation Guide

## Overview

This project now uses **Neo4j Graph Data Science (GDS) Library** to implement sophisticated recommendation algorithms. The GDS library provides optimized, production-ready graph algorithms that significantly improve recommendation quality and performance.

## What Changed

### Before (Custom Implementation)
- Manual Cypher queries with basic mathematical operations
- Similarity calculations done in Java using Apache Commons Math
- No advanced graph algorithms
- Limited scalability for large datasets

### After (GDS Implementation)
- Neo4j GDS library algorithms (Node Similarity, PageRank, Cosine Similarity)
- Native graph algorithm execution in Neo4j
- Optimized for large-scale graph processing
- Advanced algorithms like community detection and centrality measures

## Architecture

### GDS Algorithms Implemented

1. **GDS Collaborative Filtering** (`gds-collaborative`)
   - Uses: `gds.nodeSimilarity.stream`
   - Purpose: Find users with similar rating patterns
   - File: `GDSCollaborativeFilteringAlgorithm.java`

2. **GDS Content-Based Filtering** (`gds-content-based`)
   - Uses: `gds.alpha.similarity.cosine.stream`
   - Purpose: Find movies similar to user's liked movies
   - File: `GDSContentBasedAlgorithm.java`

3. **GDS PageRank Popularity** (`gds-pagerank`)
   - Uses: `gds.pageRank.stream`
   - Purpose: Identify influential movies in the network
   - File: `GDSPageRankAlgorithm.java`

4. **GDS Hybrid Algorithm** (`gds-hybrid`)
   - Combines all three GDS algorithms
   - Weighted scoring: 40% collaborative, 30% content, 30% PageRank
   - File: `GDSHybridAlgorithm.java`

## Available Algorithms

### Original Algorithms (Still Available)
- `collaborative` - Original user-based collaborative filtering
- `content-based` - Original content-based filtering
- `popular` - Original popularity-based filtering
- `hybrid` - Original hybrid algorithm

### New GDS Algorithms
- `gds-collaborative` - GDS Node Similarity based collaborative filtering
- `gds-content-based` - GDS Cosine Similarity based content filtering
- `gds-pagerank` - GDS PageRank based popularity
- `gds-hybrid` - Combined GDS hybrid algorithm (RECOMMENDED)

## How It Works

### 1. Graph Projection

GDS algorithms require a graph projection to be created first:

```cypher
CALL gds.graph.project.cypher(
    'user-movie-ratings',
    'MATCH (n) WHERE n:User OR n:Movie RETURN id(n) AS id, labels(n) AS labels',
    'MATCH (u:User)-[:RATED]->(r:Rating)-[:RATED_MOVIE]->(m:Movie)
     RETURN id(u) AS source, id(m) AS target, r.rating AS weight'
)
```

This creates an in-memory graph that GDS algorithms can efficiently process.

### 2. Node Similarity (Collaborative Filtering)

```cypher
CALL gds.nodeSimilarity.stream('user-movie-ratings', {
    similarityCutoff: 0.3,
    topK: 20
})
YIELD node1, node2, similarity
```

Finds the top 20 most similar users for each user, with minimum 0.3 similarity.

### 3. PageRank (Popularity)

```cypher
CALL gds.pageRank.stream('user-movie-ratings', {
    maxIterations: 20,
    dampingFactor: 0.85
})
YIELD nodeId, score
```

Calculates the "importance" of each movie based on the rating network structure.

### 4. Cosine Similarity (Content-Based)

```cypher
CALL gds.alpha.similarity.cosine.stream({
    nodeQuery: 'MATCH (m:Movie) RETURN id(m) AS id',
    relationshipQuery:
        'MATCH (m:Movie)-[:IN_GENRE]->(g:Genre)
         RETURN id(m) AS source, id(g) AS target, 1.0 AS weight',
    topK: 10
})
```

Finds movies with similar genre profiles.

## Usage Examples

### API Endpoint

```bash
POST /api/recommendations/generate
Content-Type: application/json

{
  "userId": "user123",
  "algorithm": "gds-hybrid",
  "limit": 10,
  "minRating": 4.0,
  "genre": ["Action", "Thriller"]
}
```

### Response

```json
{
  "userId": "user123",
  "recommendations": [
    {
      "movieId": "movie456",
      "score": 8.75,
      "algorithm": "gds-hybrid",
      "reason": "GDS Hybrid: Similar users (score: 8.90) + Similar content (score: 8.45) + Popular choice (score: 8.90)"
    }
  ]
}
```

## Algorithm Comparison

| Feature | Original | GDS-Based |
|---------|----------|-----------|
| **Performance** | Moderate | High (optimized) |
| **Scalability** | Limited | Excellent |
| **Accuracy** | Good | Better |
| **Cold Start** | Poor | Good (PageRank) |
| **Memory Usage** | Low | Higher (in-memory graph) |
| **Setup Complexity** | Simple | Moderate |

## Performance Considerations

### Graph Projection
- Created once per algorithm run
- Cached in memory for subsequent operations
- Can be reused across multiple requests
- Should be dropped and recreated periodically to reflect new data

### Memory Requirements
- GDS requires sufficient heap memory for graph projections
- Recommended: At least 4GB heap for datasets < 100K nodes
- For larger datasets, scale accordingly

### Optimization Tips

1. **Use Graph Projection Caching**
   ```java
   // Check if graph exists before creating
   if (!gdsRepository.graphProjectionExists(graphName)) {
       gdsRepository.createUserMovieGraphProjection(graphName);
   }
   ```

2. **Limit topK Parameter**
   - Don't set topK too high (20-50 is usually sufficient)
   - Higher values increase computation time

3. **Set Similarity Cutoffs**
   - Use `similarityCutoff` to filter weak similarities
   - Reduces noise and improves results

4. **Periodic Graph Cleanup**
   ```java
   // Drop old graph projections
   gdsRepository.dropGraphProjection("old-graph-name");
   ```

## Configuration

### Neo4j Docker Setup

Already configured in `docker-compose.yml`:

```yaml
neo4j:
  environment:
    - NEO4J_PLUGINS=["apoc", "graph-data-science"]
    - NEO4J_dbms_security_procedures_unrestricted=apoc.*,gds.*
    - NEO4J_dbms_security_procedures_allowlist=apoc.*,gds.*
```

### Maven Dependencies

Added to all microservices:

```xml
<!-- Neo4j Graph Data Science Library -->
<dependency>
    <groupId>org.neo4j.gds</groupId>
    <artifactId>proc</artifactId>
    <version>2.5.0</version>
</dependency>

<dependency>
    <groupId>org.neo4j</groupId>
    <artifactId>neo4j-cypher-dsl</artifactId>
    <version>2023.9.0</version>
</dependency>
```

## Testing

### Manual Testing

1. **Start services**:
   ```bash
   docker-compose up -d
   ```

2. **Verify GDS is installed**:
   ```cypher
   CALL gds.list()
   ```

3. **Test recommendation**:
   ```bash
   curl -X POST http://localhost:9083/api/recommendations/generate \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer <token>" \
     -d '{
       "userId": "user123",
       "algorithm": "gds-hybrid",
       "limit": 10
     }'
   ```

### Algorithm Selection Guide

- **New users (cold start)**: Use `gds-pagerank` or `gds-hybrid`
- **Users with few ratings (< 5)**: Use `gds-pagerank`
- **Users with moderate ratings (5-20)**: Use `gds-collaborative` or `gds-hybrid`
- **Users with many ratings (> 20)**: Use `gds-hybrid` (best results)
- **Genre-specific recommendations**: Any GDS algorithm with genre filter
- **Best overall**: `gds-hybrid` (handles all cases)

## Monitoring

### Key Metrics to Monitor

1. **Graph Projection Size**
   ```cypher
   CALL gds.graph.list()
   YIELD graphName, nodeCount, relationshipCount, memoryUsage
   ```

2. **Algorithm Performance**
   ```cypher
   CALL gds.nodeSimilarity.stats('user-movie-ratings')
   YIELD computeMillis, nodeCount, similarityPairs
   ```

3. **Memory Usage**
   - Monitor Neo4j heap usage
   - Watch for OOM errors
   - Adjust heap size if needed

## Troubleshooting

### Issue: "Graph not found"
**Solution**: Ensure graph projection is created before running algorithms
```java
gdsRepository.createUserMovieGraphProjection(graphName);
```

### Issue: "Insufficient memory"
**Solution**: Increase Neo4j heap size in docker-compose.yml
```yaml
environment:
  - NEO4J_dbms_memory_heap_initial__size=2G
  - NEO4J_dbms_memory_heap_max__size=4G
```

### Issue: "Procedure not found"
**Solution**: Verify GDS plugin is installed
```cypher
CALL dbms.procedures()
YIELD name WHERE name STARTS WITH 'gds'
RETURN name
```

### Issue: Slow performance
**Solutions**:
- Reduce `topK` parameter
- Increase `similarityCutoff`
- Create indexes on frequently queried properties
- Consider incremental graph updates instead of full rebuilds

## Advanced Features (Future Enhancements)

### Potential Additions

1. **Community Detection (Louvain)**
   - Already implemented in repository
   - Groups users into communities
   - Recommends popular movies within user's community

2. **Graph Embeddings (Node2Vec)**
   - Learn vector representations of movies
   - Enable more sophisticated similarity calculations

3. **Link Prediction**
   - Predict future user-movie ratings
   - Proactive recommendations

4. **Temporal Analysis**
   - Time-weighted recommendations
   - Trending movies detection

## References

- [Neo4j Graph Data Science Documentation](https://neo4j.com/docs/graph-data-science/)
- [Node Similarity Algorithm](https://neo4j.com/docs/graph-data-science/current/algorithms/node-similarity/)
- [PageRank Algorithm](https://neo4j.com/docs/graph-data-science/current/algorithms/page-rank/)
- [Cosine Similarity](https://neo4j.com/docs/graph-data-science/current/alpha-algorithms/cosine/)

## Conclusion

The Neo4j GDS implementation provides:
- ✅ Better recommendation quality
- ✅ Improved scalability
- ✅ Production-ready algorithms
- ✅ Built-in optimization
- ✅ Cold start handling (PageRank)
- ✅ Advanced graph analytics capabilities

**Recommended Default**: Use `gds-hybrid` algorithm for best results across all user types.
