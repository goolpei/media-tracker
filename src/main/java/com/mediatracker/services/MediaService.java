package com.mediatracker.services;

import com.mediatracker.database.HistoryDAO;
import com.mediatracker.database.MediaDAO;
import com.mediatracker.models.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

public class MediaService {
    private final MediaDAO mediaDAO;
    private final HistoryDAO historyDAO;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Pattern SEQUENTIAL_ID_PATTERN = Pattern.compile("^[A-Z]\\d{2}$");

    public MediaService() {
        this.mediaDAO = new MediaDAO();
        this.historyDAO = new HistoryDAO();
    }

    public List<Media> getAllMedia() throws SQLException {
        return mediaDAO.findAll();
    }

    public Media getMediaById(String id) throws SQLException {
        return mediaDAO.findById(id);
    }

    public String generateNextId() throws SQLException {
        int maxIndex = -1;

        for (Media m : mediaDAO.findAll()) {
            String existingId = m.getId();
            if (existingId != null && SEQUENTIAL_ID_PATTERN.matcher(existingId).matches()) {
                int letterOffset = existingId.charAt(0) - 'A';
                int numberPart = Integer.parseInt(existingId.substring(1));
                int currentIndex = (letterOffset * 100) + numberPart;
                if (currentIndex > maxIndex) {
                    maxIndex = currentIndex;
                }
            }
        }

        int nextIndex = maxIndex + 1;
        if (nextIndex >= 26 * 100) {
            throw new IllegalStateException("Maximum Media ID limit (Z99) has been reached.");
        }

        char nextLetter = (char) ('A' + (nextIndex / 100));
        int nextNumber = nextIndex % 100;
        return String.format("%c%02d", nextLetter, nextNumber);
    }

    public void addMedia(Media media) throws SQLException {
        if (media == null) {
            throw new IllegalArgumentException("Media object cannot be null.");
        }

        media.setId(generateNextId());
        validateMedia(media);

        if (mediaDAO.findById(media.getId()) != null) {
            throw new IllegalArgumentException("Media ID '" + media.getId() + "' already exists.");
        }

        mediaDAO.insert(media);
        logActivity(media, "Added (" + media.getStatus() + ")");
    }

    public void updateMedia(Media updatedMedia) throws SQLException {
        validateMedia(updatedMedia);

        Media existing = mediaDAO.findById(updatedMedia.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Cannot update: Media ID '" + updatedMedia.getId() + "' not found.");
        }

        mediaDAO.update(updatedMedia);

        if (!existing.getStatus().equals(updatedMedia.getStatus())) {
            logActivity(updatedMedia, "Status changed to " + updatedMedia.getStatus());
        } else {
            logActivity(updatedMedia, "Updated details");
        }
    }

    public void incrementProgress(String id) throws SQLException {
        Media media = mediaDAO.findById(id);
        if (media == null) {
            throw new IllegalArgumentException("Media item not found.");
        }

        String status = media.getStatus() != null ? media.getStatus().trim() : "";
        if ("Finished".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Cannot increment progress: '" + media.getTitle() + "' is already Finished.");
        }

        if (media instanceof Anime anime) {
            anime.setEpisodes(anime.getEpisodes() + 1);
        } else if (media instanceof Movie movie) {
            movie.setDurationMinutes(movie.getDurationMinutes() + 1);
        } else if (media instanceof Book book) {
            book.setPages(book.getPages() + 1);
        } else if (media instanceof Manga manga) {
            manga.setChapters(manga.getChapters() + 1);
        } else if (media instanceof Manhwa manhwa) {
            manhwa.setChapters(manhwa.getChapters() + 1);
        }

        if ("Unwatched".equalsIgnoreCase(status)
                || "Unread".equalsIgnoreCase(status)
                || "Unconsumed".equalsIgnoreCase(status)) {
            boolean isReadingType = (media instanceof Book || media instanceof Manga || media instanceof Manhwa);
            media.setStatus(isReadingType ? "Currently Reading" : "Currently Watching");
        }

        mediaDAO.update(media);
        logActivity(media, "Progress +1 (Now at " + formatMetric(media) + ")");
    }

    public void launchMedia(String id) throws SQLException {
        Media media = mediaDAO.findById(id);
        if (media == null) {
            throw new IllegalArgumentException("Media item not found.");
        }
        if (media.getSourceLink() == null || media.getSourceLink().trim().isEmpty()) {
            throw new IllegalArgumentException("No source link saved for '" + media.getTitle() + "'.");
        }

        media.consume();
        media.openSource(media.getSourceLink());
        logActivity(media, "Opened source link");
    }

    public void deleteMedia(String id) throws SQLException {
        Media existing = mediaDAO.findById(id);
        if (existing != null) {
            mediaDAO.delete(id);
            logActivity(existing, "Deleted from library");
        }
    }

    public List<HistoryLog> getAllHistory() throws SQLException {
        return historyDAO.findAll();
    }

    public void clearHistory() throws SQLException {
        historyDAO.clearAll();
    }

    // Dashboard Statistics Helpers
    public int getTotalCount() throws SQLException {
        return mediaDAO.findAll().size();
    }

    public long getCountByStatus(String status) throws SQLException {
        return mediaDAO.findAll().stream()
                .filter(m -> status.equalsIgnoreCase(m.getStatus()))
                .count();
    }

    // CSV Backup Export
    public void exportMediaToCsv(File file) throws SQLException, IOException {
        List<Media> list = mediaDAO.findAll();
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("ID,Title,Type,Genre,Rating,Status,Metric,SourceLink");
            for (Media m : list) {
                writer.printf("%s,%s,%s,%s,%.1f,%s,%s,%s%n",
                    escapeCsv(m.getId()),
                    escapeCsv(m.getTitle()),
                    escapeCsv(m.getMediaType()),
                    escapeCsv(m.getGenre()),
                    m.getRating(),
                    escapeCsv(m.getStatus()),
                    escapeCsv(formatMetric(m)),
                    escapeCsv(m.getSourceLink())
                );
            }
        }
    }

    public String formatMetric(Media media) {
        String raw = media.getExtraInfo();
        return switch (media.getMediaType()) {
            case "Anime" -> raw + " eps";
            case "Movie" -> raw + " mins";
            case "Book" -> raw + " pages";
            case "Manga", "Manhwa" -> raw + " chaps";
            default -> raw;
        };
    }

    private void logActivity(Media media, String action) throws SQLException {
        String now = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        HistoryLog log = new HistoryLog(
            media.getId(),
            media.getTitle(),
            media.getMediaType(),
            action,
            now
        );
        historyDAO.insert(log);
    }

    private void validateMedia(Media media) {
        if (media == null) {
            throw new IllegalArgumentException("Media object cannot be null.");
        }
        if (media.getId() == null || media.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Media ID is required.");
        }
        if (media.getTitle() == null || media.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required.");
        }
        if (media.getRating() < 0.0 || media.getRating() > 10.0) {
            throw new IllegalArgumentException("Rating must be between 0.0 and 10.0.");
        }
        try {
            int extra = Integer.parseInt(media.getExtraInfo());
            if (extra < 0) {
                throw new IllegalArgumentException("Count cannot be negative.");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Count must be a valid number.");
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}