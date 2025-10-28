package com.neo4flix.ratingservice.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;

/**
 * Rating entity representing a user's rating of a movie
 * Stored as a standalone node in rating-service database
 */
@Data
@NoArgsConstructor
@ToString(of = {"id", "userId", "movieId", "rating", "ratedAt"})
@Node("Rating")
public class Rating {

    @Id
    @GeneratedValue
    private Long id;

    @Property("rating")
    private Double rating;

    @Property("review")
    private String review;

    @Property("ratedAt")
    private LocalDateTime ratedAt;

    @Property("lastModified")
    private LocalDateTime lastModified;

    @Property("movieId")
    private String movieId;

    @Property("userId")
    private String userId;

    @Property("username")
    private String username;

    // Custom initialization
    {
        this.ratedAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
    }

    public Rating(String userId, String username, String movieId, Double rating, String review) {
        this.userId = userId;
        this.username = username;
        this.movieId = movieId;
        this.rating = rating;
        this.review = review;
    }

    // Custom setters with side effects
    public void setRating(Double rating) {
        this.rating = rating;
        this.lastModified = LocalDateTime.now();
    }

    public void setReview(String review) {
        this.review = review;
        this.lastModified = LocalDateTime.now();
    }
}