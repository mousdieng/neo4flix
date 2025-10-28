package com.neo4flix.movieservice.exception;

/**
 * Exception thrown when a recommendation is not found
 */
public class RecommendationNotFoundException extends RuntimeException {

    public RecommendationNotFoundException(String message) {
        super(message);
    }

    public RecommendationNotFoundException(String userId, String movieId) {
        super(String.format("Recommendation not found for user %s and movie %s", userId, movieId));
    }
}