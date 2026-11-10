package com.mediatracker.models;

public abstract class Media implements Consumable, Redirectable {
    private String id;
    private String title;
    private String genre;
    private double rating;
    private String status; 
    private String sourceLink;

    public Media(String id, String title, String genre, double rating, String status, String sourceLink) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.rating = rating;
        this.status = (status != null && !status.isEmpty()) ? status : "Unconsumed";
        this.sourceLink = sourceLink;
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

    // Abstract methods enforced on all subclasses
    public abstract String getMediaType();
    public abstract String getExtraInfo(); 
}