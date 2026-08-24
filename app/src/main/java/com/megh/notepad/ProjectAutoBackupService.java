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
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ProjectAutoBackupService extends Service {

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() { performProjectBackup(); }
        }).start();
        return START_STICKY;
    }

    private void performProjectBackup() {
        final Context context = getApplicationContext();
        SharedPreferences appSettings = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        
        if (!appSettings.getBoolean("project_auto_backup_enabled", false)) {
            stopSelf(); return;
        }

        showToast(context, "সকল প্রজেক্ট ব্যাকআপ শুরু হয়েছে... ⏳");

        String userNameEn = appSettings.getString("author_name_en", "Unknown_User");
        if (userNameEn.trim().isEmpty()) userNameEn = "TunePad_User";

        File baseDir = new File(context.getFilesDir(), "TunePad_Data/Projects");
        File cacheDir = context.getCacheDir();
        int successCount = 0;

        if (baseDir.exists() && baseDir.isDirectory()) {
            File[] categories = baseDir.listFiles();
            if (categories != null) {
                for (File cat : categories) {
                    if (cat.isDirectory()) {
                        File[] projects = cat.listFiles();
                        if (projects != null) {
                            for (File proj : projects) {
                                if (proj.isDirectory()) {
                                    try {
                                        String projTitle = proj.getName();
                                        File tboxFile = new File(cacheDir, projTitle + "_auto.tbox");
                                        
                                        // 🌟 ম্যাজিক: জিপ করার আগে A to Z ডেটার JSON ফাইল তৈরি করা 🌟
                                        File metaDataFile = new File(proj, "project_meta_data.json");
                                        saveSharedPreferencesToFile(context, projTitle, metaDataFile);
                                        
                                        // এখন জিপ হবে, তাই JSON সহ সব ফাইল প্যাক হয়ে যাবে
                                        com.megh.notepad.TBoxUtils.zipAndEncryptFolder(proj, tboxFile);
                                        
                                        // 🌟 জিপ হওয়ার পর ফোল্ডার ক্লিন করার জন্য JSON ফাইলটি ডিলিট 🌟
                                        if (metaDataFile.exists()) {
                                            metaDataFile.delete();
                                        }
                                        
                                        // সার্ভারে আপলোড
                                        if (uploadProjectSync(tboxFile, projTitle, userNameEn)) {
                                            successCount++;
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        showToast(context, successCount + " টি প্রজেক্ট (A to Z ডেটাসহ) সফলভাবে ব্যাকআপ হয়েছে! ☁️");
        stopSelf();
    }

   // 🌟 প্রজেক্টের ভেতরের সব সেটিংস, চরিত্র এবং পর্বগুলোর (Episodes) কমপ্লিট ব্যাকআপ ম্যাজিক 🌟
private void saveSharedPreferencesToFile(String projTitle, File metaFile) {
    try {
        // MmmActivity এর জন্য Context হিসেবে getSharedPreferences ব্যবহার করা হচ্ছে। 
        // ProjectAutoBackupService এ এটি context.getSharedPreferences হবে।
        android.content.SharedPreferences prefs = getSharedPreferences("ProjectData_" + projTitle, MODE_PRIVATE);
        org.json.JSONObject json = new org.json.JSONObject();
        
        // ১. প্রজেক্টের সাধারণ ডেটা সেভ
        java.util.Map<String, ?> allEntries = prefs.getAll();
        for (java.util.Map.Entry<String, ?> entry : allEntries.entrySet()) {
            json.put(entry.getKey(), entry.getValue());
        }
        
        // 🌟 ২. ম্যাজিক ফিক্স: ডাটাবেস থেকে প্রজেক্টের সব পর্ব ও মেটাডেটা টানা হচ্ছে 🌟
        org.json.JSONArray episodesArray = new org.json.JSONArray();
        android.database.sqlite.SQLiteDatabase db = openOrCreateDatabase("notes_db_v3", MODE_PRIVATE, null);
        android.database.Cursor cursor = db.rawQuery("SELECT id, title, content, timestamp, isPinned, isHidden, isDraft FROM notes WHERE label=?", new String[]{"Project: " + projTitle});
        
        if (cursor.moveToFirst()) {
            do {
                org.json.JSONObject ep = new org.json.JSONObject();
                ep.put("id", cursor.getString(0));
                ep.put("title", cursor.getString(1));
                ep.put("content", cursor.getString(2));
                ep.put("timestamp", cursor.getString(3));
                ep.put("isPinned", cursor.getInt(4));
                ep.put("isHidden", cursor.getInt(5));
                ep.put("isDraft", cursor.getInt(6));
                episodesArray.put(ep);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        json.put("project_episodes", episodesArray); // JSON-এ পর্বগুলো যুক্ত করা হলো

        // ৩. প্রজেক্টের চরিত্রের ডেটা সেভ
        android.content.SharedPreferences charPrefs = getSharedPreferences("Global_Characters_DB", MODE_PRIVATE);
        int charCount = charPrefs.getInt("char_count", 0);
        org.json.JSONArray charArray = new org.json.JSONArray();
        for (int i = 0; i < charCount; i++) {
            if (charPrefs.getBoolean("char_active_" + i, false)) {
                String story = charPrefs.getString("char_story_" + i, "");
                if (story.trim().equalsIgnoreCase(projTitle.trim())) {
                    org.json.JSONObject charObj = new org.json.JSONObject();
                    
                    charObj.put("old_id", i);
                    charObj.put("rels", charPrefs.getString("char_rels_" + i, ""));
                    charObj.put("name", charPrefs.getString("char_name_" + i, ""));
                    charObj.put("nickname", charPrefs.getString("char_nickname_" + i, ""));
                    charObj.put("role", charPrefs.getString("char_role_" + i, ""));
                    charObj.put("img", charPrefs.getString("char_img_" + i, "")); 
                    charObj.put("dob", charPrefs.getString("char_dob_" + i, ""));
                    charObj.put("age", charPrefs.getString("char_age_" + i, ""));
                    charObj.put("country", charPrefs.getString("char_country_" + i, ""));
                    charObj.put("location", charPrefs.getString("char_location_" + i, ""));
                    charObj.put("occupation", charPrefs.getString("char_occupation_" + i, ""));
                    charObj.put("height", charPrefs.getString("char_height_" + i, ""));
                    charObj.put("build", charPrefs.getString("char_build_" + i, ""));
                    charObj.put("eye_hair", charPrefs.getString("char_eye_hair_" + i, ""));
                    charObj.put("marks", charPrefs.getString("char_marks_" + i, ""));
                    charObj.put("clothing", charPrefs.getString("char_clothing_" + i, ""));
                    charObj.put("personality", charPrefs.getString("char_personality_" + i, ""));
                    charObj.put("strengths", charPrefs.getString("char_strengths_" + i, ""));
                    charObj.put("flaws", charPrefs.getString("char_flaws_" + i, ""));
                    charObj.put("habits", charPrefs.getString("char_habits_" + i, ""));
                    charObj.put("goal", charPrefs.getString("char_goal_" + i, ""));
                    charObj.put("fear", charPrefs.getString("char_fear_" + i, ""));
                    charObj.put("secrets", charPrefs.getString("char_secrets_" + i, ""));
                    charObj.put("conflict", charPrefs.getString("char_conflict_" + i, ""));
                    charObj.put("backstory", charPrefs.getString("char_backstory_" + i, ""));
                    
                    int customCount = charPrefs.getInt("char_custom_count_" + i, 0);
                    charObj.put("custom_count", customCount);
                    for (int j = 0; j < customCount; j++) {
                        charObj.put("custom_key_" + j, charPrefs.getString("char_custom_key_" + i + "_" + j, ""));
                        charObj.put("custom_val_" + j, charPrefs.getString("char_custom_val_" + i + "_" + j, ""));
                    }
                    charArray.put(charObj);
                }
            }
        }
        json.put("project_characters", charArray);
        
        java.io.FileOutputStream fos = new java.io.FileOutputStream(metaFile);
        fos.write(json.toString().getBytes("UTF-8"));
        fos.close();
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    // 🌟 বাংলা নাম সাপোর্ট সহ আপলোড ইঞ্জিন 🌟
    private boolean uploadProjectSync(File file, String projTitle, String userNameEn) {
        try {
            String uploadUrl = "https://www.shuvraafroj.info/api/upload_project.php";
            HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
            conn.setDoInput(true); conn.setDoOutput(true); conn.setUseCaches(false);
            conn.setRequestMethod("POST");

            String boundary = "*****" + System.currentTimeMillis() + "*****";
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            DataOutputStream request = new DataOutputStream(conn.getOutputStream());
            String crlf = "\r\n"; String twoHyphens = "--";

            request.write((twoHyphens + boundary + crlf).getBytes("UTF-8"));
            request.write(("Content-Disposition: form-data; name=\"username\"" + crlf + crlf).getBytes("UTF-8"));
            request.write((userNameEn + crlf).getBytes("UTF-8"));

            request.write((twoHyphens + boundary + crlf).getBytes("UTF-8"));
            request.write(("Content-Disposition: form-data; name=\"project_file\"; filename=\"" + projTitle + ".tbox\"" + crlf).getBytes("UTF-8"));
            request.write(("Content-Type: application/octet-stream" + crlf + crlf).getBytes("UTF-8"));

            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024 * 1024]; int bytesRead;
            while ((bytesRead = fis.read(buffer)) > 0) { request.write(buffer, 0, bytesRead); }
            fis.close();

            request.write((crlf).getBytes("UTF-8")); 
            request.write((twoHyphens + boundary + twoHyphens + crlf).getBytes("UTF-8"));
            request.flush(); request.close();

            int responseCode = conn.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while (in.readLine() != null) { }
            in.close();
            
            if (file.exists()) file.delete();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (Exception e) { return false; }
    }

    private void showToast(final Context context, final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override public void run() { Toast.makeText(context, message, Toast.LENGTH_LONG).show(); }
        });
    }
}
