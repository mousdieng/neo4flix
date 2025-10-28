package com.neo4flix.watchlistservice.kafka;

public class KafkaTopics {
    // Watchlist Events
    public static final String WATCHLIST_ADDED = "watchlist.added";
    public static final String WATCHLIST_REMOVED = "watchlist.removed";

    // Movie Events (consumed by watchlist service)
    public static final String MOVIE_CREATED = "movie.created";
    public static final String MOVIE_DELETED = "movie.deleted";

    // User Events
    public static final String USER_DELETED = "user.deleted";

    private KafkaTopics() {
        // Utility class
    }
}
