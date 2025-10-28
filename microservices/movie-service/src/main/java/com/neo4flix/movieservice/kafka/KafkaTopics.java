package com.neo4flix.movieservice.kafka;

public class KafkaTopics {
    // Movie Events
    public static final String MOVIE_CREATED = "movie.created";
    public static final String MOVIE_UPDATED = "movie.updated";
    public static final String MOVIE_DELETED = "movie.deleted";

    // Rating Events (consumed by movie service for statistics)
    public static final String RATING_CREATED = "rating.created";
    public static final String RATING_UPDATED = "rating.updated";
    public static final String RATING_DELETED = "rating.deleted";

    // User Events
    public static final String USER_CREATED = "user.created";
    public static final String USER_UPDATED = "user.updated";
    public static final String USER_DELETED = "user.deleted";

    // Watchlist Events
    public static final String WATCHLIST_ADDED = "watchlist.added";
    public static final String WATCHLIST_REMOVED = "watchlist.removed";

    // Recommendation Events
    public static final String RECOMMENDATION_REQUEST = "recommendation.request";
    public static final String RECOMMENDATION_RESPONSE = "recommendation.response";

    private KafkaTopics() {
        // Utility class
    }
}
