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
 * GDS-based Hybrid Recommendation Algorithm.
 * Combines multiple GDS algorithms with weighted scoring:
 * - GDS Collaborative Filtering (Node Similarity): 40%
 * - GDS Content-Based (Cosine Similarity): 30%
 * - GDS PageRank (Popularity): 30%
 *
 * This provides the best of all approaches while leveraging Neo4j GDS library's
 * optimized graph algorithms for better performance.
 */
@Component
@RequiredArgsConstructor
public class GDSHybridAlgorithm implements RecommendationAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(GDSHybridAlgorithm.class);

    // Algorithm weights
    private static final double COLLABORATIVE_WEIGHT = 0.40;
    private static final double CONTENT_WEIGHT = 0.30;
    private static final double PAGERANK_WEIGHT = 0.30;

    private final GDSCollaborativeFilteringAlgorithm gdsCollaborative;
    private final GDSContentBasedAlgorithm gdsContentBased;
    private final GDSPageRankAlgorithm gdsPageRank;

    @Override
    public String getAlgorithmName() {
        return "gds-hybrid";
    }

    @Override
    public List<Recommendation> generateRecommendations(RecommendationRequest request) {
        logger.info("Generating GDS hybrid recommendations for user: {}", request.getUserId());

        try {
            // Request more results from each algorithm for better blending
            int fetchLimit = request.getLimit() * 2;
            RecommendationRequest extendedRequest = RecommendationRequest.builder()
                    .userId(request.getUserId())
                    .limit(fetchLimit)
                    .genre(request.getGenre())
                    .minRating(request.getMinRating())
                    .build();

            // Get recommendations from all three GDS algorithms in parallel
            List<Recommendation> collaborativeRecs = new ArrayList<>();
            List<Recommendation> contentRecs = new ArrayList<>();
            List<Recommendation> pageRankRecs;

            // Try collaborative filtering if applicable
            if (gdsCollaborative.isApplicable(extendedRequest)) {
                try {
                    collaborativeRecs = gdsCollaborative.generateRecommendations(extendedRequest);
                    logger.info("GDS Collaborative: {} recommendations", collaborativeRecs.size());
                } catch (Exception e) {
                    logger.warn("GDS Collaborative filtering failed: {}", e.getMessage());
                }
            }

            // Try content-based filtering if applicable
            if (gdsContentBased.isApplicable(extendedRequest)) {
                try {
                    contentRecs = gdsContentBased.generateRecommendations(extendedRequest);
                    logger.info("GDS Content-based: {} recommendations", contentRecs.size());
                } catch (Exception e) {
                    logger.warn("GDS Content-based filtering failed: {}", e.getMessage());
                }
            }

            // PageRank is always applicable (handles cold start)
            try {
                pageRankRecs = gdsPageRank.generateRecommendations(extendedRequest);
                logger.info("GDS PageRank: {} recommendations", pageRankRecs.size());
            } catch (Exception e) {
                logger.error("GDS PageRank failed: {}", e.getMessage());
                pageRankRecs = new ArrayList<>();
            }

            // Combine and score recommendations
            Map<String, CombinedRecommendation> combinedMap = new HashMap<>();

            // Add collaborative recommendations
            for (Recommendation rec : collaborativeRecs) {
                combinedMap.computeIfAbsent(rec.getMovieId(), k -> new CombinedRecommendation(rec.getMovieId()))
                        .setCollaborativeScore(rec.getScore())
                        .setCollaborativeReason(rec.getReason());
            }

            // Add content-based recommendations
            for (Recommendation rec : contentRecs) {
                combinedMap.computeIfAbsent(rec.getMovieId(), k -> new CombinedRecommendation(rec.getMovieId()))
                        .setContentScore(rec.getScore())
                        .setContentReason(rec.getReason());
            }

            // Add PageRank recommendations
            for (Recommendation rec : pageRankRecs) {
                combinedMap.computeIfAbsent(rec.getMovieId(), k -> new CombinedRecommendation(rec.getMovieId()))
                        .setPageRankScore(rec.getScore())
                        .setPageRankReason(rec.getReason());
            }

            // Calculate weighted scores and create final recommendations
            List<Recommendation> finalRecommendations = combinedMap.values().stream()
                    .map(combined -> {
                        double weightedScore = calculateWeightedScore(combined);
                        String reason = buildCombinedReason(combined);

                        return Recommendation.builder()
                                .userId(request.getUserId())
                                .movieId(combined.getMovieId())
                                .score(weightedScore)
                                .algorithm(getAlgorithmName())
                                .reason(reason)
                                .build();
                    })
                    .sorted((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()))
                    .limit(request.getLimit())
                    .collect(Collectors.toList());

            logger.info("Generated {} GDS hybrid recommendations from {} unique movies",
                    finalRecommendations.size(), combinedMap.size());

            return finalRecommendations;

        } catch (Exception e) {
            logger.error("Error generating GDS hybrid recommendations for user {}: {}",
                    request.getUserId(), e.getMessage(), e);
            // Fallback to PageRank only
            logger.info("Falling back to GDS PageRank only");
            return gdsPageRank.generateRecommendations(request);
        }
    }

    @Override
    public double calculateScore(String userId, String movieId) {
        // For hybrid, combine scores from all algorithms
        try {
            double collaborativeScore = gdsCollaborative.calculateScore(userId, movieId);
            double contentScore = gdsContentBased.calculateScore(userId, movieId);
            double pageRankScore = gdsPageRank.calculateScore(userId, movieId);

            double score = 0.0;
            double totalWeight = 0.0;

            if (collaborativeScore > 0) {
                score += collaborativeScore * COLLABORATIVE_WEIGHT;
                totalWeight += COLLABORATIVE_WEIGHT;
            }

            if (contentScore > 0) {
                score += contentScore * CONTENT_WEIGHT;
                totalWeight += CONTENT_WEIGHT;
            }

            if (pageRankScore > 0) {
                score += pageRankScore * PAGERANK_WEIGHT;
                totalWeight += PAGERANK_WEIGHT;
            }

            return totalWeight > 0 ? score / totalWeight : 0.0;
        } catch (Exception e) {
            logger.error("Error calculating hybrid score: {}", e.getMessage());
            return 0.0;
        }
    }

    @Override
    public boolean isApplicable(RecommendationRequest request) {
        // Hybrid is always applicable because it includes PageRank (cold start solution)
        return true;
    }

    /**
     * Calculate weighted score from all algorithm scores
     */
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

        if (combined.getPageRankScore() > 0) {
            score += combined.getPageRankScore() * PAGERANK_WEIGHT;
            totalWeight += PAGERANK_WEIGHT;
        }

        return totalWeight > 0 ? score / totalWeight : 0.0;
    }

    /**
     * Build a combined reason explaining why this movie was recommended
     */
    private String buildCombinedReason(CombinedRecommendation combined) {
        List<String> reasons = new ArrayList<>();

        if (combined.getCollaborativeScore() > 0) {
            reasons.add(String.format("Similar users (score: %.2f)", combined.getCollaborativeScore()));
        }

        if (combined.getContentScore() > 0) {
            reasons.add(String.format("Similar content (score: %.2f)", combined.getContentScore()));
        }

        if (combined.getPageRankScore() > 0) {
            reasons.add(String.format("Popular choice (score: %.2f)", combined.getPageRankScore()));
        }

        return "GDS Hybrid: " + String.join(" + ", reasons);
    }

    /**
     * Helper class to combine recommendations from different algorithms
     */
    private static class CombinedRecommendation {
        private final String movieId;
        private double collaborativeScore = 0.0;
        private double contentScore = 0.0;
        private double pageRankScore = 0.0;
        private String collaborativeReason;
        private String contentReason;
        private String pageRankReason;

        public CombinedRecommendation(String movieId) {
            this.movieId = movieId;
        }

        public String getMovieId() {
            return movieId;
        }

        public double getCollaborativeScore() {
            return collaborativeScore;
        }

        public CombinedRecommendation setCollaborativeScore(double collaborativeScore) {
            this.collaborativeScore = collaborativeScore;
            return this;
        }

        public double getContentScore() {
            return contentScore;
        }

        public CombinedRecommendation setContentScore(double contentScore) {
            this.contentScore = contentScore;
            return this;
        }

        public double getPageRankScore() {
            return pageRankScore;
        }

        public CombinedRecommendation setPageRankScore(double pageRankScore) {
            this.pageRankScore = pageRankScore;
            return this;
        }

        public CombinedRecommendation setCollaborativeReason(String reason) {
            this.collaborativeReason = reason;
            return this;
        }

        public CombinedRecommendation setContentReason(String reason) {
            this.contentReason = reason;
            return this;
        }

        public CombinedRecommendation setPageRankReason(String reason) {
            this.pageRankReason = reason;
            return this;
        }
    }
}
