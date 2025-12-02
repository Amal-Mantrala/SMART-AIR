package com.example.b07demosummer2024.models;

import java.util.List;

public class MedicineLog extends BaseHealthLog {
    private String medicineType; // "rescue" or "controller"
    private String medicineName;
    private String dosage;
    private List<String> symptoms; // symptoms that triggered rescue inhaler use
    private int severityLevel; // 1-10 scale
    private String location; // where the medicine was taken
    private boolean reminderTaken; // whether this was from a reminder

    public MedicineLog() {
        super();
    }

    public MedicineLog(String logId, String childId, String medicineType, String medicineName, 
                      String dosage, long timestamp, String notes, List<String> symptoms, 
                      int severityLevel, String location, boolean reminderTaken) {
        super(logId, childId, timestamp, notes);
        this.medicineType = medicineType;
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.symptoms = symptoms;
        this.severityLevel = severityLevel;
        this.location = location;
        this.reminderTaken = reminderTaken;
    }

    // Specific getters and setters
    public String getMedicineType() { return medicineType; }
    public void setMedicineType(String medicineType) { this.medicineType = medicineType; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public List<String> getSymptoms() { return symptoms; }
    public void setSymptoms(List<String> symptoms) { this.symptoms = symptoms; }

    public int getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(int severityLevel) { this.severityLevel = severityLevel; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public boolean isReminderTaken() { return reminderTaken; }
    public void setReminderTaken(boolean reminderTaken) { this.reminderTaken = reminderTaken; }

}