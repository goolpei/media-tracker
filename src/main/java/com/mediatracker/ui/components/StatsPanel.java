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
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

public class StatsPanel extends JPanel {
    private final JLabel totalValueLabel;
    private final JLabel consumingValueLabel;
    private final JLabel finishedValueLabel;
    private final List<JPanel> cards = new ArrayList<>();
    private final List<JLabel> titleLabels = new ArrayList<>();

    public StatsPanel() {
        setLayout(new GridLayout(1, 3, 16, 0));
        setBorder(new EmptyBorder(16, 20, 8, 20));
        setOpaque(false);

        totalValueLabel = createValueLabel(new Color(99, 102, 241));
        consumingValueLabel = createValueLabel(new Color(59, 130, 246));
        finishedValueLabel = createValueLabel(new Color(16, 185, 129));

        add(createStatCard("TOTAL TRACKED", totalValueLabel, new Color(99, 102, 241)));
        add(createStatCard("CURRENTLY CONSUMING", consumingValueLabel, new Color(59, 130, 246)));
        add(createStatCard("FINISHED", finishedValueLabel, new Color(16, 185, 129)));

        applyTheme(true);
    }

    public void updateStats(int total, long consuming, long finished) {
        totalValueLabel.setText(String.valueOf(total));
        consumingValueLabel.setText(String.valueOf(consuming));
        finishedValueLabel.setText(String.valueOf(finished));
    }

    public void applyTheme(boolean isDark) {
        Color cardBg = isDark ? new Color(30, 30, 36) : Color.WHITE;
        Color titleColor = isDark ? new Color(156, 163, 175) : new Color(107, 114, 128);

        for (JPanel card : cards) {
            card.setBackground(cardBg);
        }
        for (JLabel title : titleLabels) {
            title.setForeground(titleColor);
        }
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 4, 0, 0, accentColor),
            new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        cards.add(card);
        titleLabels.add(titleLabel);
        return card;
    }

    private JLabel createValueLabel(Color color) {
        JLabel label = new JLabel("0");
        label.setFont(new Font("Segoe UI", Font.BOLD, 26));
        label.setForeground(color);
        return label;
    }
}