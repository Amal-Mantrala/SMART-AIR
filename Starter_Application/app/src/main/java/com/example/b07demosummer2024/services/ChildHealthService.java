package com.example.b07demosummer2024.services;

import com.example.b07demosummer2024.models.MedicineLog;
import com.example.b07demosummer2024.models.SymptomLog;
import com.example.b07demosummer2024.models.DailyWellnessLog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChildHealthService {
    private FirebaseFirestore db;

    public ChildHealthService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface SaveCallback {
        void onSuccess(String documentId);
        void onError(String error);
    }

    public interface HealthDataCallback {
        void onSuccess(List<?> data);
        void onError(String error);
    }

    // Generic save method to eliminate duplication
    private void saveHealthLog(Object logObject, String collection, SaveCallback callback) {
        // Set timestamp for all log types
        long timestamp = System.currentTimeMillis();
        if (logObject instanceof MedicineLog) {
            ((MedicineLog) logObject).setTimestamp(timestamp);
        } else if (logObject instanceof SymptomLog) {
            ((SymptomLog) logObject).setTimestamp(timestamp);
        } else if (logObject instanceof DailyWellnessLog) {
            ((DailyWellnessLog) logObject).setTimestamp(timestamp);
        }
        
        db.collection(collection)
                .add(logObject)
                .addOnSuccessListener(documentReference -> {
                    String docId = documentReference.getId();
                    // Set the generated ID in the object
                    if (logObject instanceof MedicineLog) {
                        ((MedicineLog) logObject).setLogId(docId);
                    } else if (logObject instanceof SymptomLog) {
                        ((SymptomLog) logObject).setLogId(docId);
                    } else if (logObject instanceof DailyWellnessLog) {
                        ((DailyWellnessLog) logObject).setLogId(docId);
                    }
                    
                    // Update document with the ID
                    documentReference.update("logId", docId)
                            .addOnSuccessListener(aVoid -> callback.onSuccess(docId))
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Generic history retrieval method
    private <T> void getHealthHistory(String childId, int limitDays, String collection, Class<T> clazz, HealthDataCallback callback) {
        long cutoffTime = System.currentTimeMillis() - (limitDays * 24L * 60L * 60L * 1000L);
        
        db.collection(collection)
                .whereEqualTo("childId", childId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<T> logs = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Filter by timestamp in code to avoid composite index requirement
                            Long timestamp = document.getLong("timestamp");
                            if (timestamp != null && timestamp >= cutoffTime) {
                                T log = document.toObject(clazz);
                                logs.add(log);
                            }
                        }
                        callback.onSuccess(logs);
                    } else {
                        callback.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    // Medicine Log Operations
    public void saveMedicineLog(MedicineLog medicineLog, SaveCallback callback) {
        saveHealthLog(medicineLog, "medicineLog", callback);
    }

    public void getMedicineHistory(String childId, int limitDays, HealthDataCallback callback) {
        getHealthHistory(childId, limitDays, "medicineLog", MedicineLog.class, callback);
    }

    // Symptom Log Operations
    public void saveSymptomLog(SymptomLog symptomLog, SaveCallback callback) {
        saveHealthLog(symptomLog, "symptomLog", callback);
    }

    public void getSymptomHistory(String childId, int limitDays, HealthDataCallback callback) {
        getHealthHistory(childId, limitDays, "symptomLog", SymptomLog.class, callback);
    }

    // Daily Wellness Log Operations
    public void saveDailyWellnessLog(DailyWellnessLog wellnessLog, SaveCallback callback) {
        saveHealthLog(wellnessLog, "dailyWellnessLog", callback);
    }

    public void getDailyWellnessHistory(String childId, int limitDays, HealthDataCallback callback) {
        getHealthHistory(childId, limitDays, "dailyWellnessLog", DailyWellnessLog.class, callback);
    }

    // Check if daily check-in has been completed today
    public void hasDailyCheckInToday(String childId, HealthDataCallback callback) {
        long startOfDay = getStartOfDay(System.currentTimeMillis());
        long endOfDay = startOfDay + 24L * 60L * 60L * 1000L;
        
        db.collection("dailyWellnessLog")
                .whereEqualTo("childId", childId)
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .whereLessThan("timestamp", endOfDay)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Boolean> result = new ArrayList<>();
                        result.add(!task.getResult().isEmpty());
                        callback.onSuccess(result);
                    } else {
                        callback.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    private long getStartOfDay(long timestamp) {
        return timestamp - (timestamp % (24L * 60L * 60L * 1000L));
    }

    // Get recent rescue inhaler usage count (last 24 hours)
    public void getRecentRescueInhalerCount(String childId, HealthDataCallback callback) {
        long last24Hours = System.currentTimeMillis() - (24L * 60L * 60L * 1000L);
        
        db.collection("medicineLog")
                .whereEqualTo("childId", childId)
                .whereEqualTo("medicineType", "rescue")
                .whereGreaterThan("timestamp", last24Hours)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Integer> result = new ArrayList<>();
                        result.add(task.getResult().size());
                        callback.onSuccess(result);
                    } else {
                        callback.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    // Get all health data for a child (for provider access)
    public void getAllHealthData(String childId, int limitDays, AllHealthDataCallback callback) {
        // Get all three types of data using the consolidated methods
        getMedicineHistory(childId, limitDays, new HealthDataCallback() {
            @Override
            public void onSuccess(List<?> medicineData) {
                getSymptomHistory(childId, limitDays, new HealthDataCallback() {
                    @Override
                    public void onSuccess(List<?> symptomData) {
                        getDailyWellnessHistory(childId, limitDays, new HealthDataCallback() {
                            @Override
                            public void onSuccess(List<?> wellnessData) {
                                callback.onSuccess((List<MedicineLog>) medicineData, 
                                                 (List<SymptomLog>) symptomData, 
                                                 (List<DailyWellnessLog>) wellnessData);
                            }
                            
                            @Override
                            public void onError(String error) {
                                callback.onError(error);
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public interface AllHealthDataCallback {
        void onSuccess(List<MedicineLog> medicineData, List<SymptomLog> symptomData, List<DailyWellnessLog> wellnessData);
        void onError(String error);
    }
}