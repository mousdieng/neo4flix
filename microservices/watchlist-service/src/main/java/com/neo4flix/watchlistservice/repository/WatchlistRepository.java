package com.neo4flix.watchlistservice.repository;


import com.neo4flix.watchlistservice.model.Watchlist;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Watchlist operations
 */
@Repository
public interface WatchlistRepository extends Neo4jRepository<Watchlist, String> {

    /**
     * Find watchlist entry by user ID and movie ID
     */
    @Query("""
        MATCH (w:Watchlist)
        WHERE w.userId = $userId AND w.movieId = $movieId
        RETURN w
    """)
    Optional<Watchlist> findByUserIdAndMovieId(
            @Param("userId") String userId,
            @Param("movieId") String movieId
    );

    /**
     * Check if a movie is in user's watchlist
     */
    @Query("""
        MATCH (w:Watchlist)
        WHERE w.userId = $userId AND w.movieId = $movieId
        RETURN COUNT(w) > 0
    """)
    boolean existsByUserIdAndMovieId(
            @Param("userId") String userId,
            @Param("movieId") String movieId
    );

    /**
     * Find all watchlist entries for a user
     */
    @Query("""
        MATCH (w:Watchlist)
        WHERE w.userId = $userId
        RETURN w
        ORDER BY w.addedAt DESC
    """)
    List<Watchlist> findByUserId(@Param("userId") String userId);

    /**
     * Find watchlist entries by user ID and watched status
     */
    @Query("""
        MATCH (w:Watchlist)
        WHERE w.userId = $userId AND w.watched = $watched
        RETURN w
        ORDER BY w.addedAt DESC
    """)
    List<Watchlist> findByUserIdAndWatched(
            @Param("userId") String userId,
            @Param("watched") Boolean watched
    );

    /**
     * Find watchlist entries with filters and sorting
     * This is a complex query that supports multiple optional filters
     */
    @Query("""
        MATCH (w:Watchlist)
        WHERE w.userId = $userId
          AND ($watched IS NULL OR w.watched = $watched)
          AND ($priority IS NULL OR w.priority = $priority)
        OPTIONAL MATCH (m:Movie)
        WHERE m.id = w.movieId
          AND ($genres IS NULL OR SIZE($genres) = 0 OR EXISTS {
            MATCH (m)-[:IN_GENRE]->(g:Genre)
            WHERE g.name IN $genres
          })
          AND ($fromYear IS NULL OR m.releaseYear >= $fromYear)
          AND ($toYear IS NULL OR m.releaseYear <= $toYear)
        WITH w, m
        WHERE m IS NOT NULL OR ($genres IS NULL AND $fromYear IS NULL AND $toYear IS NULL)
        RETURN w
        ORDER BY
          CASE WHEN $sortBy = 'addedAt' AND $sortDirection = 'DESC' THEN w.addedAt END DESC,
          CASE WHEN $sortBy = 'addedAt' AND $sortDirection = 'ASC' THEN w.addedAt END ASC,
          CASE WHEN $sortBy = 'priority' AND $sortDirection = 'DESC' THEN w.priority END DESC,
          CASE WHEN $sortBy = 'priority' AND $sortDirection = 'ASC' THEN w.priority END ASC,
          CASE WHEN $sortBy = 'title' AND $sortDirection = 'DESC' THEN m.title END DESC,
          CASE WHEN $sortBy = 'title' AND $sortDirection = 'ASC' THEN m.title END ASC,
          w.addedAt DESC
    """)
    List<Watchlist> findByUserIdWithFilters(
            @Param("userId") String userId,
            @Param("watched") Boolean watched,
            @Param("priority") Integer priority,
            @Param("genres") String[] genres,
            @Param("fromYear") Integer fromYear,
            @Param("toYear") Integer toYear,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection
    );

    /**
     * Find unwatched movies in watchlist for a user
     */
    @Query("""
        MATCH (w:Watchlist)
        WHERE w.userId = $userId AND w.watched = false
        RETURN w
        ORDER BY w.priority ASC, w.addedAt DESC
    """)
    List<Watchlist> findUnwatchedByUserId(@Param("userId") String userId);

    /**
     * Find watchlist entries by priority
     */
    @Query("""
        MATCH (w:Watchlist)
        WHERE w.userId = $userId AND w.priority = $priority
        RETURN w
        ORDER BY w.addedAt DESC
    """)
    List<Watchlist> findByUserIdAndPriority(
            @Param("userId") String userId,
            @Param("priority") Integer priority
    );

