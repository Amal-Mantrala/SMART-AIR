package com.example.b07demosummer2024.models;

/**
 * Base model class to reduce duplication of common fields across health log models
 */
public abstract class BaseHealthLog {
    protected String logId;
    protected String childId;
    protected long timestamp;
    protected String notes;
    protected String enteredBy;

    public BaseHealthLog() {}

    public BaseHealthLog(String logId, String childId, long timestamp, String notes) {
        this.logId = logId;
        this.childId = childId;
        this.timestamp = timestamp;
        this.notes = notes;
    }

    // Common getters and setters
    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getEnteredBy() {
        return enteredBy;
    }

    public void setEnteredBy(String enteredBy) {
        this.enteredBy = enteredBy;
    }
}