package com.example.b07demosummer2024.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.b07demosummer2024.MainActivity;
import com.example.b07demosummer2024.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class NotificationService {
    private static NotificationService instance;
    private FirebaseFirestore db;
    private ListenerRegistration alertListener;
    private Context context;
    private static final String CHANNEL_ID = "smart_air_alerts";

    private NotificationService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public static NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    public void initialize(Context context) {
        this.context = context;
        createNotificationChannel();
        getFCMToken();
        startListeningToAlerts();
    }

    private void createNotificationChannel() {
        if (context == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SMART-AIR Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for child health alerts");
            channel.enableVibration(true);
            channel.enableLights(true);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void getFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        FirebaseAuth auth = FirebaseAuth.getInstance();
                        if (auth.getCurrentUser() != null) {
                            String userId = auth.getCurrentUser().getUid();
                            db.collection("users").document(userId).update("fcmToken", token);
                        }
                    }
                });
    }

    private void startListeningToAlerts() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return;
        }

        String parentId = auth.getCurrentUser().getUid();
        Query query = db.collection("parentAlerts")
                .whereEqualTo("parentId", parentId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1);

        alertListener = query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                if (e != null) {
                    return;
                }

                if (snapshots != null && !snapshots.getDocumentChanges().isEmpty()) {
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            Map<String, Object> alert = dc.getDocument().getData();
                            String message = (String) alert.get("message");
                            String alertType = (String) alert.get("type");
                            if (message != null && context != null) {
                                sendLocalNotification(message, alertType);
                            }
                        }
                    }
                }
            }
        });
    }

    private void sendLocalNotification(String message, String alertType) {
        if (context == null) {
            return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("SMART-AIR Alert")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            int notificationId = (int) System.currentTimeMillis();
            notificationManager.notify(notificationId, builder.build());
        }
    }

    public void sendTestNotification() {
        if (context == null) {
            return;
        }

        createNotificationChannel();
        String testMessage = "Test notification from SMART-AIR";
        sendLocalNotification(testMessage, "test");
    }

    public void stopListening() {
        if (alertListener != null) {
            alertListener.remove();
            alertListener = null;
        }
    }
}
