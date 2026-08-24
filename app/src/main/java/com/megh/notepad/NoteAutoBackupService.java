package com.megh.notepad;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NoteAutoBackupService extends Service {

    @Override
    public IBinder onBind(Intent intent) { 
        return null; 
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() { 
                performBackup(); 
            }
        }).start();
        return START_STICKY;
    }

    private void performBackup() {
        final Context context = getApplicationContext();
        SharedPreferences appSettings = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        
        if (!appSettings.getBoolean("auto_backup_enabled", false)) {
            stopSelf(); 
            return;
        }

        showToast(context, "নোট ব্যাকআপ শুরু হয়েছে... ⏳");

        String userNameEn = appSettings.getString("author_name_en", "Unknown_User");
        if (userNameEn.trim().isEmpty()) userNameEn = "TunePad_User";

        int successCount = 0;
        android.database.sqlite.SQLiteDatabase db = null;
        android.database.Cursor cursor = null;

        try {
            db = context.openOrCreateDatabase("notes_db_v3", Context.MODE_PRIVATE, null);
            
            // 🌟 ডাটাবেস থেকে এখন id ও টেনে আনা হচ্ছে 🌟
            cursor = db.rawQuery("SELECT id, title, content FROM notes WHERE isDeleted=0 AND isDraft=0 AND (label IS NULL OR label NOT LIKE 'Project:%')", null);

            if (cursor.moveToFirst()) {
                File cacheDir = context.getCacheDir();
                do {
                    String id = cursor.getString(0);
                    String title = cursor.getString(1);
                    String content = cursor.getString(2);

                    if (title == null) title = "Untitled Note";
                    if (content == null) content = "";

                    try {
                        // ১. টাইটেলটাকে ফাইলের কন্টেন্টের ভেতর লুকিয়ে ফেলা
                        String combinedContent = "<TPAD_TITLE>" + title.trim() + "</TPAD_TITLE>\n" + content;
                        
                        // ২. পুরো কন্টেন্ট এনক্রিপ্ট করা
                        String encryptedContent = SecurityUtils.encrypt(combinedContent);
                        if (encryptedContent == null) continue;

                        // ৩. 🌟 ফাইলের নাম হিসেবে আপনার শেয়ারিং মেথডের মতো ID ব্যবহার করা 🌟
                        String safeId = id;
                        if (safeId != null && safeId.contains("notes_db_v3")) {
                            safeId = safeId.replaceAll(".*notes_db_v3", "Note_");
                        } 
                        if (safeId == null || safeId.isEmpty()) {
                            safeId = "Note_" + System.currentTimeMillis();
                        }
                        safeId = safeId.replaceAll("[^a-zA-Z0-9.-]", "_"); // সেফ নাম

                        File tpadFile = new File(cacheDir, safeId + ".tpad");
                        
                        // ৪. ফাইলে ডেটা লেখা
                        FileOutputStream fos = new FileOutputStream(tpadFile);
                        fos.write(encryptedContent.getBytes());
                        fos.close();

                        // ৫. আপলোড করা
                        if (uploadFileSync(tpadFile, userNameEn)) {
                            successCount++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } while (cursor.moveToNext());
            } else {
                showToast(context, "ব্যাকআপ করার মতো সাধারণ কোনো নোট পাওয়া যায়নি! ⚠️");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (cursor != null) cursor.close();
                if (db != null) db.close();
            } catch (Exception e) {}
        }
        
        showToast(context, successCount + " টি সাধারণ নোট সফলভাবে ব্যাকআপ হয়েছে! ☁️");
        stopSelf();
    }

    // ==========================================
    // 🌟 আপলোড ইঞ্জিন (UTF-8 সেফটি সহ) 🌟
    // ==========================================
    private boolean uploadFileSync(File file, String userNameEn) {
        try {
            String uploadUrl = "https://www.shuvraafroj.info/api/upload_handler.php";
            HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
            conn.setDoInput(true); 
            conn.setDoOutput(true); 
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");

            String boundary = "*****" + System.currentTimeMillis() + "*****";
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            DataOutputStream request = new DataOutputStream(conn.getOutputStream());
            String crlf = "\r\n"; 
            String twoHyphens = "--";

            // Username
            request.write((twoHyphens + boundary + crlf).getBytes("UTF-8"));
            request.write(("Content-Disposition: form-data; name=\"username\"" + crlf + crlf).getBytes("UTF-8"));
            request.write((userNameEn + crlf).getBytes("UTF-8"));

            // File
            request.write((twoHyphens + boundary + crlf).getBytes("UTF-8"));
            request.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + crlf).getBytes("UTF-8"));
            request.write(("Content-Type: application/octet-stream" + crlf + crlf).getBytes("UTF-8"));

            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024 * 1024]; 
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) > 0) { 
                request.write(buffer, 0, bytesRead); 
            }
            fis.close();

            request.write((crlf).getBytes("UTF-8")); 
            request.write((twoHyphens + boundary + twoHyphens + crlf).getBytes("UTF-8"));
            request.flush(); 
            request.close();

            int responseCode = conn.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while (in.readLine() != null) { }
            in.close();
            
            if (file.exists()) {
                file.delete();
            }
            
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (Exception e) { 
            return false; 
        }
    }

    private void showToast(final Context context, final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override 
            public void run() { 
                Toast.makeText(context, message, Toast.LENGTH_LONG).show(); 
            }
        });
    }
}
