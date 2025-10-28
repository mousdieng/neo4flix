package com.neo4flix.ratingservice.exception;

/**
 * Exception thrown when attempting to create a duplicate rating
 */
public class DuplicateRatingException extends RuntimeException {

    public DuplicateRatingException(String message) {
        super(message);
    }

    public DuplicateRatingException(String userId, String movieId) {
        super(String.format("Rating already exists for user %s and movie %s", userId, movieId));
    }
}