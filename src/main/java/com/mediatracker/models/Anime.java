package com.mediatracker.models;

public class Anime extends Media {
    private int episodes;

    public Anime(String id, String title, String genre, double rating, String status, String sourceLink, int episodes) {
        this(id, title, genre, rating, status, sourceLink, episodes, 0);
    }

    public Anime(String id, String title, String genre, double rating, String status, String sourceLink, int episodes, int boundary) {
        super(id, title, genre, rating, status, sourceLink, boundary);
        this.episodes = episodes;
    }

    public int getEpisodes() { return episodes; }
    public void setEpisodes(int episodes) { this.episodes = episodes; }

    @Override
    public int getCurrentProgress() { return episodes; }

    @Override
    public void setCurrentProgress(int progress) { this.episodes = Math.max(0, progress); }

    @Override
    public String getMediaType() { return "Anime"; }

    @Override
    public String getExtraInfo() { return String.valueOf(episodes); }

    @Override
    public void consume() { System.out.println("Watching Anime: " + getTitle()); }
}