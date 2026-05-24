package com.example.routing.service;

import com.example.routing.exception.InvalidCountryException;
import com.example.routing.exception.RouteNotFoundException;

import java.util.List;

public interface RoutingService {
    List<String> findRoute(String origin, String destination);
}
