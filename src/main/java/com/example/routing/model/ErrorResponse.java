package com.example.routing.model;

import java.time.Instant;

/**
 * Uniform error response body returned on 4xx/5xx responses.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path) {
}
