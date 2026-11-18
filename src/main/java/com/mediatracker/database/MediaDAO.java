package com.mediatracker.database;

import com.mediatracker.models.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MediaDAO {

    public void insert(Media media) throws SQLException {
        String sql = "INSERT INTO media(id, title, media_type, genre, rating, status, source_link, extra_info) VALUES(?,?,?,?,?,?,?,?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, media.getId());
            pstmt.setString(2, media.getTitle());
            pstmt.setString(3, media.getMediaType());
            pstmt.setString(4, media.getGenre());
            pstmt.setDouble(5, media.getRating());
            pstmt.setString(6, media.getStatus());
            pstmt.setString(7, media.getSourceLink());
            pstmt.setString(8, media.getExtraInfo());
            pstmt.executeUpdate();
        }
    }

    public List<Media> findAll() throws SQLException {
        List<Media> mediaList = new ArrayList<>();
        String sql = "SELECT * FROM media ORDER BY title ASC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Media media = mapRowToMedia(rs);
                if (media != null) {
                    mediaList.add(media);
                }
            }
        }
        return mediaList;
    }

    public Media findById(String id) throws SQLException {
        String sql = "SELECT * FROM media WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToMedia(rs);
                }
            }
        }
        return null;
    }

    public void update(Media media) throws SQLException {
        String sql = "UPDATE media SET title = ?, media_type = ?, genre = ?, rating = ?, status = ?, source_link = ?, extra_info = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, media.getTitle());
            pstmt.setString(2, media.getMediaType());
            pstmt.setString(3, media.getGenre());
            pstmt.setDouble(4, media.getRating());
            pstmt.setString(5, media.getStatus());
            pstmt.setString(6, media.getSourceLink());
            pstmt.setString(7, media.getExtraInfo());
            pstmt.setString(8, media.getId());
            pstmt.executeUpdate();
        }
    }

    public void delete(String id) throws SQLException {
        String sql = "DELETE FROM media WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        }
    }

    private Media mapRowToMedia(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String title = rs.getString("title");
        String type = rs.getString("media_type");
        String genre = rs.getString("genre");
        double rating = rs.getDouble("rating");
        String status = rs.getString("status");
        String sourceLink = rs.getString("source_link");

        int parsedExtra = 0;
        try {
            parsedExtra = Integer.parseInt(rs.getString("extra_info"));
        } catch (NumberFormatException ignored) {}

        return switch (type) {
            case "Anime" -> new Anime(id, title, genre, rating, status, sourceLink, parsedExtra);
            case "Movie" -> new Movie(id, title, genre, rating, status, sourceLink, parsedExtra);
            case "Book" -> new Book(id, title, genre, rating, status, sourceLink, parsedExtra);
            case "Manga" -> new Manga(id, title, genre, rating, status, sourceLink, parsedExtra);
            case "Manhwa" -> new Manhwa(id, title, genre, rating, status, sourceLink, parsedExtra);
            default -> null;
        };
    }
}