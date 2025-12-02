package com.example.b07demosummer2024.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;

public class ImpersonationService {
    private static final String PREFS = "impersonation_prefs";
    private static final String KEY_IMPERSONATED_CHILD = "impersonated_child_id";

    public static void setImpersonatedChild(Context ctx, String childId) {
        if (ctx == null) return;
        SharedPreferences prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_IMPERSONATED_CHILD, childId).apply();
    }

    public static void clearImpersonation(Context ctx) {
        if (ctx == null) return;
        SharedPreferences prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_IMPERSONATED_CHILD).apply();
    }

    public static String getImpersonatedChildId(Context ctx) {
        if (ctx == null) return null;
        SharedPreferences prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_IMPERSONATED_CHILD, null);
    }

    public static boolean isImpersonating(Context ctx) {
        return getImpersonatedChildId(ctx) != null;
    }

    /**
     * Returns the id that should be used as the active child id. If impersonation is active,
     * returns the impersonated child id; otherwise returns the currently signed-in user's uid.
     */
    public static String getActiveChildId(Context ctx) {
        String impersonated = ctx == null ? null : getImpersonatedChildId(ctx);
        if (impersonated != null && !impersonated.isEmpty()) return impersonated;

        // Fall back to the signed-in user's uid (if available)
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }
}
