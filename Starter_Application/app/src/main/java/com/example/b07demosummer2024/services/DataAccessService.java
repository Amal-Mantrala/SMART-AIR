package com.example.b07demosummer2024.services;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.List;
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
        verifyProviderRole(providerId, isProvider -> {
            if (!isProvider) {
                callback.onResult(false);
                return;
            }
            permissionService.hasAccess(parentId, providerId, childId, callback::onResult);
        });
    }

    public void canWrite(String parentId, String providerId, String childId, AccessCallback callback) {
        callback.onResult(false);
    }

    public interface DataCallback {
        void onResult(Map<String, Object> data);
    }

    public void getReadOnlyData(String childId, String providerId, DataCallback callback) {
        if (childId == null || providerId == null) {
            callback.onResult(new HashMap<>());
            return;
        }
        verifyProviderRole(providerId, isProvider -> {
            if (!isProvider) {
                callback.onResult(new HashMap<>());
                return;
            }
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
                                            permissionService.getSharedFields(parentId, providerId, childId, sharedFields -> {
                                                Map<String, Object> data = filterDataBySharedFields(doc, sharedFields);
                                                callback.onResult(data);
                                            });
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
        });
    }

    private void verifyProviderRole(String userId, AccessCallback callback) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            String role = doc.getString("role");
                            callback.onResult("provider".equals(role));
                        } else {
                            callback.onResult(false);
                        }
                    } else {
                        callback.onResult(false);
                    }
                });
    }

    private Map<String, Object> filterDataBySharedFields(DocumentSnapshot doc, List<String> sharedFields) {
        Map<String, Object> filteredData = new HashMap<>();
        Map<String, Object> allData = doc.getData();
        
        if (allData == null) {
            return filteredData;
        }

        if (allData.containsKey("role")) {
            filteredData.put("role", allData.get("role"));
        }

        if (sharedFields != null) {
            for (String field : sharedFields) {
                if (allData.containsKey(field)) {
                    filteredData.put(field, allData.get(field));
                }
            }
        }

        return filteredData;
    }
}

