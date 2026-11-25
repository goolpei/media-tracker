package com.mediatracker.models;

public class Book extends Media {
    private int pages;

    public Book(String id, String title, String genre, double rating, String status, String sourceLink, int pages) {
        this(id, title, genre, rating, status, sourceLink, pages, 0);
    }

    public Book(String id, String title, String genre, double rating, String status, String sourceLink, int pages, int boundary) {
        super(id, title, genre, rating, status, sourceLink, boundary);
        this.pages = pages;
    }

    public int getPages() { return pages; }
    public void setPages(int pages) { this.pages = pages; }

    @Override
    public int getCurrentProgress() { return pages; }

    @Override
    public void setCurrentProgress(int progress) { this.pages = Math.max(0, progress); }

    @Override
    public String getMediaType() { return "Book"; }

    @Override
    public String getExtraInfo() { return String.valueOf(pages); }

    @Override
    public void consume() { System.out.println("Reading Book: " + getTitle()); }
}