package com.neo4flix.ratingservice.service;

import com.neo4flix.ratingservice.dto.*;
import com.neo4flix.ratingservice.model.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for rating operations
 */
public interface RatingService {

    /**
     * Create a new rating
     */
    RatingResponse createRating(String userId, String username, CreateRatingRequest request);

    /**
     * Update an existing rating
     */
    RatingResponse updateRating(String userId, String movieId, UpdateRatingRequest request);

    /**
     * Delete a rating
     */
    void deleteRating(String userId, String movieId);

    /**
     * Get rating by user and movie
     */
    RatingResponse getRating(String userId, String movieId);

    /**
     * Get all ratings by user
     */
    Page<RatingResponse> getUserRatings(String userId, Pageable pageable);

    /**
     * Get all ratings for a movie
     */
    Page<RatingResponse> getMovieRatings(String movieId, Pageable pageable);

    /**
     * Get movie rating statistics
     */
    MovieRatingStats getMovieRatingStats(String movieId);

    /**
     * Get recent ratings
     */
    List<RatingResponse> getRecentRatings(int limit);

    /**
     * Get top rated movies
     */
    List<MovieRatingStats> getTopRatedMovies(int minRatings, int limit);

    /**
     * Get user's average rating
     */
    Double getUserAverageRating(String userId);

    /**
     * Get ratings count for user
     */
    Integer getUserRatingsCount(String userId);

    /**
     * Get ratings count for movie
     */
    Integer getMovieRatingsCount(String movieId);

    /**
     * Get ratings by rating range
     */
    Page<RatingResponse> getRatingsByRange(Double minRating, Double maxRating, Pageable pageable);

    /**
     * Get ratings created after specific date
     */
    Page<RatingResponse> getRatingsAfterDate(LocalDateTime date, Pageable pageable);

    /**
     * Check if user has rated a movie
     */
    boolean hasUserRatedMovie(String userId, String movieId);
}