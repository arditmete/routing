package com.example.routing.service;

import com.example.routing.exception.InvalidCountryException;
import com.example.routing.exception.RouteNotFoundException;
import com.example.routing.util.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BidirectionalBfsRoutingService implements RoutingService {

    private static final Logger log = LoggerFactory.getLogger(BidirectionalBfsRoutingService.class);

    private final Map<String, Set<String>> graph;

    private final Set<String> noRouteCache = ConcurrentHashMap.newKeySet();

    public BidirectionalBfsRoutingService(CountryDataLoader dataLoader) {
        this.graph = dataLoader.getAdjacencyGraph();
        log.info("BidirectionalBfsRoutingService initialised with {} countries", graph.size());
    }

    @Override
    @Cacheable(value = "routes", key = "#origin.toUpperCase() + '-' + #destination.toUpperCase()")
    public List<String> findRoute(String origin, String destination) {
        String normOrigin = origin.toUpperCase();
        String normDest = destination.toUpperCase();

        validateCountry(normOrigin);
        validateCountry(normDest);

        if (normOrigin.equals(normDest)) {
            log.debug("Same-country request: {}", normOrigin);
            return List.of(normOrigin);
        }

        String noRouteKey = normOrigin + "-" + normDest;
        if (noRouteCache.contains(noRouteKey)) {
            log.debug("Negative cache hit for route: {} -> {}", normOrigin, normDest);
            throw new RouteNotFoundException(normOrigin, normDest);
        }

        long startTime = System.currentTimeMillis();
        log.info("Calculating route: {} -> {}", normOrigin, normDest);

        Optional<List<String>> result = performBidirectionalBfs(normOrigin, normDest);

        log.info("BFS completed in {}ms for route {} -> {}",
                System.currentTimeMillis() - startTime, normOrigin, normDest);

        return result.orElseThrow(() -> {
            noRouteCache.add(noRouteKey);
            log.info("No land route found: {} -> {}", normOrigin, normDest);
            return new RouteNotFoundException(normOrigin, normDest);
        });
    }

    private Optional<List<String>> performBidirectionalBfs(String origin, String destination) {
        Map<String, String> parentFwd = new HashMap<>();
        Map<String, String> parentBwd = new HashMap<>();

        parentFwd.put(origin, null);
        parentBwd.put(destination, null);

        Deque<String> queueFwd = new ArrayDeque<>();
        Deque<String> queueBwd = new ArrayDeque<>();
        queueFwd.add(origin);
        queueBwd.add(destination);

        while (!queueFwd.isEmpty() && !queueBwd.isEmpty()) {
            if (queueFwd.size() <= queueBwd.size()) {
                List<String> newNodes = expandOneLevel(queueFwd, parentFwd);
                Optional<String> meeting = findOptimalMeetingPoint(newNodes, parentFwd, parentBwd);
                if (meeting.isPresent()) {
                    return Optional.of(RouteBuilder.buildPath(meeting.get(), parentFwd, parentBwd));
                }
            } else {
                List<String> newNodes = expandOneLevel(queueBwd, parentBwd);
                Optional<String> meeting = findOptimalMeetingPoint(newNodes, parentBwd, parentFwd);
                if (meeting.isPresent()) {
                    return Optional.of(RouteBuilder.buildPath(meeting.get(), parentFwd, parentBwd));
                }
            }
        }

        return Optional.empty();
    }

    private List<String> expandOneLevel(Deque<String> queue, Map<String, String> visited) {
        int levelSize = queue.size();
        List<String> newNodes = new ArrayList<>(levelSize * 4);
        for (int i = 0; i < levelSize; i++) {
            String current = queue.poll();
            for (String neighbour : graph.getOrDefault(current, Set.of())) {
                if (!visited.containsKey(neighbour)) {
                    visited.put(neighbour, current);
                    queue.add(neighbour);
                    newNodes.add(neighbour);
                }
            }
        }
        return newNodes;
    }

    private Optional<String> findOptimalMeetingPoint(
            List<String> candidates,
            Map<String, String> thisSideParent,
            Map<String, String> otherSideParent) {
        return candidates.stream()
                .filter(otherSideParent::containsKey)
                .min(Comparator.comparingInt(node ->
                        computeDepth(node, thisSideParent) + computeDepth(node, otherSideParent)));
    }

    private int computeDepth(String node, Map<String, String> parentMap) {
        int depth = 0;
        String current = parentMap.get(node);
        while (current != null) {
            depth++;
            current = parentMap.get(current);
        }
        return depth;
    }

    private void validateCountry(String code) {
        if (!graph.containsKey(code)) {
            throw new InvalidCountryException(code);
        }
    }
}
