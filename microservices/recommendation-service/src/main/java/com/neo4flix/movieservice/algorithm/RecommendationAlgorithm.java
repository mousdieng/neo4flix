package com.neo4flix.movieservice.algorithm;

import com.neo4flix.movieservice.dto.RecommendationRequest;
import com.neo4flix.movieservice.model.Recommendation;

import java.util.List;

/**
 * Interface for recommendation algorithms
 */
public interface RecommendationAlgorithm {

    /**
     * Get the name/identifier of this algorithm
     */
    String getAlgorithmName();

    /**
     * Generate recommendations for a user based on the request
     */
    List<Recommendation> generateRecommendations(RecommendationRequest request);

    /**
     * Calculate a score for a specific user-movie combination
     */
    double calculateScore(String userId, String movieId);

    /**
     * Check if this algorithm is applicable for the given request
     */
    boolean isApplicable(RecommendationRequest request);
}