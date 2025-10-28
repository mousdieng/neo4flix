package com.neo4flix.movieservice.dto;

/**
 * DTO for movie recommendation projections from Cypher queries
 */
public class MovieRecommendationProjection {

    private String movieId;
    private String movieTitle;
    private Double movieRating;
    private Double score;

    public MovieRecommendationProjection() {}

    public MovieRecommendationProjection(String movieId, String movieTitle, Double movieRating, Double score) {
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.movieRating = movieRating;
        this.score = score;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return String.format("MovieRecommendationProjection{movieId='%s', title='%s', rating=%.2f, score=%.3f}",
                movieId, movieTitle, movieRating, score);
    }
}
