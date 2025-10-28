package com.neo4flix.movieservice.algorithm;

import com.neo4flix.movieservice.dto.MovieRecommendationDTO;
import com.neo4flix.movieservice.dto.RecommendationRequest;
import com.neo4flix.movieservice.dto.UserSimilarity;
import com.neo4flix.movieservice.model.Recommendation;
import com.neo4flix.movieservice.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Collaborative Filtering Algorithm Implementation
 * Uses user-based collaborative filtering to recommend movies
 * Can optionally filter by specific genres
 * En gros : "les utilisateurs qui ont aimé les mêmes films que toi aiment aussi celui-là".
 */
@Component
@RequiredArgsConstructor
public class CollaborativeFilteringAlgorithm implements RecommendationAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(CollaborativeFilteringAlgorithm.class);

    private final RecommendationRepository recommendationRepository;

    @Override
    public String getAlgorithmName() {
        return "collaborative";
    }

    @Override
    public List<Recommendation> generateRecommendations(RecommendationRequest request) {
        logger.info("Generating collaborative filtering recommendations for user: {}", request.getUserId());

        boolean hasGenreFilter = request.getGenre() != null && !request.getGenre().isEmpty();
        if (hasGenreFilter) {
            logger.info("Applying genre filter: {}", String.join(", ", request.getGenre()));
        }

        try {
            // Parameters for collaborative filtering
            int minCommonRatings = 3;
            double minSimilarity = 0.3;
            double minRating = request.getMinRating() != null ? request.getMinRating() : 3.5;

            // Get collaborative recommendations from Neo4j
            List<MovieRecommendationDTO> rawRecommendations;

            if (hasGenreFilter) {
                rawRecommendations = recommendationRepository.getCollaborativeRecommendationsWithGenreFilter(
                        request.getUserId(),
                        minCommonRatings,
                        minSimilarity,
                        minRating,
                        request.getGenre(),
                        request.getLimit()
                );
            } else {
                rawRecommendations = recommendationRepository.getCollaborativeRecommendations(
                        request.getUserId(),
                        minCommonRatings,
                        minSimilarity,
                        minRating,
                        request.getLimit()
                );
            }

            List<Recommendation> recommendations = new ArrayList<>();

            for (MovieRecommendationDTO row : rawRecommendations) {
                // Create recommendation with explanation
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

            logger.info("Generated {} collaborative filtering recommendations", recommendations.size());
            return recommendations;

        } catch (Exception e) {
            logger.error("Error generating collaborative filtering recommendations for user {}: {}",
                    request.getUserId(), e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Build reason message for collaborative filtering recommendations
     */
    private String buildReasonMessage(MovieRecommendationDTO row, boolean hasGenreFilter, List<String> genres) {
        StringBuilder reason = new StringBuilder();
        reason.append(String.format("Users with similar taste rated this %.1f/5", row.getMovieRating()));

        if (hasGenreFilter && genres != null && !genres.isEmpty()) {
            reason.append(" - ");
            reason.append(formatGenreList(genres));
            reason.append(" genre");
            if (genres.size() > 1) {
                reason.append("s");
            }
        }

        return reason.toString();
    }


    /**
     * Format genre list for display in reason messages
     */
    private String formatGenreList(List<String> genres) {
        if (genres == null || genres.isEmpty()) {
            return "";
        } else if (genres.size() == 1) {
            return genres.get(0);
        } else if (genres.size() == 2) {
            return genres.get(0) + " and " + genres.get(1);
        } else {
            // Join all but the last with commas, and add "and" before the last
            return String.join(", ", genres.subList(0, genres.size() - 1))
                    + " and " + genres.get(genres.size() - 1);
        }
    }

    @Override
    public double calculateScore(String userId, String movieId) {
        try {
            // Get similar users
            List<UserSimilarity> similarUsers = recommendationRepository.findSimilarUsers(
                    userId, 3, 0.3, 50
            );

            if (similarUsers.isEmpty()) {
                return 0.0;
            }

            // Calculate weighted average rating from similar users
            double totalWeight = 0.0;
            double weightedSum = 0.0;

            for (UserSimilarity similarity : similarUsers) {
                // This would require additional query to get user's rating for this movie
                // For simplicity, using the similarity as weight
                double weight = similarity.getSimilarity();
                totalWeight += weight;
                weightedSum += weight * 4.0; // Assume average rating of 4.0
            }

            return totalWeight > 0 ? weightedSum / totalWeight : 0.0;

        } catch (Exception e) {
            logger.error("Error calculating collaborative score for user {} and movie {}: {}",
                    userId, movieId, e.getMessage());
            return 0.0;
        }
    }

    /**
     * Calculate Pearson correlation coefficient between two users
     */
    public double calculateUserSimilarity(Map<String, Double> user1Ratings, Map<String, Double> user2Ratings) {
        // Find common movies
        List<String> commonMovies = user1Ratings.keySet().stream()
                .filter(user2Ratings::containsKey)
                .collect(Collectors.toList());

        if (commonMovies.size() < 2) {
            return 0.0; // Need at least 2 common ratings
        }

        // Extract ratings for common movies
        double[] ratings1 = commonMovies.stream()
                .mapToDouble(user1Ratings::get)
                .toArray();

        double[] ratings2 = commonMovies.stream()
                .mapToDouble(user2Ratings::get)
                .toArray();

        // Calculate Pearson correlation
        PearsonsCorrelation correlation = new PearsonsCorrelation();
        return correlation.correlation(ratings1, ratings2);
    }

    /**
     * Calculate cosine similarity between two users
     */
    public double calculateCosineSimilarity(Map<String, Double> user1Ratings, Map<String, Double> user2Ratings) {
        // Find common movies
        List<String> commonMovies = user1Ratings.keySet().stream()
                .filter(user2Ratings::containsKey)
                .collect(Collectors.toList());

        if (commonMovies.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (String movieId : commonMovies) {
            double rating1 = user1Ratings.get(movieId);
            double rating2 = user2Ratings.get(movieId);

            dotProduct += rating1 * rating2;
            norm1 += rating1 * rating1;
            norm2 += rating2 * rating2;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Normalize score to 0-1 range
     */
    private double normalizeScore(Double rawScore) {
        if (rawScore == null) {
            return 0.0;
        }
        // Assuming raw score is typically between 0-5, normalize to 0-1
        return Math.min(1.0, Math.max(0.0, rawScore / 5.0));
    }

    @Override
    public boolean isApplicable(RecommendationRequest request) {
        // Collaborative filtering requires the user to have some rating history
        // This should be checked by the service layer
        return "collaborative".equals(request.getAlgorithm()) || "hybrid".equals(request.getAlgorithm());
    }
}