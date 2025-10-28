package com.neo4flix.movieservice.algorithm;

import com.neo4flix.movieservice.dto.RecommendationRequest;
import com.neo4flix.movieservice.model.Recommendation;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Hybrid Recommendation Algorithm Implementation
 * Combines collaborative filtering, content-based filtering, and popularity-based recommendations
 */
@Component
@RequiredArgsConstructor
public class HybridRecommendationAlgorithm implements RecommendationAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(HybridRecommendationAlgorithm.class);

    private final CollaborativeFilteringAlgorithm collaborativeAlgorithm;
    private final ContentBasedFilteringAlgorithm contentBasedAlgorithm;
    private final PopularityBasedAlgorithm popularityAlgorithm;

    // Weights for combining different algorithms
    private static final double COLLABORATIVE_WEIGHT = 0.5;
    private static final double CONTENT_WEIGHT = 0.3;
    private static final double POPULARITY_WEIGHT = 0.2;


    @Override
    public String getAlgorithmName() {
        return "hybrid";
    }

    @Override
    public List<Recommendation> generateRecommendations(RecommendationRequest request) {
        logger.info("Generating hybrid recommendations for user: {}", request.getUserId());

        try {
            // Create separate requests for each algorithm with higher limits
            int algorithmLimit = request.getLimit() * 2; // Get more from each algorithm for better mixing

            RecommendationRequest collaborativeRequest = createAlgorithmRequest(request, "collaborative", algorithmLimit);
            RecommendationRequest contentRequest = createAlgorithmRequest(request, "content", algorithmLimit);
            RecommendationRequest popularityRequest = createAlgorithmRequest(request, "popular", algorithmLimit);

            // Get recommendations from each algorithm
            List<Recommendation> collaborativeRecs = collaborativeAlgorithm.generateRecommendations(collaborativeRequest);
            List<Recommendation> contentRecs = contentBasedAlgorithm.generateRecommendations(contentRequest);
            List<Recommendation> popularityRecs = popularityAlgorithm.generateRecommendations(popularityRequest);

            // Combine and weight the recommendations
            Map<String, CombinedRecommendation> combinedRecommendations = combineRecommendations(
                    collaborativeRecs, contentRecs, popularityRecs
            );

            // Convert to final recommendations and add randomization for diversity
            List<Recommendation> finalRecommendations = combinedRecommendations.values().stream()
                    .map(this::createFinalRecommendation)
                    .sorted((r1, r2) -> {
                        // Add slight randomization to scores within 10% range to provide diversity
                        double score1 = r1.getScore() * (0.95 + Math.random() * 0.1);
                        double score2 = r2.getScore() * (0.95 + Math.random() * 0.1);
                        return Double.compare(score2, score1);
                    })
                    .limit(request.getLimit())
                    .collect(Collectors.toList());

            logger.info("==============================================================================");
            logger.info("Generated {} hybrid recommendations (from {} collaborative, {} content, {} popularity)",
                    finalRecommendations.size(), collaborativeRecs.size(), contentRecs.size(), popularityRecs.size());
            logger.info("==============================================================================");

            return finalRecommendations;

        } catch (Exception e) {
            logger.error("Error generating hybrid recommendations for user {}: {}",
                    request.getUserId(), e.getMessage(), e);

            // Fallback to popularity-based recommendations
            logger.info("Falling back to popularity-based recommendations");
            return popularityAlgorithm.generateRecommendations(request);
        }
    }

    private RecommendationRequest createAlgorithmRequest(RecommendationRequest original, String algorithm, int limit) {
        RecommendationRequest request = new RecommendationRequest();
        request.setUserId(original.getUserId());
        request.setAlgorithm(algorithm);
        request.setLimit(limit);
        request.setMinRating(original.getMinRating());
        request.setIncludeWatched(original.getIncludeWatched());
        request.setGenre(original.getGenre());
        request.setFromYear(original.getFromYear());
        request.setToYear(original.getToYear());
        return request;
    }

    private Map<String, CombinedRecommendation> combineRecommendations(
            List<Recommendation> collaborative,
            List<Recommendation> content,
            List<Recommendation> popularity) {

        Map<String, CombinedRecommendation> combined = new HashMap<>();

        // Add collaborative filtering recommendations
        for (Recommendation rec : collaborative) {
            combined.computeIfAbsent(rec.getMovieId(), k -> new CombinedRecommendation(rec.getUserId(), rec.getMovieId()))
                    .addCollaborativeScore(rec.getScore(), rec.getReason());
        }

        // Add content-based recommendations
        for (Recommendation rec : content) {
            combined.computeIfAbsent(rec.getMovieId(), k -> new CombinedRecommendation(rec.getUserId(), rec.getMovieId()))
                    .addContentScore(rec.getScore(), rec.getReason());
        }

        // Add popularity-based recommendations
        for (Recommendation rec : popularity) {
            combined.computeIfAbsent(rec.getMovieId(), k -> new CombinedRecommendation(rec.getUserId(), rec.getMovieId()))
                    .addPopularityScore(rec.getScore(), rec.getReason());
        }

        return combined;
    }

    private Recommendation createFinalRecommendation(CombinedRecommendation combined) {
        double finalScore = calculateWeightedScore(combined);
        String reason = buildCombinedReason(combined);

        return Recommendation.builder()
                .userId(combined.getUserId())
                .score(finalScore)
                .movieId(combined.getMovieId())
                .algorithm(getAlgorithmName())
                .reason(reason)
                .build();
    }

    private double calculateWeightedScore(CombinedRecommendation combined) {
        double score = 0.0;
        double totalWeight = 0.0;

        if (combined.getCollaborativeScore() > 0) {
            score += combined.getCollaborativeScore() * COLLABORATIVE_WEIGHT;
            totalWeight += COLLABORATIVE_WEIGHT;
        }

        if (combined.getContentScore() > 0) {
            score += combined.getContentScore() * CONTENT_WEIGHT;
            totalWeight += CONTENT_WEIGHT;
        }

        if (combined.getPopularityScore() > 0) {
            score += combined.getPopularityScore() * POPULARITY_WEIGHT;
            totalWeight += POPULARITY_WEIGHT;
        }

        // Normalize by actual total weight (in case not all algorithms contributed)
        return totalWeight > 0 ? score / totalWeight : 0.0;
    }

    private String buildCombinedReason(CombinedRecommendation combined) {
        List<String> reasons = new ArrayList<>();

        if (combined.getCollaborativeReason() != null) {
            reasons.add("Similar users liked this");
        }
        if (combined.getContentReason() != null) {
            reasons.add("Matches your preferences");
        }
        if (combined.getPopularityReason() != null) {
            reasons.add("Highly rated by many users");
        }

        return "Recommended because: " + String.join(", ", reasons);
    }

    @Override
    public double calculateScore(String userId, String movieId) {
        try {
            double collaborativeScore = collaborativeAlgorithm.calculateScore(userId, movieId);
            double contentScore = contentBasedAlgorithm.calculateScore(userId, movieId);
            double popularityScore = popularityAlgorithm.calculateScore(userId, movieId);

            return (collaborativeScore * COLLABORATIVE_WEIGHT +
                    contentScore * CONTENT_WEIGHT +
                    popularityScore * POPULARITY_WEIGHT);

        } catch (Exception e) {
            logger.error("Error calculating hybrid score for user {} and movie {}: {}",
                    userId, movieId, e.getMessage());
            return 0.0;
        }
    }

    @Override
    public boolean isApplicable(RecommendationRequest request) {
        return "hybrid".equals(request.getAlgorithm());
    }

    /**
     * Helper class to combine recommendations from different algorithms
     */
    private static class CombinedRecommendation {
        private final String userId;
        private final String movieId;
        private double collaborativeScore = 0.0;
        private double contentScore = 0.0;
        private double popularityScore = 0.0;
        private String collaborativeReason;
        private String contentReason;
        private String popularityReason;

        public CombinedRecommendation(String userId, String movieId) {
            this.userId = userId;
            this.movieId = movieId;
        }

        public void addCollaborativeScore(double score, String reason) {
            this.collaborativeScore = score;
            this.collaborativeReason = reason;
        }

        public void addContentScore(double score, String reason) {
            this.contentScore = score;
            this.contentReason = reason;
        }

        public void addPopularityScore(double score, String reason) {
            this.popularityScore = score;
            this.popularityReason = reason;
        }

        // Getters
        public String getUserId() { return userId; }
        public String getMovieId() { return movieId; }
        public double getCollaborativeScore() { return collaborativeScore; }
        public double getContentScore() { return contentScore; }
        public double getPopularityScore() { return popularityScore; }
        public String getCollaborativeReason() { return collaborativeReason; }
        public String getContentReason() { return contentReason; }
        public String getPopularityReason() { return popularityReason; }
    }
}