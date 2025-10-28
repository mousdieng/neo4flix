package com.neo4flix.movieservice.repository;

import com.neo4flix.movieservice.dto.MovieRecommendationDTO;
import com.neo4flix.movieservice.model.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Movie entities
 * Provides CRUD operations and custom queries for movies
 */
@Repository
public interface MovieRepository extends Neo4jRepository<Movie, String> {

    @Query("""
        MATCH (u:User)-[:RATED]->(rating:Rating)-[:RATED_MOVIE]->(m1:Movie)-[:IN_GENRE]->(g:Genre)
          WHERE u.id = $userId AND rating.rating >= 2
          WITH u, g, avg(rating.rating) AS genrePreference
          ORDER BY genrePreference DESC
          LIMIT 5
          MATCH (m2:Movie)-[:IN_GENRE]->(g)
          WHERE NOT EXISTS {
            MATCH (u)-[:RATED]->(:Rating)-[:RATED_MOVIE]->(m2)
          }
          RETURN DISTINCT
                 m2.id AS movieId,
                 m2.title AS movieTitle,
                 COALESCE(m2.averageRating, 0.0) AS movieRating,
                 genrePreference AS score,
                 collect(DISTINCT g.name) AS genres
          ORDER BY score DESC
          LIMIT 10
    """)
    List<MovieRecommendationDTO> getContentBasedRecommendations(
            @Param("userId") String userId
//            @Param("minRating") double minRating,
//            @Param("minMovieRating") double minMovieRating,
//            @Param("limit") int limit
    );

    /**
     * Find movie by ID with all relationships loaded
     */
    @Query("""
        MATCH (m:Movie)
        WHERE m.id = $movieId
        OPTIONAL MATCH (m)-[:IN_GENRE]->(g:Genre)
        OPTIONAL MATCH (d:Director)-[:DIRECTED]->(m)
        OPTIONAL MATCH (a:Actor)-[:ACTED_IN]->(m)
        RETURN m,
               collect(DISTINCT g) as genres,
               collect(DISTINCT d) as directors,
               collect(DISTINCT a) as actors
        """)
    Optional<Movie> findByIdWithRelationships(@Param("movieId") String movieId);

    /**
     * Find movie by title and release year
     */
    Optional<Movie> findByTitleAndReleaseYear(String title, Integer releaseYear);

    /**
     * Find movies by title containing text (case-insensitive)
     */
    List<Movie> findByTitleContainingIgnoreCase(String title);

    /**
     * Find movies by release year
     */
    List<Movie> findByReleaseYear(Integer releaseYear);

    /**
     * Find movies by release year range
     */
    List<Movie> findByReleaseYearBetween(Integer startYear, Integer endYear);

    /**
     * Find movies by genre name
     */
    @Query("MATCH (m:Movie)-[:BELONGS_TO_GENRE]->(g:Genre) WHERE g.name = $genreName RETURN m")
    List<Movie> findByGenreName(@Param("genreName") String genreName);

    /**
     * Find movies by multiple genres
     */
    @Query("MATCH (m:Movie)-[:BELONGS_TO_GENRE]->(g:Genre) " +
           "WHERE g.name IN $genreNames " +
           "WITH m, COUNT(DISTINCT g) as genreCount " +
           "WHERE genreCount = SIZE($genreNames) " +
           "RETURN m")
    List<Movie> findByGenreNames(@Param("genreNames") List<String> genreNames);

    /**
     * Find movies by director name
     */
    @Query("MATCH (d:Director)-[:DIRECTED]->(m:Movie) WHERE d.name = $directorName RETURN m")
    List<Movie> findByDirectorName(@Param("directorName") String directorName);

    /**
     * Find movies by actor name
     */
    @Query("MATCH (a:Actor)-[:ACTED_IN]->(m:Movie) WHERE a.name = $actorName RETURN m")
    List<Movie> findByActorName(@Param("actorName") String actorName);

