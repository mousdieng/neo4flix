package com.neo4flix.watchlistservice.kafka.producer;

import com.neo4flix.watchlistservice.kafka.KafkaTopics;
import com.neo4flix.watchlistservice.kafka.event.WatchlistEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class WatchlistEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(WatchlistEventProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public WatchlistEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendWatchlistAddedEvent(WatchlistEvent event) {
        logger.info("Sending watchlist added event for userId: {} and movieId: {}",
            event.getUserId(), event.getMovieId());
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(KafkaTopics.WATCHLIST_ADDED, event.getUserId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Watchlist added event sent successfully to partition: {}",
                    result.getRecordMetadata().partition());
            } else {
                logger.error("Failed to send watchlist added event", ex);
            }
        });
    }

    public void sendWatchlistRemovedEvent(WatchlistEvent event) {
        logger.info("Sending watchlist removed event for userId: {} and movieId: {}",
            event.getUserId(), event.getMovieId());
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(KafkaTopics.WATCHLIST_REMOVED, event.getUserId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Watchlist removed event sent successfully to partition: {}",
                    result.getRecordMetadata().partition());
            } else {
                logger.error("Failed to send watchlist removed event", ex);
            }
        });
    }
}
