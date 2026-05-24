package com.example.routing.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RouteBuilderTest {

    @Test
    void buildPath_directRoute() {
        // A → B  (meeting at B, forward found B)
        Map<String, String> fwd = new HashMap<>();
        fwd.put("A", null);
        fwd.put("B", "A");

        Map<String, String> bwd = new HashMap<>();
        bwd.put("B", null);   // B == destination

        List<String> path = RouteBuilder.buildPath("B", fwd, bwd);

        assertThat(path).containsExactly("A", "B");
    }

    @Test
    void buildPath_threeHopRoute() {
        // A → B → C → D,  meeting at C
        Map<String, String> fwd = new HashMap<>();
        fwd.put("A", null);
        fwd.put("B", "A");
        fwd.put("C", "B");

        Map<String, String> bwd = new HashMap<>();
        bwd.put("D", null);
        bwd.put("C", "D");

        List<String> path = RouteBuilder.buildPath("C", fwd, bwd);

        assertThat(path).containsExactly("A", "B", "C", "D");
    }

    @Test
    void buildPath_meetingAtMidpoint() {
        // Forward: A → B → M, Backward: E → D → M
        Map<String, String> fwd = new HashMap<>();
        fwd.put("A", null);
        fwd.put("B", "A");
        fwd.put("M", "B");

        Map<String, String> bwd = new HashMap<>();
        bwd.put("E", null);
        bwd.put("D", "E");
        bwd.put("M", "D");

        List<String> path = RouteBuilder.buildPath("M", fwd, bwd);

        assertThat(path).containsExactly("A", "B", "M", "D", "E");
    }

    @Test
    void buildPath_meetingPointIsOrigin_backwardSegmentOnly() {
        // Forward BFS reached origin immediately (origin == meeting point),
        // backward segment spans the rest: O ← X ← Z (destination Z)
        Map<String, String> fwd = new HashMap<>();
        fwd.put("O", null);  // O is origin; no forward parent

        Map<String, String> bwd = new HashMap<>();
        bwd.put("Z", null);
        bwd.put("X", "Z");
        bwd.put("O", "X");  // backward search reached O from X

        List<String> path = RouteBuilder.buildPath("O", fwd, bwd);

        assertThat(path).containsExactly("O", "X", "Z");
    }

    @Test
    void buildPath_resultIsImmutable() {
        Map<String, String> fwd = new HashMap<>();
        fwd.put("A", null);
        fwd.put("B", "A");

        Map<String, String> bwd = new HashMap<>();
        bwd.put("B", null);

        List<String> path = RouteBuilder.buildPath("B", fwd, bwd);

        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> path.add("X"));
    }

    @Test
    void constructor_canBeInvokedViaReflection() throws Exception {
        Constructor<RouteBuilder> constructor = RouteBuilder.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThat(constructor.newInstance()).isNotNull();
    }
}

