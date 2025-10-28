package com.neo4flix.ratingservice.repository;

import com.neo4flix.ratingservice.model.Movie;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Movie entity
 */
@Repository
public interface MovieRepository extends Neo4jRepository<Movie, String> {

    /**
     * Find movie by title
     */
    Optional<Movie> findByTitle(String title);

    /**
     * Check if movie exists by ID
     */
    @Query("MATCH (m:Movie) WHERE m.id = $movieId RETURN count(m) > 0")
    boolean existsById(@Param("movieId") String movieId);

    /**
     * Get movie with rating statistics
     */
    @Query("MATCH (m:Movie) " +
           "WHERE m.id = $movieId " +
           "OPTIONAL MATCH (:User)-[r:RATED]->(m) " +
           "RETURN m, avg(r.rating) AS averageRating, count(r) AS ratingCount")
    Optional<Movie> findByIdWithRatingStats(@Param("movieId") String movieId);

    /**
     * Update movie rating statistics
     */
    @Query("MATCH (m:Movie) " +
           "WHERE m.id = $movieId " +
           "OPTIONAL MATCH (:User)-[r:RATED]->(m) " +
           "WITH m, avg(r.rating) AS avgRating, count(r) AS totalCount " +
           "SET m.averageRating = CASE WHEN avgRating IS NULL THEN 0.0 ELSE avgRating END, " +
           "    m.totalRatings = CASE WHEN totalCount IS NULL THEN 0 ELSE toInteger(totalCount) END " +
           "RETURN m")
    Optional<Movie> updateRatingStats(@Param("movieId") String movieId);
}