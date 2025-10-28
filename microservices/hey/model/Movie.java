package com.neo4flix.movieservice.model;

import org.springframework.data.neo4j.core.schema.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Movie entity for Recommendation Service
 */
@Node("Movie")
public class Movie {

    @Id
    private String id;

    @Property("title")
    private String title;

    @Property("plot")
    private String plot;

    @Property("releaseYear")
    private Integer releaseYear;

    @Property("runtime")
    private Integer runtime;

    @Property("averageRating")
    private Double averageRating = 0.0;

    @Property("ratingCount")
    private Integer ratingCount = 0;

    @Property("popularity")
    private Double popularity = 0.0;

    @Relationship(type = "IN_GENRE", direction = Relationship.Direction.OUTGOING)
    private Set<Genre> genres = new HashSet<>();

    @Relationship(type = "DIRECTED", direction = Relationship.Direction.INCOMING)
    private Set<Director> directors = new HashSet<>();

    @Relationship(type = "ACTED_IN", direction = Relationship.Direction.INCOMING)
    private Set<Actor> actors = new HashSet<>();

    public Movie() {}

    public Movie(String id, String title, Integer releaseYear) {
        this.id = id;
        this.title = title;
        this.releaseYear = releaseYear;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public Integer getRuntime() {
        return runtime;
    }

    public void setRuntime(Integer runtime) {
        this.runtime = runtime;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public Double getPopularity() {
        return popularity;
    }

    public void setPopularity(Double popularity) {
        this.popularity = popularity;
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
    }

    public Set<Director> getDirectors() {
        return directors;
    }

    public void setDirectors(Set<Director> directors) {
        this.directors = directors;
    }

    public Set<Actor> getActors() {
        return actors;
    }

    public void setActors(Set<Actor> actors) {
        this.actors = actors;
    }

    @Override
    public String toString() {
        return String.format("Movie{id='%s', title='%s', year=%d, avgRating=%.1f}",
                id, title, releaseYear, averageRating);
    }
}