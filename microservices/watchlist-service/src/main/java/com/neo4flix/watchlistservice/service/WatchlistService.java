package com.neo4flix.watchlistservice.service;

import com.neo4flix.watchlistservice.dto.*;

import java.util.List;

/**
 * Service interface for Watchlist operations
 */
public interface WatchlistService {

    /**
     * Add a movie to user's watchlist
     */
    WatchlistResponse addToWatchlist(AddToWatchlistRequest request);

    /**
     * Remove a movie from user's watchlist
     */
    void removeFromWatchlist(String userId, String movieId);

    /**
     * Get user's watchlist with pagination and filtering
     */
    WatchlistPageResponse getUserWatchlist(String userId, WatchlistQueryParams params);

    /**
     * Get all watchlist items for a user (without pagination)
     */
    List<WatchlistResponse> getUserWatchlistSimple(String userId);

    /**
     * Update a watchlist entry
     */
    WatchlistResponse updateWatchlistEntry(String userId, String movieId, UpdateWatchlistRequest request);

    /**
     * Mark a movie as watched/unwatched
     */
    WatchlistResponse markAsWatched(String userId, String movieId, Boolean watched);

    /**
     * Check if a movie is in user's watchlist
     */
    WatchlistCheckResponse isInWatchlist(String userId, String movieId);

    /**
     * Get watchlist statistics for a user
     */
    WatchlistPageResponse.WatchlistStats getWatchlistStats(String userId);

    /**
     * Clear all watched movies from watchlist
     */
    void clearWatchedMovies(String userId);

    /**
     * Get watchlist entry by ID
     */
    WatchlistResponse getWatchlistEntry(String watchlistId);
}