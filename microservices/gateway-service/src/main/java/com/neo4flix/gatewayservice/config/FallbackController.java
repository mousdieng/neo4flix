package com.neo4flix.gatewayservice.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback controller for circuit breaker responses
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user-service")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        Map<String, Object> response = createFallbackResponse(
                "User Service is temporarily unavailable",
                "Please try again later or contact support if the problem persists"
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/movie-service")
    public ResponseEntity<Map<String, Object>> movieServiceFallback() {
        Map<String, Object> response = createFallbackResponse(
                "Movie Service is temporarily unavailable",
                "Movie information is currently not accessible. Please try again later."
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/rating-service")
    public ResponseEntity<Map<String, Object>> ratingServiceFallback() {
        Map<String, Object> response = createFallbackResponse(
                "Rating Service is temporarily unavailable",
                "Rating functionality is currently not accessible. Please try again later."
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/recommendation-service")
    public ResponseEntity<Map<String, Object>> recommendationServiceFallback() {
        Map<String, Object> response = createFallbackResponse(
                "Recommendation Service is temporarily unavailable",
                "Personalized recommendations are currently not available. Please browse our popular movies instead."
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/watchlist-service")
    public ResponseEntity<Map<String, Object>> watchlistServiceFallback() {
        Map<String, Object> response = createFallbackResponse(
                "Watchlist Service is temporarily unavailable",
                "Personalized watchlist are currently not available. Please browse our popular movies instead."
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    private Map<String, Object> createFallbackResponse(String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", error);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        response.put("fallback", true);
        return response;
    }
}