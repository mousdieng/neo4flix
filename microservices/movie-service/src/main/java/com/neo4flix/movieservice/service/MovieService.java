package com.neo4flix.movieservice.service;

import com.neo4flix.movieservice.dto.MovieSearchCriteria;
import com.neo4flix.movieservice.dto.MovieResponse;
import com.neo4flix.movieservice.dto.CreateMovieRequest;
import com.neo4flix.movieservice.dto.UpdateMovieRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for movie operations
 */
public interface MovieService {

    /**
     * Create a new movie
     */
    MovieResponse createMovie(CreateMovieRequest request);

    /**
     * Update an existing movie
     */
    MovieResponse updateMovie(String movieId, UpdateMovieRequest request);

    /**
     * Delete a movie by ID
     */
    void deleteMovie(String movieId);

    /**
     * Find movie by ID
     */
    Optional<MovieResponse> findMovieById(String movieId);

    /**
     * Find all movies with pagination
     */
    Page<MovieResponse> findAllMovies(Pageable pageable);

    /**
     * Search movies by criteria
     */
    Page<MovieResponse> searchMovies(MovieSearchCriteria criteria, Pageable pageable);

    /**
     * Find movies by genre
     */
    List<MovieResponse> findMoviesByGenre(String genreName);

    /**
     * Find movies by director
     */
    List<MovieResponse> findMoviesByDirector(String directorName);

    /**
     * Find movies by actor
     */
    List<MovieResponse> findMoviesByActor(String actorName);

    /**
     * Find movies by release year
     */
    List<MovieResponse> findMoviesByReleaseYear(Integer releaseYear);

    /**
     * Find movies by release year range
     */
    List<MovieResponse> findMoviesByReleaseYearRange(Integer startYear, Integer endYear);

    /**
     * Find top-rated movies
     */
    List<MovieResponse> findTopRatedMovies(Integer minRatings, Integer limit);

    /**
     * Find most popular movies
     */
    List<MovieResponse> findMostPopularMovies(Integer limit);

    /**
     * Find recently released movies
     */
    List<MovieResponse> findRecentMovies(Integer fromYear, Integer limit);

    /**
     * Find similar movies
     */
    List<MovieResponse> findSimilarMovies(String movieId, Integer limit);

    /**
     * Get basic recommendations for a user
     */
    List<MovieResponse> getBasicRecommendations(String userId, Double minRating, Integer limit);

    /**
     * Check if movie exists
     */
    boolean movieExists(String movieId);

    /**
     * Get movie statistics
     */
    MovieStats getMovieStatistics();

    /**
     * Update movie poster URL
     */
    void updateMoviePosterUrl(String movieId, String posterUrl);

    /**
     * Update movie trailer URL
     */
    void updateMovieTrailerUrl(String movieId, String trailerUrl);

    /**
     * Movie statistics interface
     */
    interface MovieStats {
        Long totalMovies();
        Long totalRatings();
        Double overallAverageRating();
    }
}