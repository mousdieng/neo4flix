package com.neo4flix.movieservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for generating recommendations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit cannot exceed 100")
    private Integer limit = 10;

    @Pattern(regexp = "collaborative|content|hybrid|popular",
             message = "Algorithm must be one of: collaborative, content, hybrid, popular")
    private String algorithm = "hybrid";

    @Builder.Default
    private Double minRating = 3.0;

    @Builder.Default
    private Double minAverageMovieRating = 0.0;

    @Builder.Default
    private Boolean includeWatched = false;
    private List<String> genre;
    private Integer fromYear;
    private Integer toYear;

    public double getSafeMinRating() {
        return minRating != null ? minRating : 3.0;
    }

    public double getSafeMinAverageMovieRating() {
        return minAverageMovieRating != null ? minAverageMovieRating : 0.0;
    }
}