package com.neo4flix.movieservice.exception;

/**
 * Exception thrown when attempting to create a duplicate movie
 */
public class DuplicateMovieException extends RuntimeException {

    public DuplicateMovieException(String message) {
        super(message);
    }

    public DuplicateMovieException(String message, Throwable cause) {
        super(message, cause);
    }
}