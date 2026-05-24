package com.example.routing.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class RouteBuilder {

    public static List<String> buildPath(
            String meetingPoint,
            Map<String, String> parentFromOrigin,
            Map<String, String> parentFromDest) {

        List<String> forwardSegment = buildForwardSegment(meetingPoint, parentFromOrigin);
        List<String> backwardSegment = buildBackwardSegment(meetingPoint, parentFromDest);

        List<String> fullPath = new ArrayList<>(forwardSegment.size() + backwardSegment.size());
        fullPath.addAll(forwardSegment);
        fullPath.addAll(backwardSegment);

        return Collections.unmodifiableList(fullPath);
    }

    /**
     * Walks the forward parent chain from meetingPoint back to origin, then reverses.
     * Result: [origin, …, meetingPoint]
     */
    private static List<String> buildForwardSegment(String meetingPoint, Map<String, String> parentFromOrigin) {
        List<String> segment = new ArrayList<>();
        String current = meetingPoint;
        while (current != null) {
            segment.add(current);
            current = parentFromOrigin.get(current);
        }
        Collections.reverse(segment);
        return segment;
    }

    /**
     * Walks the backward parent chain from meetingPoint's parent to destination.
     * Result: [parentOfMeetingOnDestSide, …, destination]
     */
    private static List<String> buildBackwardSegment(String meetingPoint, Map<String, String> parentFromDest) {
        List<String> segment = new ArrayList<>();
        String current = parentFromDest.get(meetingPoint);
        while (current != null) {
            segment.add(current);
            current = parentFromDest.get(current);
        }
        return segment;
    }
}
