package com.example.routing.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvalidCountryExceptionTest {

    @Test
    void constructor_setsMessageWithCountryCode() {
        InvalidCountryException ex = new InvalidCountryException("XYZ");

        assertThat(ex.getMessage()).isEqualTo("Invalid or unknown country code: XYZ");
    }

    @Test
    void isRuntimeException() {
        assertThat(new InvalidCountryException("ABC")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void messageContainsSuppliedCode() {
        String code = "ZZZ";
        InvalidCountryException ex = new InvalidCountryException(code);

        assertThat(ex.getMessage()).contains(code);
    }
}
