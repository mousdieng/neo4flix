package com.neo4flix.movieservice.repository;

import com.neo4flix.movieservice.dto.SharedRecommendationResponse;
import com.neo4flix.movieservice.model.SharedRecommendation;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for shared recommendations
 */
@Repository
public interface SharedRecommendationRepository extends Neo4jRepository<SharedRecommendation, String> {

    /**
     * Get all recommendations shared with a user (received from friends)
     */
    @Query("""
        MATCH (fromUser:User)-[:FRIENDS_WITH]->(toUser:User {id: $userId})
        MATCH (sr:SharedRecommendation {to_user_id: $userId, from_user_id: fromUser.id})
        MATCH (m:Movie {id: sr.movie_id})
        RETURN sr.id AS id,
               sr.from_user_id AS fromUserId,
               fromUser.username AS fromUsername,
               sr.to_user_id AS toUserId,
               m AS movie,
               sr.message AS message,
               sr.shared_at AS sharedAt,
               sr.viewed AS viewed,
               sr.viewed_at AS viewedAt
        ORDER BY sr.shared_at DESC
        """)
    List<SharedRecommendationResponse> findSharedWithUser(@Param("userId") String userId);

    /**
     * Get all recommendations shared by a user (sent to friends)
     */
    @Query("""
        MATCH (fromUser:User {id: $userId})-[:FRIENDS_WITH]->(toUser:User)
        MATCH (sr:SharedRecommendation {from_user_id: $userId, to_user_id: toUser.id})
        MATCH (m:Movie {id: sr.movie_id})
        RETURN sr.id AS id,
               sr.from_user_id AS fromUserId,
               fromUser.username AS fromUsername,
               sr.to_user_id AS toUserId,
               m AS movie,
               sr.message AS message,
               sr.shared_at AS sharedAt,
               sr.viewed AS viewed,
               sr.viewed_at AS viewedAt
        ORDER BY sr.shared_at DESC
        """)
    List<SharedRecommendationResponse> findSharedByUser(@Param("userId") String userId);

    /**
     * Count unviewed shared recommendations for a user
     */
    @Query("""
        MATCH (sr:SharedRecommendation {to_user_id: $userId, viewed: false})
        RETURN count(sr) AS count
        """)
    long countUnviewedForUser(@Param("userId") String userId);

    /**
     * Mark a shared recommendation as viewed
     */
    @Query("""
        MATCH (sr:SharedRecommendation {id: $sharedRecommendationId})
        SET sr.viewed = true, sr.viewed_at = datetime()
        RETURN sr
        """)
    SharedRecommendation markAsViewed(@Param("sharedRecommendationId") String sharedRecommendationId);

    /**
     * Check if a recommendation has already been shared from user to friend
     */
    @Query("""
        MATCH (sr:SharedRecommendation {
            from_user_id: $fromUserId,
            to_user_id: $toUserId,
            movie_id: $movieId
        })
        RETURN count(sr) > 0
        """)
    boolean existsByFromUserAndToUserAndMovie(
        @Param("fromUserId") String fromUserId,
        @Param("toUserId") String toUserId,
        @Param("movieId") String movieId
    );
}
