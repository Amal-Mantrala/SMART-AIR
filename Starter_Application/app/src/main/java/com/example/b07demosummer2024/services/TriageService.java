package com.example.b07demosummer2024.services;

import com.example.b07demosummer2024.models.TriageIncident;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriageService {
    private FirebaseFirestore db;

    public TriageService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface SaveCallback {
        void onSuccess(String documentId);
        void onError(String error);
    }

    public interface TriageHistoryCallback {
        void onSuccess(List<TriageIncident> incidents);
        void onError(String error);
    }

    public interface ParentAlertCallback {
        void onSuccess();
        void onError(String error);
    }

    public void saveTriageIncident(TriageIncident incident, SaveCallback callback) {
        long timestamp = System.currentTimeMillis();
        incident.setTimestamp(timestamp);

        db.collection("triageIncidents")
                .add(incident)
                .addOnSuccessListener(documentReference -> {
                    String docId = documentReference.getId();
                    incident.setLogId(docId);
                    documentReference.update("logId", docId)
                            .addOnSuccessListener(aVoid -> {
                                alertParent(incident.getChildId(), "Triage session started", callback, docId);
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void alertParent(String childId, String message, SaveCallback originalCallback, String incidentId) {
        db.collection("users")
                .document(childId)
                .get()
                .addOnSuccessListener(childDoc -> {
                    if (childDoc.exists()) {
                        String parentId = childDoc.getString("parentId");
                        if (parentId != null && !parentId.isEmpty()) {
                            String childName = childDoc.getString("name");
                            if (childName == null || childName.isEmpty()) {
                                childName = "Your child";
                            }

                            Map<String, Object> alert = new HashMap<>();
                            alert.put("parentId", parentId);
                            alert.put("childId", childId);
                            alert.put("childName", childName);
                            alert.put("message", message);
                            alert.put("timestamp", System.currentTimeMillis());
                            alert.put("type", "triage_started");
                            alert.put("incidentId", incidentId);
                            alert.put("read", false);

                            db.collection("parentAlerts")
                                    .add(alert)
                                    .addOnSuccessListener(aVoid -> {
                                        if (originalCallback != null) {
                                            originalCallback.onSuccess(incidentId);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        if (originalCallback != null) {
                                            originalCallback.onSuccess(incidentId);
                                        }
                                    });
                        } else {
                            if (originalCallback != null) {
                                originalCallback.onSuccess(incidentId);
                            }
                        }
                    } else {
                        if (originalCallback != null) {
                            originalCallback.onSuccess(incidentId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (originalCallback != null) {
                        originalCallback.onSuccess(incidentId);
                    }
                });
    }

    public void alertParentEscalation(String childId, String escalationReason, ParentAlertCallback callback) {
        db.collection("users")
                .document(childId)
                .get()
                .addOnSuccessListener(childDoc -> {
                    if (childDoc.exists()) {
                        String parentId = childDoc.getString("parentId");
                        if (parentId != null && !parentId.isEmpty()) {
                            String childName = childDoc.getString("name");
                            if (childName == null || childName.isEmpty()) {
                                childName = "Your child";
                            }

                            Map<String, Object> alert = new HashMap<>();
                            alert.put("parentId", parentId);
                            alert.put("childId", childId);
                            alert.put("childName", childName);
                            alert.put("message", "Triage escalated: " + escalationReason);
                            alert.put("timestamp", System.currentTimeMillis());
                            alert.put("type", "triage_escalated");
                            alert.put("read", false);

                            db.collection("parentAlerts")
                                    .add(alert)
                                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
                        } else {
                            callback.onSuccess();
                        }
                    } else {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getTriageHistory(String childId, int limitDays, TriageHistoryCallback callback) {
        long cutoffTime = System.currentTimeMillis() - (limitDays * 24L * 60L * 60L * 1000L);

        db.collection("triageIncidents")
                .whereEqualTo("childId", childId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<TriageIncident> incidents = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Long timestamp = document.getLong("timestamp");
                            if (timestamp != null && timestamp >= cutoffTime) {
                                TriageIncident incident = document.toObject(TriageIncident.class);
                                incidents.add(incident);
                            }
                        }
                        callback.onSuccess(incidents);
                    } else {
                        callback.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    public void getRecentRescueAttempts(String childId, RescueAttemptsCallback callback) {
        long threeHoursAgo = System.currentTimeMillis() - (3L * 60L * 60L * 1000L);

        db.collection("medicineLog")
                .whereEqualTo("childId", childId)
                .whereEqualTo("medicineType", "rescue")
                .whereGreaterThan("timestamp", threeHoursAgo)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(task.getResult().size());
                    } else {
                        callback.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    public interface RescueAttemptsCallback {
        void onSuccess(int count);
        void onError(String error);
    }
}
