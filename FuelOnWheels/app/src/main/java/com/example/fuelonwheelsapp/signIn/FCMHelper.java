package com.example.fuelonwheelsapp.signIn;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FCMHelper {

    public static void sendNotification(String fcmToken, String otp) {
        Map<String, String> data = new HashMap<>();
        data.put("otp", otp);

        RemoteMessage message = new RemoteMessage.Builder(fcmToken + "@fcm.googleapis.com")
                .setMessageId(generateMessageId())
                .setData(data)
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
            Log.d("FCMHelper", "Successfully sent message.");
        } catch (Exception e) {
            Log.e("FCMHelper", "Error sending message: " + e.getMessage(), e);
        }
    }

    private static String generateMessageId() {
        return String.valueOf(new Random().nextInt(9999));
    }
}
