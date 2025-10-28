package com.neo4flix.movieservice.algorithm;

import com.neo4flix.movieservice.dto.MovieRecommendationDTO;
import com.neo4flix.movieservice.dto.RecommendationRequest;
import com.neo4flix.movieservice.model.Recommendation;
import com.neo4flix.movieservice.repository.GDSRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Neo4j GDS-based Collaborative Filtering Algorithm Implementation
 * Uses Neo4j Graph Data Science library's Node Similarity algorithm
 * for user-based collaborative filtering recommendations.
 *
 * This algorithm:
 * 1. Creates a graph projection of User-Rating-Movie relationships
 * 2. Uses GDS Node Similarity to find similar users based on rating patterns
 * 3. Recommends movies that similar users have rated highly
 */
@Component
@RequiredArgsConstructor
public class GDSCollaborativeFilteringAlgorithm implements RecommendationAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(GDSCollaborativeFilteringAlgorithm.class);

    private final GDSRecommendationRepository gdsRepository;

    @Override
    public String getAlgorithmName() {
        return "gds-collaborative";
    }

    @Override
    public List<Recommendation> generateRecommendations(RecommendationRequest request) {
        logger.info("Generating GDS collaborative filtering recommendations for user: {}", request.getUserId());

        boolean hasGenreFilter = request.getGenre() != null && !request.getGenre().isEmpty();
        if (hasGenreFilter) {
            logger.info("Applying genre filter: {}", String.join(", ", request.getGenre()));
        }

        try {
            // Step 1: Create or use existing graph projection
            String graphName = "user-movie-ratings";
            gdsRepository.createUserMovieGraphProjection(graphName);

            // Step 2: Run Node Similarity algorithm to find similar users
            double minSimilarity = request.getMinRating() != null && request.getMinRating() > 0
                ? request.getMinRating() / 5.0  // Normalize to 0-1 range
                : 0.3;

            List<MovieRecommendationDTO> rawRecommendations;

            if (hasGenreFilter) {
                rawRecommendations = gdsRepository.getGDSCollaborativeRecommendationsWithGenre(
                    graphName,
                    request.getUserId(),
                    minSimilarity,
                    request.getGenre(),
                    request.getLimit()
                );
            } else {
                rawRecommendations = gdsRepository.getGDSCollaborativeRecommendations(
                    graphName,
                    request.getUserId(),
                    minSimilarity,
                    request.getLimit()
                );
            }

            // Step 3: Convert to Recommendation objects
            List<Recommendation> recommendations = new ArrayList<>();

            for (MovieRecommendationDTO row : rawRecommendations) {
                String reason = buildReasonMessage(row, hasGenreFilter, request.getGenre());

                Recommendation recommendation = Recommendation.builder()
                        .userId(request.getUserId())
                        .score(row.getScore())
                        .movieId(row.getMovie().getId())
                        .algorithm(getAlgorithmName())
                        .reason(reason)
                        .build();

                recommendations.add(recommendation);
            }

            logger.info("Generated {} GDS collaborative filtering recommendations", recommendations.size());
            return recommendations;

        } catch (Exception e) {
            logger.error("Error generating GDS collaborative filtering recommendations for user {}: {}",
                    request.getUserId(), e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public double calculateScore(String userId, String movieId) {
        // For collaborative filtering, score is based on similar users' ratings
        try {
            // This is a simplified implementation - in production you'd calculate actual similarity
            // For now, return a default value indicating this method should use generateRecommendations instead
            logger.warn("calculateScore not fully implemented for GDS collaborative - use generateRecommendations instead");
            return 0.0;
        } catch (Exception e) {
            logger.error("Error calculating collaborative score: {}", e.getMessage());
            return 0.0;
        }
    }

    @Override
    public boolean isApplicable(RecommendationRequest request) {
        try {
            // Check if user has rated enough movies for collaborative filtering
            int userRatingsCount = gdsRepository.getUserRatingsCount(request.getUserId());
            boolean applicable = userRatingsCount >= 3;

            if (!applicable) {
                logger.info("GDS collaborative filtering not applicable for user {} (only {} ratings)",
                        request.getUserId(), userRatingsCount);
            }

            return applicable;
        } catch (Exception e) {
            logger.error("Error checking if GDS collaborative filtering is applicable: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Build a human-readable reason for the recommendation
     */
    private String buildReasonMessage(MovieRecommendationDTO dto, boolean hasGenreFilter, List<String> genres) {
        StringBuilder reason = new StringBuilder();
        reason.append("Users with similar taste to yours rated this movie ");
        reason.append(String.format("%.1f", dto.getMovieRating()));
        reason.append("/5.0");

        if (hasGenreFilter && genres != null && !genres.isEmpty()) {
            reason.append(" in your preferred genre");
            if (genres.size() > 1) {
                reason.append("s");
            }
            reason.append(": ").append(String.join(", ", genres));
        }

        reason.append(". (GDS Node Similarity Score: ");
        reason.append(String.format("%.2f", dto.getScore()));
        reason.append(")");

        return reason.toString();
    }
}
