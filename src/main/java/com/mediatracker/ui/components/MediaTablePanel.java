package com.mediatracker.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import com.mediatracker.models.Media;

public class MediaTablePanel extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable mediaTable;
    private final TableRowSorter<DefaultTableModel> rowSorter;
    private JButton editBtn;

    public MediaTablePanel(
            Consumer<String> onOpenLink,
            Consumer<String> onIncrementProgress,
            Consumer<String> onEditMedia,
            Consumer<String> onDeleteMedia
    ) {
        setLayout(new BorderLayout(0, 10));
        setBorder(new EmptyBorder(0, 20, 16, 20));
        setOpaque(false);

        String[] columns = {"ID", "Title", "Type", "Genre", "Rating", "Status", "Metric", "Link"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 4 ? Double.class : String.class;
            }
        };

        mediaTable = new JTable(tableModel);
        mediaTable.setRowHeight(36);
        mediaTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mediaTable.getTableHeader().setReorderingAllowed(false);
        mediaTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        mediaTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Configure column widths
        mediaTable.getColumnModel().getColumn(0).setPreferredWidth(65);
        mediaTable.getColumnModel().getColumn(1).setPreferredWidth(230);
        mediaTable.getColumnModel().getColumn(2).setPreferredWidth(85);
        mediaTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        mediaTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        mediaTable.getColumnModel().getColumn(5).setPreferredWidth(150);
        mediaTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        mediaTable.getColumnModel().getColumn(7).setPreferredWidth(80);

        // Apply custom cell renderers for Rating, Status, and Link
        applyCustomRenderers();

        // Attach TableRowSorter for instant filtering & column sorting
        rowSorter = new TableRowSorter<>(tableModel);
        mediaTable.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(mediaTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(46, 46, 56)));

        // Bottom Action Bar for Selected Row
        JPanel actionBar = createActionBar(onOpenLink, onIncrementProgress, onEditMedia, onDeleteMedia);

        add(scrollPane, BorderLayout.CENTER);
        add(actionBar, BorderLayout.SOUTH);
    }

    public void setMediaList(List<Media> mediaList, Function<Media, String> metricFormatter) {
        tableModel.setRowCount(0);
        for (Media m : mediaList) {
            boolean hasLink = m.getSourceLink() != null && !m.getSourceLink().trim().isEmpty();
            tableModel.addRow(new Object[]{
                m.getId(),
                m.getTitle(),
                m.getMediaType(),
                m.getGenre() != null && !m.getGenre().isEmpty() ? m.getGenre() : "-",
                m.getRating(),
                m.getStatus(),
                metricFormatter.apply(m),
                hasLink ? "Available" : "-"
            });
        }
    }

    public void applyFilters(String category, String status, String searchQuery) {
        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        // 1. Category Filter (Column 2: Type)
        if (category != null && !"All".equalsIgnoreCase(category)) {
            filters.add(RowFilter.regexFilter("^" + Pattern.quote(category) + "$", 2));
        }

        // 2. Status Filter (Column 5: Status)
        if (status != null && !"All Statuses".equalsIgnoreCase(status)) {
            filters.add(RowFilter.regexFilter("^" + Pattern.quote(status) + "$", 5));
        }

        // 3. Search Query Filter (Columns 0: ID, 1: Title, 3: Genre)
        if (searchQuery != null && !searchQuery.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(searchQuery), 0, 1, 3));
        }

        rowSorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    public String getSelectedMediaId() {
        int viewRow = mediaTable.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = mediaTable.convertRowIndexToModel(viewRow);
        return (String) tableModel.getValueAt(modelRow, 0);
    }

    public void applyTheme(boolean isDarkTheme) {
        if (editBtn != null) {
            editBtn.setForeground(isDarkTheme ? Color.WHITE : new Color(31, 41, 55));
        }
    }

    private JPanel createActionBar(
            Consumer<String> onOpenLink,
            Consumer<String> onIncrementProgress,
            Consumer<String> onEditMedia,
            Consumer<String> onDeleteMedia
    ) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        bar.setOpaque(false);

        JButton openLinkBtn = createActionBtn("Open Link", new Color(59, 130, 246), onOpenLink);
        JButton plusOneBtn = createActionBtn("+1 Progress", new Color(16, 185, 129), onIncrementProgress);
        editBtn = createActionBtn("Edit", Color.WHITE, onEditMedia);
        JButton deleteBtn = createActionBtn("Delete", new Color(239, 68, 68), onDeleteMedia);

        bar.add(openLinkBtn);
        bar.add(plusOneBtn);
        bar.add(editBtn);
        bar.add(deleteBtn);
        return bar;
    }

    private JButton createActionBtn(String text, Color accent, Consumer<String> action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(accent);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            String selectedId = getSelectedMediaId();
            if (selectedId == null) {
                JOptionPane.showMessageDialog(this, "Please select a media item from the table first.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            action.accept(selectedId);
        });
        return btn;
    }

    private void applyCustomRenderers() {
        // Rating Renderer (Column 4)
        mediaTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Double rating) {
                    setText(rating > 0 ? String.format("★ %.1f", rating) : "-");
                    if (!isSelected) setForeground(new Color(251, 191, 36));
                }
                return this;
            }
        });

        // Status Color Renderer (Column 5)
        mediaTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value != null ? value.toString() : "";
                if (!isSelected) {
                    switch (status) {
                        case "Finished" -> setForeground(new Color(52, 211, 153));
                        case "Currently Watching", "Currently Reading", "Currently Consuming" -> setForeground(new Color(96, 165, 250));
                        default -> setForeground(new Color(251, 191, 36));
                    }
                }
                setFont(getFont().deriveFont(Font.BOLD));
                return this;
            }
        });

        // Link Availability Renderer (Column 7)
        mediaTable.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    setForeground("Available".equals(value) ? new Color(96, 165, 250) : new Color(107, 114, 128));
                }
                return this;
            }
        });
    }
}