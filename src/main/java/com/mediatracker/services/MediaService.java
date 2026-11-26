package com.mediatracker.services;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

import com.mediatracker.database.HistoryDAO;
import com.mediatracker.database.MediaDAO;
import com.mediatracker.exceptions.InvalidMediaException;
import com.mediatracker.models.Anime;
import com.mediatracker.models.Book;
import com.mediatracker.models.HistoryLog;
import com.mediatracker.models.Manga;
import com.mediatracker.models.Manhwa;
import com.mediatracker.models.Media;
import com.mediatracker.models.Movie;

public class MediaService {
    private final MediaDAO mediaDAO;
    private final HistoryDAO historyDAO;
    private final Deque<UndoAction> undoStack = new ArrayDeque<>();
    private static final int MAX_UNDO_HISTORY = 20;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Pattern SEQUENTIAL_ID_PATTERN = Pattern.compile("^[A-Z]\\d{2}$");

    private enum ActionType {
        ADD,
        EDIT,
        PROGRESS,
        DELETE
    }

    private record UndoAction(ActionType type, List<Media> snapshots, String description) {}

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

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public String getUndoDescription() {
        UndoAction latest = undoStack.peek();
        return latest != null ? "Undo: " + latest.description() : "Nothing to undo";
    }

    public void undoLastAction() throws SQLException {
        UndoAction action = undoStack.poll();
        if (action == null) {
            throw new IllegalStateException("Nothing to undo.");
        }

        switch (action.type()) {
            case ADD -> {
                for (Media added : action.snapshots()) {
                    if (mediaDAO.findById(added.getId()) != null) {
                        mediaDAO.delete(added.getId());
                        logActivity(added, "Undo: Removed added item");
                    }
                }
            }
            case EDIT -> {
                for (Media previous : action.snapshots()) {
                    if (mediaDAO.findById(previous.getId()) != null) {
                        mediaDAO.update(previous);
                        logActivity(previous, "Undo: Reverted edit");
                    }
                }
            }
            case PROGRESS -> {
                for (Media previous : action.snapshots()) {
                    if (mediaDAO.findById(previous.getId()) != null) {
                        mediaDAO.update(previous);
                        logActivity(previous, "Undo: Reverted progress (Back to " + formatMetric(previous) + ")");
                    }
                }
            }
            case DELETE -> {
                for (Media deleted : action.snapshots()) {
                    if (mediaDAO.findById(deleted.getId()) == null) {
                        mediaDAO.insert(deleted);
                        logActivity(deleted, "Undo: Restored deleted item");
                    }
                }
            }
        }
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
            throw new InvalidMediaException("Media object cannot be null.");
        }

        media.setId(generateNextId());
        validateMedia(media);

        if (mediaDAO.findById(media.getId()) != null) {
            throw new InvalidMediaException("Media ID '" + media.getId() + "' already exists.");
        }

        mediaDAO.insert(media);
        logActivity(media, "Added (" + media.getStatus() + " • " + formatMetric(media) + ")");
        pushUndo(new UndoAction(
            ActionType.ADD,
            List.of(copyMedia(media)),
            "Add '" + media.getTitle() + "'"
        ));
    }

