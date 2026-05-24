package com.example.routing.exception;

/**
 * Thrown when a country code is not found in the loaded dataset.
 */
public class InvalidCountryException extends RuntimeException {

    public InvalidCountryException(String countryCode) {
        super("Invalid or unknown country code: " + countryCode);
    }
}
