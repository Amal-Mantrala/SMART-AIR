package com.example.b07demosummer2024.utils;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Utility class to handle common Firebase operations and reduce code duplication
 */
public class FirebaseUtils {
    
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();
    
    /**
     * Generic method to get user data by ID
     */
    public static void getUserById(String userId, Consumer<Object> onSuccess, Consumer<String> onError) {
        if (userId == null || userId.isEmpty()) {
            onError.accept("Invalid user ID");
            return;
        }
        
        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        onSuccess.accept(document.getData());
                    } else {
                        onError.accept("User not found");
                    }
                })
                .addOnFailureListener(e -> onError.accept(e.getMessage()));
    }
    
    /**
     * Generic method to get multiple users by their IDs
     */
    public static void getUsersByIds(List<String> userIds, Consumer<List<Map<String, Object>>> onSuccess, Consumer<String> onError) {
        if (userIds == null || userIds.isEmpty()) {
            onSuccess.accept(new ArrayList<>());
            return;
        }
        
        List<Map<String, Object>> users = new ArrayList<>();
        int[] completedCount = {0};
        
        for (String userId : userIds) {
            getUserById(userId, 
                userData -> {
                    users.add((Map<String, Object>) userData);
                    completedCount[0]++;
                    if (completedCount[0] == userIds.size()) {
                        onSuccess.accept(users);
                    }
                },
                error -> {
                    completedCount[0]++;
                    if (completedCount[0] == userIds.size()) {
                        onSuccess.accept(users);
                    }
                }
            );
        }
    }
    
    /**
     * Get current authenticated user ID
     */
    public static String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
    
    /**
     * Get current authenticated user email
     */
    public static String getCurrentUserEmail() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : null;
    }
    
    /**
     * Check if user is authenticated
     */
    public static boolean isUserAuthenticated() {
        return auth.getCurrentUser() != null;
    }
    
    /**
     * Generic method to save document to Firebase with timestamp
     */
    public static void saveDocumentWithTimestamp(String collection, Object document, 
                                                Consumer<String> onSuccess, Consumer<String> onError) {
        db.collection(collection)
                .add(document)
                .addOnSuccessListener(documentReference -> {
                    String docId = documentReference.getId();
                    // Update document with the generated ID
                    documentReference.update("id", docId)
                            .addOnSuccessListener(aVoid -> onSuccess.accept(docId))
                            .addOnFailureListener(e -> onError.accept(e.getMessage()));
                })
                .addOnFailureListener(e -> onError.accept(e.getMessage()));
    }
    
    /**
     * Generic method to query documents by field value
     */
    public static void queryDocumentsByField(String collection, String field, Object value,
                                           Consumer<List<Map<String, Object>>> onSuccess, Consumer<String> onError) {
        db.collection(collection)
                .whereEqualTo(field, value)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Map<String, Object>> results = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            results.add(document.getData());
                        }
                        onSuccess.accept(results);
                    } else {
                        onError.accept(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }
}