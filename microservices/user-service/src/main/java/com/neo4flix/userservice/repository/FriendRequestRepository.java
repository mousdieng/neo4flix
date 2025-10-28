package com.neo4flix.userservice.repository;

import com.neo4flix.userservice.model.FriendRequest;
import com.neo4flix.userservice.model.FriendRequest.FriendRequestStatus;
import com.neo4flix.userservice.model.User;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for FriendRequest entity
 */
@Repository
public interface FriendRequestRepository extends Neo4jRepository<FriendRequest, String> {

    /**
     * Find all pending friend requests sent to a user with sender and receiver details
     */
    @Query("""
        MATCH (sender:User)-[:SENT_BY]-(fr:FriendRequest)-[:SENT_TO]->(receiver:User {id: $userId})
        WHERE fr.status = 'PENDING'
        RETURN fr.id AS requestId, fr.status AS status, fr.createdAt AS createdAt,
               fr.respondedAt AS respondedAt, fr.message AS message,
               sender AS sender, receiver AS receiver
        ORDER BY fr.createdAt DESC
        """)
    List<Map<String, Object>> findPendingRequestsDataForUser(@Param("userId") String userId);

    /**
     * Find all friend requests sent by a user with sender and receiver details
     */
    @Query("""
        MATCH (sender:User {id: $userId})-[:SENT_BY]-(fr:FriendRequest)-[:SENT_TO]->(receiver:User)
        RETURN fr.id AS requestId, fr.status AS status, fr.createdAt AS createdAt,
               fr.respondedAt AS respondedAt, fr.message AS message,
               sender AS sender, receiver AS receiver
        ORDER BY fr.createdAt DESC
        """)
    List<Map<String, Object>> findRequestsDataSentByUser(@Param("userId") String userId);

    /**
     * Find a specific friend request between two users
     */
    @Query("""
        MATCH (fr:FriendRequest)-[:SENT_BY]->(sender:User {id: $senderId})
        MATCH (fr)-[:SENT_TO]->(receiver:User {id: $receiverId})
        WHERE fr.status = $status
        RETURN fr, collect(sender), collect(receiver)
        LIMIT 1
        """)
    Optional<FriendRequest> findRequestBetweenUsers(
        @Param("senderId") String senderId,
        @Param("receiverId") String receiverId,
        @Param("status") String status
    );

    /**
     * Check if a pending request exists between two users (in either direction)
     */
    @Query("""
        MATCH (fr:FriendRequest)
        WHERE fr.status = 'PENDING'
        AND (
            (fr)-[:SENT_BY]->(:User {id: $userId1}) AND (fr)-[:SENT_TO]->(:User {id: $userId2})
            OR
            (fr)-[:SENT_BY]->(:User {id: $userId2}) AND (fr)-[:SENT_TO]->(:User {id: $userId1})
        )
        RETURN COUNT(fr) > 0
        """)
    boolean existsPendingRequestBetweenUsers(
        @Param("userId1") String userId1,
        @Param("userId2") String userId2
    );

    /**
     * Count pending friend requests for a user
     */
    @Query("""
        MATCH (fr:FriendRequest)-[:SENT_TO]->(receiver:User {id: $userId})
        WHERE fr.status = 'PENDING'
        RETURN COUNT(fr)
        """)
    long countPendingRequestsForUser(@Param("userId") String userId);

    /**
     * Delete all friend requests between two users
     */
    @Query("""
        MATCH (fr:FriendRequest)
        WHERE (
            (fr)-[:SENT_BY]->(:User {id: $userId1}) AND (fr)-[:SENT_TO]->(:User {id: $userId2})
            OR
            (fr)-[:SENT_BY]->(:User {id: $userId2}) AND (fr)-[:SENT_TO]->(:User {id: $userId1})
        )
        DETACH DELETE fr
        """)
    void deleteRequestsBetweenUsers(
        @Param("userId1") String userId1,
        @Param("userId2") String userId2
    );
}
