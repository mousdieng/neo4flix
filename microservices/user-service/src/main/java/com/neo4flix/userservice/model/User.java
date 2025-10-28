package com.neo4flix.userservice.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User entity representing a user node in Neo4j
 * Implements UserDetails for Spring Security integration
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(of = {"id", "username", "email"})
@Node("User")
public class User implements UserDetails {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    @EqualsAndHashCode.Include
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

    // Custom initialization
    {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.ratings = new HashSet<>();
        this.watchlist = new HashSet<>();
        this.friends = new HashSet<>();
    }

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // Custom methods
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return username;
    }

    // Utility methods
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
}