//package com.neo4flix.recommendationservice.repository;
//
//import com.neo4flix.recommendationservice.model.Genre;
//import org.springframework.data.neo4j.repository.Neo4jRepository;
//import org.springframework.data.neo4j.repository.query.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
///**
// * Repository interface for Genre entities
// */
//@Repository
//public interface GenreRepository extends Neo4jRepository<Genre, String> {
//
//    /**
//     * Find genre by name
//     */
//    Optional<Genre> findByName(String name);
//
//    /**
//     * Find genres by name containing text (case-insensitive)
//     */
//    List<Genre> findByNameContainingIgnoreCase(String name);
//
//    /**
//     * Check if genre exists by name
//     */
//    boolean existsByName(String name);
//
//    /**
//     * Find all genres ordered by name
//     */
//    @Query("MATCH (g:Genre) RETURN g ORDER BY g.name")
//    List<Genre> findAllOrderedByName();
//
//    /**
//     * Find popular genres (by number of movies)
//     */
//    @Query("MATCH (g:Genre)<-[:BELONGS_TO_GENRE]-(m:Movie) " +
//           "WITH g, COUNT(m) as movieCount " +
//           "RETURN g " +
//           "ORDER BY movieCount DESC " +
//           "LIMIT $limit")
//    List<Genre> findMostPopularGenres(@Param("limit") Integer limit);
//}