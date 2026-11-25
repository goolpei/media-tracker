package com.mediatracker.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.mediatracker.models.HistoryLog;
import com.mediatracker.models.Media;
import com.mediatracker.services.MediaService;
import com.mediatracker.ui.components.HeaderPanel;
import com.mediatracker.ui.components.HistoryPanel;
import com.mediatracker.ui.components.MediaTablePanel;
import com.mediatracker.ui.components.SidebarPanel;
import com.mediatracker.ui.components.StatsPanel;
import com.mediatracker.ui.dialogs.MediaFormDialog;

public class MainFrame extends JFrame {
    private final MediaService mediaService;
    private final SidebarPanel sidebarPanel;
    private final JPanel workspacePanel;
    private final StatsPanel statsPanel;
    private final HeaderPanel headerPanel;
    private final MediaTablePanel mediaTablePanel;
    private final HistoryPanel historyPanel;
    private final CardLayout centerCardLayout;
    private final JPanel centerCardContainer;

    private String currentCategory = "All";
    private boolean isDarkTheme = true;

    public MainFrame() {
        this.mediaService = new MediaService();

        setTitle("Media Tracker");
        setSize(1140, 680);
        setMinimumSize(new Dimension(940, 560));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 1. Sidebar Navigation
        sidebarPanel = new SidebarPanel(
            this::handleCategoryChange,
            this::showHistoryView
        );
        add(sidebarPanel, BorderLayout.WEST);

        // 2. Right Workspace Container
        workspacePanel = new JPanel(new BorderLayout());
        workspacePanel.setBackground(new Color(18, 18, 20));

        // Top Summary Cards
        statsPanel = new StatsPanel();
        workspacePanel.add(statsPanel, BorderLayout.NORTH);

        // Center CardLayout (Switches between Library View and History View)
        centerCardLayout = new CardLayout();
        centerCardContainer = new JPanel(centerCardLayout);
        centerCardContainer.setOpaque(false);

        // Card 1: Library View (Header + MediaTable)
        JPanel libraryView = new JPanel(new BorderLayout());
        libraryView.setOpaque(false);

        headerPanel = new HeaderPanel(
            this::applyCurrentFilters,
            this::handleAddMedia,
            this::handleExportCsv,
            this::handleToggleTheme
        );

        mediaTablePanel = new MediaTablePanel(
            this::handleOpenLink,
            this::handleIncrementProgress,
            this::handleEditMedia,
            this::handleDeleteMedia
        );

        libraryView.add(headerPanel, BorderLayout.NORTH);
        libraryView.add(mediaTablePanel, BorderLayout.CENTER);

        // Card 2: History View
        historyPanel = new HistoryPanel(this::handleClearHistory);

        centerCardContainer.add(libraryView, "LIBRARY");
        centerCardContainer.add(historyPanel, "HISTORY");

        workspacePanel.add(centerCardContainer, BorderLayout.CENTER);
        add(workspacePanel, BorderLayout.CENTER);

        // Initial Data Load
        refreshDashboard();
    }

    public void refreshDashboard() {
        try {
            List<Media> mediaList = mediaService.getAllMedia();
            mediaTablePanel.setMediaList(mediaList, mediaService::formatMetric);
            applyCurrentFilters();

            List<HistoryLog> logs = mediaService.getAllHistory();
            historyPanel.setHistoryLogs(logs);

            int total = mediaService.getTotalCount();
            long inProgress = mediaService.getCountByStatus("Currently Watching")
                            + mediaService.getCountByStatus("Currently Reading")
                            + mediaService.getCountByStatus("Currently Consuming");
            long finished = mediaService.getCountByStatus("Finished");
            statsPanel.updateStats(total, inProgress, finished);
        } catch (SQLException e) {
            showError("Database Error", "Failed to load dashboard data: " + e.getMessage());
        }
    }

    private void handleCategoryChange(String category) {
        this.currentCategory = category;
        centerCardLayout.show(centerCardContainer, "LIBRARY");
        applyCurrentFilters();
    }

    private void showHistoryView() {
        refreshDashboard();
        centerCardLayout.show(centerCardContainer, "HISTORY");
    }

    private void applyCurrentFilters() {
        mediaTablePanel.applyFilters(
            currentCategory,
            headerPanel.getSelectedStatus(),
            headerPanel.getSearchQuery()
        );
    }

    private void handleAddMedia() {
        MediaFormDialog dialog = new MediaFormDialog(this, mediaService, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshDashboard();
        }
    }

    private void handleEditMedia(String id) {
        try {
            Media existing = mediaService.getMediaById(id);
            if (existing == null) {
                showError("Not Found", "Media item '" + id + "' no longer exists.");
                return;
            }
            MediaFormDialog dialog = new MediaFormDialog(this, mediaService, existing);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refreshDashboard();
            }
        } catch (SQLException e) {
            showError("Database Error", "Could not load item for editing: " + e.getMessage());
        }
    }

    private void handleOpenLink(String id) {
        try {
            mediaService.launchMedia(id);
            refreshDashboard();
        } catch (Exception e) {
            showError("Cannot Open Link", e.getMessage());
        }
    }

    private void handleIncrementProgress(String id) {
        try {
            mediaService.incrementProgress(id);
            refreshDashboard();
        } catch (Exception e) {
            showError("Update Failed", e.getMessage());
        }
    }

    private void handleDeleteMedia(String id) {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete media item '" + id + "'?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                mediaService.deleteMedia(id);
                refreshDashboard();
            } catch (SQLException e) {
                showError("Delete Failed", e.getMessage());
            }
        }
    }

    private void handleClearHistory() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Clear all activity history logs? This cannot be undone.",
            "Clear History",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                mediaService.clearHistory();
                refreshDashboard();
            } catch (SQLException e) {
                showError("Clear Failed", e.getMessage());
            }
        }
    }

    private void handleExportCsv() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Media Library to CSV");
        fileChooser.setSelectedFile(new File("media_library_export.csv"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }

            try {
                mediaService.exportMediaToCsv(fileToSave);
                JOptionPane.showMessageDialog(
                    this,
                    "Library exported successfully to:\n" + fileToSave.getAbsolutePath(),
                    "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE
                );
            } catch (Exception e) {
                showError("Export Failed", "Could not export CSV file: " + e.getMessage());
            }
        }
    }

    private void handleToggleTheme() {
        isDarkTheme = !isDarkTheme;
        try {
            if (isDarkTheme) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            SwingUtilities.updateComponentTreeUI(this);
            workspacePanel.setBackground(isDarkTheme ? new Color(18, 18, 20) : new Color(249, 250, 251));
            sidebarPanel.applyTheme(isDarkTheme);
            statsPanel.applyTheme(isDarkTheme);
            mediaTablePanel.applyTheme(isDarkTheme);
            historyPanel.applyTheme(isDarkTheme);
            headerPanel.updateThemeButtonText(isDarkTheme);
        } catch (Exception e) {
            showError("Theme Error", "Failed to switch theme: " + e.getMessage());
        }
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }
}