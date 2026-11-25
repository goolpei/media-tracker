package com.mediatracker.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import com.mediatracker.models.Anime;
import com.mediatracker.models.Book;
import com.mediatracker.models.Manga;
import com.mediatracker.models.Manhwa;
import com.mediatracker.models.Media;
import com.mediatracker.models.Movie;
import com.mediatracker.services.MediaService;

public class MediaFormDialog extends JDialog {
    private static final String SELECT_TYPE_PROMPT = "-- Select Media Type --";

    private final MediaService mediaService;
    private final Media existingMedia;
    private boolean saved = false;

    private final JTextField idField;
    private final JComboBox<String> typeCombo;
    private final JTextField titleField;
    private final JTextField genreField;
    private final JSpinner ratingSpinner;
    private final JComboBox<String> statusCombo;
    private final JLabel extraLabel;
    private final JSpinner extraSpinner;
    private final JTextField sourceLinkField;

    public MediaFormDialog(Frame owner, MediaService mediaService, Media existingMedia) {
        super(owner, existingMedia == null ? "Add New Media" : "Edit Media", true);
        this.mediaService = mediaService;
        this.existingMedia = existingMedia;

        setSize(480, 440);
        setResizable(false);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(20, 24, 12, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Initialize Form Inputs (Media ID is strictly computer-generated and read-only)
        idField = new JTextField();
        idField.setEditable(false);
        idField.setEnabled(false);
        idField.setToolTipText("Auto-generated sequential ID");

        String[] types = {SELECT_TYPE_PROMPT, "Anime", "Movie", "Book", "Manga", "Manhwa"};
        typeCombo = new JComboBox<>(types);

        titleField = new JTextField();
        titleField.putClientProperty("JTextField.placeholderText", "Enter full title");

        genreField = new JTextField();
        genreField.putClientProperty("JTextField.placeholderText", "e.g., Action, Fantasy");

        ratingSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 10.0, 0.1));
        JSpinner.NumberEditor ratingEditor = new JSpinner.NumberEditor(ratingSpinner, "0.0");
        ratingSpinner.setEditor(ratingEditor);

        // Status starts disabled until a valid Media Type is selected
        statusCombo = new JComboBox<>(new String[]{"Select Media Type first"});
        statusCombo.setEnabled(false);

        extraLabel = new JLabel("Total Metric:");
        extraSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
        extraSpinner.setToolTipText("Set the total progress boundary (0 = unbounded)");

        sourceLinkField = new JTextField();
        sourceLinkField.putClientProperty("JTextField.placeholderText", "https://...");

        // Form Rows
        addFormRow(formPanel, gbc, 0, "Media ID:", idField);
        addFormRow(formPanel, gbc, 1, "Media Type:", typeCombo);
        addFormRow(formPanel, gbc, 2, "Title:", titleField);
        addFormRow(formPanel, gbc, 3, "Genre:", genreField);
        addFormRow(formPanel, gbc, 4, "Rating (0 - 10):", ratingSpinner);
        addFormRow(formPanel, gbc, 5, "Status:", statusCombo);
        addFormRow(formPanel, gbc, 6, extraLabel, extraSpinner);
        addFormRow(formPanel, gbc, 7, "Source Link:", sourceLinkField);

        // Dynamic Metric Label & Status Choices Switcher
        typeCombo.addActionListener(e -> onMediaTypeChanged((String) typeCombo.getSelectedItem()));

        // Populate fields if editing an existing item, or preview the next generated ID when adding
        if (existingMedia != null) {
            populateFields(existingMedia);
        } else {
            try {
                idField.setText(mediaService.generateNextId());
            } catch (SQLException e) {
                idField.setText("Auto-generated");
            }
        }

