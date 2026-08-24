package com.megh.notepad;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Base64;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class CharacterAutoBackupService extends Service {

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() { performBackup(); }
        }).start();
        return START_STICKY;
    }

    private void performBackup() {
        final Context context = getApplicationContext();
        SharedPreferences appSettings = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        
        // 🌟 সেটিংস থেকে অন থাকলে তবেই ব্যাকআপ হবে 🌟
        if (!appSettings.getBoolean("char_auto_backup_enabled", false)) {
            stopSelf(); 
            return;
        }

        showToast(context, "চরিত্র ব্যাকআপ শুরু হয়েছে... ⏳");

        String userNameEn = appSettings.getString("author_name_en", "Unknown_User");
        if (userNameEn.trim().isEmpty()) userNameEn = "TunePad_User";

        SharedPreferences charPrefs = context.getSharedPreferences("Global_Characters_DB", Context.MODE_PRIVATE);
        int count = charPrefs.getInt("char_count", 0);
        int successCount = 0;
        File cacheDir = context.getCacheDir();

        for (int i = 0; i < count; i++) {
            if (charPrefs.getBoolean("char_active_" + i, false)) {
                try {
                    String charName = charPrefs.getString("char_name_" + i, "Unknown");
                    
                    JSONObject json = new JSONObject();
                    Map<String, ?> allEntries = charPrefs.getAll();
                    for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                        String key = entry.getKey();
                        if (key.endsWith("_" + i) && !key.startsWith("char_active_")) {
                            String cleanKey = key.replace("_" + i, "");
                            json.put(cleanKey, entry.getValue());
                        }
                    }

                    String encodedData = Base64.encodeToString(json.toString().getBytes("UTF-8"), Base64.DEFAULT);

                    String safeName = charName.replaceAll("[^a-zA-Z0-9_\\-\\u0980-\\u09FF ]", "_");
                    if (safeName.trim().isEmpty()) safeName = "Character";
                    
                    // নাম হবে: Char_ID_Name.tchar
                    File tcharFile = new File(cacheDir, "Char_" + i + "_" + safeName + ".tchar");

                    FileOutputStream fos = new FileOutputStream(tcharFile);
                    fos.write(encodedData.getBytes("UTF-8"));
                    fos.close();

                    if (uploadFileSync(tcharFile, userNameEn)) {
                        successCount++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        if (successCount > 0) {
            showToast(context, successCount + " টি চরিত্র সফলভাবে ব্যাকআপ হয়েছে! ☁️");
        } else {
            showToast(context, "ব্যাকআপ করার মতো কোনো চরিত্র নেই!");
        }
        stopSelf();
    }

    // 🌟 আপলোড ইঞ্জিন 🌟
    private boolean uploadFileSync(File file, String userNameEn) {
        try {
            String uploadUrl = "https://www.shuvraafroj.info/api/upload_character.php"; // 🌟 এখানে আপডেট করা হয়েছে
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
            request.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + crlf).getBytes("UTF-8"));
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
