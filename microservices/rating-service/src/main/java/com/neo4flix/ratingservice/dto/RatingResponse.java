package com.neo4flix.ratingservice.dto;

import com.neo4flix.ratingservice.model.Rating;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for rating information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponse {

    private String id;
    private String userId;
    private String username;
    private String movieId;
    private String movieTitle;
    private Double rating;
    private String review;
    private LocalDateTime ratedAt;
    private LocalDateTime lastModified;

    public RatingResponse(Rating rating) {
        this.id = rating.getId() != null ? rating.getId().toString() : null;
        this.userId = rating.getUserId();
        this.username = rating.getUsername();
        this.movieId = rating.getMovieId();
        this.rating = rating.getRating();
        this.review = rating.getReview();
        this.ratedAt = rating.getRatedAt();
        this.lastModified = rating.getLastModified();
    }

    public RatingResponse(Rating rating, String movieTitle) {
        this(rating);
        this.movieTitle = movieTitle;
    }
}