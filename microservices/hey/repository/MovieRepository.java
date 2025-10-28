package com.neo4flix.movieservice.repository;

import com.neo4flix.movieservice.model.Movie;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Movie entity
 */
@Repository
public interface MovieRepository extends Neo4jRepository<Movie, String> {

//    /**
//     * Find movie by title
//     */
//    Optional<Movie> findByTitle(String title);

//    /**
//     * Check if movie exists by ID
//     */
//    @Query("MATCH (m:Movie) WHERE m.id = $movieId RETURN count(m) > 0")
//    boolean existsById(@Param("movieId") String movieId);

//    /**
//     * Find movies by genre
//     */
//    @Query("MATCH (m:Movie)-[:IN_GENRE]->(g:Genre) " +
//           "WHERE g.name = $genreName " +
//           "RETURN m " +
//           "ORDER BY m.averageRating DESC, m.ratingCount DESC")
//    List<Movie> findByGenre(@Param("genreName") String genreName);
//
//    /**
//     * Find movies by director
//     */
//    @Query("MATCH (d:Director)-[:DIRECTED]->(m:Movie) " +
//           "WHERE d.name = $directorName " +
//           "RETURN m " +
//           "ORDER BY m.averageRating DESC, m.ratingCount DESC")
//    List<Movie> findByDirector(@Param("directorName") String directorName);
//
//    /**
//     * Find movies by actor
//     */
//    @Query("MATCH (a:Actor)-[:ACTED_IN]->(m:Movie) " +
//           "WHERE a.name = $actorName " +
//           "RETURN m " +
//           "ORDER BY m.averageRating DESC, m.ratingCount DESC")
//    List<Movie> findByActor(@Param("actorName") String actorName);

//    /**
//     * Find similar movies based on shared genres, directors, and actors
//     */
//    @Query("MATCH (m1:Movie {id: $movieId}) " +
//           "MATCH (m2:Movie) " +
//           "WHERE m1.id <> m2.id " +
//           "OPTIONAL MATCH (m1)-[:IN_GENRE]->(g:Genre)<-[:IN_GENRE]-(m2) " +
//           "OPTIONAL MATCH (d:Director)-[:DIRECTED]->(m1), (d)-[:DIRECTED]->(m2) " +
//           "OPTIONAL MATCH (a:Actor)-[:ACTED_IN]->(m1), (a)-[:ACTED_IN]->(m2) " +
//           "WITH m2, " +
//           "     count(DISTINCT g) AS commonGenres, " +
//           "     count(DISTINCT d) AS commonDirectors, " +
//           "     count(DISTINCT a) AS commonActors " +
//           "WHERE commonGenres > 0 OR commonDirectors > 0 OR commonActors > 0 " +
//           "WITH m2, " +
//           "     (commonGenres * 2 + commonDirectors * 3 + commonActors * 1) AS similarityScore " +
//           "RETURN m2, similarityScore " +
//           "ORDER BY similarityScore DESC " +
//           "LIMIT $limit")
//    List<Object[]> findSimilarMovies(@Param("movieId") String movieId, @Param("limit") Integer limit);

//    /**
//     * Get top-rated movies
//     */
//    @Query("MATCH (m:Movie) " +
//           "WHERE m.averageRating >= $minRating AND m.ratingCount >= $minRatingCount " +
//           "RETURN m " +
//           "ORDER BY m.averageRating DESC, m.ratingCount DESC " +
//           "LIMIT $limit")
//    List<Movie> findTopRated(@Param("minRating") Double minRating,
//                            @Param("minRatingCount") Integer minRatingCount,
//                            @Param("limit") Integer limit);
//
//    /**
//     * Get popular movies (by rating count and average)
//     */
//    @Query("MATCH (m:Movie) " +
//           "WHERE m.averageRating >= $minRating AND m.ratingCount >= $minRatingCount " +
//           "WITH m, (m.averageRating * log(m.ratingCount + 1)) AS popularityScore " +
//           "RETURN m, popularityScore " +
//           "ORDER BY popularityScore DESC " +
//           "LIMIT $limit")
//    List<Object[]> findPopular(@Param("minRating") Double minRating,
//                              @Param("minRatingCount") Integer minRatingCount,
//                              @Param("limit") Integer limit);
//
//    /**
//     * Find movies by year range
//     */
//    @Query("MATCH (m:Movie) " +
//           "WHERE m.releaseYear >= $fromYear AND m.releaseYear <= $toYear " +
//           "RETURN m " +
//           "ORDER BY m.averageRating DESC, m.ratingCount DESC")
//    List<Movie> findByYearRange(@Param("fromYear") Integer fromYear, @Param("toYear") Integer toYear);
//
//    /**
//     * Get movie details with all relationships
//     */
//    @Query("MATCH (m:Movie) " +
//           "WHERE m.id = $movieId " +
//           "OPTIONAL MATCH (m)-[:IN_GENRE]->(g:Genre) " +
//           "OPTIONAL MATCH (d:Director)-[:DIRECTED]->(m) " +
//           "OPTIONAL MATCH (a:Actor)-[:ACTED_IN]->(m) " +
//           "RETURN m, " +
//           "       collect(DISTINCT g.name) AS genres, " +
//           "       collect(DISTINCT d.name) AS directors, " +
//           "       collect(DISTINCT a.name) AS actors")
//    Optional<Object[]> findMovieWithDetails(@Param("movieId") String movieId);
}