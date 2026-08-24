package com.megh.notepad;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImportActivity extends AppCompatActivity {

    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        
        int bgColor = ThemeHelper.getBgColor(this); 
        int accentColor = ThemeHelper.getAccentColor(this);
        root.setBackgroundColor(bgColor != 0 ? bgColor : Color.parseColor("#121212")); 

        ProgressBar progressBar = new ProgressBar(this);
        progressBar.getIndeterminateDrawable().setColorFilter(accentColor != 0 ? accentColor : Color.parseColor("#008744"), android.graphics.PorterDuff.Mode.SRC_IN);
        root.addView(progressBar);

        tvStatus = new TextView(this);
        tvStatus.setText("ফাইল স্ক্যান করা হচ্ছে...");
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setTextSize(16f);
        tvStatus.setPadding(0, 48, 0, 0);
        root.addView(tvStatus);
        setContentView(root);

        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            Uri data = intent.getData();
            String fileUrl = data.toString().replace(" ", "%20");
            String fileName = data.getLastPathSegment();
            
            if (fileName != null && fileName.endsWith(".tbox")) {
                fileName = fileName.replace(".tbox", "");
                try { fileName = java.net.URLDecoder.decode(fileName, "UTF-8"); } catch(Exception e){}
                downloadAndPrepareProject(fileUrl, fileName);
            } 
            else if (fileName != null && fileName.endsWith(".tpad")) {
                fileName = fileName.replace(".tpad", "");
                try { fileName = java.net.URLDecoder.decode(fileName, "UTF-8"); } catch(Exception e){}
                downloadAndPrepareNote(fileUrl, fileName); 
            } 
            else if (fileName != null && fileName.endsWith(".tchar")) {
                fileName = fileName.replace(".tchar", "");
                try { fileName = java.net.URLDecoder.decode(fileName, "UTF-8"); } catch(Exception e){}
                downloadAndPrepareCharacter(fileUrl, fileName); 
            }
            else {
                Toast.makeText(this, "অবৈধ ফাইল লিংক!", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            finish();
        }
    }

    // ==========================================
    // 🌟 ১. নোট ডাউনলোড করে সরাসরি MmmActivity তে পাঠানো 🌟
    // ==========================================
    private void downloadAndPrepareNote(final String fileUrl, final String noteFileName) {
        tvStatus.setText("নোট ওপেন হচ্ছে... ⏳");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(fileUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.connect();

                    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        showErrorAndExit("সার্ভারে নোটটি পাওয়া যায়নি!");
                        return;
                    }

                    InputStream is = conn.getInputStream();
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                    is.close();

                    String encryptedContent = stringBuilder.toString();
                    final String decryptedContent = SecurityUtils.decrypt(encryptedContent);

                    if (decryptedContent != null && !decryptedContent.isEmpty()) {
                        String finalTitle = noteFileName; 
                        String finalContent = decryptedContent;
                        
                        String startTag = "<TPAD_TITLE>";
                        String endTag = "</TPAD_TITLE>";

                        if (decryptedContent.startsWith(startTag)) {
                            int endIndex = decryptedContent.indexOf(endTag);
                            if (endIndex != -1) {
                                finalTitle = decryptedContent.substring(startTag.length(), endIndex);
                                finalContent = decryptedContent.substring(endIndex + endTag.length());
                                if (finalContent.startsWith("\n")) {
                                    finalContent = finalContent.substring(1);
                                }
                            }
                        }

                        final String realTitle = finalTitle;
                        final String realContent = finalContent;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 🌟 কোনো পপআপ ছাড়াই সরাসরি MmmActivity তে ডেটা পাঠানো হচ্ছে 🌟
                                Intent intent = new Intent(ImportActivity.this, MmmActivity.class);
                                intent.putExtra("IS_IMPORT_PREVIEW", true); // ফ্ল্যাগ: এটি প্রিভিউ মোড বোঝাবে
                                intent.putExtra("IMPORT_TITLE", realTitle);
                                intent.putExtra("IMPORT_CONTENT", realContent);
                                startActivity(intent);
                                finish(); // ImportActivity ক্লোজ করে দেওয়া হলো
                            }
                        });
                    } else {
                        showErrorAndExit("নোটটি ডিক্রিপ্ট করা যায়নি! ফাইলটি করাপ্টেড হতে পারে।");
                    }

                } catch (Exception e) {
                    showErrorAndExit("লিংক থেকে নোট নামাতে সমস্যা হয়েছে! ইন্টারনেট চেক করুন।");
                }
            }
        }).start();
    }

    // ==========================================
    // 🌟 ২. ক্যারেক্টার (.tchar) ডাউনলোড ইঞ্জিন 🌟
    // ==========================================
    private void downloadAndPrepareCharacter(final String fileUrl, final String charFileName) {
        tvStatus.setText("চরিত্র ডাউনলোড হচ্ছে... ⏳");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(fileUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.connect();

                    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        showErrorAndExit("সার্ভারে চরিত্রটি পাওয়া যায়নি!");
                        return;
                    }

                    InputStream is = conn.getInputStream();
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                    is.close();

                    String jsonString = new String(android.util.Base64.decode(stringBuilder.toString(), android.util.Base64.DEFAULT), "UTF-8");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent previewIntent = new Intent(ImportActivity.this, CharacterDetailsActivity.class);
                            previewIntent.putExtra("IMPORT_JSON", jsonString); 
                            startActivity(previewIntent);
                            finish();
                        }
                    });

                } catch (Exception e) {
                    showErrorAndExit("লিংক থেকে চরিত্র নামাতে সমস্যা হয়েছে! ইন্টারনেট চেক করুন।");
                }
            }
        }).start();
    }

    // ==========================================
    // 🌟 ৩. প্রজেক্ট ডাউনলোডের মেথড 🌟
    // ==========================================
    private void downloadAndPrepareProject(final String fileUrl, final String projectName) {
        tvStatus.setText("প্রজেক্ট ডাউনলোড হচ্ছে... ⏳");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File cacheDir = getCacheDir();
                    File tempTboxFile = new File(cacheDir, "temp_import.tbox");
                    URL url = new URL(fileUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.connect();
                    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) { showErrorAndExit("সার্ভারে প্রজেক্টটি পাওয়া যায়নি!"); return; }
                    InputStream is = conn.getInputStream(); FileOutputStream fos = new FileOutputStream(tempTboxFile);
                    byte[] buffer = new byte[1024 * 1024]; int len;
                    while ((len = is.read(buffer)) != -1) { fos.write(buffer, 0, len); }
                    fos.flush(); fos.close(); is.close();
                    
                    File tempExtractDir = new File(getFilesDir(), "TunePad_Data/Temp_Cache/Preview_Project");
                    if (tempExtractDir.exists()) { deleteRecursive(tempExtractDir); }
                    tempExtractDir.mkdirs();
                    java.io.FileInputStream fis = new java.io.FileInputStream(tempTboxFile);
                    com.megh.notepad.TBoxUtils.decryptAndUnzipFolder(fis, tempExtractDir);
                    if (tempTboxFile.exists()) tempTboxFile.delete();

                        runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent previewIntent = new Intent(ImportActivity.this, ProjectViewActivity.class);
                            previewIntent.putExtra("IS_PREVIEW_MODE", true);
                            previewIntent.putExtra("PREVIEW_DIR_PATH", tempExtractDir.getAbsolutePath());
                            previewIntent.putExtra("PROJECT_NAME", projectName);
                            startActivity(previewIntent); finish();
                        }
                    });
                } catch (Exception e) { showErrorAndExit("ইন্টারনেট সমস্যা!"); }
            }
        }).start();
    }

    private void showErrorAndExit(final String errorMsg) {
        runOnUiThread(new Runnable() { @Override public void run() { Toast.makeText(ImportActivity.this, errorMsg, Toast.LENGTH_LONG).show(); finish(); } });
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) { for (File child : fileOrDirectory.listFiles()) { deleteRecursive(child); } }
        fileOrDirectory.delete();
    }
}
