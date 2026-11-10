package com.mediatracker.models;

import java.awt.Desktop;
import java.net.URI;

public interface Redirectable {
    default void openSource(String sourceLink) {
        try {
            if (sourceLink != null && !sourceLink.trim().isEmpty() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(sourceLink));
            }
        } catch (Exception e) {
            System.err.println("Failed to open source link: " + e.getMessage());
        }
    }
}