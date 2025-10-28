package com.neo4flix.movieservice.model;

import com.neo4flix.movieservice.dto.PersonResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * Actor entity representing a movie actor node in Neo4j
 */
@Node("Actor")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Actor {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;

    @Property("name")
    @NotBlank(message = "Actor name cannot be blank")
    private String name;

    @Property("birthDate")
    private LocalDate birthDate;

    @Property("biography")
    private String biography;

    @Property("nationality")
    private String nationality;

    public PersonResponse mapActorToPersonResponse() {
        return PersonResponse.builder()
                .id(this.getId())
                .name(this.getName())
                .birthDate(this.getBirthDate())
                .biography(this.getBiography())
                .nationality(this.getNationality())
                .build();
    }
}