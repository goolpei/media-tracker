package com.mediatracker.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.mediatracker.models.HistoryLog;

public class HistoryPanel extends JPanel {
    private final DefaultTableModel tableModel;
    private final JLabel titleLabel;
    private final JScrollPane scrollPane;

    public HistoryPanel(Runnable onClearHistory) {
        setLayout(new BorderLayout(0, 12));
        setBorder(new EmptyBorder(12, 20, 16, 20));
        setOpaque(false);

        // Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        titleLabel = new JLabel("Consumption & Activity History");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));

        JButton clearBtn = new JButton("Clear History");
        clearBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        clearBtn.setForeground(new Color(239, 68, 68));
        clearBtn.setFocusPainted(false);
        clearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearBtn.addActionListener(e -> onClearHistory.run());

        topBar.add(titleLabel, BorderLayout.WEST);
        topBar.add(clearBtn, BorderLayout.EAST);

        // History Table
        String[] columns = {"Log #", "Timestamp", "Media ID", "Title", "Type", "Activity"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable historyTable = new JTable(tableModel);
        historyTable.setRowHeight(34);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.getTableHeader().setReorderingAllowed(false);
        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        historyTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(75);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(230);
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(85);
        historyTable.getColumnModel().getColumn(5).setPreferredWidth(240);

        historyTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String action = value != null ? value.toString() : "";
                if (!isSelected) {
                    if (action.contains("Finished") || action.contains("Progress +1")) {
                        setForeground(new Color(16, 185, 129));
                    } else if (action.contains("Opened source link")) {
                        setForeground(new Color(59, 130, 246));
                    } else if (action.contains("Deleted")) {
                        setForeground(new Color(239, 68, 68));
                    } else {
                        setForeground(new Color(99, 102, 241));
                    }
                }
                return this;
            }
        });

        scrollPane = new JScrollPane(historyTable);
        applyTheme(true);

        add(topBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void applyTheme(boolean isDark) {
        titleLabel.setForeground(isDark ? new Color(243, 244, 246) : new Color(17, 24, 39));
        scrollPane.setBorder(BorderFactory.createLineBorder(isDark ? new Color(46, 46, 56) : new Color(229, 231, 235)));
    }

    public void setHistoryLogs(List<HistoryLog> logs) {
        tableModel.setRowCount(0);
        for (HistoryLog log : logs) {
            tableModel.addRow(new Object[]{
                "#" + log.getLogId(),
                log.getLoggedAt(),
                log.getMediaId(),
                log.getMediaTitle(),
                log.getMediaType(),
                log.getAction()
            });
        }
    }
}