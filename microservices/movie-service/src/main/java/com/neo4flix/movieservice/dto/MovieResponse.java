package com.neo4flix.movieservice.dto;

//import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Movie response DTO
 */
@Schema(description = "Movie information response")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MovieResponse {

    @Schema(description = "Movie ID", example = "1")
    private String id;

    @Schema(description = "Movie title", example = "Inception")
    private String title;

    @Schema(description = "Movie plot/synopsis", example = "A skilled thief is offered a chance...")
    private String plot;

    @Schema(description = "Release year", example = "2010")
    private Integer releaseYear;

    @Schema(description = "Duration in minutes", example = "148")
    private Integer duration;

    @Schema(description = "Movie language", example = "English")
    private String language;

    @Schema(description = "Country of origin", example = "USA")
    private String country;

    @Schema(description = "Production budget", example = "160000000")
    private Long budget;

    @Schema(description = "Box office earnings", example = "836800000")
    private Long boxOffice;

    @Schema(description = "Poster image URL", example = "/images/inception.jpg")
    private String posterUrl;

    @Schema(description = "Trailer video URL", example = "https://youtube.com/watch?v=inception")
    private String trailerUrl;

    @Schema(description = "Backdrop image URL", example = "/images/backdrop.jpg")
    private String backdropUrl;

    @Schema(description = "Data source", example = "TMDb")
    private String source;

    @Schema(description = "TMDb ID", example = "27205")
    private Integer tmdbId;

    @Schema(description = "Creation timestamp", example = "2025-10-03T23:34:29.403Z")
    private ZonedDateTime createdAt;

    @Schema(description = "Average user rating", example = "8.5")
    private Double averageRating;

    @Schema(description = "Total number of ratings", example = "1250")
    private Integer totalRatings;

    @Schema(description = "Movie genres")
    private List<GenreResponse> genres;

    @Schema(description = "Movie directors")
    private List<PersonResponse> directors;

    @Schema(description = "Movie actors")
    private List<PersonResponse> actors;
}