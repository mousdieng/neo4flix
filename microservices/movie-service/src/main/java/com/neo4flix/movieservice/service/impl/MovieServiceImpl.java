package com.neo4flix.movieservice.service.impl;

import com.neo4flix.movieservice.dto.*;
import com.neo4flix.movieservice.exception.MovieNotFoundException;
import com.neo4flix.movieservice.exception.DuplicateMovieException;
import com.neo4flix.movieservice.model.*;
import com.neo4flix.movieservice.repository.MovieRepository;
import com.neo4flix.movieservice.service.MovieService;
import com.neo4flix.movieservice.service.FileStorageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of MovieService
 */
@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final FileStorageService fileStorageService;

    @Override
    public MovieResponse createMovie(CreateMovieRequest request) {
        return createMovie(request, null, null);
    }

    @Override
    public MovieResponse createMovie(CreateMovieRequest request, MultipartFile posterFile, MultipartFile trailerFile) {
        // Check if movie already exists
        Optional<Movie> existingMovie = movieRepository.findByTitleAndReleaseYear(
            request.getTitle(), request.getReleaseYear());
        if (existingMovie.isPresent()) {
            throw new DuplicateMovieException(
                String.format("Movie '%s' (%d) already exists", request.getTitle(), request.getReleaseYear())
            );
        }

        // Create new movie
        Movie movie = new Movie();
        request.mapRequestToMovie(movie);

        // Upload poster if provided
        if (posterFile != null && !posterFile.isEmpty()) {
            try {
                String posterUrl = fileStorageService.uploadFile(posterFile, "posters");
                movie.setPosterUrl(posterUrl);
                log.info("Uploaded poster for movie '{}': {}", movie.getTitle(), posterUrl);
            } catch (Exception e) {
                log.error("Failed to upload poster for movie '{}'", movie.getTitle(), e);
                // Continue without poster - non-critical failure
            }
        }

        // Upload trailer if provided
        if (trailerFile != null && !trailerFile.isEmpty()) {
            try {
                String trailerUrl = fileStorageService.uploadFile(trailerFile, "trailers");
                movie.setTrailerUrl(trailerUrl);
                log.info("Uploaded trailer for movie '{}': {}", movie.getTitle(), trailerUrl);
            } catch (Exception e) {
                log.error("Failed to upload trailer for movie '{}'", movie.getTitle(), e);
                // Continue without trailer - non-critical failure
            }
        }

        // Save movie with uploaded file URLs
        Movie savedMovie = movieRepository.save(movie);
        return savedMovie.mapMovieToResponse();
    }

    @Override
    public MovieResponse updateMovie(String movieId, UpdateMovieRequest request) {
        return updateMovie(movieId, request, null, null);
    }

    @Override
    public MovieResponse updateMovie(String movieId, UpdateMovieRequest request, MultipartFile posterFile, MultipartFile trailerFile) {
        Movie movie = movieRepository.findByIdWithRelationships(movieId)
            .orElseThrow(() -> new MovieNotFoundException("Movie not found with ID: " + movieId));

        // Update fields if provided
        if (request.getTitle() != null) movie.setTitle(request.getTitle());
        if (request.getPlot() != null) movie.setPlot(request.getPlot());
        if (request.getReleaseYear() != null) movie.setReleaseYear(request.getReleaseYear());
        if (request.getDuration() != null) movie.setDuration(request.getDuration());
        if (request.getLanguage() != null) movie.setLanguage(request.getLanguage());
        if (request.getCountry() != null) movie.setCountry(request.getCountry());
        if (request.getBudget() != null) movie.setBudget(request.getBudget());
        if (request.getBoxOffice() != null) movie.setBoxOffice(request.getBoxOffice());
        if (request.getPosterUrl() != null) movie.setPosterUrl(request.getPosterUrl());

        // Upload new poster if provided
        if (posterFile != null && !posterFile.isEmpty()) {
            try {
                String posterUrl = fileStorageService.uploadFile(posterFile, "posters");
                movie.setPosterUrl(posterUrl);
                log.info("Uploaded new poster for movie '{}': {}", movie.getTitle(), posterUrl);
            } catch (Exception e) {
                log.error("Failed to upload poster for movie '{}'", movie.getTitle(), e);
                // Continue without updating poster - non-critical failure
            }
        }

        // Upload new trailer if provided
        if (trailerFile != null && !trailerFile.isEmpty()) {
            try {
                String trailerUrl = fileStorageService.uploadFile(trailerFile, "trailers");
                movie.setTrailerUrl(trailerUrl);
                log.info("Uploaded new trailer for movie '{}': {}", movie.getTitle(), trailerUrl);
            } catch (Exception e) {
                log.error("Failed to upload trailer for movie '{}'", movie.getTitle(), e);
                // Continue without updating trailer - non-critical failure
            }
        }

        Movie updatedMovie = movieRepository.save(movie);
        return updatedMovie.mapMovieToResponse();
    }

    @Override
    public void deleteMovie(String movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new MovieNotFoundException("Movie not found with ID: " + movieId);
        }
        movieRepository.deleteById(movieId);
    }

    @Override
    public Optional<MovieResponse> findMovieById(String movieId) {
        log.debug("Finding movie by ID: {}", movieId);
        return movieRepository.findByIdWithRelationships(movieId)
                .map(Movie::mapMovieToResponse)
                .map(this::enrichWithPresignedUrls);
    }

    @Override
    public Page<MovieResponse> findAllMovies(Pageable pageable) {
        Page<Movie> movies;

        if (pageable.getSort().isSorted()) {
            String sortProperty = pageable.getSort().iterator().next().getProperty();

            if ("averageRating".equals(sortProperty)) {
                movies = movieRepository.findAllMoviesByAverageRatingWithGenres(pageable);
            } else if ("releaseYear".equals(sortProperty)) {
                movies = movieRepository.findAllMoviesByReleaseYearWithGenres(pageable);
            } else {
                movies = movieRepository.findAllMoviesByTitleWithGenres(pageable);
            }
        } else {
            movies = movieRepository.findAllMoviesByTitleWithGenres(pageable);
        }

        List<MovieResponse> movieResponses = movies.getContent().stream()
            .map(Movie::mapMovieToResponseLite)
            .map(this::enrichWithPresignedUrls)
            .collect(Collectors.toList());
        return new PageImpl<>(movieResponses, pageable, movies.getTotalElements());
    }

    @Override
    public Page<MovieResponse> searchMovies(MovieSearchCriteria criteria, Pageable pageable) {
        if (criteria == null) {
            criteria = new MovieSearchCriteria();
        }

        // Sanitize and validate criteria
        criteria.sanitize();

        // Log if this is an empty search (returns all movies)
        if (criteria.isEmpty()) {
            log.info("Empty search criteria provided, returning all movies with pagination");
        }

        // Extract sorting configuration
        String sortBy = "releaseYear";  // default
        String sortDir = "DESC";         // default

        if (pageable.getSort().isSorted()) {
            var sort = pageable.getSort().iterator().next();
            sortBy = sort.getProperty();
            sortDir = sort.isAscending() ? "ASC" : "DESC";
            log.debug("Sort configuration: {} {}", sortBy, sortDir);
        }

        // Execute search query with comprehensive filters
        List<Movie> movies = movieRepository.searchMoviesWithComprehensiveFilters(
            criteria.getQuery(),
            criteria.getTitle(),
            criteria.getGenre(),
            criteria.getDirector(),
            criteria.getActor(),
            criteria.getYear(),
            criteria.getMinYear(),
            criteria.getMaxYear(),
            criteria.getMinRating(),
            criteria.getMaxRating(),
            criteria.getLanguage(),
            criteria.getCountry(),
            criteria.getMinDuration(),
            criteria.getMaxDuration(),
            criteria.getMinBudget(),
            criteria.getMaxBudget(),
            criteria.getMinBoxOffice(),
            criteria.getMaxBoxOffice(),
            sortBy,
            sortDir,
            pageable.getOffset(),
            pageable.getPageSize()
        );

        // Count total matches for pagination
        long totalCount = movieRepository.countMoviesWithComprehensiveFilters(
            criteria.getQuery(),
            criteria.getTitle(),
            criteria.getGenre(),
            criteria.getDirector(),
            criteria.getActor(),
            criteria.getYear(),
            criteria.getMinYear(),
            criteria.getMaxYear(),
            criteria.getMinRating(),
            criteria.getMaxRating(),
            criteria.getLanguage(),
            criteria.getCountry(),
            criteria.getMinDuration(),
            criteria.getMaxDuration(),
            criteria.getMinBudget(),
            criteria.getMaxBudget(),
            criteria.getMinBoxOffice(),
            criteria.getMaxBoxOffice()
        );

        // Map to lightweight response DTOs
        List<MovieResponse> movieResponses = movies.stream()
            .map(Movie::mapMovieToResponseLite)
            .map(this::enrichWithPresignedUrls)
            .collect(Collectors.toList());

        log.debug("Mapped {} movies to response DTOs", movieResponses.size());

        return new PageImpl<>(movieResponses, pageable, totalCount);
    }

    @Override
    public List<MovieResponse> findMoviesByGenre(String genreName) {
        List<Movie> movies = movieRepository.findByGenreName(genreName);
        return movies.stream()
            .map(Movie::mapMovieToResponse)
            .map(this::enrichWithPresignedUrls)
            .collect(Collectors.toList());
    }

    @Override
    public List<MovieResponse> findMoviesByDirector(String directorName) {
        List<Movie> movies = movieRepository.findByDirectorName(directorName);
        return movies.stream()
            .map(Movie::mapMovieToResponse)
            .map(this::enrichWithPresignedUrls)
            .collect(Collectors.toList());
    }

    @Override
    public List<MovieResponse> findMoviesByActor(String actorName) {
        List<Movie> movies = movieRepository.findByActorName(actorName);
        return movies.stream()
            .map(Movie::mapMovieToResponse)
            .map(this::enrichWithPresignedUrls)
            .collect(Collectors.toList());
    }

    @Override
    public List<MovieResponse> findMoviesByReleaseYear(Integer releaseYear) {
        List<Movie> movies = movieRepository.findByReleaseYear(releaseYear);
        return movies.stream()
            .map(Movie::mapMovieToResponse)
            .map(this::enrichWithPresignedUrls)
            .collect(Collectors.toList());
    }

    @Override
    public List<MovieResponse> findMoviesByReleaseYearRange(Integer startYear, Integer endYear) {
        List<Movie> movies = movieRepository.findByReleaseYearBetween(startYear, endYear);
        return movies.stream()
            .map(Movie::mapMovieToResponse)
            .map(this::enrichWithPresignedUrls)
            .collect(Collectors.toList());
    }

    @Override
    public List<MovieResponse> findTopRatedMovies(Integer minRatings, Integer limit) {
        List<Movie> movies = movieRepository.findTopRatedMovies(minRatings, limit);
        return movies.stream()
            .map(Movie::mapMovieToResponse)
            .map(this::enrichWithPresignedUrls)
            .collect(Collectors.toList());
    }

    @Override
    public List<MovieResponse> findMostPopularMovies(Integer limit) {
        List<Movie> movies = movieRepository.findMostPopularMovies(limit);
        return movies.stream()
            .map(Movie::mapMovieToResponse)
            .map(this::enrichWithPresignedUrls)
            .collect(Collectors.toList());
    }

    @Override
    public List<MovieResponse> findRecentMovies(Integer fromYear, Integer limit) {
        List<Movie> movies = movieRepository.findRecentMovies(fromYear, limit);
        return movies.stream()
            .map(Movie::mapMovieToResponse)
            .map(this::enrichWithPresignedUrls)
            .collect(Collectors.toList());
    }

    @Override
    public List<MovieResponse> findSimilarMovies(String movieId, Integer limit) {
        List<Movie> movies = movieRepository.findSimilarMovies(movieId, limit);
        return movies.stream()
            .map(Movie::mapMovieToResponse)
            .map(this::enrichWithPresignedUrls)
            .collect(Collectors.toList());
    }

    @Override
    public List<MovieResponse> getBasicRecommendations(String userId, Double minRating, Integer limit) {
        List<Movie> movies = movieRepository.findRecommendedMoviesForUser(userId, minRating, limit);
        return movies.stream()
            .map(Movie::mapMovieToResponse)
            .map(this::enrichWithPresignedUrls)
            .collect(Collectors.toList());
    }

    @Override
    public boolean movieExists(String movieId) {
        return movieRepository.existsById(movieId);
    }

    @Override
    public MovieStats getMovieStatistics() {
        MovieRepository.MovieStats stats = movieRepository.getMovieStats();
        return new MovieStatsImpl(
            stats.getTotalMovies(),
            stats.getTotalRatings(),
            stats.getOverallAverageRating()
        );
    }

    @Override
    public void updateMoviePosterUrl(String movieId, String posterUrl) {
        Movie movie = movieRepository.findByIdWithRelationships(movieId)
            .orElseThrow(() -> new MovieNotFoundException("Movie not found with ID: " + movieId));
        movie.setPosterUrl(posterUrl);
        movieRepository.save(movie);
    }

    @Override
    public void updateMovieTrailerUrl(String movieId, String trailerUrl) {
        Movie movie = movieRepository.findByIdWithRelationships(movieId)
            .orElseThrow(() -> new MovieNotFoundException("Movie not found with ID: " + movieId));
        movie.setTrailerUrl(trailerUrl);
        movieRepository.save(movie);
    }

    /**
     * Enriches a MovieResponse with presigned URLs for poster and trailer.
     * Converts object keys (e.g., "posters/abc123.jpg") to temporary presigned URLs.
     * Only processes object keys (not full URLs) to avoid breaking existing URLs.
     */
    private MovieResponse enrichWithPresignedUrls(MovieResponse response) {
        // Generate presigned URL for poster if object key exists
        if (response.getPosterUrl() != null && !response.getPosterUrl().isEmpty()) {
            String posterUrl = response.getPosterUrl();
            // Only process if it's an object key (not already a full URL)
            if (!posterUrl.startsWith("http://") && !posterUrl.startsWith("https://")) {
                try {
                    String presignedUrl = fileStorageService.getFileUrl(posterUrl);
                    response.setPosterUrl(presignedUrl);
                } catch (Exception e) {
                    log.warn("Failed to generate presigned URL for poster: {}", posterUrl, e);
                    // Keep the original object key if URL generation fails
                }
            }
            // If it's already a full URL, leave it as-is
        }

        // Generate presigned URL for trailer if object key exists
        if (response.getTrailerUrl() != null && !response.getTrailerUrl().isEmpty()) {
            String trailerUrl = response.getTrailerUrl();
            // Only process if it's an object key (not already a full URL)
            if (!trailerUrl.startsWith("http://") && !trailerUrl.startsWith("https://")) {
                try {
                    String presignedUrl = fileStorageService.getFileUrl(trailerUrl);
                    response.setTrailerUrl(presignedUrl);
                } catch (Exception e) {
                    log.warn("Failed to generate presigned URL for trailer: {}", trailerUrl, e);
                    // Keep the original object key if URL generation fails
                }
            }
            // If it's already a full URL, leave it as-is
        }

        return response;
    }

    private record MovieStatsImpl(Long totalMovies, Long totalRatings,
                                      Double overallAverageRating) implements MovieStats {
    }
}