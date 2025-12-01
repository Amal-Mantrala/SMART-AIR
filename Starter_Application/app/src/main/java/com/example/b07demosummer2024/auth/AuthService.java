package com.example.b07demosummer2024.auth;

import com.google.firebase.auth.FirebaseAuth;

public class AuthService implements IAuthService {
    private final FirebaseAuth auth;

    public AuthService() {
        this.auth = FirebaseAuth.getInstance();
    }

    public interface AuthCallback {
        void onResult(boolean success, String message);
    }

    /**
     * Callback used when creating users without switching the main auth instance.
     * The third parameter will contain the newly-created user's UID on success
     * and null on failure.
     */
    public interface CreateUserSilentCallback {
        void onResult(boolean success, String message, String createdUid);
    }

    @Override
    public void signIn(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    // Ensure callback runs on main thread
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> {
                        if (task.isSuccessful()) {
                            android.util.Log.d("AuthService", "SignIn successful, calling callback");
                            callback.onResult(true, "Login successful!");
                        } else {
                            String msg = task.getException() != null ?
                                    task.getException().getMessage() :
                                    "Unknown error";
                            android.util.Log.d("AuthService", "SignIn failed: " + msg);
                            callback.onResult(false, msg);
                        }
                    });
                });
    }

    public void resetPassword(String email, AuthCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        callback.onResult(true, "Reset email sent.");
                    } else {
                        String msg = task.getException() != null ?
                                task.getException().getMessage() :
                                "Error sending email";
                        callback.onResult(false, msg);
                    }
                });
    }
    @Override
    public void signOut() {
        auth.signOut();
    }

    @Override
    public boolean isSignedIn() {
        return auth.getCurrentUser() != null;
    }

    @Override
    public void createUser(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onResult(true, "Account created successfully");
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Unknown error";

                        callback.onResult(false, msg);
                    }
                });
    }

    /**
     * Create a new user account but make the creation call on a secondary FirebaseApp
     * so the default app's currentUser is not replaced. This avoids signing the
     * client out / switching sessions when creating sub-accounts from a logged-in
     * parent flow.
     */
    public void createUserSilently(String email, String password, CreateUserSilentCallback callback) {
        try {
            com.google.firebase.FirebaseApp defaultApp = com.google.firebase.FirebaseApp.getInstance();
            com.google.firebase.FirebaseOptions options = defaultApp.getOptions();
            android.content.Context ctx = defaultApp.getApplicationContext();

            // Create a uniquely named secondary app so it won't collide
            String silentAppName = "silent_create_" + System.currentTimeMillis();
            com.google.firebase.FirebaseApp silentApp = com.google.firebase.FirebaseApp.initializeApp(ctx, options, silentAppName);
            com.google.firebase.auth.FirebaseAuth silentAuth = com.google.firebase.auth.FirebaseAuth.getInstance(silentApp);

            silentAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        // Ensure we run callback on main thread
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> {
                            if (task.isSuccessful()) {
                                String uid = null;
                                if (task.getResult() != null && task.getResult().getUser() != null) {
                                    uid = task.getResult().getUser().getUid();
                                }
                                callback.onResult(true, "Account created successfully", uid);
                            } else {
                                String msg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                                callback.onResult(false, msg, null);
                            }

                            // Clean up: sign out any session on silentAuth and delete the silent app.
                            try {
                                silentAuth.signOut();
                            } catch (Exception ignored) {}
                            try {
                                // delete() might not exist on some older SDKs; guard against it
                                silentApp.delete();
                            } catch (Exception ignored) {}
                        });
                    });
        } catch (Exception e) {
            callback.onResult(false, e.getMessage() == null ? "Failed to create user silently" : e.getMessage(), null);
        }
    }
}
