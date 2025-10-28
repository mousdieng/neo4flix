package com.neo4flix.movieservice.model;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Rating relationship properties between User and Movie
 */
@RelationshipProperties
public class Rating {

    @RelationshipId
    private Long id;

    @NotNull(message = "Rating value is required")
    @DecimalMin(value = "0.0", message = "Rating cannot be less than 0")
    @DecimalMax(value = "10.0", message = "Rating cannot be more than 10")
    private Double rating;

    private String review;

    private LocalDateTime timestamp;

    @TargetNode
    private Movie movie;

    // Constructors
    public Rating() {
        this.timestamp = LocalDateTime.now();
    }

    public Rating(Double rating, String review) {
        this();
        this.rating = rating;
        this.review = review;
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

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    @Override
    public String toString() {
        return String.format("Rating{id='%s', rating=%.1f, timestamp=%s}", id, rating, timestamp);
    }
}