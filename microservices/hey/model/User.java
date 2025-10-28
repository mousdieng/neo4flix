package com.neo4flix.movieservice.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User entity for Recommendation Service
 */
@Node("User")
@NoArgsConstructor
@Data
public class User {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;

    @Property("username")
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Property("email")
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    private String email;

    @Property("password")
    @NotBlank(message = "Password cannot be blank")
    private String password;

    @Property("firstName")
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;

    @Property("lastName")
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;

    @Property("dateOfBirth")
    private LocalDateTime dateOfBirth;

    @Property("profilePictureUrl")
    private String profilePictureUrl;

    @Property("bio")
    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;

    @Property("enabled")
    private boolean enabled = true;

    @Property("accountNonExpired")
    private boolean accountNonExpired = true;

    @Property("accountNonLocked")
    private boolean accountNonLocked = true;

    @Property("credentialsNonExpired")
    private boolean credentialsNonExpired = true;

    @Property("emailVerified")
    private boolean emailVerified = false;

    @Property("twoFactorEnabled")
    private boolean twoFactorEnabled = false;

    @Property("twoFactorSecret")
    private String twoFactorSecret;

    @Property("createdAt")
    private LocalDateTime createdAt;

    @Property("updatedAt")
    private LocalDateTime updatedAt;

    @Property("lastLoginAt")
    private LocalDateTime lastLoginAt;

    @Property("role")
    private UserRole role = UserRole.USER;

    // Relationships
    @Relationship(type = "RATED", direction = Relationship.Direction.OUTGOING)
    private Set<Rating> ratings;

    @Relationship(type = "WATCHLIST", direction = Relationship.Direction.OUTGOING)
    private Set<Movie> watchlist;

    @Relationship(type = "FRIENDS_WITH", direction = Relationship.Direction.OUTGOING)
    private Set<User> friends;
}