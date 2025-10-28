package com.neo4flix.movieservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paged response wrapper for movie data
 */
@Schema(description = "Paginated movie response")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PagedMovieResponse {

    @Schema(description = "Success status")
    private boolean success;

    @Schema(description = "Response message")
    private String message;

    @Schema(description = "Paginated movie data")
    private PageData data;

    @Schema(description = "Pagination data wrapper")
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PageData {

        @Schema(description = "List of movies")
        private List<MovieResponse> movies;

        @Schema(description = "Total number of elements")
        private long totalElements;

        @Schema(description = "Total number of pages")
        private int totalPages;

        @Schema(description = "Current page number")
        private int currentPage;

        @Schema(description = "Whether there is a next page")
        private boolean hasNext;

        @Schema(description = "Whether there is a previous page")
        private boolean hasPrevious;
    }
}
