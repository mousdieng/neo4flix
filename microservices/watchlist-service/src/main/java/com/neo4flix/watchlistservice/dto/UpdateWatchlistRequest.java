package com.neo4flix.watchlistservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating watchlist entry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWatchlistRequest {

    @Min(value = 1, message = "Priority must be between 1 and 3")
    @Max(value = 3, message = "Priority must be between 1 and 3")
    private Integer priority;

    private String notes;

    private Boolean watched;
}