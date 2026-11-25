package com.mediatracker.models;

public abstract class Media implements Consumable, Redirectable {
    private String id;
    private String title;
    private String genre;
    private double rating;
    private String status;
    private String sourceLink;
    private int boundary;

    public Media(String id, String title, String genre, double rating, String status, String sourceLink) {
        this(id, title, genre, rating, status, sourceLink, 0);
    }

    public Media(String id, String title, String genre, double rating, String status, String sourceLink, int boundary) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.rating = rating;
        if (status != null && !status.isEmpty()) {
            this.status = status;
        } else {
            this.status = (this instanceof Book || this instanceof Manga || this instanceof Manhwa)
                    ? "Unread"
                    : "Unwatched";
        }
        this.sourceLink = sourceLink;
        this.boundary = Math.max(0, boundary);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSourceLink() { return sourceLink; }
    public void setSourceLink(String sourceLink) { this.sourceLink = sourceLink; }
    public int getBoundary() { return boundary; }
    public void setBoundary(int boundary) { this.boundary = Math.max(0, boundary); }

    // Abstract methods enforced on all subclasses
    public abstract String getMediaType();
    public abstract String getExtraInfo();
    public abstract int getCurrentProgress();
    public abstract void setCurrentProgress(int progress);
}