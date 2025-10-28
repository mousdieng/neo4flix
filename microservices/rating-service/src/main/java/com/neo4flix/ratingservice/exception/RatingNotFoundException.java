package com.neo4flix.ratingservice.exception;

/**
 * Exception thrown when a rating is not found
 */
public class RatingNotFoundException extends RuntimeException {

    public RatingNotFoundException(String message) {
        super(message);
    }

    public RatingNotFoundException(String userId, String movieId) {
        super(String.format("Rating not found for user %s and movie %s", userId, movieId));
    }
}