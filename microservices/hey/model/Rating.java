package com.neo4flix.movieservice.model;

import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;

/**
 * Rating relationship for Recommendation Service
 */
@RelationshipProperties
public class Rating {

    @Id
    @GeneratedValue
    private Long id;

    @Property("rating")
    private Double rating;

    @Property("ratedAt")
    private LocalDateTime ratedAt;

    @TargetNode
    private String movieId;

    @Property("userId")
    private String userId;

    public Rating() {
        this.ratedAt = LocalDateTime.now();
    }

    public Rating(String userId, String movieId, Double rating) {
        this();
        this.userId = userId;
        this.movieId = movieId;
        this.rating = rating;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public LocalDateTime getRatedAt() {
        return ratedAt;
    }

    public void setRatedAt(LocalDateTime ratedAt) {
        this.ratedAt = ratedAt;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}