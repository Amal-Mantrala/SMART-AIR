package com.example.b07demosummer2024.services;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId).update("fcmToken", token);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
    }
}
