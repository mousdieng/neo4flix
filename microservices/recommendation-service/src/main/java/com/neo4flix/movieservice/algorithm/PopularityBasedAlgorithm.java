package com.neo4flix.movieservice.algorithm;

import com.neo4flix.movieservice.dto.MovieRecommendationDTO;
import com.neo4flix.movieservice.dto.RecommendationRequest;
import com.neo4flix.movieservice.model.Recommendation;
import com.neo4flix.movieservice.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Popularity-Based Algorithm Implementation
 * Recommends movies based on overall popularity and ratings
 * Can optionally filter by genres
 */
@Component
@RequiredArgsConstructor
public class PopularityBasedAlgorithm implements RecommendationAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(PopularityBasedAlgorithm.class);

    private final RecommendationRepository recommendationRepository;
    private final Neo4jClient neo4jClient;

    @Override
    public String getAlgorithmName() {
        return "popular";
    }

    @Override
    public List<Recommendation> generateRecommendations(RecommendationRequest request) {
        logger.info("Generating popularity-based recommendations for user: {}", request.getUserId());

        try {

            boolean hasGenreFilter = request.getGenre() != null && !request.getGenre().isEmpty();
            List<MovieRecommendationDTO> rawRecommendations;

            if (hasGenreFilter) {
                logger.info("Applying genre filter: {}", String.join(", ", request.getGenre()));
                rawRecommendations = recommendationRepository.getPopularRecommendationsByGenre(
                        request.getUserId(),
                        request.getSafeMinRating(),
                        request.getSafeMinAverageMovieRating(),
                        request.getGenre(),
                        request.getLimit()
                );
            } else {
                logger.info("No genre filter applied - showing all popular movies");
                rawRecommendations = recommendationRepository.getPopularRecommendations(
                        request.getUserId(),
                        request.getSafeMinRating(),
                        request.getSafeMinAverageMovieRating(),
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

            logger.info("Generated {} popularity-based recommendations", recommendations.size());
            return recommendations;

        } catch (Exception e) {
            logger.error("Error generating popularity-based recommendations for user {}: {}",
                    request.getUserId(), e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Build a descriptive reason message for the recommendation
     */
    private String buildReasonMessage(MovieRecommendationDTO row, boolean hasGenreFilter, List<String> genres) {
        StringBuilder reason = new StringBuilder();
        reason.append(String.format("Popular movie with %.1f/5 rating from many users", row.getMovieRating()));

        if (hasGenreFilter && genres != null && !genres.isEmpty()) {
            reason.append(" in ");
            if (genres.size() == 1) {
                reason.append(genres.get(0));
            } else if (genres.size() == 2) {
                reason.append(genres.get(0)).append(" and ").append(genres.get(1));
            } else {
                reason.append(String.join(", ", genres.subList(0, genres.size() - 1)))
                        .append(" and ").append(genres.get(genres.size() - 1));
            }
            reason.append(" genre");
            if (genres.size() > 1) {
                reason.append("s");
            }
        }

        return reason.toString();
    }


    @Override
    public double calculateScore(String userId, String movieId) {
        try {
            // For popularity-based, score is based on the movie's overall popularity
            // This would require querying the movie's rating and count
            return 0.5; // Placeholder

        } catch (Exception e) {
            logger.error("Error calculating popularity score for user {} and movie {}: {}",
                    userId, movieId, e.getMessage());
            return 0.0;
        }
    }

    /**
     * Calculate popularity score using a combination of rating and rating count
     * Uses a formula similar to IMDb's weighted rating
     */
    private double calculatePopularityScore(double averageRating, int ratingCount, double minRating, int minRatingCount) {
        // IMDb-style weighted rating formula
        // WR = (v / (v + m)) * R + (m / (v + m)) * C
        // Where:
        // WR = Weighted Rating
        // v = number of votes for the movie
        // m = minimum votes required to be listed (e.g., 25)
        // R = average rating of the movie
        // C = mean vote across the whole report

        double meanRating = 3.5; // Average rating across all movies
        double weightedRating = ((double) ratingCount / (ratingCount + minRatingCount)) * averageRating +
                ((double) minRatingCount / (ratingCount + minRatingCount)) * meanRating;

        // Add logarithmic boost for very popular movies
        double popularityBoost = Math.log(ratingCount + 1) / Math.log(1000); // Normalize to ~1.0 for 1000 ratings

        return weightedRating + popularityBoost;
    }

    /**
     * Normalize popularity score to 0-1 range
     */
    private double normalizePopularityScore(Double rawScore) {
        if (rawScore == null) {
            return 0.0;
        }
        // Popularity scores can vary widely, normalize using a reasonable upper bound
        double maxExpectedScore = 8.0; // Movies rarely exceed this popularity score
        return Math.min(1.0, Math.max(0.0, rawScore / maxExpectedScore));
    }

    @Override
    public boolean isApplicable(RecommendationRequest request) {
        return "popular".equals(request.getAlgorithm()) || "hybrid".equals(request.getAlgorithm());
    }
}