package com.example.b07demosummer2024.models;

import java.util.List;

public class TriageIncident extends BaseHealthLog {
    private boolean cannotSpeakFullSentences;
    private boolean chestRetractions;
    private boolean blueGrayLipsNails;
    private int recentRescueAttempts;
    private String peakFlowReading;
    private String decision;
    private String escalationReason;
    private boolean escalated;
    private long escalationTimestamp;
    private String userResponse;

    public TriageIncident() {
        super();
    }

    public TriageIncident(String logId, String childId, long timestamp, String notes,
                         boolean cannotSpeakFullSentences, boolean chestRetractions,
                         boolean blueGrayLipsNails, int recentRescueAttempts,
                         String peakFlowReading, String decision, boolean escalated) {
        super(logId, childId, timestamp, notes);
        this.cannotSpeakFullSentences = cannotSpeakFullSentences;
        this.chestRetractions = chestRetractions;
        this.blueGrayLipsNails = blueGrayLipsNails;
        this.recentRescueAttempts = recentRescueAttempts;
        this.peakFlowReading = peakFlowReading;
        this.decision = decision;
        this.escalated = escalated;
    }

    public boolean isCannotSpeakFullSentences() {
        return cannotSpeakFullSentences;
    }

    public void setCannotSpeakFullSentences(boolean cannotSpeakFullSentences) {
        this.cannotSpeakFullSentences = cannotSpeakFullSentences;
    }

    public boolean isChestRetractions() {
        return chestRetractions;
    }

    public void setChestRetractions(boolean chestRetractions) {
        this.chestRetractions = chestRetractions;
    }

    public boolean isBlueGrayLipsNails() {
        return blueGrayLipsNails;
    }

    public void setBlueGrayLipsNails(boolean blueGrayLipsNails) {
        this.blueGrayLipsNails = blueGrayLipsNails;
    }

    public int getRecentRescueAttempts() {
        return recentRescueAttempts;
    }

    public void setRecentRescueAttempts(int recentRescueAttempts) {
        this.recentRescueAttempts = recentRescueAttempts;
    }

    public String getPeakFlowReading() {
        return peakFlowReading;
    }

    public void setPeakFlowReading(String peakFlowReading) {
        this.peakFlowReading = peakFlowReading;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getEscalationReason() {
        return escalationReason;
    }

    public void setEscalationReason(String escalationReason) {
        this.escalationReason = escalationReason;
    }

    public boolean isEscalated() {
        return escalated;
    }

    public void setEscalated(boolean escalated) {
        this.escalated = escalated;
    }

    public long getEscalationTimestamp() {
        return escalationTimestamp;
    }

    public void setEscalationTimestamp(long escalationTimestamp) {
        this.escalationTimestamp = escalationTimestamp;
    }

    public String getUserResponse() {
        return userResponse;
    }

    public void setUserResponse(String userResponse) {
        this.userResponse = userResponse;
    }

    public boolean hasRedFlags() {
        return cannotSpeakFullSentences || chestRetractions || blueGrayLipsNails;
    }
}
