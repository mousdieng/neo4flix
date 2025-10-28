package com.neo4flix.ratingservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Client for calling the Recommendation Service
 */
@Service
public class RecommendationClient {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationClient.class);

    private final WebClient webClient;

    @Value("${recommendation.service.url:http://localhost:9083}")
    private String recommendationServiceUrl;

    public RecommendationClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Trigger recommendation refresh for a user asynchronously
     * This is called after a user rates a movie to update their recommendations
     */
    @Async
    public void triggerRecommendationRefresh(String userId, String jwtToken) {
        logger.info("Triggering recommendation refresh for user: {}", userId);

        try {
            String url = recommendationServiceUrl + "/api/v1/recommendations/refresh?algorithm=hybrid&limit=20";

            webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe(
                            response -> logger.info("Recommendations refreshed successfully for user: {}", userId),
                            error -> logger.warn("Failed to refresh recommendations for user {}: {}", userId, error.getMessage())
                    );

        } catch (Exception e) {
            // Don't fail the rating operation if recommendation refresh fails
            logger.error("Error triggering recommendation refresh for user {}: {}", userId, e.getMessage());
        }
    }
}
