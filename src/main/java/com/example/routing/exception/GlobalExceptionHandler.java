package com.example.routing.exception;

import com.example.routing.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

/**
 * Centralised exception handling for all controller-layer exceptions.
 * Maps domain exceptions to structured HTTP responses with a consistent error body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RouteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRouteNotFound(
            RouteNotFoundException ex, HttpServletRequest request) {
        log.warn("Route not found: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(errorResponse(HttpStatus.BAD_REQUEST, "Route Not Found", ex.getMessage(), request));
    }

    @ExceptionHandler(InvalidCountryException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCountry(
            InvalidCountryException ex, HttpServletRequest request) {
        log.warn("Invalid country: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(errorResponse(HttpStatus.BAD_REQUEST, "Invalid Country", ex.getMessage(), request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type mismatch in path variable: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(errorResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                        "Invalid path variable: " + ex.getName(), request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error processing request to {}", request.getRequestURI(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal Server Error", "An unexpected error occurred", request));
    }

    private ErrorResponse errorResponse(HttpStatus status, String error, String message, HttpServletRequest request) {
        return new ErrorResponse(Instant.now(), status.value(), error, message, request.getRequestURI());
    }
}
