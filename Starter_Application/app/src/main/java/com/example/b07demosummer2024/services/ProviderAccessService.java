package com.example.b07demosummer2024.services;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderAccessService {
    private FirebaseFirestore db;

    public ProviderAccessService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface AccessibleChildrenCallback {
        void onSuccess(List<AccessibleChild> children);
        void onError(String error);
    }

    public static class AccessibleChild {
        private String childId;
        private String childName;
        private String parentId;
        private String parentName;
        private String inviteCode;

        public AccessibleChild(String childId, String childName, String parentId, String parentName, String inviteCode) {
            this.childId = childId;
            this.childName = childName;
            this.parentId = parentId;
            this.parentName = parentName;
            this.inviteCode = inviteCode;
        }

        // Getters
        public String getChildId() { return childId; }
        public String getChildName() { return childName; }
        public String getParentId() { return parentId; }
        public String getParentName() { return parentName; }
        public String getInviteCode() { return inviteCode; }
    }

    public void getAccessibleChildren(String providerId, AccessibleChildrenCallback callback) {
        // First, get all active provider access records for this provider
        db.collection("providerAccess")
                .whereEqualTo("providerId", providerId)
                .whereEqualTo("status", "active")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<AccessibleChild> accessibleChildren = new ArrayList<>();
                        
                        if (task.getResult().isEmpty()) {
                            callback.onSuccess(accessibleChildren);
                            return;
                        }

                        // Get unique parent IDs from access records
                        Map<String, String> parentInviteCodes = new HashMap<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String parentId = document.getString("parentId");
                            String inviteCode = document.getString("inviteCode");
                            if (parentId != null && inviteCode != null) {
                                parentInviteCodes.put(parentId, inviteCode);
                            }
                        }

                        // For each parent, get their children and parent name
                        int[] remainingParents = {parentInviteCodes.size()};
                        
                        for (Map.Entry<String, String> entry : parentInviteCodes.entrySet()) {
                            String parentId = entry.getKey();
                            String inviteCode = entry.getValue();
                            
                            getChildrenForParent(parentId, providerId, inviteCode, accessibleChildren, () -> {
                                remainingParents[0]--;
                                if (remainingParents[0] == 0) {
                                    callback.onSuccess(accessibleChildren);
                                }
                            }, callback::onError);
                        }
                    } else {
                        callback.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    private void getChildrenForParent(String parentId, String providerId, String inviteCode, List<AccessibleChild> accessibleChildren, 
                                    Runnable onComplete, ErrorCallback onError) {
        // Get parent information first
        db.collection("users").document(parentId)
                .get()
                .addOnSuccessListener(parentDoc -> {
                    if (parentDoc.exists()) {
                        String parentName = parentDoc.getString("name");
                        // Only include children that the parent has explicitly shared with this provider
                        db.collection("sharingSettings")
                                .document(parentId)
                                .collection("providers")
                                .document(providerId)
                                .get()
                                .addOnCompleteListener(settingsTask -> {
                                    if (settingsTask.isSuccessful() && settingsTask.getResult().exists()) {
                                        com.example.b07demosummer2024.models.SharingSettings settings = settingsTask.getResult().toObject(com.example.b07demosummer2024.models.SharingSettings.class);
                                        if (settings != null && settings.getChildSettings() != null && !settings.getChildSettings().isEmpty()) {
                                            List<String> childIds = new ArrayList<>(settings.getChildSettings().keySet());
                                            int[] remainingChildren = {childIds.size()};

                                            for (String childId : childIds) {
                                                db.collection("users").document(childId)
                                                        .get()
                                                        .addOnSuccessListener(childDoc -> {
                                                            if (childDoc.exists()) {
                                                                String childName = childDoc.getString("name");
                                                                if (childName != null) {
                                                                    accessibleChildren.add(new AccessibleChild(
                                                                            childId, childName, parentId, parentName, inviteCode));
                                                                }
                                                            }
                                                            remainingChildren[0]--;
                                                            if (remainingChildren[0] == 0) {
                                                                onComplete.run();
                                                            }
                                                        })
                                                        .addOnFailureListener(e -> onError.onError(e.getMessage()));
                                            }
                                        } else {
                                            // No children shared with this provider specifically
                                            onComplete.run();
                                        }
                                    } else {
                                        // No sharingSettings document exists for this parent/provider pair.
                                        // Fallback: look up the original invite by inviteCode and use its sharedChildrenIds
                                        db.collection("providerInvites")
                                                .whereEqualTo("inviteCode", inviteCode)
                                                .whereEqualTo("parentId", parentId)
                                                .get()
                                                .addOnCompleteListener(invTask -> {
                                                    if (invTask.isSuccessful() && !invTask.getResult().isEmpty()) {
                                                        // use iterator().next() to avoid potential index warnings
                                                        DocumentSnapshot doc = invTask.getResult().getDocuments().iterator().next();
                                                        List<String> childIds = (List<String>) doc.get("sharedChildrenIds");
                                                        if (childIds != null && !childIds.isEmpty()) {
                                                            int[] remainingChildren = {childIds.size()};
                                                            for (String childId : childIds) {
                                                                db.collection("users").document(childId)
                                                                        .get()
                                                                        .addOnSuccessListener(childDoc -> {
                                                                            if (childDoc.exists()) {
                                                                                String childName = childDoc.getString("name");
                                                                                if (childName != null) {
                                                                                    accessibleChildren.add(new AccessibleChild(
                                                                                            childId, childName, parentId, parentName, inviteCode));
                                                                                }
                                                                            }
                                                                            remainingChildren[0]--;
                                                                            if (remainingChildren[0] == 0) {
                                                                                onComplete.run();
                                                                            }
                                                                        })
                                                                        .addOnFailureListener(e -> onError.onError(e.getMessage()));
                                                            }
                                                            return;
                                                        }
                                                    }

                                                    // If there's no invite or no children listed, treat as none shared
                                                    onComplete.run();
                                                });
                                    }
                                });
                    } else {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> onError.onError(e.getMessage()));
    }

    public interface ErrorCallback {
        void onError(String error);
    }
}