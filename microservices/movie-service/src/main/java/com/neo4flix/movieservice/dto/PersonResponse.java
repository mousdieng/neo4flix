package com.neo4flix.movieservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Person response DTO for actors and directors
 */
@Schema(description = "Person information response")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PersonResponse {

    @Schema(description = "Person ID", example = "1")
    private String id;

    @Schema(description = "Person name", example = "Christopher Nolan")
    private String name;

    @Schema(description = "Birth date", example = "1970-07-30")
    private LocalDate birthDate;

    @Schema(description = "Biography", example = "British-American filmmaker...")
    private String biography;

    @Schema(description = "Nationality", example = "British")
    private String nationality;

    @Schema(description = "Role in movie (for actors)", example = "Dom Cobb")
    private String role;

    @Schema(description = "Whether this is a main character (for actors)", example = "true")
    private Boolean isMainCharacter;
}