package com.neo4flix.watchlistservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for marking a movie as watched
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkAsWatchedRequest {

    private String userId;
    private String movieId;

    @Builder.Default
    private Boolean watched = true;
}