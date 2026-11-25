package com.mediatracker.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

public class StatsPanel extends JPanel {
    private final JLabel totalValueLabel;
    private final JLabel unwatchedValueLabel;
    private final JLabel consumingValueLabel;
    private final JLabel finishedValueLabel;
    private final List<CardEntry> cards = new ArrayList<>();

    private record CardEntry(JPanel card, JLabel titleLabel, Color accentColor) {}

    public StatsPanel() {
        setLayout(new GridLayout(1, 4, 14, 0));
        setBorder(new EmptyBorder(16, 20, 8, 20));
        setOpaque(false);

        totalValueLabel = createValueLabel(new Color(99, 102, 241));
        unwatchedValueLabel = createValueLabel(new Color(245, 158, 11));
        consumingValueLabel = createValueLabel(new Color(59, 130, 246));
        finishedValueLabel = createValueLabel(new Color(16, 185, 129));

        add(createStatCard("TOTAL TRACKED", totalValueLabel, new Color(99, 102, 241)));
        add(createStatCard("UNWATCHED / UNREAD", unwatchedValueLabel, new Color(245, 158, 11)));
        add(createStatCard("WATCHING / READING", consumingValueLabel, new Color(59, 130, 246)));
        add(createStatCard("FINISHED", finishedValueLabel, new Color(16, 185, 129)));

        applyTheme(true);
    }

    public void updateStats(int total, long unwatched, long consuming, long finished) {
        totalValueLabel.setText(String.valueOf(total));
        unwatchedValueLabel.setText(String.valueOf(unwatched));
        consumingValueLabel.setText(String.valueOf(consuming));
        finishedValueLabel.setText(String.valueOf(finished));
    }

    public void applyTheme(boolean isDark) {
        Color cardBg = isDark ? new Color(30, 30, 36) : Color.WHITE;
        Color titleColor = isDark ? new Color(156, 163, 175) : new Color(107, 114, 128);
        Color outerLineColor = isDark ? new Color(46, 46, 56) : new Color(229, 231, 235);

        for (CardEntry entry : cards) {
            entry.card().setBackground(cardBg);
            entry.titleLabel().setForeground(titleColor);

            Border outerEdge = BorderFactory.createMatteBorder(1, 0, 1, 1, outerLineColor);
            Border leftAccent = BorderFactory.createMatteBorder(0, 4, 0, 0, entry.accentColor());
            Border innerPadding = new EmptyBorder(14, 16, 14, 16);

            entry.card().setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(outerEdge, leftAccent),
                innerPadding
            ));
        }
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 6));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        cards.add(new CardEntry(card, titleLabel, accentColor));
        return card;
    }

    private JLabel createValueLabel(Color color) {
        JLabel label = new JLabel("0");
        label.setFont(new Font("Segoe UI", Font.BOLD, 26));
        label.setForeground(color);
        return label;
    }
}