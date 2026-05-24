package com.example.routing.service;

import com.example.routing.exception.InvalidCountryException;
import com.example.routing.exception.RouteNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Bidirectional BFS routing algorithm.
 *
 * <p>A small synthetic graph is used instead of the real countries dataset to keep
 * tests fast, deterministic, and independent of external state.</p>
 *
 * <pre>
 * Test graph topology:
 *
 *   AAA ─── BBB ─── CCC
 *    |               |
 *   DDD ─── EEE ─── FFF
 *                    |
 *                   GGG (island — no borders)
 *
 *   HHH ─── III  (disconnected component)
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
class BidirectionalBfsRoutingServiceTest {

    @Mock
    private CountryDataLoader dataLoader;

    private BidirectionalBfsRoutingService service;

    /**
     * Builds a symmetric adjacency graph representing the topology above.
     */
    @BeforeEach
    void setUp() {
        Map<String, Set<String>> graph = Map.of(
                "AAA", Set.of("BBB", "DDD"),
                "BBB", Set.of("AAA", "CCC"),
                "CCC", Set.of("BBB", "FFF"),
                "DDD", Set.of("AAA", "EEE"),
                "EEE", Set.of("DDD", "FFF"),
                "FFF", Set.of("CCC", "EEE", "GGG"),
                "GGG", Set.of("FFF"),
                "HHH", Set.of("III"),
                "III", Set.of("HHH")
        );
        when(dataLoader.getAdjacencyGraph()).thenReturn(graph);
        service = new BidirectionalBfsRoutingService(dataLoader);
    }

    // ── Happy path ─────────────────────────────────────────────────────────────

    @Test
    void findRoute_sameCountry_returnsSingleElement() {
        List<String> route = service.findRoute("AAA", "AAA");

        assertThat(route).containsExactly("AAA");
    }

    @Test
    void findRoute_directBorder_returnsTwoElements() {
        List<String> route = service.findRoute("AAA", "BBB");

        assertThat(route).containsExactly("AAA", "BBB");
    }

    @Test
    void findRoute_twoHops() {
        List<String> route = service.findRoute("AAA", "CCC");

        // Shortest: AAA → BBB → CCC
        assertThat(route).hasSize(3);
        assertThat(route).first().isEqualTo("AAA");
        assertThat(route).last().isEqualTo("CCC");
    }

    @Test
    void findRoute_multiHop_findsShortestPath() {
        // AAA to FFF: two possible paths of length 3
        //  AAA → BBB → CCC → FFF
        //  AAA → DDD → EEE → FFF
        List<String> route = service.findRoute("AAA", "FFF");

        assertThat(route).hasSize(4);
        assertThat(route.get(0)).isEqualTo("AAA");
        assertThat(route.get(3)).isEqualTo("FFF");
    }

    @Test
    void findRoute_longPath_correctEndpoints() {
        List<String> route = service.findRoute("BBB", "GGG");

        assertThat(route.get(0)).isEqualTo("BBB");
        assertThat(route.get(route.size() - 1)).isEqualTo("GGG");
        // Shortest: BBB → CCC → FFF → GGG (3 hops)
        assertThat(route).hasSize(4);
    }

    @Test
    void findRoute_reverseDirection_sameLength() {
        List<String> fwd = service.findRoute("AAA", "GGG");
        List<String> rev = service.findRoute("GGG", "AAA");

        assertThat(fwd).hasSize(rev.size());
    }

    // ── Case normalisation ─────────────────────────────────────────────────────

    @Test
    void findRoute_lowercaseInput_normalised() {
        List<String> route = service.findRoute("aaa", "bbb");

        assertThat(route).containsExactly("AAA", "BBB");
    }

    @Test
    void findRoute_mixedCaseInput_normalised() {
        List<String> route = service.findRoute("Aaa", "Bbb");

        assertThat(route).containsExactly("AAA", "BBB");
    }

    // ── Error cases ────────────────────────────────────────────────────────────

    @Test
    void findRoute_invalidOrigin_throwsInvalidCountryException() {
        assertThatThrownBy(() -> service.findRoute("ZZZ", "AAA"))
                .isInstanceOf(InvalidCountryException.class)
                .hasMessageContaining("ZZZ");
    }

    @Test
    void findRoute_invalidDestination_throwsInvalidCountryException() {
        assertThatThrownBy(() -> service.findRoute("AAA", "ZZZ"))
                .isInstanceOf(InvalidCountryException.class)
                .hasMessageContaining("ZZZ");
    }

    @Test
    void findRoute_noLandRoute_disconnectedIsland_throwsRouteNotFoundException() {
        // AAA and HHH are in separate components
        assertThatThrownBy(() -> service.findRoute("AAA", "HHH"))
                .isInstanceOf(RouteNotFoundException.class)
                .hasMessageContaining("AAA")
                .hasMessageContaining("HHH");
    }

    @Test
    void findRoute_noLandRoute_toIsolatedIsland_throwsRouteNotFoundException() {
        // GGG connects to FFF, but HHH is a completely separate component
        assertThatThrownBy(() -> service.findRoute("GGG", "HHH"))
                .isInstanceOf(RouteNotFoundException.class);
    }

    // ── Route result properties ────────────────────────────────────────────────

    @Test
    void findRoute_resultIsImmutable() {
        List<String> route = service.findRoute("AAA", "BBB");

        assertThatThrownBy(() -> route.add("XXX"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void findRoute_allCodesUppercase() {
        List<String> route = service.findRoute("aaa", "fff");

        assertThat(route).allMatch(code -> code.equals(code.toUpperCase()));
    }

    @Test
    void findRoute_pathIsContiguous() {
        // Every consecutive pair of codes in the result must be direct neighbours
        Map<String, Set<String>> graph = dataLoader.getAdjacencyGraph();
        List<String> route = service.findRoute("AAA", "GGG");

        for (int i = 0; i < route.size() - 1; i++) {
            String current = route.get(i);
            String next = route.get(i + 1);
            assertThat(graph.get(current))
                    .as("Expected %s and %s to be neighbours", current, next)
                    .contains(next);
        }
    }

    // ── Negative cache ─────────────────────────────────────────────────────────

    @Test
    void findRoute_noRoute_secondCallThrowsWithoutRerunningBfs() {
        // Both calls should throw; second call should use negative cache (no additional BFS)
        assertThatThrownBy(() -> service.findRoute("AAA", "HHH"))
                .isInstanceOf(RouteNotFoundException.class);
        assertThatThrownBy(() -> service.findRoute("AAA", "HHH"))
                .isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    void findRoute_noRoute_forwardQueueExhaustesFirst_throwsRouteNotFoundException() {
        // Origin is in the SMALL component (HHH-III, 2 nodes) and destination is in the LARGE
        // component (AAA…GGG, 7 nodes).  The forward BFS queue will empty before a meeting is
        // found, exercising the !queueFwd.isEmpty() == false branch of the while-guard.
        assertThatThrownBy(() -> service.findRoute("HHH", "AAA"))
                .isInstanceOf(RouteNotFoundException.class)
                .hasMessageContaining("HHH")
                .hasMessageContaining("AAA");
    }
}
