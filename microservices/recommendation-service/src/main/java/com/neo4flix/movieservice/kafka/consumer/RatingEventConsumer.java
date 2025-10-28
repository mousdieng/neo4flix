package com.neo4flix.movieservice.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neo4flix.movieservice.kafka.KafkaTopics;
import com.neo4flix.movieservice.kafka.event.RatingCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RatingEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RatingEventConsumer.class);
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    public RatingEventConsumer(ObjectMapper objectMapper, CacheManager cacheManager) {
        this.objectMapper = objectMapper;
        this.cacheManager = cacheManager;
    }

    @KafkaListener(topics = KafkaTopics.RATING_CREATED, groupId = "recommendation-service-group")
    public void consumeRatingCreatedEvent(Map<String, Object> payload) {
        try {
            RatingCreatedEvent event = objectMapper.convertValue(payload, RatingCreatedEvent.class);
            logger.info("Received rating created event for movieId: {} by userId: {}",
                event.getMovieId(), event.getUserId());

            // Invalidate recommendation cache for this user
            invalidateUserRecommendationCache(event.getUserId());

            // Trigger recommendation recalculation in the background
            logger.info("Invalidated recommendation cache for userId: {}", event.getUserId());

        } catch (Exception e) {
            logger.error("Error processing rating created event", e);
        }
    }

    @KafkaListener(topics = KafkaTopics.RATING_UPDATED, groupId = "recommendation-service-group")
    public void consumeRatingUpdatedEvent(Map<String, Object> payload) {
        try {
            RatingCreatedEvent event = objectMapper.convertValue(payload, RatingCreatedEvent.class);
            logger.info("Received rating updated event for movieId: {} by userId: {}",
                event.getMovieId(), event.getUserId());

            // Invalidate recommendation cache for this user
            invalidateUserRecommendationCache(event.getUserId());

            logger.info("Invalidated recommendation cache for userId: {}", event.getUserId());

        } catch (Exception e) {
            logger.error("Error processing rating updated event", e);
        }
    }

    @KafkaListener(topics = KafkaTopics.RATING_DELETED, groupId = "recommendation-service-group")
    public void consumeRatingDeletedEvent(String ratingId) {
        try {
            logger.info("Received rating deleted event for ratingId: {}", ratingId);

            // Clear all recommendation caches as we don't know which user this affects
            clearAllRecommendationCaches();

            logger.info("Cleared all recommendation caches due to rating deletion");

        } catch (Exception e) {
            logger.error("Error processing rating deleted event", e);
        }
    }

    private void invalidateUserRecommendationCache(String userId) {
        if (cacheManager != null) {
            var cache = cacheManager.getCache("recommendations");
            if (cache != null) {
                cache.evict(userId);
                logger.debug("Evicted cache for userId: {}", userId);
            }
        }
    }

    private void clearAllRecommendationCaches() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    logger.debug("Cleared cache: {}", cacheName);
                }
            });
        }
    }
}
