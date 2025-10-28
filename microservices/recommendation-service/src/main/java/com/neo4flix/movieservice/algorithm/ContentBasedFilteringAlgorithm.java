package com.neo4flix.movieservice.algorithm;

import com.neo4flix.movieservice.dto.MovieRecommendationDTO;
import com.neo4flix.movieservice.dto.RecommendationRequest;
import com.neo4flix.movieservice.model.Recommendation;
import com.neo4flix.movieservice.repository.RecommendationRepository;
import com.neo4flix.movieservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Content-Based Filtering Algorithm Implementation
 * Uses movie features (genres, directors, actors) to recommend similar movies
 * Can optionally filter by specific genres

 * Cette classe recommande des films similaires à ceux qu'un utilisateur a déjà aimés,
 * en se basant sur les caractéristiques des films eux-mêmes :
 * genres
 * réalisateurs
 * acteurs
 * (et éventuellement) résumé, année, durée, etc.
 * Exemple : si tu as aimé Black Panther, on va te recommander Captain America ou Avengers
 * parce qu'ils partagent le genre "Action/Super-Héros" et le même réalisateur ou acteur.
 */
@Component
@RequiredArgsConstructor
public class ContentBasedFilteringAlgorithm implements RecommendationAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(ContentBasedFilteringAlgorithm.class);

    private final RecommendationRepository recommendationRepository;
    private final UserRepository userRepository;

    @Override
    public String getAlgorithmName() {
        return "content";
    }

    @Override
    public List<Recommendation> generateRecommendations(RecommendationRequest request) {
        logger.info("Generating content-based recommendations for user: {}", request.getUserId());

        boolean hasGenreFilter = request.getGenre() != null && !request.getGenre().isEmpty();
        if (hasGenreFilter) {
            logger.info("Applying genre filter: {}", String.join(", ", request.getGenre()));
        }

        List<Recommendation> allRecommendations = new ArrayList<>();

        try {
            // Get genre-based recommendations
            List<Recommendation> genreRecommendations = generateGenreBasedRecommendations(request, hasGenreFilter);
            allRecommendations.addAll(genreRecommendations);

            // Get director-based recommendations
            List<Recommendation> directorRecommendations = generateDirectorBasedRecommendations(request, hasGenreFilter);
            allRecommendations.addAll(directorRecommendations);

            // Remove duplicates and sort by score
            List<Recommendation> uniqueRecommendations = removeDuplicatesAndSort(allRecommendations);

            // Limit results
            int limit = Math.min(request.getLimit(), uniqueRecommendations.size());
            List<Recommendation> finalRecommendations = uniqueRecommendations.subList(0, limit);

            logger.info("==============================================================================");
            logger.info("Generated {} content-based recommendations", finalRecommendations.size());
            logger.info("==============================================================================");

            return finalRecommendations;

        } catch (Exception e) {
            logger.error("Error generating content-based recommendations for user {}: {}",
                    request.getUserId(), e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private List<Recommendation> generateGenreBasedRecommendations(RecommendationRequest request, boolean hasGenreFilter) {
        List<MovieRecommendationDTO> rawRecommendations;

        if (hasGenreFilter) {
            rawRecommendations = recommendationRepository.getContentBasedRecommendationsWithGenreFilter(
                    request.getUserId(),
                    request.getSafeMinRating(),
                    request.getSafeMinAverageMovieRating(),
                    request.getGenre(),
                    request.getLimit()
            );
        } else {
            rawRecommendations = recommendationRepository.getContentBasedRecommendations(
                    request.getUserId(),
                    request.getSafeMinRating(),
                    request.getSafeMinAverageMovieRating(),
                    request.getLimit()
            );
        }

        List<Recommendation> recommendations = new ArrayList<>();
        for (MovieRecommendationDTO row : rawRecommendations) {
            String reason = buildGenreReasonMessage(row, hasGenreFilter, request.getGenre());

            Recommendation recommendation = Recommendation.builder()
                    .userId(request.getUserId())
                    .score(row.getScore())
                    .movieId(row.getMovie().getId())
                    .algorithm(getAlgorithmName())
                    .reason(reason)
                    .build();
            recommendations.add(recommendation);
        }

        return recommendations;
    }

    private List<Recommendation> generateDirectorBasedRecommendations(RecommendationRequest request, boolean hasGenreFilter) {
        List<MovieRecommendationDTO> rawRecommendations;

        if (hasGenreFilter) {
            rawRecommendations = recommendationRepository.getDirectorBasedRecommendationsWithGenreFilter(
                    request.getUserId(),
                    request.getSafeMinRating(),
                    request.getSafeMinAverageMovieRating(),
                    request.getGenre(),
                    request.getLimit()
            );
        } else {
            rawRecommendations = recommendationRepository.getDirectorBasedRecommendations(
                    request.getUserId(),
                    request.getSafeMinRating(),
                    request.getSafeMinAverageMovieRating(),
                    request.getLimit()
            );
        }

        List<Recommendation> recommendations = new ArrayList<>();

        for (MovieRecommendationDTO row : rawRecommendations) {
            String reason = buildDirectorReasonMessage(row, hasGenreFilter, request.getGenre());

            Recommendation recommendation = Recommendation.builder()
                    .userId(request.getUserId())
                    .score(row.getScore())
                    .movieId(row.getMovie().getId())
                    .algorithm(getAlgorithmName())
                    .reason(reason)
                    .build();

            recommendations.add(recommendation);
        }

        return recommendations;
    }

    /**
     * Build reason message for genre-based recommendations
     */
    private String buildGenreReasonMessage(MovieRecommendationDTO row, boolean hasGenreFilter, List<String> genres) {
        StringBuilder reason = new StringBuilder();
        reason.append(String.format("Based on your genre preferences (%.1f/5 rating)", row.getMovieRating()));

        if (hasGenreFilter && genres != null && !genres.isEmpty()) {
            reason.append(" - filtered for ");
            reason.append(formatGenreList(genres));
        }

        return reason.toString();
    }

    /**
     * Build reason message for director-based recommendations
     */
    private String buildDirectorReasonMessage(MovieRecommendationDTO row, boolean hasGenreFilter, List<String> genres) {
        StringBuilder reason = new StringBuilder();
        reason.append(String.format("From directors you rated highly (%.1f/5 rating)", row.getMovieRating()));

        if (hasGenreFilter && genres != null && !genres.isEmpty()) {
            reason.append(" - filtered for ");
            reason.append(formatGenreList(genres));
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
            // This would require a more complex implementation to calculate
            // content similarity between user preferences and movie features
            // For now, return a basic score
            return 0.5;

        } catch (Exception e) {
            logger.error("Error calculating content-based score for user {} and movie {}: {}",
                    userId, movieId, e.getMessage());
            return 0.0;
        }
    }

    /**
     * Calculate content similarity between two movies based on shared features
     */
    public double calculateMovieSimilarity(String movieId1, String movieId2) {
        // This would involve comparing:
        // - Shared genres (weighted)
        // - Same directors (highly weighted)
        // - Common actors (moderately weighted)
        // - Similar release years
        // - Similar runtimes

        // For now, return a placeholder value
        return 0.0;
    }

    /**
     * Calculate genre preference score for a user
     */
    public double calculateGenrePreference(String userId, String genre) {
        // This would analyze user's rating history for movies in this genre
        // and return an average preference score
        return 0.0;
    }

    /**
     * Calculate TF-IDF similarity for movie plots/descriptions
     */
    public double calculateTextSimilarity(String text1, String text2) {
        // This would implement TF-IDF or other text similarity algorithms
        // to compare movie plots, descriptions, etc.
        return 0.0;
    }

    /**
     * Remove duplicates and sort recommendations by score
     */
    private List<Recommendation> removeDuplicatesAndSort(List<Recommendation> recommendations) {
        return recommendations.stream()
                .collect(java.util.stream.Collectors.toMap(
                        Recommendation::getMovieId,
                        rec -> rec,
                        (existing, replacement) -> existing.getScore() > replacement.getScore() ? existing : replacement
                ))
                .values()
                .stream()
                .sorted((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()))
                .collect(java.util.stream.Collectors.toList());
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
        return "content".equals(request.getAlgorithm()) || "hybrid".equals(request.getAlgorithm());
    }
}