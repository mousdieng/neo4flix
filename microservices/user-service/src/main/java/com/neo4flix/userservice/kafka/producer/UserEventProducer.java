package com.neo4flix.userservice.kafka.producer;

import com.neo4flix.userservice.kafka.KafkaTopics;
import com.neo4flix.userservice.kafka.event.UserCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class UserEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(UserEventProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendUserCreatedEvent(UserCreatedEvent event) {
        logger.info("Sending user created event for userId: {}", event.getUserId());
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(KafkaTopics.USER_CREATED, event.getUserId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("User created event sent successfully for userId: {} to partition: {}",
                    event.getUserId(), result.getRecordMetadata().partition());
            } else {
                logger.error("Failed to send user created event for userId: {}",
                    event.getUserId(), ex);
            }
        });
    }

    public void sendUserUpdatedEvent(String userId, String username, String email) {
        logger.info("Sending user updated event for userId: {}", userId);
        UserCreatedEvent event = new UserCreatedEvent(userId, username, email);
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(KafkaTopics.USER_UPDATED, userId, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("User updated event sent successfully for userId: {} to partition: {}",
                    userId, result.getRecordMetadata().partition());
            } else {
                logger.error("Failed to send user updated event for userId: {}",
                    userId, ex);
            }
        });
    }

    public void sendUserDeletedEvent(String userId) {
        logger.info("Sending user deleted event for userId: {}", userId);
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(KafkaTopics.USER_DELETED, userId, userId);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("User deleted event sent successfully for userId: {} to partition: {}",
                    userId, result.getRecordMetadata().partition());
            } else {
                logger.error("Failed to send user deleted event for userId: {}",
                    userId, ex);
            }
        });
    }
}
