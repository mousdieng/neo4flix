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
 * Neo4j GDS PageRank-based Popularity Algorithm.
 * Uses PageRank to identify the most "central" movies in the rating network.
 *
 * PageRank considers both:
 * - The number of ratings a movie has
 * - The "importance" of users who rated it (users who rate many movies have higher influence)
 *
 * This provides a more sophisticated popularity metric than simple rating counts.
 */
@Component
@RequiredArgsConstructor
public class GDSPageRankAlgorithm implements RecommendationAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(GDSPageRankAlgorithm.class);

    private final GDSRecommendationRepository gdsRepository;

    @Override
    public String getAlgorithmName() {
        return "gds-pagerank";
    }

    @Override
    public List<Recommendation> generateRecommendations(RecommendationRequest request) {
        logger.info("Generating GDS PageRank recommendations for user: {}", request.getUserId());

        boolean hasGenreFilter = request.getGenre() != null && !request.getGenre().isEmpty();
        if (hasGenreFilter) {
            logger.info("Applying genre filter: {}", String.join(", ", request.getGenre()));
        }

        try {
            // Create or use existing graph projection
            String graphName = "user-movie-ratings";
            gdsRepository.createUserMovieGraphProjection(graphName);

            double minRating = request.getMinRating() != null ? request.getMinRating() : 3.5;

            List<MovieRecommendationDTO> rawRecommendations;

            if (hasGenreFilter) {
                rawRecommendations = gdsRepository.getGDSPageRankRecommendationsWithGenre(
                    graphName,
                    request.getUserId(),
                    minRating,
                    request.getGenre(),
                    request.getLimit()
                );
            } else {
                rawRecommendations = gdsRepository.getGDSPageRankRecommendations(
                    graphName,
                    request.getUserId(),
                    minRating,
                    request.getLimit()
                );
            }

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

            logger.info("Generated {} GDS PageRank recommendations", recommendations.size());
            return recommendations;

        } catch (Exception e) {
            logger.error("Error generating GDS PageRank recommendations for user {}: {}",
                    request.getUserId(), e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public double calculateScore(String userId, String movieId) {
        // For PageRank, score is based on movie's network influence
        // Not personalized per user, so userId is ignored
        try {
            String graphName = "user-movie-ratings";
            gdsRepository.createUserMovieGraphProjection(graphName);

            // This is a simplified implementation - in production you'd want to cache PageRank scores
            // For now, return a default value indicating this method should use generateRecommendations instead
            logger.warn("calculateScore not fully implemented for PageRank - use generateRecommendations instead");
            return 0.0;
        } catch (Exception e) {
            logger.error("Error calculating PageRank score: {}", e.getMessage());
            return 0.0;
        }
    }

    @Override
    public boolean isApplicable(RecommendationRequest request) {
        // PageRank-based recommendations are always applicable
        // They work even for new users (cold start problem)
        return true;
    }

    /**
     * Build a human-readable reason for the recommendation
     */
    private String buildReasonMessage(MovieRecommendationDTO dto, boolean hasGenreFilter, List<String> genres) {
        StringBuilder reason = new StringBuilder();
        reason.append("Highly influential movie in the network");

        if (hasGenreFilter && genres != null && !genres.isEmpty()) {
            reason.append(" for ");
            if (genres.size() > 1) {
                reason.append("genres: ");
            } else {
                reason.append("genre: ");
            }
            reason.append(String.join(", ", genres));
        }

        reason.append(". Average rating: ");
        reason.append(String.format("%.1f", dto.getMovieRating()));
        reason.append("/5.0");

        reason.append(". (GDS PageRank Score: ");
        reason.append(String.format("%.2f", dto.getScore()));
        reason.append(")");

        return reason.toString();
    }
}
