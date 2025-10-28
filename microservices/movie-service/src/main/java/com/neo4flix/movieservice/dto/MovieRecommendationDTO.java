package com.neo4flix.movieservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Simple DTO for movie recommendation query results.
 * Used to map Cypher query results from Neo4j.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MovieRecommendationDTO {
    private String movieId;
    private String movieTitle;
    private Double movieRating;
    private Double score;
    private List<String> genres;
}
