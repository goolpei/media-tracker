package com.mediatracker;

import com.formdev.flatlaf.FlatDarkLaf;
import com.mediatracker.database.DatabaseManager;
import com.mediatracker.ui.MainFrame;

import javax.swing.*;

public class Launcher {
    public static void main(String[] args) {
        // 1. Apply modern dark theme before creating any Swing components
        try {
            FlatDarkLaf.setup();
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf: " + e.getMessage());
        }

        // 2. Verify SQLite database and tables
        DatabaseManager.initializeDatabase();

        // 3. Launch the Swing UI safely on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}