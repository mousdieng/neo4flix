package com.neo4flix.movieservice.repository;

import com.neo4flix.movieservice.dto.MovieRecommendationDTO;
import com.neo4flix.movieservice.model.Movie;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for Neo4j Graph Data Science (GDS) based recommendations.
 * Uses GDS library algorithms like Node Similarity, PageRank, and Community Detection.
 *
 * Note: Extends Neo4jRepository<Movie, String> to satisfy Spring Data requirements,
 * but primarily used for custom GDS query methods rather than CRUD operations.
 */
public interface GDSRecommendationRepository extends Neo4jRepository<Movie, String> {

    /**
     * Create a graph projection for user-movie ratings.
     * This projection is used by GDS algorithms.
     */
    @Query("""
        CALL gds.graph.project.cypher(
            $graphName,
            'MATCH (n) WHERE n:User OR n:Movie RETURN id(n) AS id, labels(n) AS labels',
            'MATCH (u:User)-[:RATED]->(r:Rating)-[:RATED_MOVIE]->(m:Movie)
             RETURN id(u) AS source, id(m) AS target, r.rating AS weight'
        )
        YIELD graphName, nodeCount, relationshipCount
        RETURN graphName, nodeCount, relationshipCount
        """)
    void createUserMovieGraphProjection(@Param("graphName") String graphName);

    /**
     * Check if a graph projection exists.
     */
    @Query("""
        CALL gds.graph.exists($graphName)
        YIELD exists
        RETURN exists
        """)
    Boolean graphProjectionExists(@Param("graphName") String graphName);

    /**
     * Drop a graph projection if it exists.
     */
    @Query("""
        CALL gds.graph.drop($graphName, false)
        YIELD graphName
        RETURN graphName
        """)
    void dropGraphProjection(@Param("graphName") String graphName);

    /**
     * GDS Node Similarity algorithm for collaborative filtering.
     * Finds similar users based on their rating patterns.
     */
    @Query("""
        MATCH (target:User {id: $userId})
        CALL gds.nodeSimilarity.stream($graphName, {
            similarityCutoff: $minSimilarity,
            topK: 20
        })
        YIELD node1, node2, similarity
        WITH target, node1, node2, similarity
        WHERE id(target) = node1 AND node1 <> node2
        MATCH (similarUser:User)
        WHERE id(similarUser) = node2
        MATCH (similarUser)-[:RATED]->(r:Rating)-[:RATED_MOVIE]->(m:Movie)
        WHERE r.rating >= 4.0
          AND NOT EXISTS {
            MATCH (target)-[:RATED]->(:Rating)-[:RATED_MOVIE]->(m)
          }
        WITH m, avg(r.rating) AS avgRating, similarity, count(r) AS numRatings
        WHERE numRatings >= 2
        RETURN m AS movie,
               avgRating AS movieRating,
               (avgRating * similarity * log(numRatings + 1)) AS score
        ORDER BY score DESC
        LIMIT $limit
        """)
    List<MovieRecommendationDTO> getGDSCollaborativeRecommendations(
            @Param("graphName") String graphName,
            @Param("userId") String userId,
            @Param("minSimilarity") double minSimilarity,
            @Param("limit") int limit
    );

    /**
     * GDS Node Similarity for collaborative filtering with genre filter.
     */
    @Query("""
        MATCH (target:User {id: $userId})
        CALL gds.nodeSimilarity.stream($graphName, {
            similarityCutoff: $minSimilarity,
            topK: 20
        })
        YIELD node1, node2, similarity
        WITH target, node1, node2, similarity
        WHERE id(target) = node1 AND node1 <> node2
        MATCH (similarUser:User)
        WHERE id(similarUser) = node2
        MATCH (similarUser)-[:RATED]->(r:Rating)-[:RATED_MOVIE]->(m:Movie)
        WHERE r.rating >= 4.0
          AND NOT EXISTS {
            MATCH (target)-[:RATED]->(:Rating)-[:RATED_MOVIE]->(m)
          }
          AND EXISTS {
            MATCH (m)-[:IN_GENRE]->(g:Genre)
            WHERE g.name IN $genres
          }
        WITH m, avg(r.rating) AS avgRating, similarity, count(r) AS numRatings
        WHERE numRatings >= 2
        RETURN m AS movie,
               avgRating AS movieRating,
               (avgRating * similarity * log(numRatings + 1)) AS score
        ORDER BY score DESC
        LIMIT $limit
        """)
    List<MovieRecommendationDTO> getGDSCollaborativeRecommendationsWithGenre(
            @Param("graphName") String graphName,
            @Param("userId") String userId,
            @Param("minSimilarity") double minSimilarity,
            @Param("genres") List<String> genres,
            @Param("limit") int limit
    );

    /**
     * GDS PageRank algorithm for movie popularity.
     * Movies with higher PageRank are more "central" in the rating network.
     */
    @Query("""
        CALL gds.pageRank.stream($graphName, {
            maxIterations: 20,
            dampingFactor: 0.85
        })
        YIELD nodeId, score
        WITH nodeId, score
        MATCH (m:Movie)
        WHERE id(m) = nodeId
        OPTIONAL MATCH (r:Rating)
        WHERE r.movieId = m.id
        WITH m, score AS pageRankScore,
             count(r) AS ratingCount,
             COALESCE(avg(r.rating), m.averageRating, 0.0) AS avgRating
        WHERE avgRating >= $minRating
          AND NOT EXISTS {
            MATCH (u:User {id: $userId})-[:RATED]->(:Rating)-[:RATED_MOVIE]->(m)
          }
        RETURN m AS movie,
               avgRating AS movieRating,
               (pageRankScore * 100 + log(ratingCount + 1)) AS score
        ORDER BY score DESC
        LIMIT $limit
        """)
    List<MovieRecommendationDTO> getGDSPageRankRecommendations(
            @Param("graphName") String graphName,
            @Param("userId") String userId,
            @Param("minRating") double minRating,
            @Param("limit") int limit
    );

