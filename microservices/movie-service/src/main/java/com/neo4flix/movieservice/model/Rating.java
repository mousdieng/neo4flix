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
import java.time.LocalDateTime;

/**
 * Rating relationship properties between User and Movie
 */
@RelationshipProperties
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
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
}