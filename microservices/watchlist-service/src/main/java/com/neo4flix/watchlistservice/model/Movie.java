package com.neo4flix.watchlistservice.model;

import org.springframework.data.neo4j.core.schema.*;

/**
 * Movie entity for Watchlist Service (minimal representation)
 */
@Node("Movie")
public class Movie {

    @Id
    private String id;

    @Property("title")
    private String title;

    @Property("releaseYear")
    private Integer releaseYear;

    @Property("averageRating")
    private Double averageRating = 0.0;

    @Property("ratingCount")
    private Integer ratingCount = 0;

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

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
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

    @Override
    public String toString() {
        return String.format("Movie{id='%s', title='%s', year=%d, avgRating=%.1f, ratingCount=%d}",
                id, title, releaseYear, averageRating, ratingCount);
    }
}