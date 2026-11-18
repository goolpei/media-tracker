package com.mediatracker.models;

public class HistoryLog {
    private int logId;
    private String mediaId;
    private String mediaTitle;
    private String mediaType;
    private String action;
    private String loggedAt;

    public HistoryLog(int logId, String mediaId, String mediaTitle, String mediaType, String action, String loggedAt) {
        this.logId = logId;
        this.mediaId = mediaId;
        this.mediaTitle = mediaTitle;
        this.mediaType = mediaType;
        this.action = action;
        this.loggedAt = loggedAt;
    }

    public HistoryLog(String mediaId, String mediaTitle, String mediaType, String action, String loggedAt) {
        this(0, mediaId, mediaTitle, mediaType, action, loggedAt);
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }
    public String getMediaId() { return mediaId; }
    public void setMediaId(String mediaId) { this.mediaId = mediaId; }
    public String getMediaTitle() { return mediaTitle; }
    public void setMediaTitle(String mediaTitle) { this.mediaTitle = mediaTitle; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getLoggedAt() { return loggedAt; }
    public void setLoggedAt(String loggedAt) { this.loggedAt = loggedAt; }
}