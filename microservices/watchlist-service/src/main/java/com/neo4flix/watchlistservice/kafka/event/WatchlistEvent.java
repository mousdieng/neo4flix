package com.neo4flix.watchlistservice.kafka.event;

import java.time.LocalDateTime;

public class WatchlistEvent {
    private String userId;
    private String movieId;
    private String action; // ADDED or REMOVED
    private LocalDateTime timestamp;

    public WatchlistEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public WatchlistEvent(String userId, String movieId, String action) {
        this.userId = userId;
        this.movieId = movieId;
        this.action = action;
        this.timestamp = LocalDateTime.now();
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "WatchlistEvent{" +
                "userId='" + userId + '\'' +
                ", movieId='" + movieId + '\'' +
                ", action='" + action + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
