package com.neo4flix.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * DTO for updating user information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"username", "email"})
public class UpdateUserRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Email(message = "Email should be valid")
    private String email;

    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;

    private LocalDateTime dateOfBirth;

    private String profilePictureUrl;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;
}