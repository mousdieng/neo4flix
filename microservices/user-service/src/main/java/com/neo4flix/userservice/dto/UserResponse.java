package com.neo4flix.userservice.dto;

import com.neo4flix.userservice.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * DTO for user response (excludes sensitive information)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"id", "username", "email"})
public class UserResponse {

    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private LocalDateTime dateOfBirth;
    private String profilePictureUrl;
    private String bio;
    private boolean enabled;
    private boolean emailVerified;
    private boolean twoFactorEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private UserRole role;

    // Statistics
    private Long totalRatings;
    private Long watchlistSize;
    private Long friendCount;
    private Double averageRating;

    public UserResponse(String id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }
}