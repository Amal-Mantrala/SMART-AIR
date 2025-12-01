package com.example.b07demosummer2024.models;

public class MotivationSettings {
    private String settingsId;
    private String childId;
    
    // Streak thresholds
    private int controllerStreakThreshold;
    private int techniqueStreakThreshold;
    
    // Badge thresholds
    private int perfectControllerWeekDays; // Number of days for perfect week
    private int techniqueMasterSessions; // Number of high-quality technique sessions
    private int lowRescueMonthLimit; // Max rescue days per 30 days
    private int lowRescueMonthDays; // Number of days to track (default 30)
    
    // Notification settings
    private boolean streakNotificationsEnabled;
    private boolean badgeNotificationsEnabled;
    private boolean weeklyProgressEnabled;
    
    private long lastUpdated;

    public MotivationSettings() {
        // Default values
        this.controllerStreakThreshold = 7;
        this.techniqueStreakThreshold = 5;
        this.perfectControllerWeekDays = 7;
        this.techniqueMasterSessions = 10;
        this.lowRescueMonthLimit = 4;
        this.lowRescueMonthDays = 30;
        this.streakNotificationsEnabled = true;
        this.badgeNotificationsEnabled = true;
        this.weeklyProgressEnabled = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public MotivationSettings(String childId) {
        this();
        this.childId = childId;
        this.settingsId = childId + "_motivation_settings";
    }

    // Getters and setters
    public String getSettingsId() { return settingsId; }
    public void setSettingsId(String settingsId) { this.settingsId = settingsId; }

    public String getChildId() { return childId; }
    public void setChildId(String childId) { this.childId = childId; }

    public int getControllerStreakThreshold() { return controllerStreakThreshold; }
    public void setControllerStreakThreshold(int controllerStreakThreshold) { 
        this.controllerStreakThreshold = controllerStreakThreshold;
        updateTimestamp();
    }

    public int getTechniqueStreakThreshold() { return techniqueStreakThreshold; }
    public void setTechniqueStreakThreshold(int techniqueStreakThreshold) { 
        this.techniqueStreakThreshold = techniqueStreakThreshold;
        updateTimestamp();
    }

    public int getPerfectControllerWeekDays() { return perfectControllerWeekDays; }
    public void setPerfectControllerWeekDays(int perfectControllerWeekDays) { 
        this.perfectControllerWeekDays = perfectControllerWeekDays;
        updateTimestamp();
    }

    public int getTechniqueMasterSessions() { return techniqueMasterSessions; }
    public void setTechniqueMasterSessions(int techniqueMasterSessions) { 
        this.techniqueMasterSessions = techniqueMasterSessions;
        updateTimestamp();
    }

    public int getLowRescueMonthLimit() { return lowRescueMonthLimit; }
    public void setLowRescueMonthLimit(int lowRescueMonthLimit) { 
        this.lowRescueMonthLimit = lowRescueMonthLimit;
        updateTimestamp();
    }

    public int getLowRescueMonthDays() { return lowRescueMonthDays; }
    public void setLowRescueMonthDays(int lowRescueMonthDays) { 
        this.lowRescueMonthDays = lowRescueMonthDays;
        updateTimestamp();
    }

    public boolean isStreakNotificationsEnabled() { return streakNotificationsEnabled; }
    public void setStreakNotificationsEnabled(boolean streakNotificationsEnabled) { 
        this.streakNotificationsEnabled = streakNotificationsEnabled;
        updateTimestamp();
    }

    public boolean isBadgeNotificationsEnabled() { return badgeNotificationsEnabled; }
    public void setBadgeNotificationsEnabled(boolean badgeNotificationsEnabled) { 
        this.badgeNotificationsEnabled = badgeNotificationsEnabled;
        updateTimestamp();
    }

    public boolean isWeeklyProgressEnabled() { return weeklyProgressEnabled; }
    public void setWeeklyProgressEnabled(boolean weeklyProgressEnabled) { 
        this.weeklyProgressEnabled = weeklyProgressEnabled;
        updateTimestamp();
    }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    private void updateTimestamp() {
        this.lastUpdated = System.currentTimeMillis();
    }
}