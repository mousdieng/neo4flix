package com.neo4flix.movieservice.repository;

import com.neo4flix.movieservice.dto.MovieRecommendationDTO;
import com.neo4flix.movieservice.model.User;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for User entity
 */
@Repository
public interface UserRepository extends Neo4jRepository<User, String> {
    /**
     * Check if user exists by ID
     */
//    MATCH (user:`User`) WHERE user.id = $__id__ RETURN count(user)
    @Query("MATCH (user:`User`) WHERE user.id = $__id__ RETURN count(user) > 0")
    boolean existsByIdHum(@Param("__id__") String uuid);

    @Query("MATCH (u:User) RETURN keys(u), u LIMIT 1")
    User a();

//    @Query("MATCH (u:User) RETURN u LIMIT 1")
//    User findAUser();
//
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
//    /**
//     * Find user by username
//     */
//    Optional<User> findByUsername(String username);
//
//
//
//    /**
//     * Get user's rating preferences
//     */
//    @Query("MATCH (u:User)-[r:RATED]->(m:Movie)-[:IN_GENRE]->(g:Genre) " +
//           "WHERE u.id = $userId " +
//           "WITH g.name AS genre, avg(r.rating) AS avgRating, count(r) AS ratingCount " +
//           "RETURN genre, avgRating, ratingCount " +
//           "ORDER BY avgRating DESC, ratingCount DESC")
//    List<Object[]> getUserGenrePreferences(@Param("userId") String userId);
//
//    /**
//     * Get user's director preferences
//     */
//    @Query("MATCH (u:User)-[r:RATED]->(m:Movie), (d:Director)-[:DIRECTED]->(m) " +
//           "WHERE u.id = $userId " +
//           "WITH d.name AS director, avg(r.rating) AS avgRating, count(r) AS ratingCount " +
//           "RETURN director, avgRating, ratingCount " +
//           "ORDER BY avgRating DESC, ratingCount DESC")
//    List<Object[]> getUserDirectorPreferences(@Param("userId") String userId);
//
//    /**
//     * Get user's actor preferences
//     */
//    @Query("MATCH (u:User)-[r:RATED]->(m:Movie), (a:Actor)-[:ACTED_IN]->(m) " +
//           "WHERE u.id = $userId " +
//           "WITH a.name AS actor, avg(r.rating) AS avgRating, count(r) AS ratingCount " +
//           "RETURN actor, avgRating, ratingCount " +
//           "ORDER BY avgRating DESC, ratingCount DESC")
//    List<Object[]> getUserActorPreferences(@Param("userId") String userId);
//
//    /**
//     * Get users with similar rating patterns
//     */
//    @Query("MATCH (u1:User {id: $userId})-[r1:RATED]->(m:Movie)<-[r2:RATED]-(u2:User) " +
//           "WHERE u1.id <> u2.id " +
//           "WITH u1, u2, count(m) AS commonMovies, " +
//           "     sum(r1.rating * r2.rating) AS dotProduct, " +
//           "     sqrt(sum(r1.rating * r1.rating)) AS norm1, " +
//           "     sqrt(sum(r2.rating * r2.rating)) AS norm2 " +
//           "WHERE commonMovies >= $minCommonMovies " +
//           "WITH u2, commonMovies, dotProduct / (norm1 * norm2) AS similarity " +
//           "WHERE similarity > $minSimilarity " +
//           "RETURN u2.id AS userId, u2.username AS username, similarity, commonMovies " +
//           "ORDER BY similarity DESC " +
//           "LIMIT $limit")
//    List<Object[]> findSimilarUsers(@Param("userId") String userId,
//                                   @Param("minCommonMovies") Integer minCommonMovies,
//                                   @Param("minSimilarity") Double minSimilarity,
//                                   @Param("limit") Integer limit);
//
//    /**
//     * Get user's rating statistics
//     */
//    @Query("MATCH (u:User)-[r:RATED]->(m:Movie) " +
//           "WHERE u.id = $userId " +
//           "RETURN count(r) AS totalRatings, " +
//           "       avg(r.rating) AS avgRating, " +
//           "       min(r.rating) AS minRating, " +
//           "       max(r.rating) AS maxRating, " +
//           "       stddev(r.rating) AS ratingStdDev")
//    Optional<Object> getUserRatingStats(@Param("userId") String userId);
}