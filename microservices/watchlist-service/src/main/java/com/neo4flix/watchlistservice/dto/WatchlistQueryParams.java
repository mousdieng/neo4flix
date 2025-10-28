package com.neo4flix.watchlistservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for watchlist query parameters
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistQueryParams {

    @Builder.Default
    @Min(value = 0, message = "Page number must be non-negative")
    private Integer page = 0;

    @Builder.Default
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private Integer pageSize = 20;

    private Integer priority; // Filter by priority (1, 2, or 3)

    @Builder.Default
    private Boolean watched = false; // Filter by watched status

    private String sortBy; // "addedAt", "priority", "title"

    @Builder.Default
    private String sortDirection = "DESC"; // "ASC" or "DESC"

    private String[] genres; // Filter by genres

    private Integer fromYear;
    private Integer toYear;
}