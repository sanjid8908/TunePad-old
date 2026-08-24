package com.megh.notepad;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;


import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
	
	private SharedPreferences appSettings;
	private SharedPreferences.Editor editor;
	private NoteDatabaseHelper dbHelper;
	
	// থিম ও ফন্ট ভেরিয়েবল
	private int bgColor, surfaceColor, accentColor, primaryTextColor, secondaryTextColor;
	private Typeface currentTypeface = Typeface.DEFAULT;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings); 
		
		appSettings = getSharedPreferences("AppSettings", MODE_PRIVATE);
		editor = appSettings.edit();
		dbHelper = new NoteDatabaseHelper(this);
		
		loadCustomFont(); 
		applyThemeColors();
		
		ImageView btnBack = findViewById(R.id.btnSettingsBack);
		btnBack.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { finish(); } });
		
		// ১. অ্যাপ লক
		findViewById(R.id.set_applock).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) { handleAppLock(); }
		});
		
		// ২. কালার থিম
		findViewById(R.id.set_theme).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				int currentTheme = ThemeHelper.getSavedTheme(SettingsActivity.this);
				showOptionsSheet("কালার থিম নির্বাচন করুন", new String[]{"ডার্ক টিল (ডিফল্ট)", "লাইট টিল", "ডার্ক ব্লু", "সিপিয়া (Sepia)"}, currentTheme, new OptionSelectListener() {
					@Override public void onOptionSelected(int index) {
						ThemeHelper.setTheme(SettingsActivity.this, index);
						applyThemeColors(); 
						Toast.makeText(SettingsActivity.this, "থিম পরিবর্তন করা হয়েছে", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
       
       
       // ১. আপনার লিনিয়ার লেআউট (বাটন) ফাইন্ড করা
android.widget.LinearLayout btnConnect = findViewById(R.id.connect_partner); 

// ২. ক্লিক লিসেনার বসানো
btnConnect.setOnClickListener(new android.view.View.OnClickListener() {
    @Override
    public void onClick(android.view.View v) {
        try {
            // ৩. Intent তৈরি এবং পেজ চেঞ্জ করার লজিক
            android.content.Intent intent = new android.content.Intent();
            
            // ডায়নামিক প্যাকেজ নেম ব্যবহার (ক্লাস নট ফাউন্ড এরর এড়াতে)
            intent.setClassName(SettingsActivity.this, getPackageName() + ".ConnectActivity");
            
            // ৪. অ্যাক্টিভিটি স্টার্ট করা
            startActivity(intent);
            
        } catch (Exception e) {
            // যদি কোনো কারণে পেজটা না পায়, তাহলে অ্যাপ ক্র্যাশ না করে একটা মেসেজ দেখাবে
            android.widget.Toast.makeText(SettingsActivity.this, "Error: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }
});



		
		// ৩. ফন্ট স্টাইল 
		findViewById(R.id.set_font_style).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				int currentStyle = appSettings.getInt("font_style", 0);
				String[] fontOptions = {
					"সিস্টেম ডিফল্ট", 
					"সোলায়মান লিপি (SolaimanLipi)", 
					"কালপুরুষ (Kalpurush)", 
					"সিয়াম রূপালী (SiyamRupali)", 
					"হিন্দ শিলিগুড়ি (HindSiliguri)"
				};
				
				showOptionsSheet("ফন্ট স্টাইল", fontOptions, currentStyle, new OptionSelectListener() {
					@Override public void onOptionSelected(int index) {
						editor.putInt("font_style", index); 
						editor.apply(); 
						loadCustomFont(); 
						applyThemeColors(); 
						Toast.makeText(SettingsActivity.this, "ফন্ট স্টাইল পরিবর্তন করা হয়েছে", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
		
		// ૪. ফন্ট সাইজ
		findViewById(R.id.set_font_size).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				int currentSize = appSettings.getInt("font_size", 18);
				int checkedItem = 1; if (currentSize == 14) checkedItem = 0; else if (currentSize == 24) checkedItem = 2;
				showOptionsSheet("অক্ষরের আকার", new String[]{"ছোট", "মাঝারি", "বড়"}, checkedItem, new OptionSelectListener() {
					@Override public void onOptionSelected(int index) {
						int newSize = 18; if (index == 0) newSize = 14; else if (index == 2) newSize = 24;
						editor.putInt("font_size", newSize); editor.apply(); 
						Toast.makeText(SettingsActivity.this, "অক্ষরের আকার পরিবর্তন করা হয়েছে", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
		
		// ৫. অটো-সেভ
		findViewById(R.id.set_autosave).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				boolean isAutoSave = appSettings.getBoolean("is_autosave", true);
				showOptionsSheet("অটো-সেভ", new String[]{"বন্ধ রাখুন", "চালু রাখুন"}, isAutoSave ? 1 : 0, new OptionSelectListener() {
					@Override public void onOptionSelected(int index) {
						editor.putBoolean("is_autosave", index == 1); editor.apply(); 
						Toast.makeText(SettingsActivity.this, "অটো-সেভ আপডেট করা হয়েছে", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
		
		// ৬. এক্সপোর্ট
		findViewById(R.id.set_export).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				List<LocalNote> notes = dbHelper.getAllNotes(0);
				if (notes.isEmpty()) { Toast.makeText(SettingsActivity.this, "এক্সপোর্ট করার মতো কোনো নোট নেই!", Toast.LENGTH_SHORT).show(); return; }
				StringBuilder exportText = new StringBuilder("--- আমার এক্সপোর্ট করা নোট ---\n\n");
				for (LocalNote note : notes) { exportText.append("শিরোনাম: ").append(note.title).append("\nতারিখ: ").append(note.timestamp).append("\n\n").append(note.content).append("\n---------------------------\n\n"); }
				Intent intent = new Intent(Intent.ACTION_SEND); intent.setType("text/plain"); intent.putExtra(Intent.EXTRA_TEXT, exportText.toString()); startActivity(Intent.createChooser(intent, "শেয়ার করুন"));
			}
		});
		
		// ৭. রিসাইকেল বিন
		findViewById(R.id.set_trash).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) { showTrashDialog(); }
		});
		
		// ==========================================
		// 🌟 ৮. ক্লাউড সিঙ্ক আপডেট (Full Suite) 🌟
		// ==========================================
		findViewById(R.id.set_sync).setOnClickListener(new View.OnClickListener() { 
			@Override public void onClick(View v) { 
				showCloudSyncSheet(); 
			} 
		});
		
		findViewById(R.id.set_about).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { Toast.makeText(SettingsActivity.this, "TunePad v1.0 ❤️", Toast.LENGTH_LONG).show(); } });
	}
	
	// ==========================================
	// 🌟 ক্লাউড সিঙ্ক ও ব্যাকআপ কন্ট্রোল প্যানেল 🌟
	// ==========================================
	private void showCloudSyncSheet() {
		final BottomSheetDialog sheet = new BottomSheetDialog(this);
		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setBackgroundColor(surfaceColor); 
		root.setPadding(64, 64, 64, 64);
		
		TextView title = new TextView(this);
		title.setText("ক্লাউড সিঙ্ক ও ব্যাকআপ ☁️");
		title.setTextColor(accentColor);
		title.setTextSize(20f);
		title.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		title.setPadding(0, 0, 0, 48);
		root.addView(title);
		
		// 🌟 ১. সাধারণ নোট ব্যাকআপ (৩ ঘণ্টা) 🌟
		root.addView(createBackupOptionRow("নোট (Notes)", "প্রতি ৩ ঘণ্টা পর পর অটো ব্যাকআপ", 
		appSettings.getBoolean("auto_backup_enabled", false), 
		new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				editor.putBoolean("auto_backup_enabled", isChecked).apply();
				manageBackupService(NoteAutoBackupService.class, 101, isChecked, 3); 
				showToast(isChecked ? "নোট অটো-ব্যাকআপ চালু হয়েছে!" : "নোট অটো-ব্যাকআপ বন্ধ!");
			}
		}));
		
		// 🌟 ২. প্রজেক্ট ব্যাকআপ (৫ ঘণ্টা) 🌟
		root.addView(createBackupOptionRow("প্রজেক্ট (Projects)", "প্রতি ৫ ঘণ্টা পর পর অটো ব্যাকআপ", 
		appSettings.getBoolean("project_auto_backup_enabled", false), 
		new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				editor.putBoolean("project_auto_backup_enabled", isChecked).apply();
				manageBackupService(ProjectAutoBackupService.class, 102, isChecked, 5); 
				showToast(isChecked ? "প্রজেক্ট অটো-ব্যাকআপ চালু হয়েছে! 🚀" : "প্রজেক্ট অটো-ব্যাকআপ বন্ধ!");
			}
		}));
		
		// 🌟 ৩. চরিত্র ব্যাকআপ (২ ঘণ্টা) 🌟
		root.addView(createBackupOptionRow("চরিত্র (Characters)", "প্রতি ২ ঘণ্টা পর পর অটো ব্যাকআপ", 
		appSettings.getBoolean("char_auto_backup_enabled", false), 
		new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				editor.putBoolean("char_auto_backup_enabled", isChecked).apply();
				manageBackupService(CharacterAutoBackupService.class, 103, isChecked, 2); 
				showToast(isChecked ? "চরিত্র অটো-ব্যাকআপ চালু হয়েছে!" : "চরিত্র অটো-ব্যাকআপ বন্ধ!");
			}
		}));
		
		sheet.setContentView(root);
		sheet.show();
	}
	
	// ==========================================
	// 🌟 হেল্পার ১: সুন্দর সুইচ রো (Row) তৈরি করা 🌟
	// ==========================================
	private LinearLayout createBackupOptionRow(String titleText, String subText, boolean isChecked, CompoundButton.OnCheckedChangeListener listener) {
		LinearLayout row = new LinearLayout(this);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding(0, 32, 0, 32);
		
		LinearLayout textLayout = new LinearLayout(this);
		textLayout.setOrientation(LinearLayout.VERTICAL);
		textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		
		TextView tvTitle = new TextView(this);
		tvTitle.setText(titleText);
		tvTitle.setTextColor(primaryTextColor);
		tvTitle.setTextSize(18f);
		tvTitle.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		textLayout.addView(tvTitle);
		
		TextView tvSub = new TextView(this);
		tvSub.setText(subText);
		tvSub.setTextColor(secondaryTextColor);
		tvSub.setTextSize(13f);
		tvSub.setTypeface(currentTypeface);
		textLayout.addView(tvSub);
		
		row.addView(textLayout);
		
		Switch toggleSwitch = new Switch(this);
		toggleSwitch.setChecked(isChecked);
		toggleSwitch.setOnCheckedChangeListener(listener);
		row.addView(toggleSwitch);
		
		return row;
	}
	
	// ==========================================
	// 🌟 হেল্পার ২: ব্যাকগ্রাউন্ড অ্যালার্ম ম্যানেজার 🌟
	// ==========================================
	private void manageBackupService(Class<?> serviceClass, int requestCode, boolean isEnabled, int hours) {
		Intent intent = new Intent(this, serviceClass);
		int flags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S 
		? android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE 
		: android.app.PendingIntent.FLAG_UPDATE_CURRENT;
		
		android.app.PendingIntent pendingIntent = android.app.PendingIntent.getService(this, requestCode, intent, flags);
		android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
		
		if (isEnabled) {
			long intervalMillis = hours * 60 * 60 * 1000L; 
			if (alarmManager != null) {
				alarmManager.setInexactRepeating(
				android.app.AlarmManager.RTC_WAKEUP, 
				System.currentTimeMillis() + intervalMillis, 
				intervalMillis, 
				pendingIntent);
			}
			startService(intent); // অন করার সাথে সাথে একবার জোর করে রান করিয়ে দেওয়া
		} else {
			if (alarmManager != null) {
				alarmManager.cancel(pendingIntent);
			}
			stopService(intent);
		}
	}
	
	private void showToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}
	
	
	// ==========================================
	// 🌟 ফন্ট লোডিং মেথড 🌟
	// ==========================================
	private void loadCustomFont() {
		int fontIndex = appSettings.getInt("font_style", 0);
		try {
			if (fontIndex == 1) { currentTypeface = Typeface.createFromAsset(getAssets(), "fonts/solaimanlipi.ttf"); } 
			else if (fontIndex == 2) { currentTypeface = Typeface.createFromAsset(getAssets(), "fonts/kalpurush.ttf"); } 
			else if (fontIndex == 3) { currentTypeface = Typeface.createFromAsset(getAssets(), "fonts/siyamrupali.ttf"); } 
			else if (fontIndex == 4) { currentTypeface = Typeface.createFromAsset(getAssets(), "fonts/hindsiliguri.ttf"); } 
			else { currentTypeface = Typeface.DEFAULT; }
		} catch (Exception e) { currentTypeface = Typeface.DEFAULT; }
	}
	
	// ==========================================
	// 🌟 Theme Setup Method 🌟
	// ==========================================
	private void applyThemeColors() {
		bgColor = ThemeHelper.getBgColor(this);
		surfaceColor = ThemeHelper.getSurfaceColor(this);
		accentColor = ThemeHelper.getAccentColor(this);
		primaryTextColor = ThemeHelper.getPrimaryTextColor(this);
		secondaryTextColor = ThemeHelper.getSecondaryTextColor(this);
		
		// Backgrounds
		findViewById(R.id.rootSettings).setBackgroundColor(bgColor);
		findViewById(R.id.toolbarSettings).setBackgroundColor(surfaceColor);
		
		// Category Titles
		TextView tvSecurity = findViewById(R.id.tvCategorySecurity); tvSecurity.setTextColor(accentColor); tvSecurity.setTypeface(currentTypeface, Typeface.BOLD);
		TextView tvDesign = findViewById(R.id.tvCategoryDesign); tvDesign.setTextColor(accentColor); tvDesign.setTypeface(currentTypeface, Typeface.BOLD);
		TextView tvData = findViewById(R.id.tvCategoryData); tvData.setTextColor(accentColor); tvData.setTypeface(currentTypeface, Typeface.BOLD);
		TextView tvAbout = findViewById(R.id.tvCategoryAbout); tvAbout.setTextColor(accentColor); tvAbout.setTypeface(currentTypeface, Typeface.BOLD);
		
		// Primary Texts
		TextView tvTitle = findViewById(R.id.tvSettingsTitle); tvTitle.setTextColor(primaryTextColor); tvTitle.setTypeface(currentTypeface, Typeface.BOLD);
		TextView tvProfile = findViewById(R.id.tvProfileTitle); tvProfile.setTextColor(primaryTextColor); tvProfile.setTypeface(currentTypeface, Typeface.BOLD);
		TextView tvLock = findViewById(R.id.tvLockTitle); tvLock.setTextColor(primaryTextColor); tvLock.setTypeface(currentTypeface, Typeface.BOLD);
		TextView tvTheme = findViewById(R.id.tvThemeTitle); tvTheme.setTextColor(primaryTextColor); tvTheme.setTypeface(currentTypeface, Typeface.BOLD);
		TextView tvFont = findViewById(R.id.tvFontTitle); tvFont.setTextColor(primaryTextColor); tvFont.setTypeface(currentTypeface, Typeface.BOLD);
		TextView tvSize = findViewById(R.id.tvSizeTitle); tvSize.setTextColor(primaryTextColor); tvSize.setTypeface(currentTypeface, Typeface.BOLD);
		TextView tvLayout = findViewById(R.id.tvLayoutTitle); tvLayout.setTextColor(primaryTextColor); tvLayout.setTypeface(currentTypeface, Typeface.BOLD);
		TextView tvSync = findViewById(R.id.tvSyncTitle); tvSync.setTextColor(primaryTextColor); tvSync.setTypeface(currentTypeface, Typeface.BOLD);
		TextView tvAutoSave = findViewById(R.id.tvAutoSaveTitle); tvAutoSave.setTextColor(primaryTextColor); tvAutoSave.setTypeface(currentTypeface, Typeface.BOLD);
		TextView tvExport = findViewById(R.id.tvExportTitle); tvExport.setTextColor(primaryTextColor); tvExport.setTypeface(currentTypeface, Typeface.BOLD);
		TextView tvTrash = findViewById(R.id.tvTrashTitle); tvTrash.setTextColor(primaryTextColor); tvTrash.setTypeface(currentTypeface, Typeface.BOLD);
		TextView tvAboutTitle = findViewById(R.id.tvAboutTitle); tvAboutTitle.setTextColor(primaryTextColor); tvAboutTitle.setTypeface(currentTypeface, Typeface.BOLD);
		
		// Secondary Texts
		TextView subProfile = findViewById(R.id.tvProfileSub); subProfile.setTextColor(secondaryTextColor); subProfile.setTypeface(currentTypeface);
		TextView subLock = findViewById(R.id.tvLockSub); subLock.setTextColor(secondaryTextColor); subLock.setTypeface(currentTypeface);
		TextView subTheme = findViewById(R.id.tvThemeSub); subTheme.setTextColor(secondaryTextColor); subTheme.setTypeface(currentTypeface);
		TextView subFont = findViewById(R.id.tvFontSub); subFont.setTextColor(secondaryTextColor); subFont.setTypeface(currentTypeface);
		TextView subSize = findViewById(R.id.tvSizeSub); subSize.setTextColor(secondaryTextColor); subSize.setTypeface(currentTypeface);
		TextView subLayout = findViewById(R.id.tvLayoutSub); subLayout.setTextColor(secondaryTextColor); subLayout.setTypeface(currentTypeface);
		TextView subAutoSave = findViewById(R.id.tvAutoSaveSub); subAutoSave.setTextColor(secondaryTextColor); subAutoSave.setTypeface(currentTypeface);
		TextView subExport = findViewById(R.id.tvExportSub); subExport.setTextColor(secondaryTextColor); subExport.setTypeface(currentTypeface);
		TextView subTrash = findViewById(R.id.tvTrashSub); subTrash.setTextColor(secondaryTextColor); subTrash.setTypeface(currentTypeface);
		TextView subAbout = findViewById(R.id.tvAboutSub); subAbout.setTextColor(secondaryTextColor); subAbout.setTypeface(currentTypeface);
		
		// Update Theme Subtitle
		int savedTheme = ThemeHelper.getSavedTheme(this);
		String themeName = "ডার্ক টিল (ডিফল্ট)";
		if(savedTheme == 1) themeName = "লাইট টিল";
		else if(savedTheme == 2) themeName = "ডার্ক ব্লু";
		else if(savedTheme == 3) themeName = "সিপিয়া (Sepia)";
		subTheme.setText(themeName);
		
		// Update Font Subtitle
		int savedFont = appSettings.getInt("font_style", 0);
		String fontName = "সিস্টেম ডিফল্ট";
		if(savedFont == 1) fontName = "সোলায়মান লিপি";
		else if(savedFont == 2) fontName = "কালপুরুষ";
		else if(savedFont == 3) fontName = "সিয়াম রূপালী";
		else if(savedFont == 4) fontName = "হিন্দ শিলিগুড়ি";
		subFont.setText(fontName);
		
		// Icons
		((ImageView)findViewById(R.id.btnSettingsBack)).setColorFilter(accentColor);
		((ImageView)findViewById(R.id.iconProfile)).setColorFilter(secondaryTextColor);
		((ImageView)findViewById(R.id.iconLock)).setColorFilter(secondaryTextColor);
		((ImageView)findViewById(R.id.iconTheme)).setColorFilter(secondaryTextColor);
		((ImageView)findViewById(R.id.iconFontStyle)).setColorFilter(secondaryTextColor);
		((ImageView)findViewById(R.id.iconFontSize)).setColorFilter(secondaryTextColor);
		((ImageView)findViewById(R.id.iconLayout)).setColorFilter(secondaryTextColor);
		((ImageView)findViewById(R.id.iconSync)).setColorFilter(secondaryTextColor);
		((ImageView)findViewById(R.id.iconAutoSave)).setColorFilter(secondaryTextColor);
		((ImageView)findViewById(R.id.iconExport)).setColorFilter(secondaryTextColor);
		((ImageView)findViewById(R.id.iconTrash)).setColorFilter(secondaryTextColor);
		((ImageView)findViewById(R.id.iconAbout)).setColorFilter(secondaryTextColor);
	}
	
	private void showTrashDialog() {
		final Dialog trashDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
		LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bgColor);
		
		LinearLayout toolbar = new LinearLayout(this); toolbar.setOrientation(LinearLayout.HORIZONTAL); toolbar.setGravity(Gravity.CENTER_VERTICAL); toolbar.setPadding(16, 32, 16, 32); toolbar.setBackgroundColor(surfaceColor);
		ImageView btnClose = new ImageView(this); btnClose.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); btnClose.setColorFilter(accentColor); btnClose.setPadding(16,16,16,16); btnClose.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { trashDialog.dismiss(); } });
		TextView title = new TextView(this); title.setText("রিসাইকেল বিন"); title.setTextColor(primaryTextColor); title.setTextSize(20f); title.setTypeface(currentTypeface, Typeface.BOLD); title.setPadding(32, 0, 0, 0);
		toolbar.addView(btnClose); toolbar.addView(title); root.addView(toolbar);
		
		ScrollView scroll = new ScrollView(this); scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		final LinearLayout container = new LinearLayout(this); container.setOrientation(LinearLayout.VERTICAL); container.setPadding(16, 16, 16, 16); scroll.addView(container); root.addView(scroll);
		
		List<LocalNote> deletedNotes = dbHelper.getDeletedNotes();
		if(deletedNotes.isEmpty()) {
			TextView empty = new TextView(this); empty.setText("রিসাইকেল বিন খালি।"); empty.setTextColor(secondaryTextColor); empty.setTextSize(18f); empty.setTypeface(currentTypeface); empty.setGravity(Gravity.CENTER); empty.setPadding(0, 100, 0, 0); container.addView(empty);
		} else {
			for(final LocalNote note : deletedNotes) {
				final LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); params.setMargins(16, 16, 16, 16); card.setLayoutParams(params); card.setPadding(40, 40, 40, 40);
				GradientDrawable shape = new GradientDrawable(); shape.setCornerRadius(24f); shape.setColor(surfaceColor); card.setBackground(shape);
				
				TextView tTitle = new TextView(this); tTitle.setText(note.title); tTitle.setTextColor(primaryTextColor); tTitle.setTextSize(18f); tTitle.setTypeface(currentTypeface, Typeface.BOLD); card.addView(tTitle);
				TextView tContent = new TextView(this); tContent.setText(note.content); tContent.setTextColor(secondaryTextColor); tContent.setTypeface(currentTypeface); tContent.setMaxLines(2); card.addView(tContent);
				
				LinearLayout btnLayout = new LinearLayout(this); btnLayout.setOrientation(LinearLayout.HORIZONTAL); btnLayout.setGravity(Gravity.END); btnLayout.setPadding(0, 32, 0, 0);
				TextView btnRestore = new TextView(this); btnRestore.setText("ফিরিয়ে আনুন"); btnRestore.setTextColor(accentColor); btnRestore.setTypeface(currentTypeface, Typeface.BOLD); btnRestore.setPadding(32, 16, 32, 16);
				btnRestore.setOnClickListener(new View.OnClickListener() {
					@Override public void onClick(View v) {
						dbHelper.restoreNoteLocal(note.id);
						String savedName = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("UserName", "UnknownUser");
						FirebaseDatabase.getInstance().getReference("DeviceData").child(savedName).child("Notes").child(note.id).child("isDeletedLocally").setValue(false);
						container.removeView(card); Toast.makeText(SettingsActivity.this, "নোট রিস্টোর করা হয়েছে", Toast.LENGTH_SHORT).show();
					}
				});
				TextView btnDelete = new TextView(this); btnDelete.setText("স্থায়ীভাবে মুছুন"); btnDelete.setTextColor(Color.parseColor("#E53935")); btnDelete.setTypeface(currentTypeface, Typeface.BOLD); btnDelete.setPadding(32, 16, 16, 16);
				btnDelete.setOnClickListener(new View.OnClickListener() {
					@Override public void onClick(View v) {
						dbHelper.deleteForeverLocal(note.id); 
						container.removeView(card); Toast.makeText(SettingsActivity.this, "স্থায়ীভাবে মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show();
					}
				});
				btnLayout.addView(btnRestore); btnLayout.addView(btnDelete); card.addView(btnLayout); container.addView(card);
			}
		}
		trashDialog.setContentView(root); trashDialog.show();
	}
	
	private interface OptionSelectListener { void onOptionSelected(int index); }
	
	private void showOptionsSheet(String title, final String[] options, int checkedIndex, final OptionSelectListener listener) {
		final BottomSheetDialog sheet = new BottomSheetDialog(this);
		LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(surfaceColor); root.setPadding(0, 32, 0, 32);
		
		TextView tvTitle = new TextView(this); tvTitle.setText(title); tvTitle.setTextColor(accentColor); tvTitle.setTextSize(18f); tvTitle.setTypeface(currentTypeface, Typeface.BOLD); tvTitle.setPadding(48, 16, 48, 32); root.addView(tvTitle);
		
		for (int i = 0; i < options.length; i++) {
			final int index = i;
			LinearLayout item = new LinearLayout(this); item.setOrientation(LinearLayout.HORIZONTAL); item.setPadding(48, 32, 48, 32); item.setGravity(Gravity.CENTER_VERTICAL);
			TextView tv = new TextView(this); tv.setText(options[i]); tv.setTextColor(primaryTextColor); tv.setTextSize(16f); tv.setTypeface(currentTypeface); tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)); item.addView(tv);
			
			if (i == checkedIndex) {
				ImageView check = new ImageView(this); check.setImageResource(android.R.drawable.checkbox_on_background); check.setColorFilter(accentColor); item.addView(check);
			}
			item.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { listener.onOptionSelected(index); sheet.dismiss(); } });
			root.addView(item);
		}
		sheet.setContentView(root); sheet.show();
	}
	
	private void handleAppLock() {
		String currentPin = appSettings.getString("app_pin", "");
		if (currentPin.isEmpty()) { showPinSetupSheet(true); } 
		else {
			showOptionsSheet("অ্যাপ লক", new String[]{"পিন পরিবর্তন করুন", "পিন বন্ধ করুন"}, -1, new OptionSelectListener() {
				@Override public void onOptionSelected(int index) {
					if (index == 0) showPinSetupSheet(false);
					else { editor.putString("app_pin", ""); editor.apply(); Toast.makeText(SettingsActivity.this, "পিন বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show(); }
				}
			});
		}
	}
	
	private void showPinSetupSheet(final boolean isNew) {
		final BottomSheetDialog sheet = new BottomSheetDialog(this);
		LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(surfaceColor); root.setPadding(48, 48, 48, 48);
		
		TextView tvTitle = new TextView(this); tvTitle.setText(isNew ? "নতুন পিন সেট করুন" : "পিন পরিবর্তন করুন"); tvTitle.setTextColor(accentColor); tvTitle.setTextSize(18f); tvTitle.setTypeface(currentTypeface, Typeface.BOLD); tvTitle.setPadding(0, 0, 0, 32); root.addView(tvTitle);
		final EditText input = new EditText(this); input.setHint("৪-ডিজিটের পিন দিন"); input.setHintTextColor(secondaryTextColor); input.setTextColor(primaryTextColor); input.setTypeface(currentTypeface); input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD); root.addView(input);
		
		TextView btnSave = new TextView(this); btnSave.setText("সেভ করুন"); btnSave.setTextColor(accentColor); btnSave.setTypeface(currentTypeface, Typeface.BOLD); btnSave.setGravity(Gravity.END); btnSave.setPadding(0, 48, 16, 16);
		btnSave.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				String pin = input.getText().toString().trim();
				if (pin.length() >= 4) { editor.putString("app_pin", pin); editor.apply(); Toast.makeText(SettingsActivity.this, "পিন সেভ করা হয়েছে", Toast.LENGTH_SHORT).show(); sheet.dismiss(); } 
				else { Toast.makeText(SettingsActivity.this, "পিন অন্তত ৪ ডিজিটের হতে হবে", Toast.LENGTH_SHORT).show(); }
			}
		});
		root.addView(btnSave); sheet.setContentView(root); sheet.show();
	}
	
	private static class LocalNote { String id, title, content, timestamp; }
	
	private class NoteDatabaseHelper extends SQLiteOpenHelper {
		private static final String DATABASE_NAME = "notes_db_v2"; private static final int DATABASE_VERSION = 2; private static final String TABLE_NOTES = "notes";
		public NoteDatabaseHelper(Context context) { super(context, DATABASE_NAME, null, DATABASE_VERSION); }
		@Override public void onCreate(SQLiteDatabase db) {} @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
		
		public List<LocalNote> getAllNotes(int isDraft) {
			List<LocalNote> list = new ArrayList<>(); SQLiteDatabase db = this.getReadableDatabase();
			Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NOTES + " WHERE isDeleted=0 AND isDraft=" + isDraft, null);
			if (cursor.moveToFirst()) { do { LocalNote note = new LocalNote(); note.id = cursor.getString(0); note.title = cursor.getString(1); note.content = cursor.getString(2); note.timestamp = cursor.getString(4); list.add(note); } while (cursor.moveToNext()); }
			cursor.close(); return list;
		}
		
		public List<LocalNote> getDeletedNotes() {
			List<LocalNote> list = new ArrayList<>(); SQLiteDatabase db = this.getReadableDatabase();
			Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NOTES + " WHERE isDeleted=1", null);
			if (cursor.moveToFirst()) { do { LocalNote note = new LocalNote(); note.id = cursor.getString(0); note.title = cursor.getString(1); note.content = cursor.getString(2); note.timestamp = cursor.getString(4); list.add(note); } while (cursor.moveToNext()); }
			cursor.close(); return list;
		}
		
		public void restoreNoteLocal(String id) {
			SQLiteDatabase db = this.getWritableDatabase(); ContentValues values = new ContentValues(); values.put("isDeleted", 0); db.update(TABLE_NOTES, values, "id=?", new String[]{id});
		}
		
		public void deleteForeverLocal(String id) {
			SQLiteDatabase db = this.getWritableDatabase(); db.delete(TABLE_NOTES, "id=?", new String[]{id});
		}
	}
}
