package com.neo4flix.ratingservice.kafka;

public class KafkaTopics {
    // Rating Events
    public static final String RATING_CREATED = "rating.created";
    public static final String RATING_UPDATED = "rating.updated";
    public static final String RATING_DELETED = "rating.deleted";

    // Movie Events (consumed by rating service)
    public static final String MOVIE_CREATED = "movie.created";
    public static final String MOVIE_UPDATED = "movie.updated";
    public static final String MOVIE_DELETED = "movie.deleted";

    // User Events
    public static final String USER_CREATED = "user.created";
    public static final String USER_DELETED = "user.deleted";

    private KafkaTopics() {
        // Utility class
    }
}
