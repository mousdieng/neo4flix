package com.neo4flix.gatewayservice.config;

import com.neo4flix.gatewayservice.filter.AuthenticationFilter;
import com.neo4flix.gatewayservice.filter.LoggingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Gateway routing configuration for Neo4flix microservices
 */
@Configuration
public class GatewayConfig {

    private final AuthenticationFilter authenticationFilter;
    private final LoggingFilter loggingFilter;

    @Autowired
    public GatewayConfig(AuthenticationFilter authenticationFilter, LoggingFilter loggingFilter) {
        this.authenticationFilter = authenticationFilter;
        this.loggingFilter = loggingFilter;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service Routes
                .route("user-service-auth", r -> r
                        .path("/api/v1/auth/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .circuitBreaker(c -> c
                                        .setName("user-service-cb")
                                        .setFallbackUri("forward:/fallback/user-service"))
                                .retry(retryConfig -> retryConfig.setRetries(3)))
                        .uri("lb://user-service"))

                .route("user-service-users", r -> r
                        .path("/api/v1/users/**")
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .circuitBreaker(c -> c
                                        .setName("user-service-cb")
                                        .setFallbackUri("forward:/fallback/user-service"))
                                .retry(retryConfig -> retryConfig.setRetries(3)))
                        .uri("lb://user-service"))

                // Movie Service Routes
                .route("movie-service-public", r -> r
                        .path("/api/v1/movies/**")
                        .and()
                        .method("GET")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(hostAddressKeyResolver()))
                                .circuitBreaker(c -> c
                                        .setName("movie-service-cb")
                                        .setFallbackUri("forward:/fallback/movie-service"))
                                .retry(retryConfig -> retryConfig.setRetries(3)))
                        .uri("lb://movie-service"))

                .route("movie-service-admin", r -> r
                        .path("/api/v1/movies/**")
                        .and()
                        .method("POST", "PUT", "DELETE")
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .circuitBreaker(c -> c
                                        .setName("movie-service-cb")
                                        .setFallbackUri("forward:/fallback/movie-service"))
                                .retry(retryConfig -> retryConfig.setRetries(3)))
                        .uri("lb://movie-service"))

                // Rating Service Routes
                .route("rating-service-public", r -> r
                        .path("/api/v1/ratings/**")
                        .and()
                        .method("GET")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(hostAddressKeyResolver()))
                                .circuitBreaker(c -> c
                                        .setName("rating-service-cb")
                                        .setFallbackUri("forward:/fallback/rating-service"))
                                .retry(retryConfig -> retryConfig.setRetries(3)))
                        .uri("lb://rating-service"))

                .route("rating-service-auth", r -> r
                        .path("/api/v1/ratings/**")
                        .and()
                        .method("POST", "PUT", "DELETE")
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .circuitBreaker(c -> c
                                        .setName("rating-service-cb")
                                        .setFallbackUri("forward:/fallback/rating-service"))
                                .retry(retryConfig -> retryConfig.setRetries(3)))
                        .uri("lb://rating-service"))

                // Recommendation Service Routes
                .route("recommendation-service-public", r -> r
                        .path("/api/v1/recommendations/user/**",
                               "/api/v1/recommendations/new-user",
                               "/api/v1/recommendations/algorithms")
                        .and()
                        .method("GET")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(hostAddressKeyResolver()))
                                .circuitBreaker(c -> c
                                        .setName("recommendation-service-cb")
                                        .setFallbackUri("forward:/fallback/recommendation-service"))
                                .retry(retryConfig -> retryConfig.setRetries(3)))
                        .uri("http://localhost:9083"))

                .route("recommendation-service-auth", r -> r
                        .path("/api/v1/recommendations/**")
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .circuitBreaker(c -> c
                                        .setName("recommendation-service-cb")
                                        .setFallbackUri("forward:/fallback/recommendation-service"))
                                .retry(retryConfig -> retryConfig.setRetries(3)))
                        .uri("http://localhost:9083"))

                // Watchlist Service Routes - ALL require authentication
                .route("watchlist-service-auth", r -> r
                        .path("/api/v1/watchlist/**")
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                                .circuitBreaker(c -> c
                                        .setName("watchlist-service-cb")
                                        .setFallbackUri("forward:/fallback/watchlist-service"))
                                .retry(retryConfig -> retryConfig.setRetries(3)))
                        .uri("lb://watchlist-service"))

                // Actuator endpoints for all services
                .route("actuator-routes", r -> r
                        .path("/actuator/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .stripPrefix(0))
                        .uri("lb://gateway-service"))

                // Swagger/OpenAPI Documentation aggregation
                .route("user-service-docs", r -> r
                        .path("/user-service/v3/api-docs")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://user-service"))

                .route("movie-service-docs", r -> r
                        .path("/movie-service/v3/api-docs")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://movie-service"))

                .route("rating-service-docs", r -> r
                        .path("/rating-service/v3/api-docs")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://rating-service"))

                .route("recommendation-service-docs", r -> r
                        .path("/recommendation-service/v3/api-docs")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://recommendation-service"))

                .route("watchlist-service-docs", r -> r
                        .path("/watchlist-service/v3/api-docs")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://watchlist-service"))

                .build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    @Bean("hostAddressKeyResolver")
    @Primary
    public KeyResolver hostAddressKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest().getRemoteAddress() != null ?
                        exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() :
                        "unknown"
        );
    }

    @Bean("userKeyResolver")
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest().getHeaders().getFirst("X-User-Id") != null ?
                        Objects.requireNonNull(exchange.getRequest().getHeaders().getFirst("X-User-Id")) :
                        "anonymous"
        );
    }
}