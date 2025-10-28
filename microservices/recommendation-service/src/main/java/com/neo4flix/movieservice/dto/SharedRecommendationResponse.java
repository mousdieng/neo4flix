package com.neo4flix.movieservice.dto;

import com.neo4flix.movieservice.model.Movie;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for shared recommendations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedRecommendationResponse {

    private String id;
    private String fromUserId;
    private String fromUsername;
    private String toUserId;
    private Movie movie;
    private String message;
    private LocalDateTime sharedAt;
    private boolean viewed;
    private LocalDateTime viewedAt;
}
