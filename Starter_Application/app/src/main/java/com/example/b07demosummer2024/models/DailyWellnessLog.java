package com.example.b07demosummer2024.models;

public class DailyWellnessLog extends BaseHealthLog {
    private int overallFeeling; // 1-5 scale (1=terrible, 5=great)
    private int energyLevel; // 1-5 scale
    private int breathingEase; // 1-5 scale
    private int sleepQuality; // 1-5 scale
    private boolean morningController; // took morning controller medicine
    private boolean eveningController; // took evening controller medicine
    private int rescueInhalerUses; // number of times used today
    private String mood; // happy, sad, anxious, etc.
    private boolean exerciseDone;
    private String exerciseType;
    private boolean schoolAttended;

    public DailyWellnessLog() {
        super();
    }

    public DailyWellnessLog(String logId, String childId, long timestamp, int overallFeeling,
                           int energyLevel, int breathingEase, int sleepQuality,
                           boolean morningController, boolean eveningController,
                           int rescueInhalerUses, String notes, String mood,
                           boolean exerciseDone, String exerciseType, boolean schoolAttended) {
        super(logId, childId, timestamp, notes);
        this.overallFeeling = overallFeeling;
        this.energyLevel = energyLevel;
        this.breathingEase = breathingEase;
        this.sleepQuality = sleepQuality;
        this.morningController = morningController;
        this.eveningController = eveningController;
        this.rescueInhalerUses = rescueInhalerUses;
        this.mood = mood;
        this.exerciseDone = exerciseDone;
        this.exerciseType = exerciseType;
        this.schoolAttended = schoolAttended;
    }

    // Specific getters and setters
    public int getOverallFeeling() { return overallFeeling; }
    public void setOverallFeeling(int overallFeeling) { this.overallFeeling = overallFeeling; }

    public int getEnergyLevel() { return energyLevel; }
    public void setEnergyLevel(int energyLevel) { this.energyLevel = energyLevel; }

    public int getBreathingEase() { return breathingEase; }
    public void setBreathingEase(int breathingEase) { this.breathingEase = breathingEase; }

    public int getSleepQuality() { return sleepQuality; }
    public void setSleepQuality(int sleepQuality) { this.sleepQuality = sleepQuality; }

    public boolean isMorningController() { return morningController; }
    public void setMorningController(boolean morningController) { this.morningController = morningController; }

    public boolean isEveningController() { return eveningController; }
    public void setEveningController(boolean eveningController) { this.eveningController = eveningController; }

    public int getRescueInhalerUses() { return rescueInhalerUses; }
    public void setRescueInhalerUses(int rescueInhalerUses) { this.rescueInhalerUses = rescueInhalerUses; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public boolean isExerciseDone() { return exerciseDone; }
    public void setExerciseDone(boolean exerciseDone) { this.exerciseDone = exerciseDone; }

    public String getExerciseType() { return exerciseType; }
    public void setExerciseType(String exerciseType) { this.exerciseType = exerciseType; }

    public boolean isSchoolAttended() { return schoolAttended; }
    public void setSchoolAttended(boolean schoolAttended) { this.schoolAttended = schoolAttended; }
}