package com.example.routing.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Represents a country entry from the countries.json dataset.
 * Only the fields required for graph construction are mapped;
 * all other JSON fields are deliberately ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Country(
        String cca3,
        List<String> borders) {
}
