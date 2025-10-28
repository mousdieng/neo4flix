package com.neo4flix.watchlistservice.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for adding a movie to watchlist
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToWatchlistRequest {

    private String userId;

    @NotBlank(message = "Movie ID is required")
    private String movieId;

    @Min(value = 1, message = "Priority must be between 1 and 3")
    @Max(value = 3, message = "Priority must be between 1 and 3")
    @Builder.Default
    private Integer priority = 2; // Default: Medium priority

    private String notes;
}
