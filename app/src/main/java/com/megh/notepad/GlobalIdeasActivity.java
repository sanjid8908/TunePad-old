package com.megh.notepad;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GlobalIdeasActivity extends AppCompatActivity {

    private SharedPreferences ideasDb;
    private RecyclerView rvIdeas;
    private IdeaAdapter adapter;
    private List<IdeaModel> allIdeasList = new ArrayList<>();
    private List<IdeaModel> filteredList = new ArrayList<>();

    private View rootView;
    private ImageView btnBack, btnAddIdea;
    private TextView tvToolbarTitle, tvEmptyMsg;
    private EditText etSearch;

    private int bgColor, surfaceColor, accentColor, primaryTextColor, secondaryTextColor;
    private Typeface currentTypeface;

    // ডিফল্ট কালার প্যালেট ("DEFAULT" মানে থিমের কালার)
    private final String[] noteColors = {"DEFAULT", "#212B2F", "#3E2723", "#1B5E20", "#0D47A1", "#880E4F"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.global_ideas);

        ideasDb = getSharedPreferences("Global_Ideas_DB", MODE_PRIVATE);

        initViews();
        applyTheme();

        rvIdeas.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        adapter = new IdeaAdapter();
        rvIdeas.setAdapter(adapter);

        loadIdeas();

        btnBack.setOnClickListener(v -> finish());
        btnAddIdea.setOnClickListener(v -> showIdeaDialog(null, true));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterIdeas(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void initViews() {
        rootView = findViewById(R.id.rootView);
        btnBack = findViewById(R.id.btnBack);
        btnAddIdea = findViewById(R.id.btnAddIdea);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        etSearch = findViewById(R.id.etSearch);
        rvIdeas = findViewById(R.id.rvIdeas);

        tvEmptyMsg = new TextView(this);
        tvEmptyMsg.setText("এখনো কোনো আইডিয়া লেখা হয়নি!\nনতুন আইডিয়া যুক্ত করতে '+' বাটনে চাপুন।");
        tvEmptyMsg.setGravity(Gravity.CENTER);
        tvEmptyMsg.setVisibility(View.GONE);
        ((ViewGroup) rvIdeas.getParent()).addView(tvEmptyMsg, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void applyTheme() {
        currentTypeface = ThemeHelper.getCustomTypeface(this);
        bgColor = ThemeHelper.getBgColor(this);
        surfaceColor = ThemeHelper.getSurfaceColor(this);
        accentColor = ThemeHelper.getAccentColor(this);
        primaryTextColor = ThemeHelper.getPrimaryTextColor(this);
        secondaryTextColor = ThemeHelper.getSecondaryTextColor(this);

        getWindow().setStatusBarColor(bgColor);
        if(rootView != null) rootView.setBackgroundColor(bgColor);

        tvToolbarTitle.setTextColor(primaryTextColor);
        tvToolbarTitle.setTypeface(currentTypeface, Typeface.BOLD);
        tvEmptyMsg.setTextColor(secondaryTextColor);
        tvEmptyMsg.setTypeface(currentTypeface);

        btnBack.setColorFilter(primaryTextColor);
        
        GradientDrawable addBg = new GradientDrawable();
        addBg.setColor(accentColor);
        addBg.setShape(GradientDrawable.OVAL);
        btnAddIdea.setBackground(addBg);
        btnAddIdea.setColorFilter(Color.WHITE);

        etSearch.setTextColor(primaryTextColor);
        etSearch.setHintTextColor(secondaryTextColor);
        etSearch.setTypeface(currentTypeface);
        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(surfaceColor);
        searchBg.setCornerRadius(100f);
        etSearch.setBackground(searchBg);
    }

    private void loadIdeas() {
        allIdeasList.clear();
        int count = ideasDb.getInt("idea_count", 0);
        for (int i = 0; i < count; i++) {
            if (ideasDb.getBoolean("idea_active_" + i, false)) {
                IdeaModel idea = new IdeaModel();
                idea.id = i;
                idea.title = ideasDb.getString("idea_title_" + i, "");
                idea.desc = ideasDb.getString("idea_desc_" + i, "");
                idea.date = ideasDb.getString("idea_date_" + i, "");
                idea.colorCode = ideasDb.getString("idea_color_" + i, "DEFAULT");
                allIdeasList.add(idea);
            }
        }
        
        java.util.Collections.reverse(allIdeasList);
        filterIdeas(etSearch.getText().toString());
    }

    private void filterIdeas(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(allIdeasList);
        } else {
            String lowerQ = query.toLowerCase();
            for (IdeaModel idea : allIdeasList) {
                if (idea.title.toLowerCase().contains(lowerQ) || idea.desc.toLowerCase().contains(lowerQ)) {
                    filteredList.add(idea);
                }
            }
        }
        tvEmptyMsg.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    // 🌟 কালার ডার্ক নাকি লাইট সেটা চেক করার ম্যাজিক মেথড 🌟
    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5; 
    }
    
    // ==========================================
    // 🌟 HTML ছাড়াই লাইভ মেনশন হাইলাইট ও ক্লিকেবল করার ম্যাজিক মেথড 🌟
    // ==========================================
    private void applyLiveMentions(TextView textView, String text, boolean isClickable, final Dialog parentDialog) {
        if (text == null) text = "";
        android.text.SpannableString spannable = new android.text.SpannableString(text);
        
        // 🌟 মেনশন ধরার স্মার্ট লজিক (স্পেস ছাড়া যেকোনো শব্দ) 🌟
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[@$#/][^\\s]+");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            final String matchedWord = matcher.group(); 

            // ১. গাঢ় এবং কালার করা
            spannable.setSpan(new android.text.style.ForegroundColorSpan(accentColor), start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // ২. ক্লিকেবল করা এবং অন্য পেজে পাঠানো
            if (isClickable) {
                spannable.setSpan(new android.text.style.ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        String typeChar = matchedWord.substring(0, 1); // @, $, /, #
                        String name = matchedWord.substring(1); // আসল নামটুকু

                        try {
                            if (typeChar.equals("@")) {
                                // 🌟 ১. ক্যারেক্টার পেজে যাওয়া 🌟
                                SharedPreferences charPrefs = getSharedPreferences("Global_Characters_DB", MODE_PRIVATE);
                                int charCount = charPrefs.getInt("char_count", 0);
                                int foundId = -1;
                                String foundProject = "Unknown";
                                
                                for (int i = 0; i < charCount; i++) {
                                    if (charPrefs.getBoolean("char_active_" + i, false) && name.equals(charPrefs.getString("char_name_" + i, ""))) {
                                        foundId = i;
                                        foundProject = charPrefs.getString("char_story_" + i, "Unknown");
                                        break;
                                    }
                                }
                                
                                if (foundId != -1) {
                                    Intent intent = new Intent(GlobalIdeasActivity.this, CharacterDetailsActivity.class);
                                    intent.putExtra("PROJECT_NAME", foundProject);
                                    intent.putExtra("CHAR_ID", foundId);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(GlobalIdeasActivity.this, "চরিত্রটি ডাটাবেসে পাওয়া যায়নি!", Toast.LENGTH_SHORT).show();
                                }

                            } else if (typeChar.equals("$")) {
                                // 🌟 ২. প্রজেক্ট ভিউ পেজে যাওয়া (ProjectViewActivity) 🌟
                                java.io.File projRoot = new java.io.File(getFilesDir(), "TunePad_Data/Projects");
                                String foundCat = "General";
                                boolean found = false;
                                
                                if (projRoot.exists() && projRoot.isDirectory()) {
                                    for (java.io.File cat : projRoot.listFiles()) {
                                        if (cat.isDirectory()) {
                                            java.io.File projFile = new java.io.File(cat, name);
                                            if (projFile.exists() && projFile.isDirectory()) {
                                                foundCat = cat.getName();
                                                found = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                                
                                if (found) {
                                    Intent intent = new Intent(GlobalIdeasActivity.this, ProjectViewActivity.class); 
                                    intent.putExtra("PROJECT_NAME", name);
                                    intent.putExtra("CATEGORY_NAME", foundCat);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(GlobalIdeasActivity.this, "প্রজেক্টটি স্টোরেজে পাওয়া যায়নি!", Toast.LENGTH_SHORT).show();
                                }

                            } else if (typeChar.equals("/")) {
                                // 🌟 ৩. নোট পেজে যাওয়া (MmmActivity এর ট্যাবে) 🌟
                                android.database.sqlite.SQLiteDatabase db = null;
                                android.database.Cursor cursor = null;
                                String fullTitle = "";
                                String projectName = "Unknown";
                                
                                try {
                                    db = openOrCreateDatabase("notes_db_v3", MODE_PRIVATE, null);
                                    cursor = db.rawQuery("SELECT title FROM notes WHERE isDeleted=0 AND title LIKE ?", new String[]{"%" + name + "%"});
                                    if(cursor.moveToFirst()) {
                                        fullTitle = cursor.getString(0);
                                        // ডাটাবেসে সাধারণত ProjectName_NoteTitle আকারে সেভ থাকে
                                        if (fullTitle.contains("_")) {
                                            projectName = fullTitle.substring(0, fullTitle.indexOf("_"));
                                        }
                                    }
                                } catch (Exception e) {} finally {
                                    if(cursor!=null) cursor.close();
                                    if(db!=null) db.close();
                                }
                                
                                if (!fullTitle.isEmpty()) {
                                    // 🌟 MmmActivity ওপেন হবে এবং নোটের ডাটা নিয়ে যাবে 🌟
                                    Intent intent = new Intent(GlobalIdeasActivity.this, MmmActivity.class); 
                                    intent.putExtra("PROJECT_NAME", projectName);
                                    intent.putExtra("NOTE_TITLE", name);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(GlobalIdeasActivity.this, "নোটটি ডাটাবেসে পাওয়া যায়নি!", Toast.LENGTH_SHORT).show();
                                }

                            } else if (typeChar.equals("#")) {
                                // 🌟 ৪. লেবেল দিয়ে সরাসরি ফিল্টার করা 🌟
                                if (parentDialog != null) parentDialog.dismiss(); 
                                
                                // সার্চ বারে '#' সহ লেবেলের নাম সেট করা হচ্ছে, ফলে অটোমেটিক শুধু ওই লেবেলের আইডিয়াগুলো ফিল্টার হবে
                                etSearch.setText("#" + name); 
                                etSearch.setSelection(etSearch.getText().length());
                                Toast.makeText(GlobalIdeasActivity.this, "লেবেল ফিল্টার করা হয়েছে: #" + name, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    @Override
                    public void updateDrawState(@NonNull android.text.TextPaint ds) {
                        ds.setUnderlineText(false); // লিংকের নিচ থেকে বাজে আন্ডারলাইন সরানো হলো
                    }
                }, start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        
        textView.setText(spannable);
        if (isClickable) {
            textView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        } else {
            textView.setMovementMethod(null);
        }
    }

    // ==========================================
    // 🌟 ফুল স্ক্রিন ভিউ / এডিট ডায়ালগ (HTML মুক্ত) 🌟
    // ==========================================
    private void showIdeaDialog(final IdeaModel existingIdea, boolean initialEditMode) {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        final boolean[] isEditMode = {initialEditMode};
        final String[] selectedColorCode = {existingIdea == null ? "DEFAULT" : existingIdea.colorCode};
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bgColor);

        // টুলবার
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(16, 32, 16, 32);
        toolbar.setBackgroundColor(surfaceColor);

        ImageView btnClose = new ImageView(this);
        btnClose.setImageResource(android.R.drawable.ic_menu_revert);
        btnClose.setColorFilter(primaryTextColor);
        btnClose.setPadding(16, 16, 16, 16);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        TextView tvDialogTitle = new TextView(this);
        tvDialogTitle.setTextColor(primaryTextColor);
        tvDialogTitle.setTextSize(20f);
        tvDialogTitle.setTypeface(currentTypeface, Typeface.BOLD);
        tvDialogTitle.setPadding(32, 0, 0, 0);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tvDialogTitle.setLayoutParams(titleParams);

        TextView btnAction = new TextView(this);
        btnAction.setTextColor(accentColor);
        btnAction.setTextSize(16f);
        btnAction.setTypeface(currentTypeface, Typeface.BOLD);
        btnAction.setPadding(32, 16, 32, 16);

        toolbar.addView(btnClose);
        toolbar.addView(tvDialogTitle);
        toolbar.addView(btnAction);
        root.addView(toolbar);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        scrollView.setFillViewport(true);

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(48, 48, 48, 48);

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(surfaceColor);
        inputBg.setCornerRadius(24f);

        final EditText etTitle = new EditText(this);
        etTitle.setHint("শিরোনাম (যেমন: নতুন প্লট টুইস্ট)");
        etTitle.setTextColor(primaryTextColor);
        etTitle.setHintTextColor(secondaryTextColor);
        etTitle.setBackground(inputBg);
        etTitle.setPadding(40, 40, 40, 40);
        etTitle.setTypeface(currentTypeface, Typeface.BOLD);
        etTitle.setTextSize(18f);
        if(existingIdea != null) etTitle.setText(existingIdea.title);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 32);
        etTitle.setLayoutParams(params);
        contentLayout.addView(etTitle);

        // 🌟 এডিট বক্স (HTML মুক্ত) 🌟
        final EditText etDesc = new EditText(this);
        etDesc.setHint("আপনার আইডিয়াটি লিখুন...\n(@ দিয়ে ক্যারেক্টার, $ দিয়ে প্রজেক্ট, / দিয়ে নোট মেনশন করুন)");
        etDesc.setTextColor(primaryTextColor);
        etDesc.setHintTextColor(secondaryTextColor);
        etDesc.setBackground(inputBg);
        etDesc.setPadding(40, 40, 40, 40);
        etDesc.setTypeface(currentTypeface);
        etDesc.setTextSize(16f);
        etDesc.setGravity(Gravity.TOP | Gravity.START);
        etDesc.setMinLines(10);
        contentLayout.addView(etDesc);

        // 🌟 ম্যাজিক: MentionHelper যুক্ত করা 🌟
        MentionHelper.attachMentionSystem(this, etDesc, surfaceColor, bgColor, primaryTextColor, secondaryTextColor, accentColor, currentTypeface);

        final LinearLayout colorContainer = new LinearLayout(this);
        colorContainer.setOrientation(LinearLayout.HORIZONTAL);
        colorContainer.setGravity(Gravity.CENTER);
        colorContainer.setPadding(0, 48, 0, 48);
        
        HorizontalScrollView hScrollColors = new HorizontalScrollView(this);
        hScrollColors.addView(colorContainer);
        hScrollColors.setHorizontalScrollBarEnabled(false);
        contentLayout.addView(hScrollColors);

        Runnable refreshModeUI = new Runnable() {
            @Override
            public void run() {
                etTitle.setFocusable(isEditMode[0]);
                etTitle.setFocusableInTouchMode(isEditMode[0]);
                etTitle.setCursorVisible(isEditMode[0]);
                
                etDesc.setFocusable(isEditMode[0]);
                etDesc.setFocusableInTouchMode(isEditMode[0]);
                etDesc.setCursorVisible(isEditMode[0]);

                hScrollColors.setVisibility(isEditMode[0] ? View.VISIBLE : View.GONE);
                
                // 🌟 টেক্সট লোড এবং হাইলাইট করা (HTML ছাড়া) 🌟
                if (existingIdea != null) {
                   // এডিট মোড হলে শুধু কালার হবে ক্লিক হবে না, ভিউ মোড হলে ক্লিক হবে
applyLiveMentions(etDesc, existingIdea.desc, !isEditMode[0], dialog);
                }
                
                if (isEditMode[0]) {
                    tvDialogTitle.setText(existingIdea == null ? "নতুন আইডিয়া" : "এডিট আইডিয়া");
                    btnAction.setText("সেভ করুন");
                    
                    colorContainer.removeAllViews();
                    for (int i = 0; i < noteColors.length; i++) {
                        final String thisColor = noteColors[i];
                        FrameLayout circleRoot = new FrameLayout(GlobalIdeasActivity.this);
                        int size = 120;
                        LinearLayout.LayoutParams cParams = new LinearLayout.LayoutParams(size, size);
                        cParams.setMargins(16, 0, 16, 0);
                        circleRoot.setLayoutParams(cParams);
                        
                        ImageView circle = new ImageView(GlobalIdeasActivity.this);
                        circle.setLayoutParams(new FrameLayout.LayoutParams(size, size));
                        
                        GradientDrawable shape = new GradientDrawable();
                        shape.setShape(GradientDrawable.OVAL);
                        if (thisColor.equals("DEFAULT")) shape.setColor(surfaceColor); 
                        else shape.setColor(Color.parseColor(thisColor));
                        
                        if (thisColor.equals(selectedColorCode[0])) shape.setStroke(8, accentColor);
                        else shape.setStroke(2, secondaryTextColor);
                        circle.setBackground(shape);
                        circleRoot.addView(circle);
                        
                        if (thisColor.equals("DEFAULT")) {
                            TextView tvNone = new TextView(GlobalIdeasActivity.this);
                            tvNone.setText("নান");
                            tvNone.setTextColor(primaryTextColor);
                            tvNone.setTextSize(12f);
                            tvNone.setTypeface(currentTypeface, Typeface.BOLD);
                            tvNone.setGravity(Gravity.CENTER);
                            circleRoot.addView(tvNone);
                        }
                        
                        circleRoot.setOnClickListener(v -> {
                            selectedColorCode[0] = thisColor;
                            this.run(); 
                        });
                        colorContainer.addView(circleRoot);
                    }
                    
                    // কাস্টম কালার পিকার বাটন
                    FrameLayout customCircleRoot = new FrameLayout(GlobalIdeasActivity.this);
                    int size = 120;
                    LinearLayout.LayoutParams cParams = new LinearLayout.LayoutParams(size, size);
                    cParams.setMargins(16, 0, 32, 0);
                    customCircleRoot.setLayoutParams(cParams);
                    ImageView customCircle = new ImageView(GlobalIdeasActivity.this);
                    customCircle.setLayoutParams(new FrameLayout.LayoutParams(size, size));
                    GradientDrawable customShape = new GradientDrawable();
                    customShape.setShape(GradientDrawable.OVAL);
                    boolean isCustomSelected = true;
                    for (String nc : noteColors) { if (nc.equals(selectedColorCode[0])) { isCustomSelected = false; break; } }
                    if (isCustomSelected) {
                        customShape.setColor(Color.parseColor(selectedColorCode[0]));
                        customShape.setStroke(8, accentColor);
                    } else {
                        customShape.setColor(bgColor);
                        customShape.setStroke(4, secondaryTextColor);
                    }
                    customCircle.setBackground(customShape);
                    customCircleRoot.addView(customCircle);
                    TextView tvAddCustom = new TextView(GlobalIdeasActivity.this);
                    tvAddCustom.setText(isCustomSelected ? "✓" : "+");
                    tvAddCustom.setTextColor(isCustomSelected ? (isColorDark(Color.parseColor(selectedColorCode[0])) ? Color.WHITE : Color.BLACK) : primaryTextColor);
                    tvAddCustom.setTextSize(isCustomSelected ? 20f : 24f);
                    tvAddCustom.setTypeface(currentTypeface, Typeface.BOLD);
                    tvAddCustom.setGravity(Gravity.CENTER);
                    customCircleRoot.addView(tvAddCustom);
                    customCircleRoot.setOnClickListener(v -> {
                        showCustomColorPicker(selectedColorCode[0], newColorHex -> {
                            selectedColorCode[0] = newColorHex;
                            this.run();
                        });
                    });
                    colorContainer.addView(customCircleRoot);

                } else {
                    tvDialogTitle.setText("আইডিয়া ভিউ");
                    btnAction.setText("এডিট করুন");
                }
            }
        };

        btnAction.setOnClickListener(v -> {
            if (isEditMode[0]) {
                String title = etTitle.getText().toString().trim();
                
                // 🌟 ম্যাজিক: এবার একদম পিওর (Pure) টেক্সট সেভ হবে, কোনো HTML নয়! 🌟
                String plainDesc = etDesc.getText().toString().trim(); 
                
                if(title.isEmpty() && plainDesc.isEmpty()) {
                    Toast.makeText(GlobalIdeasActivity.this, "কিছু একটা তো লিখুন!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(title.isEmpty()) title = "শিরোনামহীন আইডিয়া";

                String date = new SimpleDateFormat("dd MMM, yyyy - hh:mm a", Locale.getDefault()).format(new Date());
                SharedPreferences.Editor editor = ideasDb.edit();
                int idToSave = (existingIdea == null) ? ideasDb.getInt("idea_count", 0) : existingIdea.id;

                editor.putBoolean("idea_active_" + idToSave, true)
                      .putString("idea_title_" + idToSave, title)
                      .putString("idea_desc_" + idToSave, plainDesc) // HTML মুক্ত পিওর টেক্সট
                      .putString("idea_date_" + idToSave, date)
                      .putString("idea_color_" + idToSave, selectedColorCode[0]);

                if(existingIdea == null) editor.putInt("idea_count", idToSave + 1);
                editor.apply();

                Toast.makeText(GlobalIdeasActivity.this, "আইডিয়া সেভ হয়েছে!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadIdeas();
            } else {
                isEditMode[0] = true;
                refreshModeUI.run();
                etTitle.requestFocus();
            }
        });
        
        refreshModeUI.run();
        
        scrollView.addView(contentLayout);
        root.addView(scrollView);
        
        dialog.setContentView(root);
        dialog.show();
    }


    // ==========================================
    // 🌟 স্মার্ট মেনশন সার্চ শিট (ডাটাবেস কানেকশন সহ) 🌟
    // ==========================================
    private void showMentionSearchSheet(String type, int cursorPosition, EditText etDesc) {
        final BottomSheetDialog mentionSheet = new BottomSheetDialog(this);
        mentionSheet.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(surfaceColor);
        root.setPadding(48, 56, 48, 64);

        TextView tvTitle = new TextView(this);
        String titleText = type.equals("character") ? "ক্যারেক্টার খুঁজুন (@)" : 
                           type.equals("project") ? "প্রজেক্ট খুঁজুন ($)" : 
                           type.equals("note") ? "নোট খুঁজুন (/)" : "নতুন লেবেল তৈরি করুন (#)";
        tvTitle.setText(titleText);
        tvTitle.setTextColor(accentColor);
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(currentTypeface, Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 32);
        root.addView(tvTitle);

        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(bgColor);
        searchBg.setCornerRadius(24f);

        EditText etSearch = new EditText(this);
        etSearch.setHint(type.equals("label") ? "লেবেলের নাম লিখুন..." : "নাম লিখে খুঁজুন...");
        etSearch.setTextColor(primaryTextColor);
        etSearch.setHintTextColor(secondaryTextColor);
        etSearch.setBackground(searchBg);
        etSearch.setPadding(40, 32, 40, 32);
        etSearch.setTypeface(currentTypeface);
        etSearch.setSingleLine(true);
        root.addView(etSearch);
        
        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600);
        scrollParams.setMargins(0, 32, 0, 0);
        scrollView.setLayoutParams(scrollParams);
        
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listContainer);
        root.addView(scrollView);

        // 🌟 আসল ডাটাবেস থেকে ডাটা লোড করার লজিক 🌟
        List<String> items = new ArrayList<>();
        
        if (type.equals("character")) {
            SharedPreferences charPrefs = getSharedPreferences("Global_Characters_DB", MODE_PRIVATE);
            int charCount = charPrefs.getInt("char_count", 0);
            for(int i=0; i<charCount; i++) {
                if(charPrefs.getBoolean("char_active_"+i, false)) items.add(charPrefs.getString("char_name_"+i, "Unknown"));
            }
        } 
        else if (type.equals("project")) {
            File projRoot = new File(getFilesDir(), "TunePad_Data/Projects");
            if(projRoot.exists() && projRoot.isDirectory()) {
                for(File category : projRoot.listFiles()) {
                    if(category.isDirectory()) {
                        for(File proj : category.listFiles()) {
                            if(proj.isDirectory()) items.add(proj.getName());
                        }
                    }
                }
            }
        } 
        else if (type.equals("note")) {
            android.database.sqlite.SQLiteDatabase db = null;
            android.database.Cursor cursor = null;
            try {
                db = openOrCreateDatabase("notes_db_v3", MODE_PRIVATE, null);
                cursor = db.rawQuery("SELECT title FROM notes WHERE isDeleted=0", null);
                if(cursor.moveToFirst()) {
                    do {
                        String nTitle = cursor.getString(0);
                        if(nTitle.contains("_")) nTitle = nTitle.substring(nTitle.indexOf("_")+1);
                        if(!items.contains(nTitle)) items.add(nTitle);
                    } while(cursor.moveToNext());
                }
            } catch (Exception e) {} finally {
                if(cursor!=null) cursor.close();
                if(db!=null) db.close();
            }
        }

        Runnable populateList = () -> {
            String query = etSearch.getText().toString().toLowerCase();
            listContainer.removeAllViews();
            
            if (type.equals("label") && !query.isEmpty()) {
                items.clear();
                items.add(query); 
            }

            for (String item : items) {
                if (query.isEmpty() || item.toLowerCase().contains(query)) {
                    TextView tvItem = new TextView(this);
                    tvItem.setText(type.equals("label") ? "#" + item + " (যুক্ত করুন)" : item);
                    tvItem.setTextColor(primaryTextColor);
                    tvItem.setTextSize(16f);
                    tvItem.setTypeface(currentTypeface, Typeface.BOLD);
                    tvItem.setPadding(40, 32, 40, 32);
                    
                    GradientDrawable itemBg = new GradientDrawable();
                    itemBg.setColor(bgColor);
                    itemBg.setCornerRadius(16f);
                    tvItem.setBackground(itemBg);
                    
                    LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lParams.setMargins(0, 0, 0, 16);
                    tvItem.setLayoutParams(lParams);
                    
                    tvItem.setOnClickListener(v -> {
                        Editable editable = etDesc.getText();
                        String finalName = type.equals("label") ? "#" + item : type.equals("character") ? "@" + item : type.equals("project") ? "$" + item : "/" + item; 
                        String insertText = finalName + " ";
                        
                        editable.replace(cursorPosition, cursorPosition + 1, insertText);
                        
                        // 🌟 নামটাকে গাঢ় এবং কালারফুল (Spannable) করা 🌟
                        editable.setSpan(new ForegroundColorSpan(accentColor), cursorPosition, cursorPosition + finalName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        editable.setSpan(new StyleSpan(Typeface.BOLD), cursorPosition, cursorPosition + finalName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        
                        etDesc.setSelection(cursorPosition + insertText.length());
                        mentionSheet.dismiss();
                    });
                    listContainer.addView(tvItem);
                }
            }
        };

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { populateList.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        populateList.run();

        mentionSheet.setContentView(root);
        mentionSheet.setOnShowListener(dialog -> {
            etSearch.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });
        mentionSheet.show();
    }

   

    // ==========================================
    // 🎨 ভিজ্যুয়াল কাস্টম RGB কালার পিকার 🎨
    // ==========================================
    interface ColorPickerListener { void onColorSelected(String hexColor); }

    private void showCustomColorPicker(String initialColorCode, ColorPickerListener listener) {
        final BottomSheetDialog pickerSheet = new BottomSheetDialog(this);
        pickerSheet.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(surfaceColor);
        root.setPadding(64, 64, 64, 64);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("ভিজ্যুয়াল কালার পিকার");
        tvTitle.setTextColor(primaryTextColor);
        tvTitle.setTextSize(20f);
        tvTitle.setTypeface(currentTypeface, Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 48);
        root.addView(tvTitle);

        // 🌟 বিশাল কালার প্রিভিউ বক্স 🌟
        final View previewBox = new View(this);
        LinearLayout.LayoutParams pvParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 250);
        pvParams.setMargins(0, 0, 0, 64);
        previewBox.setLayoutParams(pvParams);
        
        int initColor = primaryTextColor;
        try { initColor = initialColorCode.equals("DEFAULT") ? Color.parseColor("#42A5F5") : Color.parseColor(initialColorCode); } 
        catch (Exception e) {}

        final GradientDrawable pvShape = new GradientDrawable();
        pvShape.setCornerRadius(32f);
        previewBox.setBackground(pvShape);
        root.addView(previewBox);

        String[] labels = {"লাল (Red)", "সবুজ (Green)", "নীল (Blue)"};
        final SeekBar[] seekBars = new SeekBar[3];
        final GradientDrawable[] gradients = new GradientDrawable[3];
        final TextView[] tvValues = new TextView[3];

        for (int i = 0; i < 3; i++) {
            // Label & Value Container
            LinearLayout headerLayout = new LinearLayout(this);
            headerLayout.setOrientation(LinearLayout.HORIZONTAL);
            headerLayout.setPadding(0, 16, 0, 16);
            
            TextView tvLabel = new TextView(this);
            tvLabel.setText(labels[i]);
            tvLabel.setTextColor(secondaryTextColor);
            tvLabel.setTypeface(currentTypeface, Typeface.BOLD);
            tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            headerLayout.addView(tvLabel);

            // কালারের ভ্যালু (০-২৫৫) দেখানোর জন্য
            tvValues[i] = new TextView(this);
            tvValues[i].setTextColor(primaryTextColor);
            tvValues[i].setTypeface(currentTypeface, Typeface.BOLD);
            headerLayout.addView(tvValues[i]);
            
            root.addView(headerLayout);

            // 🌟 ভিজ্যুয়াল গ্রেডিয়েন্ট বার (যাতে ইউজার বুঝতে পারে কোন কালার হবে) 🌟
            View bgView = new View(this);
            gradients[i] = new GradientDrawable();
            gradients[i].setCornerRadius(16f);
            bgView.setBackground(gradients[i]);
            LinearLayout.LayoutParams bgParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 24);
            bgParams.setMargins(40, 8, 40, 0); // স্লাইডারের থাম্বের সাথে মেলানোর জন্য
            root.addView(bgView, bgParams);

            // আসল সিকবার (SeekBar)
            seekBars[i] = new SeekBar(this);
            seekBars[i].setMax(255);
            seekBars[i].setPadding(0, 16, 0, 48);
            
            // সিকবারের ডিফল্ট লাইনটা একটু হালকা করে দেওয়া, যাতে গ্রেডিয়েন্ট চোখে পড়ে
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                seekBars[i].setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#808080")));
                seekBars[i].setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#30808080")));
            }
            root.addView(seekBars[i]);
        }

        // আগের সিলেক্ট করা কালার লোড করা
        seekBars[0].setProgress(Color.red(initColor));
        seekBars[1].setProgress(Color.green(initColor));
        seekBars[2].setProgress(Color.blue(initColor));

        Runnable updateColors = new Runnable() {
            @Override
            public void run() {
                int r = seekBars[0].getProgress();
                int g = seekBars[1].getProgress();
                int b = seekBars[2].getProgress();
                
                // ১. মেইন প্রিভিউ আপডেট
                pvShape.setColor(Color.rgb(r, g, b));
                tvValues[0].setText(String.valueOf(r));
                tvValues[1].setText(String.valueOf(g));
                tvValues[2].setText(String.valueOf(b));
                
                // ২. ম্যাজিক: স্লাইডারের গ্রেডিয়েন্ট ব্যাকগ্রাউন্ড লাইভ আপডেট করা
                gradients[0].setColors(new int[]{Color.rgb(0, g, b), Color.rgb(255, g, b)});
                gradients[0].setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
                
                gradients[1].setColors(new int[]{Color.rgb(r, 0, b), Color.rgb(r, 255, b)});
                gradients[1].setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
                
                gradients[2].setColors(new int[]{Color.rgb(r, g, 0), Color.rgb(r, g, 255)});
                gradients[2].setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
            }
        };

        for (int i = 0; i < 3; i++) {
            seekBars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { updateColors.run(); }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
        updateColors.run(); // প্রথমবার লেআউট লোড হওয়ার সময় কল করা

        TextView btnApply = new TextView(this);
        btnApply.setText("এই কালারটি সেট করুন");
        btnApply.setTextColor(Color.WHITE); // বাটনের টেক্সট সবসময় সাদা
        btnApply.setGravity(Gravity.CENTER);
        btnApply.setTextSize(16f);
        btnApply.setTypeface(currentTypeface, Typeface.BOLD);
        btnApply.setPadding(0, 40, 0, 40);
        
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(accentColor);
        btnBg.setCornerRadius(100f);
        btnApply.setBackground(btnBg);

        btnApply.setOnClickListener(v -> {
            String hexColor = String.format("#%02x%02x%02x", seekBars[0].getProgress(), seekBars[1].getProgress(), seekBars[2].getProgress());
            listener.onColorSelected(hexColor);
            pickerSheet.dismiss();
        });

        root.addView(btnApply);
        pickerSheet.setContentView(root);
        pickerSheet.show();
    }

    // ==========================================
    // 🌟 অ্যাডাপ্টার ও ভিউহোল্ডার 🌟
    // ==========================================
    class IdeaModel { int id; String title, desc, date, colorCode; }

    private class IdeaAdapter extends RecyclerView.Adapter<IdeaAdapter.ViewHolder> {
        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout card = new LinearLayout(GlobalIdeasActivity.this);
            card.setOrientation(LinearLayout.VERTICAL);
            
            StaggeredGridLayoutManager.LayoutParams lp = new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(16, 16, 16, 16);
            card.setLayoutParams(lp);
            card.setPadding(32, 40, 32, 40);

            TextView tvTitle = new TextView(GlobalIdeasActivity.this);
            tvTitle.setTextSize(16f);
            tvTitle.setTypeface(currentTypeface, Typeface.BOLD);
            tvTitle.setPadding(0, 0, 0, 16);
            card.addView(tvTitle);

            TextView tvDesc = new TextView(GlobalIdeasActivity.this);
            tvDesc.setTextSize(14f);
            tvDesc.setTypeface(currentTypeface);
            tvDesc.setMaxLines(7); 
            tvDesc.setEllipsize(android.text.TextUtils.TruncateAt.END);
            card.addView(tvDesc);

            TextView tvDate = new TextView(GlobalIdeasActivity.this);
            tvDate.setTextSize(10f);
            tvDate.setTypeface(currentTypeface, Typeface.ITALIC);
            tvDate.setPadding(0, 24, 0, 0);
            tvDate.setGravity(Gravity.END);
            card.addView(tvDate);

            return new ViewHolder(card, tvTitle, tvDesc, tvDate);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final IdeaModel idea = filteredList.get(position);
            holder.tvTitle.setText(idea.title);
            holder.tvDesc.setText(idea.desc);
            holder.tvDate.setText(idea.date);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(32f);

            int dynamicTextColor;
            int dynamicSecondaryColor;

            if (idea.colorCode.equals("DEFAULT")) {
                bg.setColor(surfaceColor);
                dynamicTextColor = primaryTextColor;
                dynamicSecondaryColor = secondaryTextColor;
            } else {
                int customColor = Color.parseColor(idea.colorCode);
                bg.setColor(customColor);
                
                if (isColorDark(customColor)) {
                    dynamicTextColor = Color.WHITE;
                    dynamicSecondaryColor = Color.parseColor("#E0E0E0");
                } else {
                    dynamicTextColor = Color.BLACK;
                    dynamicSecondaryColor = Color.parseColor("#424242");
                }
            }

            holder.tvTitle.setTextColor(dynamicTextColor);
            holder.tvDesc.setTextColor(dynamicSecondaryColor);
            holder.tvDate.setTextColor(dynamicSecondaryColor);
            holder.itemView.setBackground(bg);

            holder.itemView.setOnClickListener(v -> showIdeaDialog(idea, false));
            holder.itemView.setOnLongClickListener(v -> {
                showIdeaMenuSheet(idea);
                return true;
            });
        }
        @Override public int getItemCount() { return filteredList.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDesc, tvDate;
            public ViewHolder(@NonNull View itemView, TextView t, TextView d, TextView dt) {
                super(itemView); tvTitle = t; tvDesc = d; tvDate = dt;
            }
        }
    }

    // ==========================================
    // 🌟 আইডিয়ার অপশন মেনু (থ্রি-ডট) 🌟
    // ==========================================
    private void showIdeaMenuSheet(final IdeaModel idea) {
        final BottomSheetDialog sheet = new BottomSheetDialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(surfaceColor);
        root.setPadding(0, 32, 0, 32);

        TextView title = new TextView(this); 
        title.setText(idea.title); 
        title.setTextColor(accentColor); 
        title.setTextSize(18f); 
        title.setTypeface(currentTypeface, Typeface.BOLD); 
        title.setPadding(64, 32, 64, 48); 
        root.addView(title);

        root.addView(createMenuItem("শেয়ার করুন", android.R.drawable.ic_menu_share, v -> {
            sheet.dismiss();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "💡 " + idea.title + "\n\n" + idea.desc);
            startActivity(Intent.createChooser(intent, "আইডিয়া শেয়ার করুন"));
        }));

        root.addView(createMenuItem("প্রজেক্টে রূপান্তর করুন", android.R.drawable.ic_menu_edit, v -> {
            sheet.dismiss();
            Toast.makeText(this, "শিগগিরই আসছে! এই আইডিয়াটি সরাসরি নতুন গল্প হিসেবে তৈরি হবে।", Toast.LENGTH_LONG).show();
        }));

        root.addView(createMenuItem("মুছে ফেলুন", android.R.drawable.ic_menu_delete, v -> {
            sheet.dismiss();
            ideasDb.edit().putBoolean("idea_active_" + idea.id, false).apply();
            Toast.makeText(this, "আইডিয়াটি মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show();
            loadIdeas();
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
}
