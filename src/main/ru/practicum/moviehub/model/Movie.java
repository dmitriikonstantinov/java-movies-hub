package ru.practicum.moviehub.model;

public class Movie {
private String title;
private int year;
private int id;

public Movie(int id, String title, int year) {
    this.id = id;
    this.title = title;
    this.year = year;
}

public Movie(String title, int year) {
    this.title = title;
    this.year = year;
}

    public int getYear() {
        return year;
    }

    public String getTitle() {
        return title;
    }

    public int getId() {
        return id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setId(int id) {
        this.id = id;
    }
}