package com.neo4flix.ratingservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingCreatedEvent {
    private String ratingId;
    private String userId;
    private String movieId;
    private Double rating;
    private String review;
    private LocalDateTime timestamp;

    {
        this.timestamp = LocalDateTime.now();
    }

    public RatingCreatedEvent(String ratingId, String userId, String movieId, Double rating, String review) {
        this.ratingId = ratingId;
        this.userId = userId;
        this.movieId = movieId;
        this.rating = rating;
        this.review = review;
    }
}
