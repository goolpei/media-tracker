package com.mediatracker.database;

import com.mediatracker.models.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:media_tracker.db";

    public static void initializeDatabase() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS media (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                media_type TEXT NOT NULL,
                genre TEXT,
                rating REAL,
                status TEXT,
                source_link TEXT,
                extra_info TEXT
            );
            """;
            
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("SQLite schema verified and ready.");
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }

    // CREATE
    public static void addMedia(Media media) {
        String insertSQL = "INSERT INTO media(id, title, media_type, genre, rating, status, source_link, extra_info) VALUES(?,?,?,?,?,?,?,?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
             
            pstmt.setString(1, media.getId());
            pstmt.setString(2, media.getTitle());
            pstmt.setString(3, media.getMediaType());
            pstmt.setString(4, media.getGenre());
            pstmt.setDouble(5, media.getRating());
            pstmt.setString(6, media.getStatus());
            pstmt.setString(7, media.getSourceLink());
            pstmt.setString(8, media.getExtraInfo());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding media: " + e.getMessage());
        }
    }

    // READ
    public static List<Media> getAllMedia() {
        List<Media> mediaList = new ArrayList<>();
        String querySQL = "SELECT * FROM media";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(querySQL)) {
             
            while (rs.next()) {
                String id = rs.getString("id");
                String title = rs.getString("title");
                String type = rs.getString("media_type");
                String genre = rs.getString("genre");
                double rating = rs.getDouble("rating");
                String status = rs.getString("status");
                String sourceLink = rs.getString("source_link");
                
                // Parse the generic string back into an integer for the specific constructors
                int parsedExtra = 0;
                try {
                    parsedExtra = Integer.parseInt(rs.getString("extra_info"));
                } catch (NumberFormatException ignored) {}

                Media media = switch (type) {
                    case "Anime" -> new Anime(id, title, genre, rating, status, sourceLink, parsedExtra);
                    case "Movie" -> new Movie(id, title, genre, rating, status, sourceLink, parsedExtra);
                    case "Book" -> new Book(id, title, genre, rating, status, sourceLink, parsedExtra);
                    case "Manga" -> new Manga(id, title, genre, rating, status, sourceLink, parsedExtra);
                    case "Manhwa" -> new Manhwa(id, title, genre, rating, status, sourceLink, parsedExtra);
                    default -> null;
                };

                if (media != null) {
                    mediaList.add(media);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving media: " + e.getMessage());
        }
        return mediaList;
    }

    // UPDATE
    public static void updateMedia(Media media) {
        String updateSQL = "UPDATE media SET title = ?, genre = ?, rating = ?, status = ?, source_link = ?, extra_info = ? WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
             
            pstmt.setString(1, media.getTitle());
            pstmt.setString(2, media.getGenre());
            pstmt.setDouble(3, media.getRating());
            pstmt.setString(4, media.getStatus());
            pstmt.setString(5, media.getSourceLink());
            pstmt.setString(6, media.getExtraInfo());
            pstmt.setString(7, media.getId());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating media: " + e.getMessage());
        }
    }

    // DELETE
    public static void deleteMedia(String id) {
        String deleteSQL = "DELETE FROM media WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
             
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting media: " + e.getMessage());
        }
    }
}