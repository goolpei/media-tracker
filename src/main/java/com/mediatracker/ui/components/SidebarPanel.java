package com.mediatracker.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

public class SidebarPanel extends JPanel {
    private final ButtonGroup navGroup;
    private final JLabel brandLabel;
    private final List<JToggleButton> navButtons = new ArrayList<>();
    private boolean isDarkTheme = true;

    public SidebarPanel(Consumer<String> onCategorySelected, Runnable onHistorySelected) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(210, 0));

        // Top Branding
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 20));
        brandPanel.setOpaque(false);
        brandLabel = new JLabel("Media Tracker");
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 19));
        brandPanel.add(brandLabel);

        // Navigation Buttons Container
        JPanel navList = new JPanel();
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setOpaque(false);
        navList.setBorder(new EmptyBorder(8, 12, 12, 12));

        navGroup = new ButtonGroup();

        navList.add(createSectionLabel("LIBRARY"));
        navList.add(Box.createVerticalStrut(6));

        navList.add(createNavButton("All Media", true, () -> onCategorySelected.accept("All")));
        navList.add(Box.createVerticalStrut(4));
        navList.add(createNavButton("Anime", false, () -> onCategorySelected.accept("Anime")));
        navList.add(Box.createVerticalStrut(4));
        navList.add(createNavButton("Movies", false, () -> onCategorySelected.accept("Movie")));
        navList.add(Box.createVerticalStrut(4));
        navList.add(createNavButton("Books", false, () -> onCategorySelected.accept("Book")));
        navList.add(Box.createVerticalStrut(4));
        navList.add(createNavButton("Manga", false, () -> onCategorySelected.accept("Manga")));
        navList.add(Box.createVerticalStrut(4));
        navList.add(createNavButton("Manhwa", false, () -> onCategorySelected.accept("Manhwa")));

        navList.add(Box.createVerticalStrut(20));
        navList.add(createSectionLabel("ACTIVITY"));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("History Log", false, onHistorySelected));

        add(brandPanel, BorderLayout.NORTH);
        add(navList, BorderLayout.CENTER);

        applyTheme(true);
    }

    public void applyTheme(boolean isDark) {
        this.isDarkTheme = isDark;
        setBackground(isDark ? new Color(26, 26, 32) : new Color(243, 244, 246));
        setBorder(new MatteBorder(0, 0, 0, 1, isDark ? new Color(46, 46, 56) : new Color(229, 231, 235)));
        brandLabel.setForeground(isDark ? new Color(129, 140, 248) : new Color(79, 70, 229));

        for (JToggleButton btn : navButtons) {
            btn.putClientProperty("JButton.buttonType", "borderless");
            updateNavButtonStyle(btn);
        }
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(new Color(107, 114, 128));
        label.setBorder(new EmptyBorder(0, 12, 0, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JToggleButton createNavButton(String text, boolean selected, Runnable action) {
        JToggleButton btn = new JToggleButton(text, selected);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(0, 12, 0, 12));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setFocusPainted(false);
        btn.putClientProperty("JButton.buttonType", "borderless");
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);

        btn.addItemListener(e -> updateNavButtonStyle(btn));
        btn.addActionListener(e -> action.run());

        navGroup.add(btn);
        navButtons.add(btn);
        updateNavButtonStyle(btn);
        return btn;
    }

    private void updateNavButtonStyle(JToggleButton btn) {
        if (btn.isSelected()) {
            btn.setOpaque(true);
            btn.setBackground(new Color(99, 102, 241));
            btn.setForeground(Color.WHITE);
            btn.setFont(btn.getFont().deriveFont(Font.BOLD));
        } else {
            btn.setOpaque(false);
            btn.setBackground(isDarkTheme ? new Color(26, 26, 32) : new Color(243, 244, 246));
            btn.setForeground(isDarkTheme ? new Color(209, 213, 219) : new Color(55, 65, 81));
            btn.setFont(btn.getFont().deriveFont(Font.PLAIN));
        }
    }
}