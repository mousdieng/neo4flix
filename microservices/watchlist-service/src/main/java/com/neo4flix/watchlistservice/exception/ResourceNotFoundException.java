package com.neo4flix.watchlistservice.exception;

/**
 * Exception thrown when a rating is not found
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String userId, String movieId) {
        super(String.format("Ressource not found for user %s and movie %s", userId, movieId));
    }
}