package com.mediatracker.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class HeaderPanel extends JPanel {
    private final JTextField searchField;
    private final JComboBox<String> statusFilterCombo;
    private final JButton themeToggleBtn;

    public HeaderPanel(
            Runnable onFilterChanged,
            Runnable onAddClicked,
            Runnable onExportCsv,
            Runnable onToggleTheme
    ) {
        setLayout(new BorderLayout(12, 0));
        setBorder(new EmptyBorder(12, 20, 12, 20));
        setOpaque(false);

        // Left: Search bar + Status filter
        JPanel filterGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterGroup.setOpaque(false);

        searchField = new JTextField(22);
        searchField.putClientProperty("JTextField.placeholderText", "Search by title, ID, or genre...");
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.setPreferredSize(new Dimension(240, 34));

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onFilterChanged.run(); }
            @Override public void removeUpdate(DocumentEvent e) { onFilterChanged.run(); }
            @Override public void changedUpdate(DocumentEvent e) { onFilterChanged.run(); }
        });

        String[] statuses = {"All Statuses", "Unconsumed", "Currently Consuming", "Finished"};
        statusFilterCombo = new JComboBox<>(statuses);
        statusFilterCombo.setPreferredSize(new Dimension(170, 34));
        statusFilterCombo.addActionListener(e -> onFilterChanged.run());

        filterGroup.add(searchField);
        filterGroup.add(statusFilterCombo);

        // Right: Theme Toggle, Export CSV, and Add Media buttons
        JPanel actionGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionGroup.setOpaque(false);

        themeToggleBtn = new JButton("Light Mode");
        themeToggleBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        themeToggleBtn.setPreferredSize(new Dimension(105, 34));
        themeToggleBtn.setFocusPainted(false);
        themeToggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        themeToggleBtn.addActionListener(e -> onToggleTheme.run());

        JButton exportCsvBtn = new JButton("Export CSV");
        exportCsvBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        exportCsvBtn.setPreferredSize(new Dimension(105, 34));
        exportCsvBtn.setFocusPainted(false);
        exportCsvBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exportCsvBtn.addActionListener(e -> onExportCsv.run());

        JButton addButton = new JButton("+ Add Media");
        addButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        addButton.setBackground(new Color(99, 102, 241));
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        addButton.setPreferredSize(new Dimension(120, 34));
        addButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addButton.addActionListener(e -> onAddClicked.run());

        actionGroup.add(themeToggleBtn);
        actionGroup.add(exportCsvBtn);
        actionGroup.add(addButton);

        add(filterGroup, BorderLayout.WEST);
        add(actionGroup, BorderLayout.EAST);
    }

    public void updateThemeButtonText(boolean isDark) {
        themeToggleBtn.setText(isDark ? "Light Mode" : "Dark Mode");
    }

    public String getSearchQuery() {
        return searchField.getText().trim();
    }

    public String getSelectedStatus() {
        return (String) statusFilterCombo.getSelectedItem();
    }
}