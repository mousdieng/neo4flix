package com.neo4flix.movieservice.model;

/**
 * User roles for authorization
 */
public enum UserRole {
    USER("Regular user with basic permissions"),
    ADMIN("Administrator with full system access"),
    MODERATOR("Moderator with content management permissions");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}