    /**
     * Count total watchlist entries for a user
     */
    @Query("""
        MATCH (w:Watchlist)
        WHERE w.userId = $userId
        RETURN COUNT(w)
    """)
    Long countByUserId(@Param("userId") String userId);

    /**
     * Count watched movies in watchlist
     */
    @Query("""
        MATCH (w:Watchlist)
        WHERE w.userId = $userId AND w.watched = true
        RETURN COUNT(w)
    """)
    Long countWatchedByUserId(@Param("userId") String userId);

    /**
     * Count unwatched movies in watchlist
     */
    @Query("""
        MATCH (w:Watchlist)
        WHERE w.userId = $userId AND w.watched = false
        RETURN COUNT(w)
    """)
    Long countUnwatchedByUserId(@Param("userId") String userId);

    /**
     * Get watchlist movies by genre
     */
    @Query("""
        MATCH (w:Watchlist), (m:Movie)-[:IN_GENRE]->(g:Genre)
        WHERE w.userId = $userId
          AND w.movieId = m.id
          AND g.name = $genre
        RETURN w
        ORDER BY w.addedAt DESC
    """)
    List<Watchlist> findByUserIdAndGenre(
            @Param("userId") String userId,
            @Param("genre") String genre
    );

    /**
     * Get high priority unwatched movies
     */
    @Query("""
        MATCH (w:Watchlist)
        WHERE w.userId = $userId
          AND w.priority = 1
          AND w.watched = false
        RETURN w
        ORDER BY w.addedAt DESC
        LIMIT $limit
    """)
    List<Watchlist> findHighPriorityUnwatched(
            @Param("userId") String userId,
            @Param("limit") Integer limit
    );

    /**
     * Delete all watched movies from watchlist
     */
    @Query("""
        MATCH (w:Watchlist)
        WHERE w.userId = $userId AND w.watched = true
        DELETE w
    """)
    void deleteWatchedByUserId(@Param("userId") String userId);

    /**
     * Get oldest unwatched movies in watchlist
     */
    @Query("""
        MATCH (w:Watchlist)
        WHERE w.userId = $userId AND w.watched = false
        RETURN w
        ORDER BY w.addedAt ASC
        LIMIT $limit
    """)
    List<Watchlist> findOldestUnwatched(
            @Param("userId") String userId,
            @Param("limit") Integer limit
    );

    /**
     * Get recently added movies to watchlist
     */
    @Query("""
        MATCH (w:Watchlist)
        WHERE w.userId = $userId
        RETURN w
        ORDER BY w.addedAt DESC
        LIMIT $limit
    """)
    List<Watchlist> findRecentlyAdded(
            @Param("userId") String userId,
            @Param("limit") Integer limit
    );

    /**
     * Get watchlist entries with movie details
     */
    @Query("""
        MATCH (w:Watchlist), (m:Movie)
        WHERE w.userId = $userId AND w.movieId = m.id
        RETURN w, m
        ORDER BY w.addedAt DESC
    """)
    List<Watchlist> findByUserIdWithMovies(@Param("userId") String userId);

    /**
     * Search watchlist by movie title
     */
    @Query("""
        MATCH (w:Watchlist), (m:Movie)
        WHERE w.userId = $userId
          AND w.movieId = m.id
          AND toLower(m.title) CONTAINS toLower($searchTerm)
        RETURN w
        ORDER BY w.addedAt DESC
    """)
    List<Watchlist> searchByMovieTitle(
            @Param("userId") String userId,
            @Param("searchTerm") String searchTerm
    );

    /**
     * Get watchlist statistics by priority
     */
    @Query("""
        MATCH (w:Watchlist)
        WHERE w.userId = $userId
        RETURN w.priority AS priority, COUNT(w) AS count
        ORDER BY priority
    """)
    List<PriorityCount> getWatchlistCountsByPriority(@Param("userId") String userId);

    /**
     * Interface for priority count projection
     */
    interface PriorityCount {
        Integer getPriority();
        Long getCount();
    }

    /**
     * Get movies in watchlist that match recommendation criteria
     * Useful for filtering out watchlist movies from recommendations
     */
    @Query("""
        MATCH (w:Watchlist)
        WHERE w.userId = $userId AND w.watched = false
        RETURN collect(w.movieId) AS movieIds
    """)
    List<String> getUnwatchedMovieIds(@Param("userId") String userId);

    /**
     * Bulk check if movies are in watchlist
     */
    @Query("""
        MATCH (w:Watchlist)
        WHERE w.userId = $userId AND w.movieId IN $movieIds
        RETURN w.movieId AS movieId
    """)
    List<String> findMoviesInWatchlist(
            @Param("userId") String userId,
            @Param("movieIds") List<String> movieIds
    );
}