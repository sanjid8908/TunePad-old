package com.megh.notepad;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.InputStream;

public class CharacterDetailsActivity extends AppCompatActivity {

    private String projectName;
    private int charId;
    private SharedPreferences charPrefs;

    private LinearLayout rootCharDetailsView, toolbarLayout, charDetailsContainer;
    private ImageView btnBack, btnEditCharacter, imgCharProfile;
    private TextView tvToolbarTitle, tvCharNameLarge, tvStoryName, tvCharRoleBadge, tvCharInitials;
    private View dividerLine;

    private int bgColor, surfaceColor, accentColor, primaryTextColor, secondaryTextColor;
    private Typeface currentTypeface = Typeface.DEFAULT;
    
    // এডিট করার সময় ছবি ও রিলেশনশিপ ট্র্যাকিং
    private String tempImgUri = "";
    private String tempRelationships = ""; 
    private static final int PICK_IMAGE_REQUEST = 1003;
    private ImageView previewEditImg;

    // 🌟 প্রিভিউ মোডের জন্য নতুন ভেরিয়েবল 🌟
    private boolean isPreviewMode = false;
    private org.json.JSONObject importJsonObj;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.character_details);

        charPrefs = getSharedPreferences("Global_Characters_DB", MODE_PRIVATE);
        initViews();
        applyThemeColors();

        // 🌟 ম্যাজিক: চেক করা হচ্ছে এটা প্রিভিউ মোড কি না 🌟
        String importData = getIntent().getStringExtra("IMPORT_JSON");
        
        if (importData != null && !importData.isEmpty()) {
            isPreviewMode = true;
            try {
                importJsonObj = new org.json.JSONObject(importData);
                projectName = importJsonObj.optString("char_story", "Imported Character");
                setupPreviewModeUI(); // প্রিভিউয়ের সেভ ও বাতিল বাটন তৈরি
            } catch (Exception e) {
                Toast.makeText(this, "ফাইলের ডেটা পড়তে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show();
                finish(); return;
            }
        } else {
            projectName = getIntent().getStringExtra("PROJECT_NAME");
            charId = getIntent().getIntExtra("CHAR_ID", -1);
            if (charId == -1 || projectName == null) {
                Toast.makeText(this, "চরিত্র পাওয়া যায়নি!", Toast.LENGTH_SHORT).show();
                finish(); return;
            }
        }

        loadCharacterProfile();

        btnBack.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { finish(); } });
        btnEditCharacter.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showEditBottomSheet(); }
        });
    }


