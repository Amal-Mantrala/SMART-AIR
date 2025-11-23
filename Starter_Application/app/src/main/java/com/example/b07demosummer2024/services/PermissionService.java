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
        grantAccessWithFields(parentId, providerId, childIds, null, callback);
    }

    public void grantAccessWithFields(String parentId, String providerId, List<String> childIds, Map<String, List<String>> sharedFieldsPerChild, GrantCallback callback) {
        Map<String, Object> accessData = new HashMap<>();
        accessData.put("accessLevel", "read_only");
        accessData.put("children", childIds);
        accessData.put("isActive", true);
        accessData.put("grantedAt", com.google.firebase.Timestamp.now());
        
        // Store field-level sharing preferences per child
        if (sharedFieldsPerChild != null) {
            accessData.put("sharedFields", sharedFieldsPerChild);
        } else {
            // Default: share only name if no fields specified
            Map<String, List<String>> defaultFields = new HashMap<>();
            for (String childId : childIds) {
                defaultFields.put(childId, new ArrayList<>());
            }
            accessData.put("sharedFields", defaultFields);
        }

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
                            providerData.put("sharedFields", doc.get("sharedFields"));
                            providers.add(providerData);
                        }
                    }
                    callback.onResult(providers);
                });
    }

    public interface SharedFieldsCallback {
        void onResult(List<String> sharedFields);
    }

    /**
     * Get the list of data fields that are shared for a specific provider-child combination.
     * Returns empty list if no access or no fields are shared.
     */
    public void getSharedFields(String parentId, String providerId, String childId, SharedFieldsCallback callback) {
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
                            if (Boolean.TRUE.equals(isActive) && children != null && children.contains(childId)) {
                                Map<String, List<String>> sharedFieldsMap = (Map<String, List<String>>) doc.get("sharedFields");
                                if (sharedFieldsMap != null && sharedFieldsMap.containsKey(childId)) {
                                    List<String> fields = sharedFieldsMap.get(childId);
                                    callback.onResult(fields != null ? fields : new ArrayList<>());
                                } else {
                                    callback.onResult(new ArrayList<>());
                                }
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

    public interface UpdateFieldsCallback {
        void onResult(boolean success, String message);
    }

    /**
     * Update the shared fields for a specific provider-child combination.
     * This allows parents to change what information is shared without revoking access.
     */
    public void updateSharedFields(String parentId, String providerId, String childId, List<String> sharedFields, UpdateFieldsCallback callback) {
        db.collection("providerAccess")
                .document(parentId)
                .collection("providers")
                .document(providerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            Map<String, List<String>> sharedFieldsMap = (Map<String, List<String>>) doc.get("sharedFields");
                            if (sharedFieldsMap == null) {
                                sharedFieldsMap = new HashMap<>();
                            }
                            sharedFieldsMap.put(childId, sharedFields != null ? sharedFields : new ArrayList<>());
                            
                            db.collection("providerAccess")
                                    .document(parentId)
                                    .collection("providers")
                                    .document(providerId)
                                    .update("sharedFields", sharedFieldsMap)
                                    .addOnSuccessListener(aVoid -> callback.onResult(true, "Sharing preferences updated"))
                                    .addOnFailureListener(e -> callback.onResult(false, e.getMessage()));
                        } else {
                            callback.onResult(false, "Provider access not found");
                        }
                    } else {
                        callback.onResult(false, "Failed to load provider access");
                    }
                });
    }

    /**
     * Update shared fields for multiple children at once for a provider.
     */
    public void updateSharedFieldsForChildren(String parentId, String providerId, Map<String, List<String>> sharedFieldsPerChild, UpdateFieldsCallback callback) {
        db.collection("providerAccess")
                .document(parentId)
                .collection("providers")
                .document(providerId)
                .update("sharedFields", sharedFieldsPerChild)
                .addOnSuccessListener(aVoid -> callback.onResult(true, "Sharing preferences updated"))
                .addOnFailureListener(e -> callback.onResult(false, e.getMessage()));
    }
}

