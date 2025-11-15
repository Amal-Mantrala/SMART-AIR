package com.example.b07demosummer2024.auth;

import com.google.firebase.auth.FirebaseAuth;

public class AuthService {
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
                    if (task.isSuccessful()) {
                        callback.onResult(true, "Login successful!");
                    } else {
                        String msg = task.getException() != null ?
                                task.getException().getMessage() :
                                "Unknown error";
                        callback.onResult(false, msg);
                    }
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
                                "Unknown error";
                        callback.onResult(false, msg);
                    }
                });
    }

    public void signOut() {
        auth.signOut();
    }
}
