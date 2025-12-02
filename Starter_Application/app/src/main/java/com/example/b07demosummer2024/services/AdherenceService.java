package com.example.b07demosummer2024.services;

import com.example.b07demosummer2024.models.AdherenceSnapshot;
import com.example.b07demosummer2024.models.DailyWellnessLog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdherenceService {
    private FirebaseFirestore db;

    public AdherenceService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface AdherenceCallback {
        void onSuccess(AdherenceSnapshot snapshot);
        void onError(String error);
    }

    public interface AdherenceHistoryCallback {
        void onSuccess(List<AdherenceSnapshot> snapshots);
        void onError(String error);
    }

    public interface CurrentAdherenceCallback {
        void onSuccess(double adherence, int plannedDays, int loggedDays);
        void onError(String error);
    }

    public interface ScheduleChangeCallback {
        void onSuccess();
        void onError(String error);
    }

    public void handleScheduleChange(String childId, Map<String, Boolean> newSchedule, ScheduleChangeCallback callback) {
        db.collection("users").document(childId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot doc = task.getResult();
                Map<String, Object> currentScheduleRaw = (Map<String, Object>) doc.get("weeklySchedule");
                Long adherencePeriodStart = doc.getLong("adherencePeriodStart");
                
                Map<String, Boolean> currentSchedule = new HashMap<>();
                if (currentScheduleRaw != null) {
                    for (int i = 1; i <= 7; i++) {
                        Object val = currentScheduleRaw.get(String.valueOf(i));
                        currentSchedule.put(String.valueOf(i), Boolean.TRUE.equals(val));
                    }
                }
                
                boolean scheduleChanged = !schedulesEqual(currentSchedule, newSchedule);
                long currentTime = System.currentTimeMillis();
                
                if (scheduleChanged && adherencePeriodStart != null && adherencePeriodStart > 0) {
                    calculateAndStoreAdherenceSnapshot(childId, adherencePeriodStart, currentTime, 
                            currentSchedule, new AdherenceCallback() {
                        @Override
                        public void onSuccess(AdherenceSnapshot snapshot) {
                            updateUserSchedule(childId, newSchedule, currentTime, callback);
                        }
                        
                        @Override
                        public void onError(String error) {
                            callback.onError("Failed to calculate adherence: " + error);
                        }
                    });
                } else {
                    if (adherencePeriodStart == null || adherencePeriodStart == 0) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(currentTime);
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        adherencePeriodStart = cal.getTimeInMillis();
                    }
                    updateUserSchedule(childId, newSchedule, adherencePeriodStart, callback);
                }
            } else {
                callback.onError("Failed to load user data");
            }
        });
    }

    private boolean schedulesEqual(Map<String, Boolean> schedule1, Map<String, Boolean> schedule2) {
        if (schedule1 == null && schedule2 == null) return true;
        if (schedule1 == null || schedule2 == null) return false;
        if (schedule1.size() != schedule2.size()) return false;
        
        for (int i = 1; i <= 7; i++) {
            String key = String.valueOf(i);
            Boolean val1 = schedule1.get(key);
            Boolean val2 = schedule2.get(key);
            if (val1 == null && val2 == null) continue;
            if (val1 == null || val2 == null) return false;
            if (!val1.equals(val2)) return false;
        }
        return true;
    }

    private void updateUserSchedule(String childId, Map<String, Boolean> newSchedule, long periodStart, ScheduleChangeCallback callback) {
        Map<String, Object> update = new HashMap<>();
        Map<String, Object> scheduleMap = new HashMap<>();
        for (Map.Entry<String, Boolean> entry : newSchedule.entrySet()) {
            scheduleMap.put(entry.getKey(), entry.getValue());
        }
        update.put("weeklySchedule", scheduleMap);
        update.put("lastScheduleChange", System.currentTimeMillis());
        update.put("adherencePeriodStart", periodStart);
        
        db.collection("users").document(childId).set(update, SetOptions.merge())
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void calculateAndStoreAdherenceSnapshot(String childId, long periodStart, long periodEnd,
                                                  Map<String, Boolean> scheduleConfig, AdherenceCallback callback) {
        countPlannedDays(periodStart, periodEnd, scheduleConfig, plannedDays -> {
            final int finalPlannedDays = plannedDays;
            countLoggedDays(childId, periodStart, periodEnd, new LoggedDaysCallback() {
                @Override
                public void onSuccess(int loggedDays) {
                    double adherence = 0.0;
                    if (finalPlannedDays > 0) {
                        adherence = (loggedDays * 100.0) / finalPlannedDays;
                    }
                    
                    android.util.Log.d("AdherenceService", "Calculating adherence: loggedDays=" + loggedDays + ", plannedDays=" + finalPlannedDays + ", adherence=" + adherence);
                    
                    AdherenceSnapshot snapshot = new AdherenceSnapshot();
                    snapshot.setChildId(childId);
                    snapshot.setPeriodStart(periodStart);
                    snapshot.setPeriodEnd(periodEnd);
                    snapshot.setScheduleConfig(scheduleConfig);
                    snapshot.setPlannedDays(finalPlannedDays);
                    snapshot.setLoggedDays(loggedDays);
                    snapshot.setAdherence(adherence);
                    snapshot.setCalculatedAt(System.currentTimeMillis());
                    
                    db.collection("adherenceSnapshots").add(snapshot)
                        .addOnSuccessListener(documentReference -> {
                            snapshot.setSnapshotId(documentReference.getId());
                            documentReference.update("snapshotId", documentReference.getId())
                                .addOnSuccessListener(aVoid -> callback.onSuccess(snapshot))
                                .addOnFailureListener(e -> callback.onError(e.getMessage()));
                        })
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                }
                
                @Override
                public void onError(String error) {
                    callback.onError(error);
                }
            });
        });
    }

    private void countPlannedDays(long periodStart, long periodEnd, Map<String, Boolean> scheduleConfig,
                                  PlannedDaysCallback callback) {
        Calendar periodStartCal = Calendar.getInstance();
        periodStartCal.setTimeInMillis(periodStart);
        periodStartCal.set(Calendar.HOUR_OF_DAY, 0);
        periodStartCal.set(Calendar.MINUTE, 0);
        periodStartCal.set(Calendar.SECOND, 0);
        periodStartCal.set(Calendar.MILLISECOND, 0);
        long normalizedPeriodStart = periodStartCal.getTimeInMillis();
        
        if (periodEnd < normalizedPeriodStart) {
            callback.onSuccess(0);
            return;
        }
        
        Calendar cal = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        cal.setTimeInMillis(normalizedPeriodStart);
        end.setTimeInMillis(periodEnd);
        
        int count = 0;
        while (!cal.after(end)) {
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            Boolean scheduled = scheduleConfig.get(String.valueOf(dayOfWeek));
            if (scheduled != null && scheduled) {
                count++;
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        callback.onSuccess(count);
    }

    private void countLoggedDays(String childId, long periodStart, long periodEnd, LoggedDaysCallback callback) {
        final Set<Long> uniqueDays = new HashSet<>();
        final int[] completed = {0};
        final int total = 2;
        
        Calendar periodStartCal = Calendar.getInstance();
        periodStartCal.setTimeInMillis(periodStart);
        periodStartCal.set(Calendar.HOUR_OF_DAY, 0);
        periodStartCal.set(Calendar.MINUTE, 0);
        periodStartCal.set(Calendar.SECOND, 0);
        periodStartCal.set(Calendar.MILLISECOND, 0);
        final long normalizedPeriodStart = periodStartCal.getTimeInMillis();
        
        android.util.Log.d("AdherenceService", "Counting logged days: periodStart=" + periodStart + " (normalized=" + normalizedPeriodStart + "), periodEnd=" + periodEnd);
        
        db.collection("dailyWellnessLog")
            .whereEqualTo("childId", childId)
            .get()
            .addOnCompleteListener(task -> {
                int wellnessCount = 0;
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Long timestamp = document.getLong("timestamp");
                        if (timestamp != null && timestamp >= normalizedPeriodStart && timestamp <= periodEnd) {
                            DailyWellnessLog log = document.toObject(DailyWellnessLog.class);
                            
                            boolean tookController = log.isMorningController() || log.isEveningController();
                            
                            if (tookController) {
                                wellnessCount++;
                                Calendar dayCal = Calendar.getInstance();
                                dayCal.setTimeInMillis(timestamp);
                                dayCal.set(Calendar.HOUR_OF_DAY, 0);
                                dayCal.set(Calendar.MINUTE, 0);
                                dayCal.set(Calendar.SECOND, 0);
                                dayCal.set(Calendar.MILLISECOND, 0);
                                long dayStart = dayCal.getTimeInMillis();
                                
                                uniqueDays.add(dayStart);
                                android.util.Log.d("AdherenceService", "Added wellness log day: " + dayStart + " from timestamp " + timestamp);
                            }
                        }
                    }
                    android.util.Log.d("AdherenceService", "Found " + wellnessCount + " wellness logs with controller in period");
                } else {
                    android.util.Log.e("AdherenceService", "Error loading dailyWellnessLog: " + (task.getException() != null ? task.getException().getMessage() : "unknown"));
                }
                synchronized (completed) {
                    completed[0]++;
                    if (completed[0] == total) {
                        int loggedDays = uniqueDays.size();
                        android.util.Log.d("AdherenceService", "Total unique logged days: " + loggedDays);
                        callback.onSuccess(loggedDays);
                    }
                }
            });
        
        db.collection("medicineLog")
            .whereEqualTo("childId", childId)
            .get()
            .addOnCompleteListener(task -> {
                int found = 0;
                int totalLogs = 0;
                if (task.isSuccessful()) {
                    totalLogs = task.getResult().size();
                    android.util.Log.d("AdherenceService", "Total medicine logs for child: " + totalLogs);
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String medicineType = document.getString("medicineType");
                        Long timestamp = document.getLong("timestamp");
                        android.util.Log.d("AdherenceService", "Medicine log: type=" + medicineType + ", timestamp=" + timestamp);
                        if (medicineType != null && ("controller".equalsIgnoreCase(medicineType) || "Controller".equals(medicineType))) {
                            if (timestamp != null) {
                                android.util.Log.d("AdherenceService", "Controller log timestamp " + timestamp + " in range? " + (timestamp >= normalizedPeriodStart && timestamp <= periodEnd));
                                if (timestamp >= normalizedPeriodStart && timestamp <= periodEnd) {
                                    found++;
                                    Calendar dayCal = Calendar.getInstance();
                                    dayCal.setTimeInMillis(timestamp);
                                    dayCal.set(Calendar.HOUR_OF_DAY, 0);
                                    dayCal.set(Calendar.MINUTE, 0);
                                    dayCal.set(Calendar.SECOND, 0);
                                    dayCal.set(Calendar.MILLISECOND, 0);
                                    long dayStart = dayCal.getTimeInMillis();
                                    
                                    uniqueDays.add(dayStart);
                                    android.util.Log.d("AdherenceService", "Added medicine log day: " + dayStart + " from timestamp " + timestamp);
                                }
                            }
                        }
                    }
                    android.util.Log.d("AdherenceService", "Found " + found + " controller medicine logs in period (out of " + totalLogs + " total)");
                } else {
                    android.util.Log.e("AdherenceService", "Error loading medicineLog: " + (task.getException() != null ? task.getException().getMessage() : "unknown"));
                }
                synchronized (completed) {
                    completed[0]++;
                    if (completed[0] == total) {
                        int loggedDays = uniqueDays.size();
                        android.util.Log.d("AdherenceService", "Total unique logged days: " + loggedDays);
                        callback.onSuccess(loggedDays);
                    }
                }
            });
    }

    public void getHistoricalAdherence(String childId, AdherenceHistoryCallback callback) {
        db.collection("adherenceSnapshots")
            .whereEqualTo("childId", childId)
            .orderBy("periodEnd", Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<AdherenceSnapshot> snapshots = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        AdherenceSnapshot snapshot = document.toObject(AdherenceSnapshot.class);
                        snapshot.setSnapshotId(document.getId());
                        snapshots.add(snapshot);
                    }
                    callback.onSuccess(snapshots);
                } else {
                    callback.onError(task.getException() != null ? 
                        task.getException().getMessage() : "Unknown error");
                }
            });
    }

    public void getCurrentPeriodAdherence(String childId, CurrentAdherenceCallback callback) {
        db.collection("users").document(childId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot doc = task.getResult();
                Long periodStart = doc.getLong("adherencePeriodStart");
                Map<String, Object> scheduleRaw = (Map<String, Object>) doc.get("weeklySchedule");
                
                android.util.Log.d("AdherenceService", "getCurrentPeriodAdherence: periodStart=" + periodStart);
                
                if (periodStart == null || periodStart == 0) {
                    android.util.Log.e("AdherenceService", "No adherence period start found");
                    callback.onError("No adherence period start found");
                    return;
                }
                
                Map<String, Boolean> schedule = new HashMap<>();
                if (scheduleRaw != null) {
                    for (int i = 1; i <= 7; i++) {
                        Object val = scheduleRaw.get(String.valueOf(i));
                        schedule.put(String.valueOf(i), Boolean.TRUE.equals(val));
                    }
                } else {
                    for (int i = 1; i <= 7; i++) {
                        schedule.put(String.valueOf(i), true);
                    }
                }
                
                long periodEnd = System.currentTimeMillis();
                android.util.Log.d("AdherenceService", "Current period: " + periodStart + " to " + periodEnd);
                countPlannedDays(periodStart, periodEnd, schedule, plannedDays -> {
                    android.util.Log.d("AdherenceService", "Planned days: " + plannedDays);
                    countLoggedDays(childId, periodStart, periodEnd, new LoggedDaysCallback() {
                        @Override
                        public void onSuccess(int loggedDays) {
                            double adherence = 0.0;
                            if (plannedDays > 0) {
                                adherence = (loggedDays * 100.0) / plannedDays;
                            }
                            android.util.Log.d("AdherenceService", "Final: loggedDays=" + loggedDays + ", plannedDays=" + plannedDays + ", adherence=" + adherence);
                            callback.onSuccess(adherence, plannedDays, loggedDays);
                        }
                        
                        @Override
                        public void onError(String error) {
                            android.util.Log.e("AdherenceService", "Error counting logged days: " + error);
                            callback.onError(error);
                        }
                    });
                });
            } else {
                callback.onError("Failed to load user data");
            }
        });
    }

    private interface PlannedDaysCallback {
        void onSuccess(int plannedDays);
    }

    private interface LoggedDaysCallback {
        void onSuccess(int loggedDays);
        void onError(String error);
    }
}
