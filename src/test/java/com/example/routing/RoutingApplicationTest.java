package com.example.routing;

import com.example.routing.service.CountryDataLoader;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Verifies that the Spring application context loads without errors.
 *
 * <p>{@code CountryDataLoader} is replaced with a Mockito mock so that the
 * {@code @PostConstruct} startup fetch of the countries dataset is suppressed —
 * no real HTTP call is made during testing.  Mockito's default answer returns
 * an empty {@link java.util.Map} for {@code getAdjacencyGraph()}, which is
 * sufficient for the context to start successfully.</p>
 */
@SpringBootTest
class RoutingApplicationTest {

    // Prevents CountryDataLoader.loadCountries() from firing during context startup.
    @MockBean
    CountryDataLoader countryDataLoader;

    @Test
    void contextLoads() {
        // If the application context starts without throwing, the test passes.
    }
}
