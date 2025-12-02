package com.example.b07demosummer2024.models;

import java.util.List;

public class SymptomLog extends BaseHealthLog {
    private List<String> symptoms; // wheezing, coughing, shortness of breath, etc.
    private int overallSeverity; // 1-10 scale
    private List<String> triggers; // dust, pollen, exercise, weather, etc.
    private List<String> tags;
    private String location; // where symptoms occurred
    private boolean rescueInhalerUsed;
    private String peakFlowReading; // if available
    private String weatherConditions;
    private String activityLevel; // resting, light activity, intense exercise
    private int sleepQuality; // 1-5 scale if symptoms affected sleep

    public SymptomLog() {
        super();
    }

    public SymptomLog(String logId, String childId, long timestamp, List<String> symptoms,
                     int overallSeverity, List<String> triggers, String location, String notes,
                     boolean rescueInhalerUsed, String peakFlowReading, String weatherConditions,
                     String activityLevel, int sleepQuality) {
        super(logId, childId, timestamp, notes);
        this.symptoms = symptoms;
        this.overallSeverity = overallSeverity;
        this.triggers = triggers;
        this.location = location;
        this.rescueInhalerUsed = rescueInhalerUsed;
        this.peakFlowReading = peakFlowReading;
        this.weatherConditions = weatherConditions;
        this.activityLevel = activityLevel;
        this.sleepQuality = sleepQuality;
    }

    // Specific getters and setters
    public List<String> getSymptoms() { return symptoms; }
    public void setSymptoms(List<String> symptoms) { this.symptoms = symptoms; }

    public int getOverallSeverity() { return overallSeverity; }
    public void setOverallSeverity(int overallSeverity) { this.overallSeverity = overallSeverity; }

    public List<String> getTriggers() { return triggers; }
    public void setTriggers(List<String> triggers) { this.triggers = triggers; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public boolean isRescueInhalerUsed() { return rescueInhalerUsed; }
    public void setRescueInhalerUsed(boolean rescueInhalerUsed) { this.rescueInhalerUsed = rescueInhalerUsed; }

    public String getPeakFlowReading() { return peakFlowReading; }
    public void setPeakFlowReading(String peakFlowReading) { this.peakFlowReading = peakFlowReading; }

    public String getWeatherConditions() { return weatherConditions; }
    public void setWeatherConditions(String weatherConditions) { this.weatherConditions = weatherConditions; }

    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }

    public int getSleepQuality() { return sleepQuality; }
    public void setSleepQuality(int sleepQuality) { this.sleepQuality = sleepQuality; }

}