package com.neo4flix.movieservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Comprehensive search criteria for movies
 * Supports all possible ways to search and filter movies
 */
@Schema(description = "Comprehensive movie search criteria supporting all filter options")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MovieSearchCriteria {

    // Text Search
    @Schema(description = "General search query (searches across title, director, actor names)",
            example = "Inception")
    private String query;

    @Schema(description = "Movie title (partial match, case-insensitive)", example = "Inception")
    private String title;

    // Genre Filters
    @Schema(description = "Single genre name (exact match)", example = "Action")
    private String genre;

    @Schema(description = "Multiple genre names (movies matching ANY of these genres)",
            example = "[\"Action\", \"Thriller\"]")
    private List<String> genres;

    @Schema(description = "If true, movies must match ALL genres in the list",
            example = "false", defaultValue = "false")
    private Boolean matchAllGenres;

    // People Filters
    @Schema(description = "Director name (partial match, case-insensitive)", example = "Nolan")
    private String director;

    @Schema(description = "Actor name (partial match, case-insensitive)", example = "DiCaprio")
    private String actor;

    // Year Filters
    @Schema(description = "Exact release year", example = "2010")
    private Integer year;

    @Schema(description = "Minimum release year (inclusive)", example = "2000")
    private Integer minYear;

    @Schema(description = "Maximum release year (inclusive)", example = "2020")
    private Integer maxYear;

    // Rating Filters
    @Schema(description = "Minimum IMDB/average rating (inclusive)", example = "7.0")
    private Double minRating;

    @Schema(description = "Maximum IMDB/average rating (inclusive)", example = "9.0")
    private Double maxRating;

    // Metadata Filters
    @Schema(description = "Movie language (partial match)", example = "English")
    private String language;

    @Schema(description = "Country of production (partial match)", example = "USA")
    private String country;

    @Schema(description = "Minimum duration in minutes", example = "90")
    private Integer minDuration;

    @Schema(description = "Maximum duration in minutes", example = "180")
    private Integer maxDuration;

    // Popularity Filters
    @Schema(description = "Minimum number of ratings (for popularity filtering)", example = "100")
    private Integer minRatingCount;

    @Schema(description = "Only include movies with complete information (genre, director, actors)",
            example = "false", defaultValue = "false")
    private Boolean completeInfoOnly;

    // Budget/Revenue Filters
    @Schema(description = "Minimum budget", example = "1000000")
    private Long minBudget;

    @Schema(description = "Maximum budget", example = "100000000")
    private Long maxBudget;

    @Schema(description = "Minimum box office revenue", example = "10000000")
    private Long minBoxOffice;

    @Schema(description = "Maximum box office revenue", example = "1000000000")
    private Long maxBoxOffice;

    /**
     * Validates and sanitizes the search criteria
     * Removes empty strings and ensures logical consistency
     */
    public void sanitize() {
        if (query != null && query.trim().isEmpty()) query = null;
        if (title != null && title.trim().isEmpty()) title = null;
        if (genre != null && genre.trim().isEmpty()) genre = null;
        if (director != null && director.trim().isEmpty()) director = null;
        if (actor != null && actor.trim().isEmpty()) actor = null;
        if (language != null && language.trim().isEmpty()) language = null;
        if (country != null && country.trim().isEmpty()) country = null;

        // Ensure year ranges are logical
        if (minYear != null && maxYear != null && minYear > maxYear) {
            Integer temp = minYear;
            minYear = maxYear;
            maxYear = temp;
        }

        // Ensure rating ranges are logical
        if (minRating != null && maxRating != null && minRating > maxRating) {
            Double temp = minRating;
            minRating = maxRating;
            maxRating = temp;
        }

        // Ensure duration ranges are logical
        if (minDuration != null && maxDuration != null && minDuration > maxDuration) {
            Integer temp = minDuration;
            minDuration = maxDuration;
            maxDuration = temp;
        }

        // Clean up genres list
        if (genres != null) {
            genres = genres.stream()
                .filter(g -> g != null && !g.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .toList();
            if (genres.isEmpty()) genres = null;
        }
    }

    /**
     * Checks if any search criteria is specified
     */
    public boolean isEmpty() {
        return query == null && title == null && genre == null && genres == null &&
               director == null && actor == null && year == null &&
               minYear == null && maxYear == null && minRating == null && maxRating == null &&
               language == null && country == null && minDuration == null && maxDuration == null &&
               minRatingCount == null && minBudget == null && maxBudget == null &&
               minBoxOffice == null && maxBoxOffice == null &&
               (completeInfoOnly == null || !completeInfoOnly);
    }
}