@Override
protected void onResume() {
    super.onResume();
    
    // 🌟 পেজে ফিরে আসলেই নতুন ফন্ট চেক করে পুরো স্ক্রিনে বসিয়ে দেবে 🌟
    try {
        android.graphics.Typeface currentTypeface = ThemeHelper.getCustomTypeface(this);
        applyFontToAllViews(getWindow().getDecorView(), currentTypeface);
    } catch (Exception e) {
        e.printStackTrace();
    }
}


    private void initViews() {
        rootCharDetailsView = findViewById(R.id.rootCharDetailsView);
        toolbarLayout = findViewById(R.id.toolbarLayout);
        btnBack = findViewById(R.id.btnBack);
        btnEditCharacter = findViewById(R.id.btnEditCharacter);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        imgCharProfile = findViewById(R.id.imgCharProfile);
        tvCharInitials = findViewById(R.id.tvCharInitials);
        tvCharNameLarge = findViewById(R.id.tvCharNameLarge);
        tvStoryName = findViewById(R.id.tvStoryName);
        tvCharRoleBadge = findViewById(R.id.tvCharRoleBadge);
        dividerLine = findViewById(R.id.dividerLine);
        charDetailsContainer = findViewById(R.id.charDetailsContainer);
    }

    private void applyThemeColors() {
    // ১. থিম কালার লোড করা
    bgColor = ThemeHelper.getBgColor(this);
    surfaceColor = ThemeHelper.getSurfaceColor(this);
    accentColor = ThemeHelper.getAccentColor(this);
    primaryTextColor = ThemeHelper.getPrimaryTextColor(this);
    secondaryTextColor = ThemeHelper.getSecondaryTextColor(this);

    // 🌟 ২. ফিক্সড: একদম শুরুতেই ThemeHelper থেকে সঠিক ফন্ট মেমোরিতে টেনে নেওয়া হলো 🌟
    currentTypeface = ThemeHelper.getCustomTypeface(this);

    // ৩. ভিউগুলোতে কালার এবং ফন্ট বসানো
    getWindow().setStatusBarColor(bgColor);
    rootCharDetailsView.setBackgroundColor(bgColor);
    tvToolbarTitle.setTextColor(primaryTextColor);
    tvToolbarTitle.setTypeface(currentTypeface, Typeface.BOLD); // এখন এখানে সঠিক ফন্ট পাবে
    
    btnBack.setColorFilter(primaryTextColor);
    btnEditCharacter.setColorFilter(accentColor);
    
    tvCharNameLarge.setTextColor(primaryTextColor);
    tvCharNameLarge.setTypeface(currentTypeface, Typeface.BOLD);
    tvStoryName.setTextColor(secondaryTextColor);
    tvStoryName.setTypeface(currentTypeface);
    dividerLine.setBackgroundColor(surfaceColor);

    GradientDrawable badgeBg = new GradientDrawable();
    badgeBg.setColor(Color.argb(40, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)));
    badgeBg.setCornerRadius(50f);
    tvCharRoleBadge.setBackground(badgeBg);
    tvCharRoleBadge.setTextColor(accentColor);
    tvCharRoleBadge.setTypeface(currentTypeface, Typeface.BOLD);
    
    // 🌟 ৪. ম্যাজিক মেথড দিয়ে পুরো স্ক্রিনের বাকি সব জায়গায় ফন্ট বসিয়ে দেওয়া হলো 🌟
    applyFontToAllViews(getWindow().getDecorView(), currentTypeface);
}

    // ==========================================
    // 🌟 প্রিভিউ মোডের UI (সেভ এবং বাতিল বাটন) 🌟
    // ==========================================
    private void setupPreviewModeUI() {
        btnEditCharacter.setVisibility(View.GONE); // প্রিভিউতে এডিট করা যাবে না
        tvToolbarTitle.setText("প্রিভিউ (Preview)");

        LinearLayout actionLayout = new LinearLayout(this);
        actionLayout.setOrientation(LinearLayout.HORIZONTAL);
        actionLayout.setPadding(32, 48, 32, 48);
        actionLayout.setGravity(Gravity.CENTER);

        // ❌ বাতিল বাটন
        TextView btnCancel = new TextView(this);
        btnCancel.setText("❌ বাতিল");
        btnCancel.setTextColor(Color.parseColor("#E53935"));
        btnCancel.setTextSize(16f);
        btnCancel.setTypeface(currentTypeface, Typeface.BOLD);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setPadding(0, 32, 0, 32);
        
        GradientDrawable cancelBg = new GradientDrawable();
        cancelBg.setColor(surfaceColor);
        cancelBg.setStroke(3, Color.parseColor("#E53935"));
        cancelBg.setCornerRadius(100f);
        btnCancel.setBackground(cancelBg);
        
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        cancelParams.setMargins(0, 0, 16, 0);
        btnCancel.setLayoutParams(cancelParams);

        // ✅ সেভ বাটন
        TextView btnSave = new TextView(this);
        btnSave.setText("✅ সেভ করুন");
        btnSave.setTextColor(Color.WHITE);
        btnSave.setTextSize(16f);
        btnSave.setTypeface(currentTypeface, Typeface.BOLD);
        btnSave.setGravity(Gravity.CENTER);
        btnSave.setPadding(0, 32, 0, 32);
        
        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setColor(accentColor);
        saveBg.setCornerRadius(100f);
        btnSave.setBackground(saveBg);
        
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        saveParams.setMargins(16, 0, 0, 0);
        btnSave.setLayoutParams(saveParams);

        // ক্লিক লিসেনার
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); } // বাতিল করলে বের হয়ে যাবে
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { saveImportedCharacter(); } // পারমানেন্ট সেভ
        });

        actionLayout.addView(btnCancel);
        actionLayout.addView(btnSave);
        rootCharDetailsView.addView(actionLayout, 1); // টুলবারের ঠিক নিচে বাটন দেখাবে
    }

       // ==========================================
    // 🌟 প্রিভিউ থেকে সেভ করার লজিক (সেম নেম ওয়ার্নিং সহ) 🌟
    // ==========================================
    private void saveImportedCharacter() {
        final String importedName = importJsonObj.optString("char_name", "Unknown");
        int count = charPrefs.getInt("char_count", 0);
        int existingId = -1;

        // 🌟 চেক করা হচ্ছে এই নামের চরিত্র আগে থেকেই আছে কি না 🌟
        for (int i = 0; i < count; i++) {
            if (charPrefs.getBoolean("char_active_" + i, false)) {
                String existingName = charPrefs.getString("char_name_" + i, "");
                if (existingName.trim().equalsIgnoreCase(importedName.trim())) {
                    existingId = i;
                    break;
                }
            }
        }

        if (existingId != -1) {
            // ⚠️ সেম নেম ওয়ার্নিং ডায়ালগ ⚠️
            final int finalExistingId = existingId;
            final int newId = count;

            new AlertDialog.Builder(this)
                .setTitle("সতর্কতা! ⚠️")
                .setMessage("'" + importedName + "' নামের একটি চরিত্র ইতিমধ্যেই আপনার ডাটাবেসে আছে। আপনি কি পুরনো চরিত্রটি আপডেট (ওভাররাইট) করতে চান, নাকি এটি নতুন চরিত্র হিসেবে যুক্ত করতে চান?")
                .setPositiveButton("নতুন যুক্ত করুন", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        performSave(newId); // সম্পূর্ণ নতুন আইডিতে সেভ হবে
                    }
                })
                .setNegativeButton("আপডেট করুন", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        performSave(finalExistingId); // পুরনো আইডিতেই ওভাররাইট হবে
                    }
                })
                .setNeutralButton("বাতিল", null)
                .show();
        } else {
            // কোনো ডুপ্লিকেট নেই, তাই সরাসরি নতুন আইডিতে সেভ
            performSave(count);
        }
    }

    // ==========================================
    // 🌟 আসল সেভ ইঞ্জিন (ওভাররাইট এবং নতুন সেভ দুটোই সামলাবে) 🌟
    // ==========================================
    private void performSave(int targetId) {
        SharedPreferences.Editor editor = charPrefs.edit();
        
        try {
            java.util.Iterator<String> keys = importJsonObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.equals("char_rels")) continue; // অন্যের ফোনের রিলেশনশিপ আইডি বাদ

                Object value = importJsonObj.get(key);
                if (value instanceof Integer) {
                    editor.putInt(key + "_" + targetId, (Integer) value);
                } else if (value instanceof Boolean) {
                    editor.putBoolean(key + "_" + targetId, (Boolean) value);
                } else {
                    editor.putString(key + "_" + targetId, value.toString());
                }
            }
            editor.putBoolean("char_active_" + targetId, true);
            
            // যদি নতুন আইডি হয়, তবেই গ্লোবাল কাউন্ট বাড়াতে হবে
            if (targetId >= charPrefs.getInt("char_count", 0)) {
                editor.putInt("char_count", targetId + 1);
            }
            
            editor.apply();
            
            Toast.makeText(this, "চরিত্রটি সফলভাবে সেভ হয়েছে! 🎉", Toast.LENGTH_SHORT).show();
            finish(); 
            
        } catch (Exception e) {
            Toast.makeText(this, "সেভ করতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show();
        }
    }


    // ==========================================
    // 🌟 ডেটা রিডার হেল্পার (প্রিভিউ এবং নরমাল মোড হ্যান্ডেল করে) 🌟
    // ==========================================
    private String getCharString(String key, String defValue) {
        if (isPreviewMode && importJsonObj != null) return importJsonObj.optString(key, defValue);
        return charPrefs.getString(key + "_" + charId, defValue);
    }

    private int getCharInt(String key, int defValue) {
        if (isPreviewMode && importJsonObj != null) return importJsonObj.optInt(key, defValue);
        return charPrefs.getInt(key + "_" + charId, defValue);
    }

    // ==========================================
    // 🌟 প্রোফাইল লোড 🌟
    // ==========================================
    private void loadCharacterProfile() {
        // 🌟 নরমাল Preference এর বদলে আমাদের স্মার্ট হেল্পার মেথড ব্যবহার হচ্ছে 🌟
        String name = getCharString("char_name", "Unknown");
        String nickname = getCharString("char_nickname", "");
        String role = getCharString("char_role", "ভূমিকা সেট করা হয়নি");
        String imgUri = getCharString("char_img", "");
        String storyName = getCharString("char_story", projectName);

        if (!nickname.isEmpty()) tvCharNameLarge.setText(name + " (" + nickname + ")");
        else tvCharNameLarge.setText(name);
        
        tvStoryName.setText("গল্প: " + storyName);
        tvCharRoleBadge.setText(role);

        if (!imgUri.isEmpty()) {
            safeLoadImage(imgUri, imgCharProfile); 
            imgCharProfile.setVisibility(View.VISIBLE);
            tvCharInitials.setVisibility(View.GONE);
        } else {
            imgCharProfile.setVisibility(View.GONE);
            tvCharInitials.setVisibility(View.VISIBLE);
            
            String initials = "?";
            String cleanName = name.trim();
            if(cleanName.length() > 0) {
                initials = String.valueOf(cleanName.charAt(0));
                if(cleanName.contains(" ")) {
                    String[] parts = cleanName.split("\\s+");
                    if(parts.length > 1 && parts[1].length() > 0) initials = String.valueOf(cleanName.charAt(0)) + String.valueOf(parts[1].charAt(0));
                }
            }
            tvCharInitials.setText(initials.toUpperCase());
            ((View)tvCharInitials.getParent()).setBackgroundColor(accentColor);
        }

        charDetailsContainer.removeAllViews();
        
        addCategoryCard("সাধারণ তথ্য", 
            new String[]{"জন্মতারিখ", "বয়স", "দেশ", "জন্মস্থান / ঠিকানা", "পেশা বা অবস্থান"}, 
            new String[]{
                getCharString("char_dob", ""), getCharString("char_age", ""),
                getCharString("char_country", ""), getCharString("char_location", ""),
                getCharString("char_occupation", "")
            });

        addCategoryCard("বাহ্যিক রূপ", 
            new String[]{"উচ্চতা", "শারীরিক গড়ন ও ওজন", "চোখ ও চুলের রঙ", "বিশেষ চিহ্ন / ট্যাটু", "পোশাকের ধরন"}, 
            new String[]{
                getCharString("char_height", ""), getCharString("char_build", ""),
                getCharString("char_eye_hair", ""), getCharString("char_marks", ""),
                getCharString("char_clothing", "")
            });

        addCategoryCard("মনস্তত্ত্ব ও ব্যক্তিত্ব", 
            new String[]{"স্বভাব ও ব্যক্তিত্ব", "ভালো গুণাবলী (Strengths)", "খারাপ দিক (Flaws)", "মুদ্রাদোষ ও অভ্যাস"}, 
            new String[]{
                getCharString("char_personality", ""), getCharString("char_strengths", ""),
                getCharString("char_flaws", ""), getCharString("char_habits", "")
            });

        addCategoryCard("গল্পের পটভূমি", 
            new String[]{"জীবনের লক্ষ্য", "সবচেয়ে বড় ভয়", "গোপন রহস্য", "অন্তর্দ্বন্দ্ব (Conflict)", "অতীত ও ব্যাকস্টোরি"}, 
            new String[]{
                getCharString("char_goal", ""), getCharString("char_fear", ""),
                getCharString("char_secrets", ""), getCharString("char_conflict", ""),
                getCharString("char_backstory", "")
            });

        int customCount = getCharInt("char_custom_count", 0);
        if (customCount > 0) {
            java.util.List<String> keys = new java.util.ArrayList<>();
            java.util.List<String> vals = new java.util.ArrayList<>();
            for (int i = 0; i < customCount; i++) {
                String k = getCharString("char_custom_key_" + i, "");
                String v = getCharString("char_custom_val_" + i, "");
                if (!k.isEmpty() && !v.isEmpty()) { keys.add(k); vals.add(v); }
            }
            if (!keys.isEmpty()) {
                addCategoryCard("অন্যান্য তথ্য (কাস্টম)", keys.toArray(new String[0]), vals.toArray(new String[0]));
            }
        }
        
        // প্রিভিউ মোডে রিলেশনশিপ লোড করার দরকার নেই, কারণ ওই আইডি গুলো এই ফোনে নেই
        if (!isPreviewMode) {
            loadRelationships();
        }
    }

    private void addCategoryCard(String title, String[] keys, String[] values) {
        boolean hasData = false;
        for (String v : values) { if (v != null && !v.trim().isEmpty()) { hasData = true; break; } }
        if (!hasData) return; 

        androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 48);
        card.setLayoutParams(cardParams);
        card.setRadius(32f);
        card.setCardElevation(4f);
        card.setCardBackgroundColor(surfaceColor);

        LinearLayout cardInner = new LinearLayout(this);
        cardInner.setOrientation(LinearLayout.VERTICAL);
        cardInner.setPadding(48, 48, 48, 48);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(accentColor);
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(currentTypeface, Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 24);
        cardInner.addView(tvTitle);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
        divider.setBackgroundColor(bgColor);
        cardInner.addView(divider);

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(0, 24, 0, 0);

        for (int i = 0; i < keys.length; i++) {
            if (values[i] != null && !values[i].trim().isEmpty()) {
                TextView tvK = new TextView(this);
                tvK.setText(keys[i]);
                tvK.setTextColor(secondaryTextColor);
                tvK.setTextSize(12f);
                tvK.setTypeface(currentTypeface, Typeface.BOLD);
                tvK.setPadding(0, 16, 0, 4);

                TextView tvV = new TextView(this);
                tvV.setText(values[i]);
                tvV.setTextColor(primaryTextColor);
                tvV.setTextSize(15f);
                tvV.setTypeface(currentTypeface);
                tvV.setPadding(0, 0, 0, 16);

                contentLayout.addView(tvK);
                contentLayout.addView(tvV);
            }
        }
        cardInner.addView(contentLayout);
        card.addView(cardInner);
        charDetailsContainer.addView(card);
    }

    // (বাকি সব কোড যেমন এডিট বটম শিট, ছবি লোডিং লজিক আগের মতোই থাকবে, কোনো পরিবর্তন নেই)
    // 🌟 ক্র্যাশ ছাড়া এবং রিলেটিভ পাথ থেকে ছবি লোড করার সেফ মেথড 🌟
    private void safeLoadImage(String uriString, ImageView imageView) {
        if (uriString == null || uriString.isEmpty()) return;
        try {
            android.graphics.Bitmap bitmap = null;
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inSampleSize = 4; // মেমরি বাঁচাতে ছবি ছোট করা
            
            if (uriString.startsWith("LOCAL_CHAR_IMG:")) {
                String fileName = uriString.substring(15);
                String currentStory = charPrefs.getString("char_story_" + charId, projectName);
                File pDir = getProjectDir(currentStory);
                if (pDir != null) {
                    File imgFile = new File(new File(pDir, "Characters"), fileName);
                    if (imgFile.exists()) {
                        bitmap = android.graphics.BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                    }
                }
            } else if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                InputStream is = getContentResolver().openInputStream(android.net.Uri.parse(uriString));
                bitmap = android.graphics.BitmapFactory.decodeStream(is, null, options);
                if (is != null) is.close();
            } else {
                File f = new File(uriString);
                if (f.exists()) bitmap = android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath(), options);
            }
            
            if (bitmap != null) imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeRelation(int targetIdToRemove) {
        String relations = charPrefs.getString("char_rels_" + charId, "");
        if (relations.isEmpty()) return;

        StringBuilder newRelations = new StringBuilder();
        String[] relList = relations.split(";;");
        
        for (String rel : relList) {
            if (rel.contains("|")) {
                String[] parts = rel.split("\\|");
                int targetId = Integer.parseInt(parts[0]);
                
                if (targetId != targetIdToRemove) {
                    if (newRelations.length() > 0) newRelations.append(";;");
                    newRelations.append(rel);
                }
            }
        }
        
        charPrefs.edit().putString("char_rels_" + charId, newRelations.toString()).apply();
        tempRelationships = newRelations.toString(); 
        loadCharacterProfile(); 
        Toast.makeText(this, "সম্পর্ক মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show();
    }

    private void loadRelationships() {
        String relations = charPrefs.getString("char_rels_" + charId, "");
        if (relations.isEmpty()) return;

        TextView tvTitle = new TextView(this);
        tvTitle.setText("সম্পর্কিত চরিত্রসমূহ 🔗");
        tvTitle.setTextColor(accentColor);
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(currentTypeface, Typeface.BOLD);
        tvTitle.setPadding(0, 32, 0, 16);
        charDetailsContainer.addView(tvTitle);

        String[] relList = relations.split(";;");
        for (String rel : relList) {
            if (rel.contains("|")) {
                String[] parts = rel.split("\\|");
                final int targetId = Integer.parseInt(parts[0]);
                String relationType = parts[1];
                
                final String targetName = charPrefs.getString("char_name_" + targetId, "Unknown");
                if (charPrefs.getBoolean("char_active_" + targetId, false)) {
                    
                    TextView tvRelChip = new TextView(this);
                    tvRelChip.setText("👤 " + targetName + " ➔ " + relationType);
                    tvRelChip.setTextColor(surfaceColor);
                    tvRelChip.setTextSize(13f);
                    tvRelChip.setTypeface(currentTypeface, Typeface.BOLD);
                    tvRelChip.setPadding(32, 20, 32, 20);
                    
                    GradientDrawable chipBg = new GradientDrawable();
                    chipBg.setColor(primaryTextColor);
                    chipBg.setCornerRadius(50f);
                    tvRelChip.setBackground(chipBg);
                    
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 0, 0, 16);
                    tvRelChip.setLayoutParams(lp);

                    tvRelChip.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(CharacterDetailsActivity.this, CharacterDetailsActivity.class);
                            intent.putExtra("PROJECT_NAME", projectName);
                            intent.putExtra("CHAR_ID", targetId);
                            startActivity(intent);
                        }
                    });

                    tvRelChip.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            new AlertDialog.Builder(CharacterDetailsActivity.this)
                            .setTitle("সম্পর্ক মুছে ফেলুন")
                            .setMessage("আপনি কি '" + targetName + "' এর সাথে সম্পর্কটি মুছে ফেলতে চান?")
                            .setPositiveButton("মুছে ফেলুন", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    removeRelation(targetId);
                                }
                            })
                            .setNegativeButton("বাতিল", null)
                            .show();
                            return true;
                        }
                    });

                    charDetailsContainer.addView(tvRelChip);
                }
            }
        }
    }

    private void showEditBottomSheet() {
        // [আপনার আগের এডিট লজিক হুবহু এখানে থাকবে, আমি আর রিপিট করলাম না জায়গা বাঁচাতে, আপনি আপনার আগের কোডটাই রেখে দেবেন]
    }

    private File getProjectDir(String projName) {
        File rootProjDir = new File(getFilesDir(), "TunePad_Data/Projects");
        File[] categories = rootProjDir.listFiles();
        if (categories != null) {
            for (File cat : categories) {
                if (cat.isDirectory()) {
                    File pDir = new File(cat, projName);
                    if (pDir.exists() && pDir.isDirectory()) {
                        return pDir;
                    }
                }
            }
        }
        return null;
    }
    
    // 🌟 ইউনিভার্সাল ফন্ট চেঞ্জার 🌟
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


}
