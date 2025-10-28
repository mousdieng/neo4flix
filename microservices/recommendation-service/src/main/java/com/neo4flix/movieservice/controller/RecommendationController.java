package com.neo4flix.movieservice.controller;

import com.neo4flix.movieservice.dto.*;
import com.neo4flix.movieservice.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for recommendation operations
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@Tag(name = "Recommendation Engine", description = "APIs for generating and managing movie recommendations")
public class RecommendationController {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);

    private final RecommendationService recommendationService;

    @Autowired
    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate recommendations", description = "Generate personalized movie recommendations for a user")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<RecommendationResponse>> generateRecommendations(
            @Valid @RequestBody RecommendationRequest request) {

        // Override user ID with authenticated user
        request.setUserId(getCurrentUserId());

        List<RecommendationResponse> recommendations = recommendationService.generateRecommendations(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(recommendations);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user recommendations", description = "Get existing recommendations for a user")
    public ResponseEntity<Page<RecommendationResponse>> getUserRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "recommendedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                   Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RecommendationResponse> recommendations = recommendationService.getUserRecommendations(userId, pageable);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my recommendations", description = "Get recommendations for the authenticated user")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Page<RecommendationResponse>> getMyRecommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "score") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        String userId = getCurrentUserId();

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                   Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RecommendationResponse> recommendations = recommendationService.getUserRecommendations(userId, pageable);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/user/{userId}/algorithm/{algorithm}")
    @Operation(summary = "Get recommendations by algorithm", description = "Get recommendations for a user filtered by algorithm")
    public ResponseEntity<Page<RecommendationResponse>> getUserRecommendationsByAlgorithm(
            @PathVariable String userId,
            @PathVariable String algorithm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("score").descending());

        Page<RecommendationResponse> recommendations = recommendationService
                .getUserRecommendationsByAlgorithm(userId, algorithm, pageable);
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/clicked")
    @Operation(summary = "Mark recommendation clicked", description = "Mark a recommendation as clicked by the user")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> markRecommendationClicked(
            @RequestParam String movieId) {

        String userId = getCurrentUserId();
        recommendationService.markRecommendationClicked(userId, movieId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/watched")
    @Operation(summary = "Mark recommendation watched", description = "Mark a recommendation as watched by the user")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> markRecommendationWatched(
            @RequestParam String movieId) {

        String userId = getCurrentUserId();
        recommendationService.markRecommendationWatched(userId, movieId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh recommendations", description = "Generate fresh recommendations for the authenticated user")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<RecommendationResponse>> refreshRecommendations(
            @RequestParam(defaultValue = "hybrid") String algorithm,
            @RequestParam(defaultValue = "20") Integer limit) {

        String userId = getCurrentUserId();

        List<RecommendationResponse> recommendations = recommendationService
                .refreshRecommendations(userId, algorithm, limit);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/user/{userId}/stats")
    @Operation(summary = "Get recommendation statistics", description = "Get recommendation statistics for a user")
    public ResponseEntity<Object> getRecommendationStats(@PathVariable String userId) {
        Object stats = recommendationService.getRecommendationStats(userId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/user/{userId}/similar-users")
    @Operation(summary = "Find similar users", description = "Find users with similar taste to the given user")
    public ResponseEntity<List<UserSimilarity>> findSimilarUsers(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") Integer limit) {

        List<UserSimilarity> similarUsers = recommendationService.findSimilarUsers(userId, limit);
        return ResponseEntity.ok(similarUsers);
    }

    @GetMapping("/personalized")
    @Operation(summary = "Get personalized recommendations", description = "Get personalized movie recommendations for authenticated user")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<RecommendationResponseWrapper> getPersonalizedRecommendations(
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "hybrid") String algorithm) {

        String userId = getCurrentUserId();

        List<RecommendationResponse> recommendations = recommendationService
                .refreshRecommendations(userId, algorithm, limit);

        RecommendationResponseWrapper wrapper = new RecommendationResponseWrapper();
        wrapper.setSuccess(true);
        wrapper.setMessage("Personalized recommendations generated successfully");
        wrapper.setRecommendations(recommendations);
        wrapper.setAlgorithm(algorithm);
        wrapper.setTotalResults(recommendations.size());
        wrapper.setConfidence(calculateAverageConfidence(recommendations));

        return ResponseEntity.ok(wrapper);
    }

    @GetMapping("/similar/{movieId}")
    @Operation(summary = "Get similar movies", description = "Get movies similar to the specified movie")
    public ResponseEntity<RecommendationResponseWrapper> getSimilarMovies(
            @PathVariable String movieId,
            @RequestParam(defaultValue = "10") Integer limit) {

        List<RecommendationResponse> recommendations = recommendationService
                .getSimilarMovies(movieId, limit);

        RecommendationResponseWrapper wrapper = new RecommendationResponseWrapper();
        wrapper.setSuccess(true);
        wrapper.setMessage("Similar movies found successfully");
        wrapper.setRecommendations(recommendations);
        wrapper.setAlgorithm("content_based");
        wrapper.setTotalResults(recommendations.size());
        wrapper.setConfidence(calculateAverageConfidence(recommendations));

        return ResponseEntity.ok(wrapper);
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending recommendations", description = "Get trending movie recommendations")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<RecommendationResponseWrapper> getTrendingRecommendations(
            @RequestParam(defaultValue = "10") Integer limit) {

        String userId = getCurrentUserId();

        List<RecommendationResponse> recommendations = recommendationService
                .getTrendingRecommendations(userId, limit);

        RecommendationResponseWrapper wrapper = new RecommendationResponseWrapper();
        wrapper.setSuccess(true);
        wrapper.setMessage("Trending movies found successfully");
        wrapper.setRecommendations(recommendations);
        wrapper.setAlgorithm("content_based");
        wrapper.setTotalResults(recommendations.size());
        wrapper.setConfidence(calculateAverageConfidence(recommendations));
        return ResponseEntity.ok(wrapper);
    }

    @PostMapping("/interactions")
    @Operation(summary = "Track user interaction", description = "Track user interaction with a movie for improving recommendations")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> trackInteraction(
            @RequestBody UserInteractionRequest interaction) {

        String userId = getCurrentUserId();
        interaction.setUserId(userId);

        recommendationService.trackUserInteraction(interaction);

        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // Helper method to calculate average confidence
    private double calculateAverageConfidence(List<RecommendationResponse> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return 0.0;
        }
        return recommendations.stream()
                .mapToDouble(RecommendationResponse::getScore)
                .average()
                .orElse(0.0);
    }

    @GetMapping("/by-genre")
    @Operation(summary = "Get recommendations by genre", description = "Get recommendations for specific genres with optional year filtering")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<RecommendationResponseWrapper> getRecommendationsByGenres(
            @RequestParam List<String> genres,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "hybrid") String algorithm,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer toYear) {

        String userId = getCurrentUserId();

        // Create request with genre and year filters
        RecommendationRequest request = RecommendationRequest.builder()
                .userId(userId)
                .limit(limit)
                .algorithm(algorithm)
                .build();

        List<String> genre = genres.isEmpty() ? List.of("Action") : genres;
        request.setGenre(genre);

        // Set year filters if provided
        if (fromYear != null) {
            request.setFromYear(fromYear);
        }
        if (toYear != null) {
            request.setToYear(toYear);
        }

        List<RecommendationResponse> recommendations = recommendationService
                .getRecommendationsByGenre(request);

        RecommendationResponseWrapper wrapper = new RecommendationResponseWrapper();
        wrapper.setSuccess(true);

        String message = "Genre-based recommendations found successfully";
        if (fromYear != null || toYear != null) {
            message += " (filtered by year";
            if (fromYear != null) message += " from " + fromYear;
            if (toYear != null) message += " to " + toYear;
            message += ")";
        }
        wrapper.setMessage(message);
        wrapper.setRecommendations(recommendations);
        wrapper.setAlgorithm(algorithm);
        wrapper.setTotalResults(recommendations.size());
        wrapper.setConfidence(calculateAverageConfidence(recommendations));

        return ResponseEntity.ok(wrapper);
    }

    @GetMapping("/genre/{genre}")
    @Operation(summary = "Get recommendations by genre", description = "Get recommendations for a specific genre")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<RecommendationResponse>> getRecommendationsByGenre(
            @PathVariable String genre,
            @RequestParam(defaultValue = "10") Integer limit) {

        String userId = getCurrentUserId();
        RecommendationRequest request = RecommendationRequest.builder()
                .userId(userId)
                .limit(limit)
                .algorithm("hybrid")
                .genre(List.of(genre))
                .build();

        List<RecommendationResponse> recommendations = recommendationService
                .getRecommendationsByGenre(request);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/history-based")
    @Operation(summary = "Get history-based recommendations", description = "Get recommendations based on user's watch history")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<RecommendationResponseWrapper> getHistoryBasedRecommendations(
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "collaborative") String algorithm) {

        String userId = getCurrentUserId();

        // For now, use personalized recommendations as watch history-based
        List<RecommendationResponse> recommendations = recommendationService
                .refreshRecommendations(userId, algorithm, limit);

        RecommendationResponseWrapper wrapper = new RecommendationResponseWrapper();
        wrapper.setSuccess(true);
        wrapper.setMessage("History-based recommendations generated successfully");
        wrapper.setRecommendations(recommendations);
        wrapper.setAlgorithm(algorithm);
        wrapper.setTotalResults(recommendations.size());
        wrapper.setConfidence(calculateAverageConfidence(recommendations));

        return ResponseEntity.ok(wrapper);
    }

    @GetMapping("/new-user")
    @Operation(summary = "Get new user recommendations", description = "Get popular recommendations for new users")
    public ResponseEntity<List<RecommendationResponse>> getNewUserRecommendations(
            @RequestParam(defaultValue = "10") Integer limit) {

        List<RecommendationResponse> recommendations = recommendationService
                .getNewUserRecommendations(limit);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get recommendation metrics", description = "Get recommendation accuracy metrics for the authenticated user")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Object> getRecommendationMetrics() {
        String userId = getCurrentUserId();

        Object metrics = recommendationService.calculateRecommendationMetrics(userId);
        return ResponseEntity.ok(metrics);
    }

    @PostMapping("/batch-generate")
    @Operation(summary = "Batch generate recommendations", description = "Generate recommendations for multiple users (Admin only)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> batchGenerateRecommendations(
            @RequestBody List<String> userIds,
            @RequestParam(defaultValue = "hybrid") String algorithm) {

        recommendationService.batchGenerateRecommendations(userIds, algorithm);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/cleanup")
    @Operation(summary = "Cleanup old recommendations", description = "Remove old recommendations (Admin only)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cleanupOldRecommendations(
            @RequestParam(defaultValue = "30") Integer daysOld) {

        recommendationService.cleanupOldRecommendations(daysOld);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/algorithms")
    @Operation(summary = "Get available algorithms", description = "Get list of available recommendation algorithms")
    public ResponseEntity<List<String>> getAvailableAlgorithms() {
        List<String> algorithms = List.of("collaborative", "content", "popular", "hybrid");
        return ResponseEntity.ok(algorithms);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the recommendation service is healthy")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Recommendation Service is healthy");
    }

    @PostMapping("/internal/refresh/{userId}")
    @Operation(summary = "Internal: Refresh recommendations", description = "Internal endpoint to refresh recommendations (called by other microservices)")
    public ResponseEntity<Void> internalRefreshRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "hybrid") String algorithm,
            @RequestParam(defaultValue = "20") Integer limit) {

        logger.info("Internal recommendation refresh triggered for user: {}", userId);

        try {
            recommendationService.refreshRecommendations(userId, algorithm, limit);
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            logger.error("Error in internal recommendation refresh for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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

    // ==================== SHARING RECOMMENDATIONS WITH FRIENDS ====================

    @PostMapping("/share")
    @Operation(summary = "Share recommendation with friends", description = "Share a movie recommendation with one or more friends")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> shareRecommendation(
            @Valid @RequestBody com.neo4flix.movieservice.dto.ShareRecommendationRequest request) {

        String userId = getCurrentUserId();

        int sharedCount = recommendationService.shareRecommendation(userId, request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Recommendation shared successfully");
        response.put("sharedCount", sharedCount);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/shared/received")
    @Operation(summary = "Get recommendations shared with me", description = "Get all movie recommendations shared with the current user by friends")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<com.neo4flix.movieservice.dto.SharedRecommendationResponse>> getSharedRecommendations() {
        String userId = getCurrentUserId();

        List<com.neo4flix.movieservice.dto.SharedRecommendationResponse> sharedRecommendations =
            recommendationService.getSharedRecommendations(userId);

        return ResponseEntity.ok(sharedRecommendations);
    }

    @GetMapping("/shared/sent")
    @Operation(summary = "Get recommendations I shared", description = "Get all movie recommendations the current user has shared with friends")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<com.neo4flix.movieservice.dto.SharedRecommendationResponse>> getMySharedRecommendations() {
        String userId = getCurrentUserId();

        List<com.neo4flix.movieservice.dto.SharedRecommendationResponse> sharedRecommendations =
            recommendationService.getMySharedRecommendations(userId);

        return ResponseEntity.ok(sharedRecommendations);
    }

    @PostMapping("/shared/{sharedRecommendationId}/mark-viewed")
    @Operation(summary = "Mark shared recommendation as viewed", description = "Mark a shared recommendation as viewed")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> markSharedRecommendationAsViewed(
            @PathVariable String sharedRecommendationId) {

        recommendationService.markSharedRecommendationAsViewed(sharedRecommendationId);

        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shared/unviewed-count")
    @Operation(summary = "Get unviewed shared recommendations count", description = "Get count of unviewed recommendations shared with the current user")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getUnviewedSharedCount() {
        String userId = getCurrentUserId();

        long count = recommendationService.getUnviewedSharedCount(userId);

        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
}