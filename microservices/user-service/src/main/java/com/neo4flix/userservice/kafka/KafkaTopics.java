package com.neo4flix.userservice.kafka;

public class KafkaTopics {
    // User Events
    public static final String USER_CREATED = "user.created";
    public static final String USER_UPDATED = "user.updated";
    public static final String USER_DELETED = "user.deleted";

    private KafkaTopics() {
        // Utility class
    }
}
