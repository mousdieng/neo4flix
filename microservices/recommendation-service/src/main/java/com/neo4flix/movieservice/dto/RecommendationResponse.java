package com.neo4flix.movieservice.dto;

import com.neo4flix.movieservice.model.Recommendation;
import java.time.LocalDateTime;

/**
 * Response DTO for recommendation information
 */
public class RecommendationResponse {

    private String id;
    private String movieId;
    private String movieTitle;
    private String moviePlot;
    private Integer movieYear;
    private Double movieRating;
    private Double score;
    private String algorithm;
    private String reason;
    private LocalDateTime recommendedAt;
    private Boolean clicked;
    private Boolean watched;

    public RecommendationResponse() {}

    public RecommendationResponse(Recommendation recommendation) {
        this.id = recommendation.getId() != null ? recommendation.getId().toString() : null;
        this.movieId = recommendation.getMovieId();
        this.score = recommendation.getScore();
        this.algorithm = recommendation.getAlgorithm();
        this.reason = recommendation.getReason();
        this.recommendedAt = recommendation.getRecommendedAt();
        this.clicked = recommendation.getClicked();
        this.watched = recommendation.getWatched();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getMoviePlot() {
        return moviePlot;
    }

    public void setMoviePlot(String moviePlot) {
        this.moviePlot = moviePlot;
    }

    public Integer getMovieYear() {
        return movieYear;
    }

    public void setMovieYear(Integer movieYear) {
        this.movieYear = movieYear;
    }

    public Double getMovieRating() {
        return movieRating;
    }

    public void setMovieRating(Double movieRating) {
        this.movieRating = movieRating;
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
}