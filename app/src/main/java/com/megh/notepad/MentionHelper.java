package com.megh.notepad;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MentionHelper {

    // 🌟 স্মার্ট টাইপিং মনিটর 🌟
    public static void attachMentionSystem(final Activity activity, final EditText editText, final int surfaceColor, final int bgColor, final int primaryTextColor, final int secondaryTextColor, final int accentColor, final Typeface typeface) {

        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 0 && editText.hasFocus()) {
                    int cursorPosition = editText.getSelectionStart();
                    if (cursorPosition == 0) return;

                    String textUpToCursor = s.toString().substring(0, cursorPosition);
                    char lastChar = textUpToCursor.charAt(cursorPosition - 1);

                    // ১. সরাসরি স্পেশাল ক্যারেক্টার টাইপ করলে শিট ওপেন হবে
                    if (lastChar == '@') showSearchSheet(activity, editText, "character", "@", cursorPosition, surfaceColor, bgColor, primaryTextColor, secondaryTextColor, accentColor, typeface);
                    else if (lastChar == '$') showSearchSheet(activity, editText, "project", "$", cursorPosition, surfaceColor, bgColor, primaryTextColor, secondaryTextColor, accentColor, typeface);
                    else if (lastChar == '/') showSearchSheet(activity, editText, "note", "/", cursorPosition, surfaceColor, bgColor, primaryTextColor, secondaryTextColor, accentColor, typeface);
                    else if (lastChar == '#') showSearchSheet(activity, editText, "label", "#", cursorPosition, surfaceColor, bgColor, primaryTextColor, secondaryTextColor, accentColor, typeface);

                    // ২. শব্দ টাইপ করলে স্মার্ট সাজেশন পপআপ আসবে
                    if (textUpToCursor.endsWith("চরিত্র") || textUpToCursor.endsWith("ক্যারেক্টার")) {
                        showSuggestionPopup(activity, editText, "চরিত্র", "@", cursorPosition);
                    } else if (textUpToCursor.endsWith("প্রজেক্ট")) {
                        showSuggestionPopup(activity, editText, "প্রজেক্ট", "$", cursorPosition);
                    } else if (textUpToCursor.endsWith("নোট") || textUpToCursor.endsWith("নোটস")) {
                        showSuggestionPopup(activity, editText, "নোট", "/", cursorPosition);
                    }
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // 🌟 স্মার্ট সাজেশন পপআপ (Floating Tip) 🌟
    private static void showSuggestionPopup(final Activity activity, final EditText editText, final String word, final String symbol, final int cursorPosition) {
        TextView tvSuggestion = new TextView(activity);
        tvSuggestion.setText("মেনশন করতে '" + symbol + "' ব্যবহার করুন");
        tvSuggestion.setTextColor(Color.WHITE);
        tvSuggestion.setBackgroundColor(Color.parseColor("#424242"));
        tvSuggestion.setPadding(32, 16, 32, 16);
        tvSuggestion.setTextSize(14f);

        final PopupWindow popup = new PopupWindow(tvSuggestion, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        popup.showAsDropDown(editText, 0, -100);

        tvSuggestion.setOnClickListener(v -> {
            Editable editable = editText.getText();
            int startPos = cursorPosition - word.length();
            editable.replace(startPos, cursorPosition, symbol);
            editText.setSelection(startPos + 1);
            popup.dismiss();
        });
    }

    // 🌟 বাগ-ফ্রি স্ক্রল সহ বটম শিট (HTML মুক্ত পিওর টেক্সট) 🌟
    private static void showSearchSheet(final Activity activity, final EditText editText, final String type, final String symbol, final int cursorPosition, int surfaceColor, int bgColor, int primaryTextColor, int secondaryTextColor, int accentColor, Typeface typeface) {
        
        final BottomSheetDialog sheet = new BottomSheetDialog(activity);
        sheet.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(surfaceColor);
        root.setPadding(48, 56, 48, 64);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText((type.equals("character") ? "ক্যারেক্টার" : type.equals("project") ? "প্রজেক্ট" : type.equals("note") ? "নোট" : "লেবেল") + " খুঁজুন (" + symbol + ")");
        tvTitle.setTextColor(accentColor);
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(typeface, Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 32);
        root.addView(tvTitle);

        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(bgColor);
        searchBg.setCornerRadius(24f);

        EditText etSearch = new EditText(activity);
        etSearch.setHint("নাম লিখুন...");
        etSearch.setTextColor(primaryTextColor);
        etSearch.setHintTextColor(secondaryTextColor);
        etSearch.setBackground(searchBg);
        etSearch.setPadding(40, 32, 40, 32);
        etSearch.setTypeface(typeface);
        etSearch.setSingleLine(true);
        root.addView(etSearch);

        NestedScrollView scrollView = new NestedScrollView(activity);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600);
        scrollParams.setMargins(0, 32, 0, 0);
        scrollView.setLayoutParams(scrollParams);
        
        LinearLayout listContainer = new LinearLayout(activity);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listContainer);
        root.addView(scrollView);

        // ডাটা লোড
        List<String> items = new ArrayList<>();
        if (type.equals("character")) {
            SharedPreferences prefs = activity.getSharedPreferences("Global_Characters_DB", Context.MODE_PRIVATE);
            int count = prefs.getInt("char_count", 0);
            for(int i=0; i<count; i++) if(prefs.getBoolean("char_active_"+i, false)) items.add(prefs.getString("char_name_"+i, ""));
        } else if (type.equals("project")) {
            File projRoot = new File(activity.getFilesDir(), "TunePad_Data/Projects");
            if(projRoot.exists()) for(File cat : projRoot.listFiles()) if(cat.isDirectory()) for(File p : cat.listFiles()) if(p.isDirectory()) items.add(p.getName());
        } else if (type.equals("note")) {
            android.database.sqlite.SQLiteDatabase db = null;
            android.database.Cursor cursor = null;
            try {
                db = activity.openOrCreateDatabase("notes_db_v3", Context.MODE_PRIVATE, null);
                cursor = db.rawQuery("SELECT title FROM notes WHERE isDeleted=0", null);
                if(cursor.moveToFirst()) do {
                    String t = cursor.getString(0);
                    if(t.contains("_")) t = t.substring(t.indexOf("_")+1);
                    if(!items.contains(t)) items.add(t);
                } while(cursor.moveToNext());
            } catch (Exception e){} finally { if(cursor!=null) cursor.close(); if(db!=null) db.close(); }
        }

        Runnable populateList = () -> {
            String query = etSearch.getText().toString().toLowerCase();
            listContainer.removeAllViews();
            if (type.equals("label") && !query.isEmpty()) { items.clear(); items.add(query); }

            for (String item : items) {
                if (query.isEmpty() || item.toLowerCase().contains(query)) {
                    TextView tvItem = new TextView(activity);
                    tvItem.setText(type.equals("label") ? "#" + item : item);
                    tvItem.setTextColor(primaryTextColor);
                    tvItem.setTextSize(16f);
                    tvItem.setTypeface(typeface, Typeface.BOLD);
                    tvItem.setPadding(40, 32, 40, 32);
                    
                    GradientDrawable itemBg = new GradientDrawable();
                    itemBg.setColor(bgColor);
                    itemBg.setCornerRadius(16f);
                    tvItem.setBackground(itemBg);
                    
                    LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lParams.setMargins(0, 0, 0, 16);
                    tvItem.setLayoutParams(lParams);
                    
                    tvItem.setOnClickListener(v -> {
                        // 🌟 ম্যাজিক: পিওর টেক্সট বসানো এবং কালার করা (কোনো লিংক বা HTML ছাড়া) 🌟
                        Editable editable = editText.getText();
                        String formattedName = type.equals("label") ? "#" + item : symbol + item; // যেমন: @মেঘ
                        String insertText = formattedName + " ";
                        
                        // আগে টাইপ করা স্পেশাল ক্যারেক্টারটি মুছে নাম বসানো হচ্ছে
                        int startReplace = cursorPosition - 1;
                        editable.replace(startReplace, cursorPosition, insertText);
                        
                        // টাইপ করার সময় সুন্দর দেখানোর জন্য কালার ও বোল্ড করে দেওয়া
                        int endSpan = startReplace + formattedName.length();
                        editable.setSpan(new ForegroundColorSpan(accentColor), startReplace, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        editable.setSpan(new StyleSpan(Typeface.BOLD), startReplace, endSpan, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        
                        editText.setSelection(startReplace + insertText.length());
                        
                        sheet.dismiss();
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
        sheet.setContentView(root);
        
        sheet.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            BottomSheetBehavior.from(bottomSheetInternal).setState(BottomSheetBehavior.STATE_EXPANDED);
            
            etSearch.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });
        sheet.show();
    }
}
