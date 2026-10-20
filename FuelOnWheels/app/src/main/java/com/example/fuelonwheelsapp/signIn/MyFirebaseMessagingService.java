package com.example.fuelonwheelsapp.signIn;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Log the entire FCM message payload
        Log.d(TAG, "FCM Message Payload: " + remoteMessage.getData());

        // Check if the message contains data
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            // Handle the data payload
            if (remoteMessage.getData().containsKey("otp")) {
                String otp = remoteMessage.getData().get("otp");
                Log.d(TAG, "Received OTP: " + otp);

                // You can perform actions based on the received OTP here
            }
        }

        // Check if the message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Notification Title: " + remoteMessage.getNotification().getTitle());
            Log.d(TAG, "Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Handle new FCM token
        Log.d(TAG, "Refreshed token: " + token);
    }
}
