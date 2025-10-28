package com.neo4flix.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Neo4flix User Service Application
 *
 * Microservice responsible for:
 * - User registration and authentication
 * - JWT token management
 * - User profile management
 * - Two-factor authentication
 * - User account operations
 * - Access control and authorization
 */
@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}