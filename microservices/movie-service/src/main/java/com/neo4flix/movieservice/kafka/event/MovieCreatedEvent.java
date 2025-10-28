package com.neo4flix.movieservice.kafka.event;

import java.time.LocalDateTime;

public class MovieCreatedEvent {
    private String movieId;
    private String title;
    private String imdbId;
    private Integer releaseYear;
    private LocalDateTime timestamp;

    public MovieCreatedEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public MovieCreatedEvent(String movieId, String title, String imdbId, Integer releaseYear) {
        this.movieId = movieId;
        this.title = title;
        this.imdbId = imdbId;
        this.releaseYear = releaseYear;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImdbId() {
        return imdbId;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "MovieCreatedEvent{" +
                "movieId='" + movieId + '\'' +
                ", title='" + title + '\'' +
                ", imdbId='" + imdbId + '\'' +
                ", releaseYear=" + releaseYear +
                ", timestamp=" + timestamp +
                '}';
    }
}
