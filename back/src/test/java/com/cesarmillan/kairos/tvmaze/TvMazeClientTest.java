package com.cesarmillan.kairos.tvmaze;

import com.cesarmillan.kairos.show.ShowNotFoundException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TvMazeClientTest {

    private MockRestServiceServer server;
    private TvMazeClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://api.tvmaze.com");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TvMazeClient(builder.build());
    }

    @Test
    void encodesSearchAndReadsNetwork() {
        server.expect(requestTo("https://api.tvmaze.com/search/shows?q=law%20%26%20order"))
                .andRespond(withSuccess("""
                        [{"score": 1, "show": {"id": 1, "name": "Law & Order",
                        "network": {"id": 2, "name": "NBC"}, "genres": ["Drama"]}}]
                        """, MediaType.APPLICATION_JSON));

        var show = client.search("law & order").getFirst().show();

        assertThat(show.name()).isEqualTo("Law & Order");
        assertThat(show.channel()).isEqualTo("NBC");
        server.verify();
    }

    @Test
    void usesWebChannelWhenNetworkIsNull() {
        server.expect(requestTo("https://api.tvmaze.com/search/shows?q=dark"))
                .andRespond(withSuccess("""
                        [{"show": {"id": 2, "name": "Dark", "network": null,
                        "webChannel": {"name": "Netflix"}, "genres": []}}]
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.search("dark").getFirst().show().channel()).isEqualTo("Netflix");
    }

    @Test
    void preservesTheFullShowIncludingUnknownFieldsAndNulls() {
        server.expect(requestTo("https://api.tvmaze.com/shows/1"))
                .andRespond(withSuccess("""
                        {"id": 1, "name": "Girls", "summary": null,
                        "custom": {"value": 10}, "_links": {"self": {"href": "https://api.tvmaze.com/shows/1"}}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.getShow(1)).containsKeys("id", "name", "summary", "custom", "_links")
                .containsEntry("summary", null);
    }

    @Test
    void returnsAnEmptyListWhenThereAreNoMatches() {
        server.expect(requestTo("https://api.tvmaze.com/search/shows?q=unknown"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.search("unknown")).isEmpty();
    }

    @Test
    void reportsMissingShows() {
        server.expect(requestTo("https://api.tvmaze.com/shows/999"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getShow(999)).isInstanceOf(ShowNotFoundException.class);
    }

    @Test
    void handlesUpstreamFailures() {
        server.expect(requestTo("https://api.tvmaze.com/shows/1"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.getShow(1)).isInstanceOf(TvMazeException.class);
    }

    @Test
    void handlesConnectionFailures() {
        server.expect(requestTo("https://api.tvmaze.com/shows/1"))
                .andRespond(withException(new IOException("Connection closed")));

        assertThatThrownBy(() -> client.getShow(1)).isInstanceOf(TvMazeException.class);
    }

    @Test
    void rejectsMalformedJson() {
        server.expect(requestTo("https://api.tvmaze.com/shows/1"))
                .andRespond(withSuccess("broken", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getShow(1)).isInstanceOf(TvMazeException.class);
    }

    @Test
    void rejectsAnEmptyResponse() {
        server.expect(requestTo("https://api.tvmaze.com/shows/1"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThatThrownBy(() -> client.getShow(1)).isInstanceOf(TvMazeException.class);
    }
}
