package com.neo4flix.movieservice.exception;

/**
 * Exception thrown when there's insufficient data to generate recommendations
 */
public class InsufficientDataException extends RuntimeException {

    public InsufficientDataException(String message) {
        super(message);
    }

    public InsufficientDataException(String userId, String algorithm) {
        super(String.format("Insufficient data to generate %s recommendations for user %s", algorithm, userId));
    }
}