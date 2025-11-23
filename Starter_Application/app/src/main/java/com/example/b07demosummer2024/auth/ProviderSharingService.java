package com.example.b07demosummer2024.auth;

import com.example.b07demosummer2024.models.ChildSharingSettings;
import com.example.b07demosummer2024.models.ProviderAccess;
import com.example.b07demosummer2024.models.SharingSettings;
import com.example.b07demosummer2024.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderSharingService {
    private FirebaseFirestore db;

    public ProviderSharingService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface SharingCallback {
        void onResult(List<User> children);
        void onError(String error);
    }

    @FunctionalInterface
    public interface SharingResultCallback {
        void onResult(List<User> children);
    }

    public interface SettingsCallback {
        void onResult(SharingSettings settings);
        void onError(String error);
    }

    @FunctionalInterface
    public interface SettingsResultCallback {
        void onResult(SharingSettings settings);
    }

    @FunctionalInterface
    public interface BooleanResultCallback {
        void onResult(boolean result);
    }

    public interface BooleanCallback {
        void onResult(boolean result);
        void onError(String error);
    }

    public void getSharedChildrenForProvider(String providerId, SharingCallback callback) {
        db.collection("providerAccess")
                .whereEqualTo("providerId", providerId)
                .whereEqualTo("status", "active")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> parentIds = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            ProviderAccess access = doc.toObject(ProviderAccess.class);
                            if (access != null && access.getParentId() != null) {
                                parentIds.add(access.getParentId());
                            }
                        }

                        if (parentIds.isEmpty()) {
                            callback.onResult(new ArrayList<>());
                            return;
                        }

                        List<User> sharedChildren = new ArrayList<>();
                        int[] completed = {0};
                        int total = parentIds.size();

                        for (String parentId : parentIds) {
                            getSharedChildrenForParentProvider(parentId, providerId, new SharingResultCallback() {
                                @Override
                                public void onResult(List<User> children) {
                                    sharedChildren.addAll(children);
                                    completed[0]++;
                                    if (completed[0] == total) {
                                        callback.onResult(sharedChildren);
                                    }
                                }
                            });
                        }
                    } else {
                        callback.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    private void getSharedChildrenForParentProvider(String parentId, String providerId, SharingResultCallback callback) {
        db.collection("sharingSettings")
                .document(parentId)
                .collection("providers")
                .document(providerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        SharingSettings settings = task.getResult().toObject(SharingSettings.class);
                        if (settings != null && settings.getChildSettings() != null) {
                            List<String> childIds = new ArrayList<>(settings.getChildSettings().keySet());
                            List<User> children = new ArrayList<>();
                            int[] completed = {0};
                            int total = childIds.size();

                            if (total == 0) {
                                callback.onResult(new ArrayList<>());
                                return;
                            }

                            for (String childId : childIds) {
                                db.collection("users").document(childId).get()
                                        .addOnCompleteListener(childTask -> {
                                            if (childTask.isSuccessful() && childTask.getResult().exists()) {
                                                User child = childTask.getResult().toObject(User.class);
                                                if (child != null) {
                                                    children.add(child);
                                                }
                                            }
                                            completed[0]++;
                                            if (completed[0] == total) {
                                                callback.onResult(children);
                                            }
                                        });
                            }
                        } else {
                            callback.onResult(new ArrayList<>());
                        }
                    } else {
                        callback.onResult(new ArrayList<>());
                    }
                });
    }

    public void getSharingSettings(String parentId, String providerId, SettingsCallback callback) {
        db.collection("sharingSettings")
                .document(parentId)
                .collection("providers")
                .document(providerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        SharingSettings settings = task.getResult().toObject(SharingSettings.class);
                        callback.onResult(settings != null ? settings : new SharingSettings());
                    } else {
                        callback.onResult(new SharingSettings());
                    }
                });
    }

    public void updateSharingSettings(String parentId, String providerId, ChildSharingSettings childSettings) {
        db.collection("sharingSettings")
                .document(parentId)
                .collection("providers")
                .document(providerId)
                .get()
                .addOnCompleteListener(task -> {
                    SharingSettings settings;
                    if (task.isSuccessful() && task.getResult().exists()) {
                        settings = task.getResult().toObject(SharingSettings.class);
                        if (settings == null) {
                            settings = new SharingSettings();
                            settings.setParentId(parentId);
                            settings.setProviderId(providerId);
                        }
                    } else {
                        settings = new SharingSettings();
                        settings.setParentId(parentId);
                        settings.setProviderId(providerId);
                    }

                    if (settings.getChildSettings() == null) {
                        settings.setChildSettings(new HashMap<>());
                    }
                    settings.getChildSettings().put(childSettings.getChildId(), childSettings);
                    settings.setLastUpdated(System.currentTimeMillis());

                    db.collection("sharingSettings")
                            .document(parentId)
                            .collection("providers")
                            .document(providerId)
                            .set(settings);
                });
    }

    public void isFieldShared(String childId, String fieldName, String providerId, BooleanCallback callback) {
        db.collection("users").document(childId).get()
                .addOnCompleteListener(childTask -> {
                    if (childTask.isSuccessful() && childTask.getResult().exists()) {
                        User child = childTask.getResult().toObject(User.class);
                        if (child != null && child.getParentId() != null) {
                            getSharingSettings(child.getParentId(), providerId, new SettingsCallback() {
                                @Override
                                public void onResult(SharingSettings settings) {
                                    if (settings.getChildSettings() != null) {
                                        ChildSharingSettings childSettings = settings.getChildSettings().get(childId);
                                        if (childSettings != null && childSettings.getSharedFields() != null) {
                                            Boolean isShared = childSettings.getSharedFields().get(fieldName);
                                            callback.onResult(isShared != null && isShared);
                                        } else {
                                            callback.onResult(false);
                                        }
                                    } else {
                                        callback.onResult(false);
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    callback.onResult(false);
                                }
                            });
                        } else {
                            callback.onResult(false);
                        }
                    } else {
                        callback.onResult(false);
                    }
                });
    }

    public void revokeProviderAccess(String parentId, String providerId) {
        db.collection("providerAccess")
                .whereEqualTo("parentId", parentId)
                .whereEqualTo("providerId", providerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            doc.getReference().update("status", "revoked");
                        }
                    }
                });

        db.collection("sharingSettings")
                .document(parentId)
                .collection("providers")
                .document(providerId)
                .delete();
    }

    public void acceptInvite(String inviteCode, String providerId) {
        db.collection("providerAccess")
                .whereEqualTo("inviteCode", inviteCode)
                .whereEqualTo("status", "pending")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                        doc.getReference().update("status", "active", "providerId", providerId);
                    }
                });
    }
}
