package com.neo4flix.watchlistservice.service.impl;

import com.neo4flix.watchlistservice.dto.*;
import com.neo4flix.watchlistservice.exception.ResourceNotFoundException;
import com.neo4flix.watchlistservice.exception.DuplicateResourceException;
import com.neo4flix.watchlistservice.model.Movie;
import com.neo4flix.watchlistservice.model.Watchlist;
import com.neo4flix.watchlistservice.repository.MovieRepository;
import com.neo4flix.watchlistservice.repository.WatchlistRepository;
import com.neo4flix.watchlistservice.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of WatchlistService
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WatchlistServiceImpl implements WatchlistService {

    private static final Logger logger = LoggerFactory.getLogger(WatchlistServiceImpl.class);

    private final WatchlistRepository watchlistRepository;
    private final MovieRepository movieRepository;

    @Override
    public WatchlistResponse addToWatchlist(AddToWatchlistRequest request) {
        logger.info("Adding movie {} to watchlist for user {}", request.getMovieId(), request.getUserId());

        // Check if movie exists
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Movie not found with id: " + request.getMovieId()));

        // Check if already in watchlist
        if (watchlistRepository.existsByUserIdAndMovieId(request.getUserId(), request.getMovieId())) {
            throw new DuplicateResourceException(
                    "Movie is already in watchlist for user: " + request.getUserId());
        }

        // Create watchlist entry
        Watchlist watchlist = Watchlist.builder()
                .id(UUID.randomUUID().toString())
                .userId(request.getUserId())
                .movieId(request.getMovieId())
                .addedAt(LocalDateTime.now())
                .priority(request.getPriority() != null ? request.getPriority() : 2)
                .notes(request.getNotes())
                .watched(false)
                .build();

        Watchlist saved = watchlistRepository.save(watchlist);

        logger.info("Successfully added movie {} to watchlist", request.getMovieId());

        return mapToResponse(saved);
    }

    @Override
    public void removeFromWatchlist(String userId, String movieId) {
        logger.info("Removing movie {} from watchlist for user {}", movieId, userId);

        Watchlist watchlist = watchlistRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Watchlist entry not found for user " + userId + " and movie " + movieId));

        watchlistRepository.delete(watchlist);

        logger.info("Successfully removed movie {} from watchlist", movieId);
    }

    @Override
    @Transactional(readOnly = true)
    public WatchlistPageResponse getUserWatchlist(String userId, WatchlistQueryParams params) {
        logger.info("Getting watchlist for user {} with params: {}", userId, params);

        // Get filtered and sorted watchlist items
        List<Watchlist> watchlistItems = watchlistRepository.findByUserIdWithFilters(
                userId,
                params.getWatched(),
                params.getPriority(),
                params.getGenres(),
                params.getFromYear(),
                params.getToYear(),
                params.getSortBy() != null ? params.getSortBy() : "addedAt",
                params.getSortDirection() != null ? params.getSortDirection() : "DESC"
        );

        // Apply pagination
        int totalItems = watchlistItems.size();
        int pageSize = params.getPageSize() != null ? params.getPageSize() : 20;
        int page = params.getPage() != null ? params.getPage() : 0;
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalItems);

        List<WatchlistResponse> paginatedItems;
        if (fromIndex < totalItems) {
            paginatedItems = watchlistItems.subList(fromIndex, toIndex).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } else {
            paginatedItems = List.of();
        }

        // Get statistics
        WatchlistPageResponse.WatchlistStats stats = getWatchlistStats(userId);

        return WatchlistPageResponse.builder()
                .items(paginatedItems)
                .totalItems(totalItems)
                .page(page)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .stats(stats)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WatchlistResponse> getUserWatchlistSimple(String userId) {
        logger.info("Getting simple watchlist for user {}", userId);

        List<Watchlist> watchlistItems = watchlistRepository.findByUserId(userId);

        return watchlistItems.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WatchlistResponse updateWatchlistEntry(String userId, String movieId, UpdateWatchlistRequest request) {
        logger.info("Updating watchlist entry for user {} and movie {}", userId, movieId);

        Watchlist watchlist = watchlistRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Watchlist entry not found for user " + userId + " and movie " + movieId));

        // Update fields if provided
        if (request.getPriority() != null) {
            watchlist.setPriority(request.getPriority());
        }
        if (request.getNotes() != null) {
            watchlist.setNotes(request.getNotes());
        }
        if (request.getWatched() != null) {
            watchlist.setWatched(request.getWatched());
            if (request.getWatched()) {
                watchlist.setWatchedAt(LocalDateTime.now());
            } else {
                watchlist.setWatchedAt(null);
            }
        }

        Watchlist updated = watchlistRepository.save(watchlist);

        logger.info("Successfully updated watchlist entry");

        return mapToResponse(updated);
    }

    @Override
    public WatchlistResponse markAsWatched(String userId, String movieId, Boolean watched) {
        logger.info("Marking movie {} as {} for user {}", movieId, watched ? "watched" : "unwatched", userId);

        Watchlist watchlist = watchlistRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Watchlist entry not found for user " + userId + " and movie " + movieId));

        watchlist.setWatched(watched);
        if (watched) {
            watchlist.setWatchedAt(LocalDateTime.now());
        } else {
            watchlist.setWatchedAt(null);
        }

        Watchlist updated = watchlistRepository.save(watchlist);

        logger.info("Successfully marked movie as {}", watched ? "watched" : "unwatched");

        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public WatchlistCheckResponse isInWatchlist(String userId, String movieId) {
        logger.debug("Checking if movie {} is in watchlist for user {}", movieId, userId);

        return watchlistRepository.findByUserIdAndMovieId(userId, movieId)
                .map(watchlist -> WatchlistCheckResponse.builder()
                        .inWatchlist(true)
                        .watchlistId(watchlist.getId())
                        .priority(watchlist.getPriority())
                        .watched(watchlist.getWatched())
                        .build())
                .orElse(WatchlistCheckResponse.builder()
                        .inWatchlist(false)
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public WatchlistPageResponse.WatchlistStats getWatchlistStats(String userId) {
        logger.debug("Getting watchlist stats for user {}", userId);

        List<Watchlist> allItems = watchlistRepository.findByUserId(userId);

        int totalMovies = allItems.size();
        int watchedMovies = (int) allItems.stream().filter(w -> Boolean.TRUE.equals(w.getWatched())).count();
        int unwatchedMovies = totalMovies - watchedMovies;
        int highPriority = (int) allItems.stream().filter(w -> w.getPriority() == 1).count();
        int mediumPriority = (int) allItems.stream().filter(w -> w.getPriority() == 2).count();
        int lowPriority = (int) allItems.stream().filter(w -> w.getPriority() == 3).count();

        return WatchlistPageResponse.WatchlistStats.builder()
                .totalMovies(totalMovies)
                .watchedMovies(watchedMovies)
                .unwatchedMovies(unwatchedMovies)
                .highPriority(highPriority)
                .mediumPriority(mediumPriority)
                .lowPriority(lowPriority)
                .build();
    }

    @Override
    public void clearWatchedMovies(String userId) {
        logger.info("Clearing watched movies from watchlist for user {}", userId);

        List<Watchlist> watchedItems = watchlistRepository.findByUserIdAndWatched(userId, true);
        watchlistRepository.deleteAll(watchedItems);

        logger.info("Cleared {} watched movies from watchlist", watchedItems.size());
    }

    @Override
    @Transactional(readOnly = true)
    public WatchlistResponse getWatchlistEntry(String watchlistId) {
        logger.debug("Getting watchlist entry with id {}", watchlistId);

        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Watchlist entry not found with id: " + watchlistId));

        return mapToResponse(watchlist);
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Map Watchlist entity to WatchlistResponse DTO
     */
    private WatchlistResponse mapToResponse(Watchlist watchlist) {
        WatchlistResponse response = WatchlistResponse.builder()
                .id(watchlist.getId())
                .userId(watchlist.getUserId())
                .movieId(watchlist.getMovieId())
                .addedAt(watchlist.getAddedAt())
                .notes(watchlist.getNotes())
                .watched(watchlist.getWatched())
                .watchedAt(watchlist.getWatchedAt())
                .build();

        // Set priority (this also sets the label)
        response.setPriority(watchlist.getPriority());
        return response;
    }
}