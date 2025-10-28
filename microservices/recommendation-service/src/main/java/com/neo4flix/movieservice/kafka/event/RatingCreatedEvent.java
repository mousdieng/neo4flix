package com.neo4flix.movieservice.kafka.event;

import java.time.LocalDateTime;

public class RatingCreatedEvent {
    private String ratingId;
    private String userId;
    private String movieId;
    private Double rating;
    private String review;
    private LocalDateTime timestamp;

    public RatingCreatedEvent() {
    }

    public RatingCreatedEvent(String ratingId, String userId, String movieId, Double rating, String review) {
        this.ratingId = ratingId;
        this.userId = userId;
        this.movieId = movieId;
        this.rating = rating;
        this.review = review;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getRatingId() {
        return ratingId;
    }

    public void setRatingId(String ratingId) {
        this.ratingId = ratingId;
    }

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

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "RatingCreatedEvent{" +
                "ratingId='" + ratingId + '\'' +
                ", userId='" + userId + '\'' +
                ", movieId='" + movieId + '\'' +
                ", rating=" + rating +
                ", timestamp=" + timestamp +
                '}';
    }
}
