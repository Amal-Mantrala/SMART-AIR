package com.example.b07demosummer2024.models;

public class TechniqueLog {
    private long timestamp;
    private String userId;

    public TechniqueLog() {}

    public TechniqueLog(long timestamp, String userId) {
        this.timestamp = timestamp;
        this.userId = userId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
