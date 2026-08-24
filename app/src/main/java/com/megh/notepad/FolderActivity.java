package com.megh.notepad;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
 
        import android.os.Environment;
    import android.util.Log;
    // ... আপনার বাকি ইমপোর্টগুলো


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FolderActivity extends AppCompatActivity {

    private RelativeLayout layoutFolderRoot;
    private LinearLayout folderToolbar, bottomActionLayout, layoutEmptyState;
    private ImageView btnBack;
    private TextView tvPathTitle, btnNewFolder, btnNewFile;
    private RecyclerView rvFiles;

    private File rootDir;
    private File currentDir;
    private FileAdapter adapter;
    private List<File> fileList = new ArrayList<>();

    // থিম কালার
    private int bgColor, surfaceColor, accentColor, primaryTextColor, secondaryTextColor;
    private Typeface currentTypeface = Typeface.DEFAULT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.folder);

        initViews();
        applyThemeColors();
        
               // 🌟 ইন্টারনাল প্রাইভেট স্টোরেজ সেটআপ (Main Root) 🌟
        rootDir = new File(getFilesDir(), "TunePad_Data");
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
        
        // নিশ্চিত করা হচ্ছে যেন ফোল্ডার এবং প্রজেক্টস ডিরেক্টরি তৈরি থাকে
        new File(rootDir, "Folders").mkdirs();
        new File(rootDir, "Projects").mkdirs();
        
        currentDir = rootDir;


        adapter = new FileAdapter();
        rvFiles.setLayoutManager(new LinearLayoutManager(this));
        rvFiles.setAdapter(adapter);

        loadDirectory(currentDir);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        btnNewFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateDialog(true);
            }
        });

        btnNewFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateDialog(false);
            }
        });
    }

    private void initViews() {
        layoutFolderRoot = findViewById(R.id.layoutFolderRoot);
        folderToolbar = findViewById(R.id.folderToolbar);
        bottomActionLayout = findViewById(R.id.bottomActionLayout);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        btnBack = findViewById(R.id.btnBack);
        tvPathTitle = findViewById(R.id.tvPathTitle);
        btnNewFolder = findViewById(R.id.btnNewFolder);
        btnNewFile = findViewById(R.id.btnNewFile);
        rvFiles = findViewById(R.id.rvFiles);
    }

    private void applyThemeColors() {
        bgColor = ThemeHelper.getBgColor(this);
        surfaceColor = ThemeHelper.getSurfaceColor(this);
        accentColor = ThemeHelper.getAccentColor(this);
        primaryTextColor = ThemeHelper.getPrimaryTextColor(this);
        secondaryTextColor = ThemeHelper.getSecondaryTextColor(this);

        layoutFolderRoot.setBackgroundColor(bgColor);
        folderToolbar.setBackgroundColor(surfaceColor);
        bottomActionLayout.setBackgroundColor(surfaceColor);

        tvPathTitle.setTextColor(primaryTextColor);
        btnBack.setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);

        // New Folder Button Background
        GradientDrawable folderBg = new GradientDrawable();
        folderBg.setColor(surfaceColor);
        folderBg.setStroke(3, accentColor);
        folderBg.setCornerRadius(30f);
        btnNewFolder.setBackground(folderBg);
        btnNewFolder.setTextColor(accentColor);

        // New File Button Background
        GradientDrawable fileBg = new GradientDrawable();
        fileBg.setColor(accentColor);
        fileBg.setCornerRadius(30f);
        btnNewFile.setBackground(fileBg);
        btnNewFile.setTextColor(bgColor); // Dark text on accent color
        
        // পুরো স্ক্রিনের (DecorView) ভেতর যত লেখা আছে, সবগুলোতে একসাথে ফন্ট বসিয়ে দেবে!
