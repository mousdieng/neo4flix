package com.neo4flix.movieservice.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

/**
 * Database initializer that loads movie data from JSON file on startup
 * if the database is empty. This allows the application to run in new
 * environments without requiring external API calls.
 */
@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final Driver neo4jDriver;
    private final ObjectMapper objectMapper;

    @Value("${neo4flix.data.seed-file:movies_seed.json}")
    private String seedFile;

    @Value("${neo4flix.data.auto-seed:true}")
    private boolean autoSeed;

    public DatabaseInitializer(Driver neo4jDriver, ObjectMapper objectMapper) {
        this.neo4jDriver = neo4jDriver;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!autoSeed) {
            logger.info("Auto-seed is disabled. Skipping database initialization.");
            return;
        }

        logger.info("Checking if database needs initialization...");

        if (!isDatabaseEmpty()) {
            logger.info("Database already contains movies. Skipping initialization.");
            return;
        }

        logger.info("Database is empty. Starting initialization from seed file: {}", seedFile);

        try {
            loadMoviesFromSeedFile();
            logger.info("Database initialization completed successfully!");
        } catch (Exception e) {
            logger.error("Failed to initialize database: {}", e.getMessage(), e);
            // Don't throw the exception - let the application start even if seeding fails
        }
    }

    /**
     * Check if database already contains movies
     */
    private boolean isDatabaseEmpty() {
        try (Session session = neo4jDriver.session()) {
            var result = session.run("MATCH (m:Movie) RETURN count(m) as count");
            if (result.hasNext()) {
                long count = result.next().get("count").asLong();
                return count == 0;
            }
            return true;
        }
    }

    /**
     * Load movies from JSON seed file
     */
    private void loadMoviesFromSeedFile() throws Exception {
        ClassPathResource resource = new ClassPathResource(seedFile);

        if (!resource.exists()) {
            logger.warn("Seed file not found: {}. Skipping database initialization.", seedFile);
            return;
        }

        logger.info("Loading seed data from: {}", seedFile);

        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);

            int totalMovies = root.path("total_movies").asInt(0);
            logger.info("Seed file contains {} movies", totalMovies);

            // Load genres first
            loadGenres(root.path("genres"));

            // Load movies with relationships
            loadMovies(root.path("movies"));

            logger.info("Successfully loaded {} movies from seed file", totalMovies);
        }
    }

    /**
     * Load genres into database
     */
    private void loadGenres(JsonNode genresNode) {
        if (!genresNode.isArray()) {
            return;
        }

        List<String> genres = new ArrayList<>();
        genresNode.forEach(node -> genres.add(node.asText()));

        logger.info("Creating {} genres...", genres.size());

        String query = """
            UNWIND $genres as genreName
            MERGE (g:Genre {name: genreName})
            """;

        try (Session session = neo4jDriver.session()) {
            session.run(query, Map.of("genres", genres));
            logger.info("✓ Genres created successfully");
        }
    }

    /**
     * Load movies into database
     */
    private void loadMovies(JsonNode moviesNode) {
        if (!moviesNode.isArray()) {
            return;
        }

        int batchSize = 100;
        List<Map<String, Object>> batch = new ArrayList<>();
        int totalProcessed = 0;

        for (JsonNode movieNode : moviesNode) {
            Map<String, Object> movieData = parseMovieNode(movieNode);
            batch.add(movieData);

            if (batch.size() >= batchSize) {
                importMovieBatch(batch);
                totalProcessed += batch.size();
                logger.info("Processed {} movies...", totalProcessed);
                batch.clear();
            }
        }

        // Import remaining movies
        if (!batch.isEmpty()) {
            importMovieBatch(batch);
            totalProcessed += batch.size();
            logger.info("Processed {} movies total", totalProcessed);
        }
    }

    /**
     * Parse movie JSON node into map
     */
    private Map<String, Object> parseMovieNode(JsonNode movieNode) {
        Map<String, Object> movie = new HashMap<>();

        movie.put("id", movieNode.path("id").asText());
        movie.put("title", movieNode.path("title").asText());
        movie.put("plot", movieNode.path("plot").asText(null));
        movie.put("releaseYear", movieNode.path("releaseYear").asInt(0));
        movie.put("runtime", movieNode.path("runtime").asInt(0));
        movie.put("imdbRating", movieNode.path("imdbRating").asDouble(0.0));
        movie.put("imdbVotes", movieNode.path("imdbVotes").asInt(0));
        movie.put("posterUrl", movieNode.path("posterUrl").asText(null));
        movie.put("backdropUrl", movieNode.path("backdropUrl").asText(null));

        // Parse genres
        List<String> genres = new ArrayList<>();
        JsonNode genresNode = movieNode.path("genres");
        if (genresNode.isArray()) {
            genresNode.forEach(node -> genres.add(node.asText()));
        }
        movie.put("genres", genres);

        // Parse directors
        List<Map<String, String>> directors = new ArrayList<>();
        JsonNode directorsNode = movieNode.path("directors");
        if (directorsNode.isArray()) {
            directorsNode.forEach(node -> {
                Map<String, String> director = new HashMap<>();
                director.put("id", node.path("id").asText());
                director.put("name", node.path("name").asText());
                directors.add(director);
            });
        }
        movie.put("directors", directors);

        // Parse actors
        List<Map<String, String>> actors = new ArrayList<>();
        JsonNode actorsNode = movieNode.path("actors");
        if (actorsNode.isArray()) {
            actorsNode.forEach(node -> {
                Map<String, String> actor = new HashMap<>();
                actor.put("id", node.path("id").asText());
                actor.put("name", node.path("name").asText());
                actors.add(actor);
            });
        }
        movie.put("actors", actors);

        return movie;
    }

    /**
     * Import a batch of movies using Cypher
     */
    private void importMovieBatch(List<Map<String, Object>> movies) {
        String query = """
            UNWIND $movies as movieData

            // Create movie node
            MERGE (m:Movie {id: movieData.id})
            SET m.title = movieData.title,
                m.plot = movieData.plot,
                m.releaseYear = movieData.releaseYear,
                m.duration = movieData.runtime,
                m.imdbRating = movieData.imdbRating,
                m.posterUrl = movieData.posterUrl

            // Create genre relationships
            WITH m, movieData
            UNWIND movieData.genres as genreName
            MATCH (g:Genre {name: genreName})
            MERGE (m)-[:BELONGS_TO_GENRE]->(g)

            // Create director relationships
            WITH m, movieData
            UNWIND movieData.directors as directorData
            MERGE (d:Director {id: directorData.id})
            ON CREATE SET d.name = directorData.name
            MERGE (d)-[:DIRECTED]->(m)

            // Create actor relationships
            WITH m, movieData
            UNWIND movieData.actors as actorData
            MERGE (a:Actor {id: actorData.id})
            ON CREATE SET a.name = actorData.name
            MERGE (a)-[:ACTED_IN]->(m)
            """;

        try (Session session = neo4jDriver.session()) {
            session.run(query, Map.of("movies", movies));
        }
    }
}
