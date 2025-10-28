package com.neo4flix.movieservice.model;

import org.springframework.data.neo4j.core.schema.*;

/**
 * Actor entity for content-based recommendations
 */
@Node("Actor")
public class Actor {

    @Id
    private String name;

    @Property("bio")
    private String bio;

    @Property("birthYear")
    private Integer birthYear;

    public Actor() {}

    public Actor(String name) {
        this.name = name;
    }

    public Actor(String name, Integer birthYear) {
        this.name = name;
        this.birthYear = birthYear;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }

    @Override
    public String toString() {
        return String.format("Actor{name='%s', birthYear=%d}", name, birthYear);
    }
}