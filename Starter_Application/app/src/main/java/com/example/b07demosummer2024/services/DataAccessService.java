package com.example.b07demosummer2024.services;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class DataAccessService {
    private final FirebaseFirestore db;
    private final PermissionService permissionService;

    public DataAccessService() {
        this.db = FirebaseFirestore.getInstance();
        this.permissionService = new PermissionService();
    }

    public interface AccessCallback {
        void onResult(boolean canAccess);
    }

    public void canRead(String parentId, String providerId, String childId, AccessCallback callback) {
        if (parentId == null || providerId == null || childId == null) {
            callback.onResult(false);
            return;
        }
        permissionService.hasAccess(parentId, providerId, childId, callback::onResult);
    }

    public void canWrite(String parentId, String providerId, String childId, AccessCallback callback) {
        callback.onResult(false);
    }

    public interface DataCallback {
        void onResult(Map<String, Object> data);
    }

    public void getReadOnlyData(String childId, String providerId, DataCallback callback) {
        db.collection("users")
                .document(childId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            String parentId = doc.getString("parentId");
                            if (parentId != null) {
                                canRead(parentId, providerId, childId, canAccess -> {
                                    if (canAccess) {
                                        Map<String, Object> data = new HashMap<>();
                                        data.put("name", doc.get("name"));
                                        data.put("role", doc.get("role"));
                                        callback.onResult(data);
                                    } else {
                                        callback.onResult(new HashMap<>());
                                    }
                                });
                            } else {
                                callback.onResult(new HashMap<>());
                            }
                        } else {
                            callback.onResult(new HashMap<>());
                        }
                    } else {
                        callback.onResult(new HashMap<>());
                    }
                });
    }
}

