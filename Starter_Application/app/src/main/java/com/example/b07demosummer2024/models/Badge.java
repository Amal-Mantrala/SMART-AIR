package com.example.b07demosummer2024.models;

public class Badge {
    private String badgeId;
    private String childId;
    private String badgeType; // "perfect_controller_week", "technique_master", "low_rescue_month"
    private String title;
    private String description;
    private String iconName;
    private long earnedDate;
    private boolean isUnlocked;
    private int progress;
    private int targetValue;
    private long createdAt;
    private String tier; // "bronze", "silver", "gold"

    public Badge() {
        this.createdAt = System.currentTimeMillis();
        this.isUnlocked = false;
        this.progress = 0;
        this.tier = "gold"; // all badges are gold when unlocked
    }

    public Badge(String badgeId, String childId, String badgeType, String title, String description, int targetValue) {
        this();
        this.badgeId = badgeId;
        this.childId = childId;
        this.badgeType = badgeType;
        this.title = title;
        this.description = description;
        this.targetValue = targetValue;
        this.iconName = getDefaultIconForType(badgeType);
    }

    // Getters and setters
    public String getBadgeId() { return badgeId; }
    public void setBadgeId(String badgeId) { this.badgeId = badgeId; }

    public String getChildId() { return childId; }
    public void setChildId(String childId) { this.childId = childId; }

    public String getBadgeType() { return badgeType; }
    public void setBadgeType(String badgeType) { this.badgeType = badgeType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }

    public long getEarnedDate() { return earnedDate; }
    public void setEarnedDate(long earnedDate) { this.earnedDate = earnedDate; }

    public boolean isUnlocked() { return isUnlocked; }
    public void setUnlocked(boolean unlocked) { 
        this.isUnlocked = unlocked;
        if (unlocked && this.earnedDate == 0) {
            this.earnedDate = System.currentTimeMillis();
        }
    }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { 
        this.progress = progress;
        if (progress >= targetValue && !isUnlocked) {
            setUnlocked(true);
            updateTierBasedOnProgress();
        }
    }

    public int getTargetValue() { return targetValue; }
    public void setTargetValue(int targetValue) { this.targetValue = targetValue; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    // Utility methods
    public double getProgressPercentage() {
        if (targetValue == 0) return 0;
        return Math.min(100.0, (progress * 100.0) / targetValue);
    }

    public void incrementProgress() {
        setProgress(progress + 1);
    }

    public String getTrophyEmoji() {
        return "🥇";
    }

    public void updateTierBasedOnProgress() {
        if (isUnlocked) {
            tier = "gold";
        }
    }

    private String getDefaultIconForType(String badgeType) {
        switch (badgeType) {
            case "perfect_controller_week":
                return "ic_star_gold";
            case "technique_master":
                return "ic_trophy_technique";
            case "low_rescue_month":
                return "ic_shield_green";
            default:
                return "ic_badge_default";
        }
    }
}