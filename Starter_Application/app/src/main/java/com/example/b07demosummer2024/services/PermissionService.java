package com.example.b07demosummer2024.services;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionService {
    private final FirebaseFirestore db;

    public PermissionService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface PermissionCallback {
        void onResult(boolean hasAccess);
    }

    public void hasAccess(String parentId, String providerId, String childId, PermissionCallback callback) {
        db.collection("providerAccess")
                .document(parentId)
                .collection("providers")
                .document(providerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            Boolean isActive = doc.getBoolean("isActive");
                            List<String> children = (List<String>) doc.get("children");
                            boolean hasAccess = Boolean.TRUE.equals(isActive) && children != null && children.contains(childId);
                            callback.onResult(hasAccess);
                        } else {
                            callback.onResult(false);
                        }
                    } else {
                        callback.onResult(false);
                    }
                });
    }

    public interface ChildrenCallback {
        void onResult(List<String> childIds);
    }

    public void getAccessibleChildren(String parentId, String providerId, ChildrenCallback callback) {
        db.collection("providerAccess")
                .document(parentId)
                .collection("providers")
                .document(providerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            Boolean isActive = doc.getBoolean("isActive");
                            if (Boolean.TRUE.equals(isActive)) {
                                List<String> children = (List<String>) doc.get("children");
                                callback.onResult(children != null ? children : new ArrayList<>());
                            } else {
                                callback.onResult(new ArrayList<>());
                            }
                        } else {
                            callback.onResult(new ArrayList<>());
                        }
                    } else {
                        callback.onResult(new ArrayList<>());
                    }
                });
    }

    public interface GrantCallback {
        void onResult(boolean success, String message);
    }

    public void grantAccess(String parentId, String providerId, List<String> childIds, GrantCallback callback) {
        Map<String, Object> accessData = new HashMap<>();
        accessData.put("accessLevel", "read_only");
        accessData.put("children", childIds);
        accessData.put("isActive", true);
        accessData.put("grantedAt", com.google.firebase.Timestamp.now());

        db.collection("providerAccess")
                .document(parentId)
                .collection("providers")
                .document(providerId)
                .set(accessData)
                .addOnSuccessListener(aVoid -> callback.onResult(true, "Access granted"))
                .addOnFailureListener(e -> callback.onResult(false, e.getMessage()));
    }

    public interface RevokeCallback {
        void onResult(boolean success, String message);
    }

    public void revokeAccess(String parentId, String providerId, RevokeCallback callback) {
        db.collection("providerAccess")
                .document(parentId)
                .collection("providers")
                .document(providerId)
                .update("isActive", false)
                .addOnSuccessListener(aVoid -> callback.onResult(true, "Access revoked"))
                .addOnFailureListener(e -> callback.onResult(false, e.getMessage()));
    }

    public interface DefaultsCallback {
        void onResult(Map<String, Object> defaults);
    }

    public void getPrivacyDefaults(String parentId, DefaultsCallback callback) {
        db.collection("users")
                .document(parentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            Map<String, Object> defaults = (Map<String, Object>) doc.get("privacyDefaults");
                            if (defaults == null) {
                                defaults = new HashMap<>();
                                defaults.put("shareByDefault", false);
                                defaults.put("defaultShareLevel", "none");
                            }
                            callback.onResult(defaults);
                        } else {
                            Map<String, Object> defaults = new HashMap<>();
                            defaults.put("shareByDefault", false);
                            defaults.put("defaultShareLevel", "none");
                            callback.onResult(defaults);
                        }
                    } else {
                        Map<String, Object> defaults = new HashMap<>();
                        defaults.put("shareByDefault", false);
                        defaults.put("defaultShareLevel", "none");
                        callback.onResult(defaults);
                    }
                });
    }

    public interface ProvidersCallback {
        void onResult(List<Map<String, Object>> providers);
    }

    public void getActiveProviders(String parentId, ProvidersCallback callback) {
        db.collection("providerAccess")
                .document(parentId)
                .collection("providers")
                .whereEqualTo("isActive", true)
                .get()
                .addOnCompleteListener(task -> {
                    List<Map<String, Object>> providers = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Map<String, Object> providerData = new HashMap<>();
                            providerData.put("providerId", doc.getId());
                            providerData.put("children", doc.get("children"));
                            providerData.put("grantedAt", doc.get("grantedAt"));
                            providers.add(providerData);
                        }
                    }
                    callback.onResult(providers);
                });
    }
}

