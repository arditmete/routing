package com.example.routing.service;

import com.example.routing.model.Country;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CountryDataLoader {

    private static final Logger log = LoggerFactory.getLogger(CountryDataLoader.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String countriesUrl;

    private Map<String, Set<String>> adjacencyGraph;

    public CountryDataLoader(
            RestClient restClient,
            ObjectMapper objectMapper,
            @Value("${app.countries.url}") String countriesUrl) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.countriesUrl = countriesUrl;
    }

    @PostConstruct
    public void loadCountries() {
        log.info("Downloading countries dataset from {}", countriesUrl);
        try {
            String json = restClient.get()
                    .uri(countriesUrl)
                    .retrieve()
                    .body(String.class);

            if (json == null || json.isBlank()) {
                throw new IllegalStateException("Received empty response from countries dataset URL");
            }

            List<Country> countries = objectMapper.readValue(json, new TypeReference<>() {});
            Map<String, Set<String>> graph = buildGraph(countries);
            this.adjacencyGraph = Collections.unmodifiableMap(graph);

            log.info("Countries dataset loaded successfully: {} countries in adjacency graph", graph.size());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load countries dataset — application cannot start", e);
        }
    }

    private Map<String, Set<String>> buildGraph(List<Country> countries) {
        Map<String, Set<String>> graph = new HashMap<>(countries.size() * 2);
        for (Country country : countries) {
            if (country.cca3() == null || country.cca3().isBlank()) {
                continue;
            }
            String code = country.cca3().toUpperCase();
            Set<String> borders = (country.borders() == null || country.borders().isEmpty())
                    ? Set.of()
                    : country.borders().stream()
                            .filter(b -> b != null && !b.isBlank())
                            .map(String::toUpperCase)
                            .collect(Collectors.toUnmodifiableSet());
            graph.put(code, borders);
        }
        return graph;
    }

    public Map<String, Set<String>> getAdjacencyGraph() {
        if (adjacencyGraph == null) {
            throw new IllegalStateException("Adjacency graph has not been initialised yet");
        }
        return adjacencyGraph;
    }
}
