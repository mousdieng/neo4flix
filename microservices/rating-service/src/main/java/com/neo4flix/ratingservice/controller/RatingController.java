package com.neo4flix.ratingservice.controller;

import com.neo4flix.ratingservice.dto.*;
import com.neo4flix.ratingservice.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST Controller for rating operations
 */
@RestController
@RequestMapping("/api/v1/ratings")
@Tag(name = "Rating Management", description = "APIs for managing movie ratings")
public class RatingController {

    private final RatingService ratingService;

    @Autowired
    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    @Operation(summary = "Create a new rating", description = "Rate a movie (requires authentication)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<RatingResponse> createRating(@Valid @RequestBody CreateRatingRequest request) {
        String userId = getCurrentUserId();
        String username = getCurrentUsername();

        RatingResponse response = ratingService.createRating(userId, username, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/movie/{movieId}")
    @Operation(summary = "Update a rating", description = "Update an existing rating (requires authentication)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<RatingResponse> updateRating(
            @PathVariable String movieId,
            @Valid @RequestBody UpdateRatingRequest request) {
        String userId = getCurrentUserId();

        RatingResponse response = ratingService.updateRating(userId, movieId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/movie/{movieId}")
    @Operation(summary = "Delete a rating", description = "Delete a rating (requires authentication)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRating(@PathVariable String movieId) {
        String userId = getCurrentUserId();

        ratingService.deleteRating(userId, movieId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/movie/{movieId}")
    @Operation(summary = "Get user's rating for a movie", description = "Get current user's rating for a specific movie")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<RatingResponse> getUserRatingForMovie(@PathVariable String movieId) {
        String userId = getCurrentUserId();

        RatingResponse response = ratingService.getRating(userId, movieId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/movie/{movieId}/user")
    @Operation(summary = "Get user's rating for a movie", description = "Get current user's rating for a specific movie")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<RatingResponse> getUserRatingForMovieAlt(@PathVariable String movieId) {
        String userId = getCurrentUserId();

        RatingResponse response = ratingService.getRating(userId, movieId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's ratings", description = "Get all ratings by a specific user")
    public ResponseEntity<Page<RatingResponse>> getUserRatings(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "ratedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                   Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RatingResponse> ratings = ratingService.getUserRatings(userId, pageable);
        return ResponseEntity.ok(ratings);
    }

    @GetMapping("/movie/{movieId}/all")
    @Operation(summary = "Get movie ratings", description = "Get all ratings for a specific movie")
    public ResponseEntity<Page<RatingResponse>> getMovieRatings(
            @PathVariable String movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "ratedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                   Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RatingResponse> ratings = ratingService.getMovieRatings(movieId, pageable);
        return ResponseEntity.ok(ratings);
    }

    @GetMapping("/movie/{movieId}/stats")
    @Operation(summary = "Get movie rating statistics", description = "Get rating statistics for a specific movie")
    public ResponseEntity<MovieRatingStats> getMovieRatingStats(@PathVariable String movieId) {
        MovieRatingStats stats = ratingService.getMovieRatingStats(movieId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent ratings", description = "Get recently created ratings")
    public ResponseEntity<List<RatingResponse>> getRecentRatings(
            @RequestParam(defaultValue = "10") int limit) {

        List<RatingResponse> recentRatings = ratingService.getRecentRatings(limit);
        return ResponseEntity.ok(recentRatings);
    }

    @GetMapping("/top-rated-movies")
    @Operation(summary = "Get top rated movies", description = "Get movies with highest average ratings")
    public ResponseEntity<List<MovieRatingStats>> getTopRatedMovies(
            @RequestParam(defaultValue = "5") int minRatings,
            @RequestParam(defaultValue = "10") int limit) {

        List<MovieRatingStats> topMovies = ratingService.getTopRatedMovies(minRatings, limit);
        return ResponseEntity.ok(topMovies);
    }

    @GetMapping("/user/{userId}/average")
    @Operation(summary = "Get user's average rating", description = "Get average rating given by a user")
    public ResponseEntity<Double> getUserAverageRating(@PathVariable String userId) {
        Double averageRating = ratingService.getUserAverageRating(userId);
        return ResponseEntity.ok(averageRating);
    }

    @GetMapping("/user/{userId}/count")
    @Operation(summary = "Get user's ratings count", description = "Get total number of ratings by a user")
    public ResponseEntity<Integer> getUserRatingsCount(@PathVariable String userId) {
        Integer count = ratingService.getUserRatingsCount(userId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/movie/{movieId}/count")
    @Operation(summary = "Get movie's ratings count", description = "Get total number of ratings for a movie")
    public ResponseEntity<Integer> getMovieRatingsCount(@PathVariable String movieId) {
        Integer count = ratingService.getMovieRatingsCount(movieId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/range")
    @Operation(summary = "Get ratings by range", description = "Get ratings within a specific rating range")
    public ResponseEntity<Page<RatingResponse>> getRatingsByRange(
            @RequestParam Double minRating,
            @RequestParam Double maxRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("ratedAt").descending());

        Page<RatingResponse> ratings = ratingService.getRatingsByRange(minRating, maxRating, pageable);
        return ResponseEntity.ok(ratings);
    }

    @GetMapping("/after")
    @Operation(summary = "Get ratings after date", description = "Get ratings created after a specific date")
    public ResponseEntity<Page<RatingResponse>> getRatingsAfterDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("ratedAt").descending());

        Page<RatingResponse> ratings = ratingService.getRatingsAfterDate(date, pageable);
        return ResponseEntity.ok(ratings);
    }

    @GetMapping("/user/{userId}/movie/{movieId}/exists")
    @Operation(summary = "Check if user rated movie", description = "Check if a user has rated a specific movie")
    public ResponseEntity<Boolean> hasUserRatedMovie(
            @PathVariable String userId,
            @PathVariable String movieId) {

        Boolean hasRated = ratingService.hasUserRatedMovie(userId, movieId);
        return ResponseEntity.ok(hasRated);
    }

    @GetMapping("/my")
    @Operation(summary = "Get current user's ratings", description = "Get all ratings by the authenticated user")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Page<RatingResponse>> getMyRatings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "ratedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        String userId = getCurrentUserId();

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                   Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RatingResponse> ratings = ratingService.getUserRatings(userId, pageable);
        return ResponseEntity.ok(ratings);
    }

    @GetMapping("/my/average")
    @Operation(summary = "Get current user's average rating", description = "Get average rating given by the authenticated user")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Double> getMyAverageRating() {
        String userId = getCurrentUserId();
        Double averageRating = ratingService.getUserAverageRating(userId);
        return ResponseEntity.ok(averageRating);
    }

    @GetMapping("/my/count")
    @Operation(summary = "Get current user's ratings count", description = "Get total number of ratings by the authenticated user")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Integer> getMyRatingsCount() {
        String userId = getCurrentUserId();
        Integer count = ratingService.getUserRatingsCount(userId);
        return ResponseEntity.ok(count);
    }

    // Helper methods to extract user information from JWT
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() != null) {
            // Extract user ID from authentication details (set by JWT filter)
            Object details = authentication.getDetails();
            if (details.getClass().getSimpleName().equals("UserAuthenticationDetails")) {
                try {
                    return (String) details.getClass().getMethod("getUserId").invoke(details);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to extract user ID from authentication", e);
                }
            }
        }
        throw new RuntimeException("User ID not found in authentication context");
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getName();
        }
        throw new RuntimeException("Username not found in authentication context");
    }
}