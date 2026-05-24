package com.example.routing.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CountryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Record accessors ───────────────────────────────────────────────────────

    @Test
    void accessors_returnValuesPassedToConstructor() {
        Country country = new Country("DEU", List.of("AUT", "FRA", "CHE"));

        assertThat(country.cca3()).isEqualTo("DEU");
        assertThat(country.borders()).containsExactly("AUT", "FRA", "CHE");
    }

    @Test
    void nullBorders_arePreserved() {
        Country country = new Country("ISL", null);

        assertThat(country.cca3()).isEqualTo("ISL");
        assertThat(country.borders()).isNull();
    }

    @Test
    void emptyBorders_arePreserved() {
        Country country = new Country("AUS", List.of());

        assertThat(country.borders()).isEmpty();
    }

    // ── equals / hashCode / toString (record semantics) ───────────────────────

    @Test
    void equalCountries_haveEqualHashCodes() {
        Country a = new Country("DEU", List.of("AUT"));
        Country b = new Country("DEU", List.of("AUT"));

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void countriesWithDifferentCodes_areNotEqual() {
        Country a = new Country("DEU", List.of("AUT"));
        Country b = new Country("FRA", List.of("AUT"));

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void toString_containsCodeAndBorders() {
        Country country = new Country("DEU", List.of("AUT"));

        assertThat(country.toString()).contains("DEU").contains("AUT");
    }

    // ── JSON deserialization ───────────────────────────────────────────────────

    @Test
    void deserializesFromJson_withFullFields() throws Exception {
        String json = """
                {"cca3": "DEU", "borders": ["AUT", "FRA"]}
                """;

        Country country = MAPPER.readValue(json, Country.class);

        assertThat(country.cca3()).isEqualTo("DEU");
        assertThat(country.borders()).containsExactly("AUT", "FRA");
    }

    @Test
    void deserializesFromJson_withNullBorders() throws Exception {
        String json = """
                {"cca3": "ISL", "borders": null}
                """;

        Country country = MAPPER.readValue(json, Country.class);

        assertThat(country.cca3()).isEqualTo("ISL");
        assertThat(country.borders()).isNull();
    }

    @Test
    void deserializesFromJson_withMissingBorders_treatsAsNull() throws Exception {
        String json = """
                {"cca3": "ISL"}
                """;

        Country country = MAPPER.readValue(json, Country.class);

        assertThat(country.cca3()).isEqualTo("ISL");
        assertThat(country.borders()).isNull();
    }

    @Test
    void jsonIgnoreProperties_silentlyIgnoresUnknownFields() throws Exception {
        // The real countries.json contains dozens of extra fields; they must all be ignored.
        String json = """
                {
                  "cca3": "DEU",
                  "borders": ["AUT"],
                  "name": {"common": "Germany"},
                  "region": "Europe",
                  "population": 83000000,
                  "landlocked": false,
                  "flag": "🇩🇪"
                }
                """;

        Country country = MAPPER.readValue(json, Country.class);

        assertThat(country.cca3()).isEqualTo("DEU");
        assertThat(country.borders()).containsExactly("AUT");
    }
}
