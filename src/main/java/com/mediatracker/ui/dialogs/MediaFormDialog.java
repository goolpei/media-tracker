package com.mediatracker.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.text.DecimalFormat;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.DocumentFilter;
import javax.swing.text.NumberFormatter;

import com.mediatracker.exceptions.InvalidMediaException;
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
    private boolean isPopulating = false;

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
        configureNumericSpinner(ratingSpinner, true);

        // Status starts disabled until a valid Media Type is selected
        statusCombo = new JComboBox<>(new String[]{"Select Media Type first"});
        statusCombo.setEnabled(false);

        extraLabel = new JLabel("Total Metric:");
        extraSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
        extraSpinner.setToolTipText("Set the total progress boundary (0 = unbounded)");
        configureNumericSpinner(extraSpinner, false);

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

        // Dynamic Metric Label, Reset, & Status Choices Switcher
        typeCombo.addActionListener(e -> onMediaTypeChanged((String) typeCombo.getSelectedItem()));

        // Auto-revert "Finished" status if total metric count is increased above current progress
        extraSpinner.addChangeListener(e -> onBoundaryChanged());

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

    private static void configureNumericSpinner(JSpinner spinner, boolean allowDecimal) {
        DecimalFormat format = new DecimalFormat(allowDecimal ? "0.0" : "0");
        format.setGroupingUsed(false);

        String regex = allowDecimal ? "^\\d*(\\.\\d*)?$" : "^\\d*$";
        DocumentFilter numericFilter = new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string == null) return;
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = current.substring(0, offset) + string + current.substring(offset);
                if (next.matches(regex)) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String replacement = text == null ? "" : text;
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = current.substring(0, offset) + replacement + current.substring(offset + length);
                if (next.matches(regex)) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        };

        NumberFormatter formatter = new NumberFormatter(format) {
            @Override
            protected DocumentFilter getDocumentFilter() {
                return numericFilter;
            }
        };
        formatter.setValueClass(allowDecimal ? Double.class : Integer.class);
        formatter.setAllowsInvalid(true);

        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        JFormattedTextField textField = editor.getTextField();
        textField.setFormatterFactory(new DefaultFormatterFactory(formatter));
    }

    private void onMediaTypeChanged(String mediaType) {
        if (!isPopulating) {
            extraSpinner.setValue(0);
        }

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

    private void onBoundaryChanged() {
        if (isPopulating || existingMedia == null) {
            return;
        }

        String selectedType = (String) typeCombo.getSelectedItem();
        if (selectedType == null || !selectedType.equals(existingMedia.getMediaType())) {
            return;
        }

        String currentStatus = (String) statusCombo.getSelectedItem();
        int newBoundary = ((Number) extraSpinner.getValue()).intValue();

        if ("Finished".equalsIgnoreCase(currentStatus) && newBoundary > existingMedia.getCurrentProgress()) {
            statusCombo.setSelectedItem(getInProgressStatus(selectedType));
        }
    }

    private void populateFields(Media media) {
        isPopulating = true;
        try {
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
            extraSpinner.setValue(media.getBoundary());
        } finally {
            isPopulating = false;
        }
    }

    private String getInProgressStatus(String mediaType) {
        boolean isReadingType = "Book".equals(mediaType) || "Manga".equals(mediaType) || "Manhwa".equals(mediaType);
        return isReadingType ? "Currently Reading" : "Currently Watching";
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

    private double parseRatingInput() {
        String rawText = ((JSpinner.DefaultEditor) ratingSpinner.getEditor())
                .getTextField()
                .getText()
                .trim();

        if (rawText.isEmpty() || ".".equals(rawText)) {
            return 0.0;
        }

        double parsed;
        try {
            parsed = Double.parseDouble(rawText);
        } catch (NumberFormatException e) {
            throw new InvalidMediaException("Rating must be a valid number.", e);
        }

        if (parsed < 0.0 || parsed > 10.0) {
            throw new InvalidMediaException("Rating must be between 0.0 and 10.0.");
        }

        return parsed;
    }

    private int parseBoundaryInput() {
        String rawText = ((JSpinner.DefaultEditor) extraSpinner.getEditor())
                .getTextField()
                .getText()
                .trim();

        if (rawText.isEmpty()) {
            return 0;
        }

        int parsed;
        try {
            parsed = Integer.parseInt(rawText);
        } catch (NumberFormatException e) {
            throw new InvalidMediaException("Total metric count must be a valid whole number.", e);
        }

        if (parsed < 0) {
            throw new InvalidMediaException("Total metric count cannot be negative.");
        }

        return parsed;
    }

    private void handleSave() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            String id = idField.getText().trim();
            String type = (String) typeCombo.getSelectedItem();
            if (type == null || SELECT_TYPE_PROMPT.equals(type)) {
                throw new InvalidMediaException("Please select a Media Type first.");
            }

            String title = titleField.getText().trim();
            String genre = genreField.getText().trim();
            double rating = parseRatingInput();
            int boundary = parseBoundaryInput();
            String status = (String) statusCombo.getSelectedItem();
            String link = sourceLinkField.getText().trim();

            int currentProgress;
            if (existingMedia == null || !existingMedia.getMediaType().equals(type)) {
                currentProgress = "Finished".equalsIgnoreCase(status) ? boundary : 0;
            } else {
                currentProgress = existingMedia.getCurrentProgress();

                if ("Finished".equalsIgnoreCase(existingMedia.getStatus())
                        && "Finished".equalsIgnoreCase(status)
                        && boundary > currentProgress) {
                    status = getInProgressStatus(type);
                } else if ("Finished".equalsIgnoreCase(status) && boundary > 0) {
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
                default -> throw new InvalidMediaException("Unsupported media type.");
            };

            if (existingMedia == null) {
                mediaService.addMedia(media);
            } else {
                mediaService.updateMedia(media);
            }

            saved = true;
            dispose();
        } catch (InvalidMediaException ex) {
            JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                "Validation Error",
                JOptionPane.WARNING_MESSAGE
            );
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Failed to save media: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        } finally {
            setCursor(Cursor.getDefaultCursor());
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