package com.example.b07demosummer2024.auth;

import com.google.firebase.auth.FirebaseAuth;

public class AuthService implements IAuthService{
    private final FirebaseAuth auth;

    public AuthService() {
        this.auth = FirebaseAuth.getInstance();
    }

    public interface AuthCallback {
        void onResult(boolean success, String message);
    }

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
}
