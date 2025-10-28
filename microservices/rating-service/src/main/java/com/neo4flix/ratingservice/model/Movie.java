package com.neo4flix.ratingservice.model;

import org.springframework.data.neo4j.core.schema.*;

/**
 * Movie entity for Rating Service (minimal representation)
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

    @Property("totalRatings")
    private Integer totalRatings = 0;

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

    public Integer getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(Integer totalRatings) {
        this.totalRatings = totalRatings;
    }

    @Override
    public String toString() {
        return String.format("Movie{id='%s', title='%s', year=%d, avgRating=%.1f, totalRatings=%d}",
                id, title, releaseYear, averageRating, totalRatings);
    }
}