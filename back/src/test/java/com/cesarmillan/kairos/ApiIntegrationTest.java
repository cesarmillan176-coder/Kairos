package com.cesarmillan.kairos;

import com.cesarmillan.kairos.comment.CommentRepository;
import com.cesarmillan.kairos.show.ShowRepository;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final AtomicInteger SHOW_REQUESTS = new AtomicInteger();
    private static final String DATABASE = "kairos_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final String SHOW = readShow();
    private static final HttpServer TVMAZE = startTvMaze();
    private static volatile int upstreamStatus = 200;

    @LocalServerPort
    private int port;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> System.getProperty("test.mongodb.uri",
                "mongodb://localhost:27017/?serverSelectionTimeoutMS=3000"));
        registry.add("spring.mongodb.database", () -> DATABASE);
        registry.add("tvmaze.base-url", () -> "http://localhost:" + TVMAZE.getAddress().getPort());
    }

    @BeforeEach
    void reset() {
        showRepository.deleteAll();
        commentRepository.deleteAll();
        SHOW_REQUESTS.set(0);
        upstreamStatus = 200;
    }

    @AfterAll
    static void cleanUp(@Autowired MongoTemplate mongoTemplate) {
        TVMAZE.stop(0);
        mongoTemplate.getDb().drop();
    }

    @Test
    void cachesTheWholeShowAndKeepsCommentsFresh() throws Exception {
        var first = get("/show?show_id=1");
        assertThat(first.statusCode()).isEqualTo(200);
        var expected = JSON.readTree(SHOW).asObject();
        expected.putArray("comments");
        assertThat(json(first)).isEqualTo(expected);
        assertThat(showRepository.count()).isEqualTo(1);

        var created = post("""
                {"show_id": 1, "comment": "  Buena serie  ", "rating": 4.5}
                """);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(json(created).get("status").asString()).isEqualTo("created");

        upstreamStatus = 503;
        var cached = get("/show?show_id=1");
        assertThat(cached.statusCode()).isEqualTo(200);
        var cachedJson = json(cached).asObject();
        var comments = cachedJson.remove("comments");
        assertThat(cachedJson).isEqualTo(JSON.readTree(SHOW));
        assertThat(comments.size()).isEqualTo(1);
        assertThat(comments.get(0).get("comment").asString()).isEqualTo("Buena serie");
        assertThat(comments.get(0).get("rating").asDouble()).isEqualTo(4.5);
        assertThat(comments.get(0).propertyNames()).containsExactlyInAnyOrder("comment", "rating");
        assertThat(SHOW_REQUESTS.get()).isEqualTo(1);
        assertThat(showRepository.findById(1).orElseThrow().data()).doesNotContainKey("comments");
    }

    @Test
    void addsOnlyTheMatchingCommentsToEachSearchResult() throws Exception {
        assertThat(post("""
                {"show_id": 1, "comment": "Me gustó", "rating": 5}
                """).statusCode()).isEqualTo(201);

        var response = get("/search?search_query=girls");
        assertThat(response.statusCode()).isEqualTo(200);
        var results = json(response);
        assertThat(results.size()).isEqualTo(3);
        assertThat(results.get(0).propertyNames())
                .containsExactlyInAnyOrder("id", "name", "channel", "summary", "genres", "comments");
        assertThat(results.get(0).get("channel").asString()).isEqualTo("HBO");
        assertThat(results.get(0).get("comments").size()).isEqualTo(1);
        assertThat(results.get(1).get("channel").asString()).isEqualTo("Netflix");
        assertThat(results.get(1).get("comments").isEmpty()).isTrue();
        assertThat(results.get(2).get("channel").isNull()).isTrue();
        assertThat(results.get(2).get("summary").isNull()).isTrue();
        assertThat(results.get(2).get("genres").isEmpty()).isTrue();
    }

    @Test
    void returnsAnEmptyArrayWhenThereAreNoMatches() throws Exception {
        var response = get("/search?search_query=unknown");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json(response).isArray()).isTrue();
        assertThat(json(response).isEmpty()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "5", "2.5"})
    void acceptsRatingsWithinTheRange(String rating) throws Exception {
        var response = post("{\"show_id\":1,\"comment\":\"Buena serie\",\"rating\":" + rating + "}");
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(commentRepository.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"show_id\":1,\"comment\":\"Buena serie\"}",
            "{\"show_id\":1,\"comment\":\"Buena serie\",\"rating\":null}",
            "{\"show_id\":1,\"comment\":\"Buena serie\",\"rating\":-0.1}",
            "{\"show_id\":1,\"comment\":\"Buena serie\",\"rating\":5.1}",
            "{\"show_id\":1,\"comment\":\"   \",\"rating\":4}",
            "{\"show_id\":0,\"comment\":\"Buena serie\",\"rating\":4}",
            "{\"show_id\":1.5,\"comment\":\"Buena serie\",\"rating\":4}",
            "{\"comment\":\"Buena serie\",\"rating\":4}",
            "{\"show_id\":1,\"rating\":4}",
            "not-json"
    })
    void rejectsInvalidCommentsWithoutSavingThem(String body) throws Exception {
        var response = post(body);
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(commentRepository.count()).isZero();
        assertThat(showRepository.count()).isZero();
        assertThat(SHOW_REQUESTS.get()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/search", "/search?search_query=", "/search?search_query=%20%20",
            "/show", "/show?show_id=0", "/show?show_id=-1", "/show?show_id=abc"})
    void rejectsInvalidQueryParameters(String path) throws Exception {
        var response = get(path);
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(SHOW_REQUESTS.get()).isZero();
    }

    @Test
    void rejectsAnOversizedComment() throws Exception {
        var response = post("{\"show_id\":1,\"comment\":\"" + "a".repeat(2001) + "\",\"rating\":4}");
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(commentRepository.count()).isZero();
    }

    @Test
    void returns404AndDoesNotCacheMissingShows() throws Exception {
        upstreamStatus = 404;

        assertThat(get("/show?show_id=999").statusCode()).isEqualTo(404);
        assertThat(post("""
                {"show_id": 999, "comment": "Comentario", "rating": 4}
                """).statusCode()).isEqualTo(404);
        assertThat(showRepository.count()).isZero();
        assertThat(commentRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 500, 503})
    void returns502WhenTvMazeIsUnavailable(int status) throws Exception {
        upstreamStatus = status;

        assertThat(get("/show?show_id=1").statusCode()).isEqualTo(502);
        assertThat(get("/search?search_query=girls").statusCode()).isEqualTo(502);
        assertThat(showRepository.count()).isZero();
    }

    @Test
    void createsTheIndexUsedToReadComments() {
        var indexes = mongoTemplate.getCollection("comments").listIndexes();
        assertThat(indexes).anySatisfy(index -> assertThat(index.getString("name")).isEqualTo("show_comments"));
    }

    @Test
    void servesTheApiPrefixUsedByFirebaseHosting() throws Exception {
        assertThat(get("/api/search?search_query=girls").statusCode()).isEqualTo(200);
        assertThat(get("/api/show?show_id=1").statusCode()).isEqualTo(200);
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/comments"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"show_id":1,"comment":"Buena serie","rating":4}
                        """))
                .build();
        assertThat(httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(201);
        assertThat(json(get("/api/show?show_id=1")).get("comments").size()).isEqualTo(1);
    }

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10)).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/comments"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) {
        return JSON.readTree(response.body());
    }

    private static String readShow() {
        try (var input = ApiIntegrationTest.class.getResourceAsStream("/shows/girls.json")) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static HttpServer startTvMaze() {
        try {
            var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                String body;
                if (exchange.getRequestURI().getPath().startsWith("/shows/")) {
                    SHOW_REQUESTS.incrementAndGet();
                    body = SHOW;
                } else if ("q=unknown".equals(exchange.getRequestURI().getRawQuery())) {
                    body = "[]";
                } else {
                    body = "[{\"score\":1,\"show\":" + SHOW + "}," + """
                            {"show":{"id":2,"name":"Dark","network":null,"webChannel":{"name":"Netflix"},"genres":["Drama"]}},
                            {"show":{"id":3,"name":"Unknown","network":null,"webChannel":null,"summary":null,"genres":null}}]
                            """;
                }
                byte[] bytes = (upstreamStatus == 200 ? body : "{}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(upstreamStatus, bytes.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(bytes);
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
