package com.mediatracker.models;

public class Manga extends Media {
    private int chapters;

    public Manga(String id, String title, String genre, double rating, String status, String sourceLink, int chapters) {
        this(id, title, genre, rating, status, sourceLink, chapters, 0);
    }

    public Manga(String id, String title, String genre, double rating, String status, String sourceLink, int chapters, int boundary) {
        super(id, title, genre, rating, status, sourceLink, boundary);
        this.chapters = chapters;
    }

    public int getChapters() { return chapters; }
    public void setChapters(int chapters) { this.chapters = chapters; }

    @Override
    public int getCurrentProgress() { return chapters; }

    @Override
    public void setCurrentProgress(int progress) { this.chapters = Math.max(0, progress); }

    @Override
    public String getMediaType() { return "Manga"; }

    @Override
    public String getExtraInfo() { return String.valueOf(chapters); }

    @Override
    public void consume() { System.out.println("Reading Manga: " + getTitle()); }
}