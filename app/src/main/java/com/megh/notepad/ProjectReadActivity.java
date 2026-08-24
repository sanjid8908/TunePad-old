package com.megh.notepad;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Layout;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProjectReadActivity extends AppCompatActivity {

    private String projectName, categoryName;
    private File projectDir;

    private DrawerLayout drawerLayout;
    private RelativeLayout rootReadView, toolbarLayout;
    private LinearLayout floatingButtonsLayout;
    private ImageView btnBack, btnReaderSettings, btnTocHeader, imgReadCover;
    private ImageView fabSettings, fabToc;
    private TextView tvBookTitle, tvReadTitle, tvReadAuthor;
    private ScrollView vscroll8;
    private ListView lvToc;
    
    // ডাইনামিক পর্ব লোড করার জন্য কন্টেইনার
    private LinearLayout chapterContainer;
    private List<View> chapterTitleViews = new ArrayList<>(); 

    private boolean isFullScreen = false;
    private GestureDetector gestureDetector;
    private List<String> chapterList = new ArrayList<>();

    // 🌟 সেটিংস ভেরিয়েবল 🌟
    private float tempTextSize = 18f;
    private Typeface tempTypeface = Typeface.DEFAULT;

    private int[] bgColors = {
            Color.parseColor("#FFFFFF"), Color.parseColor("#121212"),
            Color.parseColor("#E3F2FD"), Color.parseColor("#FCE4EC"),
            Color.parseColor("#E8F5E9"), Color.parseColor("#F4ECD8")
    };

    private int[] textColors = {
            Color.parseColor("#212121"), Color.parseColor("#E0E0E0"),
            Color.parseColor("#0D47A1"), Color.parseColor("#880E4F"),
            Color.parseColor("#1B5E20"), Color.parseColor("#3E2723")
    };
    
    private int currentThemeIndex = 5;
    
    // 🌟 ফিক্সড: ফন্ট ইন্ডেক্স সেভ রাখার জন্য ভেরিয়েবল 🌟
    private int currentFontIndex = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // কালো নচ (Notch) এরিয়া দূর করে ফুলস্ক্রিন করার কোড
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        setContentView(R.layout.project_read);

        projectName = getIntent().getStringExtra("PROJECT_NAME");
        categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        if (projectName == null) projectName = "Unknown";
        if (categoryName == null) categoryName = "General";

        projectDir = new File(getFilesDir(), "TunePad_Data/Projects/" + categoryName + "/" + projectName);

        initViews();
        setupGestureDetector();
        
        // 🌟 ফিক্সড: ThemeHelper থেকে সরাসরি ফন্ট লোড 🌟
        SharedPreferences appSettings = getSharedPreferences("AppSettings", MODE_PRIVATE);
        currentFontIndex = appSettings.getInt("font_style", 0);
        tempTypeface = ThemeHelper.getCustomTypeface(this);
        
        applyReaderTheme();
        
        loadFullStory();

        // বাটন লিসেনার্স
        btnBack.setOnClickListener(v -> finish());
        
        View.OnClickListener settingsListener = v -> showReaderSettingsBottomSheet();
        btnReaderSettings.setOnClickListener(settingsListener);
        fabSettings.setOnClickListener(settingsListener);

        View.OnClickListener tocListener = v -> drawerLayout.openDrawer(GravityCompat.END);
        btnTocHeader.setOnClickListener(tocListener);
        fabToc.setOnClickListener(tocListener);
        
        vscroll8.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; 
        });
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        rootReadView = findViewById(R.id.rootReadView);
        toolbarLayout = findViewById(R.id.toolbarLayout);
        floatingButtonsLayout = findViewById(R.id.floatingButtonsLayout);
        vscroll8 = findViewById(R.id.vscroll8);

        btnBack = findViewById(R.id.btnBack);
        btnReaderSettings = findViewById(R.id.btnReaderSettings);
        btnTocHeader = findViewById(R.id.btnTocHeader);
        imgReadCover = findViewById(R.id.imgReadCover);

        fabSettings = findViewById(R.id.fabSettings);
        fabToc = findViewById(R.id.fabToc);

        tvBookTitle = findViewById(R.id.tvBookTitle);
        chapterContainer = findViewById(R.id.chapterContainer);

        tvReadTitle = findViewById(R.id.tvReadTitle);
        tvReadAuthor = findViewById(R.id.tvReadAuthor);
        lvToc = findViewById(R.id.lvToc);

        tvBookTitle.setText(projectName);
        tvReadTitle.setText(projectName);
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                toggleFullScreen();
                return true;
            }
        });
    }

    private void toggleFullScreen() {
        isFullScreen = !isFullScreen;
        if (isFullScreen) {
            toolbarLayout.setVisibility(View.GONE);
            floatingButtonsLayout.setVisibility(View.VISIBLE);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            toolbarLayout.setVisibility(View.VISIBLE);
            floatingButtonsLayout.setVisibility(View.GONE);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void applyReaderTheme() {
        int bgColor = bgColors[currentThemeIndex];
        int textColor = textColors[currentThemeIndex];

        rootReadView.setBackgroundColor(bgColor);
        toolbarLayout.setBackgroundColor(bgColor);

        tvBookTitle.setTextColor(textColor);
        btnBack.setColorFilter(textColor);
        btnReaderSettings.setColorFilter(textColor);
        btnTocHeader.setColorFilter(textColor);

        tvReadTitle.setTextColor(textColor);
        tvReadAuthor.setTextColor(textColor);

        if (!isFullScreen) {
            getWindow().setStatusBarColor(bgColor);
        }

        tvBookTitle.setTypeface(tempTypeface, Typeface.BOLD);
        tvReadTitle.setTypeface(tempTypeface, Typeface.BOLD);
        tvReadAuthor.setTypeface(tempTypeface);

        if (chapterContainer != null) {
            for (int i = 0; i < chapterContainer.getChildCount(); i++) {
                View child = chapterContainer.getChildAt(i);
                if (child instanceof TextView) {
                    TextView tv = (TextView) child;
                    tv.setTextColor(textColor);
                    tv.setTypeface(tempTypeface);
                    
                    if ("heading".equals(tv.getTag())) {
                        tv.setTypeface(tempTypeface, Typeface.BOLD);
                        tv.setTextSize(tempTextSize + 4f); 
                    } else {
                        tv.setTextSize(tempTextSize);
                    }
                }
            }
        }
        
        // TOC এর ফন্ট আপডেট
        if(lvToc != null && lvToc.getAdapter() != null) {
            ((ArrayAdapter)lvToc.getAdapter()).notifyDataSetChanged();
        }
    }

        // ==========================================
    // 🌟 ফুল স্টোরি লোড এবং কাস্টম সর্টিং (মাস্টারপিস) 🌟
    // ==========================================
    private void loadFullStory() {
        if (imgReadCover != null) {
            File coverFile = new File(projectDir, "cover.jpg");
            if (coverFile.exists()) {
                android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                options.inSampleSize = 2; 
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(coverFile.getAbsolutePath(), options);
                imgReadCover.setImageBitmap(bitmap);

                int marginInPx = (int) (100 * getResources().getDisplayMetrics().density);
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int expectedWidth = screenWidth - marginInPx;
                int expectedHeight = (int) (expectedWidth * 1.5f); // ২:৩ রেশিও

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) imgReadCover.getLayoutParams();
                params.height = expectedHeight;
                imgReadCover.setLayoutParams(params);
                imgReadCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imgReadCover.setVisibility(View.VISIBLE);
            } else {
                imgReadCover.setVisibility(View.GONE);
            }
        }

        if (!projectDir.exists()) return;

        File[] files = projectDir.listFiles();
        if (files == null || files.length == 0) return;

        final List<File> chapterFiles = new ArrayList<>();
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".tpad")) chapterFiles.add(f);
        }

        // ==========================================
        // 🌟 ম্যাজিক: কাস্টম ড্র্যাগ-অ্যান্ড-ড্রপ সিরিয়াল লোড করা 🌟
        // ==========================================
        SharedPreferences projectDataPrefs = getSharedPreferences("ProjectData_" + projectName, MODE_PRIVATE);
        String savedOrder = projectDataPrefs.getString("chapter_custom_order", "");
        
        if (!savedOrder.isEmpty()) {
            final java.util.List<String> orderList = java.util.Arrays.asList(savedOrder.split(";;"));
            java.util.Collections.sort(chapterFiles, new java.util.Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    int index1 = orderList.indexOf(f1.getName());
                    int index2 = orderList.indexOf(f2.getName());
                    
                    // যদি দুটোর কোনোটিই লিস্টে না থাকে (যেমন: নতুন পর্ব)
                    if (index1 == -1 && index2 == -1) return f1.getName().compareTo(f2.getName());
                    // নতুন ফাইল হলে সবার শেষে দেখাবে
                    if (index1 == -1) return 1;
                    if (index2 == -1) return -1;
                    
                    return Integer.compare(index1, index2);
                }
            });
        } else {
            // যদি কোনো কাস্টম সিরিয়াল সেভ করা না থাকে, তবে আগের মতো সাজাবে
            Collections.sort(chapterFiles);
        }
        // ==========================================

        chapterContainer.removeAllViews();
        chapterTitleViews.clear();
        chapterList.clear();

        new Thread(() -> {
            final List<String[]> loadedChapters = new ArrayList<>(); 
            
            for (File f : chapterFiles) {
                String chapterName = f.getName().replace(".tpad", "");
                String content = getNoteContentFromDB(chapterName);
                if (!content.trim().isEmpty()) {
                    loadedChapters.add(new String[]{chapterName, content});
                }
            }

            runOnUiThread(() -> {
                for (String[] chapterData : loadedChapters) {
                    String chapterName = chapterData[0];
                    String content = chapterData[1];

                    chapterList.add(chapterName);

                    TextView tvHeading = new TextView(ProjectReadActivity.this);
                    tvHeading.setText("— " + chapterName + " —");
                    tvHeading.setTextSize(tempTextSize + 4f);
                    tvHeading.setTypeface(tempTypeface, Typeface.BOLD);
                    tvHeading.setGravity(Gravity.CENTER);
                    tvHeading.setPadding(0, 100, 0, 40);
                    tvHeading.setTextColor(textColors[currentThemeIndex]);
                    tvHeading.setTag("heading"); 
                    chapterContainer.addView(tvHeading);

                    chapterTitleViews.add(tvHeading);

                    TextView tvContent = new TextView(ProjectReadActivity.this);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        tvContent.setText(Html.fromHtml(content.replace("\n", "<br>"), Html.FROM_HTML_MODE_COMPACT));
                    } else {
                        tvContent.setText(Html.fromHtml(content.replace("\n", "<br>")));
                    }
                    tvContent.setTextSize(tempTextSize);
                    tvContent.setTypeface(tempTypeface);
                    tvContent.setLineSpacing(0, 1.8f);
                    tvContent.setTextColor(textColors[currentThemeIndex]);
                    tvContent.setTag("content"); 
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        tvContent.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
                    }
                    chapterContainer.addView(tvContent);
                }
                
                loadTableOfContents(); 
            });
        }).start();
    }

    private void loadTableOfContents() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, chapterList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                
                text.setTypeface(tempTypeface, Typeface.BOLD);
                text.setTextColor(textColors[currentThemeIndex]); 
                return view;
            }
        };
        lvToc.setAdapter(adapter);

        lvToc.setOnItemClickListener((parent, view, position, id) -> {
            drawerLayout.closeDrawer(GravityCompat.END);

            View targetView = chapterTitleViews.get(position);
            
            targetView.post(() -> {
                int[] scrollViewLocation = new int[2];
                vscroll8.getLocationOnScreen(scrollViewLocation);
                
                int[] viewLocation = new int[2];
                targetView.getLocationOnScreen(viewLocation);
                
                int offset = viewLocation[1] - scrollViewLocation[1];
                vscroll8.smoothScrollBy(0, offset - 50); 
            });
        });
    }

    private String getNoteContentFromDB(String title) {
        String content = "";
        android.database.sqlite.SQLiteDatabase db = null;
        android.database.Cursor cursor = null;
        try {
            String uniqueTitle = projectName + "_" + title;
            db = openOrCreateDatabase("notes_db_v3", MODE_PRIVATE, null);
            cursor = db.rawQuery("SELECT content FROM notes WHERE title=?", new String[]{uniqueTitle});

            if (cursor.moveToFirst()) { content = cursor.getString(0); }
            else {
                cursor.close();
                cursor = db.rawQuery("SELECT content FROM notes WHERE title=?", new String[]{title});
                if(cursor.moveToFirst()) { content = cursor.getString(0); }
            }
        } catch (Exception e) {
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close(); 
            if (db != null && db.isOpen()) db.close();
        }
        return content;
    }

    private void showReaderSettingsBottomSheet() {
        final BottomSheetDialog sheet = new BottomSheetDialog(this);
        
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_editor_settings, null);
        sheet.setContentView(view);

        TextView tvFontSizeLabel = view.findViewById(R.id.tvFontSizeLabel);
        SeekBar seekBarFontSize = view.findViewById(R.id.seekBarFontSize);
        LinearLayout fontListContainer = view.findViewById(R.id.fontListContainer);
        LinearLayout themeColorsContainer = view.findViewById(R.id.themeColorsContainer);
        TextView btnToggleFullScreen = view.findViewById(R.id.btnToggleFullScreen);

        // ১. ফন্ট সাইজ
        tvFontSizeLabel.setText("ফন্ট সাইজ: " + (int)tempTextSize + "sp");
        seekBarFontSize.setProgress((int)tempTextSize - 12);
        seekBarFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tempTextSize = 12 + progress;
                tvFontSizeLabel.setText("ফন্ট সাইজ: " + (int)tempTextSize + "sp");
                applyReaderTheme();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // ==========================================
        // 🌟 ২. ফিক্সড: ফন্ট লিস্ট সেটআপ 🌟
        // ==========================================
        String[] fonts = {"সিস্টেম ডিফল্ট", "সোলায়মান লিপি", "কালপুরুষ", "সিয়াম রূপালী", "হিন্দ শিলিগুড়ি"};
        fontListContainer.removeAllViews();
        
        for (int i = 0; i < fonts.length; i++) {
            final int index = i;
            
            RelativeLayout fontRow = new RelativeLayout(this);
            fontRow.setPadding(0, 24, 0, 24);
            
            TextView tvFont = new TextView(this);
            tvFont.setText(fonts[i]);
            tvFont.setTextSize(18f);
            
            // 🌟 লিস্টের আইটেমগুলোতেও নির্দিষ্ট ফন্ট দেখানো 🌟
            Typeface listFace = Typeface.DEFAULT;
            try {
                if (index == 1) listFace = Typeface.createFromAsset(getAssets(), "fonts/solaimanlipi.ttf");
                else if (index == 2) listFace = Typeface.createFromAsset(getAssets(), "fonts/kalpurush.ttf");
                else if (index == 3) listFace = Typeface.createFromAsset(getAssets(), "fonts/siyamrupali.ttf");
                else if (index == 4) listFace = Typeface.createFromAsset(getAssets(), "fonts/hindsiliguri.ttf");
            } catch (Exception e){}
            
            tvFont.setTypeface(listFace);
            
            TextView tvCheck = new TextView(this);
            tvCheck.setText("✓");
            tvCheck.setTextSize(20f);
            tvCheck.setTextColor(Color.parseColor("#42A5F5"));
            tvCheck.setTypeface(null, Typeface.BOLD);
            RelativeLayout.LayoutParams checkParams = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            checkParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            tvCheck.setLayoutParams(checkParams);

            if (index == currentFontIndex) {
                tvFont.setTextColor(Color.parseColor("#3E2723")); 
                tvFont.setTypeface(listFace, Typeface.BOLD);
                fontRow.addView(tvCheck); 
            } else {
                tvFont.setTextColor(Color.parseColor("#757575")); 
            }
            fontRow.addView(tvFont);

            fontRow.setOnClickListener(v -> {
                currentFontIndex = index;
                
                // 🌟 ফিক্সড: গ্লোবাল সেটিংসে ফন্ট সেভ করা হচ্ছে 🌟
                SharedPreferences appSettings = getSharedPreferences("AppSettings", MODE_PRIVATE);
                appSettings.edit().putInt("font_style", index).apply();
                
                tempTypeface = ThemeHelper.getCustomTypeface(ProjectReadActivity.this);

                applyReaderTheme();
                
                // 🌟 লেআউট রিফ্রেশ করা যাতে সূচিপত্র বা অন্য জায়গার ফন্ট আপডেট হয় 🌟
                chapterContainer.requestLayout(); 

                sheet.dismiss();
                // showReaderSettingsBottomSheet(); // আপনি চাইলে এটা সরাতে পারেন যদি সাথে সাথে শিট বন্ধ করতে চান
            });
            fontListContainer.addView(fontRow);
        }

        // ৩. থিম কালার
        themeColorsContainer.removeAllViews();
        for (int i = 0; i < bgColors.length; i++) {
            final int index = i;
            ImageView colorCircle = new ImageView(this);
            int size = 90;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(16, 0, 16, 0);
            colorCircle.setLayoutParams(params);

            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(bgColors[i]);
            
            if (i == currentThemeIndex) {
                circle.setStroke(8, Color.parseColor("#000000")); 
            } else {
                circle.setStroke(2, Color.parseColor("#BDBDBD"));
            }
            colorCircle.setBackground(circle);

            colorCircle.setOnClickListener(v -> {
                currentThemeIndex = index;
                applyReaderTheme();
                sheet.dismiss();
                // showReaderSettingsBottomSheet();
            });
            themeColorsContainer.addView(colorCircle);
        }

        // ৪. ফুল স্ক্রিন বাটন
        btnToggleFullScreen.setText(isFullScreen ? "ফুল স্ক্রিন বন্ধ করুন" : "ফুল স্ক্রিন চালু করুন");
        btnToggleFullScreen.setOnClickListener(v -> {
            toggleFullScreen();
            sheet.dismiss();
        });

        sheet.show();
    }
}
