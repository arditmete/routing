package com.example.routing.service;

import com.example.routing.model.Country;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CountryDataLoader}.
 * The HTTP client is mocked so no real network calls are made.
 */
@ExtendWith(MockitoExtension.class)
class CountryDataLoaderTest {

    @Mock
    private RestClient restClient;

    private static final String COUNTRIES_URL = "http://test-mock-url/countries.json";

    private static final String VALID_JSON = """
            [
              {"cca3": "AAA", "borders": ["BBB", "CCC"]},
              {"cca3": "BBB", "borders": ["AAA"]},
              {"cca3": "CCC", "borders": ["AAA"]},
              {"cca3": "DDI", "borders": []}
            ]
            """;

    // ── Successful load ────────────────────────────────────────────────────────

    @Test
    void loadCountries_buildsCorrectAdjacencyGraph() {
        stubRestClientToReturn(VALID_JSON);

        CountryDataLoader loader = new CountryDataLoader(restClient, new ObjectMapper(), COUNTRIES_URL);
        loader.loadCountries();

        Map<String, Set<String>> graph = loader.getAdjacencyGraph();

        assertThat(graph).containsKeys("AAA", "BBB", "CCC", "DDI");
        assertThat(graph.get("AAA")).containsExactlyInAnyOrder("BBB", "CCC");
        assertThat(graph.get("BBB")).containsExactlyInAnyOrder("AAA");
        assertThat(graph.get("CCC")).containsExactlyInAnyOrder("AAA");
        assertThat(graph.get("DDI")).isEmpty();
    }

    @Test
    void loadCountries_normalisesCodesUppercase() {
        String json = """
                [{"cca3": "abc", "borders": ["def"]}, {"cca3": "def", "borders": ["abc"]}]
                """;
        stubRestClientToReturn(json);

        CountryDataLoader loader = new CountryDataLoader(restClient, new ObjectMapper(), COUNTRIES_URL);
        loader.loadCountries();

        Map<String, Set<String>> graph = loader.getAdjacencyGraph();

        assertThat(graph).containsKeys("ABC", "DEF");
        assertThat(graph.get("ABC")).contains("DEF");
    }

    @Test
    void loadCountries_graphIsImmutable() {
        stubRestClientToReturn(VALID_JSON);

        CountryDataLoader loader = new CountryDataLoader(restClient, new ObjectMapper(), COUNTRIES_URL);
        loader.loadCountries();

        Map<String, Set<String>> graph = loader.getAdjacencyGraph();

        assertThatThrownBy(() -> graph.put("NEW", Set.of()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void loadCountries_countryWithNullBorders_treatedAsIsland() {
        String json = """
                [{"cca3": "ISL"}]
                """;
        stubRestClientToReturn(json);

        CountryDataLoader loader = new CountryDataLoader(restClient, new ObjectMapper(), COUNTRIES_URL);
        loader.loadCountries();

        assertThat(loader.getAdjacencyGraph().get("ISL")).isEmpty();
    }

    // ── Failure cases ──────────────────────────────────────────────────────────

    @Test
    void loadCountries_emptyResponse_throwsIllegalStateException() {
        stubRestClientToReturn("");

        CountryDataLoader loader = new CountryDataLoader(restClient, new ObjectMapper(), COUNTRIES_URL);

        assertThatThrownBy(loader::loadCountries)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void loadCountries_invalidJson_throwsIllegalStateException() {
        stubRestClientToReturn("NOT_VALID_JSON");

        CountryDataLoader loader = new CountryDataLoader(restClient, new ObjectMapper(), COUNTRIES_URL);

        assertThatThrownBy(loader::loadCountries)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void loadCountries_networkError_throwsIllegalStateException() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();
        doReturn(null).when(responseSpec).body(String.class);
        when(responseSpec.body(String.class))
                .thenThrow(new org.springframework.web.client.ResourceAccessException("Connection refused"));

        CountryDataLoader loader = new CountryDataLoader(restClient, new ObjectMapper(), COUNTRIES_URL);

        assertThatThrownBy(loader::loadCountries)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to load countries dataset");
    }

    @Test
    void getAdjacencyGraph_beforeLoad_throwsIllegalStateException() {
        CountryDataLoader loader = new CountryDataLoader(restClient, new ObjectMapper(), COUNTRIES_URL);

        assertThatThrownBy(loader::getAdjacencyGraph)
                .isInstanceOf(IllegalStateException.class);
    }

    // ── Null / blank cca3 entries ──────────────────────────────────────────────

    @Test
    void loadCountries_entryWithNullCca3_isSkipped() {
        // Jackson will deserialise cca3 as null when the field is absent.
        String json = """
                [
                  {"borders": ["AAA"]},
                  {"cca3": "AAA", "borders": []}
                ]
                """;
        stubRestClientToReturn(json);

        CountryDataLoader loader = new CountryDataLoader(restClient, new ObjectMapper(), COUNTRIES_URL);
        loader.loadCountries();

        // Only the valid entry must appear; the null-cca3 entry must be silently dropped.
        assertThat(loader.getAdjacencyGraph()).containsOnlyKeys("AAA");
    }

    @Test
    void loadCountries_entryWithBlankCca3_isSkipped() {
        String json = """
                [
                  {"cca3": "   ", "borders": []},
                  {"cca3": "AAA", "borders": []}
                ]
                """;
        stubRestClientToReturn(json);

        CountryDataLoader loader = new CountryDataLoader(restClient, new ObjectMapper(), COUNTRIES_URL);
        loader.loadCountries();

        assertThat(loader.getAdjacencyGraph()).containsOnlyKeys("AAA");
    }

    @Test
    void loadCountries_bordersContainingNullOrBlankEntries_filteredOut() {
        // The raw dataset may (rarely) contain malformed border entries; they must be silently dropped.
        String json = """
                [{"cca3": "AAA", "borders": [null, "  ", "BBB"]}]
                """;
        stubRestClientToReturn(json);

        CountryDataLoader loader = new CountryDataLoader(restClient, new ObjectMapper(), COUNTRIES_URL);
        loader.loadCountries();

        assertThat(loader.getAdjacencyGraph().get("AAA")).containsExactly("BBB");
    }

    @Test
    void loadCountries_nullJsonBody_throwsIllegalStateException() {
        stubRestClientToReturn(null);

        CountryDataLoader loader = new CountryDataLoader(restClient, new ObjectMapper(), COUNTRIES_URL);

        assertThatThrownBy(loader::loadCountries)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty response");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubRestClientToReturn(String body) {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();
        doReturn(body).when(responseSpec).body(String.class);
    }
}
