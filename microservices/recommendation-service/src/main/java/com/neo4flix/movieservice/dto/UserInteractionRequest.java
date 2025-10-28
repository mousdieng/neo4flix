package com.neo4flix.movieservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for tracking user interactions
 */
public class UserInteractionRequest {

    private String userId; // Set by controller from auth context

    @NotBlank(message = "Movie ID is required")
    private String movieId;

    @NotNull(message = "Action is required")
    private String action; // view, rate, like, share, watchlist_add, watchlist_remove

    private Double value; // For rating action (0.5-10.0)

    private String timestamp; // ISO format timestamp

    public UserInteractionRequest() {}

    public UserInteractionRequest(String userId, String movieId, String action, Double value, String timestamp) {
        this.userId = userId;
        this.movieId = movieId;
        this.action = action;
        this.value = value;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
