package com.neo4flix.movieservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.LocalDateTime;

/**
 * Represents a movie recommendation shared from one user to another
 */
@Node("SharedRecommendation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedRecommendation {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;

    @Property("from_user_id")
    private String fromUserId;

    @Property("to_user_id")
    private String toUserId;

    @Property("movie_id")
    private String movieId;

    @Property("message")
    private String message;

    @Property("shared_at")
    private LocalDateTime sharedAt;

    @Property("viewed")
    @Builder.Default
    private boolean viewed = false;

    @Property("viewed_at")
    private LocalDateTime viewedAt;

    public void markAsViewed() {
        this.viewed = true;
        this.viewedAt = LocalDateTime.now();
    }
}
