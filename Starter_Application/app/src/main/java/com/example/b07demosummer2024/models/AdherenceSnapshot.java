package com.example.b07demosummer2024.models;

import java.util.Map;

public class AdherenceSnapshot {
    private String snapshotId;
    private String childId;
    private long periodStart;
    private long periodEnd;
    private Map<String, Boolean> scheduleConfig;
    private int plannedDays;
    private int loggedDays;
    private double adherence;
    private long calculatedAt;

    public AdherenceSnapshot() {
    }

    public AdherenceSnapshot(String snapshotId, String childId, long periodStart, long periodEnd,
                           Map<String, Boolean> scheduleConfig, int plannedDays, int loggedDays,
                           double adherence, long calculatedAt) {
        this.snapshotId = snapshotId;
        this.childId = childId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.scheduleConfig = scheduleConfig;
        this.plannedDays = plannedDays;
        this.loggedDays = loggedDays;
        this.adherence = adherence;
        this.calculatedAt = calculatedAt;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    public long getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(long periodStart) {
        this.periodStart = periodStart;
    }

    public long getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(long periodEnd) {
        this.periodEnd = periodEnd;
    }

    public Map<String, Boolean> getScheduleConfig() {
        return scheduleConfig;
    }

    public void setScheduleConfig(Map<String, Boolean> scheduleConfig) {
        this.scheduleConfig = scheduleConfig;
    }

    public int getPlannedDays() {
        return plannedDays;
    }

    public void setPlannedDays(int plannedDays) {
        this.plannedDays = plannedDays;
    }

    public int getLoggedDays() {
        return loggedDays;
    }

    public void setLoggedDays(int loggedDays) {
        this.loggedDays = loggedDays;
    }

    public double getAdherence() {
        return adherence;
    }

    public void setAdherence(double adherence) {
        this.adherence = adherence;
    }

    public long getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(long calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
