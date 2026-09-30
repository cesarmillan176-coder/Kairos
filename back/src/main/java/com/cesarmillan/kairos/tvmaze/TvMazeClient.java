package com.cesarmillan.kairos.tvmaze;

import com.cesarmillan.kairos.show.ShowNotFoundException;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class TvMazeClient {

    private final RestClient restClient;

    public TvMazeClient(RestClient tvMazeRestClient) {
        this.restClient = tvMazeRestClient;
    }

    public List<TvMazeSearchResult> search(String query) {
        try {
            var results = restClient.get()
                    .uri("/search/shows?q={query}", query)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<TvMazeSearchResult>>() {});

            if (results == null || results.stream().anyMatch(result ->
                    result == null || result.show() == null || result.show().id() == null)) {
                throw new TvMazeException("TVmaze devolvió una respuesta inválida");
            }
            return results;
        } catch (RestClientException exception) {
            throw new TvMazeException("No se pudo consultar TVmaze", exception);
        }
    }

    public Map<String, Object> getShow(Integer showId) {
        try {
            var show = restClient.get()
                    .uri("/shows/{id}", showId)
                    .retrieve()
                    .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND),
                            (request, response) -> {
                                throw new ShowNotFoundException(showId);
                            })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (show == null || !(show.get("id") instanceof Number id)
                    || id.intValue() != showId) {
                throw new TvMazeException("TVmaze devolvió una respuesta inválida");
            }
            return show;
        } catch (RestClientException exception) {
            throw new TvMazeException("No se pudo consultar TVmaze", exception);
        }
    }
}
