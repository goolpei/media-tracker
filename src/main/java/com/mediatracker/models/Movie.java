package com.mediatracker.models;


public class Movie extends Media {
    private int durationMinutes;

    public Movie(String id, String title, String genre, double rating, String status, String sourceLink, int durationMinutes) {
        super(id, title, genre, rating, status, sourceLink);
        this.durationMinutes = durationMinutes;
    }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    @Override
    public String getMediaType() { return "Movie"; }

    @Override
    public String getExtraInfo() { return String.valueOf(durationMinutes); }

    @Override
    public void consume() { System.out.println("Watching Movie: " + getTitle()); }
}