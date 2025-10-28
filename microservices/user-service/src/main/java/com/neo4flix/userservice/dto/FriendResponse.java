package com.neo4flix.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FriendResponse {
    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private Long friendCount;
    private String profilePictureUrl;
}
