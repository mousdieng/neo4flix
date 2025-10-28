package com.neo4flix.watchlistservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for checking if a movie is in watchlist
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistCheckResponse {

    private Boolean inWatchlist;
    private String watchlistId;
    private Integer priority;
    private Boolean watched;
}