        // Bottom Button Bar
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        buttonPanel.setBorder(new EmptyBorder(0, 16, 6, 16));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton(existingMedia == null ? "Save Media" : "Update Media");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveBtn.setBackground(new Color(99, 102, 241));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> handleSave());

        getRootPane().setDefaultButton(saveBtn);

        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public boolean isSaved() {
        return saved;
    }

    private void onMediaTypeChanged(String mediaType) {
        if (mediaType == null || SELECT_TYPE_PROMPT.equals(mediaType)) {
            extraLabel.setText("Total Metric:");
            statusCombo.setModel(new DefaultComboBoxModel<>(new String[]{"Select Media Type first"}));
            statusCombo.setEnabled(false);
            return;
        }

        boolean wasEnabled = statusCombo.isEnabled();
        int previousIndex = wasEnabled ? statusCombo.getSelectedIndex() : 0;

        String[] statusOptions = switch (mediaType) {
            case "Anime", "Movie" -> new String[]{"Unwatched", "Currently Watching", "Finished"};
            case "Book", "Manga", "Manhwa" -> new String[]{"Unread", "Currently Reading", "Finished"};
            default -> new String[]{"Unwatched", "Currently Watching", "Finished"};
        };

        statusCombo.setModel(new DefaultComboBoxModel<>(statusOptions));
        if (previousIndex >= 0 && previousIndex < statusOptions.length) {
            statusCombo.setSelectedIndex(previousIndex);
        }
        statusCombo.setEnabled(true);

        updateExtraLabel(mediaType);
    }

    private void populateFields(Media media) {
        idField.setText(media.getId());
        idField.setEditable(false);
        idField.setEnabled(false);

        typeCombo.setSelectedItem(media.getMediaType());
        onMediaTypeChanged(media.getMediaType());

        titleField.setText(media.getTitle());
        genreField.setText(media.getGenre() != null ? media.getGenre() : "");
        ratingSpinner.setValue(media.getRating());

        String normalizedStatus = normalizeLegacyStatus(media.getMediaType(), media.getStatus());
        statusCombo.setSelectedItem(normalizedStatus);

        sourceLinkField.setText(media.getSourceLink() != null ? media.getSourceLink() : "");

        int boundaryValue = media.getBoundary() > 0 ? media.getBoundary() : media.getCurrentProgress();
        extraSpinner.setValue(boundaryValue);
    }

    private String normalizeLegacyStatus(String mediaType, String status) {
        if (status == null) return "Finished";
        boolean isReadingType = "Book".equals(mediaType) || "Manga".equals(mediaType) || "Manhwa".equals(mediaType);

        if ("Unconsumed".equalsIgnoreCase(status.trim())) {
            return isReadingType ? "Unread" : "Unwatched";
        }
        if ("Currently Consuming".equalsIgnoreCase(status.trim())) {
            return isReadingType ? "Currently Reading" : "Currently Watching";
        }
        return status;
    }

    private void updateExtraLabel(String mediaType) {
        String label = switch (mediaType) {
            case "Anime" -> "Total Episodes:";
            case "Movie" -> "Total Minutes:";
            case "Book" -> "Total Pages:";
            case "Manga", "Manhwa" -> "Total Chapters:";
            default -> "Total Metric:";
        };
        extraLabel.setText(label);
    }

    private void handleSave() {
        try {
            String id = idField.getText().trim();
            String type = (String) typeCombo.getSelectedItem();
            if (type == null || SELECT_TYPE_PROMPT.equals(type)) {
                throw new IllegalArgumentException("Please select a Media Type first.");
            }

            String title = titleField.getText().trim();
            String genre = genreField.getText().trim();
            double rating = ((Number) ratingSpinner.getValue()).doubleValue();
            String status = (String) statusCombo.getSelectedItem();
            int boundary = ((Number) extraSpinner.getValue()).intValue();
            String link = sourceLinkField.getText().trim();

            int currentProgress;
            if (existingMedia == null) {
                currentProgress = "Finished".equalsIgnoreCase(status) ? boundary : 0;
            } else {
                currentProgress = existingMedia.getCurrentProgress();
                if ("Finished".equalsIgnoreCase(status) && boundary > 0) {
                    currentProgress = boundary;
                } else if (boundary > 0 && currentProgress >= boundary) {
                    currentProgress = boundary;
                    status = "Finished";
                }
            }

            Media media = switch (type) {
                case "Anime" -> new Anime(id, title, genre, rating, status, link, currentProgress, boundary);
                case "Movie" -> new Movie(id, title, genre, rating, status, link, currentProgress, boundary);
                case "Book" -> new Book(id, title, genre, rating, status, link, currentProgress, boundary);
                case "Manga" -> new Manga(id, title, genre, rating, status, link, currentProgress, boundary);
                case "Manhwa" -> new Manhwa(id, title, genre, rating, status, link, currentProgress, boundary);
                default -> throw new IllegalArgumentException("Unsupported media type.");
            };

            if (existingMedia == null) {
                mediaService.addMedia(media);
            } else {
                mediaService.updateMedia(media);
            }

            saved = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                "Validation Error",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent field) {
        addFormRow(panel, gbc, row, new JLabel(labelText), field);
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, JLabel label, JComponent field) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.34;
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.66;
        field.setPreferredSize(new Dimension(220, 32));
        panel.add(field, gbc);
    }
}