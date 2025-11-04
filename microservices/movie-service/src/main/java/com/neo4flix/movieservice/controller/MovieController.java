package com.neo4flix.movieservice.controller;

import com.neo4flix.movieservice.dto.*;
import com.neo4flix.movieservice.model.Genre;
import com.neo4flix.movieservice.repository.GenreRepository;
import com.neo4flix.movieservice.service.FileStorageService;
import com.neo4flix.movieservice.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for movie operations
 */
@RestController
@RequestMapping("/api/v1/movies")
@Tag(name = "Movies", description = "Movie management operations")
public class MovieController {

    private final MovieService movieService;
    private final GenreRepository genreRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public MovieController(MovieService movieService, GenreRepository genreRepository, FileStorageService fileStorageService) {
        this.movieService = movieService;
        this.genreRepository = genreRepository;
        this.fileStorageService = fileStorageService;
    }

    @Operation(summary = "Create a new movie", description = "Creates a new movie with the provided information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Movie created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Movie already exists")
    })
    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<MovieResponse> createMovie(@Valid @RequestBody CreateMovieRequest request) {
        MovieResponse response = movieService.createMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Create a new movie with files", description = "Creates a new movie with the provided information and optional files")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Movie created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Movie already exists")
    })
    @PostMapping(consumes = "multipart/form-data", produces = "application/json")
    public ResponseEntity<MovieResponse> createMovieWithFiles(
            @Parameter(description = "Movie data as JSON string") @RequestParam("movie") String movieJson,
            @Parameter(description = "Optional poster image file") @RequestParam(value = "poster", required = false) MultipartFile posterFile,
            @Parameter(description = "Optional trailer video file") @RequestParam(value = "trailer", required = false) MultipartFile trailerFile) {

        // Parse JSON to CreateMovieRequest
        CreateMovieRequest request;
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            request = objectMapper.readValue(movieJson, CreateMovieRequest.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        MovieResponse response = movieService.createMovie(request, posterFile, trailerFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update a movie", description = "Updates an existing movie with the provided information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Movie updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Movie not found")
    })
    @PutMapping(value = "/{movieId}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<MovieResponse> updateMovie(
            @Parameter(description = "Movie ID") @PathVariable String movieId,
            @Valid @RequestBody UpdateMovieRequest request) {
        MovieResponse response = movieService.updateMovie(movieId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update a movie with files", description = "Updates an existing movie with the provided information and optional files")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Movie updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Movie not found")
    })
    @PutMapping(value = "/{movieId}", consumes = "multipart/form-data", produces = "application/json")
    public ResponseEntity<MovieResponse> updateMovieWithFiles(
            @Parameter(description = "Movie ID") @PathVariable String movieId,
            @Parameter(description = "Movie data as JSON string") @RequestParam("movie") String movieJson,
            @Parameter(description = "Optional poster image file") @RequestParam(value = "poster", required = false) MultipartFile posterFile,
            @Parameter(description = "Optional trailer video file") @RequestParam(value = "trailer", required = false) MultipartFile trailerFile) {

        // Parse JSON to UpdateMovieRequest
        UpdateMovieRequest request;
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            request = objectMapper.readValue(movieJson, UpdateMovieRequest.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        MovieResponse response = movieService.updateMovie(movieId, request, posterFile, trailerFile);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a movie", description = "Deletes a movie by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Movie deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Movie not found")
    })
    @DeleteMapping("/{movieId}")
    public ResponseEntity<Void> deleteMovie(@Parameter(description = "Movie ID") @PathVariable String movieId) {
        movieService.deleteMovie(movieId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get movie by ID", description = "Retrieves a movie by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Movie found"),
        @ApiResponse(responseCode = "404", description = "Movie not found")
    })
    @GetMapping("/{movieId}")
    public ResponseEntity<?> getMovie(@Parameter(description = "Movie ID") @PathVariable String movieId) {
        Optional<MovieResponse> movie = movieService.findMovieById(movieId);
        return movie.map(m -> ResponseEntity.ok(Map.of("success", true, "data", m)))
                   .orElse(ResponseEntity.status(404).body(Map.of("success", false, "message", "Movie not found")));
    }

    @Operation(summary = "Get all movies", description = "Retrieves all movies with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Movies retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<PagedMovieResponse> getAllMovies(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "title") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "ASC") String sortDir,
            @Parameter(description = "Sort order (asc/desc)") @RequestParam(defaultValue = "asc") String sortOrder) {

        // Use sortOrder parameter if provided, otherwise fall back to sortDir
        String direction = sortOrder != null && !sortOrder.isEmpty() ? sortOrder : sortDir;
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, normalizeSortField(sortBy)));
        Page<MovieResponse> movies = movieService.findAllMovies(pageable);

        return ResponseEntity.ok(PagedMovieResponse.builder()
                .success(true)
                .message("Movies retrieved successfully")
                .data(PagedMovieResponse.PageData.builder()
                        .movies(movies.getContent())
                        .currentPage(movies.getNumber())
                        .totalPages(movies.getTotalPages())
                        .totalElements(movies.getTotalElements())
                        .hasNext(movies.hasNext())
                        .hasPrevious(movies.hasPrevious())
                        .build())
                .build());
    }

    @Operation(summary = "Search movies", description = "Comprehensive movie search supporting all filter combinations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<PagedMovieResponse> searchMovies(
            // Text search
            @Parameter(description = "General search query (searches title, director, actor)")
            @RequestParam(required = false) String query,
            @Parameter(description = "Movie title (partial match)")
            @RequestParam(required = false) String title,

            // Genre filters
            @Parameter(description = "Genre filter")
            @RequestParam(required = false) String genre,

            // People filters
            @Parameter(description = "Director name (partial match)")
            @RequestParam(required = false) String director,
            @Parameter(description = "Actor name (partial match)")
            @RequestParam(required = false) String actor,

            // Year filters
            @Parameter(description = "Exact year")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Minimum year")
            @RequestParam(required = false) Integer minYear,
            @Parameter(description = "Maximum year")
            @RequestParam(required = false) Integer maxYear,

            // Rating filters
            @Parameter(description = "Minimum rating")
            @RequestParam(required = false) Double minRating,
            @Parameter(description = "Maximum rating")
            @RequestParam(required = false) Double maxRating,

            // Metadata filters
            @Parameter(description = "Language")
            @RequestParam(required = false) String language,
            @Parameter(description = "Country")
            @RequestParam(required = false) String country,
            @Parameter(description = "Minimum duration (minutes)")
            @RequestParam(required = false) Integer minDuration,
            @Parameter(description = "Maximum duration (minutes)")
            @RequestParam(required = false) Integer maxDuration,

            // Budget/Revenue filters
            @Parameter(description = "Minimum budget")
            @RequestParam(required = false) Long minBudget,
            @Parameter(description = "Maximum budget")
            @RequestParam(required = false) Long maxBudget,
            @Parameter(description = "Minimum box office")
            @RequestParam(required = false) Long minBoxOffice,
            @Parameter(description = "Maximum box office")
            @RequestParam(required = false) Long maxBoxOffice,

            // Pagination
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field (title, releaseYear, averageRating, duration)")
            @RequestParam(defaultValue = "releaseYear") String sortBy,
            @Parameter(description = "Sort order (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortOrder) {

        // Build search criteria from request parameters
        MovieSearchCriteria criteria = MovieSearchCriteria.builder()
            .query(query)
            .title(title)
            .genre(genre)
            .director(director)
            .actor(actor)
            .year(year)
            .minYear(minYear)
            .maxYear(maxYear)
            .minRating(minRating)
            .maxRating(maxRating)
            .language(language)
            .country(country)
            .minDuration(minDuration)
            .maxDuration(maxDuration)
            .minBudget(minBudget)
            .maxBudget(maxBudget)
            .minBoxOffice(minBoxOffice)
            .maxBoxOffice(maxBoxOffice)
            .build();

        // Configure pagination and sorting
        Sort.Direction sortDirection = sortOrder.equalsIgnoreCase("DESC") ?
            Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, normalizeSortField(sortBy)));

        // Execute search using the single comprehensive search method
        Page<MovieResponse> movies = movieService.searchMovies(criteria, pageable);

        return ResponseEntity.ok(PagedMovieResponse.builder()
                        .success(true)
                        .message("Success")
                        .data(PagedMovieResponse.PageData.builder()
                                .movies(movies.getContent())
                                .currentPage(movies.getPageable().getPageNumber())
                                .totalPages(movies.getTotalPages())
                                .totalElements(movies.getTotalElements())
                                .hasNext(movies.hasNext())
                                .hasPrevious(movies.hasPrevious())
                        .build())
                .build());
    }

    /**
     * Normalizes sort field names to match entity properties
     */
    private String normalizeSortField(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "title" -> "title";
            case "year", "release_date", "releaseyear" -> "releaseYear";
            case "rating", "averagerating" -> "averageRating";
            case "duration" -> "duration";
            case "popularity", "totalratings" -> "totalRatings";
            default -> "releaseYear";
        };
    }

    @Operation(summary = "Get movies by genre", description = "Retrieves movies by genre name")
    @GetMapping("/genre/{genreName}")
    public ResponseEntity<List<MovieResponse>> getMoviesByGenre(
            @Parameter(description = "Genre name") @PathVariable String genreName) {
        List<MovieResponse> movies = movieService.findMoviesByGenre(genreName);
        return ResponseEntity.ok(movies);
    }

    @Operation(summary = "Get movies by director", description = "Retrieves movies by director name")
    @GetMapping("/director/{directorName}")
    public ResponseEntity<List<MovieResponse>> getMoviesByDirector(
            @Parameter(description = "Director name") @PathVariable String directorName) {
        List<MovieResponse> movies = movieService.findMoviesByDirector(directorName);
        return ResponseEntity.ok(movies);
    }

    @Operation(summary = "Get movies by actor", description = "Retrieves movies by actor name")
    @GetMapping("/actor/{actorName}")
    public ResponseEntity<List<MovieResponse>> getMoviesByActor(
            @Parameter(description = "Actor name") @PathVariable String actorName) {
        List<MovieResponse> movies = movieService.findMoviesByActor(actorName);
        return ResponseEntity.ok(movies);
    }

    @Operation(summary = "Get movies by release year", description = "Retrieves movies by release year")
    @GetMapping("/year/{year}")
    public ResponseEntity<List<MovieResponse>> getMoviesByYear(
            @Parameter(description = "Release year") @PathVariable Integer year) {
        List<MovieResponse> movies = movieService.findMoviesByReleaseYear(year);
        return ResponseEntity.ok(movies);
    }

    @Operation(summary = "Get top-rated movies", description = "Retrieves top-rated movies")
    @GetMapping("/top-rated")
    public ResponseEntity<List<MovieResponse>> getTopRatedMovies(
            @Parameter(description = "Minimum number of ratings") @RequestParam(defaultValue = "10") Integer minRatings,
            @Parameter(description = "Maximum results") @RequestParam(defaultValue = "20") Integer limit) {
        List<MovieResponse> movies = movieService.findTopRatedMovies(minRatings, limit);
        return ResponseEntity.ok(movies);
    }

    @Operation(summary = "Get most popular movies", description = "Retrieves most popular movies by rating count")
    @GetMapping("/popular")
    public ResponseEntity<List<MovieResponse>> getPopularMovies(
            @Parameter(description = "Maximum results") @RequestParam(defaultValue = "20") Integer limit) {
        List<MovieResponse> movies = movieService.findMostPopularMovies(limit);
        return ResponseEntity.ok(movies);
    }

    @Operation(summary = "Get recent movies", description = "Retrieves recently released movies")
    @GetMapping("/recent")
    public ResponseEntity<List<MovieResponse>> getRecentMovies(
            @Parameter(description = "From year") @RequestParam(defaultValue = "2020") Integer fromYear,
            @Parameter(description = "Maximum results") @RequestParam(defaultValue = "20") Integer limit) {
        List<MovieResponse> movies = movieService.findRecentMovies(fromYear, limit);
        return ResponseEntity.ok(movies);
    }

    @Operation(summary = "Get similar movies", description = "Retrieves movies similar to the given movie")
    @GetMapping("/{movieId}/similar")
    public ResponseEntity<List<MovieResponse>> getSimilarMovies(
            @Parameter(description = "Movie ID") @PathVariable String movieId,
            @Parameter(description = "Maximum results") @RequestParam(defaultValue = "10") Integer limit) {
        List<MovieResponse> movies = movieService.findSimilarMovies(movieId, limit);
        return ResponseEntity.ok(movies);
    }

    @Operation(summary = "Get basic recommendations", description = "Gets basic movie recommendations for a user")
    @GetMapping("/recommendations/{userId}")
    public ResponseEntity<List<MovieResponse>> getRecommendations(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Minimum rating threshold") @RequestParam(defaultValue = "7.0") Double minRating,
            @Parameter(description = "Maximum results") @RequestParam(defaultValue = "20") Integer limit) {
        List<MovieResponse> movies = movieService.getBasicRecommendations(userId, minRating, limit);
        return ResponseEntity.ok(movies);
    }

    @Operation(summary = "Get movie statistics", description = "Retrieves overall movie statistics")
    @GetMapping("/stats")
    public ResponseEntity<MovieService.MovieStats> getMovieStats() {
        MovieService.MovieStats stats = movieService.getMovieStatistics();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Check if movie exists", description = "Checks if a movie exists by ID")
    @GetMapping("/{movieId}/exists")
    public ResponseEntity<Boolean> movieExists(@Parameter(description = "Movie ID") @PathVariable String movieId) {
        boolean exists = movieService.movieExists(movieId);
        return ResponseEntity.ok(exists);
    }

    @Operation(summary = "Get all genres", description = "Retrieves all available movie genres")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Genres retrieved successfully")
    })
    @GetMapping("/genres")
    public ResponseEntity<GenreListResponse> getAllGenres() {
        List<Genre> genres = genreRepository.findAllOrderedByName();
        List<String> genreNames = genres.stream()
                .map(Genre::getName)
                .toList();

        return ResponseEntity.ok(GenreListResponse.builder()
                .success(true)
                .message("Genres retrieved successfully")
                .data(genreNames)
                .build());
    }


    @Operation(summary = "Upload movie poster", description = "Uploads a poster image for a movie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Poster uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file"),
        @ApiResponse(responseCode = "404", description = "Movie not found")
    })
    @PostMapping("/{movieId}/upload-poster")
    public ResponseEntity<Map<String, String>> uploadMoviePoster(
            @Parameter(description = "Movie ID") @PathVariable String movieId,
            @Parameter(description = "Poster image file") @RequestParam("file") MultipartFile file) {

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        // Validate movie exists
        if (!movieService.movieExists(movieId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Movie not found"));
        }

        // Upload file
        String posterUrl = fileStorageService.uploadFile(file, "posters");

        // Update movie with poster URL
        movieService.updateMoviePosterUrl(movieId, posterUrl);

        return ResponseEntity.ok(Map.of(
            "success", "true",
            "message", "Poster uploaded successfully",
            "posterUrl", posterUrl
        ));
    }

    @Operation(summary = "Upload movie trailer", description = "Uploads a trailer video for a movie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trailer uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file"),
        @ApiResponse(responseCode = "404", description = "Movie not found")
    })
    @PostMapping("/{movieId}/upload-trailer")
    public ResponseEntity<Map<String, String>> uploadMovieTrailer(
            @Parameter(description = "Movie ID") @PathVariable String movieId,
            @Parameter(description = "Trailer video file") @RequestParam("file") MultipartFile file) {

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        // Validate movie exists
        if (!movieService.movieExists(movieId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Movie not found"));
        }

        // Upload file
        String trailerUrl = fileStorageService.uploadFile(file, "trailers");

        // Update movie with trailer URL
        movieService.updateMovieTrailerUrl(movieId, trailerUrl);

        return ResponseEntity.ok(Map.of(
            "success", "true",
            "message", "Trailer uploaded successfully",
            "trailerUrl", trailerUrl
        ));
    }
}