package com.neo4flix.movieservice.kafka;

public class KafkaTopics {
    // Rating Events (consumed by recommendation service)
    public static final String RATING_CREATED = "rating.created";
    public static final String RATING_UPDATED = "rating.updated";
    public static final String RATING_DELETED = "rating.deleted";

    // Movie Events
    public static final String MOVIE_CREATED = "movie.created";
    public static final String MOVIE_UPDATED = "movie.updated";
    public static final String MOVIE_DELETED = "movie.deleted";

    // User Events
    public static final String USER_CREATED = "user.created";
    public static final String USER_DELETED = "user.deleted";

    // Watchlist Events
    public static final String WATCHLIST_ADDED = "watchlist.added";
    public static final String WATCHLIST_REMOVED = "watchlist.removed";

    private KafkaTopics() {
        // Utility class
    }
}
