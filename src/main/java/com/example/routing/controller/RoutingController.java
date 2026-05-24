package com.example.routing.controller;

import com.example.routing.model.ErrorResponse;
import com.example.routing.model.RouteResponse;
import com.example.routing.service.RoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing the land-route calculation endpoint.
 * Country codes are normalised to uppercase before reaching the service.
 */
@RestController
@RequestMapping("/routing")
@Tag(name = "Routing", description = "Land route calculation between countries")
public class RoutingController {

    private static final Logger log = LoggerFactory.getLogger(RoutingController.class);

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @Operation(
            summary = "Calculate land route",
            description = """
                    Returns the shortest land route between two countries identified by their
                    ISO 3166-1 alpha-3 (cca3) codes.
                    Country codes are case-insensitive.
                    Returns a single-element list when origin equals destination.
                    Returns HTTP 400 when no land route exists or when a country code is invalid.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Route found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RouteResponse.class),
                            examples = @ExampleObject(
                                    name = "CZE to ITA",
                                    value = """
                                            {"route": ["CZE", "AUT", "ITA"]}
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "No land route exists or invalid country code",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "No route",
                                            value = """
                                                    {
                                                      "timestamp": "2024-01-01T12:00:00Z",
                                                      "status": 400,
                                                      "error": "Route Not Found",
                                                      "message": "No land route found between USA and FRA",
                                                      "path": "/routing/USA/FRA"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Invalid country",
                                            value = """
                                                    {
                                                      "timestamp": "2024-01-01T12:00:00Z",
                                                      "status": 400,
                                                      "error": "Invalid Country",
                                                      "message": "Invalid or unknown country code: XYZ",
                                                      "path": "/routing/XYZ/ITA"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    @GetMapping("/{origin}/{destination}")
    public ResponseEntity<RouteResponse> getRoute(
            @Parameter(description = "ISO cca3 code of the origin country (e.g. CZE)", example = "CZE")
            @PathVariable String origin,
            @Parameter(description = "ISO cca3 code of the destination country (e.g. ITA)", example = "ITA")
            @PathVariable String destination) {

        log.info("Routing request received: {} -> {}", origin.toUpperCase(), destination.toUpperCase());
        long start = System.currentTimeMillis();

        List<String> route = routingService.findRoute( origin.toUpperCase(), destination.toUpperCase());

        log.info("Routing request fulfilled in {}ms: {}", System.currentTimeMillis() - start, route);
        return ResponseEntity.ok(new RouteResponse(route));
    }
}
