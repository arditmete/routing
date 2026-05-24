package com.example.routing.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RouteResponseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Record accessor ────────────────────────────────────────────────────────

    @Test
    void accessor_returnsRoute() {
        RouteResponse response = new RouteResponse(List.of("CZE", "AUT", "ITA"));

        assertThat(response.route()).containsExactly("CZE", "AUT", "ITA");
    }

    @Test
    void emptyRoute_isPreserved() {
        RouteResponse response = new RouteResponse(List.of());

        assertThat(response.route()).isEmpty();
    }

    @Test
    void singleElement_isPreserved() {
        RouteResponse response = new RouteResponse(List.of("DEU"));

        assertThat(response.route()).containsExactly("DEU");
    }

    // ── equals / hashCode (record semantics) ──────────────────────────────────

    @Test
    void equalResponses_haveEqualHashCodes() {
        RouteResponse a = new RouteResponse(List.of("CZE", "AUT", "ITA"));
        RouteResponse b = new RouteResponse(List.of("CZE", "AUT", "ITA"));

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void responsesWithDifferentRoutes_areNotEqual() {
        RouteResponse a = new RouteResponse(List.of("CZE", "AUT"));
        RouteResponse b = new RouteResponse(List.of("CZE", "ITA"));

        assertThat(a).isNotEqualTo(b);
    }

    // ── JSON serialization ─────────────────────────────────────────────────────

    @Test
    void serializesToJson_withRouteKey() throws Exception {
        RouteResponse response = new RouteResponse(List.of("CZE", "AUT", "ITA"));

        String json = MAPPER.writeValueAsString(response);

        assertThat(json).contains("\"route\"")
                .contains("\"CZE\"")
                .contains("\"AUT\"")
                .contains("\"ITA\"");
    }

    @Test
    void deserializesFromJson() throws Exception {
        String json = """
                {"route": ["CZE", "AUT", "ITA"]}
                """;

        RouteResponse response = MAPPER.readValue(json, RouteResponse.class);

        assertThat(response.route()).containsExactly("CZE", "AUT", "ITA");
    }
}
