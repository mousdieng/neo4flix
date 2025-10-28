package com.neo4flix.movieservice.kafka.producer;

import com.neo4flix.movieservice.kafka.KafkaTopics;
import com.neo4flix.movieservice.kafka.event.MovieCreatedEvent;
import com.neo4flix.movieservice.kafka.event.MovieUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class MovieEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(MovieEventProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MovieEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMovieCreatedEvent(MovieCreatedEvent event) {
        logger.info("Sending movie created event for movieId: {}", event.getMovieId());
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(KafkaTopics.MOVIE_CREATED, event.getMovieId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Movie created event sent successfully for movieId: {} to partition: {}",
                    event.getMovieId(), result.getRecordMetadata().partition());
            } else {
                logger.error("Failed to send movie created event for movieId: {}",
                    event.getMovieId(), ex);
            }
        });
    }

    public void sendMovieUpdatedEvent(MovieUpdatedEvent event) {
        logger.info("Sending movie updated event for movieId: {}", event.getMovieId());
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(KafkaTopics.MOVIE_UPDATED, event.getMovieId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Movie updated event sent successfully for movieId: {} to partition: {}",
                    event.getMovieId(), result.getRecordMetadata().partition());
            } else {
                logger.error("Failed to send movie updated event for movieId: {}",
                    event.getMovieId(), ex);
            }
        });
    }

    public void sendMovieDeletedEvent(String movieId) {
        logger.info("Sending movie deleted event for movieId: {}", movieId);
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(KafkaTopics.MOVIE_DELETED, movieId, movieId);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Movie deleted event sent successfully for movieId: {} to partition: {}",
                    movieId, result.getRecordMetadata().partition());
            } else {
                logger.error("Failed to send movie deleted event for movieId: {}",
                    movieId, ex);
            }
        });
    }
}