    /**
     * Search movies by multiple criteria
     */
    @Query("MATCH (m:Movie) " +
           "OPTIONAL MATCH (m)-[:BELONGS_TO_GENRE]->(g:Genre) " +
           "OPTIONAL MATCH (d:Director)-[:DIRECTED]->(m) " +
           "OPTIONAL MATCH (a:Actor)-[:ACTED_IN]->(m) " +
           "WHERE ($title IS NULL OR m.title CONTAINS $title) " +
           "AND ($genre IS NULL OR g.name = $genre) " +
           "AND ($director IS NULL OR d.name CONTAINS $director) " +
           "AND ($actor IS NULL OR a.name CONTAINS $actor) " +
           "AND ($minYear IS NULL OR m.releaseYear >= $minYear) " +
           "AND ($maxYear IS NULL OR m.releaseYear <= $maxYear) " +
           "RETURN DISTINCT m " +
           "ORDER BY m.title")
    List<Movie> searchMovies(@Param("title") String title,
                            @Param("genre") String genre,
                            @Param("director") String director,
                            @Param("actor") String actor,
                            @Param("minYear") Integer minYear,
                            @Param("maxYear") Integer maxYear);

    /**
     * Find top-rated movies
     */
    @Query("MATCH (m:Movie)<-[r:RATED]-(u:User) " +
           "WITH m, AVG(r.rating) as avgRating, COUNT(r) as ratingCount " +
           "WHERE ratingCount >= $minRatings " +
           "RETURN m, avgRating " +
           "ORDER BY avgRating DESC " +
           "LIMIT $limit")
    List<Movie> findTopRatedMovies(@Param("minRatings") Integer minRatings, @Param("limit") Integer limit);

    /**
     * Find most popular movies (by number of ratings)
     */
    @Query("MATCH (m:Movie)<-[r:RATED]-(u:User) " +
           "WITH m, COUNT(r) as ratingCount " +
           "RETURN m, ratingCount " +
           "ORDER BY ratingCount DESC " +
           "LIMIT $limit")
    List<Movie> findMostPopularMovies(@Param("limit") Integer limit);

    /**
     * Find recently released movies
     */
    @Query("MATCH (m:Movie) " +
           "WHERE m.releaseYear >= $fromYear " +
           "RETURN m " +
           "ORDER BY m.releaseYear DESC, m.title " +
           "LIMIT $limit")
    List<Movie> findRecentMovies(@Param("fromYear") Integer fromYear, @Param("limit") Integer limit);

    /**
     * Find movies similar to a given movie
     */
    @Query("MATCH (m:Movie)-[s:SIMILAR_TO]->(similar:Movie) " +
           "WHERE m.id = $movieId " +
           "RETURN similar, s.score as similarityScore " +
           "ORDER BY s.score DESC " +
           "LIMIT $limit")
    List<Movie> findSimilarMovies(@Param("movieId") String movieId, @Param("limit") Integer limit);

    /**
     * Find movies recommended for a user based on their ratings and genre preferences
     */
    @Query("MATCH (u:User {id: $userId})-[r:RATED]->(m:Movie)-[:BELONGS_TO_GENRE]->(g:Genre) " +
           "WITH u, g, AVG(r.rating) as avgGenreRating, COUNT(r) as genreCount " +
           "WHERE avgGenreRating >= $minRating " +
           "MATCH (rec:Movie)-[:BELONGS_TO_GENRE]->(g) " +
           "WHERE NOT EXISTS((u)-[:RATED]->(rec)) " +
           "WITH rec, SUM(avgGenreRating * genreCount) as recommendationScore " +
           "RETURN rec " +
           "ORDER BY recommendationScore DESC " +
           "LIMIT $limit")
    List<Movie> findRecommendedMoviesForUser(@Param("userId") String userId,
                                           @Param("minRating") Double minRating,
                                           @Param("limit") Integer limit);

    /**
     * Get movie statistics
     */
    @Query("MATCH (m:Movie) " +
           "OPTIONAL MATCH (m)<-[r:RATED]-() " +
           "RETURN COUNT(m) as totalMovies, " +
           "       COUNT(r) as totalRatings, " +
           "       AVG(r.rating) as overallAverageRating")
    MovieStats getMovieStats();

    /**
     * Interface for movie statistics projection
     */
    interface MovieStats {
        Long getTotalMovies();
        Long getTotalRatings();
        Double getOverallAverageRating();
    }

