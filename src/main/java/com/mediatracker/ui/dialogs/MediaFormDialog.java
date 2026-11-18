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

        // Initialize Form Inputs
        idField = new JTextField();
        idField.putClientProperty("JTextField.placeholderText", "e.g., A01, B02");

        String[] types = {"Anime", "Movie", "Book", "Manga", "Manhwa"};
        typeCombo = new JComboBox<>(types);

        titleField = new JTextField();
        titleField.putClientProperty("JTextField.placeholderText", "Enter full title");

        genreField = new JTextField();
        genreField.putClientProperty("JTextField.placeholderText", "e.g., Action, Fantasy");

        ratingSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 10.0, 0.1));
        JSpinner.NumberEditor ratingEditor = new JSpinner.NumberEditor(ratingSpinner, "0.0");
        ratingSpinner.setEditor(ratingEditor);

        String[] statuses = {"Unconsumed", "Currently Consuming", "Finished"};
        statusCombo = new JComboBox<>(statuses);

        extraLabel = new JLabel("Episodes:");
        extraSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));

        sourceLinkField = new JTextField();
        sourceLinkField.putClientProperty("JTextField.placeholderText", "https://...");

        // Row 0: ID & Media Type
        addFormRow(formPanel, gbc, 0, "Media ID:", idField);
        addFormRow(formPanel, gbc, 1, "Media Type:", typeCombo);
        addFormRow(formPanel, gbc, 2, "Title:", titleField);
        addFormRow(formPanel, gbc, 3, "Genre:", genreField);
        addFormRow(formPanel, gbc, 4, "Rating (0 - 10):", ratingSpinner);
        addFormRow(formPanel, gbc, 5, "Status:", statusCombo);
        addFormRow(formPanel, gbc, 6, extraLabel, extraSpinner);
        addFormRow(formPanel, gbc, 7, "Source Link:", sourceLinkField);

        // Dynamic Metric Label Switcher
        typeCombo.addActionListener(e -> updateExtraLabel((String) typeCombo.getSelectedItem()));

        // Populate fields if editing an existing item
        if (existingMedia != null) {
            populateFields(existingMedia);
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

    private void populateFields(Media media) {
        idField.setText(media.getId());
        idField.setEnabled(false); // Prevent changing primary key on edit
        typeCombo.setSelectedItem(media.getMediaType());
        titleField.setText(media.getTitle());
        genreField.setText(media.getGenre() != null ? media.getGenre() : "");
        ratingSpinner.setValue(media.getRating());
        statusCombo.setSelectedItem(media.getStatus());
        sourceLinkField.setText(media.getSourceLink() != null ? media.getSourceLink() : "");

        try {
            extraSpinner.setValue(Integer.parseInt(media.getExtraInfo()));
        } catch (NumberFormatException ignored) {
            extraSpinner.setValue(0);
        }

        updateExtraLabel(media.getMediaType());
    }

    private void updateExtraLabel(String mediaType) {
        String label = switch (mediaType) {
            case "Anime" -> "Episodes:";
            case "Movie" -> "Duration (Minutes):";
            case "Book" -> "Pages:";
            case "Manga", "Manhwa" -> "Chapters:";
            default -> "Count:";
        };
        extraLabel.setText(label);
    }

    private void handleSave() {
        try {
            String id = idField.getText().trim();
            String type = (String) typeCombo.getSelectedItem();
            String title = titleField.getText().trim();
            String genre = genreField.getText().trim();
            double rating = ((Number) ratingSpinner.getValue()).doubleValue();
            String status = (String) statusCombo.getSelectedItem();
            int extraCount = ((Number) extraSpinner.getValue()).intValue();
            String link = sourceLinkField.getText().trim();

            Media media = switch (type) {
                case "Anime" -> new Anime(id, title, genre, rating, status, link, extraCount);
                case "Movie" -> new Movie(id, title, genre, rating, status, link, extraCount);
                case "Book" -> new Book(id, title, genre, rating, status, link, extraCount);
                case "Manga" -> new Manga(id, title, genre, rating, status, link, extraCount);
                case "Manhwa" -> new Manhwa(id, title, genre, rating, status, link, extraCount);
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
        gbc.weightx = 0.32;
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.68;
        field.setPreferredSize(new Dimension(220, 32));
        panel.add(field, gbc);
    }
}