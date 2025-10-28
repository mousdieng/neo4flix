package com.neo4flix.movieservice.service;

import com.neo4flix.movieservice.dto.RecommendationRequest;
import com.neo4flix.movieservice.dto.RecommendationResponse;
import com.neo4flix.movieservice.dto.UserSimilarity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for recommendation operations
 */
public interface RecommendationService {

    /**
     * Generate recommendations for a user
     */
    List<RecommendationResponse> generateRecommendations(RecommendationRequest request);

    /**
     * Get existing recommendations for a user
     */
    Page<RecommendationResponse> getUserRecommendations(String userId, Pageable pageable);

    /**
     * Get recommendations by algorithm
     */
    Page<RecommendationResponse> getUserRecommendationsByAlgorithm(String userId, String algorithm, Pageable pageable);

    /**
     * Update recommendation interaction (clicked)
     */
    void markRecommendationClicked(String userId, String movieId);

    /**
     * Update recommendation interaction (watched)
     */
    void markRecommendationWatched(String userId, String movieId);

    /**
     * Refresh recommendations for a user (delete old and generate new)
     */
    List<RecommendationResponse> refreshRecommendations(String userId, String algorithm, Integer limit);

    /**
     * Get recommendation statistics for a user
     */
    Object getRecommendationStats(String userId);

    /**
     * Find similar users for a given user
     */
    List<UserSimilarity> findSimilarUsers(String userId, Integer limit);

    /**
     * Get trending recommendations (popular among similar users)
     */
    List<RecommendationResponse> getTrendingRecommendations(String userId, Integer limit);

    /**
     * Get recommendations by genre
     */
    List<RecommendationResponse> getRecommendationsByGenre(RecommendationRequest request);

    /**
     * Get recommendations for new users (cold start problem)
     */
    List<RecommendationResponse> getNewUserRecommendations(Integer limit);

    /**
     * Calculate recommendation accuracy metrics
     */
    Object calculateRecommendationMetrics(String userId);

    /**
     * Batch generate recommendations for multiple users
     */
    void batchGenerateRecommendations(List<String> userIds, String algorithm);

    /**
     * Clean up old recommendations
     */
    void cleanupOldRecommendations(Integer daysOld);

    /**
     * Get similar movies (content-based recommendations for a specific movie)
     */
    List<RecommendationResponse> getSimilarMovies(String movieId, Integer limit);

    /**
     * Track user interaction with a movie
     */
    void trackUserInteraction(com.neo4flix.movieservice.dto.UserInteractionRequest interaction);

    // ==================== SHARING WITH FRIENDS ====================

    /**
     * Share a movie recommendation with friends
     */
    int shareRecommendation(String userId, com.neo4flix.movieservice.dto.ShareRecommendationRequest request);

    /**
     * Get recommendations shared with the user by friends
     */
    List<com.neo4flix.movieservice.dto.SharedRecommendationResponse> getSharedRecommendations(String userId);

    /**
     * Get recommendations the user has shared with friends
     */
    List<com.neo4flix.movieservice.dto.SharedRecommendationResponse> getMySharedRecommendations(String userId);

    /**
     * Mark a shared recommendation as viewed
     */
    void markSharedRecommendationAsViewed(String sharedRecommendationId);

    /**
     * Get count of unviewed shared recommendations
     */
    long getUnviewedSharedCount(String userId);
}