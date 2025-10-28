package com.neo4flix.ratingservice.repository;

import com.neo4flix.ratingservice.model.User;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity
 */
@Repository
public interface UserRepository extends Neo4jRepository<User, String> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user exists by ID
     */
    @Query("MATCH (u:User) WHERE u.id = $userId RETURN count(u) > 0")
    boolean existsById(@Param("userId") String userId);

    /**
     * Get user with ratings count
     */
    @Query("MATCH (u:User) " +
           "WHERE u.id = $userId " +
           "OPTIONAL MATCH (u)-[r:RATED]->() " +
           "RETURN u, count(r) AS ratingsCount")
    Optional<User> findByIdWithRatingsCount(@Param("userId") String userId);
}