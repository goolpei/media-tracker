package com.mediatracker.database;

import java.sql.Connection;
import java.sql.DriverManager;
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
                extra_info TEXT
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
            System.out.println("SQLite media and history tables verified.");
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }
}