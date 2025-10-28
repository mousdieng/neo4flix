package com.neo4flix.movieservice.dto;

/**
 * DTO for movie similarity calculations in content-based filtering
 */
public class MovieSimilarity {

    private String movieId1;
    private String movieId2;
    private String movieTitle1;
    private String movieTitle2;
    private Double similarity;
    private String similarityType;

    public MovieSimilarity() {}

    public MovieSimilarity(String movieId1, String movieId2, Double similarity, String similarityType) {
        this.movieId1 = movieId1;
        this.movieId2 = movieId2;
        this.similarity = similarity;
        this.similarityType = similarityType;
    }

    // Getters and Setters
    public String getMovieId1() {
        return movieId1;
    }

    public void setMovieId1(String movieId1) {
        this.movieId1 = movieId1;
    }

    public String getMovieId2() {
        return movieId2;
    }

    public void setMovieId2(String movieId2) {
        this.movieId2 = movieId2;
    }

    public String getMovieTitle1() {
        return movieTitle1;
    }

    public void setMovieTitle1(String movieTitle1) {
        this.movieTitle1 = movieTitle1;
    }

    public String getMovieTitle2() {
        return movieTitle2;
    }

    public void setMovieTitle2(String movieTitle2) {
        this.movieTitle2 = movieTitle2;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    public String getSimilarityType() {
        return similarityType;
    }

    public void setSimilarityType(String similarityType) {
        this.similarityType = similarityType;
    }

    @Override
    public String toString() {
        return String.format("MovieSimilarity{movie1='%s', movie2='%s', similarity=%.3f, type='%s'}",
                movieId1, movieId2, similarity, similarityType);
    }
}