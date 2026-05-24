package com.example.routing.exception;

import com.example.routing.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Direct unit tests for {@link GlobalExceptionHandler}.
 * Handlers are invoked without a Spring context to keep tests fast and focused.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/routing/CZE/ITA");
    }

    // ── handleRouteNotFound ────────────────────────────────────────────────────

    @Test
    void handleRouteNotFound_returns400() {
        RouteNotFoundException ex = new RouteNotFoundException("USA", "FRA");

        ResponseEntity<ErrorResponse> response = handler.handleRouteNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Route Not Found");
        assertThat(response.getBody().message()).isEqualTo("No land route found between USA and FRA");
        assertThat(response.getBody().path()).isEqualTo("/routing/CZE/ITA");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    // ── handleInvalidCountry ───────────────────────────────────────────────────

    @Test
    void handleInvalidCountry_returns400() {
        InvalidCountryException ex = new InvalidCountryException("XYZ");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidCountry(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Invalid Country");
        assertThat(response.getBody().message()).contains("XYZ");
        assertThat(response.getBody().path()).isEqualTo("/routing/CZE/ITA");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    // ── handleTypeMismatch ─────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void handleTypeMismatch_returns400WithParameterName() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("origin");

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("Invalid path variable: origin");
        assertThat(response.getBody().path()).isEqualTo("/routing/CZE/ITA");
    }

    // ── handleGenericException ─────────────────────────────────────────────────

    @Test
    void handleGenericException_returns500() {
        Exception ex = new RuntimeException("Unexpected database failure");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().path()).isEqualTo("/routing/CZE/ITA");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void handleGenericException_doesNotExposeInternalMessage() {
        Exception ex = new RuntimeException("sensitive internal detail");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        // The internal exception message must NOT be leaked in the response body.
        assertThat(response.getBody().message())
                .doesNotContain("sensitive internal detail");
    }
}
