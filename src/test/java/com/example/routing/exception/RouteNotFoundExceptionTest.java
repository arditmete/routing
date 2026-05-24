package com.example.routing.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouteNotFoundExceptionTest {

    @Test
    void constructor_setsMessageWithOriginAndDestination() {
        RouteNotFoundException ex = new RouteNotFoundException("USA", "FRA");

        assertThat(ex.getMessage()).isEqualTo("No land route found between USA and FRA");
    }

    @Test
    void isRuntimeException() {
        assertThat(new RouteNotFoundException("A", "B")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void messageContainsBothCodes() {
        RouteNotFoundException ex = new RouteNotFoundException("CZE", "AUS");

        assertThat(ex.getMessage()).contains("CZE").contains("AUS");
    }
}
