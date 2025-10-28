package com.neo4flix.watchlistservice.controller;

import com.neo4flix.watchlistservice.dto.*;
import com.neo4flix.watchlistservice.exception.AuthenticationException;
import com.neo4flix.watchlistservice.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Watchlist operations
 */
@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
@Validated
@Tag(name = "Watchlist", description = "Watchlist management endpoints")
public class WatchlistController {

    private static final Logger logger = LoggerFactory.getLogger(WatchlistController.class);

    private final WatchlistService watchlistService;

    /**
     * Add a movie to the current user's watchlist
     */
    @PostMapping
    @Operation(summary = "Add movie to watchlist", description = "Add a movie to the authenticated user's watchlist")
    public ResponseEntity<WatchlistResponse> addToWatchlist(
            @Valid @RequestBody AddToWatchlistRequest request) {

        String userId = getCurrentUserId();
        logger.info("Adding movie {} to watchlist for user {}", request.getMovieId(), userId);

        // Set the user ID from authentication context
        request.setUserId(userId);

        WatchlistResponse response = watchlistService.addToWatchlist(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Remove a movie from the current user's watchlist
     */
    @DeleteMapping("/movies/{movieId}")
    @Operation(summary = "Remove movie from watchlist", description = "Remove a movie from the authenticated user's watchlist")
    public ResponseEntity<Void> removeFromWatchlist(
            @Parameter(description = "Movie ID") @PathVariable String movieId) {

        String userId = getCurrentUserId();
        logger.info("Removing movie {} from watchlist for user {}", movieId, userId);

        watchlistService.removeFromWatchlist(userId, movieId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get the current user's watchlist with pagination and filtering
     */
    @GetMapping
    @Operation(summary = "Get user watchlist", description = "Get the authenticated user's watchlist with pagination and filtering options")
    public ResponseEntity<WatchlistPageResponse> getUserWatchlist(
            @Parameter(description = "Page number (0-based)") @Min(0) @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @Min(1) @Max(100) @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "Filter by priority (1=High, 2=Medium, 3=Low)") @RequestParam(required = false) Integer priority,
            @Parameter(description = "Filter by watched status") @RequestParam(defaultValue = "false") Boolean watched,
            @Parameter(description = "Sort by field (addedAt, priority, title)") @RequestParam(defaultValue = "addedAt") String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)") @RequestParam(defaultValue = "DESC") String sortDirection,
            @Parameter(description = "Filter by genres") @RequestParam(required = false) String[] genres,
            @Parameter(description = "Filter by from year") @RequestParam(required = false) Integer fromYear,
            @Parameter(description = "Filter by to year") @RequestParam(required = false) Integer toYear) {

        String userId = getCurrentUserId();
        logger.info("Getting watchlist for user {} with filters", userId);

        WatchlistQueryParams params = WatchlistQueryParams.builder()
                .page(page)
                .pageSize(pageSize)
                .priority(priority)
                .watched(watched)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .genres(genres)
                .fromYear(fromYear)
                .toYear(toYear)
                .build();

        WatchlistPageResponse response = watchlistService.getUserWatchlist(userId, params);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the current user's complete watchlist (no pagination)
     */
    @GetMapping("/all")
    @Operation(summary = "Get all watchlist items", description = "Get all items in the authenticated user's watchlist without pagination")
    public ResponseEntity<List<WatchlistResponse>> getUserWatchlistSimple() {
        String userId = getCurrentUserId();
        logger.info("Getting complete watchlist for user {}", userId);

        List<WatchlistResponse> response = watchlistService.getUserWatchlistSimple(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a watchlist entry
     */
    @PatchMapping("/movies/{movieId}")
    @Operation(summary = "Update watchlist entry", description = "Update a watchlist entry (priority, notes, watched status)")
    public ResponseEntity<WatchlistResponse> updateWatchlistEntry(
            @Parameter(description = "Movie ID") @PathVariable String movieId,
            @Valid @RequestBody UpdateWatchlistRequest request) {

        String userId = getCurrentUserId();
        logger.info("Updating watchlist entry for user {} and movie {}", userId, movieId);

        WatchlistResponse response = watchlistService.updateWatchlistEntry(userId, movieId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark a movie as watched or unwatched
     */
    @PutMapping("/movies/{movieId}/watched")
    @Operation(summary = "Mark as watched/unwatched", description = "Mark a movie as watched or unwatched")
    public ResponseEntity<WatchlistResponse> markAsWatched(
            @Parameter(description = "Movie ID") @PathVariable String movieId,
            @Parameter(description = "Watched status") @RequestParam(defaultValue = "true") Boolean watched) {

        String userId = getCurrentUserId();
        logger.info("Marking movie {} as {} for user {}", movieId, watched ? "watched" : "unwatched", userId);

        WatchlistResponse response = watchlistService.markAsWatched(userId, movieId, watched);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if a movie is in the current user's watchlist
     */
    @GetMapping("/movies/{movieId}/check")
    @Operation(summary = "Check if movie is in watchlist", description = "Check if a movie is in the authenticated user's watchlist")
    public ResponseEntity<WatchlistCheckResponse> isInWatchlist(
            @Parameter(description = "Movie ID") @PathVariable String movieId) {

        String userId = getCurrentUserId();
        logger.debug("Checking if movie {} is in watchlist for user {}", movieId, userId);

        WatchlistCheckResponse response = watchlistService.isInWatchlist(userId, movieId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get watchlist statistics for the current user
     */
    @GetMapping("/stats")
    @Operation(summary = "Get watchlist statistics", description = "Get statistics about the authenticated user's watchlist")
    public ResponseEntity<WatchlistPageResponse.WatchlistStats> getWatchlistStats() {
        String userId = getCurrentUserId();
        logger.info("Getting watchlist stats for user {}", userId);

        WatchlistPageResponse.WatchlistStats stats = watchlistService.getWatchlistStats(userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Clear all watched movies from the current user's watchlist
     */
    @DeleteMapping("/watched")
    @Operation(summary = "Clear watched movies", description = "Remove all watched movies from the authenticated user's watchlist")
    public ResponseEntity<Void> clearWatchedMovies() {
        String userId = getCurrentUserId();
        logger.info("Clearing watched movies for user {}", userId);

        watchlistService.clearWatchedMovies(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get a specific watchlist entry by ID
     */
    @GetMapping("/{watchlistId}")
    @Operation(summary = "Get watchlist entry", description = "Get a specific watchlist entry by ID")
    public ResponseEntity<WatchlistResponse> getWatchlistEntry(
            @Parameter(description = "Watchlist entry ID") @PathVariable String watchlistId) {

        logger.info("Getting watchlist entry {}", watchlistId);

        WatchlistResponse response = watchlistService.getWatchlistEntry(watchlistId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get unwatched movies from watchlist
     */
    @GetMapping("/unwatched")
    @Operation(summary = "Get unwatched movies", description = "Get all unwatched movies from the authenticated user's watchlist")
    public ResponseEntity<WatchlistPageResponse> getUnwatchedMovies(
            @Parameter(description = "Page number (0-based)") @Min(0) @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @Min(1) @Max(100) @RequestParam(defaultValue = "20") Integer pageSize) {

        String userId = getCurrentUserId();
        logger.info("Getting unwatched movies for user {}", userId);

        WatchlistQueryParams params = WatchlistQueryParams.builder()
                .page(page)
                .pageSize(pageSize)
                .watched(false)
                .sortBy("priority")
                .sortDirection("ASC")
                .build();

        WatchlistPageResponse response = watchlistService.getUserWatchlist(userId, params);
        return ResponseEntity.ok(response);
    }

    /**
     * Get high priority unwatched movies
     */
    @GetMapping("/priority/high")
    @Operation(summary = "Get high priority movies", description = "Get high priority unwatched movies from the authenticated user's watchlist")
    public ResponseEntity<WatchlistPageResponse> getHighPriorityMovies(
            @Parameter(description = "Page number (0-based)") @Min(0) @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @Min(1) @Max(100) @RequestParam(defaultValue = "20") Integer pageSize) {

        String userId = getCurrentUserId();
        logger.info("Getting high priority movies for user {}", userId);

        WatchlistQueryParams params = WatchlistQueryParams.builder()
                .page(page)
                .pageSize(pageSize)
                .priority(1)
                .watched(false)
                .sortBy("addedAt")
                .sortDirection("DESC")
                .build();

        WatchlistPageResponse response = watchlistService.getUserWatchlist(userId, params);
        return ResponseEntity.ok(response);
    }

    /**
     * Bulk add movies to watchlist
     */
    @PostMapping("/bulk")
    @Operation(summary = "Bulk add to watchlist", description = "Add multiple movies to the authenticated user's watchlist")
    public ResponseEntity<List<WatchlistResponse>> bulkAddToWatchlist(
            @Valid @RequestBody List<AddToWatchlistRequest> requests) {

        String userId = getCurrentUserId();
        logger.info("Bulk adding {} movies to watchlist for user {}", requests.size(), userId);

        List<WatchlistResponse> responses = requests.stream()
                .peek(request -> request.setUserId(userId))
                .map(watchlistService::addToWatchlist)
                .toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    // ========================================================================
    // HELPER METHODS - JWT Authentication
    // ========================================================================

    /**
     * Extract user ID from JWT authentication context
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() != null) {
            // Extract user ID from authentication details (set by JWT filter)
            Object details = authentication.getDetails();
            if (details.getClass().getSimpleName().equals("UserAuthenticationDetails")) {
                try {
                    return (String) details.getClass().getMethod("getUserId").invoke(details);
                } catch (Exception e) {
                    throw new AuthenticationException("Unable to extract user ID from authentication", e);
                }
            }
        }
        throw new AuthenticationException("User ID not found in authentication context");
    }

    /**
     * Extract username from JWT authentication context
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getName();
        }
        throw new AuthenticationException("Username not found in authentication context");
    }
}