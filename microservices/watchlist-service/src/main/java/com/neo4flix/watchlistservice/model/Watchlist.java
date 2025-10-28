package com.neo4flix.watchlistservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;

/**
 * Watchlist Node Entity
 * Represents a user's watchlist entry for a movie
 */
@Node("Watchlist")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Watchlist {

    @Id
    private String id;

    @Property("userId")
    private String userId;

    @Property("movieId")
    private String movieId;

    @Property("addedAt")
    private LocalDateTime addedAt;

    @Property("priority")
    private Integer priority; // 1 = High, 2 = Medium, 3 = Low

    @Property("notes")
    private String notes;

    @Property("watched")
    private Boolean watched;

    @Property("watchedAt")
    private LocalDateTime watchedAt;
}
