package com.neo4flix.ratingservice.repository;

import com.neo4flix.ratingservice.model.Rating;
import com.neo4flix.ratingservice.dto.MovieRatingStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Rating entity
 */
@Repository
public interface RatingRepository extends Neo4jRepository<Rating, Long> {

    /**
     * Find rating by user and movie
     */
    @Query("MATCH (r:Rating) " +
           "WHERE r.userId = $userId AND r.movieId = $movieId " +
           "RETURN r")
    Optional<Rating> findByUserIdAndMovieId(@Param("userId") String userId, @Param("movieId") String movieId);

    /**
     * Find all ratings by user
     */
    @Query(value = "MATCH (r:Rating) " +
           "WHERE r.userId = $userId " +
           "RETURN r " +
           "ORDER BY r.ratedAt DESC " +
           "SKIP $skip LIMIT $limit",
           countQuery = "MATCH (r:Rating) " +
           "WHERE r.userId = $userId " +
           "RETURN count(r)")
    Page<Rating> findByUserId(@Param("userId") String userId, Pageable pageable);

    /**
     * Find all ratings for a movie
     */
    @Query(value = "MATCH (r:Rating) " +
           "WHERE r.movieId = $movieId " +
           "RETURN r " +
           "ORDER BY r.ratedAt DESC " +
           "SKIP $skip LIMIT $limit",
           countQuery = "MATCH (r:Rating) " +
           "WHERE r.movieId = $movieId " +
           "RETURN count(r)")
    Page<Rating> findByMovieId(@Param("movieId") String movieId, Pageable pageable);

    /**
     * Get movie rating statistics
     */
    @Query("MATCH (r:Rating) " +
           "WHERE r.movieId = $movieId " +
           "RETURN r.movieId AS movieId, " +
           "       r.movieTitle AS movieTitle, " +
           "       avg(r.rating) AS averageRating, " +
           "       count(r) AS ratingCount, " +
           "       sum(CASE WHEN r.rating >= 4.5 THEN 1 ELSE 0 END) AS fiveStars, " +
           "       sum(CASE WHEN r.rating >= 3.5 AND r.rating < 4.5 THEN 1 ELSE 0 END) AS fourStars, " +
           "       sum(CASE WHEN r.rating >= 2.5 AND r.rating < 3.5 THEN 1 ELSE 0 END) AS threeStars, " +
           "       sum(CASE WHEN r.rating >= 1.5 AND r.rating < 2.5 THEN 1 ELSE 0 END) AS twoStars, " +
           "       sum(CASE WHEN r.rating < 1.5 THEN 1 ELSE 0 END) AS oneStar")
    Optional<MovieRatingStats> getMovieRatingStats(@Param("movieId") String movieId);

    /**
     * Delete rating by user and movie
     */
    @Query("MATCH (r:Rating) " +
           "WHERE r.userId = $userId AND r.movieId = $movieId " +
           "DELETE r")
    void deleteByUserIdAndMovieId(@Param("userId") String userId, @Param("movieId") String movieId);

    /**
     * Get recent ratings
     */
    @Query("MATCH (r:Rating) " +
           "RETURN r " +
           "ORDER BY r.ratedAt DESC " +
           "LIMIT $limit")
    List<Rating> findRecentRatings(@Param("limit") int limit);

    /**
     * Get top rated movies
     */
    @Query("MATCH (r:Rating) " +
           "WITH r.movieId AS movieId, r.movieTitle AS movieTitle, avg(r.rating) AS avgRating, count(r) AS ratingCount " +
           "WHERE ratingCount >= $minRatings " +
           "RETURN movieId, " +
           "       movieTitle, " +
           "       avgRating AS averageRating, " +
           "       ratingCount " +
           "ORDER BY avgRating DESC, ratingCount DESC " +
           "LIMIT $limit")
    List<MovieRatingStats> findTopRatedMovies(@Param("minRatings") int minRatings, @Param("limit") int limit);

    /**
     * Get user's average rating
     */
    @Query("MATCH (r:Rating) " +
           "WHERE r.userId = $userId " +
           "RETURN avg(r.rating) AS averageRating")
    Optional<Double> getUserAverageRating(@Param("userId") String userId);

    /**
     * Count ratings by user
     */
    @Query("MATCH (r:Rating) " +
           "WHERE r.userId = $userId " +
           "RETURN count(r)")
    Integer countByUserId(@Param("userId") String userId);

    /**
     * Count ratings for movie
     */
    @Query("MATCH (r:Rating) " +
           "WHERE r.movieId = $movieId " +
           "RETURN count(r)")
    Integer countByMovieId(@Param("movieId") String movieId);

    /**
     * Find ratings by rating range
     */
    @Query(value = "MATCH (r:Rating) " +
           "WHERE r.rating >= $minRating AND r.rating <= $maxRating " +
           "RETURN r " +
           "ORDER BY r.ratedAt DESC " +
           "SKIP $skip LIMIT $limit",
           countQuery = "MATCH (r:Rating) " +
           "WHERE r.rating >= $minRating AND r.rating <= $maxRating " +
           "RETURN count(r)")
    Page<Rating> findByRatingBetween(@Param("minRating") Double minRating,
                                   @Param("maxRating") Double maxRating,
                                   Pageable pageable);

    /**
     * Find ratings created after a specific date
     */
    @Query(value = "MATCH (r:Rating) " +
           "WHERE r.ratedAt > $date " +
           "RETURN r " +
           "ORDER BY r.ratedAt DESC " +
           "SKIP $skip LIMIT $limit",
           countQuery = "MATCH (r:Rating) " +
           "WHERE r.ratedAt > $date " +
           "RETURN count(r)")
    Page<Rating> findByRatedAtAfter(@Param("date") LocalDateTime date, Pageable pageable);

    /**
     * Update movie average rating and total ratings count
     * This query finds all ratings for a movie, calculates statistics, and updates the Movie node
     */
    @Query("MATCH (m:Movie {id: $movieId}) " +
           "OPTIONAL MATCH (r:Rating {movieId: $movieId}) " +
           "WITH m, " +
           "     CASE WHEN count(r) > 0 THEN avg(r.rating) ELSE 0.0 END AS avgRating, " +
           "     count(r) AS totalCount " +
           "SET m.averageRating = avgRating, " +
           "    m.totalRatings = toInteger(totalCount) " +
           "RETURN m")
    void updateMovieRatingStats(@Param("movieId") String movieId);
}