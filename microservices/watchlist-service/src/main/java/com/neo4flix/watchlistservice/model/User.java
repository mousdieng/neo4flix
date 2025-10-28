package com.neo4flix.watchlistservice.model;

import org.springframework.data.neo4j.core.schema.*;

/**
 * User entity for Watchlist Service (minimal representation)
 */
@Node("User")
public class User {

    @Id
    private String id;

    @Property("username")
    private String username;

    @Property("email")
    private String email;

    public User() {}

    public User(String id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}