package com.neo4flix.ratingservice.kafka.producer;

import com.neo4flix.ratingservice.kafka.KafkaTopics;
import com.neo4flix.ratingservice.kafka.event.RatingCreatedEvent;
import com.neo4flix.ratingservice.kafka.event.RatingUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class RatingEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(RatingEventProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RatingEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendRatingCreatedEvent(RatingCreatedEvent event) {
        logger.info("Sending rating created event for ratingId: {} on movieId: {}",
            event.getRatingId(), event.getMovieId());
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(KafkaTopics.RATING_CREATED, event.getRatingId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Rating created event sent successfully for ratingId: {} to partition: {}",
                    event.getRatingId(), result.getRecordMetadata().partition());
            } else {
                logger.error("Failed to send rating created event for ratingId: {}",
                    event.getRatingId(), ex);
            }
        });
    }

    public void sendRatingUpdatedEvent(RatingUpdatedEvent event) {
        logger.info("Sending rating updated event for ratingId: {}", event.getRatingId());
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(KafkaTopics.RATING_UPDATED, event.getRatingId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Rating updated event sent successfully for ratingId: {} to partition: {}",
                    event.getRatingId(), result.getRecordMetadata().partition());
            } else {
                logger.error("Failed to send rating updated event for ratingId: {}",
                    event.getRatingId(), ex);
            }
        });
    }

    public void sendRatingDeletedEvent(String ratingId) {
        logger.info("Sending rating deleted event for ratingId: {}", ratingId);
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(KafkaTopics.RATING_DELETED, ratingId, ratingId);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Rating deleted event sent successfully for ratingId: {} to partition: {}",
                    ratingId, result.getRecordMetadata().partition());
            } else {
                logger.error("Failed to send rating deleted event for ratingId: {}",
                    ratingId, ex);
            }
        });
    }
}
