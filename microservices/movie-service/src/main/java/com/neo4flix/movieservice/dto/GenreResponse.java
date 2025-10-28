package com.neo4flix.movieservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Genre response DTO
 */
@Schema(description = "Genre information response")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GenreResponse {

    @Schema(description = "Genre ID", example = "1")
    private String id;

    @Schema(description = "Genre name", example = "Action")
    private String name;

    @Schema(description = "Genre description", example = "Action movies with thrilling sequences")
    private String description;
}