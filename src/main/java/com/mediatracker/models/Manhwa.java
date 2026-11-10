package com.mediatracker.models;

public class Manhwa extends Media {
    private int chapters;

    public Manhwa(String id, String title, String genre, double rating, String status, String sourceLink, int chapters) {
        super(id, title, genre, rating, status, sourceLink);
        this.chapters = chapters;
    }

    public int getChapters() { return chapters; }
    public void setChapters(int chapters) { this.chapters = chapters; }

    @Override
    public String getMediaType() { return "Manhwa"; }

    @Override
    public String getExtraInfo() { return String.valueOf(chapters); }

    @Override
    public void consume() { System.out.println("Reading Manhwa: " + getTitle()); }
}