public void updateMedia(Media updatedMedia) throws SQLException {
        validateMedia(updatedMedia);

        Media existing = mediaDAO.findById(updatedMedia.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Cannot update: Media ID '" + updatedMedia.getId() + "' not found.");
        }

        if ("Finished".equalsIgnoreCase(existing.getStatus())
                && "Finished".equalsIgnoreCase(updatedMedia.getStatus())
                && updatedMedia.getBoundary() > existing.getBoundary()
                && updatedMedia.getCurrentProgress() < updatedMedia.getBoundary()) {
            boolean isReadingType = (updatedMedia instanceof Book || updatedMedia instanceof Manga || updatedMedia instanceof Manhwa);
            updatedMedia.setStatus(isReadingType ? "Currently Reading" : "Currently Watching");
        }

        Media beforeSnapshot = copyMedia(existing);
        mediaDAO.update(updatedMedia);

        if (!existing.getStatus().equals(updatedMedia.getStatus())) {
            logActivity(updatedMedia, "Status changed to " + updatedMedia.getStatus());
        } else {
            logActivity(updatedMedia, "Updated details (" + formatMetric(updatedMedia) + ")");
        }

        pushUndo(new UndoAction(
            ActionType.EDIT,
            List.of(beforeSnapshot),
            "Edit '" + updatedMedia.getTitle() + "'"
        ));
    }

    public void incrementProgress(String id) throws SQLException {
        Media media = mediaDAO.findById(id);
        if (media == null) {
            throw new IllegalArgumentException("Media item not found.");
        }

        int[] result = incrementProgressBatch(List.of(id));
        if (result[0] == 0 && result[1] > 0) {
            throw new IllegalStateException("Cannot increment progress: '" + media.getTitle() + "' is already Finished.");
        }
    }

    public int[] incrementProgressBatch(List<String> ids) throws SQLException {
        int updatedCount = 0;
        int skippedFinishedCount = 0;
        List<Media> beforeSnapshots = new ArrayList<>();

        for (String id : ids) {
            Media media = mediaDAO.findById(id);
            if (media == null) {
                continue;
            }

            String status = media.getStatus() != null ? media.getStatus().trim() : "";
            int boundary = media.getBoundary();
            int current = media.getCurrentProgress();

            if ("Finished".equalsIgnoreCase(status) || (boundary > 0 && current >= boundary)) {
                skippedFinishedCount++;
                continue;
            }

            beforeSnapshots.add(copyMedia(media));
            applySingleIncrement(media, status);
            mediaDAO.update(media);

            String logMsg = "Finished".equalsIgnoreCase(media.getStatus())
                ? "Progress +1 (" + formatMetric(media) + " — Finished)"
                : "Progress +1 (Now at " + formatMetric(media) + ")";
            logActivity(media, logMsg);
            updatedCount++;
        }

        if (!beforeSnapshots.isEmpty()) {
            String desc = beforeSnapshots.size() == 1
                ? "+1 Progress on '" + beforeSnapshots.get(0).getTitle() + "'"
                : "+1 Progress on " + beforeSnapshots.size() + " items";
            pushUndo(new UndoAction(ActionType.PROGRESS, beforeSnapshots, desc));
        }

        return new int[]{updatedCount, skippedFinishedCount};
    }

   public void setCustomProgress(String id, int newProgress) throws SQLException {
        Media media = mediaDAO.findById(id);
        if (media == null) {
            throw new InvalidMediaException("Media item not found.");
        }

        if (newProgress < 0) {
            throw new InvalidMediaException("Progress cannot be negative.");
        }

        int boundary = media.getBoundary();
        if (boundary > 0 && newProgress > boundary) {
            throw new InvalidMediaException(
                "Progress (" + newProgress + ") cannot exceed the boundary (" + boundary + ")."
            );
        }

        Media beforeSnapshot = copyMedia(media);
        media.setCurrentProgress(newProgress);

        boolean isReadingType = (media instanceof Book || media instanceof Manga || media instanceof Manhwa);
        if (boundary > 0 && newProgress >= boundary) {
            media.setStatus("Finished");
        } else if (newProgress == 0) {
            media.setStatus(isReadingType ? "Unread" : "Unwatched");
        } else {
            media.setStatus(isReadingType ? "Currently Reading" : "Currently Watching");
        }

        mediaDAO.update(media);
        logActivity(media, "Set progress to " + formatMetric(media) + " (" + media.getStatus() + ")");
        pushUndo(new UndoAction(
            ActionType.PROGRESS,
            List.of(beforeSnapshot),
            "Set progress on '" + media.getTitle() + "'"
        ));
    }

    public void launchMedia(String id) throws SQLException {
        Media media = mediaDAO.findById(id);
        if (media == null) {
            throw new InvalidMediaException("Media item not found.");
        }
        if (media.getSourceLink() == null || media.getSourceLink().trim().isEmpty()) {
            throw new InvalidMediaException("No source link saved for '" + media.getTitle() + "'.");
        }

        media.consume();
        media.openSource(media.getSourceLink());
        logActivity(media, "Opened source link");
    }

    public void deleteMedia(String id) throws SQLException {
        deleteMediaBatch(List.of(id));
    }

    public void deleteMediaBatch(List<String> ids) throws SQLException {
        List<Media> deletedSnapshots = new ArrayList<>();

        for (String id : ids) {
            Media existing = mediaDAO.findById(id);
            if (existing != null) {
                deletedSnapshots.add(copyMedia(existing));
                mediaDAO.delete(id);
                logActivity(existing, "Deleted from library");
            }
        }

        if (!deletedSnapshots.isEmpty()) {
            String desc = deletedSnapshots.size() == 1
                ? "Delete '" + deletedSnapshots.get(0).getTitle() + "'"
                : "Delete " + deletedSnapshots.size() + " items";
            pushUndo(new UndoAction(ActionType.DELETE, deletedSnapshots, desc));
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

    public String getMetricUnit(Media media) {
        return switch (media.getMediaType()) {
            case "Anime" -> "eps";
            case "Movie" -> "mins";
            case "Book" -> "pages";
            case "Manga", "Manhwa" -> "chaps";
            default -> "";
        };
    }

    public String formatMetric(Media media) {
        int current = media.getCurrentProgress();
        int boundary = media.getBoundary();
        String numbers = boundary > 0 ? (current + " / " + boundary) : String.valueOf(current);
        String unit = getMetricUnit(media);
        return unit.isEmpty() ? numbers : numbers + " " + unit;
    }

    private void applySingleIncrement(Media media, String status) {
        int nextProgress = media.getCurrentProgress() + 1;
        int boundary = media.getBoundary();

        if (boundary > 0 && nextProgress >= boundary) {
            media.setCurrentProgress(boundary);
            media.setStatus("Finished");
            return;
        }

        media.setCurrentProgress(nextProgress);

        if ("Unwatched".equalsIgnoreCase(status)
                || "Unread".equalsIgnoreCase(status)
                || "Unconsumed".equalsIgnoreCase(status)) {
            boolean isReadingType = (media instanceof Book || media instanceof Manga || media instanceof Manhwa);
            media.setStatus(isReadingType ? "Currently Reading" : "Currently Watching");
        }
    }

    private Media copyMedia(Media m) {
        int current = m.getCurrentProgress();
        int boundary = m.getBoundary();

        return switch (m.getMediaType()) {
            case "Anime" -> new Anime(m.getId(), m.getTitle(), m.getGenre(), m.getRating(), m.getStatus(), m.getSourceLink(), current, boundary);
            case "Movie" -> new Movie(m.getId(), m.getTitle(), m.getGenre(), m.getRating(), m.getStatus(), m.getSourceLink(), current, boundary);
            case "Book" -> new Book(m.getId(), m.getTitle(), m.getGenre(), m.getRating(), m.getStatus(), m.getSourceLink(), current, boundary);
            case "Manga" -> new Manga(m.getId(), m.getTitle(), m.getGenre(), m.getRating(), m.getStatus(), m.getSourceLink(), current, boundary);
            case "Manhwa" -> new Manhwa(m.getId(), m.getTitle(), m.getGenre(), m.getRating(), m.getStatus(), m.getSourceLink(), current, boundary);
            default -> throw new IllegalArgumentException("Unsupported media type: " + m.getMediaType());
        };
    }

    private void pushUndo(UndoAction action) {
        if (undoStack.size() >= MAX_UNDO_HISTORY) {
            undoStack.removeLast();
        }
        undoStack.push(action);
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
            throw new InvalidMediaException("Media object cannot be null.");
        }
        if (media.getId() == null || media.getId().trim().isEmpty()) {
            throw new InvalidMediaException("Media ID is required.");
        }
        if (media.getTitle() == null || media.getTitle().trim().isEmpty()) {
            throw new InvalidMediaException("Title is required.");
        }
        if (media.getRating() < 0.0 || media.getRating() > 10.0) {
            throw new InvalidMediaException("Rating must be between 0.0 and 10.0.");
        }
        if (media.getCurrentProgress() < 0 || media.getBoundary() < 0) {
            throw new InvalidMediaException("Progress and boundary counts cannot be negative.");
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