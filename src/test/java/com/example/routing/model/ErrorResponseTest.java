package com.example.routing.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    // ── Record accessors ───────────────────────────────────────────────────────

    @Test
    void accessors_returnAllFieldsPassedToConstructor() {
        Instant now = Instant.parse("2024-01-15T10:00:00Z");

        ErrorResponse response = new ErrorResponse(now, 400, "Bad Request", "some message", "/routing/X/Y");

        assertThat(response.timestamp()).isEqualTo(now);
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.error()).isEqualTo("Bad Request");
        assertThat(response.message()).isEqualTo("some message");
        assertThat(response.path()).isEqualTo("/routing/X/Y");
    }

    @Test
    void status500_preserved() {
        ErrorResponse response = new ErrorResponse(Instant.now(), 500,
                "Internal Server Error", "An unexpected error occurred", "/routing/A/B");

        assertThat(response.status()).isEqualTo(500);
        assertThat(response.error()).isEqualTo("Internal Server Error");
    }

    // ── equals / hashCode (record semantics) ──────────────────────────────────

    @Test
    void equalResponses_haveEqualHashCodes() {
        Instant ts = Instant.parse("2024-06-01T00:00:00Z");
        ErrorResponse a = new ErrorResponse(ts, 400, "Bad Request", "msg", "/path");
        ErrorResponse b = new ErrorResponse(ts, 400, "Bad Request", "msg", "/path");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void responsesWithDifferentStatus_areNotEqual() {
        Instant ts = Instant.parse("2024-06-01T00:00:00Z");
        ErrorResponse a = new ErrorResponse(ts, 400, "Bad Request", "msg", "/path");
        ErrorResponse b = new ErrorResponse(ts, 500, "Server Error", "msg", "/path");

        assertThat(a).isNotEqualTo(b);
    }

    // ── JSON serialization ─────────────────────────────────────────────────────

    @Test
    void serializesToJson_withAllFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Instant ts = Instant.parse("2024-01-15T10:00:00Z");

        ErrorResponse response = new ErrorResponse(ts, 400, "Route Not Found",
                "No land route found between USA and FRA", "/routing/USA/FRA");

        String json = mapper.writeValueAsString(response);

        assertThat(json)
                .contains("\"status\":400")
                .contains("\"error\":\"Route Not Found\"")
                .contains("\"message\":\"No land route found between USA and FRA\"")
                .contains("\"path\":\"/routing/USA/FRA\"");
    }
}
