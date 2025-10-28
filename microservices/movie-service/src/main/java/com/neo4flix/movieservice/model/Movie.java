package com.neo4flix.movieservice.model;

import com.neo4flix.movieservice.dto.GenreResponse;
import com.neo4flix.movieservice.dto.MovieResponse;
import com.neo4flix.movieservice.dto.PersonResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Movie entity representing a movie node in Neo4j
 */
@Node("Movie")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
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

    @Property("trailerUrl")
    private String trailerUrl;

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

    @Property("backdropUrl")
    private String backdropUrl;

    @Property("source")
    private String source; // e.g., "TMDb", "IMDB"

    @Property("tmdbId")
    private Integer tmdbId;

    @Property("createdAt")
    private ZonedDateTime createdAt; // Timestamp when movie was added to the database

    @Property("averageRating")
    private Double averageRating; // Stored average rating (0-10 scale)

    @Property("totalRatings")
    private Integer totalRatings; // Stored count of ratings

    // Relationships
    @Relationship(type = "IN_GENRE", direction = Relationship.Direction.OUTGOING)
    private Set<Genre> genres;

    @Relationship(type = "DIRECTED", direction = Relationship.Direction.INCOMING)
    private Set<Director> directors;

    @Relationship(type = "ACTED_IN", direction = Relationship.Direction.INCOMING)
    private Set<Actor> actors;

    @Relationship(type = "SIMILAR_TO", direction = Relationship.Direction.OUTGOING)
    private Set<MovieSimilarity> similarMovies;

    @Relationship(type = "RATED", direction = Relationship.Direction.INCOMING)
    private Set<Rating> ratings;

    /**
     * Full mapping with all relationships - use for detailed movie view.
     */
    public MovieResponse mapMovieToResponse() {
        MovieResponse response = baseMovieResponse(this);

        response.setGenres(mapGenres(this.getGenres()));
        response.setDirectors(mapDirectors(this.getDirectors()));
        response.setActors(mapActors(this.getActors()));

        return response;
    }

    /**
     * Lightweight mapping for list views - excludes relationships for better performance.
     */
    public MovieResponse mapMovieToResponseLite() {
        MovieResponse response = baseMovieResponse(this);

        // Include only genres (for filtering/search purposes)
        response.setGenres(mapGenres(this.getGenres()));
        response.setDirectors(Collections.emptyList());
        response.setActors(Collections.emptyList());

        return response;
    }

    // ------------------- Private Helpers -------------------

    /**
     * Builds the base MovieResponse (common fields shared by all mappings).
     */
    private MovieResponse baseMovieResponse(Movie movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .plot(movie.getPlot())
                .releaseYear(movie.getReleaseYear())
                .duration(movie.getDuration())
                .language(movie.getLanguage())
                .country(movie.getCountry())
                .budget(movie.getBudget())
                .boxOffice(movie.getBoxOffice())
                .posterUrl(movie.getPosterUrl())
                .trailerUrl(movie.getTrailerUrl())
                .backdropUrl(movie.getBackdropUrl())
                .source(movie.getSource())
                .tmdbId(movie.getTmdbId())
                .createdAt(movie.getCreatedAt())
                .averageRating(movie.getAverageRating())
                .totalRatings(movie.getTotalRatings())
                .build();
    }

    /**
     * Maps a set of Genre entities to a list of GenreResponse DTOs.
     */
    private List<GenreResponse> mapGenres(Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) return Collections.emptyList();

        return genres.stream()
                .map(g -> GenreResponse.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .description(g.getDescription())
                        .build())
                .toList();
    }

    /**
     * Maps a set of Director entities to a list of PersonResponse DTOs.
     */
    private List<PersonResponse> mapDirectors(Set<Director> directors) {
        if (directors == null || directors.isEmpty()) return Collections.emptyList();

        return directors.stream()
                .map(Director::mapDirectorToPersonResponse)
                .toList();
    }

    /**
     * Maps a set of Actor entities to a list of PersonResponse DTOs.
     */
    private List<PersonResponse> mapActors(Set<Actor> actors) {
        if (actors == null || actors.isEmpty()) return Collections.emptyList();

        return actors.stream()
                .map(Actor::mapActorToPersonResponse)
                .toList();
    }
}