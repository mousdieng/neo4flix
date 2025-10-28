package com.neo4flix.watchlistservice.exception;

/**
 * Exception thrown when attempting to create a duplicate rating
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String userId, String movieId) {
        super(String.format("Resource already exists for user %s and movie %s", userId, movieId));
    }
}