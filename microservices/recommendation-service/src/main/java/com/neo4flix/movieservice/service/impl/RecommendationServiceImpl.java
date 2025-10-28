package com.neo4flix.movieservice.service.impl;

import com.neo4flix.movieservice.algorithm.*;
import com.neo4flix.movieservice.dto.*;
import com.neo4flix.movieservice.model.Recommendation;
import com.neo4flix.movieservice.repository.MovieRepository;
import com.neo4flix.movieservice.repository.RecommendationRepository;
import com.neo4flix.movieservice.repository.UserRepository;
import com.neo4flix.movieservice.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of RecommendationService
 */
@Service
@Transactional
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    private final RecommendationRepository recommendationRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final com.neo4flix.movieservice.repository.SharedRecommendationRepository sharedRecommendationRepository;

    private final Map<String, RecommendationAlgorithm> algorithms;

    @Autowired
    public RecommendationServiceImpl(
            RecommendationRepository recommendationRepository,
            UserRepository userRepository,
            MovieRepository movieRepository,
            com.neo4flix.movieservice.repository.SharedRecommendationRepository sharedRecommendationRepository,
            ContentBasedFilteringAlgorithm contentBasedAlgorithm,
            CollaborativeFilteringAlgorithm collaborativeAlgorithm,
            PopularityBasedAlgorithm popularityAlgorithm,
            HybridRecommendationAlgorithm hybridAlgorithm,
            GDSCollaborativeFilteringAlgorithm gdsCollaborativeAlgorithm,
            GDSContentBasedAlgorithm gdsContentBasedAlgorithm,
            GDSPageRankAlgorithm gdsPageRankAlgorithm,
            GDSHybridAlgorithm gdsHybridAlgorithm) {

        this.recommendationRepository = recommendationRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.sharedRecommendationRepository = sharedRecommendationRepository;

        this.algorithms = new HashMap<>();
        // Original algorithms
        this.algorithms.put(collaborativeAlgorithm.getAlgorithmName(), collaborativeAlgorithm);
        this.algorithms.put(contentBasedAlgorithm.getAlgorithmName(), contentBasedAlgorithm);
        this.algorithms.put(popularityAlgorithm.getAlgorithmName(), popularityAlgorithm);
        this.algorithms.put(hybridAlgorithm.getAlgorithmName(), hybridAlgorithm);

        // GDS-based algorithms
        this.algorithms.put(gdsCollaborativeAlgorithm.getAlgorithmName(), gdsCollaborativeAlgorithm);
        this.algorithms.put(gdsContentBasedAlgorithm.getAlgorithmName(), gdsContentBasedAlgorithm);
        this.algorithms.put(gdsPageRankAlgorithm.getAlgorithmName(), gdsPageRankAlgorithm);
        this.algorithms.put(gdsHybridAlgorithm.getAlgorithmName(), gdsHybridAlgorithm);
    }

    @Override
    @CacheEvict(value = "recommendations", key = "#request.userId")
    public List<RecommendationResponse> generateRecommendations(RecommendationRequest request) {
        logger.info("Generating recommendations for user {} using {} algorithm",
                request.getUserId(), request.getAlgorithm());

        try {
            // Validate user exists
//            if (!userRepository.existsById(request.getUserId())) {
//                throw new IllegalArgumentException("User not found: " + request.getUserId());
//            }

            // Get the appropriate algorithm
            RecommendationAlgorithm algorithm = algorithms.get(request.getAlgorithm());
            if (algorithm == null) {
                throw new IllegalArgumentException("Unknown algorithm: " + request.getAlgorithm());
            }

            // Check if algorithm is applicable
            if (!algorithm.isApplicable(request)) {
                logger.warn("Algorithm {} not applicable for user {}, falling back to popular",
                        request.getAlgorithm(), request.getUserId());
                algorithm = algorithms.get("popular");
            }

            // Generate recommendations
            List<Recommendation> recommendations = algorithm.generateRecommendations(request);

            if (recommendations.isEmpty()) {
                logger.warn("No recommendations generated for user {} with algorithm {}",
                        request.getUserId(), request.getAlgorithm());
                return new ArrayList<>();
            }

            // Convert to response DTOs
            List<RecommendationResponse> responses = recommendations.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            logger.info("Successfully generated {} recommendations for user {}",
                    responses.size(), request.getUserId());

            return responses;

        } catch (Exception e) {
            logger.error("Error generating recommendations for user {}: {}",
                    request.getUserId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate recommendations", e);
        }
    }

    @Override
    @Cacheable(value = "recommendations", key = "#userId")
    @Transactional(readOnly = true)
    public Page<RecommendationResponse> getUserRecommendations(String userId, Pageable pageable) {
        Page<Recommendation> recommendations = recommendationRepository.findByUserId(userId, pageable);
        return recommendations.map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecommendationResponse> getUserRecommendationsByAlgorithm(String userId, String algorithm, Pageable pageable) {
        Page<Recommendation> recommendations = recommendationRepository.findByUserIdAndAlgorithm(userId, algorithm, pageable);
        return recommendations.map(this::convertToResponse);
    }

    @Override
    @CacheEvict(value = "recommendations", key = "#userId")
    public void markRecommendationClicked(String userId, String movieId) {
        logger.info("Marking recommendation clicked for user {} and movie {}", userId, movieId);

        recommendationRepository.updateClicked(userId, movieId, true)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found"));
    }

    @Override
    @CacheEvict(value = "recommendations", key = "#userId")
    public void markRecommendationWatched(String userId, String movieId) {
        logger.info("Marking recommendation watched for user {} and movie {}", userId, movieId);

        recommendationRepository.updateWatched(userId, movieId, true)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found"));
    }

    @Override
    @CacheEvict(value = "recommendations", key = "#userId")
    public List<RecommendationResponse> refreshRecommendations(String userId, String algorithm, Integer limit) {
        logger.info("Refreshing recommendations for user {} with algorithm {}", userId, algorithm);

        // Delete old recommendations to ensure fresh results
        recommendationRepository.deleteAllByUserId(userId);

        // Generate new recommendations
        RecommendationRequest request = RecommendationRequest.builder()
                .userId(userId)
                .limit(limit)
                .algorithm(algorithm)
                .build();
        return generateRecommendations(request);
    }

    @Override
    @Transactional(readOnly = true)
    public Object getRecommendationStats(String userId) {
        return recommendationRepository.getRecommendationStats(userId)
                .orElse(createEmptyStats());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSimilarity> findSimilarUsers(String userId, Integer limit) {
        return recommendationRepository.findSimilarUsers(userId, 3, 0.3, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getTrendingRecommendations(String userId, Integer limit) {
        // Get recommendations that are popular among similar users
        RecommendationRequest request = RecommendationRequest.builder()
                .limit(limit)
                .userId(userId)
                .algorithm("collaborative")
                .build();
        return generateRecommendations(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendationsByGenre(RecommendationRequest request) {
//        RecommendationRequest request = RecommendationRequest.builder()
//                .limit(limit)
//                .userId(userId)
//                .algorithm(algorithms)
//                .build();
//        request.setGenre(genre);
        return generateRecommendations(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getNewUserRecommendations(Integer limit) {
        // For new users, return popular movies
        RecommendationRequest request = RecommendationRequest.builder()
                .limit(limit)
                .userId("")
                .algorithm("popular")
                .build();;

//        RecommendationAlgorithm popularityAlgorithm = algorithms.get("popular");
//        List<Recommendation> recommendations = popularityAlgorithm.generateRecommendations(request);
//
//        return recommendations.stream()
//                .map(this::convertToResponse)
//                .collect(Collectors.toList());
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Object calculateRecommendationMetrics(String userId) {
        // This would calculate metrics like precision, recall, F1-score
        // Based on user interactions with recommendations
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("precision", 0.0);
        metrics.put("recall", 0.0);
        metrics.put("f1Score", 0.0);
        metrics.put("clickThroughRate", 0.0);
        metrics.put("conversionRate", 0.0);
        return metrics;
    }

    @Override
    @Async
    public void batchGenerateRecommendations(List<String> userIds, String algorithm) {
        logger.info("Starting batch recommendation generation for {} users with algorithm {}",
                userIds.size(), algorithm);

        for (String userId : userIds) {
            try {
                RecommendationRequest request = RecommendationRequest.builder()
                        .limit(20)
                        .userId(userId)
                        .algorithm(algorithm)
                        .build();
                generateRecommendations(request);

                // Small delay to avoid overwhelming the system
                Thread.sleep(100);

            } catch (Exception e) {
                logger.error("Error generating recommendations for user {} in batch: {}",
                        userId, e.getMessage());
            }
        }

        logger.info("Completed batch recommendation generation for {} users", userIds.size());
    }

    @Override
    public void cleanupOldRecommendations(Integer daysOld) {
        logger.info("Cleaning up recommendations older than {} days", daysOld);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);

        // This would require a repository method to delete old recommendations
        // For now, we'll just log the action
        logger.info("Would delete recommendations older than {}", cutoffDate);
    }

    private List<Recommendation> saveRecommendations(List<Recommendation> recommendations) {
        List<Recommendation> savedRecommendations = new ArrayList<>();

        for (Recommendation recommendation : recommendations) {
            try {
                // Use custom save method to create RECOMMENDED relationship
                Long id = recommendationRepository.saveRecommendation(
                        recommendation.getUserId(),
                        recommendation.getMovieId(),
                        recommendation.getScore(),
                        recommendation.getAlgorithm(),
                        recommendation.getReason(),
                        recommendation.getRecommendedAt().toString(),
                        recommendation.getClicked(),
                        recommendation.getWatched()
                );

                if (id != null) {
                    recommendation.setId(id);
                    savedRecommendations.add(recommendation);
                    logger.debug("Saved recommendation with id {} for user {} and movie {}",
                            id, recommendation.getUserId(), recommendation.getMovieId());
                }
            } catch (Exception e) {
                logger.warn("Error saving recommendation for user {} and movie {}: {}",
                        recommendation.getUserId(), recommendation.getMovieId(), e.getMessage(), e);
            }
        }

        logger.info("Successfully saved {} out of {} recommendations",
                savedRecommendations.size(), recommendations.size());
        return savedRecommendations;
    }

    private RecommendationResponse convertToResponse(Recommendation recommendation) {
        RecommendationResponse response = new RecommendationResponse(recommendation);

        // Enhance with movie details if needed
        try {
            // This would typically fetch movie details from the movie repository
            // For now, we'll use placeholder values
            response.setMovieTitle("Movie Title"); // Would be fetched from movie service
            response.setMovieRating(4.5); // Would be fetched from movie service

        } catch (Exception e) {
            logger.warn("Error fetching movie details for movie {}: {}",
                    recommendation.getMovieId(), e.getMessage());
        }

        return response;
    }

    private Object createEmptyStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecommendations", 0);
        stats.put("clickedCount", 0);
        stats.put("watchedCount", 0);
        stats.put("averageScore", 0.0);
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getSimilarMovies(String movieId, Integer limit) {
        logger.info("Getting similar movies for movie {} with limit {}", movieId, limit);

        try {
            // Use repository method to find similar movies based on genres, directors, actors
            List<Object[]> rawRecommendations = movieRepository.findSimilarMovies(movieId, limit);

            List<RecommendationResponse> recommendations = new ArrayList<>();

            for (Object[] row : rawRecommendations) {
                // row[0] is Movie object, row[1] is similarity score
                Object movieObj = row[0];
                Double similarityScore = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;

                // Extract movie details using reflection or casting
                String similarMovieId = "";
                String movieTitle = "Unknown Movie";
                Double movieRating = 0.0;

                try {
                    // Access Movie object properties
                    similarMovieId = (String) movieObj.getClass().getMethod("getId").invoke(movieObj);
                    movieTitle = (String) movieObj.getClass().getMethod("getTitle").invoke(movieObj);
                    Object ratingObj = movieObj.getClass().getMethod("getAverageRating").invoke(movieObj);
                    movieRating = ratingObj != null ? (Double) ratingObj : 0.0;
                } catch (Exception e) {
                    logger.warn("Error extracting movie details: {}", e.getMessage());
                }

                String reason = "Similar based on shared genres, directors, and actors";

                Recommendation recommendation = Recommendation.builder()
                        .userId(similarMovieId)
                        .score(normalizeScore(similarityScore))
                        .algorithm("content")
                        .reason(reason)
                        .build();

                RecommendationResponse response = convertToResponse(recommendation);
                response.setMovieTitle(movieTitle);
                response.setMovieRating(movieRating);

                recommendations.add(response);
            }

            logger.info("Found {} similar movies for movie {}", recommendations.size(), movieId);
            return recommendations;

        } catch (Exception e) {
            logger.error("Error getting similar movies for movie {}: {}", movieId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Normalize score to 0-1 range
     */
    private double normalizeScore(Double rawScore) {
        if (rawScore == null) {
            return 0.0;
        }
        // Normalize similarity scores (typically 0-10) to 0-1 range
        return Math.min(1.0, Math.max(0.0, rawScore / 10.0));
    }

    @Override
    public void trackUserInteraction(UserInteractionRequest interaction) {
        logger.info("Tracking user interaction for user {} and movie {} with action {}",
                interaction.getUserId(), interaction.getMovieId(), interaction.getAction());

        try {
            // Track the interaction based on action type
            switch (interaction.getAction().toLowerCase()) {
                case "view":
                case "click":
                    markRecommendationClicked(interaction.getUserId(), interaction.getMovieId());
                    break;
                case "watch":
                case "watched":
                    markRecommendationWatched(interaction.getUserId(), interaction.getMovieId());
                    break;
                case "rate":
                    // Rating interactions are typically handled by the rating service
                    logger.info("Rating interaction tracked for user {} on movie {} with value {}",
                            interaction.getUserId(), interaction.getMovieId(), interaction.getValue());
                    break;
                case "like":
                case "share":
                case "watchlist_add":
                case "watchlist_remove":
                    // These interactions could be stored for future recommendation improvements
                    logger.info("Interaction {} tracked for user {} on movie {}",
                            interaction.getAction(), interaction.getUserId(), interaction.getMovieId());
                    break;
                default:
                    logger.warn("Unknown interaction action: {}", interaction.getAction());
            }

        } catch (Exception e) {
            logger.error("Error tracking user interaction: {}", e.getMessage(), e);
            // Don't throw exception - tracking failures shouldn't break the user experience
        }
    }

    // ==================== SHARING WITH FRIENDS ====================

    @Override
    public int shareRecommendation(String userId, com.neo4flix.movieservice.dto.ShareRecommendationRequest request) {
        logger.info("User {} sharing movie {} with {} friends", userId, request.getMovieId(), request.getFriendIds().size());

        int sharedCount = 0;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        for (String friendId : request.getFriendIds()) {
            try {
                // Check if already shared
                if (sharedRecommendationRepository.existsByFromUserAndToUserAndMovie(userId, friendId, request.getMovieId())) {
                    logger.info("Movie {} already shared with friend {}, skipping", request.getMovieId(), friendId);
                    continue;
                }

                // Create shared recommendation
                com.neo4flix.movieservice.model.SharedRecommendation sharedRec =
                    com.neo4flix.movieservice.model.SharedRecommendation.builder()
                        .fromUserId(userId)
                        .toUserId(friendId)
                        .movieId(request.getMovieId())
                        .message(request.getMessage())
                        .sharedAt(now)
                        .viewed(false)
                        .build();

                sharedRecommendationRepository.save(sharedRec);
                sharedCount++;

                logger.info("Shared movie {} with friend {}", request.getMovieId(), friendId);

            } catch (Exception e) {
                logger.error("Error sharing movie {} with friend {}: {}", request.getMovieId(), friendId, e.getMessage());
            }
        }

        logger.info("Successfully shared movie {} with {} out of {} friends",
            request.getMovieId(), sharedCount, request.getFriendIds().size());

        return sharedCount;
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.neo4flix.movieservice.dto.SharedRecommendationResponse> getSharedRecommendations(String userId) {
        logger.info("Getting shared recommendations for user {}", userId);
        return sharedRecommendationRepository.findSharedWithUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.neo4flix.movieservice.dto.SharedRecommendationResponse> getMySharedRecommendations(String userId) {
        logger.info("Getting recommendations shared by user {}", userId);
        return sharedRecommendationRepository.findSharedByUser(userId);
    }

    @Override
    public void markSharedRecommendationAsViewed(String sharedRecommendationId) {
        logger.info("Marking shared recommendation {} as viewed", sharedRecommendationId);
        sharedRecommendationRepository.markAsViewed(sharedRecommendationId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnviewedSharedCount(String userId) {
        return sharedRecommendationRepository.countUnviewedForUser(userId);
    }
}