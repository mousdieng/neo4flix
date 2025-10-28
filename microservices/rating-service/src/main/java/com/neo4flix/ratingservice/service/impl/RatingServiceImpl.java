package com.neo4flix.ratingservice.service.impl;

import com.neo4flix.ratingservice.client.RecommendationClient;
import com.neo4flix.ratingservice.dto.*;
import com.neo4flix.ratingservice.exception.*;
import com.neo4flix.ratingservice.model.Rating;
import com.neo4flix.ratingservice.repository.MovieRepository;
import com.neo4flix.ratingservice.repository.RatingRepository;
import com.neo4flix.ratingservice.repository.UserRepository;
import com.neo4flix.ratingservice.service.RatingService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of RatingService
 */
@Service
@Transactional
public class RatingServiceImpl implements RatingService {

    private static final Logger logger = LoggerFactory.getLogger(RatingServiceImpl.class);

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final RecommendationClient recommendationClient;

    @Autowired
    public RatingServiceImpl(RatingRepository ratingRepository,
                           UserRepository userRepository,
                           MovieRepository movieRepository,
                           RecommendationClient recommendationClient) {
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.recommendationClient = recommendationClient;
    }

    @Override
    public RatingResponse createRating(String userId, String username, CreateRatingRequest request) {
        logger.info("Creating rating for user {} and movie {}", userId, request.getMovieId());

        validateRatingValue(request.getRating());

        // Check if user already rated this movie
        Optional<Rating> existingRating = ratingRepository.findByUserIdAndMovieId(userId, request.getMovieId());
        if (existingRating.isPresent()) {
            throw new DuplicateRatingException(userId, request.getMovieId());
        }

        // Note: Movie existence is validated by the movie-service
        // Rating service trusts the movieId provided by the client

        // Create new rating
        Rating rating = new Rating(userId, username, request.getMovieId(), request.getRating(), request.getReview());
        Rating savedRating = ratingRepository.save(rating);

        // Update movie rating statistics
        ratingRepository.updateMovieRatingStats(request.getMovieId());

        logger.info("Rating created successfully with id: {}", savedRating.getId());

        // Trigger recommendation generation asynchronously
        triggerRecommendationRefresh(userId);

        return new RatingResponse(savedRating);
    }

    @Override
    public RatingResponse updateRating(String userId, String movieId, UpdateRatingRequest request) {
        logger.info("Updating rating for user {} and movie {}", userId, movieId);

        validateRatingValue(request.getRating());

        Rating rating = ratingRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new RatingNotFoundException(userId, movieId));

        rating.setRating(request.getRating());
        rating.setReview(request.getReview());
        rating.setLastModified(LocalDateTime.now());

        Rating updatedRating = ratingRepository.save(rating);

        // Update movie rating statistics
        ratingRepository.updateMovieRatingStats(movieId);

        logger.info("Rating updated successfully with id: {}", updatedRating.getId());

        // Trigger recommendation generation asynchronously
        triggerRecommendationRefresh(userId);

        return new RatingResponse(updatedRating);
    }

    @Override
    public void deleteRating(String userId, String movieId) {
        logger.info("Deleting rating for user {} and movie {}", userId, movieId);

        if (!ratingRepository.findByUserIdAndMovieId(userId, movieId).isPresent()) {
            throw new RatingNotFoundException(userId, movieId);
        }

        ratingRepository.deleteByUserIdAndMovieId(userId, movieId);

        // Update movie rating statistics
        ratingRepository.updateMovieRatingStats(movieId);

        logger.info("Rating deleted successfully for user {} and movie {}", userId, movieId);
    }

    @Override
    @Transactional(readOnly = true)
    public RatingResponse getRating(String userId, String movieId) {
        Rating rating = ratingRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new RatingNotFoundException(userId, movieId));

        return new RatingResponse(rating);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RatingResponse> getUserRatings(String userId, Pageable pageable) {
        Page<Rating> ratings = ratingRepository.findByUserId(userId, pageable);
        return ratings.map(RatingResponse::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RatingResponse> getMovieRatings(String movieId, Pageable pageable) {
        Page<Rating> ratings = ratingRepository.findByMovieId(movieId, pageable);
        return ratings.map(RatingResponse::new);
    }

    @Override
    @Transactional(readOnly = true)
    public MovieRatingStats getMovieRatingStats(String movieId) {
        return ratingRepository.getMovieRatingStats(movieId)
                .orElseThrow(() -> new IllegalArgumentException("No ratings found for movie: " + movieId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingResponse> getRecentRatings(int limit) {
        List<Rating> recentRatings = ratingRepository.findRecentRatings(limit);
        return recentRatings.stream()
                .map(RatingResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieRatingStats> getTopRatedMovies(int minRatings, int limit) {
        return ratingRepository.findTopRatedMovies(minRatings, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getUserAverageRating(String userId) {
        return ratingRepository.getUserAverageRating(userId).orElse(0.0);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getUserRatingsCount(String userId) {
        return ratingRepository.countByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getMovieRatingsCount(String movieId) {
        return ratingRepository.countByMovieId(movieId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RatingResponse> getRatingsByRange(Double minRating, Double maxRating, Pageable pageable) {
        validateRatingValue(minRating);
        validateRatingValue(maxRating);

        if (minRating > maxRating) {
            throw new IllegalArgumentException("Minimum rating cannot be greater than maximum rating");
        }

        Page<Rating> ratings = ratingRepository.findByRatingBetween(minRating, maxRating, pageable);
        return ratings.map(RatingResponse::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RatingResponse> getRatingsAfterDate(LocalDateTime date, Pageable pageable) {
        Page<Rating> ratings = ratingRepository.findByRatedAtAfter(date, pageable);
        return ratings.map(RatingResponse::new);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserRatedMovie(String userId, String movieId) {
        return ratingRepository.findByUserIdAndMovieId(userId, movieId).isPresent();
    }

    private void validateRatingValue(Double rating) {
        if (rating == null || rating < 0.5 || rating > 10.0) {
            throw new InvalidRatingException(rating);
        }
    }

    /**
     * Trigger recommendation generation for a user asynchronously
     * This method extracts the JWT token from the current request and calls the recommendation service
     */
    private void triggerRecommendationRefresh(String userId) {
        try {
            String jwtToken = extractJwtToken();
            if (jwtToken != null) {
                logger.info("Triggering recommendation refresh for user: {}", userId);
                recommendationClient.triggerRecommendationRefresh(userId, jwtToken);
            } else {
                logger.warn("Cannot trigger recommendation refresh - no JWT token found in request");
            }
        } catch (Exception e) {
            // Don't fail the rating operation if recommendation trigger fails
            logger.error("Error triggering recommendation refresh for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Extract JWT token from the current HTTP request
     */
    private String extractJwtToken() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    return authHeader.substring(7); // Remove "Bearer " prefix
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract JWT token from request: {}", e.getMessage());
        }
        return null;
    }
}