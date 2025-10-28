package com.neo4flix.movieservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response wrapper for genre list
 */
@Schema(description = "Genre list response")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GenreListResponse {

    @Schema(description = "Success status")
    private boolean success;

    @Schema(description = "Response message")
    private String message;

    @Schema(description = "List of genre names")
    private List<String> data;
}
