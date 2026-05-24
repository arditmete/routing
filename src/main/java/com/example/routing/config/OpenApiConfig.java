package com.example.routing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration.
 * Swagger UI is available at {@code /swagger-ui.html}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Country Land Route API")
                        .description("""
                                REST API that calculates the shortest possible land route between two countries.
                                Countries are identified using their ISO 3166-1 alpha-3 (cca3) codes.
                                The routing algorithm uses Bidirectional BFS over an in-memory adjacency graph
                                built from the mledoze/countries dataset.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Senior Java Engineer Assignment")
                                .url("https://github.com/mledoze/countries")));
    }
}
