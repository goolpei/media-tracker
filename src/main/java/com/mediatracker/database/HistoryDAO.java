package com.mediatracker.database;

import com.mediatracker.models.HistoryLog;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HistoryDAO {

    public void insert(HistoryLog log) throws SQLException {
        String sql = "INSERT INTO history(media_id, media_title, media_type, action, logged_at) VALUES(?,?,?,?,?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, log.getMediaId());
            pstmt.setString(2, log.getMediaTitle());
            pstmt.setString(3, log.getMediaType());
            pstmt.setString(4, log.getAction());
            pstmt.setString(5, log.getLoggedAt());
            pstmt.executeUpdate();
        }
    }

    public List<HistoryLog> findAll() throws SQLException {
        List<HistoryLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM history ORDER BY log_id DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(new HistoryLog(
                    rs.getInt("log_id"),
                    rs.getString("media_id"),
                    rs.getString("media_title"),
                    rs.getString("media_type"),
                    rs.getString("action"),
                    rs.getString("logged_at")
                ));
            }
        }
        return logs;
    }

    public void clearAll() throws SQLException {
        String sql = "DELETE FROM history";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
}