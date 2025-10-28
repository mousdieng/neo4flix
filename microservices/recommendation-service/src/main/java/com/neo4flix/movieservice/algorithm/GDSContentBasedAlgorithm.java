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
 * Neo4j GDS-based Content-Based Filtering Algorithm.
 * Uses GDS Cosine Similarity to find movies similar to those the user has liked.
 *
 * Similarity is based on:
 * - Shared genres
 * - Shared directors
 * - Shared actors
 *
 * The GDS algorithm provides more efficient similarity computation compared to
 * custom Cypher queries, especially for large datasets.
 */
@Component
@RequiredArgsConstructor
public class GDSContentBasedAlgorithm implements RecommendationAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(GDSContentBasedAlgorithm.class);

    private final GDSRecommendationRepository gdsRepository;

    @Override
    public String getAlgorithmName() {
        return "gds-content-based";
    }

    @Override
    public List<Recommendation> generateRecommendations(RecommendationRequest request) {
        logger.info("Generating GDS content-based recommendations for user: {}", request.getUserId());

        try {
            double minUserRating = request.getMinRating() != null ? request.getMinRating() : 4.0;
            double minMovieRating = 3.5;

            List<MovieRecommendationDTO> rawRecommendations = gdsRepository.getGDSContentBasedRecommendations(
                request.getUserId(),
                minUserRating,
                minMovieRating,
                request.getLimit()
            );

            List<Recommendation> recommendations = new ArrayList<>();

            for (MovieRecommendationDTO row : rawRecommendations) {
                String reason = buildReasonMessage(row);

                Recommendation recommendation = Recommendation.builder()
                        .userId(request.getUserId())
                        .score(row.getScore())
                        .movieId(row.getMovie().getId())
                        .algorithm(getAlgorithmName())
                        .reason(reason)
                        .build();

                recommendations.add(recommendation);
            }

            logger.info("Generated {} GDS content-based recommendations", recommendations.size());
            return recommendations;

        } catch (Exception e) {
            logger.error("Error generating GDS content-based recommendations for user {}: {}",
                    request.getUserId(), e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public double calculateScore(String userId, String movieId) {
        // For content-based, score is based on similarity to user's liked movies
        try {
            // This is a simplified implementation - in production you'd calculate actual similarity
            // For now, return a default value indicating this method should use generateRecommendations instead
            logger.warn("calculateScore not fully implemented for GDS content-based - use generateRecommendations instead");
            return 0.0;
        } catch (Exception e) {
            logger.error("Error calculating content-based score: {}", e.getMessage());
            return 0.0;
        }
    }

    @Override
    public boolean isApplicable(RecommendationRequest request) {
        try {
            // Check if user has rated enough movies for content-based filtering
            int userRatingsCount = gdsRepository.getUserRatingsCount(request.getUserId());
            boolean applicable = userRatingsCount >= 2;

            if (!applicable) {
                logger.info("GDS content-based filtering not applicable for user {} (only {} ratings)",
                        request.getUserId(), userRatingsCount);
            }

            return applicable;
        } catch (Exception e) {
            logger.error("Error checking if GDS content-based filtering is applicable: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Build a human-readable reason for the recommendation
     */
    private String buildReasonMessage(MovieRecommendationDTO dto) {
        StringBuilder reason = new StringBuilder();
        reason.append("Similar to movies you've rated highly. ");
        reason.append("Average rating: ");
        reason.append(String.format("%.1f", dto.getMovieRating()));
        reason.append("/5.0");

        reason.append(". (GDS Cosine Similarity Score: ");
        reason.append(String.format("%.2f", dto.getScore()));
        reason.append(")");

        return reason.toString();
    }
}
