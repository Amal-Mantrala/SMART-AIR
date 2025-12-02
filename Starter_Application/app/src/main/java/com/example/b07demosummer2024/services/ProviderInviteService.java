package com.example.b07demosummer2024.services;

import com.example.b07demosummer2024.models.ProviderInvite;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.b07demosummer2024.models.SharingSettings;
import com.example.b07demosummer2024.models.ChildSharingSettings;
import java.util.Map;
import java.util.HashMap;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProviderInviteService {
    private FirebaseFirestore db;
    private static final long INVITE_EXPIRY_DAYS = 7;
    private static final String INVITE_COLLECTION = "providerInvites";

    public ProviderInviteService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface InviteCallback {
        void onSuccess(ProviderInvite invite);
        void onError(String error);
    }

    public interface BooleanCallback {
        void onResult(boolean success);
        void onError(String error);
    }

    public interface ValidateCallback {
        void onValid(ProviderInvite invite);
        void onInvalid(String reason);
        void onError(String error);
    }

    public void createProviderInvite(String parentId, String parentName, String providerName, 
                                   List<String> sharedChildrenIds, InviteCallback callback) {
        String inviteCode = generateInviteCode();
        long currentTime = System.currentTimeMillis();
        long expiryTime = currentTime + TimeUnit.DAYS.toMillis(INVITE_EXPIRY_DAYS);

        ProviderInvite invite = new ProviderInvite();
        invite.setParentId(parentId);
        invite.setParentName(parentName);
        invite.setProviderName(providerName);
        invite.setInviteCode(inviteCode);
        invite.setStatus("pending");
        invite.setSharedChildrenIds(sharedChildrenIds);
        invite.setCreatedAt(currentTime);
        invite.setExpiresAt(expiryTime);

        db.collection(INVITE_COLLECTION)
                .add(invite)
                .addOnSuccessListener(documentReference -> {
                    invite.setInviteId(documentReference.getId());
                    callback.onSuccess(invite);
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    public void validateInviteCode(String inviteCode, ValidateCallback callback) {
        db.collection(INVITE_COLLECTION)
                .whereEqualTo("inviteCode", inviteCode)
                .whereEqualTo("status", "pending")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            callback.onInvalid("Invalid invite code");
                            return;
                        }

                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                        ProviderInvite invite = document.toObject(ProviderInvite.class);
                        invite.setInviteId(document.getId());

                        if (invite.isExpired()) {
                            // Update status to expired
                            document.getReference().update("status", "expired");
                            callback.onInvalid("Invite code has expired");
                            return;
                        }

                        callback.onValid(invite);
                    } else {
                        callback.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    public void acceptInvite(String inviteCode, String providerId, BooleanCallback callback) {
        validateInviteCode(inviteCode, new ValidateCallback() {
            @Override
            public void onValid(ProviderInvite invite) {
                // Update invite status
                db.collection(INVITE_COLLECTION)
                        .document(invite.getInviteId())
                        .update("status", "accepted", "acceptedByProviderId", providerId)
                        .addOnSuccessListener(aVoid -> {
                            // Create provider access records for each shared child
                            createProviderAccessRecords(invite, providerId, callback);
                        })
                        .addOnFailureListener(e -> {
                            callback.onError(e.getMessage());
                        });
            }

            @Override
            public void onInvalid(String reason) {
                callback.onError(reason);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private void createProviderAccessRecords(ProviderInvite invite, String providerId, BooleanCallback callback) {
        // This creates provider access records in the existing providerAccess collection
        // for compatibility with the existing ProviderSharingService
        
        for (String childId : invite.getSharedChildrenIds()) {
            ProviderAccess access = new ProviderAccess();
            access.setParentId(invite.getParentId());
            access.setProviderId(providerId);
            access.setStatus("active");
            access.setInviteCode(invite.getInviteCode());
            access.setCreatedAt(System.currentTimeMillis());

            db.collection("providerAccess").add(access);
        }
        
        // Now write sharingSettings/{parentId}/providers/{providerId} so the provider has explicit
        // sharing configuration. Default to sharing the 'name' field for invited children.
        List<String> childIds = invite.getSharedChildrenIds();
        if (childIds == null || childIds.isEmpty()) {
            callback.onResult(true);
            return;
        }

        SharingSettings settings = new SharingSettings();
        settings.setParentId(invite.getParentId());
        settings.setProviderId(providerId);
        Map<String, ChildSharingSettings> childSettingsMap = new HashMap<>();

        for (String childId : childIds) {
            ChildSharingSettings cs = new ChildSharingSettings();
            cs.setChildId(childId);
            Map<String, Boolean> shared = new HashMap<>();
            // Default granular fields - start conservative (all false). Parent can enable as desired.
            shared.put("rescueLogs", false);
            shared.put("controllerSummary", false);
            shared.put("symptoms", false);
            shared.put("triggers", false);
            shared.put("peakFlow", false);
            shared.put("triageIncidents", false);
            shared.put("summaryCharts", false);
            cs.setSharedFields(shared);
            childSettingsMap.put(childId, cs);
        }

        settings.setChildSettings(childSettingsMap);
        settings.setLastUpdated(System.currentTimeMillis());

        db.collection("sharingSettings")
                .document(invite.getParentId())
                .collection("providers")
                .document(providerId)
                .set(settings)
                .addOnSuccessListener(aVoid -> callback.onResult(true))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void revokeInvite(String inviteId, BooleanCallback callback) {
        db.collection(INVITE_COLLECTION)
                .document(inviteId)
                .update("status", "revoked")
                .addOnSuccessListener(aVoid -> {
                    callback.onResult(true);
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    private String generateInviteCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return sb.toString();
    }

    // Helper class for compatibility with existing code
    private static class ProviderAccess {
        private String parentId;
        private String providerId;
        private String status;
        private String inviteCode;
        private long createdAt;

        public String getParentId() { return parentId; }
        public void setParentId(String parentId) { this.parentId = parentId; }
        
        public String getProviderId() { return providerId; }
        public void setProviderId(String providerId) { this.providerId = providerId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getInviteCode() { return inviteCode; }
        public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
        
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    }
}