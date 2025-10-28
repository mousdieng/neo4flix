package com.neo4flix.watchlistservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated watchlist response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistPageResponse {

    private List<WatchlistResponse> items;
    private Integer totalItems;
    private Integer page;
    private Integer pageSize;
    private Integer totalPages;
    private Boolean hasNext;
    private Boolean hasPrevious;

    // Statistics
    private WatchlistStats stats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WatchlistStats {
        private Integer totalMovies;
        private Integer watchedMovies;
        private Integer unwatchedMovies;
        private Integer highPriority;
        private Integer mediumPriority;
        private Integer lowPriority;
    }
}