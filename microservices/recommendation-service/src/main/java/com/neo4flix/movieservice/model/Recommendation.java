package com.neo4flix.movieservice.model;

import lombok.*;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;

/**
 * Represents a Recommendation relationship between a User and a Movie.
 * Stores metadata such as the algorithm used, the confidence score,
 * and user interactions (clicked/watched).
 */
@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class Recommendation {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * Recommendation confidence score.
     * The higher the score, the more relevant the movie is expected to be for the user.
     */
    @Property("score")
    @ToString.Include
    private Double score;

    /** Algorithm name used to generate the recommendation (e.g. "content-based", "collaborative"). */
    @Property("algorithm")
    @ToString.Include
    private String algorithm;

    /** Optional explanation or justification for the recommendation. */
    @Property("reason")
    private String reason;

    /** Timestamp when the recommendation was generated. */
    @Property("recommendedAt")
    @Builder.Default
    private LocalDateTime recommendedAt = LocalDateTime.now();

    /** Whether the user clicked the recommendation. */
    @Property("clicked")
    @Builder.Default
    private Boolean clicked = false;

    /** Whether the user watched the recommended movie. */
    @Property("watched")
    @Builder.Default
    private Boolean watched = false;

    /** The recommended movie’s unique ID (target node reference). */
    @TargetNode
    private String movieId;

    /** The user for whom the recommendation was generated. */
    @Property("userId")
    private String userId;
}