    /**
     * GDS PageRank with genre filter.
     */
    @Query("""
        CALL gds.pageRank.stream($graphName, {
            maxIterations: 20,
            dampingFactor: 0.85
        })
        YIELD nodeId, score
        WITH nodeId, score
        MATCH (m:Movie)
        WHERE id(m) = nodeId
          AND EXISTS {
            MATCH (m)-[:IN_GENRE]->(g:Genre)
            WHERE g.name IN $genres
          }
        OPTIONAL MATCH (r:Rating)
        WHERE r.movieId = m.id
        WITH m, score AS pageRankScore,
             count(r) AS ratingCount,
             COALESCE(avg(r.rating), m.averageRating, 0.0) AS avgRating
        WHERE avgRating >= $minRating
          AND NOT EXISTS {
            MATCH (u:User {id: $userId})-[:RATED]->(:Rating)-[:RATED_MOVIE]->(m)
          }
        RETURN m AS movie,
               avgRating AS movieRating,
               (pageRankScore * 100 + log(ratingCount + 1)) AS score
        ORDER BY score DESC
        LIMIT $limit
        """)
    List<MovieRecommendationDTO> getGDSPageRankRecommendationsWithGenre(
            @Param("graphName") String graphName,
            @Param("userId") String userId,
            @Param("minRating") double minRating,
            @Param("genres") List<String> genres,
            @Param("limit") int limit
    );

    /**
     * GDS Cosine Similarity for content-based filtering.
     * Finds movies similar to those the user has rated highly.
     */
    @Query("""
        MATCH (u:User {id: $userId})-[:RATED]->(r:Rating)-[:RATED_MOVIE]->(likedMovie:Movie)
        WHERE r.rating >= $minUserRating
        WITH u, collect(id(likedMovie)) AS likedMovieIds
        CALL gds.alpha.similarity.cosine.stream({
            nodeQuery: 'MATCH (m:Movie) RETURN id(m) AS id',
            relationshipQuery:
                'MATCH (m:Movie)-[:IN_GENRE]->(g:Genre)
                 RETURN id(m) AS source, id(g) AS target, 1.0 AS weight',
            topK: 10
        })
        YIELD item1, item2, similarity
        WITH u, likedMovieIds, item1, item2, similarity
        WHERE item1 IN likedMovieIds AND NOT item2 IN likedMovieIds
        MATCH (m:Movie)
        WHERE id(m) = item2
        OPTIONAL MATCH (rating:Rating)
        WHERE rating.movieId = m.id
        WITH m, similarity,
             COALESCE(avg(rating.rating), m.averageRating, 0.0) AS avgRating,
             count(rating) AS numRatings
        WHERE avgRating >= $minMovieRating
        RETURN m AS movie,
               avgRating AS movieRating,
               (similarity * avgRating * log(numRatings + 1)) AS score
        ORDER BY score DESC
        LIMIT $limit
        """)
    List<MovieRecommendationDTO> getGDSContentBasedRecommendations(
            @Param("userId") String userId,
            @Param("minUserRating") double minUserRating,
            @Param("minMovieRating") double minMovieRating,
            @Param("limit") int limit
    );

    /**
     * Get count of ratings for a user.
     */
    @Query("""
        MATCH (u:User {id: $userId})-[:RATED]->(r:Rating)
        RETURN count(r) AS count
        """)
    int getUserRatingsCount(@Param("userId") String userId);

    /**
     * GDS Community Detection (Louvain) to find user communities.
     * Recommends popular movies within the user's community.
     */
    @Query("""
        CALL gds.louvain.stream($graphName, {
            relationshipWeightProperty: 'weight'
        })
        YIELD nodeId, communityId
        WITH nodeId, communityId
        MATCH (target:User {id: $userId})
        WHERE id(target) = nodeId
        WITH communityId AS userCommunity
        CALL gds.louvain.stream($graphName, {
            relationshipWeightProperty: 'weight'
        })
        YIELD nodeId AS otherNodeId, communityId AS otherCommunity
        WHERE otherCommunity = userCommunity
        MATCH (m:Movie)
        WHERE id(m) = otherNodeId
        OPTIONAL MATCH (r:Rating)
        WHERE r.movieId = m.id
        WITH m,
             count(r) AS ratingCount,
             COALESCE(avg(r.rating), m.averageRating, 0.0) AS avgRating
        WHERE avgRating >= $minRating
          AND NOT EXISTS {
            MATCH (u:User {id: $userId})-[:RATED]->(:Rating)-[:RATED_MOVIE]->(m)
          }
        RETURN m AS movie,
               avgRating AS movieRating,
               (avgRating * log(ratingCount + 1)) AS score
        ORDER BY score DESC
        LIMIT $limit
        """)
    List<MovieRecommendationDTO> getGDSCommunityRecommendations(
            @Param("graphName") String graphName,
            @Param("userId") String userId,
            @Param("minRating") double minRating,
            @Param("limit") int limit
    );
}
