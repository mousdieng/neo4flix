package com.neo4flix.movieservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

/**
 * Neo4flix Movie Service Application
 * Microservice responsible for:
 * - Movie CRUD operations
 * - Basic movie recommendations
 * - Movie search functionality
 * - Genre and metadata management
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableNeo4jRepositories(basePackages = "com.neo4flix.movieservice.repository")
public class MovieServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieServiceApplication.class, args);
    }
}