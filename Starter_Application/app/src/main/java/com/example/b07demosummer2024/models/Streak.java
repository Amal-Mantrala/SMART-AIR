package com.example.b07demosummer2024.models;

public class Streak {
    private String streakId;
    private String childId;
    private String streakType; // "controller_planned", "technique_completed"
    private int currentCount;
    private int bestCount;
    private long lastUpdateDate;
    private boolean isActive;
    private long createdAt;

    public Streak() {
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
    }

    public Streak(String streakId, String childId, String streakType) {
        this();
        this.streakId = streakId;
        this.childId = childId;
        this.streakType = streakType;
        this.currentCount = 0;
        this.bestCount = 0;
        this.lastUpdateDate = 0;
    }

    // Getters and setters
    public String getStreakId() { return streakId; }
    public void setStreakId(String streakId) { this.streakId = streakId; }

    public String getChildId() { return childId; }
    public void setChildId(String childId) { this.childId = childId; }

    public String getStreakType() { return streakType; }
    public void setStreakType(String streakType) { this.streakType = streakType; }

    public int getCurrentCount() { return currentCount; }
    public void setCurrentCount(int currentCount) { 
        this.currentCount = currentCount;
        if (currentCount > bestCount) {
            this.bestCount = currentCount;
        }
    }

    public int getBestCount() { return bestCount; }
    public void setBestCount(int bestCount) { this.bestCount = bestCount; }

    public long getLastUpdateDate() { return lastUpdateDate; }
    public void setLastUpdateDate(long lastUpdateDate) { this.lastUpdateDate = lastUpdateDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // Utility methods
    public void incrementStreak() {
        this.currentCount++;
        this.lastUpdateDate = System.currentTimeMillis();
        if (this.currentCount > this.bestCount) {
            this.bestCount = this.currentCount;
        }
    }

    public void resetStreak() {
        this.currentCount = 0;
        this.lastUpdateDate = System.currentTimeMillis();
    }

    public boolean shouldReset(long daysSinceLastUpdate) {
        // Reset if more than 1 day has passed without update
        return daysSinceLastUpdate > 1;
    }
}