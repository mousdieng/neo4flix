package com.neo4flix.userservice.model;

import com.neo4flix.userservice.dto.FriendRequestResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.LocalDateTime;

/**
 * FriendRequest entity representing a friend request between users in Neo4j
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Node("FriendRequest")
public class FriendRequest {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;

    @Relationship(type = "SENT_BY", direction = Relationship.Direction.OUTGOING)
    private User sender;

    @Relationship(type = "SENT_TO", direction = Relationship.Direction.OUTGOING)
    private User receiver;

    @Property("status")
    private FriendRequestStatus status;

    @Property("createdAt")
    private LocalDateTime createdAt;

    @Property("respondedAt")
    private LocalDateTime respondedAt;

    @Property("message")
    private String message;

    /**
     * Friend request status enumeration
     */
    public enum FriendRequestStatus {
        PENDING,
        ACCEPTED,
        REJECTED
    }

    /**
     * Pre-persist callback to set creation timestamp
     */
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = FriendRequestStatus.PENDING;
        }
    }

    public FriendRequestResponse convertToFriendRequestResponse() {
        return FriendRequestResponse.builder()
                .id(this.getId())
//                .senderId(senderId)
//                .senderEmail(senderEmail)
//                .senderFirstName(senderFirstName)
//                .senderLastName(senderLastName)
//                .senderUsername(senderUsername)
//                .receiverId(receiverId)
//                .receiverEmail(receiverEmail)
//                .receiverFirstName(receiverFirstName)
//                .receiverLastName(receiverLastName)
//                .receiverUsername(receiverUsername)
                .status(this.getStatus())
                .createdAt(this.getCreatedAt())
                .respondedAt(this.getRespondedAt())
                .build();
    }
}
