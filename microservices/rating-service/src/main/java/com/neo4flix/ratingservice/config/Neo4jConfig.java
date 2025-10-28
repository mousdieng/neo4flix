package com.neo4flix.ratingservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

/**
 * Neo4j configuration for Rating Service
 */
@Configuration
@EnableNeo4jRepositories(basePackages = "com.neo4flix.ratingservice.repository")
public class Neo4jConfig {
}