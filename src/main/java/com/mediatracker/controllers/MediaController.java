package com.mediatracker.controllers;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatracker.database.DatabaseManager;
import com.mediatracker.models.Anime;
import com.mediatracker.models.Book;
import com.mediatracker.models.Manga;
import com.mediatracker.models.Manhwa;
import com.mediatracker.models.Media;
import com.mediatracker.models.Movie;

import io.javalin.http.Context;

public class MediaController {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // GET: Fetch all media records
    public static void getAllMedia(Context ctx) {
        List<Media> mediaList = DatabaseManager.getAllMedia();
        ctx.json(mediaList);
    }

    // POST: Add new media (dynamically decides subclass based on mediaType)
    public static void addMedia(Context ctx) {
        try {
            JsonNode node = objectMapper.readTree(ctx.body());
            
            String id = node.get("id").asText();
            String title = node.get("title").asText();
            String mediaType = node.get("mediaType").asText();
            String genre = node.get("genre").asText();
            double rating = node.get("rating").asDouble();
            String status = node.get("status").asText();
            String sourceLink = node.get("sourceLink").asText();
            int extraInfo = node.get("extraInfo").asInt(); // Episodes, pages, chapters, or minutes

            Media newMedia = switch (mediaType) {
                case "Anime" -> new Anime(id, title, genre, rating, status, sourceLink, extraInfo);
                case "Movie" -> new Movie(id, title, genre, rating, status, sourceLink, extraInfo);
                case "Book" -> new Book(id, title, genre, rating, status, sourceLink, extraInfo);
                case "Manga" -> new Manga(id, title, genre, rating, status, sourceLink, extraInfo);
                case "Manhwa" -> new Manhwa(id, title, genre, rating, status, sourceLink, extraInfo);
                default -> null;
            };

            if (newMedia != null) {
                DatabaseManager.addMedia(newMedia);
                ctx.status(201).json(node);
            } else {
                ctx.status(400).result("Invalid media type specified.");
            }
        } catch (Exception e) {
            ctx.status(400).result("Error parsing request: " + e.getMessage());
        }
    }

    // PUT: Update an existing record
    public static void updateMedia(Context ctx) {
        try {
            JsonNode node = objectMapper.readTree(ctx.body());
            
            String id = node.get("id").asText();
            String title = node.get("title").asText();
            String mediaType = node.get("mediaType").asText();
            String genre = node.get("genre").asText();
            double rating = node.get("rating").asDouble();
            String status = node.get("status").asText();
            String sourceLink = node.get("sourceLink").asText();
            int extraInfo = node.get("extraInfo").asInt();

            Media updatedMedia = switch (mediaType) {
                case "Anime" -> new Anime(id, title, genre, rating, status, sourceLink, extraInfo);
                case "Movie" -> new Movie(id, title, genre, rating, status, sourceLink, extraInfo);
                case "Book" -> new Book(id, title, genre, rating, status, sourceLink, extraInfo);
                case "Manga" -> new Manga(id, title, genre, rating, status, sourceLink, extraInfo);
                case "Manhwa" -> new Manhwa(id, title, genre, rating, status, sourceLink, extraInfo);
                default -> null;
            };

            if (updatedMedia != null) {
                DatabaseManager.updateMedia(updatedMedia);
                ctx.result("Media updated successfully.");
            } else {
                ctx.status(400).result("Invalid media type specified.");
            }
        } catch (Exception e) {
            ctx.status(400).result("Error updating record: " + e.getMessage());
        }
    }

    // DELETE: Remove media by ID
    public static void deleteMedia(Context ctx) {
        String id = ctx.pathParam("id");
        DatabaseManager.deleteMedia(id);
        ctx.result("Media with ID " + id + " deleted successfully.");
    }

    // POST: Trigger the Redirectable interface to open external source links
    public static void openSourceLink(Context ctx) {
        String id = ctx.pathParam("id");
        List<Media> mediaList = DatabaseManager.getAllMedia();
        
        for (Media m : mediaList) {
            if (m.getId().equals(id)) {
                m.openSource(m.getSourceLink());
                ctx.result("Triggered browser redirect for: " + m.getTitle());
                return;
            }
        }
        ctx.status(404).result("Media ID not found.");
    }
}