package com.megh.notepad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class BatteryHelper {

    private static BroadcastReceiver batteryReceiver;
    
    // ডুপ্লিকেট আপডেট ঠেকানোর জন্য ভ্যারিয়েবল
    private static int lastBatteryPct = -1;
    private static boolean lastChargingState = false;

    // এই মেথডটি কল করলে লাইভ মনিটরিং শুরু হবে
    public static void startMonitoring(Context context) {
        if (batteryReceiver == null) {
            batteryReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    
                    int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    int batteryPct = (int) ((level / (float) scale) * 100);

                    int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                         status == BatteryManager.BATTERY_STATUS_FULL;

                    int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                    String plugType = "Unplugged";
                    if (chargePlug == BatteryManager.BATTERY_PLUGGED_USB) {
                        plugType = "USB Charging";
                    } else if (chargePlug == BatteryManager.BATTERY_PLUGGED_AC) {
                        plugType = "AC Wall Charger";
                    }

                    // শুধুমাত্র পার্সেন্টেজ বা চার্জিং অবস্থা পরিবর্তন হলেই ফায়ারবেসে পাঠাবো
                    if (batteryPct != lastBatteryPct || isCharging != lastChargingState) {
                        lastBatteryPct = batteryPct;
                        lastChargingState = isCharging;

                        sendToFirebase(batteryPct, isCharging, plugType);
                    }
                }
            };

            // রিসিভার রেজিস্টার করা
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            context.registerReceiver(batteryReceiver, filter);
            Log.d("BatteryHelper", "Live battery monitoring started.");
        }
    }

    // সার্ভিস বন্ধ হলে মনিটরিং বন্ধ করার জন্য
    public static void stopMonitoring(Context context) {
        if (batteryReceiver != null) {
            context.unregisterReceiver(batteryReceiver);
            batteryReceiver = null;
            Log.d("BatteryHelper", "Live battery monitoring stopped.");
        }
    }

    private static void sendToFirebase(int batteryPct, boolean isCharging, String plugType) {
        try {
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(BackgroundService.USER_ID).child("BatteryInfo");

            Map<String, Object> batteryData = new HashMap<>();
            batteryData.put("Percentage", batteryPct + "%");
            batteryData.put("Is_Charging", isCharging);
            batteryData.put("Plug_Type", plugType);
            batteryData.put("Last_Updated", System.currentTimeMillis());

            dbRef.setValue(batteryData);
            Log.d("BatteryHelper", "Battery info updated in Firebase!");
        } catch (Exception e) {
            Log.e("BatteryHelper", "Error: " + e.getMessage());
        }
    }
}
