package com.neo4flix.movieservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
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
}