ThemeHelper.applyFontToAllViews(this, getWindow().getDecorView());

    }

    // 🌟 ফোল্ডারের ফাইল লোড করার লজিক 🌟
    private void loadDirectory(File dir) {
        fileList.clear();
        File[] files = dir.listFiles();
        
        if (files != null && files.length > 0) {
            for (File f : files) {
                // শুধু ফোল্ডার এবং .tpad ফাইল দেখাবে
                if (f.isDirectory() || f.getName().endsWith(".tpad")) {
                    fileList.add(f);
                }
            }
            // ফোল্ডারগুলো উপরে এবং ফাইল নিচে সাজানো
            Collections.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            });
            layoutEmptyState.setVisibility(View.GONE);
            rvFiles.setVisibility(View.VISIBLE);
        } else {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvFiles.setVisibility(View.GONE);
        }

        // টাইটেল আপডেট
        if (dir.equals(rootDir)) {
            tvPathTitle.setText("ফোল্ডারসমূহ");
        } else {
            tvPathTitle.setText(dir.getName());
        }
        
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if (currentDir != null && !currentDir.equals(rootDir)) {
            // যদি ভেতরে থাকি, এক ধাপ পেছনে যাবে
            currentDir = currentDir.getParentFile();
            loadDirectory(currentDir);
        } else {
            super.onBackPressed();
        }
    }

          // 🌟 নতুন ফোল্ডার বা ফাইল তৈরির বটম শিট 🌟
    private void showCreateDialog(final boolean isFolder) {
        final BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(surfaceColor);
        rootLayout.setPadding(64, 56, 64, 64);

        TextView titleView = new TextView(this);
        titleView.setText(isFolder ? "নতুন ফোল্ডার" : "নতুন নোট");
        titleView.setTextColor(primaryTextColor);
        titleView.setTextSize(20f);
        titleView.setTypeface(currentTypeface, Typeface.BOLD);
        titleView.setPadding(0, 0, 0, 48);
        rootLayout.addView(titleView);

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(bgColor);
        inputBg.setCornerRadius(24f);

        final EditText etName = new EditText(this);
        etName.setHint(isFolder ? "ফোল্ডারের নাম দিন" : "নোটের নাম দিন (খালি রাখলে Untitled হবে)");
        etName.setHintTextColor(secondaryTextColor);
        etName.setTextColor(primaryTextColor);
        etName.setPadding(48, 40, 48, 40);
        etName.setBackground(inputBg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 48);
        etName.setLayoutParams(params);
        rootLayout.addView(etName);

        TextView btnSave = new TextView(this);
        btnSave.setText("তৈরি করুন");
        btnSave.setTextColor(bgColor);
        btnSave.setTextSize(16f);
        btnSave.setGravity(Gravity.CENTER);
        btnSave.setTypeface(currentTypeface, Typeface.BOLD);
        btnSave.setPadding(0, 40, 0, 40);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(accentColor);
        btnBg.setCornerRadius(100f);
        btnSave.setBackground(btnBg);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etName.getText().toString().trim();
                
                if (isFolder && name.isEmpty()) {
                    Toast.makeText(FolderActivity.this, "ফোল্ডারের নাম দিন!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isFolder) {
                    File newFolder = new File(currentDir, name);
                    if (!newFolder.exists()) {
                        newFolder.mkdirs();
                        Toast.makeText(FolderActivity.this, "ফোল্ডার তৈরি হয়েছে", Toast.LENGTH_SHORT).show();
                    } else {
                        showDuplicateNameWarning(); // 🌟 ফোল্ডারের জন্য বটম শিট
                        return; 
                    }
                } else {
                    String safeName = name.replaceAll("[^a-zA-Z0-9_\\-\\u0980-\\u09FF ]", "");
                    
                    if (safeName.trim().isEmpty()) {
                        safeName = "Untitled Note";
                    }

                    File newFile = new File(currentDir, safeName + ".tpad");

                    if (newFile.exists()) {
                        if (safeName.startsWith("Untitled Note")) {
                            int counter = 1;
                            while (true) {
                                String tempName = "Untitled Note " + counter;
                                newFile = new File(currentDir, tempName + ".tpad");
                                if (!newFile.exists()) {
                                    safeName = tempName;
                                    break;
                                }
                                counter++;
                            }
                        } else {
                            showDuplicateNameWarning(); // 🌟 ফাইলের জন্য বটম শিট (টোস্ট ফিক্সড!)
                            return; 
                        }
                    }

                    try {
                        FileOutputStream fos = new FileOutputStream(newFile);
                        String initialData = "{\n  \"status\": \"Draft\",\n  \"content\": \"\"\n}";
                        fos.write(initialData.getBytes());
                        fos.close();
                        
                        String newNoteId = "fol_" + System.currentTimeMillis(); 
                        String newTimestamp = new java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date()); 
                        
                        String relativePath = currentDir.getAbsolutePath().replace(getFilesDir().getAbsolutePath() + "/TunePad_Data/", "");
                        String saveLabel = "";
                        String uniqueTitleForDb = safeName; 
                        
                        if (relativePath.startsWith("Projects/")) {
                            String[] parts = relativePath.split("/");
                            if (parts.length >= 3) {
                                saveLabel = "Project: " + parts[2];
                                uniqueTitleForDb = parts[2] + "_" + safeName;
                            } else {
                                saveLabel = "Project: " + currentDir.getName();
                                uniqueTitleForDb = currentDir.getName() + "_" + safeName; 
                            }
                        } else {
                            String folderPath = relativePath.replace("Folders", "");
                            if(folderPath.startsWith("/")) folderPath = folderPath.substring(1);
                            if(folderPath.trim().isEmpty()) folderPath = "মেইন ফোল্ডার";
                            saveLabel = "Folder: " + folderPath;
                        }

                        android.database.sqlite.SQLiteDatabase db = openOrCreateDatabase("notes_db_v3", MODE_PRIVATE, null);
                        android.content.ContentValues cv = new android.content.ContentValues();
                        cv.put("id", newNoteId);
                        cv.put("title", uniqueTitleForDb); 
                        cv.put("content", "");
                        cv.put("label", saveLabel);
                        cv.put("timestamp", newTimestamp);
                        cv.put("isPinned", 0);
                        cv.put("isDeleted", 0);
                        cv.put("isDraft", 0);
                        cv.put("isHidden", 0);
                        db.insertWithOnConflict("notes", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
                        db.close();
                        
                        Toast.makeText(FolderActivity.this, "নোট তৈরি হয়ে ডাটাবেসে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(FolderActivity.this, "এরর: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                bottomSheet.dismiss();
                loadDirectory(currentDir);
            }
        });

        rootLayout.addView(btnSave);
        bottomSheet.setContentView(rootLayout);
        bottomSheet.show();
    }





    // ==========================================
    // 🌟 File & Folder Adapter 🌟
    // ==========================================
    private class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

        @NonNull
        @Override
        public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // ডাইনামিক কাস্টম লেআউট তৈরি
            LinearLayout itemLayout = new LinearLayout(FolderActivity.this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setGravity(Gravity.CENTER_VERTICAL);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 8, 0, 8);
            itemLayout.setLayoutParams(lp);
            
            GradientDrawable shape = new GradientDrawable();
            shape.setColor(surfaceColor);
            shape.setCornerRadius(24f);
            itemLayout.setBackground(shape);
            itemLayout.setPadding(32, 32, 32, 32);

            TextView tvIcon = new TextView(FolderActivity.this);
            tvIcon.setTextSize(28f);
            tvIcon.setPadding(0, 0, 32, 0);
            
            TextView tvName = new TextView(FolderActivity.this);
            tvName.setTextColor(primaryTextColor);
            tvName.setTextSize(16f);
            tvName.setTypeface(currentTypeface, Typeface.BOLD);
            tvName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            tvName.setMaxLines(1);
            tvName.setEllipsize(android.text.TextUtils.TruncateAt.END);

            itemLayout.addView(tvIcon);
            itemLayout.addView(tvName);

            return new FileViewHolder(itemLayout, tvIcon, tvName);
        }

       @Override
        public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
            final File file = fileList.get(position);
            
            if (file.isDirectory()) {
                holder.tvIcon.setText("📁");
                holder.tvName.setText(file.getName());
            } else {
                holder.tvIcon.setText("📄");
                holder.tvName.setText(file.getName().replace(".tpad", ""));
            }

                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                public void onClick(View v) {
                    if (file.isDirectory()) {
                        currentDir = file;
                        loadDirectory(currentDir);
                    } else {
                        // 🌟 File Open Logic (ব্যাক বাটন বাগ ফিক্সড) 🌟
                        String fileName = file.getName().replace(".tpad", ""); 
                        String uniqueDbTitle = fileName; // ডিফল্ট
                        
                        // প্রজেক্টের নাম বের করে ইউনিক টাইটেল বানানো
                        String relativePath = currentDir.getAbsolutePath().replace(getFilesDir().getAbsolutePath() + "/TunePad_Data/", "");
                        if (relativePath.startsWith("Projects/")) {
                            String[] parts = relativePath.split("/");
                            String exactProjName = (parts.length >= 3) ? parts[2] : currentDir.getName();
                            uniqueDbTitle = exactProjName + "_" + fileName;
                        }
                        
                        android.database.sqlite.SQLiteDatabase db = openOrCreateDatabase("notes_db_v3", MODE_PRIVATE, null);
                        
                        // প্রথমে ইউনিক নাম দিয়ে খুঁজবে
                        android.database.Cursor cursor = db.rawQuery("SELECT id, content, label, isPinned, isHidden FROM notes WHERE title=?", new String[]{uniqueDbTitle});
                        
                        if (!cursor.moveToFirst()) {
                            // না পেলে পুরনো শুধু নাম দিয়ে খুঁজবে (সেফটির জন্য)
                            cursor.close();
                            cursor = db.rawQuery("SELECT id, content, label, isPinned, isHidden FROM notes WHERE title=?", new String[]{fileName});
                        }
                        
                        if (cursor.moveToFirst()) {
                            Intent intent = new Intent(FolderActivity.this, MmmActivity.class);
                            intent.putExtra("OPEN_NOTE", true);
                            intent.putExtra("noteId", cursor.getString(0));
                            intent.putExtra("title", fileName); // এডিটরে শুধু আসল নামটাই দেখাবে
                            intent.putExtra("content", cursor.getString(1));
                            intent.putExtra("label", cursor.getString(2));
                            intent.putExtra("isPinned", cursor.getInt(3) == 1);
                            intent.putExtra("isHidden", cursor.getInt(4) == 1);
                            
                            // 💡 ম্যাজিক ফিক্স: এখান থেকে intent.addFlags(...) এবং finish(); দুটোই মুছে দেওয়া হয়েছে!
                            startActivity(intent); // 🌟 এখন শুধু এই লাইনটাই থাকবে
                            
                        } else {
                            Toast.makeText(FolderActivity.this, "নোটটি ডাটাবেসে পাওয়া যায়নি!", Toast.LENGTH_SHORT).show();
                        }
                        cursor.close();
                        db.close();
                    }
                }

            });


           holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showOptionsBottomSheet(file);// 👈 এখানে আপডেট করা হয়েছে
                    return true;
                }
            });
        }

        @Override
        public int getItemCount() {
            return fileList.size();
        }

        class FileViewHolder extends RecyclerView.ViewHolder {
            TextView tvIcon, tvName;
            public FileViewHolder(@NonNull View itemView, TextView icon, TextView name) {
                super(itemView);
                tvIcon = icon;
                tvName = name;
            }
        }
    }


    // 🌟 লং-প্রেস করলে সিগনেচার বটম শিটে অপশন দেখাবে 🌟
    private void showOptionsBottomSheet(final File file) {
        final BottomSheetDialog sheet = new BottomSheetDialog(this);
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(surfaceColor);
        rootLayout.setPadding(64, 64, 64, 64);

        // ফাইলের নাম (টাইটেল)
        TextView titleView = new TextView(this);
        titleView.setText(file.getName());
        titleView.setTextColor(primaryTextColor);
        titleView.setTextSize(20f);
        titleView.setTypeface(currentTypeface, Typeface.BOLD);
        titleView.setPadding(0, 0, 0, 48);
        rootLayout.addView(titleView);

        // ফাইল হলে ডাউনলোড অপশন দেখাবে
        if (!file.isDirectory()) {
            TextView btnDownload = new TextView(this);
            btnDownload.setText("⬇️  ডাউনলোড করুন (Export)");
            btnDownload.setTextColor(primaryTextColor);
            btnDownload.setTextSize(18f);
            btnDownload.setTypeface(currentTypeface, Typeface.BOLD);
            btnDownload.setPadding(0, 32, 0, 32);
            btnDownload.setOnClickListener(v -> {
                sheet.dismiss();
                downloadFileEncrypted(file);
            });
            rootLayout.addView(btnDownload);
            
            // মাঝখানে একটা দাগ (Divider)
            View divider = new View(this);
            divider.setBackgroundColor(bgColor);
            divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
            rootLayout.addView(divider);
        }

        // ডিলিট অপশন (ফাইল ও ফোল্ডার উভয়ের জন্য)
        TextView btnDelete = new TextView(this);
        btnDelete.setText("🗑️  মুছে ফেলুন (Delete)");
        btnDelete.setTextColor(Color.parseColor("#E53935")); // লাল রঙ
        btnDelete.setTextSize(18f);
        btnDelete.setTypeface(currentTypeface, Typeface.BOLD);
        btnDelete.setPadding(0, 32, 0, 32);
        btnDelete.setOnClickListener(v -> {
            sheet.dismiss();
            confirmDeleteBottomSheet(file); // ডিলিট কনফার্মেশন বটম শিট কল করবে
        });
        rootLayout.addView(btnDelete);

        sheet.setContentView(rootLayout);
        sheet.show();
    }

    // 🌟 ডিলিট কনফার্ম করার সিগনেচার বটম শিট 🌟
    private void confirmDeleteBottomSheet(final File file) {
        final BottomSheetDialog sheet = new BottomSheetDialog(this);
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(surfaceColor);
        rootLayout.setPadding(64, 64, 64, 64);

        TextView titleView = new TextView(this);
        titleView.setText("সতর্কতা!");
        titleView.setTextColor(Color.parseColor("#E53935"));
        titleView.setTextSize(22f);
        titleView.setTypeface(currentTypeface, Typeface.BOLD);
        titleView.setPadding(0, 0, 0, 24);
        rootLayout.addView(titleView);

        TextView tvMessage = new TextView(this);
        tvMessage.setText("আপনি কি '" + file.getName() + "' মুছে ফেলতে চান? এই কাজ আর ফেরানো যাবে না।");
        tvMessage.setTextColor(secondaryTextColor);
        tvMessage.setTextSize(16f);
        tvMessage.setLineSpacing(0, 1.2f);
        tvMessage.setTypeface(currentTypeface);
        tvMessage.setPadding(0, 0, 0, 64);
        rootLayout.addView(tvMessage);

        // বাটন রাখার জন্য লেআউট
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);

        // বাতিল বাটন
        TextView btnCancel = new TextView(this);
        btnCancel.setText("বাতিল");
        btnCancel.setTextColor(primaryTextColor);
        btnCancel.setTextSize(16f);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setTypeface(currentTypeface, Typeface.BOLD);
        btnCancel.setPadding(0, 32, 0, 32);
        LinearLayout.LayoutParams param1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        param1.setMargins(0, 0, 16, 0);
        btnCancel.setLayoutParams(param1);
        
        GradientDrawable bgCancel = new GradientDrawable();
        bgCancel.setColor(bgColor);
        bgCancel.setCornerRadius(100f);
        btnCancel.setBackground(bgCancel);
        btnCancel.setOnClickListener(v -> sheet.dismiss());

        // মুছে ফেলুন বাটন
        TextView btnConfirm = new TextView(this);
        btnConfirm.setText("মুছে ফেলুন");
        btnConfirm.setTextColor(Color.WHITE);
        btnConfirm.setTextSize(16f);
        btnConfirm.setGravity(Gravity.CENTER);
        btnConfirm.setTypeface(currentTypeface, Typeface.BOLD);
        btnConfirm.setPadding(0, 32, 0, 32);
        LinearLayout.LayoutParams param2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        param2.setMargins(16, 0, 0, 0);
        btnConfirm.setLayoutParams(param2);
        
        GradientDrawable bgConfirm = new GradientDrawable();
        bgConfirm.setColor(Color.parseColor("#E53935"));
        bgConfirm.setCornerRadius(100f);
        btnConfirm.setBackground(bgConfirm);
        btnConfirm.setOnClickListener(v -> {
            sheet.dismiss();
            deleteRecursive(file);
            loadDirectory(currentDir);
            Toast.makeText(FolderActivity.this, "মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show();
        });

        btnLayout.addView(btnCancel);
        btnLayout.addView(btnConfirm);
        rootLayout.addView(btnLayout);

        sheet.setContentView(rootLayout);
        sheet.show();
    }

    // ফোল্ডার এবং তার ভেতরের সব ফাইল ডিলিট করার লজিক
        // 🌟 ফোল্ডার এবং তার ভেতরের সব ফাইল ফিজিক্যালি এবং ডাটাবেস থেকে ডিলিট করার স্মার্ট লজিক 🌟
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            // যদি ফোল্ডার হয়, তবে এর ভেতরের সব ফাইলে আগে এই লজিকটা চালাবে
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child); 
                }
            }
        } else {
            // 🌟 যদি ফাইল হয় (.tpad), তবে আগে ডাটাবেস থেকে ডিলিট করবে 🌟
            if (fileOrDirectory.getName().endsWith(".tpad")) {
                String fileName = fileOrDirectory.getName().replace(".tpad", ""); // .tpad বাদ দিয়ে আসল নাম বের করা
                try {
                    android.database.sqlite.SQLiteDatabase db = openOrCreateDatabase("notes_db_v3", MODE_PRIVATE, null);
                    // ডাটাবেসের notes টেবিল থেকে ওই নামের এন্ট্রি ডিলিট করা
                    db.delete("notes", "title=?", new String[]{fileName});
                    db.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        // 🌟 সবার শেষে ফিজিক্যাল ফাইল বা ফোল্ডারটা ডিলিট করে দেবে 🌟
        fileOrDirectory.delete();
    }
    
    
        // 🌟 ডুপ্লিকেট নামের জন্য সুন্দর বটম শিট ওয়ার্নিং (FolderActivity) 🌟
    private void showDuplicateNameWarning() {
        final BottomSheetDialog sheet = new BottomSheetDialog(this);
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(surfaceColor);
        rootLayout.setPadding(64, 64, 64, 64);
        rootLayout.setGravity(Gravity.CENTER);

        TextView tvIcon = new TextView(this);
        tvIcon.setText("⚠️");
        tvIcon.setTextSize(48f);
        tvIcon.setGravity(Gravity.CENTER);
        rootLayout.addView(tvIcon);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("একই নামের ফাইল!");
        tvTitle.setTextColor(primaryTextColor);
        tvTitle.setTextSize(20f);
        tvTitle.setTypeface(currentTypeface, Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 24, 0, 16);
        rootLayout.addView(tvTitle);

        TextView tvMessage = new TextView(this);
        tvMessage.setText("এই ফোল্ডারে একই নামে একটি ফাইল বা ফোল্ডার আগে থেকেই আছে। দয়া করে অন্য কোনো নাম দিয়ে চেষ্টা করুন।");
        tvMessage.setTextColor(secondaryTextColor);
        tvMessage.setTextSize(16f);
        tvMessage.setGravity(Gravity.CENTER);
        tvMessage.setLineSpacing(0, 1.2f);
        tvMessage.setPadding(0, 0, 0, 48);
        rootLayout.addView(tvMessage);

        TextView btnOk = new TextView(this);
        btnOk.setText("ঠিক আছে");
        btnOk.setTextColor(bgColor);
        btnOk.setTextSize(16f);
        btnOk.setTypeface(currentTypeface, Typeface.BOLD);
        btnOk.setGravity(Gravity.CENTER);
        btnOk.setPadding(0, 32, 0, 32);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(accentColor);
        btnBg.setCornerRadius(100f);
        btnOk.setBackground(btnBg);

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheet.dismiss();
            }
        });

        rootLayout.addView(btnOk);
        sheet.setContentView(rootLayout);
        sheet.show();
    }
    
   
    // 🌟 ফাইল ডাউনলোড এবং এনক্রিপ্ট করার মেথড 🌟
    private void downloadFileEncrypted(File file) {
        String fileName = file.getName().replace(".tpad", "");
        String uniqueDbTitle = fileName;

        // ডেটাবেস থেকে ইউনিক টাইটেল বের করা
        String relativePath = currentDir.getAbsolutePath().replace(getFilesDir().getAbsolutePath() + "/TunePad_Data/", "");
        if (relativePath.startsWith("Projects/")) {
            String[] parts = relativePath.split("/");
            String exactProjName = (parts.length >= 3) ? parts[2] : currentDir.getName();
            uniqueDbTitle = exactProjName + "_" + fileName;
        }

        String fileContent = "";
        try {
            android.database.sqlite.SQLiteDatabase db = openOrCreateDatabase("notes_db_v3", MODE_PRIVATE, null);
            // প্রথমে ইউনিক নাম দিয়ে খুঁজবে
            android.database.Cursor cursor = db.rawQuery("SELECT content FROM notes WHERE title=?", new String[]{uniqueDbTitle});
            if (!cursor.moveToFirst()) {
                // না পেলে পুরনো নাম দিয়ে খুঁজবে
                cursor.close();
                cursor = db.rawQuery("SELECT content FROM notes WHERE title=?", new String[]{fileName});
            }
            if (cursor.moveToFirst()) {
                fileContent = cursor.getString(0);
            }
            cursor.close();
            db.close();

            if (fileContent.isEmpty()) {
                Toast.makeText(this, "ফাইলে কোনো লেখা নেই বা ডেটাবেসে পাওয়া যায়নি!", Toast.LENGTH_SHORT).show();
                return;
            }

            // ১. লেখাকে এনক্রিপ্ট করা
            String encryptedData = SecurityUtils.encrypt(fileContent);

            if (encryptedData == null) {
                Toast.makeText(this, "এনক্রিপ্ট করতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show();
                return;
            }

                        // ২. ফোনের Downloads ফোল্ডারের ভেতরে 'TunePad' ফোল্ডারে সেভ করা
            File downloadsRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File appDownloadDir = new File(downloadsRoot, "TunePad"); // আপনার অ্যাপের নাম
            if (!appDownloadDir.exists()) appDownloadDir.mkdirs();
            
            File tpadFile = new File(appDownloadDir, fileName + ".tpad");

            FileOutputStream fos = new FileOutputStream(tpadFile);
            fos.write(encryptedData.getBytes());
            fos.close();

            Toast.makeText(this, "ফাইলটি এনক্রিপ্ট হয়ে Downloads ফোল্ডারে সেভ হয়েছে!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "ফাইল ডাউনলোড করতে ব্যর্থ হয়েছে: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }



}
