package com.neo4flix.ratingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Rating Service Application for Neo4flix
 * Handles movie ratings and user rating analytics
 */
@SpringBootApplication
@EnableDiscoveryClient
public class RatingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RatingServiceApplication.class, args);
    }
}