package com.megh.notepad;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface; // 🌟 ফন্টের জন্য এই ইমপোর্টটি যুক্ত করা হলো

public class ThemeHelper {

    private static final String PREF_NAME = "AppSettings";
    private static final String KEY_THEME = "app_theme";

    // থিমগুলোর নাম (ইনডেক্স হিসেবে)
    public static final int THEME_DARK_TEAL = 0;   // Default
    public static final int THEME_LIGHT_TEAL = 1;
    public static final int THEME_DARK_BLUE = 2;
    public static final int THEME_SEPIA = 3;

    // বর্তমান থিম সেভ এবং লোড করার মেথড
    public static void setTheme(Context context, int themeIndex) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME, themeIndex).apply();
    }

    public static int getSavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, THEME_DARK_TEAL); // ডার্ক টিল হলো ডিফল্ট
    }

    // =========================================
    // 🎨 কালার রিটার্ন করার মেথডসমূহ
    // =========================================

    public static int getBgColor(Context context) {
        switch (getSavedTheme(context)) {
            case THEME_LIGHT_TEAL: return Color.parseColor("#F4F6F6");
            case THEME_DARK_BLUE:  return Color.parseColor("#151B25");
            case THEME_SEPIA:      return Color.parseColor("#F4E8D3");
            case THEME_DARK_TEAL: 
            default:               return Color.parseColor("#192125");
        }
    }

    public static int getSurfaceColor(Context context) {
        switch (getSavedTheme(context)) {
            case THEME_LIGHT_TEAL: return Color.parseColor("#FFFFFF");
            case THEME_DARK_BLUE:  return Color.parseColor("#1E2732");
            case THEME_SEPIA:      return Color.parseColor("#EFE0C7");
            case THEME_DARK_TEAL: 
            default:               return Color.parseColor("#212B2F");
        }
    }

    public static int getAccentColor(Context context) {
        switch (getSavedTheme(context)) {
            case THEME_LIGHT_TEAL: return Color.parseColor("#108F6E");
            case THEME_DARK_BLUE:  return Color.parseColor("#46A883");
            case THEME_SEPIA:      return Color.parseColor("#614F41");
            case THEME_DARK_TEAL: 
            default:               return Color.parseColor("#108F6E");
        }
    }

    public static int getPrimaryTextColor(Context context) {
        switch (getSavedTheme(context)) {
            case THEME_LIGHT_TEAL: return Color.parseColor("#2D3E45");
            case THEME_SEPIA:      return Color.parseColor("#201A16");
            case THEME_DARK_BLUE:  
            case THEME_DARK_TEAL: 
            default:               return Color.parseColor("#FFFFFF");
        }
    }

    public static int getSecondaryTextColor(Context context) {
        switch (getSavedTheme(context)) {
            case THEME_LIGHT_TEAL: return Color.parseColor("#6F7D82");
            case THEME_DARK_BLUE:  return Color.parseColor("#A0AAB8");
            case THEME_SEPIA:      return Color.parseColor("#786C61");
            case THEME_DARK_TEAL: 
            default:               return Color.parseColor("#9CA8AE");
        }
    }

    // =========================================
    // 🔤 কাস্টম ফন্ট রিটার্ন করার ম্যাজিক মেথড
    // =========================================
    public static Typeface getCustomTypeface(Context context) {
        SharedPreferences appSettings = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int fontIndex = appSettings.getInt("font_style", 1);
        
        try {
            if (fontIndex == 1) {
                return Typeface.createFromAsset(context.getAssets(), "fonts/solaimanlipi.ttf");
            } else if (fontIndex == 2) {
                return Typeface.createFromAsset(context.getAssets(), "fonts/kalpurush.ttf");
            } else if (fontIndex == 3) {
                return Typeface.createFromAsset(context.getAssets(), "fonts/siyamrupali.ttf");
            } else if (fontIndex == 4) {
                return Typeface.createFromAsset(context.getAssets(), "fonts/hindsiliguri.ttf");
            } else {
                return Typeface.DEFAULT; // সিস্টেম ডিফল্ট
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Typeface.DEFAULT; // কোনো ফন্ট মিসিং থাকলে অ্যাপ ক্র্যাশ করবে না
        }
    }
    
        // ==========================================
    // 🌟 এক ক্লিকে পুরো পেজের সব ফন্ট চেঞ্জ করার ম্যাজিক মেথড 🌟
    // ==========================================
    public static void applyFontToAllViews(android.content.Context context, android.view.View root) {
        Typeface typeface = getCustomTypeface(context);

        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup viewGroup = (android.view.ViewGroup) root;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                applyFontToAllViews(context, viewGroup.getChildAt(i)); // 🔄 নিজে নিজেকে কল করে সব ভিউ খুঁজবে (Recursion)
            }
        } else if (root instanceof android.widget.TextView) {
            android.widget.TextView tv = (android.widget.TextView) root;
            Typeface currentFace = tv.getTypeface();
            int style = Typeface.NORMAL;
            if (currentFace != null) {
                style = currentFace.getStyle(); // 🌟 আগে থেকে Bold বা Italic করা থাকলে সেটা ধরে রাখবে
            }
            tv.setTypeface(typeface, style);
        }
    }

}
