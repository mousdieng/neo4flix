package com.neo4flix.userservice.dto;

import com.neo4flix.userservice.model.FriendRequest;
import com.neo4flix.userservice.model.FriendRequest.FriendRequestStatus;
import com.neo4flix.userservice.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for FriendRequest responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestResponse {
    private String id;

    private String receiverId;
    private String receiverUsername;
    private String receiverFirstName;
    private String receiverLastName;
    private String receiverEmail;


    private String senderId;
    private String senderUsername;
    private String senderFirstName;
    private String senderLastName;
    private String senderEmail;

    private FriendRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}