    /**
     * Find movies with complete information (has genre, director, and at least one actor)
     */
    @Query("MATCH (m:Movie) " +
           "WHERE EXISTS((m)-[:BELONGS_TO_GENRE]->(:Genre)) " +
           "AND EXISTS((:Director)-[:DIRECTED]->(m)) " +
           "AND EXISTS((:Actor)-[:ACTED_IN]->(m)) " +
           "RETURN m " +
           "ORDER BY m.title")
    List<Movie> findMoviesWithCompleteInfo();

    /**
     * Count movies by genre
     */
    @Query("MATCH (m:Movie)-[:BELONGS_TO_GENRE]->(g:Genre) " +
           "RETURN g.name as genreName, COUNT(m) as movieCount " +
           "ORDER BY movieCount DESC")
    List<GenreMovieCount> countMoviesByGenre();

    /**
     * Interface for genre movie count projection
     */
    interface GenreMovieCount {
        String getGenreName();
        Long getMovieCount();
    }

    /**
     * Find all movies with pagination (includes genres for filtering)
     * Returns just movie IDs, then loads full entities with relationships
     */
    @Query(value = "MATCH (m:Movie) RETURN m.id AS id ORDER BY m.title ASC SKIP $skip LIMIT $limit",
           countQuery = "MATCH (m:Movie) RETURN count(m)")
    Page<String> findAllMovieIdsByTitle(Pageable pageable);

    /**
     * Find all movies sorted by IMDB rating
     */
    @Query(value = "MATCH (m:Movie) WHERE m.averageRating IS NOT NULL RETURN m.id AS id ORDER BY m.averageRating DESC SKIP $skip LIMIT $limit",
           countQuery = "MATCH (m:Movie) WHERE m.averageRating IS NOT NULL RETURN count(m)")
    Page<String> findAllMovieIdsByAverageRating(Pageable pageable);

    /**
     * Find all movies sorted by release year
     */
    @Query(value = "MATCH (m:Movie) WHERE m.releaseYear IS NOT NULL RETURN m.id AS id ORDER BY m.releaseYear DESC SKIP $skip LIMIT $limit",
           countQuery = "MATCH (m:Movie) WHERE m.releaseYear IS NOT NULL RETURN count(m)")
    Page<String> findAllMovieIdsByReleaseYear(Pageable pageable);

    /**
     * Find all movies with genres loaded (for the WithGenres methods)
     */
    @Query("MATCH (m:Movie) WHERE m.id IN $ids RETURN m")
    List<Movie> findAllByIdWithGenres(List<String> ids);

    // Temporary compatibility - these will be refactored
    default Page<Movie> findAllMoviesByTitleWithGenres(Pageable pageable) {
        Page<String> ids = findAllMovieIdsByTitle(pageable);
        List<Movie> movies = findAllByIdWithGenres(ids.getContent());
        return new PageImpl<>(movies, pageable, ids.getTotalElements());
    }

    default Page<Movie> findAllMoviesByAverageRatingWithGenres(Pageable pageable) {
        Page<String> ids = findAllMovieIdsByAverageRating(pageable);
        List<Movie> movies = findAllByIdWithGenres(ids.getContent());
        return new PageImpl<>(movies, pageable, ids.getTotalElements());
    }

    default Page<Movie> findAllMoviesByReleaseYearWithGenres(Pageable pageable) {
        Page<String> ids = findAllMovieIdsByReleaseYear(pageable);
        List<Movie> movies = findAllByIdWithGenres(ids.getContent());
        return new PageImpl<>(movies, pageable, ids.getTotalElements());
    }

    // Keep old methods for backward compatibility
    @Query(value = "MATCH (m:Movie) RETURN m ORDER BY m.title ASC SKIP $skip LIMIT $limit",
           countQuery = "MATCH (m:Movie) RETURN count(m)")
    Page<Movie> findAllMoviesByTitle(Pageable pageable);

    @Query(value = "MATCH (m:Movie) WHERE m.averageRating IS NOT NULL RETURN m ORDER BY m.averageRating DESC SKIP $skip LIMIT $limit",
           countQuery = "MATCH (m:Movie) WHERE m.averageRating IS NOT NULL RETURN count(m)")
    Page<Movie> findAllMoviesByAverageRating(Pageable pageable);

