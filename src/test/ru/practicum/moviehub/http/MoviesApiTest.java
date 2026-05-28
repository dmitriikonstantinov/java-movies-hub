package ru.practicum.moviehub.http;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080"; // !!! добавьте базовую часть URL
    private static MoviesServer server;
    private static HttpClient client;


    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer();
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        server.getStore().clear();

    }

    @AfterAll
    static void afterAll() {

        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies")) // !!! Добавьте правильный URI
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void postMovies_whenValid_returnsCreated() throws Exception {
        String json = "{\"title\":\"Inception\",\"year\":2010}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode());
        assertTrue(resp.body().contains("\"id\""));
        assertTrue(resp.body().contains("\"title\":\"Inception\""));
        assertTrue(resp.body().contains("\"year\":2010"));
    }

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        String postJson = "{\"title\":\"Inception\",\"year\":2010}";
        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(postJson, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> postResp = client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, postResp.statusCode());
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();
        HttpResponse<String> getResp = client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, getResp.statusCode());
        assertTrue(getResp.body().contains("\"id\":1"));
        assertTrue(getResp.body().contains("\"title\":\"Inception\""));
        assertTrue(getResp.body().contains("\"year\":2010"));
    }

    @Test
    void deleteMovie_byId() throws Exception {
        String postJson = "{\"title\":\"Inception\",\"year\":2010}";
        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(postJson, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> postResp = client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, postResp.statusCode());
        HttpRequest delReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();
        HttpResponse<String> delResp = client.send(delReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(204, delResp.statusCode());
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();
        HttpResponse<String> getResp = client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, getResp.statusCode());
    }

    @Test
    void getMovie_whenIncorrectId_return_400() throws Exception {
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .build();
        HttpResponse<String> getResp = client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, getResp.statusCode());
    }

    @Test
    void getMovie_notFound_return_404() throws Exception {
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/123"))
                .GET()
                .build();
        HttpResponse<String> getResp = client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, getResp.statusCode());
    }

    @Test
    void putMovies_return_405() throws Exception {
        HttpRequest putReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .PUT(HttpRequest.BodyPublishers.ofString("{}", StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> putResp = client.send(putReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(405, putResp.statusCode());
    }

    @Test
    void postMovie_wrongContentType_return_415() throws Exception {
        String strJson = "{\"title\":\"Inception\",\"year\":2010}";
        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(strJson, StandardCharsets.UTF_8))
                .header("Content-Type", "text/plain")
                .build();
        HttpResponse<String> postResp = client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(415, postResp.statusCode());
    }

    @Test
    void getMovie_withYearFilter_return_200() throws Exception {
        String strJson = "{\"title\":\"Inception\",\"year\":2010}";
        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(strJson, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> postResp = client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2010"))
                .GET()
                .build();
        HttpResponse<String> getResp = client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, getResp.statusCode());
    }

}