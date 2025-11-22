package com.example.b07demosummer2024.services;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class InviteService {
    private final FirebaseFirestore db;
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;

    public InviteService() {
        this.db = FirebaseFirestore.getInstance();
    }

    private String generateInviteCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return code.toString();
    }

    public interface InviteCallback {
        void onResult(String inviteCode, boolean success, String message);
    }

    public void generateInviteCode(String parentId, List<String> children, int expiresInDays, InviteCallback callback) {
        String code = generateInviteCode();
        long expiresAt = System.currentTimeMillis() + (expiresInDays * 24L * 60 * 60 * 1000);

        Map<String, Object> inviteData = new HashMap<>();
        inviteData.put("parentId", parentId);
        inviteData.put("children", children);
        inviteData.put("expiresAt", expiresAt);
        inviteData.put("isUsed", false);
        inviteData.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("invites")
                .document(code)
                .set(inviteData)
                .addOnSuccessListener(aVoid -> callback.onResult(code, true, "Invite code generated"))
                .addOnFailureListener(e -> callback.onResult(null, false, e.getMessage()));
    }

    public interface ValidateCallback {
        void onResult(boolean valid, Map<String, Object> inviteData);
    }

    public void validateInviteCode(String inviteCode, ValidateCallback callback) {
        db.collection("invites")
                .document(inviteCode)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            Boolean isUsed = doc.getBoolean("isUsed");
                            Long expiresAt = doc.getLong("expiresAt");
                            boolean valid = !Boolean.TRUE.equals(isUsed) && expiresAt != null && expiresAt > System.currentTimeMillis();
                            
                            if (valid) {
                                Map<String, Object> data = new HashMap<>();
                                data.put("parentId", doc.get("parentId"));
                                data.put("children", doc.get("children"));
                                callback.onResult(true, data);
                            } else {
                                callback.onResult(false, new HashMap<>());
                            }
                        } else {
                            callback.onResult(false, new HashMap<>());
                        }
                    } else {
                        callback.onResult(false, new HashMap<>());
                    }
                });
    }

    public interface RedeemCallback {
        void onResult(boolean success, String message);
    }

    public void redeemInvite(String inviteCode, String providerId, RedeemCallback callback) {
        validateInviteCode(inviteCode, (valid, inviteData) -> {
            if (valid) {
                String parentId = (String) inviteData.get("parentId");
                List<String> children = (List<String>) inviteData.get("children");
                
                PermissionService permissionService = new PermissionService();
                permissionService.grantAccess(parentId, providerId, children != null ? children : new ArrayList<>(), (success, message) -> {
                    if (success) {
                        db.collection("invites")
                                .document(inviteCode)
                                .update("isUsed", true)
                                .addOnSuccessListener(aVoid -> callback.onResult(true, "Invite redeemed successfully"))
                                .addOnFailureListener(e -> callback.onResult(false, e.getMessage()));
                    } else {
                        callback.onResult(false, message);
                    }
                });
            } else {
                callback.onResult(false, "Invalid or expired invite code");
            }
        });
    }

    public interface InvitesCallback {
        void onResult(List<Map<String, Object>> invites);
    }

    public void getActiveInvites(String parentId, InvitesCallback callback) {
        db.collection("invites")
                .whereEqualTo("parentId", parentId)
                .get()
                .addOnCompleteListener(task -> {
                    List<Map<String, Object>> invites = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Boolean isUsed = doc.getBoolean("isUsed");
                            Long expiresAt = doc.getLong("expiresAt");
                            boolean isExpired = expiresAt != null && expiresAt <= System.currentTimeMillis();
                            
                            if (!Boolean.TRUE.equals(isUsed) && !isExpired) {
                                Map<String, Object> inviteData = new HashMap<>();
                                inviteData.put("code", doc.getId());
                                inviteData.put("children", doc.get("children"));
                                inviteData.put("expiresAt", expiresAt);
                                inviteData.put("createdAt", doc.get("createdAt"));
                                invites.add(inviteData);
                            }
                        }
                    }
                    callback.onResult(invites);
                });
    }

    public interface RevokeInviteCallback {
        void onResult(boolean success, String message);
    }

    public void revokeInvite(String inviteCode, RevokeInviteCallback callback) {
        db.collection("invites")
                .document(inviteCode)
                .update("isUsed", true)
                .addOnSuccessListener(aVoid -> callback.onResult(true, "Invite revoked"))
                .addOnFailureListener(e -> callback.onResult(false, e.getMessage()));
    }
}