    @Query(value = "MATCH (m:Movie) WHERE m.releaseYear IS NOT NULL RETURN m ORDER BY m.releaseYear DESC SKIP $skip LIMIT $limit",
           countQuery = "MATCH (m:Movie) WHERE m.releaseYear IS NOT NULL RETURN count(m)")
    Page<Movie> findAllMoviesByReleaseYear(Pageable pageable);

    /**
     * Comprehensive dynamic search with all filters applied at database level
     * Supports all possible search criteria for maximum flexibility
     */
    @Query(value = """
        MATCH (m:Movie)
        WHERE ($query IS NULL OR $query = '' OR
               toLower(m.title) CONTAINS toLower($query) OR
               EXISTS {
                   MATCH (m)<-[:DIRECTED]-(d:Director)
                   WHERE toLower(d.name) CONTAINS toLower($query)
               } OR
               EXISTS {
                   MATCH (m)<-[:ACTED_IN]-(a:Actor)
                   WHERE toLower(a.name) CONTAINS toLower($query)
               })
          AND ($title IS NULL OR $title = '' OR toLower(m.title) CONTAINS toLower($title))
          AND ($director IS NULL OR $director = '' OR EXISTS {
              MATCH (m)<-[:DIRECTED]-(d:Director)
              WHERE toLower(d.name) CONTAINS toLower($director)
          })
          AND ($actor IS NULL OR $actor = '' OR EXISTS {
              MATCH (m)<-[:ACTED_IN]-(a:Actor)
              WHERE toLower(a.name) CONTAINS toLower($actor)
          })
          AND ($genre IS NULL OR $genre = '' OR EXISTS {
              MATCH (m)-[:IN_GENRE]->(g:Genre)
              WHERE g.name = $genre
          })
          AND ($year IS NULL OR m.releaseYear = $year)
          AND ($minYear IS NULL OR m.releaseYear >= $minYear)
          AND ($maxYear IS NULL OR m.releaseYear <= $maxYear)
          AND ($minRating IS NULL OR COALESCE(m.averageRating, 0.0) >= $minRating)
          AND ($maxRating IS NULL OR COALESCE(m.averageRating, 0.0) <= $maxRating)
          AND ($language IS NULL OR $language = '' OR toLower(m.language) CONTAINS toLower($language))
          AND ($country IS NULL OR $country = '' OR toLower(m.country) CONTAINS toLower($country))
          AND ($minDuration IS NULL OR m.duration >= $minDuration)
          AND ($maxDuration IS NULL OR m.duration <= $maxDuration)
          AND ($minBudget IS NULL OR m.budget >= $minBudget)
          AND ($maxBudget IS NULL OR m.budget <= $maxBudget)
          AND ($minBoxOffice IS NULL OR m.boxOffice >= $minBoxOffice)
          AND ($maxBoxOffice IS NULL OR m.boxOffice <= $maxBoxOffice)
        WITH DISTINCT m
        ORDER BY
            CASE WHEN $sortBy = 'title' AND $sortDir = 'ASC' THEN m.title END ASC,
            CASE WHEN $sortBy = 'title' AND $sortDir = 'DESC' THEN m.title END DESC,
            CASE WHEN $sortBy = 'releaseYear' AND $sortDir = 'ASC' THEN m.releaseYear END ASC,
            CASE WHEN $sortBy = 'releaseYear' AND $sortDir = 'DESC' THEN m.releaseYear END DESC,
            CASE WHEN $sortBy = 'averageRating' AND $sortDir = 'ASC' THEN COALESCE(m.averageRating, 0.0) END ASC,
            CASE WHEN $sortBy = 'averageRating' AND $sortDir = 'DESC' THEN COALESCE(m.averageRating, 0.0) END DESC,
            CASE WHEN $sortBy = 'duration' AND $sortDir = 'ASC' THEN m.duration END ASC,
            CASE WHEN $sortBy = 'duration' AND $sortDir = 'DESC' THEN m.duration END DESC,
            m.releaseYear DESC
        SKIP $skip
        LIMIT $limit
        RETURN m
        """)
    List<Movie> searchMoviesWithComprehensiveFilters(
        @Param("query") String query,
        @Param("title") String title,
        @Param("genre") String genre,
        @Param("director") String director,
        @Param("actor") String actor,
        @Param("year") Integer year,
        @Param("minYear") Integer minYear,
        @Param("maxYear") Integer maxYear,
        @Param("minRating") Double minRating,
        @Param("maxRating") Double maxRating,
        @Param("language") String language,
        @Param("country") String country,
        @Param("minDuration") Integer minDuration,
        @Param("maxDuration") Integer maxDuration,
        @Param("minBudget") Long minBudget,
        @Param("maxBudget") Long maxBudget,
        @Param("minBoxOffice") Long minBoxOffice,
        @Param("maxBoxOffice") Long maxBoxOffice,
        @Param("sortBy") String sortBy,
        @Param("sortDir") String sortDir,
        @Param("skip") long skip,
        @Param("limit") int limit
    );

