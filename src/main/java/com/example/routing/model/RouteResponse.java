package com.example.routing.model;

import java.util.List;

/**
 * Successful route response returned by the routing endpoint.
 */
public record RouteResponse(List<String> route) {
}
