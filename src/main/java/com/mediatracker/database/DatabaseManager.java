package com.mediatracker.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:media_tracker.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        String createMediaTable = """
            CREATE TABLE IF NOT EXISTS media (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                media_type TEXT NOT NULL,
                genre TEXT,
                rating REAL,
                status TEXT,
                source_link TEXT,
                extra_info TEXT,
                boundary INTEGER DEFAULT 0
            );
            """;

        String createHistoryTable = """
            CREATE TABLE IF NOT EXISTS history (
                log_id INTEGER PRIMARY KEY AUTOINCREMENT,
                media_id TEXT NOT NULL,
                media_title TEXT NOT NULL,
                media_type TEXT NOT NULL,
                action TEXT NOT NULL,
                logged_at TEXT NOT NULL
            );
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createMediaTable);
            stmt.execute(createHistoryTable);
            migrateBoundaryColumn(stmt);
            System.out.println("SQLite media and history tables verified.");
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }

    private static void migrateBoundaryColumn(Statement stmt) throws SQLException {
        boolean hasBoundaryColumn = false;
        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(media);")) {
            while (rs.next()) {
                if ("boundary".equalsIgnoreCase(rs.getString("name"))) {
                    hasBoundaryColumn = true;
                    break;
                }
            }
        }

        if (!hasBoundaryColumn) {
            stmt.execute("ALTER TABLE media ADD COLUMN boundary INTEGER DEFAULT 0;");
            stmt.execute("""
                UPDATE media
                SET boundary = CAST(extra_info AS INTEGER)
                WHERE CAST(extra_info AS INTEGER) > 0;
                """);
            stmt.execute("""
                UPDATE media
                SET extra_info = '0'
                WHERE LOWER(TRIM(status)) IN ('unwatched', 'unread', 'unconsumed')
                  AND boundary > 0;
                """);
            System.out.println("Migrated existing media records to progress/boundary schema.");
        }
    }
}