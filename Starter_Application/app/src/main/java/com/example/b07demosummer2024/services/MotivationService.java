package com.example.b07demosummer2024.services;

import com.example.b07demosummer2024.models.Badge;
import com.example.b07demosummer2024.models.Streak;
import com.example.b07demosummer2024.models.MotivationSettings;
import com.example.b07demosummer2024.models.MedicineLog;
import com.example.b07demosummer2024.models.DailyWellnessLog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Locale;

public class MotivationService {
    private static final String COLLECTION_STREAKS = "streaks";
    private static final String COLLECTION_BADGES = "badges";
    private static final String COLLECTION_MOTIVATION_SETTINGS = "motivation_settings";
    
    private final FirebaseFirestore db;

    public MotivationService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface MotivationCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface StreakCallback {
        void onStreaksLoaded(List<Streak> streaks);
        void onError(String error);
    }

    public interface BadgeCallback {
        void onBadgesLoaded(List<Badge> badges);
        void onError(String error);
    }

    public interface SettingsCallback {
        void onSettingsLoaded(MotivationSettings settings);
        void onError(String error);
    }

    // Initialize default streaks and badges for a new child
    public void initializeMotivationForChild(String childId, MotivationCallback callback) {
        // Create default settings
        MotivationSettings settings = new MotivationSettings(childId);
        saveMotivationSettings(settings, new MotivationCallback() {
            @Override
            public void onSuccess(String message) {
                // Initialize default streaks
                initializeStreaks(childId, settings, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError("Failed to initialize settings: " + error);
            }
        });
    }

    private void initializeStreaks(String childId, MotivationSettings settings, MotivationCallback callback) {
        List<Streak> defaultStreaks = new ArrayList<>();
        
        // Controller streak
        Streak controllerStreak = new Streak(
            childId + "_controller_streak", 
            childId, 
            "controller_planned"
        );
        defaultStreaks.add(controllerStreak);
        
        // Technique streak
        Streak techniqueStreak = new Streak(
            childId + "_technique_streak", 
            childId, 
            "technique_completed"
        );
        defaultStreaks.add(techniqueStreak);

        // Save streaks
        saveStreaks(defaultStreaks, new MotivationCallback() {
            @Override
            public void onSuccess(String message) {
                initializeBadges(childId, settings, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError("Failed to initialize streaks: " + error);
            }
        });
    }

    private void initializeBadges(String childId, MotivationSettings settings, MotivationCallback callback) {
        List<Badge> defaultBadges = new ArrayList<>();

        // Perfect Controller Week Badge
        Badge perfectWeekBadge = new Badge(
            childId + "_perfect_week",
            childId,
            "perfect_controller_week",
            "Perfect Week Champion",
            "Take your controller medicine every day for " + settings.getPerfectControllerWeekDays() + " days straight!",
            settings.getPerfectControllerWeekDays()
        );
        defaultBadges.add(perfectWeekBadge);

        // Technique Master Badge
        Badge techniqueBadge = new Badge(
            childId + "_technique_master",
            childId,
            "technique_master",
            "Technique Master",
            "Complete " + settings.getTechniqueMasterSessions() + " high-quality breathing technique sessions!",
            settings.getTechniqueMasterSessions()
        );
        defaultBadges.add(techniqueBadge);

        // Low Rescue Month Badge
        Badge lowRescueBadge = new Badge(
            childId + "_low_rescue_month",
            childId,
            "low_rescue_month",
            "Steady Breather",
            "Use your rescue inhaler ≤" + settings.getLowRescueMonthLimit() + " days in a " + settings.getLowRescueMonthDays() + "-day period!",
            1 // Target is 1 (meaning achieved the goal once)
        );
        defaultBadges.add(lowRescueBadge);

        saveBadges(defaultBadges, callback);
    }

    // Save multiple streaks
    private void saveStreaks(List<Streak> streaks, MotivationCallback callback) {
        if (streaks.isEmpty()) {
            callback.onSuccess("No streaks to save");
            return;
        }

        for (int i = 0; i < streaks.size(); i++) {
            final boolean isLast = (i == streaks.size() - 1);
            Streak streak = streaks.get(i);
            
            db.collection(COLLECTION_STREAKS)
                .document(streak.getStreakId())
                .set(streak)
                .addOnSuccessListener(aVoid -> {
                    if (isLast) {
                        callback.onSuccess("Streaks initialized successfully");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
        }
    }

    // Save multiple badges
    private void saveBadges(List<Badge> badges, MotivationCallback callback) {
        if (badges.isEmpty()) {
            callback.onSuccess("No badges to save");
            return;
        }

        for (int i = 0; i < badges.size(); i++) {
            final boolean isLast = (i == badges.size() - 1);
            Badge badge = badges.get(i);
            
            db.collection(COLLECTION_BADGES)
                .document(badge.getBadgeId())
                .set(badge)
                .addOnSuccessListener(aVoid -> {
                    if (isLast) {
                        callback.onSuccess("Badges initialized successfully");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
        }
    }

    // Update streak based on medicine log
    public void updateControllerStreak(String childId, boolean tookController, MotivationCallback callback) {
        getStreakByType(childId, "controller_planned", new StreakCallback() {
            @Override
            public void onStreaksLoaded(List<Streak> streaks) {
                if (streaks.isEmpty()) {
                    // If a streak document doesn't exist yet for this child/type, create one
                    Streak newStreak = new Streak(childId + "_controller_streak", childId, "controller_planned");
                    newStreak.setCurrentCount(0);
                    newStreak.setBestCount(0);
                    newStreak.setLastUpdateDate(0);
                    saveStreak(newStreak, new MotivationCallback() {
                        @Override
                        public void onSuccess(String message) {
                            // retry the update now that the streak exists
                            updateControllerStreak(childId, tookController, callback);
                        }

                        @Override
                        public void onError(String error) {
                            callback.onError("Failed to create controller streak: " + error);
                        }
                    });
                    return;
                }

                Streak streak = streaks.get(0);
                long today = getTodayTimestamp();
                long daysSinceLastUpdate = getDaysDifference(streak.getLastUpdateDate(), today);

                if (tookController) {
                    // If we've already recorded a controller hit today, do nothing (only once per day)
                    if (getDaysDifference(streak.getLastUpdateDate(), today) == 0) {
                        // already counted today - nothing to do
                    } else if (getDaysDifference(streak.getLastUpdateDate(), today) == 1) {
                        // consecutive day -> increment
                        streak.incrementStreak();
                    } else {
                        // more than 1 day gap -> start new streak
                        streak.setCurrentCount(1);
                        streak.setLastUpdateDate(today);
                    }
                } else {
                    if (daysSinceLastUpdate > 1) {
                        streak.resetStreak();
                    }
                }

                saveStreak(streak, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError("Failed to load controller streak: " + error);
            }
        });
    }

    // Update technique streak based on technique completion
    public void updateTechniqueStreak(String childId, boolean completedTechnique, MotivationCallback callback) {
        getStreakByType(childId, "technique_completed", new StreakCallback() {
            @Override
            public void onStreaksLoaded(List<Streak> streaks) {
                if (streaks.isEmpty()) {
                    // Create a new technique streak if missing and retry
                    Streak newStreak = new Streak(childId + "_technique_streak", childId, "technique_completed");
                    newStreak.setCurrentCount(0);
                    newStreak.setBestCount(0);
                    newStreak.setLastUpdateDate(0);
                    saveStreak(newStreak, new MotivationCallback() {
                        @Override
                        public void onSuccess(String message) {
                            updateTechniqueStreak(childId, completedTechnique, callback);
                        }

                        @Override
                        public void onError(String error) {
                            callback.onError("Failed to create technique streak: " + error);
                        }
                    });
                    return;
                }

                Streak streak = streaks.get(0);
                long today = getTodayTimestamp();
                long daysSinceLastUpdate = getDaysDifference(streak.getLastUpdateDate(), today);

                if (completedTechnique) {
                    // Only count one technique-completed per day
                    if (getDaysDifference(streak.getLastUpdateDate(), today) == 0) {
                        // already counted today
                    } else if (getDaysDifference(streak.getLastUpdateDate(), today) == 1) {
                        // consecutive day -> increment
                        streak.incrementStreak();
                    } else {
                        // start new streak
                        streak.setCurrentCount(1);
                        streak.setLastUpdateDate(today);
                    }
                } else {
                    if (daysSinceLastUpdate > 1) {
                        streak.resetStreak();
                    }
                }

                saveStreak(streak, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError("Failed to load technique streak: " + error);
            }
        });
    }

    // Check and update badge progress
    public void checkBadgeProgress(String childId, MotivationCallback callback) {
        getMotivationSettings(childId, new SettingsCallback() {
            @Override
            public void onSettingsLoaded(MotivationSettings settings) {
                checkPerfectControllerWeekBadge(childId, settings, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError("Failed to load motivation settings: " + error);
            }
        });
    }

    private void checkPerfectControllerWeekBadge(String childId, MotivationSettings settings, MotivationCallback callback) {
        getStreakByType(childId, "controller_planned", new StreakCallback() {
            @Override
            public void onStreaksLoaded(List<Streak> streaks) {
                if (!streaks.isEmpty()) {
                    Streak controllerStreak = streaks.get(0);
                    if (controllerStreak.getCurrentCount() >= settings.getPerfectControllerWeekDays()) {
                        updateBadgeProgress(childId, "perfect_controller_week", 1, callback);
                    } else {
                        checkTechniqueMasterBadge(childId, settings, callback);
                    }
                } else {
                    checkTechniqueMasterBadge(childId, settings, callback);
                }
            }

            @Override
            public void onError(String error) {
                checkTechniqueMasterBadge(childId, settings, callback);
            }
        });
    }

    private void checkTechniqueMasterBadge(String childId, MotivationSettings settings, MotivationCallback callback) {
        // Count high-quality technique sessions (this would need to be tracked in your app)
        // For now, we'll use technique streak as a proxy
        getStreakByType(childId, "technique_completed", new StreakCallback() {
            @Override
            public void onStreaksLoaded(List<Streak> streaks) {
                if (!streaks.isEmpty()) {
                    Streak techniqueStreak = streaks.get(0);
                    if (techniqueStreak.getBestCount() >= settings.getTechniqueMasterSessions()) {
                        updateBadgeProgress(childId, "technique_master", settings.getTechniqueMasterSessions(), callback);
                    } else {
                        checkLowRescueMonthBadge(childId, settings, callback);
                    }
                } else {
                    checkLowRescueMonthBadge(childId, settings, callback);
                }
            }

            @Override
            public void onError(String error) {
                checkLowRescueMonthBadge(childId, settings, callback);
            }
        });
    }

    private void checkLowRescueMonthBadge(String childId, MotivationSettings settings, MotivationCallback callback) {
        // Count rescue inhaler days in the last month
        long thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(settings.getLowRescueMonthDays());
        
        db.collection("medicine_logs")
            .whereEqualTo("childId", childId)
            .whereEqualTo("medicineType", "rescue")
            .whereGreaterThan("timestamp", thirtyDaysAgo)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                int rescueDays = querySnapshot.size(); // Simplified - each log = one day
                if (rescueDays <= settings.getLowRescueMonthLimit()) {
                    updateBadgeProgress(childId, "low_rescue_month", 1, callback);
                } else {
                    callback.onSuccess("Badge progress checked");
                }
            })
            .addOnFailureListener(e -> callback.onError("Failed to check rescue usage: " + e.getMessage()));
    }

    private void updateBadgeProgress(String childId, String badgeType, int progress, MotivationCallback callback) {
        getBadgeByType(childId, badgeType, new BadgeCallback() {
            @Override
            public void onBadgesLoaded(List<Badge> badges) {
                if (!badges.isEmpty()) {
                    Badge badge = badges.get(0);
                    if (!badge.isUnlocked()) {
                        badge.setProgress(progress);
                        saveBadge(badge, callback);
                    } else {
                        callback.onSuccess("Badge already unlocked");
                    }
                } else {
                    callback.onError("Badge not found");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError("Failed to load badge: " + error);
            }
        });
    }

    // Get all streaks for a child
    public void getChildStreaks(String childId, StreakCallback callback) {
        db.collection(COLLECTION_STREAKS)
            .whereEqualTo("childId", childId)
            .whereEqualTo("active", true)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Streak> streaks = new ArrayList<>();
                for (QueryDocumentSnapshot document : querySnapshot) {
                    Streak streak = document.toObject(Streak.class);
                    streaks.add(streak);
                }
                callback.onStreaksLoaded(streaks);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Get all badges for a child
    public void getChildBadges(String childId, BadgeCallback callback) {
        db.collection(COLLECTION_BADGES)
            .whereEqualTo("childId", childId)
            .orderBy("earnedDate")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Badge> badges = new ArrayList<>();
                for (QueryDocumentSnapshot document : querySnapshot) {
                    Badge badge = document.toObject(Badge.class);
                    badges.add(badge);
                }
                callback.onBadgesLoaded(badges);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Get motivation settings
    public void getMotivationSettings(String childId, SettingsCallback callback) {
        db.collection(COLLECTION_MOTIVATION_SETTINGS)
            .document(childId + "_motivation_settings")
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    MotivationSettings settings = document.toObject(MotivationSettings.class);
                    callback.onSettingsLoaded(settings);
                } else {
                    // Create default settings
                    MotivationSettings defaultSettings = new MotivationSettings(childId);
                    saveMotivationSettings(defaultSettings, new MotivationCallback() {
                        @Override
                        public void onSuccess(String message) {
                            callback.onSettingsLoaded(defaultSettings);
                        }

                        @Override
                        public void onError(String error) {
                            callback.onError(error);
                        }
                    });
                }
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Save motivation settings
    public void saveMotivationSettings(MotivationSettings settings, MotivationCallback callback) {
        db.collection(COLLECTION_MOTIVATION_SETTINGS)
            .document(settings.getSettingsId())
            .set(settings)
            .addOnSuccessListener(aVoid -> callback.onSuccess("Settings saved successfully"))
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Helper methods
    private void getStreakByType(String childId, String streakType, StreakCallback callback) {
        db.collection(COLLECTION_STREAKS)
            .whereEqualTo("childId", childId)
            .whereEqualTo("streakType", streakType)
            .whereEqualTo("active", true)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Streak> streaks = new ArrayList<>();
                for (QueryDocumentSnapshot document : querySnapshot) {
                    Streak streak = document.toObject(Streak.class);
                    streaks.add(streak);
                }
                callback.onStreaksLoaded(streaks);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void getBadgeByType(String childId, String badgeType, BadgeCallback callback) {
        db.collection(COLLECTION_BADGES)
            .whereEqualTo("childId", childId)
            .whereEqualTo("badgeType", badgeType)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Badge> badges = new ArrayList<>();
                for (QueryDocumentSnapshot document : querySnapshot) {
                    Badge badge = document.toObject(Badge.class);
                    badges.add(badge);
                }
                callback.onBadgesLoaded(badges);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void saveStreak(Streak streak, MotivationCallback callback) {
        db.collection(COLLECTION_STREAKS)
            .document(streak.getStreakId())
            .set(streak)
            .addOnSuccessListener(aVoid -> {
                // Only return celebration message for significant milestones
                if (streak.getCurrentCount() > 0 && (streak.getCurrentCount() % 7 == 0 || streak.getCurrentCount() == streak.getBestCount())) {
                    callback.onSuccess("🎉 " + streak.getCurrentCount() + " day streak!");
                } else {
                    callback.onSuccess(""); // Empty success message for regular updates
                }
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void saveBadge(Badge badge, MotivationCallback callback) {
        db.collection(COLLECTION_BADGES)
            .document(badge.getBadgeId())
            .set(badge)
            .addOnSuccessListener(aVoid -> {
                if (badge.isUnlocked()) {
                    callback.onSuccess("🎉 Badge earned: " + badge.getTitle() + "!");
                } else {
                    callback.onSuccess(""); // Empty success message for progress updates
                }
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private long getTodayTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getDaysDifference(long timestamp1, long timestamp2) {
        return TimeUnit.MILLISECONDS.toDays(Math.abs(timestamp2 - timestamp1));
    }

    // New methods to calculate motivation data from actual health logs
    public void calculateStreaksFromLogs(String childId) {
        // Calculate controller streak from medicine logs
        calculateControllerStreakFromLogs(childId);
        // Calculate technique streak from breathing technique logs
        calculateTechniqueStreakFromLogs(childId);
    }

    private void calculateControllerStreakFromLogs(String childId) {
        db.collection("medicineLog")
                .whereEqualTo("childId", childId)
                .whereEqualTo("medicineType", "controller")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<DocumentSnapshot> logs = task.getResult().getDocuments();
                        
                        android.util.Log.d("MotivationService", "Found " + logs.size() + " controller medicine logs for child: " + childId);
                        
                        int currentStreak = 0;
                        int bestStreak = 0;
                        
                        // Group logs by day - only count 1 per day
                        Map<String, Boolean> dayHasController = new HashMap<>();
                        
                        for (DocumentSnapshot log : logs) {
                            Long timestamp = log.getLong("timestamp");
                            
                            if (timestamp != null) {
                                String dayKey = getDayKey(timestamp);
                                dayHasController.put(dayKey, true); // Only 1 per day
                                android.util.Log.d("MotivationService", "Added controller day: " + dayKey);
                            }
                        }
                        
                        // Calculate streak from sorted days
                        List<String> sortedDays = new ArrayList<>(dayHasController.keySet());
                        Collections.sort(sortedDays, Collections.reverseOrder());
                        
                        // Get today's date key for comparison
                        String todayKey = getDayKey(System.currentTimeMillis());
                        
                        // Only count as current streak if it includes today
                        if (!sortedDays.isEmpty() && sortedDays.get(0).equals(todayKey)) {
                            currentStreak = 1;
                            bestStreak = 1;
                            
                            // Check backwards from today for consecutive days
                            for (int i = 1; i < sortedDays.size(); i++) {
                                if (areConsecutiveDays(sortedDays.get(i), sortedDays.get(i-1))) {
                                    currentStreak++;
                                } else {
                                    break; // Streak is broken
                                }
                            }
                        } else {
                            currentStreak = 0; // No current streak if today is not included
                        }
                        
                        // Calculate best streak from all data
                        int tempStreak = 0;
                        for (int i = 0; i < sortedDays.size(); i++) {
                            if (i == 0) {
                                tempStreak = 1;
                            } else {
                                if (areConsecutiveDays(sortedDays.get(i), sortedDays.get(i-1))) {
                                    tempStreak++;
                                } else {
                                    bestStreak = Math.max(bestStreak, tempStreak);
                                    tempStreak = 1;
                                }
                            }
                        }
                        bestStreak = Math.max(bestStreak, tempStreak);
                        
                        android.util.Log.d("MotivationService", "Controller streak calculation: current=" + currentStreak + ", best=" + bestStreak + ", days=" + dayHasController.keySet());
                        
                        // Update the controller streak
                        updateCalculatedStreak(childId, "controller_planned", currentStreak, bestStreak);
                    }
                });
    }

    private void calculateTechniqueStreakFromLogs(String childId) {
        db.collection("techniqueLogs")
                .whereEqualTo("childId", childId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<DocumentSnapshot> logs = task.getResult().getDocuments();
                        
                        android.util.Log.d("MotivationService", "Found " + logs.size() + " technique logs for child: " + childId);
                        
                        int currentStreak = 0;
                        int bestStreak = 0;
                        
                        // Group logs by day
                        Map<String, Boolean> dayHasTechnique = new HashMap<>();
                        
                        for (DocumentSnapshot log : logs) {
                            Long timestamp = log.getLong("timestamp");
                            
                            android.util.Log.d("MotivationService", "Technique log: timestamp=" + timestamp);
                            
                            if (timestamp != null) {
                                String dayKey = getDayKey(timestamp);
                                dayHasTechnique.put(dayKey, true);
                                android.util.Log.d("MotivationService", "Added technique day: " + dayKey);
                            }
                        }
                        
                        // Calculate streak from sorted days
                        List<String> sortedDays = new ArrayList<>(dayHasTechnique.keySet());
                        Collections.sort(sortedDays, Collections.reverseOrder());
                        
                        // Get today's date key for comparison
                        String todayKey = getDayKey(System.currentTimeMillis());
                        
                        // Only count as current streak if it includes today
                        if (!sortedDays.isEmpty() && sortedDays.get(0).equals(todayKey)) {
                            currentStreak = 1;
                            bestStreak = 1;
                            
                            // Check backwards from today for consecutive days
                            for (int i = 1; i < sortedDays.size(); i++) {
                                if (areConsecutiveDays(sortedDays.get(i), sortedDays.get(i-1))) {
                                    currentStreak++;
                                } else {
                                    break; // Streak is broken
                                }
                            }
                        } else {
                            currentStreak = 0; // No current streak if today is not included
                        }
                        
                        // Calculate best streak from all data
                        int tempStreak = 0;
                        for (int i = 0; i < sortedDays.size(); i++) {
                            if (i == 0) {
                                tempStreak = 1;
                            } else {
                                if (areConsecutiveDays(sortedDays.get(i), sortedDays.get(i-1))) {
                                    tempStreak++;
                                } else {
                                    bestStreak = Math.max(bestStreak, tempStreak);
                                    tempStreak = 1;
                                }
                            }
                        }
                        bestStreak = Math.max(bestStreak, tempStreak);
                        
                        android.util.Log.d("MotivationService", "Technique streak calculation: current=" + currentStreak + ", best=" + bestStreak + ", days=" + dayHasTechnique.keySet());
                        
                        // Update the technique streak
                        updateCalculatedStreak(childId, "technique_completed", currentStreak, bestStreak);
                    }
                });
    }

    public void calculateBadgesFromLogs(String childId) {
        // Calculate perfect controller week badge
        calculatePerfectWeekBadge(childId);
        // Calculate technique mastery badge
        calculateTechniqueMasteryBadge(childId);
        // Calculate low rescue usage badge
        calculateLowRescueBadge(childId);
    }

    private void calculatePerfectWeekBadge(String childId) {
        // Check for perfect controller weeks (7 consecutive days with controller medicine)
        db.collection("medicineLogs")
                .whereEqualTo("childId", childId)
                .whereEqualTo("medicineType", "Controller")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<DocumentSnapshot> logs = task.getResult().getDocuments();
                        
                        Map<String, Boolean> dayHasController = new HashMap<>();
                        for (DocumentSnapshot log : logs) {
                            Long timestamp = log.getLong("timestamp");
                            if (timestamp != null) {
                                dayHasController.put(getDayKey(timestamp), true);
                            }
                        }
                        
                        List<String> sortedDays = new ArrayList<>(dayHasController.keySet());
                        Collections.sort(sortedDays);
                        
                        boolean hasWeeklyStreakGoal = false;
                        int consecutiveDays = 0;
                        
                        for (int i = 0; i < sortedDays.size(); i++) {
                            if (i == 0) {
                                consecutiveDays = 1;
                            } else {
                                if (areConsecutiveDays(sortedDays.get(i-1), sortedDays.get(i))) {
                                    consecutiveDays++;
                                } else {
                                    consecutiveDays = 1;
                                }
                            }
                            
                            if (consecutiveDays >= 7) {
                                hasWeeklyStreakGoal = true;
                                break;
                            }
                        }
                        
                        updateBadgeProgress(childId, "perfect_week", hasWeeklyStreakGoal ? 100 : 0, new MotivationCallback() {
                            @Override
                            public void onSuccess(String message) {
                                // Badge updated successfully
                            }

                            @Override
                            public void onError(String error) {
                                // Silent error
                            }
                        });
                    }
                });
    }

    private void calculateTechniqueMasteryBadge(String childId) {
        // Count completed breathing technique sessions
        db.collection("breathingTechniqueLogs")
                .whereEqualTo("childId", childId)
                .whereEqualTo("completed", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int completedSessions = task.getResult().size();
                        
                        // Get the threshold from settings (default 10)
                        getMotivationSettings(childId, new SettingsCallback() {
                            @Override
                            public void onSettingsLoaded(MotivationSettings settings) {
                                int threshold = settings.getTechniqueMasterSessions();
                                int progress = Math.min(100, (completedSessions * 100) / threshold);
                                updateBadgeProgress(childId, "technique_mastery", progress, new MotivationCallback() {
                                    @Override
                                    public void onSuccess(String message) {
                                        // Badge updated successfully
                                    }

                                    @Override
                                    public void onError(String error) {
                                        // Silent error
                                    }
                                });
                            }

                            @Override
                            public void onError(String error) {
                                // Silent error - use default threshold
                                int threshold = 10;
                                int progress = Math.min(100, (completedSessions * 100) / threshold);
                                updateBadgeProgress(childId, "technique_mastery", progress, new MotivationCallback() {
                                    @Override
                                    public void onSuccess(String message) {
                                        // Badge updated successfully
                                    }

                                    @Override
                                    public void onError(String error) {
                                        // Silent error
                                    }
                                });
                            }
                        });
                    }
                });
    }

    private void calculateLowRescueBadge(String childId) {
        // Check rescue medicine usage in the last 30 days
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        
        db.collection("medicineLogs")
                .whereEqualTo("childId", childId)
                .whereEqualTo("medicineType", "Rescue")
                .whereGreaterThan("timestamp", thirtyDaysAgo)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int rescueDays = task.getResult().size();
                        
                        // Get the threshold from settings (default 4)
                        getMotivationSettings(childId, new SettingsCallback() {
                            @Override
                            public void onSettingsLoaded(MotivationSettings settings) {
                                int threshold = settings.getLowRescueMonthLimit();
                                boolean qualifies = rescueDays <= threshold;
                                updateBadgeProgress(childId, "low_rescue", qualifies ? 100 : 0, new MotivationCallback() {
                                    @Override
                                    public void onSuccess(String message) {
                                        // Badge updated successfully
                                    }

                                    @Override
                                    public void onError(String error) {
                                        // Silent error
                                    }
                                });
                            }

                            @Override
                            public void onError(String error) {
                                // Silent error - use default threshold
                                int threshold = 4;
                                boolean qualifies = rescueDays <= threshold;
                                updateBadgeProgress(childId, "low_rescue", qualifies ? 100 : 0, new MotivationCallback() {
                                    @Override
                                    public void onSuccess(String message) {
                                        // Badge updated successfully
                                    }

                                    @Override
                                    public void onError(String error) {
                                        // Silent error
                                    }
                                });
                            }
                        });
                    }
                });
    }

    private String getDayKey(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return String.format(Locale.getDefault(), "%d-%02d-%02d", 
                calendar.get(Calendar.YEAR), 
                calendar.get(Calendar.MONTH) + 1, 
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    private boolean areConsecutiveDays(String day1, String day2) {
        try {
            String[] parts1 = day1.split("-");
            String[] parts2 = day2.split("-");
            
            Calendar cal1 = Calendar.getInstance();
            cal1.set(Integer.parseInt(parts1[0]), Integer.parseInt(parts1[1]) - 1, Integer.parseInt(parts1[2]));
            
            Calendar cal2 = Calendar.getInstance();
            cal2.set(Integer.parseInt(parts2[0]), Integer.parseInt(parts2[1]) - 1, Integer.parseInt(parts2[2]));
            
            long diffInDays = (cal2.getTimeInMillis() - cal1.getTimeInMillis()) / (24 * 60 * 60 * 1000);
            return Math.abs(diffInDays) == 1;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateCalculatedStreak(String childId, String streakType, int currentCount, int bestCount) {
        DocumentReference streakRef = db.collection("streaks")
                .document(childId + "_" + streakType);
        
        Map<String, Object> streakData = new HashMap<>();
        streakData.put("currentCount", currentCount);
        streakData.put("bestCount", bestCount);
        // Keep field name consistent with Streak model: lastUpdateDate
        streakData.put("lastUpdateDate", System.currentTimeMillis());
        
        streakRef.set(streakData, SetOptions.merge());
    }
}