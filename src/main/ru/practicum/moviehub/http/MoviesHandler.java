package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if (method.equalsIgnoreCase("GET")) {
            try {
                handleGet(ex);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (method.equalsIgnoreCase("POST")) {
            try {
                handlePost(ex);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (method.equalsIgnoreCase("DELETE")) {
            try {
                handleDelete(ex);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            sendJson(ex, 405, gson.toJson(new ErrorResponse("Некорректный метод", List.of())));
        }
    }

    private void handleGet(HttpExchange ex) throws Exception {
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();

        Integer year = null;
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("year=")) {
                    try {
                        year = Integer.parseInt(param.substring(5));
                    } catch (NumberFormatException e) {
                        sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный параметр year", List.of())));
                        return;
                    }
                    break;
                }
            }
        }

        List<Movie> movies = store.getAll();
        if (year != null) {
            final int yearValue = year;
            List<Movie> filtered = movies.stream()
                    .filter(m -> m.getYear() == yearValue)
                    .collect(Collectors.toList());
            sendJson(ex, 200, gson.toJson(filtered));
        } else if (path.equals("/movies")) {
            sendJson(ex, 200, gson.toJson(movies));
        } else {
            Integer id = getIdPath(ex);
            if (id != null) {
                Movie movie = store.getById(id);
                if (movie != null) {
                    sendJson(ex, 200, gson.toJson(movie));
                } else {
                    sendJson(ex, 404, gson.toJson(new ErrorResponse("Фильм не найден", List.of())));
                }
            } else {
                sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный id", List.of())));
            }
        }
    }

    private void handlePost(HttpExchange ex) throws Exception {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (!"application/json".equalsIgnoreCase(contentType)) {
            sendJson(ex, 415, gson.toJson(new ErrorResponse("Неверный Content-Type", List.of())));
            return;
        }

        Movie newMovie;
        try (InputStreamReader reader = new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8)) {
            newMovie = gson.fromJson(reader, Movie.class);
        }

        int currentYear = LocalDate.now().getYear();

        List<String> details = new ArrayList<>();
        String title = newMovie.getTitle();
        int year = newMovie.getYear();

        if (title == null || title.trim().isEmpty()) {
            details.add("Название не должно быть пустым");
        }
        if (title.length() > 100) {
            details.add("Название слишком длинное");
        }
        if (year < 1888) {
            details.add("Год должен быть не раньше 1888");
        }
        if (year > currentYear + 1) {
            details.add("Год должен быть не позже " + (currentYear + 1));
        }

        if (!details.isEmpty()) {
            sendJson(ex, 422, gson.toJson(new ErrorResponse("Ошибка валидации", details)));
            return;
        }

        Movie saved = store.add(newMovie);
        sendJson(ex, 201, gson.toJson(saved));
    }

    private void handleDelete(HttpExchange ex) throws Exception {
        Integer id = getIdPath(ex);
        if (id == null) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный id", List.of())));
            return;
        }
        Movie deleted = store.delete(id);
        if (deleted != null) {
            sendNoContent(ex);
        } else {
            sendJson(ex, 404, gson.toJson(new ErrorResponse("Фильм не найден", List.of())));
        }
    }

    public Integer getIdPath(HttpExchange ex) {
        String path = ex.getRequestURI().getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String[] parts = path.split("/");
        if (parts.length == 3 && parts[1].equals("movies")) {
            try {
                return Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}