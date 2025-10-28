package com.neo4flix.movieservice.dto;

import com.neo4flix.movieservice.model.Movie;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Simple DTO for movie recommendation query results.
 * Used to map Cypher query results from Neo4j.
 */
@Data
@AllArgsConstructor
public class MovieRecommendationDTO {
    private Movie movie;
    private Double movieRating;
    private Double score;
    private List<String> genres;
}
