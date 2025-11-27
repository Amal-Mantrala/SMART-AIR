package com.example.b07demosummer2024.services;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
                            
                            getChildrenForParent(parentId, inviteCode, accessibleChildren, () -> {
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

    private void getChildrenForParent(String parentId, String inviteCode, List<AccessibleChild> accessibleChildren, 
                                    Runnable onComplete, ErrorCallback onError) {
        // Get parent information first
        db.collection("users").document(parentId)
                .get()
                .addOnSuccessListener(parentDoc -> {
                    if (parentDoc.exists()) {
                        String parentName = parentDoc.getString("name");
                        List<String> childrenIds = (List<String>) parentDoc.get("children");
                        
                        if (childrenIds != null && !childrenIds.isEmpty()) {
                            // Get information for each child
                            int[] remainingChildren = {childrenIds.size()};
                            
                            for (String childId : childrenIds) {
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
                            onComplete.run();
                        }
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