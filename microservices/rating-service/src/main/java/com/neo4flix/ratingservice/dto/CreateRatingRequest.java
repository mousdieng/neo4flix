package com.neo4flix.ratingservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new rating
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRatingRequest {

    @NotBlank(message = "Movie ID is required")
    private String movieId;

    @NotNull(message = "Rating is required")
    @DecimalMin(value = "0.5", message = "Rating must be at least 0.5")
    @DecimalMax(value = "10.0", message = "Rating must be at most 10.0")
    private Double rating; // 0.5-10.0 scale (0.5 increments, matching IMDB scale)

    @Size(max = 1000, message = "Review must not exceed 1000 characters")
    private String review;
}