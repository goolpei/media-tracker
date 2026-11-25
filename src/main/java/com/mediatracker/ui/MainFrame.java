package com.mediatracker.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.DocumentFilter;
import javax.swing.text.NumberFormatter;

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
            this::handleCustomProgress,
            this::handleEditMedia,
            this::handleDeleteMedia,
            this::handleUndo
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
            long unwatched = mediaService.getCountByStatus("Unwatched")
                           + mediaService.getCountByStatus("Unread")
                           + mediaService.getCountByStatus("Unconsumed");
            long inProgress = mediaService.getCountByStatus("Currently Watching")
                            + mediaService.getCountByStatus("Currently Reading")
                            + mediaService.getCountByStatus("Currently Consuming");
            long finished = mediaService.getCountByStatus("Finished");
            statsPanel.updateStats(total, unwatched, inProgress, finished);

            mediaTablePanel.updateUndoState(
                mediaService.canUndo(),
                mediaService.getUndoDescription()
            );
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

    private void handleIncrementProgress(List<String> ids) {
        try {
            int[] result = mediaService.incrementProgressBatch(ids);
            int updated = result[0];
            int skippedFinished = result[1];

            refreshDashboard();

            if (updated == 0 && skippedFinished > 0) {
                showError("Update Failed", "Cannot increment progress: selected item(s) are already Finished.");
            } else if (skippedFinished > 0) {
                JOptionPane.showMessageDialog(
                    this,
                    "Incremented progress for " + updated + " item(s).\nSkipped " + skippedFinished + " already Finished item(s).",
                    "Partial Progress Update",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        } catch (Exception e) {
            showError("Update Failed", e.getMessage());
        }
    }

   private void handleCustomProgress(String id) {
        try {
            Media media = mediaService.getMediaById(id);
            if (media == null) {
                showError("Not Found", "Media item '" + id + "' no longer exists.");
                return;
            }

            int current = media.getCurrentProgress();
            int boundary = media.getBoundary();
            int maxAllowed = boundary > 0 ? boundary : 100000;
            String unit = mediaService.getMetricUnit(media);

            JDialog dialog = new JDialog(this, "Update Progress - " + media.getTitle(), true);
            dialog.setSize(410, 235);
            dialog.setResizable(false);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout());

            JPanel content = new JPanel(new GridBagLayout());
            content.setBorder(new EmptyBorder(16, 20, 8, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel currentInfo = new JLabel(
                "Current: " + mediaService.formatMetric(media)
                + (boundary > 0 ? "  (Boundary: " + boundary + " " + unit + ")" : "  (No boundary set)")
            );
            currentInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            content.add(currentInfo, gbc);

            gbc.gridwidth = 1;
            gbc.gridy = 1;
            gbc.gridx = 0;
            gbc.weightx = 0.45;
            JLabel inputLabel = new JLabel("Set Progress (" + unit + "):");
            inputLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            content.add(inputLabel, gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.55;
            JSpinner progressSpinner = new JSpinner(new SpinnerNumberModel(Math.min(current, maxAllowed), 0, maxAllowed, 1));
            progressSpinner.setPreferredSize(new Dimension(140, 30));
            configureIntegerSpinner(progressSpinner);
            content.add(progressSpinner, gbc);

            // Quick-increment helper buttons inside the modal
            JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            quickPanel.setOpaque(false);

            JButton add10Btn = new JButton("+10");
            add10Btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            add10Btn.addActionListener(e -> {
                int val = readSpinnerInt(progressSpinner);
                progressSpinner.setValue(Math.min(maxAllowed, val + 10));
            });

            JButton add30Btn = new JButton("+30");
            add30Btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            add30Btn.addActionListener(e -> {
                int val = readSpinnerInt(progressSpinner);
                progressSpinner.setValue(Math.min(maxAllowed, val + 30));
            });

            quickPanel.add(add10Btn);
            quickPanel.add(add30Btn);

            if (boundary > 0) {
                JButton maxBtn = new JButton("Max (" + boundary + ")");
                maxBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
                maxBtn.addActionListener(e -> progressSpinner.setValue(boundary));
                quickPanel.add(maxBtn);
            }

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 2;
            content.add(quickPanel, gbc);

            JLabel previewLabel = new JLabel();
            previewLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            previewLabel.setForeground(new Color(16, 185, 129));
            Runnable updatePreview = () -> {
                int val = readSpinnerInt(progressSpinner);
                if (boundary > 0 && val >= boundary) {
                    previewLabel.setText("Will reach boundary (" + val + " / " + boundary + " " + unit + ") -> Finished");
                } else if (boundary > 0) {
                    previewLabel.setText("New metric: " + val + " / " + boundary + " " + unit);
                } else {
                    previewLabel.setText("New metric: " + val + " " + unit);
                }
            };
            updatePreview.run();
            progressSpinner.addChangeListener(e -> updatePreview.run());

            gbc.gridy = 3;
            content.add(previewLabel, gbc);

            JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
            btnBar.setBorder(new EmptyBorder(0, 14, 4, 14));

            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(e -> dialog.dispose());

            JButton applyBtn = new JButton("Apply Progress");
            applyBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            applyBtn.setBackground(new Color(16, 185, 129));
            applyBtn.setForeground(Color.WHITE);
            applyBtn.addActionListener(e -> {
                try {
                    int chosenValue = readSpinnerInt(progressSpinner);
                    mediaService.setCustomProgress(id, chosenValue);
                    dialog.dispose();
                    refreshDashboard();
                } catch (Exception ex) {
                    showError("Invalid Progress", ex.getMessage());
                }
            });

            dialog.getRootPane().setDefaultButton(applyBtn);
            btnBar.add(cancelBtn);
            btnBar.add(applyBtn);

            dialog.add(content, BorderLayout.CENTER);
            dialog.add(btnBar, BorderLayout.SOUTH);
            dialog.setVisible(true);
        } catch (SQLException e) {
            showError("Database Error", "Could not load media item: " + e.getMessage());
        }
    }

private static void configureIntegerSpinner(JSpinner spinner) {
        DecimalFormat format = new DecimalFormat("0");
        format.setGroupingUsed(false);

        DocumentFilter integerFilter = new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string == null) return;
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = current.substring(0, offset) + string + current.substring(offset);
                if (next.matches("^\\d*$")) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String replacement = text == null ? "" : text;
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = current.substring(0, offset) + replacement + current.substring(offset + length);
                if (next.matches("^\\d*$")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        };

        NumberFormatter formatter = new NumberFormatter(format) {
            @Override
            protected DocumentFilter getDocumentFilter() {
                return integerFilter;
            }
        };
        formatter.setValueClass(Integer.class);
        formatter.setAllowsInvalid(true);

        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        JFormattedTextField textField = editor.getTextField();
        textField.setFormatterFactory(new DefaultFormatterFactory(formatter));
    }

    private static int readSpinnerInt(JSpinner spinner) {
        String raw = ((JSpinner.DefaultEditor) spinner.getEditor())
                .getTextField()
                .getText()
                .trim();
        return raw.isEmpty() ? 0 : Integer.parseInt(raw);
    }

    private void handleDeleteMedia(List<String> ids) {
        String prompt = ids.size() == 1
            ? "Delete media item '" + ids.get(0) + "'?"
            : "Delete " + ids.size() + " selected media items? This cannot be undone.";

        int confirm = JOptionPane.showConfirmDialog(
            this,
            prompt,
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                mediaService.deleteMediaBatch(ids);
                refreshDashboard();
            } catch (SQLException e) {
                showError("Delete Failed", e.getMessage());
            }
        }
    }

    private void handleUndo() {
        try {
            mediaService.undoLastAction();
            refreshDashboard();
        } catch (Exception e) {
            showError("Undo Failed", e.getMessage());
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