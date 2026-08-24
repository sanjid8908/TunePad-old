package com.megh.notepad;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AllProjectsActivity extends AppCompatActivity {

    private LinearLayout allProjectsContainer;
    
    // 🌟 থিম এবং কালার ভেরিয়েবল 🌟
    private Typeface currentTypeface;
    private int bgColor;
    private int surfaceColor;
    private int primaryTextColor;
    private int secondaryTextColor;
    private int accentColor;

    // 🌟 কভার ইমেজের জন্য 🌟
    private File pendingCoverProjectDir = null;
    private static final int PICK_COVER_REQUEST = 1005;
    
    // 🌟 ইমপোর্টের জন্য রিকোয়েস্ট কোড 🌟
    private static final int IMPORT_REQUEST_CODE = 3005;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        applyThemeColors();
        
        // ==========================================
        // 🌟 ম্যাজিক ১: স্ট্যাটাস বারের নীল রঙ সরিয়ে থিমের কালার দেওয়া হলো 🌟
        // ==========================================
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(bgColor);
            getWindow().setNavigationBarColor(bgColor); // নিচের নেভিগেশন বারও ডার্ক হয়ে যাবে
        }
        
        setContentView(R.layout.all_projects);

        allProjectsContainer = findViewById(R.id.allProjectsContainer);
        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvTopTitle = findViewById(R.id.tvTopTitle);

        View mainLayout = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        if (mainLayout != null) {
            mainLayout.setBackgroundColor(bgColor);
        }

        if(tvTopTitle != null) {
            tvTopTitle.setTextColor(primaryTextColor);
            tvTopTitle.setTypeface(currentTypeface, Typeface.BOLD);
            
            ViewGroup headerLayout = (ViewGroup) tvTopTitle.getParent();
            if (headerLayout instanceof LinearLayout) {
                LinearLayout.LayoutParams titleParams = (LinearLayout.LayoutParams) tvTopTitle.getLayoutParams();
                titleParams.width = 0;
                titleParams.weight = 1.0f;
                tvTopTitle.setLayoutParams(titleParams);
                
                ImageView btnMoreMenu = new ImageView(this);
                btnMoreMenu.setImageResource(android.R.drawable.ic_menu_more); 
                btnMoreMenu.setColorFilter(primaryTextColor);
                btnMoreMenu.setPadding(24, 24, 24, 24);
                
                LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(
                        (int)(48 * getResources().getDisplayMetrics().density), 
                        (int)(48 * getResources().getDisplayMetrics().density));
                moreParams.gravity = Gravity.CENTER_VERTICAL;
                btnMoreMenu.setLayoutParams(moreParams);
                
                headerLayout.addView(btnMoreMenu);
                btnMoreMenu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showImportMenu();
                    }
                });
            }
        }
        
        if(btnBack != null) {
            btnBack.setColorFilter(primaryTextColor);
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish(); 
                }
            });
        }

        loadAllProjectsInGrid();
    }

    private void applyThemeColors() {
        bgColor = ThemeHelper.getBgColor(this);
        surfaceColor = ThemeHelper.getSurfaceColor(this);
        accentColor = ThemeHelper.getAccentColor(this);
        primaryTextColor = ThemeHelper.getPrimaryTextColor(this);
        secondaryTextColor = ThemeHelper.getSecondaryTextColor(this);

        SharedPreferences appSettings = getSharedPreferences("AppSettings", MODE_PRIVATE);
        if (appSettings.getInt("font_style", 0) == 3) {
            try { currentTypeface = Typeface.createFromAsset(getAssets(), "fonts/bangla.ttf"); } catch (Exception e) { currentTypeface = Typeface.DEFAULT; }
        } else {
            currentTypeface = Typeface.DEFAULT;
        }
        
        
        android.graphics.Typeface currentTypeface = ThemeHelper.getCustomTypeface(this);
        
        // ২. আপনার ম্যাজিক মেথড দিয়ে পুরো স্ক্রিনে বসিয়ে দেওয়া
        applyFontToAllViews(getWindow().getDecorView(), currentTypeface);
    }

    private void loadAllProjectsInGrid() {
        allProjectsContainer.removeAllViews();

        File projDir = new File(getFilesDir(), "TunePad_Data/Projects");
        List<File> allProjectsList = new ArrayList<>();
        
        File[] categories = projDir.listFiles();
        if (categories != null) {
            for (File cat : categories) {
                if (cat.isDirectory()) {
                    File[] projects = cat.listFiles();
                    if (projects != null) {
                        for (File proj : projects) {
                            if (proj.isDirectory()) allProjectsList.add(proj);
                        }
                    }
                }
            }
        }

        // ==========================================
        // 🌟 ম্যাজিক ২: সব প্রজেক্টের লিস্টেও নতুনগুলো সবার উপরে দেখানোর লজিক 🌟
        // ==========================================
        java.util.Collections.sort(allProjectsList, new java.util.Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return Long.compare(f2.lastModified(), f1.lastModified());
            }
        });
        // ==========================================

        int totalProjects = allProjectsList.size();
        LinearLayout currentRow = null;

        for (int i = 0; i < totalProjects; i++) {
            if (i % 3 == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                currentRow.setWeightSum(3f);
                allProjectsContainer.addView(currentRow);
            }

            File proj = allProjectsList.get(i);
            
            int fileCount = 0;
            File[] files = proj.listFiles();
            if (files != null) {
                for(File f : files) {
                    if (f.isFile() && f.getName().endsWith(".tpad")) fileCount++;
                }
            }
            
            View card = createDynamicProjectCard(proj.getName(), proj.getParentFile().getName(), fileCount, proj);
            if (currentRow != null) {
                currentRow.addView(card);
            }
        }

        if (totalProjects % 3 != 0 && currentRow != null) {
            int emptySlots = 3 - (totalProjects % 3);
            for (int e = 0; e < emptySlots; e++) {
                View emptyView = new View(this);
                LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(0, 1, 1.0f);
                emptyParams.setMargins(12, 12, 12, 24);
                emptyView.setLayoutParams(emptyParams);
                currentRow.addView(emptyView);
            }
        }
    }

    private View createDynamicProjectCard(final String title, final String category, int count, final File projDir) {
        RelativeLayout card = new RelativeLayout(this);
        
        int cardHeight = (int) (getResources().getDisplayMetrics().density * 155); 
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, cardHeight, 1.0f);
        params.setMargins(12, 12, 12, 24); 
        card.setLayoutParams(params);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(surfaceColor); 
        bg.setCornerRadius(24f); 
        card.setBackground(bg);
        card.setElevation(6f);
        card.setClipToOutline(true);

        ImageView coverImg = new ImageView(this);
        coverImg.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        coverImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        File coverFile = new File(projDir, "cover.jpg");
        if(coverFile.exists()) {
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inSampleSize = 4; 
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(coverFile.getAbsolutePath(), options);
            coverImg.setImageBitmap(bitmap);
        } else {
            coverImg.setImageResource(android.R.drawable.ic_menu_gallery);
            coverImg.setColorFilter(Color.parseColor("#9CA8AE"));
            coverImg.setScaleType(ImageView.ScaleType.CENTER);
            coverImg.setBackgroundColor(Color.parseColor("#2A3439"));
        }
        card.addView(coverImg);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        textLayout.setLayoutParams(textParams);
        
        GradientDrawable gradientBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.TRANSPARENT, Color.parseColor("#CC000000"), Color.parseColor("#E6000000")});
        textLayout.setBackground(gradientBg);
        textLayout.setPadding(16, 48, 16, 16); 

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.WHITE); 
        tvTitle.setTextSize(13f); 
        tvTitle.setTypeface(currentTypeface, Typeface.BOLD);
        tvTitle.setMaxLines(2);
        tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tvTitle.setShadowLayer(4f, 0f, 2f, Color.BLACK);
        textLayout.addView(tvTitle);
        
        TextView tvCategory = new TextView(this);
        tvCategory.setText(category);
        tvCategory.setTextColor(accentColor); 
        tvCategory.setTextSize(10f); 
        tvCategory.setTypeface(currentTypeface, Typeface.BOLD);
        tvCategory.setPadding(0, 4, 0, 2);
        textLayout.addView(tvCategory);
        
        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText(count + " পর্ব");
        tvSubtitle.setTextColor(Color.parseColor("#DDDDDD"));
        tvSubtitle.setTextSize(9f);
        textLayout.addView(tvSubtitle);

        card.addView(textLayout);

        TextView btnMore = new TextView(this);
        btnMore.setText("⋮"); 
        btnMore.setTextColor(Color.WHITE);
        btnMore.setTextSize(24f);
        btnMore.setTypeface(null, Typeface.BOLD);
        btnMore.setGravity(Gravity.CENTER);
        
        RelativeLayout.LayoutParams moreParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        moreParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        moreParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        moreParams.setMargins(0, 8, 8, 0);
        btnMore.setLayoutParams(moreParams);

        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setColor(Color.parseColor("#66000000"));
        dotBg.setCornerRadius(100f);
        btnMore.setBackground(dotBg);
        btnMore.setPadding(16, -10, 16, 10); 
        card.addView(btnMore);

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AllProjectsActivity.this, ProjectViewActivity.class);
                intent.putExtra("PROJECT_NAME", title);
                intent.putExtra("CATEGORY_NAME", category);
                startActivity(intent);
            }
        });

        btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProjectOptionsBottomSheet(projDir, title, category);
            }
        });
        
        return card;
    }

    private void showProjectOptionsBottomSheet(final File projDir, final String projTitle, final String category) {
        final BottomSheetDialog sheet = new BottomSheetDialog(this);
        LinearLayout root = new LinearLayout(this); 
        root.setOrientation(LinearLayout.VERTICAL); 
        root.setBackgroundColor(surfaceColor); 
        root.setPadding(0, 32, 0, 32);
        
        TextView title = new TextView(this); 
        title.setText(projTitle); 
        title.setTextColor(accentColor); 
        title.setTextSize(20f); 
        title.setTypeface(currentTypeface, Typeface.BOLD); 
        title.setPadding(64, 32, 64, 48); 
        root.addView(title);

        root.addView(createMenuItem("কভার ফটো সেট করুন", android.R.drawable.ic_menu_gallery, new View.OnClickListener() { 
            @Override public void onClick(View v) { 
                sheet.dismiss(); 
                pendingCoverProjectDir = projDir;
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_COVER_REQUEST);
            } 
        }));
        
        root.addView(createMenuItem("পড়ুন 📖", android.R.drawable.ic_menu_info_details, new View.OnClickListener() { 
            @Override public void onClick(View v) { 
                sheet.dismiss(); 
                Intent intent = new Intent(AllProjectsActivity.this, ProjectReadActivity.class);
                intent.putExtra("PROJECT_NAME", projTitle);
                intent.putExtra("CATEGORY_NAME", category);
                startActivity(intent);
            } 
        }));
        
        root.addView(createMenuItem("প্রজেক্ট এক্সপোর্ট করুন (.tbox)", android.R.drawable.ic_menu_save, new View.OnClickListener() {
            @Override public void onClick(View v) {
                sheet.dismiss();
                AllProjectsActivity.this.exportProjectAsTbox(projDir, projTitle);
            }
        }));
        
        
        // 🌟 স্মার্ট শেয়ার বাটন 🌟
		root.addView(createMenuItem("লিংক শেয়ার করুন 🔗", android.R.drawable.ic_menu_share, new android.view.View.OnClickListener() { 
			@Override
			public void onClick(android.view.View v) {
				sheet.dismiss(); // আপনার বটম শিটের ভেরিয়েবল নাম যদি sheet হয়
				if (isNetworkAvailable()) {
					uploadAndShareProject(projDir, projTitle);
				} else {
					android.widget.Toast.makeText(getApplicationContext(), "ইন্টারনেট নেই! অফলাইন লিংক তৈরি করা হচ্ছে... 📶", android.widget.Toast.LENGTH_SHORT).show();
					shareOfflinePredictableLink(projTitle);
				}
			}
		}));

		// 🌟 অটো-ব্যাকআপ টগল বাটন 🌟
		final android.content.SharedPreferences autoBackupPrefs = getSharedPreferences("AutoBackupPrefs", android.content.Context.MODE_PRIVATE);
		final boolean isAutoBackupOn = autoBackupPrefs.getBoolean("auto_backup_" + projTitle, false);

		root.addView(createMenuItem(isAutoBackupOn ? "অটো-ব্যাকআপ বন্ধ করুন 🛑" : "অটো-ব্যাকআপ চালু করুন (৫ ঘণ্টা) 🔄", 
			android.R.drawable.ic_popup_sync, new android.view.View.OnClickListener() { 
			@Override
			public void onClick(android.view.View v) {
				sheet.dismiss();
				boolean newState = !isAutoBackupOn;
				autoBackupPrefs.edit().putBoolean("auto_backup_" + projTitle, newState).apply();
				android.widget.Toast.makeText(getApplicationContext(), newState ? "'" + projTitle + "' এর অটো-ব্যাকআপ চালু হয়েছে!" : "'" + projTitle + "' এর অটো-ব্যাকআপ বন্ধ করা হয়েছে!", android.widget.Toast.LENGTH_SHORT).show();
			}
		}));

        root.addView(createMenuItem("প্রজেক্ট ডিলিট করুন", android.R.drawable.ic_menu_delete, new View.OnClickListener() { 
            @Override public void onClick(View v) { 
                sheet.dismiss(); 
                showDeleteProjectWarning(projDir, projTitle);
            } 
        }));

        sheet.setContentView(root); 
        sheet.show();
    }

    private LinearLayout createMenuItem(String text, int iconRes, View.OnClickListener listener) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setPadding(64, 32, 64, 32);
        layout.setOnClickListener(listener);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(primaryTextColor);
        layout.addView(icon, new LinearLayout.LayoutParams(56, 56));

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(primaryTextColor);
        tv.setTextSize(16f);
        tv.setTypeface(currentTypeface);
        tv.setPadding(32, 0, 0, 0);
        layout.addView(tv);

        return layout;
    }

    // ==========================================
    // 🌟 ম্যাজিক ৩: পুরনো বোরিং অ্যালার্টের বদলে সিগনেচার ডিলিট শিট 🌟
    // ==========================================
    private void showDeleteProjectWarning(final File projDir, final String projTitle) {
        final BottomSheetDialog deleteSheet = new BottomSheetDialog(this);
        
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(surfaceColor);
        rootLayout.setPadding(64, 80, 64, 64);
        rootLayout.setGravity(Gravity.CENTER);
        
        ImageView dangerIcon = new ImageView(this);
        dangerIcon.setImageResource(android.R.drawable.ic_menu_delete); 
        dangerIcon.setColorFilter(Color.parseColor("#E53935")); 
        rootLayout.addView(dangerIcon, new LinearLayout.LayoutParams(140, 140));
        
        TextView tvTitle = new TextView(this);
        tvTitle.setText("মুছে ফেলবেন?");
        tvTitle.setTextColor(primaryTextColor);
        tvTitle.setTextSize(22f);
        tvTitle.setTypeface(currentTypeface, Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 32, 0, 16);
        rootLayout.addView(tvTitle);

        TextView tvMsg = new TextView(this);
        tvMsg.setText("আপনি কি সত্যিই '" + projTitle + "' মুছে ফেলতে চান? এটি আর ফিরিয়ে আনা সম্ভব হবে না।");
        tvMsg.setTextColor(secondaryTextColor);
        tvMsg.setTextSize(15f);
        tvMsg.setTypeface(currentTypeface);
        tvMsg.setGravity(Gravity.CENTER);
        tvMsg.setPadding(0, 0, 0, 64);
        tvMsg.setLineSpacing(0, 1.3f);
        rootLayout.addView(tvMsg);
        
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        
        TextView btnCancel = new TextView(this);
        btnCancel.setText("না, থাক");
        btnCancel.setTextColor(primaryTextColor);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setTextSize(16f);
        btnCancel.setTypeface(currentTypeface, Typeface.BOLD);
        btnCancel.setPadding(0, 32, 0, 32);
        GradientDrawable cancelBg = new GradientDrawable();
        cancelBg.setColor(bgColor);
        cancelBg.setCornerRadius(100f);
        btnCancel.setBackground(cancelBg);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        cancelParams.setMargins(0, 0, 16, 0);
        btnCancel.setLayoutParams(cancelParams);
        
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSheet.dismiss(); 
            }
        });
        
        TextView btnConfirm = new TextView(this);
        btnConfirm.setText("হ্যাঁ, মুছে ফেলুন");
        btnConfirm.setTextColor(Color.WHITE);
        btnConfirm.setGravity(Gravity.CENTER);
        btnConfirm.setTextSize(16f);
        btnConfirm.setTypeface(currentTypeface, Typeface.BOLD);
        btnConfirm.setPadding(0, 32, 0, 32);
        GradientDrawable confirmBg = new GradientDrawable();
        confirmBg.setColor(Color.parseColor("#E53935")); 
        confirmBg.setCornerRadius(100f);
        btnConfirm.setBackground(confirmBg);
        LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        confirmParams.setMargins(16, 0, 0, 0);
        btnConfirm.setLayoutParams(confirmParams);
        
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSheet.dismiss();
                // ⬇️ ডিলিট লজিক শুরু ⬇️
                try {
                    android.database.sqlite.SQLiteDatabase db = openOrCreateDatabase("notes_db_v3", MODE_PRIVATE, null);
                    db.delete("notes", "label=?", new String[]{"Project: " + projTitle});
                    File[] files = projDir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (f.getName().endsWith(".tpad")) {
                                String fileName = f.getName().replace(".tpad", "");
                                String uniqueDbTitle = projTitle + "_" + fileName;
                                db.delete("notes", "title=? OR title=?", new String[]{uniqueDbTitle, fileName});
                            }
                        }
                    }
                    db.close();
                } catch (Exception e) {}
                
                SharedPreferences allCharPrefs = getSharedPreferences("Global_Characters_DB", MODE_PRIVATE);
                int charCount = allCharPrefs.getInt("char_count", 0);
                SharedPreferences.Editor charEditor = allCharPrefs.edit();
                for (int i = 0; i < charCount; i++) {
                    if (allCharPrefs.getBoolean("char_active_" + i, false)) {
                        String story = allCharPrefs.getString("char_story_" + i, "");
                        if (story.trim().equalsIgnoreCase(projTitle.trim())) {
                            charEditor.putBoolean("char_active_" + i, false);
                        }
                    }
                }
                charEditor.apply();
                
                deleteRecursiveFolder(projDir); 
                
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AllProjectsActivity.this, "প্রজেক্ট মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show();
                        loadAllProjectsInGrid(); 
                    }
                }, 300);
            }
        });
        
        btnLayout.addView(btnCancel);
        btnLayout.addView(btnConfirm);
        rootLayout.addView(btnLayout);
        
        deleteSheet.setContentView(rootLayout);
        deleteSheet.show();
    }

    private void deleteRecursiveFolder(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) deleteRecursiveFolder(child);
        }
        fileOrDirectory.delete();
    }

    private void showImportMenu() {
        final BottomSheetDialog sheet = new BottomSheetDialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(surfaceColor);
        root.setPadding(0, 32, 0, 32);
        
        root.addView(createMenuItem("প্রজেক্ট ইমপোর্ট করুন (.tbox)", android.R.drawable.ic_menu_upload, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheet.dismiss();
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*"); 
                startActivityForResult(intent, IMPORT_REQUEST_CODE);
            }
        }));
        
        sheet.setContentView(root);
        sheet.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_COVER_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            if (pendingCoverProjectDir != null) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(data.getData());
                    File coverFile = new File(pendingCoverProjectDir, "cover.jpg");
                    FileOutputStream outputStream = new FileOutputStream(coverFile);
                    
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) outputStream.write(buffer, 0, length);
                    
                    inputStream.close();
                    outputStream.close();
                    
                    Toast.makeText(this, "কভার ফটো সেট করা হয়েছে!", Toast.LENGTH_SHORT).show();
                    loadAllProjectsInGrid(); 
                } catch (Exception e) {
                    Toast.makeText(this, "ছবি সেট করতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show();
                }
            }
        }
        
        if (requestCode == IMPORT_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            processImportedTbox(data.getData());
        }
    }

    private String getBanglaNumber(int number) {
        String[] engDigits = {"0","1","2","3","4","5","6","7","8","9"};
        String[] bngDigits = {"০","১","২","৩","৪","৫","৬","৭","৮","৯"};
        String numStr = String.valueOf(number);
        for (int i = 0; i < engDigits.length; i++) {
            numStr = numStr.replace(engDigits[i], bngDigits[i]);
        }
        return numStr;
    }

    private void processImportedTbox(android.net.Uri uri) {
        File projectDir = null; 
        android.database.sqlite.SQLiteDatabase db = null;
        android.database.Cursor cursor = null;

        try {
            String fileName = "Imported_Project";
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) fileName = cursor.getString(nameIndex);
            }
            if (cursor != null) cursor.close();

            String originalProjectName = fileName.replace(".tbox", "").replaceAll("[^a-zA-Z0-9_\\-\\u0980-\\u09FF ]", "").trim();
            if (originalProjectName.isEmpty()) originalProjectName = "Imported_Project"; 
            String projectName = originalProjectName;

            projectDir = new File(getFilesDir(), "TunePad_Data/Projects/General/" + projectName);
            int counter = 1;
            while (projectDir.exists()) {
                String banglaCount = getBanglaNumber(counter); 
                projectName = originalProjectName + " (ইমপোর্ট " + banglaCount + ")"; 
                projectDir = new File(getFilesDir(), "TunePad_Data/Projects/General/" + projectName);
                counter++;
            }
            projectDir.mkdirs();

            InputStream inputStream = getContentResolver().openInputStream(uri);
            TBoxUtils.decryptAndUnzipFolder(inputStream, projectDir);

            File metaFile = new File(projectDir, "project_meta_data.json");
            if (metaFile.exists()) {
                java.io.FileInputStream fis = new java.io.FileInputStream(metaFile);
                byte[] dataBytes = new byte[(int) metaFile.length()];
                fis.read(dataBytes);
                fis.close();
                String jsonString = new String(dataBytes, "UTF-8");

                org.json.JSONObject json = new org.json.JSONObject(jsonString);
                SharedPreferences.Editor editor = getSharedPreferences("ProjectData_" + projectName, MODE_PRIVATE).edit();

                if (json.has("project_characters")) {
                    org.json.JSONArray charArray = json.getJSONArray("project_characters");
                    SharedPreferences charPrefs = getSharedPreferences("Global_Characters_DB", MODE_PRIVATE);
                    SharedPreferences.Editor charEditor = charPrefs.edit();
                    
                    int nextAvailableId = charPrefs.getInt("char_count", 0);
                    java.util.HashMap<Integer, Integer> oldToNewIdMap = new java.util.HashMap<>();
                    java.util.HashMap<Integer, String> tempRelsMap = new java.util.HashMap<>();
                    
                    for (int i = 0; i < charArray.length(); i++) {
                        org.json.JSONObject charObj = charArray.getJSONObject(i);
                        
                        int newId = nextAvailableId; 
                        nextAvailableId++; 
                        
                        int oldId = charObj.optInt("old_id", -1);
                        if (oldId != -1) oldToNewIdMap.put(oldId, newId);
                        tempRelsMap.put(newId, charObj.optString("rels", ""));
                        
                        charEditor.putBoolean("char_active_" + newId, true);
                        charEditor.putString("char_story_" + newId, projectName); 
                        
                        charEditor.putString("char_name_" + newId, charObj.optString("name", ""));
                        charEditor.putString("char_nickname_" + newId, charObj.optString("nickname", ""));
                        charEditor.putString("char_role_" + newId, charObj.optString("role", ""));
                        charEditor.putString("char_img_" + newId, charObj.optString("img", "")); 
                        charEditor.putString("char_dob_" + newId, charObj.optString("dob", ""));
                        charEditor.putString("char_age_" + newId, charObj.optString("age", ""));
                        charEditor.putString("char_country_" + newId, charObj.optString("country", ""));
                        charEditor.putString("char_location_" + newId, charObj.optString("location", ""));
                        charEditor.putString("char_occupation_" + newId, charObj.optString("occupation", ""));
                        charEditor.putString("char_height_" + newId, charObj.optString("height", ""));
                        charEditor.putString("char_build_" + newId, charObj.optString("build", ""));
                        charEditor.putString("char_eye_hair_" + newId, charObj.optString("eye_hair", ""));
                        charEditor.putString("char_marks_" + newId, charObj.optString("marks", ""));
                        charEditor.putString("char_clothing_" + newId, charObj.optString("clothing", ""));
                        charEditor.putString("char_personality_" + newId, charObj.optString("personality", ""));
                        charEditor.putString("char_strengths_" + newId, charObj.optString("strengths", ""));
                        charEditor.putString("char_flaws_" + newId, charObj.optString("flaws", ""));
                        charEditor.putString("char_habits_" + newId, charObj.optString("habits", ""));
                        charEditor.putString("char_goal_" + newId, charObj.optString("goal", ""));
                        charEditor.putString("char_fear_" + newId, charObj.optString("fear", ""));
                        charEditor.putString("char_secrets_" + newId, charObj.optString("secrets", ""));
                        charEditor.putString("char_conflict_" + newId, charObj.optString("conflict", ""));
                        charEditor.putString("char_backstory_" + newId, charObj.optString("backstory", ""));
                        
                        int customCount = charObj.optInt("custom_count", 0);
                        charEditor.putInt("char_custom_count_" + newId, customCount);
                        for (int j = 0; j < customCount; j++) {
                            charEditor.putString("char_custom_key_" + newId + "_" + j, charObj.optString("custom_key_" + j, ""));
                            charEditor.putString("char_custom_val_" + newId + "_" + j, charObj.optString("custom_val_" + j, ""));
                        }
                    }
                    
                    charEditor.putInt("char_count", nextAvailableId);
                    
                    for (java.util.Map.Entry<Integer, String> entry : tempRelsMap.entrySet()) {
                        int currentNewId = entry.getKey();
                        String oldRels = entry.getValue();
                        
                        if (!oldRels.isEmpty()) {
                            StringBuilder newRelsBuilder = new StringBuilder();
                            String[] relList = oldRels.split(";;");
                            for (String rel : relList) {
                                if (rel.contains("|")) {
                                    String[] parts = rel.split("\\|");
                                    try {
                                        int targetOldId = Integer.parseInt(parts[0]);
                                        String relationType = parts[1];
                                        if (oldToNewIdMap.containsKey(targetOldId)) {
                                            int targetNewId = oldToNewIdMap.get(targetOldId);
                                            if (newRelsBuilder.length() > 0) newRelsBuilder.append(";;");
                                            newRelsBuilder.append(targetNewId).append("|").append(relationType);
                                        }
                                    } catch (Exception e) {}
                                }
                            }
                            charEditor.putString("char_rels_" + currentNewId, newRelsBuilder.toString());
                        }
                    }
                    charEditor.apply();
                }

                java.util.Iterator<String> keys = json.keys();
                while(keys.hasNext()) {
                    String key = keys.next();
                    if (key.equals("project_characters")) continue; 
                    
                    Object val = json.get(key);
                    if (val instanceof String) editor.putString(key, (String)val);
                    else if (val instanceof Integer) editor.putInt(key, (Integer)val);
                    else if (val instanceof Boolean) editor.putBoolean(key, (Boolean)val);
                    else if (val instanceof Long) editor.putLong(key, (Long)val);
                    else if (val instanceof Float) editor.putFloat(key, (Float)val);
                }
                editor.apply();
                metaFile.delete(); 
            }

            File[] files = projectDir.listFiles();
            if (files != null) {
                db = openOrCreateDatabase("notes_db_v3", MODE_PRIVATE, null);
                for (File f : files) {
                    if (f.getName().endsWith(".tpad")) {
                        String title = f.getName().replace(".tpad", "");
                        String uniqueTitle = projectName + "_" + title;

                        java.io.FileInputStream fis = new java.io.FileInputStream(f);
                        byte[] bytes = new byte[(int) f.length()];
                        fis.read(bytes);
                        fis.close();
                        String content = new String(bytes, "UTF-8");

                        android.content.ContentValues cv = new android.content.ContentValues();
                        cv.put("id", "imp_" + System.currentTimeMillis() + "_" + title);
                        cv.put("title", uniqueTitle);
                        cv.put("content", content);
                        cv.put("label", "Project: " + projectName); 
                        cv.put("timestamp", new java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date()));
                        cv.put("isPinned", 0);
                        cv.put("isDeleted", 0);
                        cv.put("isDraft", 0);
                        cv.put("isHidden", 0);

                        db.insertWithOnConflict("notes", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
                    }
                }
            }

            Toast.makeText(this, "সুপার-সিকিউর প্রজেক্ট সফলভাবে রিস্টোর হয়েছে! 🚀", Toast.LENGTH_LONG).show();
            loadAllProjectsInGrid();

        } catch (Exception e) {
            e.printStackTrace();
            if (projectDir != null && projectDir.exists()) {
                deleteRecursiveFolder(projectDir);
            }
            Toast.makeText(this, "ইমপোর্ট করতে সমস্যা হয়েছে: ভুল ফাইল বা পাসওয়ার্ড!", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
            if (db != null && db.isOpen()) db.close();
        }
    }

    private void exportProjectAsTbox(File projDir, String projTitle) {
        File metaDataFile = new File(projDir, "project_meta_data.json");
        try {
            File downloadsRoot = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            File appDownloadDir = new File(downloadsRoot, "TunePad");
            if (!appDownloadDir.exists()) appDownloadDir.mkdirs();

            File tboxFile = new File(appDownloadDir, projTitle + ".tbox");

            saveSharedPreferencesToFile(projTitle, metaDataFile);

            TBoxUtils.zipAndEncryptFolder(projDir, tboxFile);

            Toast.makeText(this, "প্রজেক্ট সফলভাবে " + projTitle + ".tbox নামে Downloads/TunePad-এ সেভ হয়েছে! 🔒", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "প্রজেক্ট এক্সপোর্ট করতে সমস্যা হয়েছে: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (metaDataFile.exists()) {
                metaDataFile.delete();
            }
        }
    }
    private void saveSharedPreferencesToFile(String projTitle, File metaFile) {
        try {
            SharedPreferences prefs = getSharedPreferences("ProjectData_" + projTitle, MODE_PRIVATE);
            org.json.JSONObject json = new org.json.JSONObject();
            java.util.Map<String, ?> allEntries = prefs.getAll();
            for (java.util.Map.Entry<String, ?> entry : allEntries.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
            
            SharedPreferences charPrefs = getSharedPreferences("Global_Characters_DB", MODE_PRIVATE);
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
            
            // 🌟 এই লাইনগুলো আপনার কোডে মিসিং হয়ে গিয়েছিল 🌟
            java.io.FileOutputStream fos = new java.io.FileOutputStream(metaFile);
            fos.write(json.toString().getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    
    	// ==========================================
	// 🌟 ইঞ্জিন ১: ইন্টারনেট কানেকশন চেক 🌟
	// ==========================================
	private boolean isNetworkAvailable() {
		android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
		android.net.NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	// ==========================================
	// 🌟 ইঞ্জিন ২: অফলাইন / ডাইরেক্ট লিংক জেনারেটর 🌟
	// ==========================================
	private void shareOfflinePredictableLink(String projTitle) {
		android.content.SharedPreferences appSettings = getSharedPreferences("AppSettings", MODE_PRIVATE);
		String userNameEn = appSettings.getString("author_name_en", "Unknown_User");
		String safeUserName = userNameEn.replaceAll("[^a-zA-Z0-9_]", "_");
		
		String safeFileName = projTitle.replace(" ", "_") + ".tbox";
		String encodedFileName = safeFileName;
		try { encodedFileName = java.net.URLEncoder.encode(safeFileName, "UTF-8").replace("+", "%20"); } catch (Exception e) {}

		String shareLink = "https://www.shuvraafroj.info/api/users/" + safeUserName + "/Manual_Backup/Projects/" + encodedFileName;
		String beautifulUrl = shareLink;
		try { beautifulUrl = java.net.URLDecoder.decode(shareLink, "UTF-8"); } catch (Exception e) {}

		android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(android.content.Intent.EXTRA_SUBJECT, projTitle);
		intent.putExtra(android.content.Intent.EXTRA_TEXT, "আমার লেখা '" + projTitle + "' প্রজেক্টটি ডাউনলোড করুন:\n\n" + beautifulUrl);
		startActivity(android.content.Intent.createChooser(intent, "লিংক শেয়ার করুন..."));
	}

	// ==========================================
	// 🌟 ইঞ্জিন ৩: রিয়েল আপলোড ও লিংক শেয়ার (Bangla Fix সহ) 🌟
	// ==========================================
	private void uploadAndShareProject(final java.io.File projDir, final String projTitle) {
		// (UI এর জন্য টোস্ট মেথড আপনার ওই অ্যাক্টিভিটিগুলোতে না থাকলে সাধারণ Toast ব্যবহার করতে পারেন)
		android.widget.Toast.makeText(this, "প্রজেক্ট আপলোড হচ্ছে, দয়া করে অপেক্ষা করুন... ⏳", android.widget.Toast.LENGTH_SHORT).show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					java.io.File cacheDir = getCacheDir();
					java.io.File tboxFile = new java.io.File(cacheDir, projTitle + ".tbox");

					// মেটা-ডেটা সেভ করা (আপনার আগের লজিক অনুযায়ী)
					java.io.File metaDataFile = new java.io.File(projDir, "project_meta_data.json");
					
					// নোট: আপনার ProjectViewActivity-তে saveSharedPreferencesToFile মেথডটি থাকতে হবে।
					// যদি না থাকে, তবে আপাতত নিচের লাইনটি কমেন্ট করে রাখতে পারেন।
					// saveSharedPreferencesToFile(projTitle, metaDataFile);

					com.megh.notepad.TBoxUtils.zipAndEncryptFolder(projDir, tboxFile);
					if (metaDataFile.exists()) metaDataFile.delete();

					String uploadUrl = "https://www.shuvraafroj.info/api/upload_project.php";
					String boundary = "*****" + System.currentTimeMillis() + "*****";
					String crlf = "\r\n";
					String twoHyphens = "--";

					java.net.URL url = new java.net.URL(uploadUrl);
					java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
					conn.setUseCaches(false); conn.setDoOutput(true); conn.setRequestMethod("POST");
					conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

					java.io.DataOutputStream request = new java.io.DataOutputStream(conn.getOutputStream());
					android.content.SharedPreferences appSettings = getSharedPreferences("AppSettings", MODE_PRIVATE);
					String userNameEn = appSettings.getString("author_name_en", "Unknown_User"); 

					request.write((twoHyphens + boundary + crlf).getBytes("UTF-8"));
					request.write(("Content-Disposition: form-data; name=\"username\"" + crlf).getBytes("UTF-8"));
					request.write((crlf).getBytes("UTF-8"));
					request.write((userNameEn + crlf).getBytes("UTF-8"));

					request.write((twoHyphens + boundary + crlf).getBytes("UTF-8"));
					request.write(("Content-Disposition: form-data; name=\"project_file\";filename=\"" + tboxFile.getName() + "\"" + crlf).getBytes("UTF-8"));
					request.write((crlf).getBytes("UTF-8"));

					java.io.FileInputStream fileInputStream = new java.io.FileInputStream(tboxFile);
					byte[] buffer = new byte[1024 * 1024]; int bytesRead;
					while ((bytesRead = fileInputStream.read(buffer)) > 0) { request.write(buffer, 0, bytesRead); }
					request.write((crlf).getBytes("UTF-8"));
					request.write((twoHyphens + boundary + twoHyphens + crlf).getBytes("UTF-8"));
					
					fileInputStream.close(); request.flush(); request.close();
					if (tboxFile.exists()) tboxFile.delete();

					final int responseCode = conn.getResponseCode();
					if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
						java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
						StringBuilder response = new StringBuilder();
						String inputLine;
						while ((inputLine = in.readLine()) != null) response.append(inputLine);
						in.close();

						final org.json.JSONObject serverResponse = new org.json.JSONObject(response.toString());
						final String status = serverResponse.optString("status");
						final String message = serverResponse.optString("message");
						final String fileUrl = serverResponse.optString("file_url"); 

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if ("success".equals(status) && fileUrl != null && !fileUrl.isEmpty()) {
									try {
										String beautifulUrl = java.net.URLDecoder.decode(fileUrl, "UTF-8");
										android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
										intent.setType("text/plain");
										intent.putExtra(android.content.Intent.EXTRA_SUBJECT, projTitle);
										intent.putExtra(android.content.Intent.EXTRA_TEXT, "আমার লেখা '" + projTitle + "' প্রজেক্টটি ডাউনলোড করুন:\n\n" + beautifulUrl);
										startActivity(android.content.Intent.createChooser(intent, "লিংক শেয়ার করুন..."));
										android.widget.Toast.makeText(getApplicationContext(), "✅ আপলোড সফল!", android.widget.Toast.LENGTH_SHORT).show();
									} catch (Exception e) {}
								} else {
									android.widget.Toast.makeText(getApplicationContext(), "❌ " + message, android.widget.Toast.LENGTH_SHORT).show();
								}
							}
						});
					}
				} catch (final Exception e) {
					runOnUiThread(new Runnable() { @Override public void run() { android.widget.Toast.makeText(getApplicationContext(), "আপলোড এরর!", android.widget.Toast.LENGTH_SHORT).show(); } });
				}
			}
		}).start();
	}


// 🌟 ইউনিভার্সাল ফন্ট চেঞ্জার (পুরো স্ক্রিনের সব টেক্সটে একসাথে ফন্ট বসিয়ে দেয়) 🌟
private void applyFontToAllViews(android.view.View view, android.graphics.Typeface typeface) {
    if (view instanceof android.view.ViewGroup) {
        android.view.ViewGroup vg = (android.view.ViewGroup) view;
        for (int i = 0; i < vg.getChildCount(); i++) {
            applyFontToAllViews(vg.getChildAt(i), typeface);
        }
    } else if (view instanceof android.widget.TextView) {
        android.graphics.Typeface current = ((android.widget.TextView) view).getTypeface();
        if (current != null && current.isBold()) {
            ((android.widget.TextView) view).setTypeface(typeface, android.graphics.Typeface.BOLD);
        } else {
            ((android.widget.TextView) view).setTypeface(typeface);
        }
    }
}

} // 🌟 ক্লাসের শেষ ব্র্যাকেট! এটি খুব জরুরি 🌟

