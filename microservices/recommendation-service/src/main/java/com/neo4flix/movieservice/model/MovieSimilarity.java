package com.neo4flix.movieservice.model;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Movie similarity relationship properties
 */
@RelationshipProperties
public class MovieSimilarity {

    @RelationshipId
    private Long id;

    @NotNull(message = "Similarity score is required")
    @DecimalMin(value = "0.0", message = "Similarity score cannot be less than 0")
    @DecimalMax(value = "1.0", message = "Similarity score cannot be more than 1")
    private Double score;

    private String reason;

    @TargetNode
    private Movie similarMovie;

    // Constructors
    public MovieSimilarity() {}

    public MovieSimilarity(Double score, String reason) {
        this.score = score;
        this.reason = reason;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Movie getSimilarMovie() {
        return similarMovie;
    }

    public void setSimilarMovie(Movie similarMovie) {
        this.similarMovie = similarMovie;
    }

    @Override
    public String toString() {
        return String.format("MovieSimilarity{id='%s', score=%.2f, reason='%s'}", id, score, reason);
    }
}