package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
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
                            sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный параметр " +
                                    "year", null)));
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
            } else if (path.startsWith("/movies/")) {
                String[] parts = path.split("/");
                if (parts.length == 3) {
                    try {
                        int id = Integer.parseInt(parts[2]);
                        Movie movie = store.getById(id);
                        if (movie != null) {
                            sendJson(ex, 200, gson.toJson(movie));
                        } else {
                            sendJson(ex, 404, gson.toJson(new ErrorResponse("Фильм не " +
                                    "найден", null)));
                        }
                    } catch (NumberFormatException e) {
                        sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный id", null)));
                    }
                } else {
                    sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный путь", null)));
                }
            } else {
                sendJson(ex, 404, gson.toJson(new ErrorResponse("Не найдено", null)));
            }
        } else if (method.equalsIgnoreCase("POST")) {
            String contentType = ex.getRequestHeaders().getFirst("Content-Type");
            if (!"application/json".equals(contentType)) {
                sendJson(ex, 415, gson.toJson(new ErrorResponse("Неверный Content-Type", null)));
                return;
            }

            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Movie newMovie = gson.fromJson(body, Movie.class);
            int currentYear = LocalDate.now().getYear();

            List<String> details = new ArrayList<>();
            String title = newMovie.getTitle();
            int year = newMovie.getYear();

            if (title == null || title.trim().isEmpty()) {
                details.add("Название не должно быть пустым");
            }
            if (title != null && title.length() > 100) {
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
        } else if (method.equalsIgnoreCase("DELETE")) {
            String path = ex.getRequestURI().getPath();
            if (path.matches("/movies/\\d+")) {
                int id = Integer.parseInt(path.split("/")[2]);
                Movie deleted = store.delete(id);
                if (deleted != null) {
                    sendNoContent(ex);
                } else {
                    sendJson(ex, 404, gson.toJson(new ErrorResponse("Фильм не найден", null)));
                }
            } else {
                sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный путь", null)));
            }
        } else {
            sendJson(ex, 405, gson.toJson(new ErrorResponse("Некорректный метод", null)));
        }
    }
}