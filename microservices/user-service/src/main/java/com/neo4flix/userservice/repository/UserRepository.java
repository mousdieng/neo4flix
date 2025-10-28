package com.neo4flix.userservice.repository;

import com.neo4flix.userservice.model.User;
import com.neo4flix.userservice.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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

    @Query("MATCH (u:User) WHERE u.id = $userId RETURN count(u) > 0")
    boolean existsByIdHum(@Param("userId") String id);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username or email
     */
    @Query("MATCH (u:User) WHERE u.username = $usernameOrEmail OR u.email = $usernameOrEmail RETURN u")
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find users by role
     */
    List<User> findByRole(UserRole role);

    /**
     * Find enabled users
     */
    List<User> findByEnabledTrue();

    /**
     * Find users with email verification status
     */
    List<User> findByEmailVerified(boolean emailVerified);

    /**
     * Find users with 2FA enabled
     */
    List<User> findByTwoFactorEnabledTrue();

    /**
     * Find users created after a specific date
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find users by first name and last name
     */
    List<User> findByFirstNameAndLastName(String firstName, String lastName);

    /**
     * Search users by username containing text (case insensitive)
     */
    @Query("MATCH (u:User) WHERE toLower(u.username) CONTAINS toLower($searchTerm) RETURN u")
    List<User> searchByUsername(@Param("searchTerm") String searchTerm);

    /**
     * Search users by full name containing text
     */
    @Query("MATCH (u:User) WHERE toLower(u.firstName) CONTAINS toLower($searchTerm) OR toLower(u.lastName) CONTAINS toLower($searchTerm) RETURN u")
    List<User> searchByFullName(@Param("searchTerm") String searchTerm);

    /**
     * Find users with most ratings
     */
    @Query("MATCH (u:User)-[r:RATED]->(:Movie) RETURN u, count(r) as ratingCount ORDER BY ratingCount DESC LIMIT $limit")
    List<User> findMostActiveRaters(@Param("limit") int limit);

    /**
     * Find mutual friends between two users
     */
    @Query("MATCH (u1:User {id: $userId1})-[:FRIENDS_WITH]-(mutual:User)-[:FRIENDS_WITH]-(u2:User {id: $userId2}) RETURN mutual")
    List<User> findMutualFriends(@Param("userId1") String userId1, @Param("userId2") String userId2);

    /**
     * Find users who rated a specific movie
     */
    @Query("MATCH (u:User)-[:RATED]->(m:Movie {id: $movieId}) RETURN u")
    List<User> findUsersWhoRatedMovie(@Param("movieId") String movieId);

    /**
     * Find users with similar movie tastes
     */
    @Query("""
        MATCH (u1:User {id: $userId})-[:RATED]->(m:Movie)<-[:RATED]-(u2:User)
        WHERE u1 <> u2
        WITH u2, count(m) as commonMovies
        WHERE commonMovies >= $minCommonMovies
        RETURN u2
        ORDER BY commonMovies DESC
        LIMIT $limit
        """)
    List<User> findUsersWithSimilarTastes(@Param("userId") String userId,
                                         @Param("minCommonMovies") int minCommonMovies,
                                         @Param("limit") int limit);

    /**
     * Get user with statistics - returns just the user
     */
    @Query("MATCH (u:User {id: $userId}) RETURN u")
    Optional<User> findUserForStatistics(@Param("userId") String userId);

    /**
     * Count user ratings
     */
    @Query("MATCH (u:User {id: $userId})-[r:RATED]->(:Movie) RETURN count(r)")
    Long countUserRatings(@Param("userId") String userId);

    /**
     * Get average rating
     */
    @Query("MATCH (u:User {id: $userId})-[r:RATED]->(:Movie) RETURN avg(r.rating)")
    Double getAverageRating(@Param("userId") String userId);

    /**
     * Count watchlist items
     */
    @Query("MATCH (wl:Watchlist {userId: $userId}) RETURN count(wl)")
    Long countWatchlistItems(@Param("userId") String userId);

    /**
     * Count friends
     */
    @Query("MATCH (u:User {id: $userId})-[:FRIENDS_WITH]-(f:User) RETURN count(f)")
    Long countFriends(@Param("userId") String userId);

    /**
     * Update last login time
     */
    @Query("MATCH (u:User {id: $userId}) SET u.lastLoginAt = $lastLoginAt RETURN u")
    User updateLastLogin(@Param("userId") String userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    /**
     * Find users by page with search
     */
    @Query(value = """
        MATCH (u:User)
        WHERE ($searchTerm IS NULL OR
               toLower(u.username) CONTAINS toLower($searchTerm) OR
               toLower(u.firstName) CONTAINS toLower($searchTerm) OR
               toLower(u.lastName) CONTAINS toLower($searchTerm) OR
               toLower(u.email) CONTAINS toLower($searchTerm))
        RETURN u
        ORDER BY u.createdAt DESC
        SKIP $skip LIMIT $limit
        """,
        countQuery = """
        MATCH (u:User)
        WHERE ($searchTerm IS NULL OR
               toLower(u.username) CONTAINS toLower($searchTerm) OR
               toLower(u.firstName) CONTAINS toLower($searchTerm) OR
               toLower(u.lastName) CONTAINS toLower($searchTerm) OR
               toLower(u.email) CONTAINS toLower($searchTerm))
        RETURN count(u)
        """)
    Page<User> findUsersWithSearch(@Param("searchTerm") String searchTerm, Pageable pageable);
}