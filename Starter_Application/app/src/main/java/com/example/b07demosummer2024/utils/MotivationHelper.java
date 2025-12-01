package com.example.b07demosummer2024.utils;

import android.content.Context;
import android.widget.Toast;
import com.example.b07demosummer2024.models.Badge;
import com.example.b07demosummer2024.models.Streak;
import com.example.b07demosummer2024.models.MotivationSettings;
import com.example.b07demosummer2024.services.MotivationService;
import java.util.List;

/**
 * Utility class to easily integrate motivation system into child activities
 * Usage examples for the ChildHomeFragment:
 * 
 * 1. Initialize for new users:
 *    MotivationHelper.initializeForNewChild(context, childId);
 * 
 * 2. Update streaks when controller medicine is taken:
 *    MotivationHelper.updateControllerStreak(context, childId, true);
 * 
 * 3. Update streaks when breathing technique is completed:
 *    MotivationHelper.updateTechniqueStreak(context, childId, true);
 * 
 * 4. Check for new badges earned:
 *    MotivationHelper.checkForNewBadges(context, childId);
 */
public class MotivationHelper {
    
    private static MotivationService motivationService = new MotivationService();

    /**
     * Initialize motivation system for a new child user
     */
    public static void initializeForNewChild(Context context, String childId) {
        motivationService.initializeMotivationForChild(childId, new MotivationService.MotivationCallback() {
            @Override
            public void onSuccess(String message) {
                // Silent success - motivation system ready
            }

            @Override
            public void onError(String error) {
                // Silent error - don't interrupt user experience
            }
        });
    }

    /**
     * Update controller medicine streak
     * Call this when child logs controller medicine
     */
    public static void updateControllerStreak(Context context, String childId, boolean tookMedicine) {
        motivationService.updateControllerStreak(childId, tookMedicine, new MotivationService.MotivationCallback() {
            @Override
            public void onSuccess(String message) {
                if (message.contains("Streak")) {
                    showToast(context, "🏆 " + message);
                }
                checkForNewBadges(context, childId);
            }

            @Override
            public void onError(String error) {
                // Silent error
            }
        });
    }

    /**
     * Update breathing technique streak
     * Call this when child completes a breathing technique session
     */
    public static void updateTechniqueStreak(Context context, String childId, boolean completedTechnique) {
        motivationService.updateTechniqueStreak(childId, completedTechnique, new MotivationService.MotivationCallback() {
            @Override
            public void onSuccess(String message) {
                if (message.contains("Streak")) {
                    showToast(context, "🧘‍♀️ " + message);
                }
                checkForNewBadges(context, childId);
            }

            @Override
            public void onError(String error) {
                // Silent error
            }
        });
    }

    /**
     * Check for newly earned badges and show celebration
     */
    public static void checkForNewBadges(Context context, String childId) {
        motivationService.checkBadgeProgress(childId, new MotivationService.MotivationCallback() {
            @Override
            public void onSuccess(String message) {
                if (message.contains("Badge earned")) {
                    showToast(context, "🎉 " + message);
                }
            }

            @Override
            public void onError(String error) {
                // Silent error
            }
        });
    }

    /**
     * Get current streaks for display
     */
    public static void getCurrentStreaks(String childId, StreakDisplayCallback callback) {
        motivationService.getChildStreaks(childId, new MotivationService.StreakCallback() {
            @Override
            public void onStreaksLoaded(List<Streak> streaks) {
                int controllerStreak = 0;
                int techniqueStreak = 0;
                
                for (Streak streak : streaks) {
                    if ("controller_planned".equals(streak.getStreakType())) {
                        controllerStreak = streak.getCurrentCount();
                    } else if ("technique_completed".equals(streak.getStreakType())) {
                        techniqueStreak = streak.getCurrentCount();
                    }
                }
                
                callback.onStreaksLoaded(controllerStreak, techniqueStreak);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Get badges for display
     */
    public static void getBadges(String childId, BadgeDisplayCallback callback) {
        motivationService.getChildBadges(childId, new MotivationService.BadgeCallback() {
            @Override
            public void onBadgesLoaded(List<Badge> badges) {
                callback.onBadgesLoaded(badges);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private static void showToast(Context context, String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }

    // Callback interfaces for UI updates
    public interface StreakDisplayCallback {
        void onStreaksLoaded(int controllerStreak, int techniqueStreak);
        void onError(String error);
    }

    public interface BadgeDisplayCallback {
        void onBadgesLoaded(List<Badge> badges);
        void onError(String error);
    }

    /**
     * Example usage in ChildHomeFragment:
     * 
     * // In onViewCreated after tutorial check:
     * if (isFirstTimeUser) {
     *     MotivationHelper.initializeForNewChild(requireContext(), childId);
     * }
     * 
     * // In medicine logging success callback:
     * if ("controller".equals(medicineType)) {
     *     MotivationHelper.updateControllerStreak(requireContext(), childId, true);
     * }
     * 
     * // In breathing technique completion:
     * MotivationHelper.updateTechniqueStreak(requireContext(), childId, true);
     * 
     * // To display current progress:
     * MotivationHelper.getCurrentStreaks(childId, new MotivationHelper.StreakDisplayCallback() {
     *     @Override
     *     public void onStreaksLoaded(int controllerStreak, int techniqueStreak) {
     *         // Update UI with streak counts
     *     }
     *     
     *     @Override
     *     public void onError(String error) {
     *         // Handle error
     *     }
     * });
     */
}