package com.megh.notepad; // আপনার প্যাকেজের সঠিক নাম দিন

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UserAlarmHelper {

    private static BroadcastReceiver alarmReceiver;

    // 🌟 ফিক্স ১: Static ভেরিয়েবলের বদলে SharedPreferences থেকে ডাইনামিক ইউজারনেম 🌟
    private static String getUserName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        return prefs.getString("UserName", "UnknownUser");
    }

    // লাইভ এলার্ম মনিটরিং শুরু করার মেথড
    public static void startMonitoring(final Context context) {
        if (alarmReceiver == null) {
            alarmReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // যখনই ইউজার নতুন এলার্ম সেট করবে বা ডিলিট করবে, এটি কল হবে
                    syncNextAlarm(context);
                }
            };

            // ইউজারের এলার্ম পরিবর্তনের ওপর নজর রাখা
            IntentFilter filter = new IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);
            context.registerReceiver(alarmReceiver, filter);
            Log.d("UserAlarmHelper", "Live alarm monitoring started.");

            // সার্ভিস চালু হওয়ার সময় একবার বর্তমান এলার্ম চেক করা
            syncNextAlarm(context);
        }
    }

    public static void stopMonitoring(Context context) {
        if (alarmReceiver != null) {
            // 🌟 ফিক্স ২: Exception এড়াতে try-catch ব্যবহার করা হলো 🌟
            try {
                context.unregisterReceiver(alarmReceiver);
            } catch (Exception e) {
                Log.e("UserAlarmHelper", "Error unregistering receiver: " + e.getMessage());
            }
            alarmReceiver = null;
            Log.d("UserAlarmHelper", "Live alarm monitoring stopped.");
        }
    }

    // পরবর্তী এলার্মের সময় বের করে ফায়ারবেসে পাঠানোর মেথড
    private static void syncNextAlarm(Context context) {
        try {
            // 🌟 ফিক্স ১ ইমপ্লিমেন্টেশন 🌟
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(getUserName(context)).child("UpcomingAlarm");

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            
            // পরবর্তী এলার্মের তথ্য নেওয়া
            AlarmManager.AlarmClockInfo nextAlarm = alarmManager.getNextAlarmClock();

            Map<String, Object> alarmData = new HashMap<>();

            if (nextAlarm != null) {
                // এলার্ম সেট করা থাকলে তার সময় বের করা
                long triggerTime = nextAlarm.getTriggerTime();
                
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
                String alarmTimeStr = sdf.format(new Date(triggerTime));
                
                // বর্তমান সময় থেকে কতক্ষণ পর বাজবে তা হিসাব করা
                long timeDiff = triggerTime - System.currentTimeMillis();
                String timeRemaining = getDurationString(timeDiff);

                alarmData.put("Status", "Alarm Set");
                alarmData.put("Next_Alarm_Time", alarmTimeStr);
                alarmData.put("Time_Remaining", timeRemaining);
                
                Log.d("UserAlarmHelper", "Next alarm set for: " + alarmTimeStr);
            } else {
                // কোনো এলার্ম সেট করা না থাকলে
                alarmData.put("Status", "No Alarm Set");
                alarmData.put("Next_Alarm_Time", "N/A");
                alarmData.put("Time_Remaining", "N/A");
                
                Log.d("UserAlarmHelper", "No upcoming alarm.");
            }
            
            alarmData.put("Last_Checked", System.currentTimeMillis());

            // ফায়ারবেসে ডাটা আপডেট করা
            dbRef.setValue(alarmData);

        } catch (Exception e) {
            Log.e("UserAlarmHelper", "Error syncing alarm: " + e.getMessage());
        }
    }

    // মিলি-সেকেন্ডকে দিন, ঘণ্টা ও মিনিটে রূপান্তর করার হেল্পার মেথড
    private static String getDurationString(long millis) {
        if (millis < 0) return "Already ringing or passed";
        
        long days = millis / (1000 * 60 * 60 * 24);
        long hours = (millis / (1000 * 60 * 60)) % 24;
        long minutes = (millis / (1000 * 60)) % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(" days, ");
        if (hours > 0) sb.append(hours).append(" hours, ");
        sb.append(minutes).append(" minutes left");

        return sb.toString();
    }
}