    /**
     * Count movies matching comprehensive filters
     */
    @Query("""
        MATCH (m:Movie)
        WHERE ($query IS NULL OR $query = '' OR
               toLower(m.title) CONTAINS toLower($query) OR
               EXISTS {
                   MATCH (m)<-[:DIRECTED]-(d:Director)
                   WHERE toLower(d.name) CONTAINS toLower($query)
               } OR
               EXISTS {
                   MATCH (m)<-[:ACTED_IN]-(a:Actor)
                   WHERE toLower(a.name) CONTAINS toLower($query)
               })
          AND ($title IS NULL OR $title = '' OR toLower(m.title) CONTAINS toLower($title))
          AND ($director IS NULL OR $director = '' OR EXISTS {
              MATCH (m)<-[:DIRECTED]-(d:Director)
              WHERE toLower(d.name) CONTAINS toLower($director)
          })
          AND ($actor IS NULL OR $actor = '' OR EXISTS {
              MATCH (m)<-[:ACTED_IN]-(a:Actor)
              WHERE toLower(a.name) CONTAINS toLower($actor)
          })
          AND ($genre IS NULL OR $genre = '' OR EXISTS {
              MATCH (m)-[:IN_GENRE]->(g:Genre)
              WHERE g.name = $genre
          })
          AND ($year IS NULL OR m.releaseYear = $year)
          AND ($minYear IS NULL OR m.releaseYear >= $minYear)
          AND ($maxYear IS NULL OR m.releaseYear <= $maxYear)
          AND ($minRating IS NULL OR COALESCE(m.averageRating, 0.0) >= $minRating)
          AND ($maxRating IS NULL OR COALESCE(m.averageRating, 0.0) <= $maxRating)
          AND ($language IS NULL OR $language = '' OR toLower(m.language) CONTAINS toLower($language))
          AND ($country IS NULL OR $country = '' OR toLower(m.country) CONTAINS toLower($country))
          AND ($minDuration IS NULL OR m.duration >= $minDuration)
          AND ($maxDuration IS NULL OR m.duration <= $maxDuration)
          AND ($minBudget IS NULL OR m.budget >= $minBudget)
          AND ($maxBudget IS NULL OR m.budget <= $maxBudget)
          AND ($minBoxOffice IS NULL OR m.boxOffice >= $minBoxOffice)
          AND ($maxBoxOffice IS NULL OR m.boxOffice <= $maxBoxOffice)
        RETURN count(DISTINCT m)
        """)
    long countMoviesWithComprehensiveFilters(
        @Param("query") String query,
        @Param("title") String title,
        @Param("genre") String genre,
        @Param("director") String director,
        @Param("actor") String actor,
        @Param("year") Integer year,
        @Param("minYear") Integer minYear,
        @Param("maxYear") Integer maxYear,
        @Param("minRating") Double minRating,
        @Param("maxRating") Double maxRating,
        @Param("language") String language,
        @Param("country") String country,
        @Param("minDuration") Integer minDuration,
        @Param("maxDuration") Integer maxDuration,
        @Param("minBudget") Long minBudget,
        @Param("maxBudget") Long maxBudget,
        @Param("minBoxOffice") Long minBoxOffice,
        @Param("maxBoxOffice") Long maxBoxOffice
    );
}