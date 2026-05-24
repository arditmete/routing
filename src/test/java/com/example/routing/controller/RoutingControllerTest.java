package com.example.routing.controller;

import com.example.routing.exception.InvalidCountryException;
import com.example.routing.exception.RouteNotFoundException;
import com.example.routing.service.RoutingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-layer integration tests using MockMvc.
 * The {@link RoutingService} is mocked to isolate HTTP behaviour from business logic.
 */
@WebMvcTest(RoutingController.class)
class RoutingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoutingService routingService;


    @Test
    void getRoute_validRoute_returns200WithRouteArray() throws Exception {
        when(routingService.findRoute("CZE", "ITA")).thenReturn(List.of("CZE", "AUT", "ITA"));

        mockMvc.perform(get("/routing/CZE/ITA").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route").isArray())
                .andExpect(jsonPath("$.route[0]").value("CZE"))
                .andExpect(jsonPath("$.route[1]").value("AUT"))
                .andExpect(jsonPath("$.route[2]").value("ITA"));
    }

    @Test
    void getRoute_sameCountry_returns200WithSingleElement() throws Exception {
        when(routingService.findRoute("DEU", "DEU")).thenReturn(List.of("DEU"));

        mockMvc.perform(get("/routing/DEU/DEU").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route").isArray())
                .andExpect(jsonPath("$.route.length()").value(1))
                .andExpect(jsonPath("$.route[0]").value("DEU"));
    }

    @Test
    void getRoute_directBorder_returns200() throws Exception {
        when(routingService.findRoute("DEU", "AUT")).thenReturn(List.of("DEU", "AUT"));

        mockMvc.perform(get("/routing/DEU/AUT").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route.length()").value(2));
    }


    @Test
    void getRoute_lowercaseInput_normalisedToUppercase() throws Exception {
        when(routingService.findRoute("CZE", "ITA")).thenReturn(List.of("CZE", "AUT", "ITA"));

        // Lowercase codes are normalised by the controller before passing to the service
        mockMvc.perform(get("/routing/cze/ita").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route[0]").value("CZE"));
    }

    @Test
    void getRoute_mixedCaseInput_normalisedToUppercase() throws Exception {
        when(routingService.findRoute("CZE", "ITA")).thenReturn(List.of("CZE", "AUT", "ITA"));

        mockMvc.perform(get("/routing/Cze/Ita").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route[0]").value("CZE"));
    }


    @Test
    void getRoute_noLandRoute_returns400WithErrorBody() throws Exception {
        when(routingService.findRoute("USA", "FRA"))
                .thenThrow(new RouteNotFoundException("USA", "FRA"));

        mockMvc.perform(get("/routing/USA/FRA").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Route Not Found"))
                .andExpect(jsonPath("$.message").value("No land route found between USA and FRA"))
                .andExpect(jsonPath("$.path").value("/routing/USA/FRA"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }


    @Test
    void getRoute_invalidOrigin_returns400WithErrorBody() throws Exception {
        when(routingService.findRoute("XYZ", "ITA"))
                .thenThrow(new InvalidCountryException("XYZ"));

        mockMvc.perform(get("/routing/XYZ/ITA").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid Country"))
                .andExpect(jsonPath("$.message").value("Invalid or unknown country code: XYZ"));
    }

    @Test
    void getRoute_invalidDestination_returns400WithErrorBody() throws Exception {
        when(routingService.findRoute("CZE", "XYZ"))
                .thenThrow(new InvalidCountryException("XYZ"));

        mockMvc.perform(get("/routing/CZE/XYZ").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Country"));
    }


    @Test
    void getRoute_responseContentTypeIsJson() throws Exception {
        when(routingService.findRoute(anyString(), anyString())).thenReturn(List.of("A", "B"));

        mockMvc.perform(get("/routing/AAA/BBB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route").exists());
    }

    @Test
    void getRoute_errorResponse_hasAllRequiredFields() throws Exception {
        when(routingService.findRoute("USA", "AUS"))
                .thenThrow(new RouteNotFoundException("USA", "AUS"));

        mockMvc.perform(get("/routing/USA/AUS").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").exists());
    }

    @Test
    void getRoute_unexpectedException_returns500() throws Exception {
        when(routingService.findRoute("CZE", "ITA"))
                .thenThrow(new RuntimeException("Unexpected internal failure"));

        mockMvc.perform(get("/routing/CZE/ITA").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
