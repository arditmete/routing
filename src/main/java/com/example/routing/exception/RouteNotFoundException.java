package com.example.routing.exception;

/**
 * Thrown when no land route exists between the requested origin and destination.
 */
public class RouteNotFoundException extends RuntimeException {

    public RouteNotFoundException(String origin, String destination) {
        super("No land route found between " + origin + " and " + destination);
    }
}
