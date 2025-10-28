package com.neo4flix.movieservice.dto;

import com.neo4flix.movieservice.model.Movie;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new movie
 */
@Schema(description = "Request to create a new movie")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateMovieRequest {

    @Schema(description = "Movie title", example = "Inception", required = true)
    @NotBlank(message = "Movie title cannot be blank")
    private String title;

    @Schema(description = "Movie plot/synopsis", example = "A skilled thief is offered a chance...")
    private String plot;

    @Schema(description = "Release year", example = "2010", required = true)
    @NotNull(message = "Release year is required")
    @Min(value = 1900, message = "Release year must be after 1900")
    @Max(value = 2030, message = "Release year cannot be in the far future")
    private Integer releaseYear;

    @Schema(description = "Duration in minutes", example = "148")
    @Min(value = 1, message = "Duration must be positive")
    private Integer duration;

    @Schema(description = "Movie language", example = "English")
    private String language;

    @Schema(description = "Country of origin", example = "USA")
    private String country;

    @Schema(description = "Production budget", example = "160000000")
    @Min(value = 0, message = "Budget cannot be negative")
    private Long budget;

    @Schema(description = "Box office earnings", example = "836800000")
    @Min(value = 0, message = "Box office cannot be negative")
    private Long boxOffice;

    @Schema(description = "Poster image URL", example = "/images/inception.jpg")
    private String posterUrl;

    @Schema(description = "Trailer video URL", example = "https://youtube.com/watch?v=inception")
    private String trailerUrl;

    @Schema(description = "IMDB rating", example = "8.8")
    @Min(value = 0, message = "IMDB rating cannot be negative")
    @Max(value = 10, message = "IMDB rating cannot exceed 10")
    private Double imdbRating;

    @Schema(description = "Metacritic score", example = "74")
    @Min(value = 0, message = "Metacritic score cannot be negative")
    @Max(value = 100, message = "Metacritic score cannot exceed 100")
    private Integer metaCriticScore;

    @Schema(description = "Genre names", example = "[\"Action\", \"Sci-Fi\", \"Thriller\"]")
    private List<String> genreNames;

    @Schema(description = "Director names", example = "[\"Christopher Nolan\"]")
    private List<String> directorNames;

    @Schema(description = "Actor information")
    private List<ActorInfo> actors;

    /**
     * Actor information for movie creation
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ActorInfo {
        @Schema(description = "Actor name", example = "Leonardo DiCaprio")
        @NotBlank(message = "Actor name cannot be blank")
        private String name;

        @Schema(description = "Role in movie", example = "Dom Cobb")
        private String role;

        @Schema(description = "Whether this is a main character", example = "true")
        private Boolean isMainCharacter = false;
    }

    public void mapRequestToMovie(Movie movie) {
        movie.setTitle(this.getTitle());
        movie.setPlot(this.getPlot());
        movie.setReleaseYear(this.getReleaseYear());
        movie.setDuration(this.getDuration());
        movie.setLanguage(this.getLanguage());
        movie.setCountry(this.getCountry());
        movie.setBudget(this.getBudget());
        movie.setBoxOffice(this.getBoxOffice());
        movie.setPosterUrl(this.getPosterUrl());
    }
}