package com.neo4flix.movieservice.model;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.util.Set;

/**
 * Movie entity representing a movie node in Neo4j
 */
@Node("Movie")
public class Movie {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;

    @Property("title")
    @NotBlank(message = "Movie title cannot be blank")
    private String title;

    @Property("plot")
    private String plot;

    @Property("releaseYear")
    @NotNull(message = "Release year is required")
    @Min(value = 1900, message = "Release year must be after 1900")
    @Max(value = 2030, message = "Release year cannot be in the far future")
    private Integer releaseYear;

    @Property("duration")
    @Min(value = 1, message = "Duration must be positive")
    private Integer duration; // in minutes

    @Property("language")
    private String language;

    @Property("country")
    private String country;

    @Property("budget")
    @Min(value = 0, message = "Budget cannot be negative")
    private Long budget;

    @Property("boxOffice")
    @Min(value = 0, message = "Box office cannot be negative")
    private Long boxOffice;

    @Property("posterUrl")
    private String posterUrl;

    @Property("trailerUrl")
    private String trailerUrl;

    @Property("imdbRating")
    @Min(value = 0, message = "IMDB rating cannot be negative")
    @Max(value = 10, message = "IMDB rating cannot exceed 10")
    private Double imdbRating;

    @Property("metaCriticScore")
    @Min(value = 0, message = "Metacritic score cannot be negative")
    @Max(value = 100, message = "Metacritic score cannot exceed 100")
    private Integer metaCriticScore;

    // Relationships
    @Relationship(type = "BELONGS_TO_GENRE", direction = Relationship.Direction.OUTGOING)
    private Set<Genre> genres;

    @Relationship(type = "DIRECTED", direction = Relationship.Direction.INCOMING)
    private Set<Director> directors;

    @Relationship(type = "ACTED_IN", direction = Relationship.Direction.INCOMING)
    private Set<Actor> actors;

    @Relationship(type = "SIMILAR_TO", direction = Relationship.Direction.OUTGOING)
    private Set<MovieSimilarity> similarMovies;

    @Relationship(type = "RATED", direction = Relationship.Direction.INCOMING)
    private Set<Rating> ratings;

    // Constructors
    public Movie() {}

    public Movie(String title, Integer releaseYear) {
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

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Long getBudget() {
        return budget;
    }

    public void setBudget(Long budget) {
        this.budget = budget;
    }

    public Long getBoxOffice() {
        return boxOffice;
    }

    public void setBoxOffice(Long boxOffice) {
        this.boxOffice = boxOffice;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getTrailerUrl() {
        return trailerUrl;
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }

    public Double getImdbRating() {
        return imdbRating;
    }

    public void setImdbRating(Double imdbRating) {
        this.imdbRating = imdbRating;
    }

    public Integer getMetaCriticScore() {
        return metaCriticScore;
    }

    public void setMetaCriticScore(Integer metaCriticScore) {
        this.metaCriticScore = metaCriticScore;
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

    public Set<MovieSimilarity> getSimilarMovies() {
        return similarMovies;
    }

    public void setSimilarMovies(Set<MovieSimilarity> similarMovies) {
        this.similarMovies = similarMovies;
    }

    public Set<Rating> getRatings() {
        return ratings;
    }

    public void setRatings(Set<Rating> ratings) {
        this.ratings = ratings;
    }

    // Calculated fields
    /**
     * Get average user rating (0-10 scale)
     * Returns null if no user ratings exist yet
     */
    public Double getAverageRating() {
        if (ratings == null || ratings.isEmpty()) {
            return null; // No user ratings yet
        }
        return ratings.stream()
                .mapToDouble(Rating::getRating)
                .average()
                .orElse(0.0);
    }

    public Integer getTotalRatings() {
        return ratings != null ? ratings.size() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return id != null && id.equals(movie.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return String.format("Movie{id='%s', title='%s', releaseYear=%d}", id, title, releaseYear);
    }
}