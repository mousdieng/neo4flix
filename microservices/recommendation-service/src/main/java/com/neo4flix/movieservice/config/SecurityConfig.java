package com.neo4flix.movieservice.config;

import com.neo4flix.movieservice.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the Recommendation Service
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
//            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - recommendation browsing and statistics
                .requestMatchers(
                    "/api/v1/recommendations/user/{userId}",
                    "/api/v1/recommendations/user/{userId}/stats",
                    "/api/v1/recommendations/user/{userId}/similar-users",
                    "/api/v1/recommendations/user/{userId}/algorithm/{algorithm}",
                    "/api/v1/recommendations/new-user",
                    "/api/v1/recommendations/algorithms",
                    "/api/v1/recommendations/health",
                    "/api/v1/recommendations/internal/**",
                    "/api/v1/recommendations/similar/**"
                ).permitAll()
                // Public documentation and health endpoints
                .requestMatchers(
                    "/actuator/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/api-docs/**"
                ).permitAll()
                // Authenticated endpoints - recommendation management
                .requestMatchers(
                    "/api/v1/recommendations/generate",
                    "/api/v1/recommendations/my/**",
                    "/api/v1/recommendations/clicked",
                    "/api/v1/recommendations/watched",
                    "/api/v1/recommendations/refresh",
                    "/api/v1/recommendations/trending",
                    "/api/v1/recommendations/genre/{genre}",
                    "/api/v1/recommendations/metrics"
                ).authenticated()
                // Admin only endpoints
                .requestMatchers(
                    "/api/v1/recommendations/batch-generate",
                    "/api/v1/recommendations/cleanup"
                ).hasRole("ADMIN")
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.setAllowedOriginPatterns(List.of("*"));
//        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
//        configuration.setAllowedHeaders(Arrays.asList("*"));
//        configuration.setAllowCredentials(true);
//        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }
}