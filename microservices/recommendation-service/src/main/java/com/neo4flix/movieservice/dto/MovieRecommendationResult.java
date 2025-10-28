package com.neo4flix.movieservice.dto;

/**
 * Interface-based projection for movie recommendation query results.
 * Spring Data Neo4j handles interface projections better than class-based DTOs for custom queries.
 */
public interface MovieRecommendationResult {
    String getMovieId();
    String getMovieTitle();
    Double getMovieRating();
    Double getScore();
}
