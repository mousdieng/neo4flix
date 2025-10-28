package com.neo4flix.movieservice.repository;

import com.neo4flix.movieservice.dto.MovieRecommendationDTO;
import com.neo4flix.movieservice.dto.UserSimilarity;
import com.neo4flix.movieservice.model.Recommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for Recommendation entity
 */
@Repository
public interface RecommendationRepository extends Neo4jRepository<Recommendation, Long> {
//    /**
//     * Get content-based recommendations based on genres
//     */
//    @Query(
//            "MATCH (u:User)-[:RATED]->(rating:Rating)-[:RATED_MOVIE]->(m1:Movie)-[:IN_GENRE]->(g:Genre) " +
//                    "WHERE u.id = $userId AND rating.rating >= 2 " +
//                    "WITH u, g, avg(rating.rating) AS genrePreference " +
//                    "ORDER BY genrePreference DESC " +
//                    "LIMIT 5 " +
//                    "MATCH (m2:Movie)-[:IN_GENRE]->(g) " +
//                    "WHERE NOT EXISTS { " +
//                    "  MATCH (u)-[:RATED]->(:Rating)-[:RATED_MOVIE]->(m2) " +
//                    "} " +
//                    "RETURN DISTINCT " +
//                    "       m2.id AS movieId, " +
//                    "       m2.title AS movieTitle, " +
//                    "       COALESCE(m2.averageRating, 0.0) AS movieRating, " +
//                    "       genrePreference AS score, " +
//                    "       collect(DISTINCT g.name) AS genres " +
//                    "ORDER BY score DESC " +
//                    "LIMIT 10"
//    )
//    List<MovieRecommendationDTO> getContentBasedRecommendations(
//            @Param("userId") String userId
////            @Param("minRating") double minRating,
////            @Param("minMovieRating") double minMovieRating,
////            @Param("limit") int limit
//    );
//
//    /**
//     * Find recommendations for a user
//     */
//    @Query(value = "MATCH (u:User)-[r:RECOMMENDED]->(m:Movie) " +
//           "WHERE u.id = $userId " +
//           "RETURN r, m.title AS movieTitle, m.plot AS moviePlot, " +
//           "       m.releaseYear AS movieYear, m.averageRating AS movieRating " +
//           "ORDER BY r.score DESC " +
//           "SKIP $skip LIMIT $limit",
//           countQuery = "MATCH (u:User)-[r:RECOMMENDED]->(m:Movie) " +
//           "WHERE u.id = $userId " +
//           "RETURN count(r)")
//    Page<Recommendation> findByUserId(@Param("userId") String userId, Pageable pageable);
//
//    /**
//     * Find recommendations for a user by algorithm
//     */
//    @Query(value = "MATCH (u:User)-[r:RECOMMENDED]->(m:Movie) " +
//           "WHERE u.id = $userId AND r.algorithm = $algorithm " +
//           "RETURN r, m.title AS movieTitle, m.plot AS moviePlot, " +
//           "       m.releaseYear AS movieYear, m.averageRating AS movieRating " +
//           "ORDER BY r.score DESC " +
//           "SKIP $skip LIMIT $limit",
//           countQuery = "MATCH (u:User)-[r:RECOMMENDED]->(m:Movie) " +
//           "WHERE u.id = $userId AND r.algorithm = $algorithm " +
//           "RETURN count(r)")
//    Page<Recommendation> findByUserIdAndAlgorithm(@Param("userId") String userId,
//                                                 @Param("algorithm") String algorithm,
//                                                 Pageable pageable);
//
//    /**
//     * Check if recommendation exists for user and movie
//     */
//    @Query("MATCH (u:User)-[r:RECOMMENDED]->(m:Movie) " +
//           "WHERE u.id = $userId AND m.id = $movieId " +
//           "RETURN count(r) > 0")
//    boolean existsByUserIdAndMovieId(@Param("userId") String userId, @Param("movieId") String movieId);
//
//    /**
//     * Delete old recommendations for a user
//     */
//    @Query("MATCH (u:User)-[r:RECOMMENDED]->(m:Movie) " +
//           "WHERE u.id = $userId AND r.recommendedAt < $cutoffDate " +
//           "DELETE r")
//    void deleteOldRecommendations(@Param("userId") String userId, @Param("cutoffDate") LocalDateTime cutoffDate);
//
//    /**
//     * Delete all recommendations for a user
//     */
//    @Query("MATCH (u:User)-[r:RECOMMENDED]->(m:Movie) " +
//           "WHERE u.id = $userId " +
//           "DELETE r")
//    void deleteAllByUserId(@Param("userId") String userId);
//
//    /**
//     * Update recommendation interaction (clicked)
//     */
//    @Query("MATCH (u:User)-[r:RECOMMENDED]->(m:Movie) " +
//           "WHERE u.id = $userId AND m.id = $movieId " +
//           "SET r.clicked = $clicked " +
//           "RETURN r")
//    Optional<Recommendation> updateClicked(@Param("userId") String userId,
//                                         @Param("movieId") String movieId,
//                                         @Param("clicked") Boolean clicked);
//
//    /**
//     * Update recommendation interaction (watched)
//     */
//    @Query("MATCH (u:User)-[r:RECOMMENDED]->(m:Movie) " +
//           "WHERE u.id = $userId AND m.id = $movieId " +
//           "SET r.watched = $watched " +
//           "RETURN r")
//    Optional<Recommendation> updateWatched(@Param("userId") String userId,
//                                         @Param("movieId") String movieId,
//                                         @Param("watched") Boolean watched);
//
//    /**
//     * Get recommendation statistics for a user
//     */
//    @Query("MATCH (u:User)-[r:RECOMMENDED]->(m:Movie) " +
//           "WHERE u.id = $userId " +
//           "RETURN count(r) AS totalRecommendations, " +
//           "       sum(CASE WHEN r.clicked = true THEN 1 ELSE 0 END) AS clickedCount, " +
//           "       sum(CASE WHEN r.watched = true THEN 1 ELSE 0 END) AS watchedCount, " +
//           "       avg(r.score) AS averageScore")
//    Optional<Object> getRecommendationStats(@Param("userId") String userId);
//
//    /**
//     * Find similar users based on rating patterns (for collaborative filtering)
//     * Updated to work with Rating nodes instead of RATED relationships
//     */
//    @Query("MATCH (u1:User), (r1:Rating), (m:Movie), (r2:Rating), (u2:User) " +
//           "WHERE u1.id = $userId " +
//           "  AND r1.userId = u1.id AND r1.movieId = m.id " +
//           "  AND r2.movieId = m.id AND r2.userId = u2.id " +
//           "  AND u1.id <> u2.id " +
//           "WITH u1, u2, " +
//           "     count(m) AS commonRatings, " +
//           "     sum(r1.rating * r2.rating) AS dotProduct, " +
//           "     sum(r1.rating * r1.rating) AS rating1Squared, " +
//           "     sum(r2.rating * r2.rating) AS rating2Squared " +
//           "WHERE commonRatings >= $minCommonRatings " +
//           "WITH u1, u2, commonRatings, " +
//           "     dotProduct / (sqrt(rating1Squared) * sqrt(rating2Squared)) AS similarity " +
//           "WHERE similarity > $minSimilarity " +
//           "RETURN u2.id AS userId, u2.username AS username, " +
//           "       similarity, commonRatings " +
//           "ORDER BY similarity DESC " +
//           "LIMIT $limit")
//    List<UserSimilarity> findSimilarUsers(@Param("userId") String userId,
//                                         @Param("minCommonRatings") Integer minCommonRatings,
//                                         @Param("minSimilarity") Double minSimilarity,
//                                         @Param("limit") Integer limit);
//
//    /**
//     * Get movies rated highly by similar users (collaborative filtering)
//     * Updated to work with Rating nodes instead of RATED relationships
//     */
//    @Query("MATCH (u1:User), (r1:Rating), (m1:Movie), (r2:Rating), (u2:User) " +
//           "WHERE u1.id = $userId " +
//           "  AND r1.userId = u1.id AND r1.movieId = m1.id " +
//           "  AND r2.movieId = m1.id AND r2.userId = u2.id " +
//           "  AND u1.id <> u2.id " +
//           "WITH u1, u2, " +
//           "     count(m1) AS commonRatings, " +
//           "     sum(r1.rating * r2.rating) AS dotProduct, " +
//           "     sum(r1.rating * r1.rating) AS rating1Squared, " +
//           "     sum(r2.rating * r2.rating) AS rating2Squared " +
//           "WHERE commonRatings >= $minCommonRatings " +
//           "WITH u1, u2, commonRatings, " +
//           "     dotProduct / (sqrt(rating1Squared) * sqrt(rating2Squared)) AS similarity " +
//           "WHERE similarity > $minSimilarity " +
//           "MATCH (r3:Rating), (m2:Movie) " +
//           "WHERE r3.userId = u2.id AND r3.movieId = m2.id " +
//           "  AND r3.rating >= $minRating " +
//           "  AND NOT EXISTS { " +
//           "    MATCH (userRating:Rating) " +
//           "    WHERE userRating.userId = u1.id AND userRating.movieId = m2.id " +
//           "  } " +
//           "RETURN m2.id AS movieId, m2.title AS movieTitle, " +
//           "       COALESCE(m2.averageRating, 0.0) AS movieRating, " +
//           "       avg(r3.rating * similarity) AS score " +
//           "ORDER BY score DESC " +
//           "LIMIT $limit")
//    List<Map<String, Object>> getCollaborativeRecommendations(@Param("userId") String userId,
//                                                   @Param("minCommonRatings") Integer minCommonRatings,
//                                                   @Param("minSimilarity") Double minSimilarity,
//                                                   @Param("minRating") Double minRating,
//                                                   @Param("limit") Integer limit);
//
//
//
//    /**
//     * Get popular movies (popularity-based recommendations)
//     * Updated to work with Rating nodes instead of RATED relationships
//     * Returns a single map with all fields
//     */
//    @Query("MATCH (m:Movie) " +
//           "OPTIONAL MATCH (rating:Rating) " +
//           "WHERE rating.movieId = m.id " +
//           "WITH m, count(rating) AS ratingCount, avg(rating.rating) AS avgRating " +
//           "WHERE (avgRating >= $minRating OR avgRating IS NULL OR ratingCount = 0) " +
//           "  AND (ratingCount >= $minRatingCount OR ratingCount = 0) " +
//           "  AND NOT EXISTS { " +
//           "    MATCH (userRating:Rating) " +
//           "    WHERE userRating.userId = $userId AND userRating.movieId = m.id " +
//           "  } " +
//           "WITH m.id AS movieId, m.title AS movieTitle, " +
//           "     COALESCE(avgRating, m.averageRating, 0.0) AS movieRating, " +
//           "     (COALESCE(avgRating, m.averageRating, 0.0) * log(ratingCount + 1)) AS score " +
//           "ORDER BY score DESC " +
//           "LIMIT $limit " +
//           "RETURN movieId, movieTitle, movieRating, score")
//    List<Map<String, Object>> getPopularRecommendations(@Param("userId") String userId,
//                                            @Param("minRating") Double minRating,
//                                            @Param("minRatingCount") Integer minRatingCount,
//                                            @Param("limit") Integer limit);
//
//    /**
//     * Get recommendations by director preference
//     * Updated to work with Rating nodes instead of RATED relationships
//     */
//    @Query("MATCH (u:User), (rating:Rating), (m1:Movie), (d:Director) " +
//           "WHERE u.id = $userId " +
//           "  AND rating.userId = u.id AND rating.movieId = m1.id " +
//           "  AND rating.rating >= $minRating " +
//           "  AND (d)-[:DIRECTED]->(m1) " +
//           "WITH u, d, avg(rating.rating) AS directorPreference, count(rating) AS directorCount " +
//           "ORDER BY directorPreference DESC, directorCount DESC " +
//           "LIMIT 3 " +
//           "MATCH (d)-[:DIRECTED]->(m2:Movie) " +
//           "WHERE m2.averageRating >= $minMovieRating " +
//           "  AND NOT EXISTS { " +
//           "    MATCH (userRating:Rating) " +
//           "    WHERE userRating.userId = u.id AND userRating.movieId = m2.id " +
//           "  } " +
//           "RETURN m2.id AS movieId, m2.title AS movieTitle, " +
//           "       COALESCE(m2.averageRating, 0.0) AS movieRating, " +
//           "       avg(directorPreference) AS score " +
//           "ORDER BY score DESC " +
//           "LIMIT $limit")
//    List<Map<String, Object>> getDirectorBasedRecommendations(@Param("userId") String userId,
//                                                   @Param("minRating") Double minRating,
//                                                   @Param("minMovieRating") Double minMovieRating,
//                                                   @Param("limit") Integer limit);
//
//    /**
//     * Custom save method to create RECOMMENDED relationship
//     */
//    @Query("MATCH (u:User {id: $userId}), (m:Movie {id: $movieId}) " +
//           "MERGE (u)-[r:RECOMMENDED]->(m) " +
//           "SET r.score = $score, " +
//           "    r.algorithm = $algorithm, " +
//           "    r.reason = $reason, " +
//           "    r.recommendedAt = datetime($recommendedAt), " +
//           "    r.clicked = $clicked, " +
//           "    r.watched = $watched " +
//           "RETURN id(r) AS id")
//    Long saveRecommendation(@Param("userId") String userId,
//                           @Param("movieId") String movieId,
//                           @Param("score") Double score,
//                           @Param("algorithm") String algorithm,
//                           @Param("reason") String reason,
//                           @Param("recommendedAt") String recommendedAt,
//                           @Param("clicked") Boolean clicked,
//                           @Param("watched") Boolean watched);
}