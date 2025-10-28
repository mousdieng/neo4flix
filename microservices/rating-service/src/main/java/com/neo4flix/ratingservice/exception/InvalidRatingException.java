package com.neo4flix.ratingservice.exception;

/**
 * Exception thrown when a rating value is invalid
 */
public class InvalidRatingException extends RuntimeException {

    public InvalidRatingException(String message) {
        super(message);
    }

    public InvalidRatingException(Double rating) {
        super(String.format("Invalid rating value: %.1f. Rating must be between 0.5 and 5.0", rating));
    }
}