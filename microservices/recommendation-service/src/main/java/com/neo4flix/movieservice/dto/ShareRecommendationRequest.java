package com.neo4flix.movieservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for sharing a movie recommendation with friends
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareRecommendationRequest {

    @NotBlank(message = "Movie ID is required")
    private String movieId;

    @NotEmpty(message = "At least one friend must be selected")
    private List<String> friendIds;

    private String message;
}
