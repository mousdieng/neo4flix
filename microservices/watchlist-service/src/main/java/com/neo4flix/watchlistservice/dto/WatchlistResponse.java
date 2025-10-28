package com.neo4flix.watchlistservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for watchlist response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistResponse {

    private String id;
    private String userId;
    private String movieId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime addedAt;

    private Integer priority;
    private String priorityLabel; // "High", "Medium", "Low"
    private String notes;
    private Boolean watched;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime watchedAt;

    /**
     * Helper method to set priority label based on priority value
     */
    public void setPriority(Integer priority) {
        this.priority = priority;
        if (priority != null) {
            switch (priority) {
                case 1:
                    this.priorityLabel = "High";
                    break;
                case 2:
                    this.priorityLabel = "Medium";
                    break;
                case 3:
                    this.priorityLabel = "Low";
                    break;
                default:
                    this.priorityLabel = "Unknown";
            }
        }
    }
}