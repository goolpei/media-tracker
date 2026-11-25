package com.mediatracker.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
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
    private JCheckBox selectAllCheckBox;
    private JButton editBtn;
    private JButton undoBtn;
    private boolean isUpdatingSelection = false;

    public MediaTablePanel(
            Consumer<String> onOpenLink,
            Consumer<List<String>> onIncrementProgress,
            Consumer<String> onCustomProgress,
            Consumer<String> onEditMedia,
            Consumer<List<String>> onDeleteMedia,
            Runnable onUndo
    ) {
        setLayout(new BorderLayout(0, 10));
        setBorder(new EmptyBorder(0, 20, 16, 20));
        setOpaque(false);

        String[] columns = {"", "ID", "Title", "Type", "Genre", "Rating", "Status", "Metric", "Link"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                if (columnIndex == 5) return Double.class;
                return String.class;
            }
        };

        mediaTable = new JTable(tableModel);
        mediaTable.setRowHeight(36);
        mediaTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        mediaTable.getTableHeader().setReorderingAllowed(false);
        mediaTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        mediaTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Configure column widths
        mediaTable.getColumnModel().getColumn(0).setPreferredWidth(42);
        mediaTable.getColumnModel().getColumn(0).setMaxWidth(48);
        mediaTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        mediaTable.getColumnModel().getColumn(2).setPreferredWidth(215);
        mediaTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        mediaTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        mediaTable.getColumnModel().getColumn(5).setPreferredWidth(75);
        mediaTable.getColumnModel().getColumn(6).setPreferredWidth(145);
        mediaTable.getColumnModel().getColumn(7).setPreferredWidth(105);
        mediaTable.getColumnModel().getColumn(8).setPreferredWidth(75);

        // Apply custom cell renderers for Rating, Status, and Link
        applyCustomRenderers();

        // Attach TableRowSorter for instant filtering & column sorting
        rowSorter = new TableRowSorter<>(tableModel);
        rowSorter.setSortable(0, false);
        mediaTable.setRowSorter(rowSorter);

        // Keep Select All checkbox synchronized when individual checkboxes are toggled
        tableModel.addTableModelListener(e -> {
            if (!isUpdatingSelection && (e.getColumn() == 0 || e.getColumn() == -1)) {
                syncSelectAllState();
            }
        });

        JScrollPane scrollPane = new JScrollPane(mediaTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(46, 46, 56)));

        // Bottom Action Bar with Select All + Action Buttons
        JPanel actionBar = createActionBar(
            onOpenLink,
            onIncrementProgress,
            onCustomProgress,
            onEditMedia,
            onDeleteMedia,
            onUndo
        );

        add(scrollPane, BorderLayout.CENTER);
        add(actionBar, BorderLayout.SOUTH);
    }

    public void setMediaList(List<Media> mediaList, Function<Media, String> metricFormatter) {
        Set<String> previouslyCheckedIds = getCheckedMediaIds();

        isUpdatingSelection = true;
        try {
            tableModel.setRowCount(0);
            for (Media m : mediaList) {
                boolean hasLink = m.getSourceLink() != null && !m.getSourceLink().trim().isEmpty();
                tableModel.addRow(new Object[]{
                    previouslyCheckedIds.contains(m.getId()),
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
        } finally {
            isUpdatingSelection = false;
        }
        syncSelectAllState();
    }

    public void applyFilters(String category, String status, String searchQuery) {
        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        // 1. Category Filter (Column 3: Type)
        if (category != null && !"All".equalsIgnoreCase(category)) {
            filters.add(RowFilter.regexFilter("^" + Pattern.quote(category) + "$", 3));
        }

        // 2. Status Filter (Column 6: Status)
        if (status != null && !"All Statuses".equalsIgnoreCase(status)) {
            filters.add(RowFilter.regexFilter("^" + Pattern.quote(status) + "$", 6));
        }

        // 3. Search Query Filter (Columns 1: ID, 2: Title, 4: Genre)
        if (searchQuery != null && !searchQuery.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(searchQuery), 1, 2, 4));
        }

        rowSorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        syncSelectAllState();
    }

    public List<String> getSelectedMediaIds() {
        Set<String> selectedIds = getCheckedMediaIds();

        // Fallback to highlighted row(s) if no checkboxes are ticked
        if (selectedIds.isEmpty()) {
            for (int viewRow : mediaTable.getSelectedRows()) {
                int modelRow = mediaTable.convertRowIndexToModel(viewRow);
                selectedIds.add((String) tableModel.getValueAt(modelRow, 1));
            }
        }
        return new ArrayList<>(selectedIds);
    }

    public void updateUndoState(boolean canUndo, String description) {
        if (undoBtn != null) {
            undoBtn.setEnabled(canUndo);
            undoBtn.setToolTipText(description);
        }
    }

    public void applyTheme(boolean isDarkTheme) {
        if (editBtn != null) {
            editBtn.setForeground(isDarkTheme ? Color.WHITE : new Color(31, 41, 55));
        }
        if (selectAllCheckBox != null) {
            selectAllCheckBox.setForeground(isDarkTheme ? new Color(229, 231, 235) : new Color(31, 41, 55));
        }
    }

    private Set<String> getCheckedMediaIds() {
        Set<String> checkedIds = new LinkedHashSet<>();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (Boolean.TRUE.equals(tableModel.getValueAt(row, 0))) {
                checkedIds.add((String) tableModel.getValueAt(row, 1));
            }
        }
        return checkedIds;
    }

    private void syncSelectAllState() {
        if (selectAllCheckBox == null) return;

        int visibleRows = mediaTable.getRowCount();
        int visibleChecked = 0;

        for (int viewRow = 0; viewRow < visibleRows; viewRow++) {
            int modelRow = mediaTable.convertRowIndexToModel(viewRow);
            if (Boolean.TRUE.equals(tableModel.getValueAt(modelRow, 0))) {
                visibleChecked++;
            }
        }

        int totalChecked = getCheckedMediaIds().size();

        isUpdatingSelection = true;
        try {
            selectAllCheckBox.setSelected(visibleRows > 0 && visibleChecked == visibleRows);
            selectAllCheckBox.setText(totalChecked > 0
                ? "Select All (" + totalChecked + " selected)"
                : "Select All");
        } finally {
            isUpdatingSelection = false;
        }
    }

    private JPanel createActionBar(
            Consumer<String> onOpenLink,
            Consumer<List<String>> onIncrementProgress,
            Consumer<String> onCustomProgress,
            Consumer<String> onEditMedia,
            Consumer<List<String>> onDeleteMedia,
            Runnable onUndo
    ) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        // Left: Checkable Select All control
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        leftPanel.setOpaque(false);

        selectAllCheckBox = new JCheckBox("Select All");
        selectAllCheckBox.setFont(new Font("Segoe UI", Font.BOLD, 12));
        selectAllCheckBox.setForeground(new Color(229, 231, 235));
        selectAllCheckBox.setOpaque(false);
        selectAllCheckBox.setFocusPainted(false);
        selectAllCheckBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        selectAllCheckBox.addActionListener(e -> {
            if (isUpdatingSelection) return;
            boolean checkAll = selectAllCheckBox.isSelected();
            isUpdatingSelection = true;
            try {
                for (int viewRow = 0; viewRow < mediaTable.getRowCount(); viewRow++) {
                    int modelRow = mediaTable.convertRowIndexToModel(viewRow);
                    tableModel.setValueAt(checkAll, modelRow, 0);
                }
            } finally {
                isUpdatingSelection = false;
            }
            syncSelectAllState();
        });
        leftPanel.add(selectAllCheckBox);

        // Right: Action buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        rightPanel.setOpaque(false);

        JButton openLinkBtn = createSingleActionBtn("Open Link", new Color(59, 130, 246), "open its link", onOpenLink);
        JPanel progressSplitBtn = createProgressSplitButton(onIncrementProgress, onCustomProgress);
        editBtn = createSingleActionBtn("Edit", Color.WHITE, "edit", onEditMedia);
        JButton deleteBtn = createBatchActionBtn("Delete", new Color(239, 68, 68), onDeleteMedia);

        undoBtn = new JButton("Undo");
        undoBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        undoBtn.setForeground(new Color(245, 158, 11));
        undoBtn.setFocusPainted(false);
        undoBtn.setEnabled(false);
        undoBtn.setToolTipText("Nothing to undo");
        undoBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        undoBtn.addActionListener(e -> onUndo.run());

        rightPanel.add(openLinkBtn);
        rightPanel.add(progressSplitBtn);
        rightPanel.add(editBtn);
        rightPanel.add(deleteBtn);
        rightPanel.add(undoBtn);

        bar.add(leftPanel, BorderLayout.WEST);
        bar.add(rightPanel, BorderLayout.EAST);
        return bar;
    }

    private JPanel createProgressSplitButton(
            Consumer<List<String>> onIncrementProgress,
            Consumer<String> onCustomProgress
    ) {
        Color emerald = new Color(16, 185, 129);

        JPanel splitContainer = new JPanel(new BorderLayout(0, 0));
        splitContainer.setOpaque(false);

        JButton mainBtn = new JButton("+ Progress");
        mainBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        mainBtn.setForeground(emerald);
        mainBtn.setFocusPainted(false);
        mainBtn.setMargin(new Insets(4, 12, 4, 10));
        mainBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mainBtn.setToolTipText("Add +1 progress to selected item(s)");
        mainBtn.addActionListener(e -> {
            List<String> selectedIds = getSelectedMediaIds();
            if (selectedIds.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select at least one media item from the table first.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            onIncrementProgress.accept(selectedIds);
        });

        JButton dropBtn = new JButton(createDownArrowIcon(emerald));
        dropBtn.setFocusPainted(false);
        dropBtn.setMargin(new Insets(4, 8, 4, 8));
        dropBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        dropBtn.setToolTipText("More progress options");

        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem plusOneItem = new JMenuItem("+1 Progress (Selected)");
        plusOneItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        plusOneItem.addActionListener(e -> {
            List<String> selectedIds = getSelectedMediaIds();
            if (selectedIds.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select at least one media item from the table first.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            onIncrementProgress.accept(selectedIds);
        });

        JMenuItem customProgressItem = new JMenuItem("Enter Custom Progress...");
        customProgressItem.setFont(new Font("Segoe UI", Font.BOLD, 12));
        customProgressItem.addActionListener(e -> {
            List<String> selectedIds = getSelectedMediaIds();
            if (selectedIds.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a media item from the table first.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (selectedIds.size() > 1) {
                JOptionPane.showMessageDialog(this, "Please select only one media item to enter custom progress.", "Multiple Items Selected", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            onCustomProgress.accept(selectedIds.get(0));
        });

        popupMenu.add(plusOneItem);
        popupMenu.addSeparator();
        popupMenu.add(customProgressItem);

        dropBtn.addActionListener(e -> popupMenu.show(dropBtn, 0, dropBtn.getHeight()));

        splitContainer.add(mainBtn, BorderLayout.CENTER);
        splitContainer.add(dropBtn, BorderLayout.EAST);
        return splitContainer;
    }

    private JButton createSingleActionBtn(String text, Color accent, String actionLabel, Consumer<String> action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(accent);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            List<String> selectedIds = getSelectedMediaIds();
            if (selectedIds.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a media item from the table first.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (selectedIds.size() > 1) {
                JOptionPane.showMessageDialog(this, "Please select only one media item to " + actionLabel + ".", "Multiple Items Selected", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            action.accept(selectedIds.get(0));
        });
        return btn;
    }

    private JButton createBatchActionBtn(String text, Color accent, Consumer<List<String>> action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(accent);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            List<String> selectedIds = getSelectedMediaIds();
            if (selectedIds.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select at least one media item from the table first.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            action.accept(selectedIds);
        });
        return btn;
    }

    private void applyCustomRenderers() {
        final Icon goldStarIcon = createStarIcon(new Color(251, 191, 36));
        final Icon whiteStarIcon = createStarIcon(Color.WHITE);

        // Rating Renderer (Column 5)
        mediaTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Double rating) {
                    if (rating > 0) {
                        setText(String.format("%.1f", rating));
                        setIcon(isSelected ? whiteStarIcon : goldStarIcon);
                        setIconTextGap(5);
                    } else {
                        setText("-");
                        setIcon(null);
                    }
                    if (!isSelected) setForeground(new Color(251, 191, 36));
                }
                return this;
            }
        });

        // Status Color Renderer (Column 6)
        mediaTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
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

        // Link Availability Renderer (Column 8)
        mediaTable.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
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

    private Icon createStarIcon(Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);

                double cx = x + 6.0;
                double cy = y + 6.0;
                double outerRadius = 5.2;
                double innerRadius = 2.2;

                Path2D star = new Path2D.Double();
                for (int i = 0; i < 10; i++) {
                    double angle = Math.toRadians(-90 + i * 36);
                    double radius = (i % 2 == 0) ? outerRadius : innerRadius;
                    double px = cx + radius * Math.cos(angle);
                    double py = cy + radius * Math.sin(angle);
                    if (i == 0) {
                        star.moveTo(px, py);
                    } else {
                        star.lineTo(px, py);
                    }
                }
                star.closePath();
                g2.fill(star);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 12;
            }

            @Override
            public int getIconHeight() {
                return 12;
            }
        };
    }

    private Icon createDownArrowIcon(Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);

                Path2D triangle = new Path2D.Double();
                triangle.moveTo(x + 1.0, y + 3.0);
                triangle.lineTo(x + 8.0, y + 3.0);
                triangle.lineTo(x + 4.5, y + 7.5);
                triangle.closePath();

                g2.fill(triangle);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 9;
            }

            @Override
            public int getIconHeight() {
                return 10;
            }
        };
    }
}