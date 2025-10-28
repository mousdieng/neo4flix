package com.neo4flix.movieservice.model;

import org.springframework.data.neo4j.core.schema.*;

/**
 * Genre entity for content-based recommendations
 */
@Node("Genre")
public class Genre {

    @Id
    private String name;

    @Property("description")
    private String description;

    public Genre() {}

    public Genre(String name) {
        this.name = name;
    }

    public Genre(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return String.format("Genre{name='%s'}", name);
    }
}