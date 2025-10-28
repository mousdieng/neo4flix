package com.neo4flix.movieservice.model;

import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;

/**
 * Recommendation relationship for storing generated recommendations
 */
@RelationshipProperties
public class Recommendation {

    @Id
    @GeneratedValue
    private Long id;

    @Property("score")
    private Double score;

    @Property("algorithm")
    private String algorithm;

    @Property("reason")
    private String reason;

    @Property("recommendedAt")
    private LocalDateTime recommendedAt;

    @Property("clicked")
    private Boolean clicked = false;

    @Property("watched")
    private Boolean watched = false;

    @TargetNode
    private String movieId;

    @Property("userId")
    private String userId;

    public Recommendation() {
        this.recommendedAt = LocalDateTime.now();
    }

    public Recommendation(String userId, String movieId, Double score, String algorithm, String reason) {
        this();
        this.userId = userId;
        this.movieId = movieId;
        this.score = score;
        this.algorithm = algorithm;
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

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getRecommendedAt() {
        return recommendedAt;
    }

    public void setRecommendedAt(LocalDateTime recommendedAt) {
        this.recommendedAt = recommendedAt;
    }

    public Boolean getClicked() {
        return clicked;
    }

    public void setClicked(Boolean clicked) {
        this.clicked = clicked;
    }

    public Boolean getWatched() {
        return watched;
    }

    public void setWatched(Boolean watched) {
        this.watched = watched;
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

    @Override
    public String toString() {
        return String.format("Recommendation{userId='%s', movieId='%s', score=%.2f, algorithm='%s'}",
                userId, movieId, score, algorithm);
    }
}