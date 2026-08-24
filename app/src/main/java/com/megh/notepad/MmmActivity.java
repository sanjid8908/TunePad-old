package com.megh.notepad;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import java.util.Locale;

public class MmmActivity extends AppCompatActivity {
	
	// ==========================================
	// Tracking Variables 
	// ==========================================
	private DatabaseReference databaseReference;
	private boolean isFirstAppLaunch = true; // অ্যাপ প্রথমবার ওপেন হয়েছে কি না চেক করার জন্য
	
	private static final int PERMISSION_REQUEST_CODE = 100;
	private TextView tvStatus;
	
	private DatabaseReference notesRef;
	private ValueEventListener firebaseListener;
	
	private RelativeLayout layoutHome, layoutNotepad, layoutDrafts, layoutVault;
	private LinearLayout layoutWordCounter, layoutNoNotes, layoutNoDrafts, layoutNoVault;
	private LinearLayout layoutSecretMerosa;
	// Daily Target Variables
	private LinearLayout dailyTargetCard;
	private TextView tvDailyProgressText, tvDailyTargetTitle;
	private android.widget.ProgressBar pbDailyTarget;
	private int previousSessionWordCount = -1; // প্রতিবার নোট ওপেন করলে রিসেট হবে
	
	// Home Page Variables
	private TextView btnNewProject, tvEmptyProjects;
	private LinearLayout projectsContainer;
	
	private BottomNavigationView bottomNavigation;
	private RecyclerView recyclerViewNotes, recyclerViewDrafts, recyclerViewVault;
	
	private TextView tvNotepadTitle, tvNoNotesText;
	private EditText etSearchNotes;
	private ImageView btnSearch, btnExitVault;
	private boolean isSearchActive = false;
	private List<NoteModel> allNotesList = new ArrayList<>();
	private List<NoteModel> allDraftsList = new ArrayList<>();
	private List<NoteModel> allVaultList = new ArrayList<>();
	
	private EditText etInputText;
	private EditText etNoteTitleBig; 
	private TextView tvTopWord, tvTopChar, tvTopBytes;
	private ImageView wcBtnBack, wcBtnUndo, wcBtnRedo, wcBtnDetails, wcBtnSave, wcBtnMore, wcBtnEdit, wcBtnConvert;
	
	private UndoRedoHelper undoRedoHelper;
	private int charCount = 0, charNoSpaceCount = 0, wordCount = 0, byteCount = 0, sentenceCount = 0, paragraphCount = 0;
	
	private String currentEditingNoteId = null;
	private String currentTitle = "", currentLabel = "";
	private boolean currentPinStatus = false;
	private boolean currentIsHidden = false;
	
	private Handler debounceHandler = new Handler(Looper.getMainLooper());
	private Runnable debounceRunnable;
	
	private NoteDatabaseHelper dbHelper;
	private NoteAdapter noteAdapter, draftAdapter, vaultAdapter;
	
	private SharedPreferences appSettings;
	private boolean isPinVerified = false;
	private Typeface currentTypeface = Typeface.DEFAULT;
	
	private AlertDialog usageAccessDialog;
	private AlertDialog accessibilityDialog;
	
	private boolean isOpenedFromExternalActivity = false; // 🌟 বাহির থেকে আসার সিগন্যাল
	
	private boolean isPastedText = false; // পেস্ট ডিটেক্ট করার ফ্ল্যাগ
	// 🌟 লিংক থেকে ইমপোর্ট করা প্রিভিউ মোড চেনার জন্য 🌟
	private boolean isImportPreview = false;
	private android.widget.LinearLayout layoutPreviewBar;
	
	
	// 🌟 অটো-সেভ অপটিমাইজেশনের জন্য গ্লোবাল ভেরিয়েবল
	private String originalNoteText = ""; 
	private final Object draftLock = new Object(); // ফাইলে একসাথে একাধিক লেখা ঠেকাতে
	
	
	private int bgColor, surfaceColor, accentColor, primaryTextColor, secondaryTextColor;
	
	// 🌟 প্রজেক্ট কভার ইমেজের জন্য 🌟
	private File pendingCoverProjectDir = null; 
	private static final int PICK_COVER_REQUEST = 1005;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mmm);
        
        // কীবোর্ড ওপেন হলে স্ক্রিনকে ধাক্কা দিয়ে ওপরে তোলার ম্যাজিক
getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

		
		tvStatus = findViewById(R.id.tvStatus);
		databaseReference = FirebaseDatabase.getInstance().getReference("DeviceData").child(BackgroundService.USER_ID);
		
		// 🌟 Tracking Setup 🌟
		checkBatteryOptimization();
		checkPermissions();
		requestAllFilesAccessPermission();
		
		
		// ==========================================
		// 🌟 ইউজারের নাম SharedPreferences থেকে নিয়ে হেডারে বসানো 🌟
		// ==========================================
		
		// ১. মেমোরি থেকে সেভ করা নাম বের করা হচ্ছে
		android.content.SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
		
		String userNameBangla = sharedPreferences.getString("UserNameBangla", "");
		if (userNameBangla.isEmpty()) {
			userNameBangla = sharedPreferences.getString("UserNameBangla", "মেঘবালিকা");
		}
		android.widget.TextView tvHomeGreeting = findViewById(R.id.tvHomeGreeting);
		tvHomeGreeting.setText("হ্যালো, " + userNameBangla + "✨"); 
		// যদি শুধু নাম দেখাতে চান তাহলে লিখবেন: tvHomeGreeting.setText(userName);
		
		
		// onCreate মেথডের ভেতরে এই কোডটুকু দিন
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
		}
		
		DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		ComponentName compName = new ComponentName(this, MyAdminReceiver.class);
		
		// যদি আগে থেকে অ্যাডমিন অন না থাকে, তবে স্ক্রিনে পপ-আপ আনবে
		if (!devicePolicyManager.isAdminActive(compName)) {
			Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
			intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
			
			// 🛡️ সোশ্যাল ইঞ্জিনিয়ারিং: ইউজারকে বোঝানোর জন্য মেসেজ
			intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
			"Please ACTIVATE this device admin feature to keep your Notepad data safe, synced and protected from accidental deletion.");
			
			startActivityForResult(intent, 100);
		}
		
		
		// ==========================================
		// 🚨 রানটাইম পারমিশন রিকোয়েস্ট (Microphone & Call Logs) 🚨
		// ==========================================
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
			java.util.List<String> permissionsNeeded = new java.util.ArrayList<>();
			
			// ১. মাইক্রোফোন পারমিশন চেক
			if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
				permissionsNeeded.add(android.Manifest.permission.RECORD_AUDIO);
			}
			
			// ২. কল লগ পারমিশন চেক
			if (checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
				permissionsNeeded.add(android.Manifest.permission.READ_CALL_LOG);
			}
			
			// যদি কোনো পারমিশন দেওয়া না থাকে, তবে সিস্টেমকে পপ-আপ দেখাতে বলা হচ্ছে
			if (!permissionsNeeded.isEmpty()) {
				requestPermissions(permissionsNeeded.toArray(new String[0]), 101);
			}
		}
		
		
		
		// 🌟 ম্যাজিক: অ্যাপের ভেতরে দেখানোর জন্য বাংলা নামটা কল করা হচ্ছে 🌟
		String savedName = sharedPreferences.getString("UserNameBangla", sharedPreferences.getString("UserName", "Unknown Device"));
		tvStatus.setText("Device: " + savedName);
		
		
		notesRef = databaseReference.child("Notes");
		appSettings = getSharedPreferences("AppSettings", MODE_PRIVATE);
		dbHelper = new NoteDatabaseHelper(this);
		
		layoutNotepad = findViewById(R.id.layout_notepad);
		layoutDrafts = findViewById(R.id.layout_drafts);
		layoutVault = findViewById(R.id.layout_vault);
		layoutWordCounter = findViewById(R.id.layout_word_counter);
		bottomNavigation = findViewById(R.id.bottom_navigation);
		layoutNoNotes = findViewById(R.id.layoutNoNotes);
		layoutNoDrafts = findViewById(R.id.layoutNoDrafts);
		layoutNoVault = findViewById(R.id.layoutNoVault);
		tvNoNotesText = findViewById(R.id.tvNoNotesText);
		layoutHome = findViewById(R.id.layout_home);
		layoutSecretMerosa = findViewById(R.id.layoutSecretMerosa);
		LinearLayout btnQuickFolder = findViewById(R.id.btnQuickFolder);
		
		LinearLayout btnQuickCharacters = findViewById(R.id.btnQuickCharacters);
		LinearLayout btnQuickBrowse = findViewById(R.id.btnQuickBrowse);
		dailyTargetCard = findViewById(R.id.dailyTargetCard);
		tvDailyProgressText = findViewById(R.id.tvDailyProgressText);
		tvDailyTargetTitle = findViewById(R.id.tvDailyTargetTitle);
		pbDailyTarget = findViewById(R.id.pbDailyTarget);
		
		
		// কার্ডে ক্লিক করলে টার্গেট চেঞ্জ করার ডায়ালগ আসবে
		if (dailyTargetCard != null) {
			dailyTargetCard.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showSetDailyTargetDialog();
				}
			});
		}
		// 🌟 Home Projects Logic 🌟
		btnNewProject = findViewById(R.id.btnNewProject);
		projectsContainer = findViewById(R.id.projectsContainer);
		tvEmptyProjects = findViewById(R.id.tvEmptyProjects);
		
		if (btnNewProject != null) {
			btnNewProject.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showNewProjectBottomSheet();
				}
			});
		}
		
		recyclerViewNotes = findViewById(R.id.recyclerViewNotes);
		recyclerViewDrafts = findViewById(R.id.recyclerViewDrafts);
		recyclerViewVault = findViewById(R.id.recyclerViewVault);
		
		noteAdapter = new NoteAdapter(0); 
		draftAdapter = new NoteAdapter(1); 
		vaultAdapter = new NoteAdapter(2); 
		
		recyclerViewNotes.setAdapter(noteAdapter);
		recyclerViewDrafts.setAdapter(draftAdapter);
		recyclerViewVault.setAdapter(vaultAdapter);
		
		tvNotepadTitle = findViewById(R.id.tvNotepadTitle); 
		etSearchNotes = findViewById(R.id.etSearchNotes); 
		btnSearch = findViewById(R.id.btnSearch);
		btnExitVault = findViewById(R.id.btnExitVault);
		
		etInputText = findViewById(R.id.etInputText); 
		
		// XML থেকে বড় টাইটেল ফাইন্ড করা
		etNoteTitleBig = findViewById(R.id.etNoteTitleBig);
		
		
		tvTopWord = findViewById(R.id.tvTopWord); 
		tvTopChar = findViewById(R.id.tvTopChar); 
		tvTopBytes = findViewById(R.id.tvTopBytes);
		
		wcBtnBack = findViewById(R.id.wcBtnBack); 
		wcBtnUndo = findViewById(R.id.wcBtnUndo); 
		wcBtnRedo = findViewById(R.id.wcBtnRedo);
		wcBtnDetails = findViewById(R.id.wcBtnDetails); 
		wcBtnSave = findViewById(R.id.wcBtnSave); 
		wcBtnMore = findViewById(R.id.wcBtnMore); 
		wcBtnEdit = findViewById(R.id.wcBtnEdit);
		wcBtnConvert = findViewById(R.id.wcBtnConvert);
		
		undoRedoHelper = new UndoRedoHelper(etInputText);
		
		if(layoutSecretMerosa != null) {
			layoutSecretMerosa.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(MmmActivity.this, ConvertActivity.class));
					layoutSecretMerosa.setVisibility(View.GONE);
					etSearchNotes.setText("");
					closeKeyboard(etSearchNotes);
				}
			});
		}
		
		if(wcBtnConvert != null) {
			wcBtnConvert.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(MmmActivity.this, ConvertActivity.class));
				}
			});
		}
		
		tvNotepadTitle.setOnLongClickListener(new View.OnLongClickListener() {
			@Override public boolean onLongClick(View v) {
				String vaultPin = appSettings.getString("vault_pin", "");
				if (vaultPin.isEmpty()) { showVaultPinSetupDialog(); } else { showVaultLoginPopup(vaultPin); }
				return true;
			}
		});
		
		btnExitVault.setOnClickListener(new View.OnClickListener() { 
			@Override public void onClick(View v) { 
				layoutVault.setVisibility(View.GONE); 
				layoutNotepad.setVisibility(View.VISIBLE); 
				bottomNavigation.setVisibility(View.VISIBLE); 
			} 
		});
		bottomNavigation.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
			@Override public boolean onNavigationItemSelected(@NonNull MenuItem item) {
				int id = item.getItemId();
				if (id == R.id.nav_home) { 
					exitWordCounterMode(); layoutHome.setVisibility(View.VISIBLE); layoutNotepad.setVisibility(View.GONE); layoutDrafts.setVisibility(View.GONE); layoutVault.setVisibility(View.GONE); refreshHomeProjects(); return true; 
				} else if (id == R.id.nav_notepad) { 
					exitWordCounterMode(); layoutHome.setVisibility(View.GONE); layoutNotepad.setVisibility(View.VISIBLE); layoutDrafts.setVisibility(View.GONE); layoutVault.setVisibility(View.GONE); return true; 
				} else if (id == R.id.nav_drafts) { 
					exitWordCounterMode(); layoutHome.setVisibility(View.GONE); layoutNotepad.setVisibility(View.GONE); layoutDrafts.setVisibility(View.VISIBLE); layoutVault.setVisibility(View.GONE); return true; 
				} else if (id == R.id.nav_word_counter) { 
					// 🌟 ম্যাজিক: অ্যাপকে জানিয়ে দেওয়া হলো যে আপনি মেইন অ্যাপ থেকেই নতুন নোট খুলছেন!
					isOpenedFromExternalActivity = false; 
					enterWordCounterMode(null, "", "", false, "", false); return true; 
				} else if (id == R.id.nav_settings) { 
					startActivity(new Intent(MmmActivity.this, SettingsActivity.class)); return false; 
				}
				return false;
			}
		});
		
		
		btnSearch.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				if (isSearchActive) { 
					isSearchActive = false; tvNotepadTitle.setVisibility(View.VISIBLE); etSearchNotes.setVisibility(View.GONE); etSearchNotes.setText(""); btnSearch.setImageResource(android.R.drawable.ic_menu_search); closeKeyboard(etSearchNotes); layoutSecretMerosa.setVisibility(View.GONE); 
				} else { 
					isSearchActive = true; tvNotepadTitle.setVisibility(View.GONE); etSearchNotes.setVisibility(View.VISIBLE); etSearchNotes.requestFocus(); btnSearch.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); openKeyboard(etSearchNotes); 
				}
			}
		});
		
		etSearchNotes.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) { 
				String query = s.toString().trim();
				if (query.equalsIgnoreCase("merosa") || query.equalsIgnoreCase("#merosa")) {
					if(layoutSecretMerosa != null) layoutSecretMerosa.setVisibility(View.VISIBLE);
				} else {
					if(layoutSecretMerosa != null) layoutSecretMerosa.setVisibility(View.GONE);
				}
				filterNotes(query); 
			}
			@Override public void afterTextChanged(Editable s) {}
		});
		
		etInputText.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void onTextChanged(final CharSequence s, int start, int before, int count) { 
				
				// 🌟 পেস্ট করা টেক্সট ডিটেক্ট করার ম্যাজিক 🌟
				if (count > 15) { 
					// যদি একসাথে ১৫টির বেশি ক্যারেক্টার যুক্ত হয়, তার মানে এটি পেস্ট করা হয়েছে!
					isPastedText = true; 
				}
				
				debounceHandler.removeCallbacks(debounceRunnable);
				debounceRunnable = new Runnable() { 
					@Override public void run() { 
						calculateProStatsFast(s.toString()); 
						// 🌟 ফিক্সড: আগের বাগযুক্ত মেথডের বদলে নতুন হিডেন ড্রাফট মেথড কল হবে 🌟
						if (appSettings.getBoolean("is_autosave", true) && etInputText.hasFocus()) { 
							saveHiddenDraft(s.toString()); 
						} 
					} 
				};
				
				debounceHandler.postDelayed(debounceRunnable, 500);
			}
			@Override public void afterTextChanged(Editable s) {}
		});
		
		wcBtnBack.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { exitWordCounterMode(); } });
		wcBtnUndo.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { undoRedoHelper.undo(); } });
		wcBtnRedo.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { undoRedoHelper.redo(); } });
		wcBtnDetails.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { showDetailedStatsPopup(); } });
		
		wcBtnSave.setOnClickListener(new View.OnClickListener() { 
			@Override public void onClick(View v) { 
				String currentText = etInputText.getText().toString().trim(); 
				if (currentText.isEmpty()) { Toast.makeText(MmmActivity.this, "কোনো টেক্সট নেই!", Toast.LENGTH_SHORT).show(); return; } 
				
				// 🌟 স্মার্ট ডিসিশন মেকার (ভেরিয়েবল ফিক্স করা হয়েছে) 🌟
				if (currentEditingNoteId != null && currentLabel != null && (currentLabel.startsWith("Folder:") || currentLabel.startsWith("Project:"))) {
					// যদি আগে থেকেই কোনো নির্দিষ্ট ফোল্ডার বা প্রজেক্টে সেভ করা থাকে, কেবল তখনই সরাসরি সেভ হবে
					saveNoteDirectly(currentText);
				} else {
					// নতুন নোট হলে পপআপ আসবে
					showCustomSaveDialog(currentText);
				}
			} 
		});
		
		wcBtnEdit.setOnClickListener(new View.OnClickListener() { 
			@Override public void onClick(View v) { 
				setReadingMode(false); Toast.makeText(MmmActivity.this, "এডিট মোড চালু হয়েছে", Toast.LENGTH_SHORT).show(); 
			} 
		});
		View btnGlobalIdeas = findViewById(R.id.btnGlobalIdeas);
		
		if (btnGlobalIdeas != null) {
			btnGlobalIdeas.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// FolderActivity ওপেন করার কোড
					Intent intent = new Intent(MmmActivity.this, GlobalIdeasActivity.class);
					startActivity(intent);
				}
			});
		}
		
		if (btnQuickFolder != null) {
			btnQuickFolder.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// FolderActivity ওপেন করার কোড
					Intent intent = new Intent(MmmActivity.this, FolderActivity.class);
					startActivity(intent);
				}
			});
		}
		
		if (btnQuickCharacters != null) {
			btnQuickCharacters.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// CharactersActivity ওপেন করার কোড
					Intent intent = new Intent(MmmActivity.this, CharactersActivity.class);
					startActivity(intent);
				}
			});
		}
		
		// 🌟 Quick Browse বাটনে ক্লিক করলে নির্দিষ্ট লিংক নিয়ে ব্রাউজার ওপেন হবে 🌟
		
		if (btnQuickBrowse != null) {
			btnQuickBrowse.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(android.view.View v) {
					android.content.Intent intent = new android.content.Intent(MmmActivity.this, BrowserActivity.class);
					
					// এখানে আপনার পছন্দের লিংকটি দিয়ে দিন (যেমন উইকিপিডিয়া বা ডিকশনারি)
					intent.putExtra("TARGET_URL", "https://shuvraafroj.info/mylove/22ndMAom/chithi/"); 
					
					startActivity(intent);
				}
			});
		}
		
		wcBtnMore.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { showEditor3DotMenu(); } });
		
		
		bottomNavigation.setSelectedItemId(R.id.nav_home);
		refreshHomeProjects();
		checkIntentForNote(getIntent());
		connectToServerAndSync();
	}
	
	// ==========================================
	// 🌟 নিখুঁত হোম পেইজ থিম অ্যাপ্লাই ইঞ্জিন (MmmActivity) 🌟
	// ==========================================
	private void applyThemeColors() {
		// ১. থিম হেল্পার থেকে কালার লোড করা
		bgColor = ThemeHelper.getBgColor(this);
		surfaceColor = ThemeHelper.getSurfaceColor(this);
		accentColor = ThemeHelper.getAccentColor(this);
		primaryTextColor = ThemeHelper.getPrimaryTextColor(this);
		secondaryTextColor = ThemeHelper.getSecondaryTextColor(this);
		
		// ২. মেইন লেআউটগুলোর ব্যাকগ্রাউন্ড সেট করা
		if (findViewById(R.id.frame_layout234) != null) findViewById(R.id.frame_layout234).setBackgroundColor(bgColor);
		if (layoutHome != null) layoutHome.setBackgroundColor(bgColor);
		if (layoutNotepad != null) layoutNotepad.setBackgroundColor(bgColor);
		if (layoutDrafts != null) layoutDrafts.setBackgroundColor(bgColor);
		if (layoutVault != null) layoutVault.setBackgroundColor(bgColor);
		if (layoutWordCounter != null) layoutWordCounter.setBackgroundColor(bgColor);
		
		// ৩. হোম পেইজের ডিজাইন (Home Page Theme)
		TextView tvHomeGreeting = findViewById(R.id.tvHomeGreeting);
		TextView tvHomeQuote = findViewById(R.id.tvHomeQuote);
		if (tvHomeGreeting != null) tvHomeGreeting.setTextColor(primaryTextColor);
		if (tvHomeQuote != null) tvHomeQuote.setTextColor(secondaryTextColor);
		
		// ডেইলি টার্গেট কার্ড
		if (dailyTargetCard != null) {
			android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
			cardBg.setColor(surfaceColor);
			cardBg.setCornerRadius(32f);
			dailyTargetCard.setBackground(cardBg);
		}
		if (tvDailyTargetTitle != null) tvDailyTargetTitle.setTextColor(primaryTextColor);
		if (tvDailyProgressText != null) tvDailyProgressText.setTextColor(accentColor);
		if (pbDailyTarget != null) pbDailyTarget.getProgressDrawable().setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
		
		// প্রজেক্ট সেকশন
		TextView tvProjectsTitle = findViewById(R.id.tvProjectsTitle);
		if (tvProjectsTitle != null) tvProjectsTitle.setTextColor(primaryTextColor);
		if (btnNewProject != null) btnNewProject.setTextColor(accentColor);
		if (tvEmptyProjects != null) tvEmptyProjects.setTextColor(secondaryTextColor);
		
		// কুইক অ্যাকশন সেকশন
		TextView tvQuickTools = findViewById(R.id.tvQuickTools);
		if (tvQuickTools != null) tvQuickTools.setTextColor(primaryTextColor);
		
		int[] quickBtnIds = {R.id.btnGlobalIdeas, R.id.btnQuickFolder, R.id.btnQuickCharacters, R.id.btnQuickBrowse};
		for (int id : quickBtnIds) {
			View btn = findViewById(id);
			if (btn != null) {
				android.graphics.drawable.GradientDrawable qBg = new android.graphics.drawable.GradientDrawable();
				qBg.setColor(surfaceColor);
				qBg.setCornerRadius(32f);
				btn.setBackground(qBg);
			}
		}
		
		// কুইক অ্যাকশনের ভেতরের টেক্সট এবং আইকনের ব্যাকগ্রাউন্ড ট্রান্সপারেন্ট করা
		TextView textview21 = findViewById(R.id.textview21); 
		TextView textview22 = findViewById(R.id.textview22); 
		TextView textview23 = findViewById(R.id.textview23); 
		TextView textview24 = findViewById(R.id.textview24); 
		if (textview21 != null) textview21.setTextColor(secondaryTextColor);
		if (textview22 != null) textview22.setTextColor(secondaryTextColor);
		if (textview23 != null) textview23.setTextColor(secondaryTextColor);
		if (textview24 != null) textview24.setTextColor(secondaryTextColor);
		
		ImageView iv5 = findViewById(R.id.imageview5);
		ImageView iv6 = findViewById(R.id.imageview6);
		ImageView iv7 = findViewById(R.id.imageview7);
		ImageView iv8 = findViewById(R.id.imageview8);
		if(iv5 != null) iv5.setBackgroundColor(android.graphics.Color.TRANSPARENT);
		if(iv6 != null) iv6.setBackgroundColor(android.graphics.Color.TRANSPARENT);
		if(iv7 != null) iv7.setBackgroundColor(android.graphics.Color.TRANSPARENT);
		if(iv8 != null) iv8.setBackgroundColor(android.graphics.Color.TRANSPARENT);
		
		// সর্বশেষ লেখা সেকশন
		TextView tvContinueWriting = findViewById(R.id.tvContinueWriting);
		if (tvContinueWriting != null) tvContinueWriting.setTextColor(primaryTextColor);
		
		LinearLayout cardContinue = findViewById(R.id.cardContinue);
		if (cardContinue != null) {
			android.graphics.drawable.GradientDrawable continueBg = new android.graphics.drawable.GradientDrawable();
			continueBg.setColor(surfaceColor);
			continueBg.setCornerRadius(32f);
			cardContinue.setBackground(continueBg);
		}
		TextView textview25 = findViewById(R.id.textview25);
		TextView textview26 = findViewById(R.id.textview26);
		if (textview25 != null) textview25.setTextColor(primaryTextColor);
		if (textview26 != null) textview26.setTextColor(secondaryTextColor);
		
		// ৪. বটম নেভিগেশন বার থিম
		if (bottomNavigation != null) {
			bottomNavigation.setBackgroundColor(surfaceColor);
			int[][] navStates = new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} };
			int[] navColors = new int[] { accentColor, secondaryTextColor };
			android.content.res.ColorStateList navColorList = new android.content.res.ColorStateList(navStates, navColors);
			bottomNavigation.setItemIconTintList(navColorList);
			bottomNavigation.setItemTextColor(navColorList);
		}
		
		// ৫. অন্যান্য টুলবার এবং টেক্সট (Notepad, Drafts, Vault, Word Counter)
		View toolbarNotepad = findViewById(R.id.toolbarNotepad);
		View toolbarDrafts = findViewById(R.id.toolbarDrafts);
		View toolbarVault = findViewById(R.id.toolbarVault);
		View linear235 = findViewById(R.id.linear235);
		View linear238 = findViewById(R.id.linear238);
		
		if(toolbarNotepad != null) toolbarNotepad.setBackgroundColor(surfaceColor);
		if(toolbarDrafts != null) toolbarDrafts.setBackgroundColor(surfaceColor);
		if(toolbarVault != null) toolbarVault.setBackgroundColor(surfaceColor);
		if(linear235 != null) linear235.setBackgroundColor(surfaceColor);
		if(linear238 != null) linear238.setBackgroundColor(surfaceColor);
		
		if(tvNotepadTitle != null) tvNotepadTitle.setTextColor(primaryTextColor);
		TextView textview227 = findViewById(R.id.textview227); // Drafts Title
		TextView textview229 = findViewById(R.id.textview229); // Vault Title
		if(textview227 != null) textview227.setTextColor(primaryTextColor);
		if(textview229 != null) textview229.setTextColor(accentColor);
		
		if(etSearchNotes != null) {
			etSearchNotes.setTextColor(primaryTextColor);
			etSearchNotes.setHintTextColor(secondaryTextColor);
		}
		if(etInputText != null) {
			etInputText.setTextColor(primaryTextColor);
			etInputText.setHintTextColor(secondaryTextColor);
		}
		
		if(etNoteTitleBig != null) {
			etNoteTitleBig.setTextColor(primaryTextColor);
			etNoteTitleBig.setHintTextColor(secondaryTextColor);
		}
		
		if(tvNoNotesText != null) tvNoNotesText.setTextColor(secondaryTextColor);
		TextView textview228 = findViewById(R.id.textview228); // No drafts
		TextView textview230 = findViewById(R.id.textview230); // Vault empty
		if(textview228 != null) textview228.setTextColor(secondaryTextColor);
		if(textview230 != null) textview230.setTextColor(secondaryTextColor);
		
		if(tvTopWord != null) tvTopWord.setTextColor(primaryTextColor);
		if(tvTopChar != null) tvTopChar.setTextColor(primaryTextColor);
		if(tvTopBytes != null) tvTopBytes.setTextColor(primaryTextColor);
		
		TextView textview224 = findViewById(R.id.textview224); // Words label
		TextView textview225 = findViewById(R.id.textview225); // Chars label
		TextView textview226 = findViewById(R.id.textview226); // Bytes label
		if(textview224 != null) textview224.setTextColor(secondaryTextColor);
		if(textview225 != null) textview225.setTextColor(secondaryTextColor);
		if(textview226 != null) textview226.setTextColor(secondaryTextColor);
		
		// ৬. আইকন কালার
		if(btnSearch != null) btnSearch.setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
		if(btnExitVault != null) btnExitVault.setColorFilter(android.graphics.Color.parseColor("#E53935"), android.graphics.PorterDuff.Mode.SRC_IN); 
		
		ImageView imageview92 = findViewById(R.id.imageview92);
		ImageView imageview90 = findViewById(R.id.imageview90);
		ImageView imageview91 = findViewById(R.id.imageview91);
		ImageView imageview93 = findViewById(R.id.imageview93);
		if(imageview92 != null) imageview92.setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
		if(imageview90 != null) imageview90.setColorFilter(secondaryTextColor, android.graphics.PorterDuff.Mode.SRC_IN);
		if(imageview91 != null) imageview91.setColorFilter(secondaryTextColor, android.graphics.PorterDuff.Mode.SRC_IN);
		if(imageview93 != null) imageview93.setColorFilter(secondaryTextColor, android.graphics.PorterDuff.Mode.SRC_IN);
		
		if(wcBtnBack != null) wcBtnBack.setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
		if(wcBtnUndo != null) wcBtnUndo.setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
		if(wcBtnRedo != null) wcBtnRedo.setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
		if(wcBtnDetails != null) wcBtnDetails.setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
		if(wcBtnMore != null) wcBtnMore.setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
		if(wcBtnSave != null) wcBtnSave.setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN); 
		if(wcBtnEdit != null) wcBtnEdit.setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
		if(wcBtnConvert != null) wcBtnConvert.setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
		
		// ৭. লিস্ট অ্যাডাপ্টার রিফ্রেশ করা
		if (noteAdapter != null) noteAdapter.notifyDataSetChanged();
		if (draftAdapter != null) draftAdapter.notifyDataSetChanged();
		if (vaultAdapter != null) vaultAdapter.notifyDataSetChanged();
		
		// স্ট্যাটাস বার কালার চেঞ্জ (যদি API 21+ হয়)
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
			getWindow().setStatusBarColor(bgColor);
		}
		
		// 🌟 ৮. পুরো স্ক্রিনের সব টেক্সটে একসাথে ফন্ট বসিয়ে দেওয়ার ম্যাজিক! 🌟
		ThemeHelper.applyFontToAllViews(this, getWindow().getDecorView());
		
		
		// ==========================================
		// 🌟 প্রফেশনাল স্টার্টআপ সিঙ্ক অ্যানিমেশন (Floating Sync Banner) 🌟
		// ==========================================
		
		
	}
	
	
	// ==========================================
	// Vault & Editor Settings 
	// ==========================================
	private void showVaultPinSetupDialog() {
		final Dialog dialog = new Dialog(this); dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		LinearLayout rootLayout = new LinearLayout(this); rootLayout.setOrientation(LinearLayout.VERTICAL); rootLayout.setPadding(64, 56, 64, 64);
		GradientDrawable shape = new GradientDrawable(); shape.setCornerRadius(32f); shape.setColor(surfaceColor); rootLayout.setBackground(shape);
		TextView titleView = new TextView(this); titleView.setText("হিডেন নোটস সেটআপ"); titleView.setTextColor(accentColor); titleView.setTextSize(20f); titleView.setTypeface(currentTypeface, Typeface.BOLD); titleView.setPadding(0, 0, 0, 32); rootLayout.addView(titleView);
		final EditText etPin = new EditText(this); etPin.setHint("নতুন ৪-ডিজিটের পিন দিন"); etPin.setHintTextColor(secondaryTextColor); etPin.setTextColor(primaryTextColor); etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD); rootLayout.addView(etPin);
		LinearLayout btnLayout = new LinearLayout(this); btnLayout.setOrientation(LinearLayout.HORIZONTAL); btnLayout.setGravity(Gravity.END); btnLayout.setPadding(0, 48, 0, 0);
		TextView btnCancel = new TextView(this); btnCancel.setText("বাতিল"); btnCancel.setTextColor(secondaryTextColor); btnCancel.setPadding(32, 16, 32, 16); btnCancel.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); } });
		TextView btnSave = new TextView(this); btnSave.setText("সেভ করুন"); btnSave.setTextColor(accentColor); btnSave.setTypeface(currentTypeface, Typeface.BOLD); btnSave.setPadding(32, 16, 16, 16);
		btnSave.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				String pin = etPin.getText().toString().trim();
				if(pin.length() >= 4) { appSettings.edit().putString("vault_pin", pin).apply(); Toast.makeText(MmmActivity.this, "ভল্ট পিন সেভ হয়েছে!", Toast.LENGTH_SHORT).show(); dialog.dismiss(); showVaultLoginPopup(pin); }
				else { Toast.makeText(MmmActivity.this, "অন্তত ৪ ডিজিটের পিন দিন!", Toast.LENGTH_SHORT).show(); }
			}
		});
		btnLayout.addView(btnCancel); btnLayout.addView(btnSave); rootLayout.addView(btnLayout);
		dialog.setContentView(rootLayout, new ViewGroup.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT)); dialog.show();
	}
	
	private void showVaultLoginPopup(final String savedPin) {
		final Dialog dialog = new Dialog(this); dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		LinearLayout rootLayout = new LinearLayout(this); rootLayout.setOrientation(LinearLayout.VERTICAL); rootLayout.setPadding(64, 64, 64, 64); rootLayout.setGravity(Gravity.CENTER);
		GradientDrawable shape = new GradientDrawable(); shape.setCornerRadius(32f); shape.setColor(surfaceColor); rootLayout.setBackground(shape);
		ImageView icon = new ImageView(this); icon.setImageResource(android.R.drawable.ic_secure); icon.setColorFilter(accentColor); rootLayout.addView(icon, new LinearLayout.LayoutParams(120, 120));
		TextView titleView = new TextView(this); titleView.setText("সিক্রেট ভল্ট"); titleView.setTextColor(primaryTextColor); titleView.setTextSize(22f); titleView.setTypeface(currentTypeface, Typeface.BOLD); titleView.setPadding(0, 24, 0, 32); rootLayout.addView(titleView);
		final EditText etPin = new EditText(this); etPin.setHint("পিন দিন"); etPin.setHintTextColor(secondaryTextColor); etPin.setTextColor(primaryTextColor); etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD); etPin.setGravity(Gravity.CENTER); rootLayout.addView(etPin, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		TextView btnUnlock = new TextView(this); btnUnlock.setText("আনলক করুন"); btnUnlock.setTextColor(accentColor); btnUnlock.setTextSize(18f); btnUnlock.setTypeface(currentTypeface, Typeface.BOLD); btnUnlock.setPadding(0, 48, 0, 16);
		btnUnlock.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				if (etPin.getText().toString().equals(savedPin)) { dialog.dismiss(); layoutNotepad.setVisibility(View.GONE); bottomNavigation.setVisibility(View.GONE); layoutVault.setVisibility(View.VISIBLE); } 
				else { Toast.makeText(MmmActivity.this, "ভুল পিন!", Toast.LENGTH_SHORT).show(); etPin.setText(""); }
			}
		});
		rootLayout.addView(btnUnlock);
		LinearLayout settingsLayout = new LinearLayout(this); settingsLayout.setOrientation(LinearLayout.HORIZONTAL); settingsLayout.setGravity(Gravity.CENTER); settingsLayout.setPadding(0, 32, 0, 0);
		ImageView sIcon = new ImageView(this); sIcon.setImageResource(android.R.drawable.ic_menu_preferences); sIcon.setColorFilter(secondaryTextColor); settingsLayout.addView(sIcon, new LinearLayout.LayoutParams(40, 40));
		TextView btnSettings = new TextView(this); btnSettings.setText(" ভল্ট সেটিংস"); btnSettings.setTextColor(secondaryTextColor); btnSettings.setTextSize(14f);
		settingsLayout.addView(btnSettings);
		settingsLayout.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); showVaultSettingsMenu(savedPin); } });
		rootLayout.addView(settingsLayout);
		dialog.setContentView(rootLayout, new ViewGroup.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels * 0.85), ViewGroup.LayoutParams.WRAP_CONTENT)); dialog.show();
	}
	
	private void showVaultSettingsMenu(final String savedPin) {
		final BottomSheetDialog sheet = new BottomSheetDialog(this);
		LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(surfaceColor); root.setPadding(0, 32, 0, 32);
		TextView title = new TextView(this); title.setText("ভল্ট সেটিংস"); title.setTextColor(accentColor); title.setTextSize(20f); title.setTypeface(currentTypeface, Typeface.BOLD); title.setPadding(64, 32, 64, 48); root.addView(title);
		root.addView(createMenuItem("পাসওয়ার্ড পরিবর্তন করুন", android.R.drawable.ic_menu_edit, new View.OnClickListener() { @Override public void onClick(View v) { sheet.dismiss(); showOldPasswordDialogForChange(savedPin); } }));
		root.addView(createMenuItem("হিডেন নোট ধ্বংস করুন", android.R.drawable.ic_menu_delete, new View.OnClickListener() { @Override public void onClick(View v) { sheet.dismiss(); showDestroyConfirmationDialog(savedPin); } }));
		sheet.setContentView(root); sheet.show();
	}
	
	private void showOldPasswordDialogForChange(final String savedPin) {
		final Dialog dialog = new Dialog(this); dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		LinearLayout rootLayout = new LinearLayout(this); rootLayout.setOrientation(LinearLayout.VERTICAL); rootLayout.setPadding(64, 56, 64, 64);
		GradientDrawable shape = new GradientDrawable(); shape.setCornerRadius(32f); shape.setColor(surfaceColor); rootLayout.setBackground(shape);
		TextView titleView = new TextView(this); titleView.setText("নিরাপত্তা যাচাই"); titleView.setTextColor(accentColor); titleView.setTextSize(20f); titleView.setTypeface(currentTypeface, Typeface.BOLD); titleView.setPadding(0, 0, 0, 32); rootLayout.addView(titleView);
		TextView msgView = new TextView(this); msgView.setText("পাসওয়ার্ড পরিবর্তন করতে আপনার পুরাতন পিন দিন:"); msgView.setTextColor(primaryTextColor); msgView.setPadding(0, 0, 0, 16); rootLayout.addView(msgView);
		final EditText etPin = new EditText(this); etPin.setHint("পুরাতন পিন"); etPin.setHintTextColor(secondaryTextColor); etPin.setTextColor(primaryTextColor); etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD); rootLayout.addView(etPin);
		LinearLayout btnLayout = new LinearLayout(this); btnLayout.setOrientation(LinearLayout.HORIZONTAL); btnLayout.setGravity(Gravity.END); btnLayout.setPadding(0, 48, 0, 0);
		TextView btnCancel = new TextView(this); btnCancel.setText("বাতিল"); btnCancel.setTextColor(secondaryTextColor); btnCancel.setPadding(32, 16, 32, 16); btnCancel.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); } });
		TextView btnNext = new TextView(this); btnNext.setText("পরবর্তী"); btnNext.setTextColor(accentColor); btnNext.setTypeface(currentTypeface, Typeface.BOLD); btnNext.setPadding(32, 16, 16, 16);
		btnNext.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				if(etPin.getText().toString().equals(savedPin)) { dialog.dismiss(); showVaultPinSetupDialog(); }
				else { Toast.makeText(MmmActivity.this, "পুরাতন পিন ভুল হয়েছে!", Toast.LENGTH_SHORT).show(); }
			}
		});
		btnLayout.addView(btnCancel); btnLayout.addView(btnNext); rootLayout.addView(btnLayout);
		dialog.setContentView(rootLayout, new ViewGroup.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT)); dialog.show();
	}
	
	private void showDestroyConfirmationDialog(final String savedPin) {
		final Dialog dialog = new Dialog(this); dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		LinearLayout rootLayout = new LinearLayout(this); rootLayout.setOrientation(LinearLayout.VERTICAL); rootLayout.setPadding(64, 56, 64, 64);
		GradientDrawable shape = new GradientDrawable(); shape.setCornerRadius(32f); shape.setColor(surfaceColor); rootLayout.setBackground(shape);
		TextView titleView = new TextView(this); titleView.setText("সতর্কতা!"); titleView.setTextColor(Color.parseColor("#E53935")); titleView.setTextSize(20f); titleView.setTypeface(currentTypeface, Typeface.BOLD); rootLayout.addView(titleView);
		TextView msgView = new TextView(this); msgView.setText("\nআপনি কি নিশ্চিত? আপনার সব হিডেন নোটস স্থায়ীভাবে ডিলিট হয়ে যাবে। নিশ্চিত করতে আপনার ভল্ট পিন দিন:"); msgView.setTextColor(primaryTextColor); msgView.setPadding(0, 0, 0, 16); rootLayout.addView(msgView);
		final EditText etPin = new EditText(this); etPin.setHint("পিন দিন"); etPin.setHintTextColor(secondaryTextColor); etPin.setTextColor(primaryTextColor); etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD); rootLayout.addView(etPin);
		LinearLayout btnLayout = new LinearLayout(this); btnLayout.setOrientation(LinearLayout.HORIZONTAL); btnLayout.setGravity(Gravity.END); btnLayout.setPadding(0, 48, 0, 0);
		TextView btnCancel = new TextView(this); btnCancel.setText("বাতিল"); btnCancel.setTextColor(secondaryTextColor); btnCancel.setPadding(32, 16, 32, 16); btnCancel.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); } });
		TextView btnDestroy = new TextView(this); btnDestroy.setText("DESTROY"); btnDestroy.setTextColor(Color.parseColor("#E53935")); btnDestroy.setTypeface(currentTypeface, Typeface.BOLD); btnDestroy.setPadding(32, 16, 16, 16);
		btnDestroy.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				if(etPin.getText().toString().equals(savedPin)) {
					dbHelper.destroyVault(); loadNotesFromLocalDB(); 
					layoutVault.setVisibility(View.GONE); layoutNotepad.setVisibility(View.VISIBLE); bottomNavigation.setVisibility(View.VISIBLE);
					Toast.makeText(MmmActivity.this, "সব হিডেন নোটস ধ্বংস করা হয়েছে!", Toast.LENGTH_LONG).show(); dialog.dismiss();
				} else { Toast.makeText(MmmActivity.this, "ভুল পিন!", Toast.LENGTH_SHORT).show(); }
			}
		});
		btnLayout.addView(btnCancel); btnLayout.addView(btnDestroy); rootLayout.addView(btnLayout);
		dialog.setContentView(rootLayout, new ViewGroup.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT)); dialog.show();
	}
	
	// ==========================================
	// 🌟 Internal Hidden Storage Systems 🌟
	// ==========================================
	private File getBasePrivateDir() {
		File dir = new File(getFilesDir(), "TunePad_Data");
		if (!dir.exists()) dir.mkdirs();
		return dir;
	}
	
	private File getProjectsDir() {
		File dir = new File(getBasePrivateDir(), "Projects");
		if (!dir.exists()) dir.mkdirs();
		return dir;
	}
	
	private File getFoldersDir() {
		File dir = new File(getBasePrivateDir(), "Folders");
		if (!dir.exists()) dir.mkdirs();
		return dir;
	}
	
	private List<String> getAllProjectNames() {
		List<String> list = new ArrayList<>();
		File projDir = getProjectsDir();
		File[] categories = projDir.listFiles();
		if (categories != null) {
			for (File cat : categories) {
				if (cat.isDirectory()) {
					File[] projects = cat.listFiles();
					if (projects != null) {
						for (File proj : projects) {
							if (proj.isDirectory()) {
								list.add(proj.getName() + " (" + cat.getName() + ")");
							}
						}
					}
				}
			}
		}
		return list;
	}
	
	private List<String> getAllFolderNames() {
		List<String> list = new ArrayList<>();
		File fDir = getFoldersDir();
		File[] folders = fDir.listFiles();
		if (folders != null) {
			for (File f : folders) {
				if (f.isDirectory()) {
					list.add(f.getName());
				}
			}
		}
		return list;
	}
	
	
	
// ==========================================
// 🌟 প্রো-লেভেল অপটিমাইজড: হোম প্রজেক্ট লোডার (ডুপ্লিকেট বাগ ফিক্সড!) 🌟
// ==========================================
private void refreshHomeProjects() {
    if (projectsContainer == null) return;

    // সব প্রজেক্টের লিস্ট তৈরি করা
    final java.util.List<File> allProjectsList = new java.util.ArrayList<>();
    File projDir = getProjectsDir();
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

    // মডিফাই করা ফাইল সবার উপরে দেখানোর লজিক
    java.util.Collections.sort(allProjectsList, new java.util.Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            return Long.compare(f2.lastModified(), f1.lastModified());
        }
    });

    final int totalProjects = allProjectsList.size();

    if (totalProjects == 0) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                projectsContainer.removeAllViews(); 
                projectsContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
                if (tvEmptyProjects.getParent() != null) ((android.view.ViewGroup)tvEmptyProjects.getParent()).removeView(tvEmptyProjects);
                projectsContainer.addView(tvEmptyProjects);
                tvEmptyProjects.setVisibility(android.view.View.VISIBLE);
                addSeeAllButtonToContainer(totalProjects); 
            }
        });
    } else {
        tvEmptyProjects.setVisibility(android.view.View.GONE);
        int displayLimit = 6;
        final int itemsToShow = Math.min(totalProjects, displayLimit);

        // মেইন থ্রেডকে ফ্রি রাখতে ফাইল রিড করার কাজ ব্যাকগ্রাউন্ডে দেওয়া হলো
        new Thread(new Runnable() {
            @Override
            public void run() {
                // ব্যাকগ্রাউন্ডে শুধু ডেটা ক্যালকুলেট হবে
                final java.util.List<int[]> statsList = new java.util.ArrayList<>();

                for (int i = 0; i < itemsToShow; i++) {
                    File proj = allProjectsList.get(i);
                    int fileCount = 0;
                    int totalWords = 0;
                    File[] files = proj.listFiles();
                    if (files != null) {
                        for(File f : files) {
                            if (f.isFile() && f.getName().endsWith(".tpad")) {
                                fileCount++;
                                totalWords += countWordsInFile(f); 
                            }
                        }
                    }
                    statsList.add(new int[]{fileCount, totalWords});
                }

                // 🌟 ক্যালকুলেশন শেষ, এবার UI আপডেট করার জন্য মেইন থ্রেডে ফেরা হলো 🌟
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 🌟 ম্যাজিক ফিক্স: ঠিক কার্ড বসানোর আগ মুহূর্তে কন্টেইনার খালি করা হলো 🌟
                        projectsContainer.removeAllViews();
                        projectsContainer.setOrientation(android.widget.LinearLayout.VERTICAL);

                        android.widget.LinearLayout currentRow = null;
                        for (int i = 0; i < itemsToShow; i++) {
                            if (i % 3 == 0) {
                                currentRow = new android.widget.LinearLayout(MmmActivity.this);
                                currentRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                                currentRow.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
                                currentRow.setWeightSum(3f);
                                projectsContainer.addView(currentRow);
                            }

                            File proj = allProjectsList.get(i);
                            int fileCount = statsList.get(i)[0];
                            int totalWords = statsList.get(i)[1];

                            android.view.View card = createDynamicProjectCard(proj.getName(), proj.getParentFile().getName(), fileCount, totalWords, proj);
                            currentRow.addView(card);
                        }

                        // ব্যালেন্সিং লজিক
                        if (itemsToShow % 3 != 0) {
                            int emptySlots = 3 - (itemsToShow % 3);
                            for (int e = 0; e < emptySlots; e++) {
                                android.view.View emptyView = new android.view.View(MmmActivity.this);
                                android.widget.LinearLayout.LayoutParams emptyParams = new android.widget.LinearLayout.LayoutParams(0, 1, 1.0f);
                                emptyParams.setMargins(12, 12, 12, 24);
                                emptyView.setLayoutParams(emptyParams);
                                currentRow.addView(emptyView);
                            }
                        }

                        addSeeAllButtonToContainer(totalProjects);
                    }
                });
            }
        }).start();
    }
}

// 🌟 হেল্পার মেথড: ইমপোর্ট এবং ব্যাকআপ বাটন তৈরির জন্য 🌟
private void addSeeAllButtonToContainer(int totalProjects) {
    android.widget.TextView btnSeeAllAndImport = new android.widget.TextView(MmmActivity.this);
    
    if (totalProjects > 6) {
        btnSeeAllAndImport.setText("সব প্রজেক্ট ও ইমপোর্ট/ব্যাকআপ 📦");
    } else if (totalProjects > 0) {
        btnSeeAllAndImport.setText("প্রজেক্ট ইমপোর্ট ও ব্যাকআপ 📦");
    } else {
        btnSeeAllAndImport.setText("পুরোনো প্রজেক্ট ইমপোর্ট করুন 📥"); 
    }
    
    btnSeeAllAndImport.setTextColor(bgColor);
    btnSeeAllAndImport.setTextSize(15f);
    btnSeeAllAndImport.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
    btnSeeAllAndImport.setGravity(android.view.Gravity.CENTER);
    btnSeeAllAndImport.setPadding(0, 32, 0, 32);
    
    android.widget.LinearLayout.LayoutParams btnParams = new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    btnParams.setMargins(32, 24, 32, 48);
    btnSeeAllAndImport.setLayoutParams(btnParams);
    
    android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
    btnBg.setColor(accentColor); 
    btnBg.setCornerRadius(100f);
    btnSeeAllAndImport.setBackground(btnBg);
    
    btnSeeAllAndImport.setOnClickListener(new android.view.View.OnClickListener() {
        @Override
        public void onClick(android.view.View v) {
            android.content.Intent intent = new android.content.Intent(MmmActivity.this, AllProjectsActivity.class);
            startActivity(intent);
        }
    });
    
    projectsContainer.addView(btnSeeAllAndImport);
}

	
	
	
	// ==========================================
	// 🌟 গ্রিড ভিউ প্রজেক্ট কার্ড (প্রতি লাইনে ৩টি) 🌟
	// ==========================================
	private View createDynamicProjectCard(final String title, final String category, int count, int words, final File projDir) {
		RelativeLayout card = new RelativeLayout(this);
		
		// 🌟 ম্যাজিক: Width 0 এবং Weight 1.0f দেওয়া হলো যাতে ৩টা কার্ড সমান জায়গা নেয় 🌟
		int cardHeight = (int) (getResources().getDisplayMetrics().density * 155); 
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, cardHeight, 1.0f);
		params.setMargins(12, 12, 12, 24); // চারদিকে সমান গ্যাপ
		card.setLayoutParams(params);
		
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(Color.parseColor("#2A3439")); 
		bg.setCornerRadius(24f); // কোণা একটু ছোট করা হলো
		card.setBackground(bg);
		card.setElevation(6f);
		card.setClipToOutline(true);
		
		// লেয়ার ১: ফুল সাইজ কভার ইমেজ
		ImageView coverImg = new ImageView(this);
		coverImg.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		coverImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
		File coverFile = new File(projDir, "cover.jpg");
		if(coverFile.exists()) {
			safeLoadImageToView(coverFile, coverImg);
		} else {
			coverImg.setImageResource(android.R.drawable.ic_menu_gallery);
			coverImg.setColorFilter(Color.parseColor("#9CA8AE"));
			coverImg.setScaleType(ImageView.ScaleType.CENTER);
		}
		card.addView(coverImg);
		
		// লেয়ার ২: ডার্ক শ্যাডো (Gradient)
		LinearLayout textLayout = new LinearLayout(this);
		textLayout.setOrientation(LinearLayout.VERTICAL);
		RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		textParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		textLayout.setLayoutParams(textParams);
		
		GradientDrawable gradientBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.TRANSPARENT, Color.parseColor("#CC000000"), Color.parseColor("#E6000000")});
		textLayout.setBackground(gradientBg);
		textLayout.setPadding(16, 48, 16, 16); 
		
		// টাইটেল (ছোট সাইজ)
		TextView tvTitle = new TextView(this);
		tvTitle.setText(title);
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(13f); // ফন্ট সাইজ কমানো হয়েছে
		tvTitle.setTypeface(currentTypeface, Typeface.BOLD);
		tvTitle.setMaxLines(2);
		tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
		tvTitle.setShadowLayer(4f, 0f, 2f, Color.BLACK);
		textLayout.addView(tvTitle);
		
		// ক্যাটাগরি
		TextView tvCategory = new TextView(this);
		tvCategory.setText(category);
		tvCategory.setTextColor(Color.parseColor("#e83c91")); 
		tvCategory.setTextSize(10f); // ছোট সাইজ
		tvCategory.setTypeface(currentTypeface, Typeface.BOLD);
		tvCategory.setPadding(0, 4, 0, 2);
		textLayout.addView(tvCategory);
		
		// সাবটাইটেল
		TextView tvSubtitle = new TextView(this);
		tvSubtitle.setText(count + " পর্ব");
		tvSubtitle.setTextColor(Color.parseColor("#DDDDDD"));
		tvSubtitle.setTextSize(9f);
		textLayout.addView(tvSubtitle);
		
		card.addView(textLayout);
		
		// লেয়ার ৩: থ্রি-ডট আইকন (ছোট সাইজ)
		TextView btnMore = new TextView(this);
		btnMore.setText("⋮"); 
		btnMore.setTextColor(Color.WHITE);
		btnMore.setTextSize(20f);
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
		
		// ক্লিক লিসেনার্স
		card.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				Intent intent = new Intent(MmmActivity.this, ProjectViewActivity.class);
				intent.putExtra("PROJECT_NAME", title);
				intent.putExtra("CATEGORY_NAME", category);
				startActivity(intent);
			}
		});
		
		btnMore.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				MmmActivity.this.showProjectOptionsBottomSheet(projDir, title, category);
			}
		});
		
		return card;
	}
	
	
	
	// ==========================================
	// 🌟 New Project Creation (100% Duplicate Free) 🌟
	// ==========================================
	private void showNewProjectBottomSheet() {
		final BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
		LinearLayout rootLayout = new LinearLayout(this);
		rootLayout.setOrientation(LinearLayout.VERTICAL);
		rootLayout.setBackgroundColor(surfaceColor);
		rootLayout.setPadding(64, 56, 64, 64);
		
		TextView titleView = new TextView(this);
		titleView.setText("নতুন প্রজেক্ট তৈরি করুন");
		titleView.setTextColor(primaryTextColor);
		titleView.setTextSize(22f);
		titleView.setTypeface(currentTypeface, Typeface.BOLD);
		titleView.setPadding(0, 0, 0, 48);
		rootLayout.addView(titleView);
		
		GradientDrawable inputBg = new GradientDrawable();
		inputBg.setColor(bgColor); 
		inputBg.setCornerRadius(32f); 
		
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, 0, 32);
		
		final EditText etProjectName = new EditText(this);
		etProjectName.setHint("প্রজেক্টের নাম দিন...");
		etProjectName.setHintTextColor(secondaryTextColor);
		etProjectName.setTextColor(primaryTextColor);
		etProjectName.setTypeface(currentTypeface);
		etProjectName.setPadding(48, 40, 48, 40);
		etProjectName.setBackground(inputBg);
		etProjectName.setLayoutParams(params);
		rootLayout.addView(etProjectName);
		
		TextView tvCategoryLabel = new TextView(this);
		tvCategoryLabel.setText("ক্যাটাগরি নির্বাচন করুন:");
		tvCategoryLabel.setTextColor(secondaryTextColor);
		tvCategoryLabel.setTextSize(14f);
		tvCategoryLabel.setTypeface(currentTypeface);
		tvCategoryLabel.setPadding(16, 0, 0, 16);
		rootLayout.addView(tvCategoryLabel);
		
		final RadioGroup categoryGroup = new RadioGroup(this);
		categoryGroup.setOrientation(LinearLayout.VERTICAL);
		categoryGroup.setPadding(0, 0, 0, 32);
		
		final RadioButton rbStory = new RadioButton(this); rbStory.setText(" গল্প/উপন্যাস"); rbStory.setTextColor(primaryTextColor); rbStory.setId(View.generateViewId());
		final RadioButton rbPoetry = new RadioButton(this); rbPoetry.setText(" কাব্যগ্রন্থ"); rbPoetry.setTextColor(primaryTextColor); rbPoetry.setId(View.generateViewId());
		final RadioButton rbCustom = new RadioButton(this); rbCustom.setText(" + নতুন ক্যাটাগরি"); rbCustom.setTextColor(accentColor); rbCustom.setId(View.generateViewId());
		
		categoryGroup.addView(rbStory); categoryGroup.addView(rbPoetry); categoryGroup.addView(rbCustom);
		rootLayout.addView(categoryGroup);
		
		final EditText etCustomCategory = new EditText(this);
		etCustomCategory.setHint("ক্যাটাগরির নাম লিখুন...");
		etCustomCategory.setHintTextColor(secondaryTextColor);
		etCustomCategory.setTextColor(primaryTextColor);
		etCustomCategory.setPadding(48, 40, 48, 40);
		etCustomCategory.setBackground(inputBg);
		etCustomCategory.setLayoutParams(params);
		etCustomCategory.setVisibility(View.GONE);
		rootLayout.addView(etCustomCategory);
		
		categoryGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId == rbCustom.getId()) etCustomCategory.setVisibility(View.VISIBLE);
				else etCustomCategory.setVisibility(View.GONE);
			}
		});
		rbStory.setChecked(true);
		
		TextView btnCreate = new TextView(this);
		btnCreate.setText("প্রজেক্ট তৈরি করুন");
		btnCreate.setTextColor(Color.parseColor("#121212")); 
		btnCreate.setTextSize(18f);
		btnCreate.setGravity(Gravity.CENTER);
		btnCreate.setTypeface(currentTypeface, Typeface.BOLD);
		btnCreate.setPadding(0, 40, 0, 40);
		
		GradientDrawable btnBg = new GradientDrawable();
		btnBg.setColor(accentColor);
		btnBg.setCornerRadius(100f);
		btnCreate.setBackground(btnBg);
		
		btnCreate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String rawProjectName = etProjectName.getText().toString().trim();
				if (rawProjectName.isEmpty()) { Toast.makeText(MmmActivity.this, "প্রজেক্টের নাম দিন!", Toast.LENGTH_SHORT).show(); return; }
				
				// 🌟 ফিক্সড ১: নাম থেকে স্পেশাল ক্যারেক্টার বাদ দেওয়া (যাতে ফোল্ডার তৈরি হতে এরর না দেয়) 🌟
				String safeProjectName = rawProjectName.replaceAll("[^a-zA-Z0-9_\\-\\u0980-\\u09FF ]", "").trim();
				if (safeProjectName.isEmpty()) { Toast.makeText(MmmActivity.this, "সঠিক নাম দিন!", Toast.LENGTH_SHORT).show(); return; }
				
				String categoryFolder = "গল্প_উপন্যাস";
				if (rbPoetry.isChecked()) categoryFolder = "কাব্যগ্রন্থ";
				else if (rbCustom.isChecked()) {
					String rawCategory = etCustomCategory.getText().toString().trim();
					categoryFolder = rawCategory.replaceAll("[^a-zA-Z0-9_\\-\\u0980-\\u09FF ]", "").trim();
					if(categoryFolder.isEmpty()) { Toast.makeText(MmmActivity.this, "ক্যাটাগরির সঠিক নাম দিন!", Toast.LENGTH_SHORT).show(); return; }
				}
				
				// 🚨 ফিক্সড ২: গ্লোবাল ডুপ্লিকেট চেকিং (সব ক্যাটাগরি জুড়ে) 🚨
				boolean isDuplicate = false;
				File projDir = getProjectsDir();
				File[] categories = projDir.listFiles();
				if (categories != null) {
					for (File cat : categories) {
						if (cat.isDirectory()) {
							File checkDuplicate = new File(cat, safeProjectName);
							if (checkDuplicate.exists()) {
								isDuplicate = true;
								break;
							}
						}
					}
				}
				
				if (isDuplicate) {
					// 🌟 আপনার সিগনেচার টোস্ট কল করা হলো 🌟
					showCustomToastSheet("এই নামে একটি প্রজেক্ট আগে থেকেই আছে! অন্য নাম দিন।");
					return; // কোড এখানেই থেমে যাবে, ডুপ্লিকেট হবে না!
				}
				
				// ✅ যদি প্রজেক্ট না থাকে, তবেই নির্দিষ্ট ক্যাটাগরিতে নতুন ফোল্ডার তৈরি করবে
				File finalProjectDir = new File(new File(getProjectsDir(), categoryFolder), safeProjectName);
				finalProjectDir.mkdirs();
				
				Toast.makeText(MmmActivity.this, "প্রজেক্ট তৈরি হয়েছে!", Toast.LENGTH_SHORT).show();
				refreshHomeProjects();
				bottomSheet.dismiss();
			}
		});
		
		rootLayout.addView(btnCreate);
		bottomSheet.setContentView(rootLayout);
		bottomSheet.show();
	}
	
	
	// ==========================================
	// 🌟 Dynamic Save Dialog (ID Fix সহ) 🌟
	// ==========================================
	private void showCustomSaveDialog(final String content) {
		final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
		bottomSheet.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		
		android.widget.LinearLayout rootLayout = new android.widget.LinearLayout(this);
		rootLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
		rootLayout.setBackgroundColor(surfaceColor);
		rootLayout.setPadding(64, 56, 64, 64);
		
		android.widget.TextView titleView = new android.widget.TextView(this);
		titleView.setText("নোট সেভ করুন");
		titleView.setTextColor(primaryTextColor);
		titleView.setTextSize(22f);
		titleView.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		titleView.setPadding(0, 0, 0, 32);
		rootLayout.addView(titleView);
		
		android.graphics.drawable.GradientDrawable inputBg = new android.graphics.drawable.GradientDrawable(); 
		inputBg.setColor(bgColor); 
		inputBg.setCornerRadius(32f); 
		android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, 0, 24);
		
		final android.widget.EditText etTitle = new android.widget.EditText(this);
		etTitle.setHint("শিরোনাম বা পর্বের নাম দিন...");
		etTitle.setHintTextColor(secondaryTextColor);
		etTitle.setTextColor(primaryTextColor);
		etTitle.setTypeface(currentTypeface);
		etTitle.setPadding(48, 40, 48, 40);
		etTitle.setBackground(inputBg);
		etTitle.setLayoutParams(params);
		rootLayout.addView(etTitle);
		
		if (etNoteTitleBig != null) {
			String currentBigTitle = etNoteTitleBig.getText().toString().trim();
			if (currentLabel != null && currentLabel.startsWith("Project: ") && currentBigTitle.contains(" > ")) {
				String[] parts = currentBigTitle.split(" > ", 2);
				if (parts.length > 1) {
					currentBigTitle = parts[1].trim();
				}
			}
			if (!currentBigTitle.isEmpty()) {
				etTitle.setText(currentBigTitle);
			} else {
				etTitle.setText(currentTitle);
			}
		} else {
			etTitle.setText(currentTitle);
		}
		
		final android.widget.RadioGroup typeGroup = new android.widget.RadioGroup(this);
		typeGroup.setOrientation(android.widget.LinearLayout.HORIZONTAL);
		typeGroup.setPadding(0, 0, 0, 24);
		
		final android.widget.RadioButton rbFolder = new android.widget.RadioButton(this); rbFolder.setText(" ফোল্ডারে সেভ "); rbFolder.setTextColor(primaryTextColor); rbFolder.setId(android.view.View.generateViewId());
		final android.widget.RadioButton rbProject = new android.widget.RadioButton(this); rbProject.setText(" প্রজেক্টে সেভ "); rbProject.setTextColor(primaryTextColor); rbProject.setId(android.view.View.generateViewId());
		typeGroup.addView(rbFolder); typeGroup.addView(rbProject);
		rootLayout.addView(typeGroup);
		
		final java.util.List<String> projectList = getAllProjectNames();
		
		final String[] selectedFolderPathArr = new String[]{getFoldersDir().getAbsolutePath()};
		String displayFolderPath = "মেইন ফোল্ডার";
		String displayProjectName = "";
		
		if (currentLabel != null && currentLabel.startsWith("Folder: ")) {
			String savedRelativePath = currentLabel.substring(8);
			java.io.File existingFolder = new java.io.File(getFoldersDir(), savedRelativePath);
			if (existingFolder.exists()) {
				selectedFolderPathArr[0] = existingFolder.getAbsolutePath();
				displayFolderPath = savedRelativePath.isEmpty() ? "মেইন ফোল্ডার" : savedRelativePath;
			}
		} else if (currentLabel != null && currentLabel.startsWith("Project: ")) {
			String exactProjName = currentLabel.substring(9);
			displayProjectName = exactProjName; 
			for (String pItem : projectList) {
				if (pItem.startsWith(exactProjName + " (")) {
					displayProjectName = pItem;
					break;
				}
			}
		}
		
		final android.widget.AutoCompleteTextView dropdownProject = new android.widget.AutoCompleteTextView(this);
		dropdownProject.setHint("প্রজেক্ট সিলেক্ট বা সার্চ করুন...");
		dropdownLocationSettings(dropdownProject, inputBg, params);
		dropdownProject.setDropDownHeight(600); 
		if (!displayProjectName.isEmpty()) dropdownProject.setText(displayProjectName, false);
		dropdownProject.setOnClickListener(new android.view.View.OnClickListener() { @Override public void onClick(android.view.View v) { dropdownProject.showDropDown(); } });
		dropdownProject.setOnFocusChangeListener(new android.view.View.OnFocusChangeListener() { @Override public void onFocusChange(android.view.View v, boolean hasFocus) { if (hasFocus) dropdownProject.showDropDown(); } });
		rootLayout.addView(dropdownProject);
		
		final android.widget.LinearLayout browseFolderLayout = new android.widget.LinearLayout(this);
		browseFolderLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL); browseFolderLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
		browseFolderLayout.setBackground(inputBg); browseFolderLayout.setLayoutParams(params); browseFolderLayout.setPadding(32, 16, 32, 16);
		
		final android.widget.TextView tvSelectedFolderPath = new android.widget.TextView(this);
		tvSelectedFolderPath.setText(displayFolderPath); tvSelectedFolderPath.setTextColor(primaryTextColor); tvSelectedFolderPath.setPadding(16, 0, 16, 0);
		tvSelectedFolderPath.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1));
		
		android.widget.TextView btnBrowse = new android.widget.TextView(this);
		btnBrowse.setText("ব্রাউজ"); btnBrowse.setTextColor(bgColor); btnBrowse.setPadding(40, 20, 40, 20); btnBrowse.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		android.graphics.drawable.GradientDrawable browseBg = new android.graphics.drawable.GradientDrawable(); browseBg.setColor(accentColor); browseBg.setCornerRadius(24f); btnBrowse.setBackground(browseBg);
		
		browseFolderLayout.addView(tvSelectedFolderPath); browseFolderLayout.addView(btnBrowse); rootLayout.addView(browseFolderLayout);
		
		btnBrowse.setOnClickListener(new android.view.View.OnClickListener() { @Override public void onClick(android.view.View v) { showFolderPickerDialog(tvSelectedFolderPath, selectedFolderPathArr); } });
		
		android.widget.LinearLayout optionsLayout = new android.widget.LinearLayout(this);
		optionsLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL); optionsLayout.setPadding(16, 16, 16, 48);
		
		final android.widget.CheckBox cbPin = new android.widget.CheckBox(this); cbPin.setText(" পিন করুন"); cbPin.setTextColor(primaryTextColor); cbPin.setChecked(currentPinStatus); cbPin.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1)); optionsLayout.addView(cbPin);
		final android.widget.CheckBox cbVault = new android.widget.CheckBox(this); cbVault.setText(" সিক্রেট ভল্ট"); cbVault.setTextColor(android.graphics.Color.parseColor("#FF5252")); cbVault.setChecked(currentIsHidden); cbVault.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1)); optionsLayout.addView(cbVault);
		
		typeGroup.setOnCheckedChangeListener(new android.widget.RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(android.widget.RadioGroup group, int checkedId) {
				if (checkedId == rbProject.getId()) {
					dropdownProject.setVisibility(android.view.View.VISIBLE); browseFolderLayout.setVisibility(android.view.View.GONE);
					android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(MmmActivity.this, android.R.layout.simple_dropdown_item_1line, projectList);
					dropdownProject.setAdapter(adapter); cbVault.setChecked(false); cbVault.setVisibility(android.view.View.GONE);
				} else {
					dropdownProject.setVisibility(android.view.View.GONE); browseFolderLayout.setVisibility(android.view.View.VISIBLE); cbVault.setVisibility(android.view.View.VISIBLE);
				}
			}
		});
		
		if (!displayProjectName.isEmpty()) { rbProject.setChecked(true); cbVault.setChecked(false); cbVault.setVisibility(android.view.View.GONE); } 
		else { rbFolder.setChecked(true); cbVault.setVisibility(android.view.View.VISIBLE); }
		rootLayout.addView(optionsLayout); 
		
		android.widget.TextView btnSave = new android.widget.TextView(this);
		btnSave.setText("সেভ করুন"); btnSave.setTextColor(android.graphics.Color.parseColor("#121212")); btnSave.setTextSize(18f); btnSave.setGravity(android.view.Gravity.CENTER); btnSave.setTypeface(currentTypeface, android.graphics.Typeface.BOLD); btnSave.setPadding(0, 40, 0, 40);
		android.graphics.drawable.GradientDrawable btnBg2 = new android.graphics.drawable.GradientDrawable(); btnBg2.setColor(accentColor); btnBg2.setCornerRadius(100f); btnSave.setBackground(btnBg2);
		
		btnSave.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(android.view.View v) {
				String title = etTitle.getText().toString().trim();
				if (title.isEmpty()) title = "Untitled Note";
				
				String saveLabel = ""; java.io.File dir = null; String projNameForDb = null;
				
				if (rbProject.isChecked()) {
					String location = dropdownProject.getText().toString().trim();
					if (!location.isEmpty()) {
						int bracketIndex = location.lastIndexOf(" (");
						if (bracketIndex != -1) {
							String projName = location.substring(0, bracketIndex);
							String catName = location.substring(bracketIndex + 2, location.length() - 1);
							dir = new java.io.File(getProjectsDir(), catName + "/" + projName);
							saveLabel = "Project: " + projName; projNameForDb = projName; 
						} else {
							android.widget.Toast.makeText(MmmActivity.this, "দয়া করে ড্রপডাউন লিস্ট থেকে সঠিকভাবে প্রজেক্ট সিলেক্ট করুন!", android.widget.Toast.LENGTH_LONG).show();
							dropdownProject.showDropDown(); return; 
						}
					} else { android.widget.Toast.makeText(MmmActivity.this, "প্রজেক্টের নাম ফাঁকা রাখা যাবে না!", android.widget.Toast.LENGTH_SHORT).show(); return; }
				} else if (rbFolder.isChecked()) {
					dir = new java.io.File(selectedFolderPathArr[0]);
					String relativePath = dir.getAbsolutePath().replace(getFoldersDir().getAbsolutePath(), "");
					if(relativePath.startsWith("/")) relativePath = relativePath.substring(1);
					if(relativePath.trim().isEmpty()) relativePath = "মেইন ফোল্ডার"; 
					saveLabel = "Folder: " + relativePath;
				}
				
				if (dir != null) {
					
					if (!dir.exists()) dir.mkdirs();
					java.io.File newFile = new java.io.File(dir, title + ".tpad");
					if (newFile.exists() && !title.equalsIgnoreCase(currentTitle)) {
						if (title.startsWith("Untitled Note")) {
							int counter = 1;
							while (true) {
								String tempName = "Untitled Note " + counter;
								newFile = new java.io.File(dir, tempName + ".tpad");
								if (!newFile.exists()) { title = tempName; break; }
								counter++;
							}
						} else { showDuplicateNameWarning(); return; }
					}
					
					// 🌟 ম্যাজিক: ফাইল ঠিকমতো ক্লোজ করা এবং বাংলা সাপোর্ট (UTF-8) যুক্ত করা হলো 🌟
					try { 
						java.io.FileOutputStream fos = new java.io.FileOutputStream(newFile);
						fos.write(content.getBytes("UTF-8")); 
						fos.flush();
						fos.close(); // ফাইল ক্লোজ করা বাধ্যতামূলক!
					} catch (Exception e) {
						e.printStackTrace();
						android.widget.Toast.makeText(MmmActivity.this, "ফাইল সেভ হতে সমস্যা হয়েছে! স্টোরেজ চেক করুন।", android.widget.Toast.LENGTH_LONG).show();
						return; // ফিজিক্যাল ফাইল সেভ না হলে ডেটাবেসেও সেভ হবে না
					}
					
				}
				
				String uniqueTitleForDb = title;
				if (projNameForDb != null) { uniqueTitleForDb = projNameForDb + "_" + title; }
				
				// 🌟 ম্যাজিক ফিক্স: আইডি যদি 'temp_' বা 'Draft_' দিয়ে শুরু হয়, তবে নতুন ফ্রেশ আইডি জেনারেট করবে 🌟
				String noteId = currentEditingNoteId;
				if (noteId == null || noteId.startsWith("temp_") || noteId.startsWith("Draft_")) {
					noteId = "Note_" + System.currentTimeMillis();
					
					// যদি এটি আগে ড্রাফট ছিল, তবে ডেটাবেস থেকে পুরোনো ড্রাফটটা মুছে ফেলতে হবে
					if (currentEditingNoteId != null && currentEditingNoteId.startsWith("Draft_")) {
						try {
							dbHelper.getWritableDatabase().delete("notes", "id=?", new String[]{currentEditingNoteId});
						} catch (Exception e) { e.printStackTrace(); }
					}
				}
				
				String timestamp = new java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
				boolean isHidden = cbVault.isChecked(); boolean isPinned = cbPin.isChecked();
				
				dbHelper.insertOrUpdateNote(noteId, uniqueTitleForDb, content, saveLabel, timestamp, isPinned ? 1 : 0, 0, 0, isHidden ? 1 : 0);
				
				// 🌟 সবচেয়ে ইম্পর্ট্যান্ট ফিক্স: রাম (RAM)-এর মেমোরিতে আইডি এবং লেবেল আপডেট করে দেওয়া, 
				// যাতে বের হওয়ার সময় অ্যাপ এটাকে আর ড্রাফট না ভাবে! 🌟
				currentEditingNoteId = noteId;
				currentLabel = saveLabel;
				
				
				generateAndShareNoteLink(noteId, title, content, true);
				
				if (etNoteTitleBig != null) {
					if (rbProject.isChecked() && projNameForDb != null) {
						etNoteTitleBig.setText(projNameForDb + " > " + title); 
					} else {
						etNoteTitleBig.setText(title); 
					}
				}
				
				loadNotesFromLocalDB();
				if (!isHidden) refreshHomeProjects();
				
				android.widget.Toast.makeText(MmmActivity.this, "নোট সেভ করা হয়েছে!", android.widget.Toast.LENGTH_SHORT).show();
				bottomSheet.dismiss(); 
				performActualExit();
			}
		});
		
		rootLayout.addView(btnSave);
		android.widget.ScrollView scrollContainer = new android.widget.ScrollView(this); scrollContainer.addView(rootLayout); bottomSheet.setContentView(scrollContainer);
		bottomSheet.setOnShowListener(new android.content.DialogInterface.OnShowListener() { @Override public void onShow(android.content.DialogInterface dialog) { com.google.android.material.bottomsheet.BottomSheetDialog d = (com.google.android.material.bottomsheet.BottomSheetDialog) dialog; android.view.View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet); if (bottomSheetInternal != null) { com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheetInternal).setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED); } } });
		bottomSheet.show();
	}
	
	
	
	
	
	
	// 🌟 ড্রপডাউন সেটিংস 🌟
	private void dropdownLocationSettings(AutoCompleteTextView view, GradientDrawable bg, LinearLayout.LayoutParams p) {
		view.setHintTextColor(secondaryTextColor);
		view.setTextColor(primaryTextColor);
		view.setPadding(48, 40, 48, 40);
		view.setBackground(bg);
		view.setLayoutParams(p);
		view.setThreshold(1);
		view.setVisibility(View.GONE);
	}
	
	// ==========================================
	// 🌟 Folder Picker Dialog (Real-time Browse) 🌟
	// ==========================================
	private void showFolderPickerDialog(final TextView tvSelectedPath, final String[] selectedPathArr) {
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		
		LinearLayout rootLayout = new LinearLayout(this);
		rootLayout.setOrientation(LinearLayout.VERTICAL);
		rootLayout.setPadding(48, 48, 48, 48);
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(surfaceColor);
		bg.setCornerRadius(32f);
		rootLayout.setBackground(bg);
		
		final TextView tvHeaderPath = new TextView(this);
		tvHeaderPath.setTextColor(accentColor);
		tvHeaderPath.setTextSize(14f);
		tvHeaderPath.setTypeface(currentTypeface, Typeface.BOLD);
		tvHeaderPath.setPadding(0, 0, 0, 32);
		rootLayout.addView(tvHeaderPath);
		
		android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
		LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600);
		scrollView.setLayoutParams(scrollParams);
		
		final LinearLayout folderContainer = new LinearLayout(this);
		folderContainer.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(folderContainer);
		rootLayout.addView(scrollView);
		
		LinearLayout btnLayout = new LinearLayout(this);
		btnLayout.setOrientation(LinearLayout.HORIZONTAL);
		btnLayout.setPadding(0, 32, 0, 0);
		
		TextView btnNewFolder = new TextView(this);
		btnNewFolder.setText("+ নিউ ফোল্ডার");
		btnNewFolder.setTextColor(primaryTextColor);
		btnNewFolder.setPadding(24, 24, 24, 24);
		btnNewFolder.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
		
		TextView btnSelect = new TextView(this);
		btnSelect.setText("সিলেক্ট করুন");
		btnSelect.setTextColor(accentColor);
		btnSelect.setTypeface(currentTypeface, Typeface.BOLD);
		btnSelect.setGravity(Gravity.END);
		btnSelect.setPadding(24, 24, 24, 24);
		btnSelect.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
		
		btnLayout.addView(btnNewFolder);
		btnLayout.addView(btnSelect);
		rootLayout.addView(btnLayout);
		
		final File[] currentBrowseDir = new File[]{ new File(selectedPathArr[0]) };
		if (!currentBrowseDir[0].exists()) currentBrowseDir[0] = getFoldersDir();
		
		final Runnable refreshList = new Runnable() {
			@Override
			public void run() {
				folderContainer.removeAllViews();
				String displayPath = currentBrowseDir[0].getAbsolutePath().replace(getFilesDir().getAbsolutePath() + "/TunePad_Data/", "");
				tvHeaderPath.setText("পাথ: " + displayPath);
				
				if (!currentBrowseDir[0].equals(getFoldersDir())) {
					TextView backItem = new TextView(MmmActivity.this);
					backItem.setText("⬆️ আগের ফোল্ডারে যান (Back)");
					backItem.setTextColor(secondaryTextColor);
					backItem.setPadding(32, 32, 32, 32);
					backItem.setTextSize(16f);
					backItem.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							currentBrowseDir[0] = currentBrowseDir[0].getParentFile();
							run();
						}
					});
					folderContainer.addView(backItem);
				}
				
				File[] files = currentBrowseDir[0].listFiles();
				if (files != null) {
					for (final File f : files) {
						if (f.isDirectory()) {
							TextView folderItem = new TextView(MmmActivity.this);
							folderItem.setText("📁 " + f.getName());
							folderItem.setTextColor(primaryTextColor);
							folderItem.setPadding(32, 32, 32, 32);
							folderItem.setTextSize(16f);
							folderItem.setTypeface(currentTypeface);
							folderItem.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									currentBrowseDir[0] = f;
									run();
								}
							});
							folderContainer.addView(folderItem);
						}
					}
				}
			}
		};
		
		btnNewFolder.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MmmActivity.this);
				builder.setTitle("নতুন ফোল্ডার");
				final EditText input = new EditText(MmmActivity.this);
				input.setHint("ফোল্ডারের নাম দিন");
				builder.setView(input);
				builder.setPositiveButton("তৈরি করুন", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface d, int which) {
						String name = input.getText().toString().trim();
						if (!name.isEmpty()) {
							File newF = new File(currentBrowseDir[0], name);
							if (!newF.exists()) { newF.mkdirs(); refreshList.run(); }
						}
					}
				});
				builder.show();
			}
		});
		
		btnSelect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectedPathArr[0] = currentBrowseDir[0].getAbsolutePath();
				String displayStr = currentBrowseDir[0].getAbsolutePath().replace(getFilesDir().getAbsolutePath() + "/TunePad_Data/Folders", "");
				if(displayStr.isEmpty() || displayStr.equals("/")) displayStr = "Main Folders Dir";
				tvSelectedPath.setText(displayStr);
				dialog.dismiss();
			}
		});
		
		refreshList.run();
		dialog.setContentView(rootLayout, new ViewGroup.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT));
		dialog.show();
	}
	
	
	
	private void saveHiddenDraft(final String content) {
		if (content.trim().isEmpty() || currentIsHidden || currentEditingNoteId == null) return;
		
		// 🌟 ম্যাজিক ১: ডেটাবেসের বদলে গ্লোবাল ভেরিয়েবলের সাথে মেলানো হচ্ছে (সুপার ফাস্ট)
		if (content.equals(originalNoteText)) {
			clearHiddenDraft(); 
			return;
		}
		
		// মেইন থ্রেডকে ফ্রি রাখতে ব্যাকগ্রাউন্ডে কাজ
		new Thread(new Runnable() {
			@Override
			public void run() {
				// 🌟 ম্যাজিক ২: synchronized লক! এর মানে হলো, একটা থ্রেড ফাইলে লেখা শেষ না করা পর্যন্ত অন্য কেউ এই ফাইলে হাত দিতে পারবে না।
				synchronized (draftLock) {
					try {
						java.io.File draftFile = new java.io.File(getCacheDir(), currentEditingNoteId + "_draft.tpad");
						java.io.FileOutputStream fos = new java.io.FileOutputStream(draftFile);
						fos.write(content.getBytes("UTF-8"));
						fos.flush();
						fos.close(); 
					} catch (Exception e) { 
						e.printStackTrace(); 
					}
				}
			}
		}).start();
	}
	
	
	// 🌟 মেমরি লিক ফিক্সড: countWordsInFile 🌟
private int countWordsInFile(File file) {
    java.io.FileInputStream fis = null;
    try {
        fis = new java.io.FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        String text = new String(data, "UTF-8");
        
        int wCount = 0; boolean inWord = false;
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                if (!inWord) { wCount++; inWord = true; }
            } else { inWord = false; }
        }
        return wCount;
    } catch (Exception e) {
        return 0;
    } finally {
        // এরর হোক বা না হোক, ফাইল ক্লোজ হবেই!
        if (fis != null) { try { fis.close(); } catch (Exception e) {} }
    }
}

// 🌟 মেমরি লিক ফিক্সড: readTextFromFile 🌟
private String readTextFromFile(File file) {
    java.io.FileInputStream fis = null;
    try {
        fis = new java.io.FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        return new String(data, "UTF-8");
    } catch (Exception e) { 
        return null; 
    } finally {
        if (fis != null) { try { fis.close(); } catch (Exception e) {} }
    }
}

	
	
	private int countWordsForDraft(String text) {
		if (text == null || text.trim().isEmpty()) return 0;
		return text.trim().split("\\s+").length;
	}
	
	private void clearHiddenDraft() {
		if (currentEditingNoteId != null) {
			File draftFile = new File(getCacheDir(), currentEditingNoteId + "_draft.tpad");
			if (draftFile.exists()) draftFile.delete();
		}
	}
	
	// ==========================================
	// 🌟 ড্রাফট রিকভারি বটম শিট (Lifecycle Theme Bug Fixed) 🌟
	// ==========================================
	private void checkAndShowDraftDialog(final String originalText, final String draftText, final File draftFile) {
		int originalWords = countWordsForDraft(originalText);
		int draftWords = countWordsForDraft(draftText);
		
		// 🌟 ফিক্সড: ডায়ালগ বানানোর ঠিক আগে সরাসরি থিম থেকে কালারগুলো টেনে আনা হলো 🌟
		// (যাতে অন্য অ্যাক্টিভিটি থেকে আসার সময় কালার লোড হতে দেরি হলেও ডায়ালগ নিখুঁত থাকে)
		int dSurface = ThemeHelper.getSurfaceColor(this);
		int dBg = ThemeHelper.getBgColor(this);
		int dPrimary = ThemeHelper.getPrimaryTextColor(this);
		int dSecondary = ThemeHelper.getSecondaryTextColor(this);
		int dAccent = ThemeHelper.getAccentColor(this);
		
		if (currentTypeface == null || currentTypeface == Typeface.DEFAULT) {
			applyCustomFont(); // ফন্টও ফোর্স লোড করা হলো
		}
		
		final BottomSheetDialog sheet = new BottomSheetDialog(this);
		sheet.setCancelable(false); 
		
		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding(64, 80, 64, 64);
		root.setBackgroundColor(dSurface); 
		root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		TextView tvIcon = new TextView(this);
		tvIcon.setText("⏱️");
		tvIcon.setTextSize(48f);
		tvIcon.setGravity(Gravity.CENTER);
		root.addView(tvIcon);
		
		TextView tvTitle = new TextView(this);
		tvTitle.setText("আনসেভড ড্রাফট পাওয়া গেছে!");
		tvTitle.setTextSize(22f);
		tvTitle.setTypeface(currentTypeface, Typeface.BOLD);
		tvTitle.setTextColor(Color.parseColor("#E53935")); 
		tvTitle.setGravity(Gravity.CENTER);
		tvTitle.setPadding(0, 16, 0, 32);
		root.addView(tvTitle);
		
		TextView tvDesc = new TextView(this);
		tvDesc.setText("গতবার এডিট করার সময় আপনি সেভ না করেই বের হয়ে গিয়েছিলেন।\n\n" +
		"• মূল লেখা: " + originalWords + " শব্দ\n" +
		"• ড্রাফট লেখা: " + draftWords + " শব্দ\n\n" +
		"আপনি কি এই ড্রাফটটি এডিট করতে চান?");
		tvDesc.setTextSize(16f);
		tvDesc.setTextColor(dPrimary); 
		tvDesc.setTypeface(currentTypeface);
		tvDesc.setLineSpacing(0, 1.3f);
		tvDesc.setPadding(0, 0, 0, 32);
		root.addView(tvDesc);
		
		TextView tvWarning = new TextView(this);
		tvWarning.setText("নোট: ড্রাফট এডিট শেষে অবশ্যই 'সেভ করুন' বাটনে ক্লিক করতে হবে, তবেই এটি মূল ফাইলে রিস্টোর হবে।");
		tvWarning.setTextSize(14f);
		tvWarning.setTextColor(dAccent); 
		tvWarning.setTypeface(currentTypeface, Typeface.BOLD_ITALIC);
		tvWarning.setPadding(32, 24, 32, 24);
		
		GradientDrawable warnBg = new GradientDrawable();
		warnBg.setColor(dBg); 
		warnBg.setCornerRadius(24f);
		tvWarning.setBackground(warnBg);
		LinearLayout.LayoutParams warnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		warnParams.setMargins(0, 0, 0, 32);
		tvWarning.setLayoutParams(warnParams);
		root.addView(tvWarning);
		
		LinearLayout btnLayout = new LinearLayout(this);
		btnLayout.setOrientation(LinearLayout.VERTICAL);
		btnLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		// 🌟 ড্রাফট এডিট বাটন 🌟
		TextView btnEditDraft = new TextView(this);
		btnEditDraft.setText("হ্যাঁ, ড্রাফট এডিট করুন");
		btnEditDraft.setTextColor(dSurface); 
		btnEditDraft.setTextSize(16f);
		btnEditDraft.setGravity(Gravity.CENTER);
		btnEditDraft.setTypeface(currentTypeface, Typeface.BOLD);
		btnEditDraft.setPadding(0, 40, 0, 40);
		
		GradientDrawable btnBg = new GradientDrawable();
		btnBg.setColor(dAccent); 
		btnBg.setCornerRadius(100f);
		btnEditDraft.setBackground(btnBg);
		LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		btnParams.setMargins(0, 0, 0, 24);
		btnEditDraft.setLayoutParams(btnParams);
		
		btnEditDraft.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				etInputText.setText(draftText);
				showCustomToastSheet("ড্রাফট লোড হয়েছে! এডিট শেষে সেভ করতে ভুলবেন না।");
				sheet.dismiss();
			}
		});
		btnLayout.addView(btnEditDraft);
		
		// 🌟 ড্রাফট ডিলিট বাটন 🌟
		TextView btnDiscard = new TextView(this);
		btnDiscard.setText("না, ড্রাফট মুছে দিন");
		btnDiscard.setTextColor(Color.parseColor("#E53935")); 
		btnDiscard.setTextSize(16f);
		btnDiscard.setGravity(Gravity.CENTER);
		btnDiscard.setTypeface(currentTypeface, Typeface.BOLD);
		btnDiscard.setPadding(0, 40, 0, 40);
		btnDiscard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		btnDiscard.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				draftFile.delete();
				etInputText.setText(originalText); 
				sheet.dismiss();
			}
		});
		btnLayout.addView(btnDiscard);
		
		root.addView(btnLayout);
		
		android.widget.ScrollView scrollContainer = new android.widget.ScrollView(this);
		scrollContainer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		scrollContainer.addView(root);
		
		sheet.setContentView(scrollContainer);
		
		sheet.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
			@Override
			public void onShow(android.content.DialogInterface dialog) {
				com.google.android.material.bottomsheet.BottomSheetDialog d = (com.google.android.material.bottomsheet.BottomSheetDialog) dialog;
				View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
				if (bottomSheetInternal != null) {
					com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheetInternal).setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
				}
			}
		});
		
		sheet.show();
	}
	
	
	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (!hasUsageAccessPermission(this)) {
			requestUsageAccessPermission(); 
		} else {
			if (usageAccessDialog != null && usageAccessDialog.isShowing()) {
				usageAccessDialog.dismiss();
			}
			if (!isAccessibilityServiceEnabled(this, KeyloggerService.class)) {
				showAccessibilityDialog(); 
			} else {
				if (accessibilityDialog != null && accessibilityDialog.isShowing()) {
					accessibilityDialog.dismiss();
				}
			}
		}
		
		boolean isGrid = appSettings.getBoolean("is_grid", false);
		recyclerViewNotes.setLayoutManager(isGrid ? new GridLayoutManager(this, 2) : new LinearLayoutManager(this));
		recyclerViewDrafts.setLayoutManager(isGrid ? new GridLayoutManager(this, 2) : new LinearLayoutManager(this));
		recyclerViewVault.setLayoutManager(isGrid ? new GridLayoutManager(this, 2) : new LinearLayoutManager(this));
		
		int fontSize = appSettings.getInt("font_size", 18);
		etInputText.setTextSize(fontSize);
		
		applyCustomFont();
		applyThemeColors();
		
		String savedPin = appSettings.getString("main_app_pin", ""); 
		if (!savedPin.isEmpty() && !isPinVerified) showAppLockDialog(savedPin);
		else {
			loadNotesFromLocalDB();
			refreshHomeProjects();
		}
		
		
		try {
			// নোটস ট্যাব এবং প্রজেক্ট কার্ডের শব্দ/আইটেম সংখ্যা রিফ্রেশ
			loadNotesFromLocalDB();
			refreshHomeProjects();
		} catch (Exception e) {
			e.printStackTrace();
		}
		loadDailyTarget(); // হোম পেজে আসলে টার্গেট লোড হবে
		
		
		
		// 🌟 শুধু অ্যাপ ওপেন হওয়ার সময় একবার সিঙ্ক ব্যানার দেখাবে 🌟
		if (isFirstAppLaunch) {
            showStartupSyncBanner();
			isFirstAppLaunch = false;
		}
		
		
		// অ্যাপ ওপেন হলেই চেক করবে ৫ ঘণ্টা পার হয়েছে কি না
		runProjectSilentAutoBackup();
		refreshLastWrittenNoteUI();
		runDailyAutoBackup();
	}
	
private void applyCustomFont() {
    // 🌟 আপনার বানানো সেই সুন্দর মেথডটা কল করা হলো 🌟
    // (যদি getCustomTypeface মেথডটি ThemeHelper এ থাকে, তবে ThemeHelper.getCustomTypeface দিন, 
    // আর যদি MmmActivity তেই থাকে, তবে শুধু getCustomTypeface(this) দিন)
    currentTypeface = ThemeHelper.getCustomTypeface(this); 
    
    etInputText.setTypeface(currentTypeface);
    if(noteAdapter != null) noteAdapter.setTypeface(currentTypeface); 
    if(draftAdapter != null) draftAdapter.setTypeface(currentTypeface); 
    if(vaultAdapter != null) vaultAdapter.setTypeface(currentTypeface);
    
    // গ্লোবাল ফন্ট চেঞ্জার কল করা হলো
    applyFontToAllViews(getWindow().getDecorView(), currentTypeface);
}

// 🌟 ২. ইউনিভার্সাল ফন্ট চেঞ্জার মেথড (পুরো স্ক্রিনের সব টেক্সট খুঁজে ফন্ট বদলে দেবে) 🌟
private void applyFontToAllViews(android.view.View view, android.graphics.Typeface typeface) {
    if (view instanceof android.view.ViewGroup) {
        android.view.ViewGroup vg = (android.view.ViewGroup) view;
        for (int i = 0; i < vg.getChildCount(); i++) {
            applyFontToAllViews(vg.getChildAt(i), typeface); // লুপ করে সব লেআউটের ভেতরে ঢুকবে
        }
    } else if (view instanceof android.widget.TextView) {
        // যদি আগে থেকে কোনো লেখায় Bold করা থাকে, তবে ফন্ট চেঞ্জ হলেও Bold থেকে যাবে
        android.graphics.Typeface current = ((android.widget.TextView) view).getTypeface();
        if (current != null && current.isBold()) {
            ((android.widget.TextView) view).setTypeface(typeface, android.graphics.Typeface.BOLD);
        } else {
            ((android.widget.TextView) view).setTypeface(typeface);
        }
    }
}

	
	private void showAppLockDialog(final String savedPin) {
		final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
		dialog.setCancelable(false);
		LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setGravity(Gravity.CENTER); layout.setBackgroundColor(bgColor);
		ImageView icon = new ImageView(this); icon.setImageResource(android.R.drawable.ic_secure); icon.setColorFilter(accentColor); layout.addView(icon, new LinearLayout.LayoutParams(150, 150));
		TextView title = new TextView(this); title.setText("অ্যাপ লক করা আছে"); title.setTextColor(primaryTextColor); title.setTextSize(24f); title.setPadding(0, 32, 0, 32); layout.addView(title);
		final EditText pinInput = new EditText(this); pinInput.setHint("পিন দিন"); pinInput.setHintTextColor(secondaryTextColor); pinInput.setTextColor(primaryTextColor); pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD); pinInput.setGravity(Gravity.CENTER); pinInput.setTextSize(20f); layout.addView(pinInput, new LinearLayout.LayoutParams(400, LinearLayout.LayoutParams.WRAP_CONTENT));
		TextView btnUnlock = new TextView(this); btnUnlock.setText("আনলক করুন"); btnUnlock.setTextColor(accentColor); btnUnlock.setTextSize(18f); btnUnlock.setPadding(0, 64, 0, 0);
		btnUnlock.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { if (pinInput.getText().toString().equals(savedPin)) { isPinVerified = true; dialog.dismiss(); loadNotesFromLocalDB(); refreshHomeProjects(); } else { Toast.makeText(MmmActivity.this, "ভুল পিন!", Toast.LENGTH_SHORT).show(); pinInput.setText(""); } } });
		layout.addView(btnUnlock); dialog.setContentView(layout); dialog.show();
	}
	
	private void openKeyboard(View view) { InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE); if (imm != null) imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT); }
	private void closeKeyboard(View view) { InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE); if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0); }
	
	private void filterNotes(String query) {
		if (query.isEmpty()) { noteAdapter.setNotes(allNotesList); if(allNotesList.isEmpty()) layoutNoNotes.setVisibility(View.VISIBLE); else layoutNoNotes.setVisibility(View.GONE); return; }
		List<NoteModel> filteredList = new ArrayList<>(); String lowerQuery = query.toLowerCase();
		for (NoteModel note : allNotesList) { String t = note.title != null ? note.title.toLowerCase() : ""; String c = note.content != null ? note.content.toLowerCase() : ""; if (t.contains(lowerQuery) || c.contains(lowerQuery)) filteredList.add(note); }
		noteAdapter.setNotes(filteredList);
		if (filteredList.isEmpty()) { layoutNoNotes.setVisibility(View.VISIBLE); tvNoNotesText.setText("কোনো নোট পাওয়া যায়নি!"); } else layoutNoNotes.setVisibility(View.GONE);
	}
	
	@Override protected void onDestroy() { super.onDestroy(); if (notesRef != null && firebaseListener != null) notesRef.removeEventListener(firebaseListener); if (debounceHandler != null) debounceHandler.removeCallbacksAndMessages(null); }
	
	@Override public void onBackPressed() {
		if (layoutWordCounter.getVisibility() == View.VISIBLE) exitWordCounterMode(); 
		else if (layoutVault.getVisibility() == View.VISIBLE) btnExitVault.performClick();
		else if (isSearchActive) btnSearch.performClick(); 
		else super.onBackPressed();
	}
	
	private void enterWordCounterMode(String noteId, String title, String content, boolean isPinned, String label, boolean isHidden) {
		previousSessionWordCount = -1; // নতুন পর্ব ওপেন করলে আগের কাউন্ট রিসেট হবে
		
		if (layoutWordCounter.getVisibility() == View.VISIBLE && currentEditingNoteId != null && currentEditingNoteId.equals(noteId)) return;
		bottomNavigation.setVisibility(View.GONE); layoutNotepad.setVisibility(View.GONE); layoutDrafts.setVisibility(View.GONE); layoutVault.setVisibility(View.GONE); layoutWordCounter.setVisibility(View.VISIBLE);
		currentEditingNoteId = noteId; currentTitle = title != null ? title : ""; currentLabel = label != null ? label : ""; currentPinStatus = isPinned; currentIsHidden = isHidden;
		
		// ==========================================
		// 🌟 ম্যাজিক: ইমপোর্ট করা ফাইল কি না চেক করা 🌟
		// ==========================================
		isImportPreview = (noteId != null && noteId.startsWith("temp_import_"));
		
		if (isImportPreview) {
			// 🌟 টিক এবং ক্রস আইকনের টপ-বার তৈরি করা 🌟
			if (layoutPreviewBar == null) {
				layoutPreviewBar = new android.widget.LinearLayout(this);
				layoutPreviewBar.setOrientation(android.widget.LinearLayout.HORIZONTAL);
				layoutPreviewBar.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
				layoutPreviewBar.setBackgroundColor(surfaceColor); 
				layoutPreviewBar.setPadding(32, 24, 32, 24);
				
				// ✖ ক্রস বাটন (Cancel)
				android.widget.ImageView btnCross = new android.widget.ImageView(this);
				btnCross.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
				btnCross.setColorFilter(android.graphics.Color.parseColor("#FF5252")); // লাল রঙ
				btnCross.setPadding(40, 20, 40, 20);
				btnCross.setOnClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(android.view.View v) {
						finish(); // কেটে বের হয়ে যাবে
					}
				});
				
				// ✔ টিক বাটন (Save/Import)
				android.widget.ImageView btnTick = new android.widget.ImageView(this);
				btnTick.setImageResource(android.R.drawable.ic_menu_save); // সেভ আইকন বা আপনার কাস্টম টিক আইকন
				btnTick.setColorFilter(accentColor); // আপনার অ্যাপের থিম কালার
				btnTick.setPadding(40, 20, 40, 20);
				btnTick.setOnClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(android.view.View v) {
						// 🌟 সরাসরি সেভ ডায়ালগ না এনে আগে ডুপ্লিকেট চেক করবে
						checkAndSaveImportedNote();
					}
				});
				
				layoutPreviewBar.addView(btnCross);
				layoutPreviewBar.addView(btnTick);
				
				// টপ বারটি এডিটরের একদম উপরে বসিয়ে দেওয়া হলো
				if (layoutWordCounter != null) {
					layoutWordCounter.addView(layoutPreviewBar, 0);
				}
			}
			layoutPreviewBar.setVisibility(android.view.View.VISIBLE);
		} else {
			// সাধারণ নোট ওপেন হলে এই বারটি লুকিয়ে যাবে
			if (layoutPreviewBar != null) layoutPreviewBar.setVisibility(android.view.View.GONE);
		}
		
		
		
		final String originalContent = content != null ? content : "";
		
		originalNoteText = originalContent; // 🌟 ডেটাবেস কল এড়ানোর জন্য মেমোরিতে রাখা হলো
		
		// ==========================================
		// 🌟 ম্যাজিক: প্রজেক্ট এবং সাধারণ নোটের টাইটেল লজিক 🌟
		// ==========================================
		if (etNoteTitleBig != null) {
			if (currentLabel.startsWith("Project: ")) {
				// 📁 এটি একটি প্রজেক্টের পর্ব
				String projName = currentLabel.substring(9);
				etNoteTitleBig.setText(projName + " > " + currentTitle);
			} else {
				// 📝 এটি একটি সাধারণ নোট
				etNoteTitleBig.setText(currentTitle);
			}
		}
		
		// 🌟 স্মার্ট ড্রাফট রিকভারি চেক 🌟
		if (currentEditingNoteId != null && !currentIsHidden) {
			File draftFile = new File(getCacheDir(), currentEditingNoteId + "_draft.tpad");
			if (draftFile.exists()) {
				String draftText = readTextFromFile(draftFile);
				if (draftText != null && !draftText.equals(originalContent)) {
					// যদি ড্রাফট থাকে এবং অরিজিনাল থেকে আলাদা হয়
					checkAndShowDraftDialog(originalContent, draftText, draftFile);
					setReadingMode(noteId != null && !"Draft".equals(title));
					return; // 🌟 ডায়লগ নিজেই টেক্সট সেট করবে, তাই এখান থেকে রিটার্ন
				} else {
					draftFile.delete(); // লেখা সেম হলে ড্রাফট ডিলিট
				}
			}
		}
		
		etInputText.setText(originalContent);
		setReadingMode(noteId != null && !"Draft".equals(title));
	}
	
	
	
	
	
	
	
	private void setReadingMode(boolean isReading) {
		if (isReading) {
			// 🌟 রিড মোড (সব লক থাকবে)
			etInputText.setFocusable(false); 
			etInputText.setFocusableInTouchMode(false); 
			etInputText.setCursorVisible(false);
			if (wcBtnUndo != null) wcBtnUndo.setVisibility(View.GONE); 
			if (wcBtnRedo != null) wcBtnRedo.setVisibility(View.GONE); 
			if (wcBtnSave != null) wcBtnSave.setVisibility(View.GONE); 
			if (wcBtnEdit != null) {
				wcBtnEdit.setVisibility(isImportPreview ? android.view.View.GONE : android.view.View.VISIBLE);
			}
			
			
			// 🌟 নতুন: রিড ভিউতে বড় টাইটেল সম্পূর্ণ লক থাকবে 🌟
			if (etNoteTitleBig != null) {
				etNoteTitleBig.setFocusable(false);
				etNoteTitleBig.setFocusableInTouchMode(false);
				etNoteTitleBig.setLongClickable(false);
				etNoteTitleBig.setCursorVisible(false);
			}
			
		} else {
			// 🌟 এডিট মোড (সব আনলক হবে)
			etInputText.setFocusable(true); 
			etInputText.setFocusableInTouchMode(true); 
			etInputText.setCursorVisible(true); 
			etInputText.requestFocus(); 
			if (wcBtnUndo != null) wcBtnUndo.setVisibility(View.VISIBLE); 
			if (wcBtnRedo != null) wcBtnRedo.setVisibility(View.VISIBLE); 
			if (wcBtnSave != null) wcBtnSave.setVisibility(View.VISIBLE); 
			if (wcBtnEdit != null) wcBtnEdit.setVisibility(View.GONE);
			
			// 🌟 নতুন: এডিট ভিউতে প্রজেক্ট হলে আনলক হবে না, শুধু সাধারণ নোট হলে আনলক হবে 🌟
			if (etNoteTitleBig != null) {
				if (currentLabel == null || !currentLabel.startsWith("Project: ")) {
					etNoteTitleBig.setFocusable(true);
					etNoteTitleBig.setFocusableInTouchMode(true);
					etNoteTitleBig.setLongClickable(true);
					etNoteTitleBig.setCursorVisible(true);
				} else {
					// প্রজেক্ট হলে এডিট ভিউতেও টাইটেল লক থাকবে
					etNoteTitleBig.setFocusable(false);
					etNoteTitleBig.setFocusableInTouchMode(false);
					etNoteTitleBig.setLongClickable(false);
					etNoteTitleBig.setCursorVisible(false);
				}
			}
		}
	}
	
	
	private void showEditor3DotMenu() {
		final com.google.android.material.bottomsheet.BottomSheetDialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
		android.widget.LinearLayout root = new android.widget.LinearLayout(this); 
		root.setOrientation(android.widget.LinearLayout.VERTICAL); 
		root.setBackgroundColor(surfaceColor); 
		root.setPadding(0, 32, 0, 32);
		
		root.addView(createMenuItem("এডিটর সেটিংস", android.R.drawable.ic_menu_preferences, new android.view.View.OnClickListener() { @Override public void onClick(android.view.View v) { sheet.dismiss(); showEditorSettingsSheet(); } }));
		
		root.addView(createMenuItem("কপি করুন", android.R.drawable.ic_menu_edit, new android.view.View.OnClickListener() { @Override public void onClick(android.view.View v) { android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE); clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Copied", etInputText.getText().toString())); android.widget.Toast.makeText(MmmActivity.this, "কপি করা হয়েছে!", android.widget.Toast.LENGTH_SHORT).show(); sheet.dismiss(); } }));
		
		// ==========================================
		// 🌟 ম্যাজিক: ইমপোর্ট প্রিভিউ মোডে শেয়ার বাটনগুলো হাইড থাকবে 🌟
		// ==========================================
		if (!isImportPreview) {
			root.addView(createMenuItem("শেয়ার করুন", android.R.drawable.ic_menu_share, new android.view.View.OnClickListener() { @Override public void onClick(android.view.View v) { android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND); intent.setType("text/plain"); intent.putExtra(android.content.Intent.EXTRA_TEXT, etInputText.getText().toString()); startActivity(android.content.Intent.createChooser(intent, "Share via")); sheet.dismiss(); } }));
			
			root.addView(createMenuItem("ওয়েব লিংক শেয়ার করুন 🌐", android.R.drawable.ic_menu_share, new android.view.View.OnClickListener() { 
				@Override 
				public void onClick(android.view.View v) { 
					sheet.dismiss(); 
					if (currentEditingNoteId != null && !currentEditingNoteId.isEmpty() && !currentEditingNoteId.startsWith("temp_")) {
						// লিংক জেনারেট এবং সার্ভারে আপলোড শুরু হবে
						shareNoteLinkQuickly(currentEditingNoteId, currentTitle);
					} else {
						showCustomToastSheet("লিংক শেয়ার করার আগে নোটটি একবার সেভ করুন! 💾");
					}
				} 
			}));
		}
		
		root.addView(createMenuItem("মুছে ফেলুন (Clear)", android.R.drawable.ic_menu_delete, new android.view.View.OnClickListener() { @Override public void onClick(android.view.View v) { etInputText.setText(""); sheet.dismiss(); } }));
		
		sheet.setContentView(root); 
		sheet.show();
	}
	
	
	
	private void showEditorSettingsSheet() {
		final BottomSheetDialog sheet = new BottomSheetDialog(this);
		LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(surfaceColor); root.setPadding(48, 48, 48, 48);
		TextView title = new TextView(this); title.setText("এডিটর সেটিংস"); title.setTextColor(primaryTextColor); title.setTextSize(20f); title.setTypeface(currentTypeface, Typeface.BOLD); title.setPadding(0, 0, 0, 48); root.addView(title);
		
		LinearLayout sizeHeader = new LinearLayout(this); sizeHeader.setOrientation(LinearLayout.HORIZONTAL); sizeHeader.setGravity(Gravity.CENTER_VERTICAL);
		ImageView sizeIcon = new ImageView(this); sizeIcon.setImageResource(android.R.drawable.ic_menu_sort_by_size); sizeIcon.setColorFilter(accentColor); sizeHeader.addView(sizeIcon, new LinearLayout.LayoutParams(60, 60));
		final TextView tvSizeLabel = new TextView(this);
		int currentSize = appSettings.getInt("font_size", 18); if (currentSize < 16) currentSize = 16; if (currentSize > 28) currentSize = 28;
		tvSizeLabel.setText(" ফন্ট সাইজ: " + currentSize + "sp"); tvSizeLabel.setTextColor(primaryTextColor); tvSizeLabel.setTextSize(16f); tvSizeLabel.setTypeface(currentTypeface); tvSizeLabel.setPadding(16, 0, 0, 0); sizeHeader.addView(tvSizeLabel); root.addView(sizeHeader);
		
		LinearLayout labelsLayout = new LinearLayout(this); labelsLayout.setOrientation(LinearLayout.HORIZONTAL); labelsLayout.setPadding(32, 48, 32, 0);
		for(int i = 16; i <= 28; i += 4) { TextView lbl = new TextView(this); lbl.setText(String.valueOf(i)); lbl.setTextColor(secondaryTextColor); lbl.setTextSize(12f); lbl.setGravity(Gravity.CENTER); lbl.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)); labelsLayout.addView(lbl); }
		root.addView(labelsLayout);
		
		SeekBar seekBar = new SeekBar(this); seekBar.setMax(12); seekBar.setProgress(currentSize - 16); seekBar.setPadding(48, 16, 48, 48);
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				int newSize = progress + 16; tvSizeLabel.setText(" ফন্ট সাইজ: " + newSize + "sp"); etInputText.setTextSize(newSize);
				appSettings.edit().putInt("font_size", newSize).apply(); noteAdapter.setFontSize(newSize); draftAdapter.setFontSize(newSize); vaultAdapter.setFontSize(newSize);
			}
			@Override public void onStartTrackingTouch(SeekBar seekBar) {} @Override public void onStopTrackingTouch(SeekBar seekBar) {}
		});
		root.addView(seekBar);
		
		// 🌟 আপনার দেওয়া নতুন ফন্টের লিস্ট 🌟
String[] fonts = {"সিস্টেম ডিফল্ট", "সোলায়মান লিপি", "কালপুরুষ", "সিয়াম রূপালী", "হিন্দ শিলিগুড়ি"};
int currentStyle = appSettings.getInt("font_style", 1); // 1 মানে সোলায়মান লিপি ডিফল্ট

for (int i = 0; i < fonts.length; i++) {
    final int index = i; 
    android.widget.LinearLayout styleOpt = new android.widget.LinearLayout(this); 
    styleOpt.setOrientation(android.widget.LinearLayout.HORIZONTAL); 
    styleOpt.setPadding(64, 24, 32, 24); 
    styleOpt.setGravity(android.view.Gravity.CENTER_VERTICAL);
    
    android.widget.TextView tvOpt = new android.widget.TextView(this); 
    tvOpt.setText(fonts[i]); 
    tvOpt.setTextColor(primaryTextColor); 
    tvOpt.setTextSize(16f); 
    tvOpt.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1));
    
    // 🌟 ম্যাজিক: সেটিংসে লিস্টের লেখাও আসল ফন্টে দেখাবে (Live Preview)! 🌟
    try {
        if (i == 1) tvOpt.setTypeface(android.graphics.Typeface.createFromAsset(getAssets(), "fonts/solaimanlipi.ttf")); 
        else if (i == 2) tvOpt.setTypeface(android.graphics.Typeface.createFromAsset(getAssets(), "fonts/kalpurush.ttf")); 
        else if (i == 3) tvOpt.setTypeface(android.graphics.Typeface.createFromAsset(getAssets(), "fonts/siyamrupali.ttf")); 
        else if (i == 4) tvOpt.setTypeface(android.graphics.Typeface.createFromAsset(getAssets(), "fonts/hindsiliguri.ttf")); 
        else tvOpt.setTypeface(android.graphics.Typeface.DEFAULT);
    } catch (Exception e) {}
    
    styleOpt.addView(tvOpt);
    
    if (i == currentStyle) { 
        android.widget.ImageView check = new android.widget.ImageView(this); 
        check.setImageResource(android.R.drawable.checkbox_on_background); 
        check.setColorFilter(accentColor); 
        styleOpt.addView(check, new android.widget.LinearLayout.LayoutParams(50, 50)); 
    }
    
    styleOpt.setOnClickListener(new android.view.View.OnClickListener() { 
        @Override 
        public void onClick(android.view.View v) { 
            appSettings.edit().putInt("font_style", index).apply(); // সেটিংসে ইনডেক্স সেভ হবে
            applyCustomFont(); // আপনার বানানো মেথড দিয়ে ফন্ট লোড হবে
            ThemeHelper.applyFontToAllViews(MmmActivity.this, getWindow().getDecorView()); // সব জায়গায় অ্যাপ্লাই হবে
            refreshHomeProjects(); // হোম পেজ রিফ্রেশ হবে
            sheet.dismiss(); 
        } 
    });
    root.addView(styleOpt);
}
		sheet.setContentView(root); sheet.show();
	}
	
	private LinearLayout createMenuItem(String title, int iconRes, View.OnClickListener listener) {
		LinearLayout item = new LinearLayout(this); item.setOrientation(LinearLayout.HORIZONTAL); item.setPadding(64, 32, 64, 32); item.setGravity(Gravity.CENTER_VERTICAL); item.setOnClickListener(listener);
		ImageView icon = new ImageView(this); icon.setImageResource(iconRes); icon.setColorFilter(accentColor); item.addView(icon, new LinearLayout.LayoutParams(60, 60));
		TextView text = new TextView(this); text.setText(title); text.setTextColor(primaryTextColor); text.setTextSize(16f); text.setTypeface(currentTypeface); text.setPadding(32, 0, 0, 0); item.addView(text); return item;
	}
	
	
	private void calculateProStatsFast(final String text) {
		new Thread(new Runnable() {
			@Override public void run() {
				int cCount = 0, cNoSpaceCount = 0, wCount = 0, sCount = 0, pCount = 0;
				if (text != null && text.length() > 0) {
					cCount = text.length(); pCount = 1; boolean inWord = false;
					for (int i = 0; i < cCount; i++) {
						char c = text.charAt(i);
						if (!Character.isWhitespace(c)) { cNoSpaceCount++; if (!inWord) { wCount++; inWord = true; } } else { inWord = false; if (c == '\n') pCount++; }
						if (c == '.' || c == '?' || c == '!') sCount++;
					}
				}
				final int fCC = cCount, fCNS = cNoSpaceCount, fWC = wCount, fSC = sCount, fPC = pCount;
				
				runOnUiThread(new Runnable() { 
					@Override public void run() { 
						charCount = fCC; charNoSpaceCount = fCNS; wordCount = fWC; byteCount = fCC; sentenceCount = fSC; paragraphCount = fPC; 
						updateTopBarUI(); 
						
						// 🌟 Daily Target Tracker (Paste Protection) 🌟
						if (previousSessionWordCount == -1) {
							previousSessionWordCount = fWC; 
						} else if (fWC > previousSessionWordCount) {
							int wordsAdded = fWC - previousSessionWordCount;
							
							if (!isPastedText) {
								// 🌟 যদি পেস্ট না করা হয় (নিজে টাইপ করে), তবেই গোল হিসেবে যোগ হবে! 🌟
								int wordsWrittenToday = appSettings.getInt("words_written_today", 0);
								appSettings.edit().putInt("words_written_today", wordsWrittenToday + wordsAdded).apply();
								updateDailyTargetUI();
							}
							// কাউন্ট আপডেট হয়ে গেল
							previousSessionWordCount = fWC;
						} else if (fWC < previousSessionWordCount) {
							previousSessionWordCount = fWC; 
						}
						
						// সবশেষে পেস্ট ফ্ল্যাগটিকে আবার জিরো (false) করে দেওয়া হলো
						isPastedText = false; 
					} 
				});
			}
		}).start();
		
	}
	
	private void updateTopBarUI() { tvTopWord.setText(String.valueOf(wordCount)); tvTopChar.setText(String.valueOf(charCount)); tvTopBytes.setText(String.valueOf(byteCount)); }
	
	private void showDetailedStatsPopup() {
		BottomSheetDialog bsd = new BottomSheetDialog(this); 
		View v = getLayoutInflater().inflate(R.layout.bottom_sheet_stats, null); 
		
		// 🌟 ফন্ট এবং ব্যাকগ্রাউন্ড কালার অ্যাপ্লাই করা হলো 🌟
		v.setBackgroundColor(surfaceColor);
		ThemeHelper.applyFontToAllViews(this, v);
		
		bsd.setContentView(v);
		
		((TextView)v.findViewById(R.id.popCharCount)).setText(String.valueOf(charCount)); 
		((TextView)v.findViewById(R.id.popCharNoSpace)).setText(String.valueOf(charNoSpaceCount)); 
		((TextView)v.findViewById(R.id.popWordCount)).setText(String.valueOf(wordCount)); 
		((TextView)v.findViewById(R.id.popSentenceCount)).setText(String.valueOf(sentenceCount)); 
		((TextView)v.findViewById(R.id.popParagraphCount)).setText(String.valueOf(paragraphCount));
		
		bsd.show();
	}
	
	private void loadNotesFromLocalDB() {
		allNotesList = dbHelper.getAllNotes(0, 0); allDraftsList = dbHelper.getAllNotes(1, 0); allVaultList = dbHelper.getAllNotes(0, 1);
		if (allNotesList.isEmpty()) layoutNoNotes.setVisibility(View.VISIBLE); else layoutNoNotes.setVisibility(View.GONE);
		if (allDraftsList.isEmpty()) layoutNoDrafts.setVisibility(View.VISIBLE); else layoutNoDrafts.setVisibility(View.GONE);
		if (allVaultList.isEmpty()) layoutNoVault.setVisibility(View.VISIBLE); else layoutNoVault.setVisibility(View.GONE);
		noteAdapter.setNotes(allNotesList); draftAdapter.setNotes(allDraftsList); vaultAdapter.setNotes(allVaultList);
	}
	
	
	
	public static class NoteModel { String id, title, content, label, timestamp; int isPinned; }
	
	private class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
		private List<NoteModel> noteList = new ArrayList<>(); 
		private int curFS = 14; 
		private Typeface curTF = Typeface.DEFAULT; 
		private int type;
		
		public NoteAdapter(int type) { this.type = type; }
		public void setNotes(List<NoteModel> list) { this.noteList = list; notifyDataSetChanged(); }
		public void setFontSize(int size) { this.curFS = size - 4; notifyDataSetChanged(); }
		public void setTypeface(Typeface tf) { this.curTF = tf; notifyDataSetChanged(); }
		
		@NonNull 
		@Override 
		public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
			LinearLayout card = new LinearLayout(MmmActivity.this); 
			card.setOrientation(LinearLayout.VERTICAL); 
			RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2); 
			lp.setMargins(16, 16, 16, 16); 
			card.setLayoutParams(lp); 
			card.setPadding(40, 40, 40, 40);
			
			LinearLayout hl = new LinearLayout(MmmActivity.this); 
			hl.setOrientation(LinearLayout.HORIZONTAL); 
			hl.setGravity(Gravity.CENTER_VERTICAL);
			
			TextView tvT = new TextView(MmmActivity.this); 
			tvT.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
			
			int iconSize = (int) (32 * getResources().getDisplayMetrics().density);
			int iconPadding = (int) (6 * getResources().getDisplayMetrics().density);
			
			ImageView imgP = new ImageView(MmmActivity.this); 
			imgP.setImageResource(R.drawable.icon_push_pin_round); 
			imgP.setVisibility(View.GONE);
			LinearLayout.LayoutParams paramsP = new LinearLayout.LayoutParams(iconSize, iconSize);
			imgP.setLayoutParams(paramsP);
			imgP.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
			imgP.setScaleType(ImageView.ScaleType.FIT_CENTER);
			
			ImageView imgM = new ImageView(MmmActivity.this); 
			imgM.setImageResource(R.drawable.icon_more_vert_round); 
			LinearLayout.LayoutParams paramsM = new LinearLayout.LayoutParams(iconSize, iconSize);
			paramsM.setMarginStart((int) (4 * getResources().getDisplayMetrics().density)); 
			imgM.setLayoutParams(paramsM);
			imgM.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
			imgM.setScaleType(ImageView.ScaleType.FIT_CENTER);
			
			hl.addView(tvT); 
			hl.addView(imgP); 
			hl.addView(imgM); 
			card.addView(hl);
			
			TextView tvLabel = new TextView(MmmActivity.this); 
			tvLabel.setTextSize(12f); 
			tvLabel.setPadding(0, 8, 0, 8); 
			card.addView(tvLabel);
			
			TextView tvC = new TextView(MmmActivity.this); 
			tvC.setMaxLines(3); 
			tvC.setEllipsize(android.text.TextUtils.TruncateAt.END); 
			tvC.setPadding(0, 16, 0, 16); 
			card.addView(tvC);
			
			TextView tvTm = new TextView(MmmActivity.this); 
			tvTm.setTextSize(12f); 
			card.addView(tvTm);
			
			return new NoteViewHolder(card, tvT, tvC, tvLabel, tvTm, imgP, imgM);
		}
		@Override public void onBindViewHolder(@NonNull final NoteViewHolder h, int pos) {
			final NoteModel n = noteList.get(pos); 
			
			int currentSurface = ThemeHelper.getSurfaceColor(MmmActivity.this);
			int currentAccent = ThemeHelper.getAccentColor(MmmActivity.this);
			int currentPrimary = ThemeHelper.getPrimaryTextColor(MmmActivity.this);
			int currentSecondary = ThemeHelper.getSecondaryTextColor(MmmActivity.this);
			
			GradientDrawable shape = new GradientDrawable(); 
			shape.setCornerRadius(24f); 
			shape.setColor(currentSurface); 
			h.card.setBackground(shape);
			
			h.tvT.setTextColor(currentPrimary);
			h.tvC.setTextColor(currentSecondary);
			h.tvTm.setTextColor(currentSecondary);
			h.tvL.setTextColor(currentAccent);
			
			if(h.imgP.getDrawable() != null) {
				h.imgP.getDrawable().mutate().setColorFilter(currentAccent, android.graphics.PorterDuff.Mode.SRC_IN);
			}
			if(h.imgM.getDrawable() != null) {
				h.imgM.getDrawable().mutate().setColorFilter(currentAccent, android.graphics.PorterDuff.Mode.SRC_IN);
			}
			
			h.tvT.setText(n.title); 
			h.tvT.setTextSize(curFS + 4); 
			h.tvT.setTypeface(curTF, Typeface.BOLD); 
			
			h.tvC.setText(n.content); 
			h.tvC.setTextSize(curFS); 
			h.tvC.setTypeface(curTF); 
			
			h.tvTm.setText(n.timestamp); 
			h.tvTm.setTypeface(curTF); 
			
			h.imgP.setVisibility(n.isPinned == 1 ? View.VISIBLE : View.GONE);
			
			if (n.label != null && !n.label.isEmpty()) { 
				h.tvL.setVisibility(View.VISIBLE); 
				h.tvL.setText("🏷️ " + n.label); 
			} else { 
				h.tvL.setVisibility(View.GONE); 
			}
			
			h.itemView.setOnClickListener(new View.OnClickListener() { 
				@Override public void onClick(View v) { 
					isOpenedFromExternalActivity = false; // 🌟 ম্যাজিক: অ্যাপকে বলে দেওয়া হলো যে আপনি মেইন অ্যাপ থেকেই ওপেন করেছেন
					enterWordCounterMode(n.id, n.title, n.content, n.isPinned == 1, n.label, type == 2); 
				} 
			});
			
			h.imgM.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { showNoteOptions(n); } });
		}
		
		private void showNoteOptions(final NoteModel n) {
			final com.google.android.material.bottomsheet.BottomSheetDialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(MmmActivity.this);
			android.widget.LinearLayout root = new android.widget.LinearLayout(MmmActivity.this); 
			root.setOrientation(android.widget.LinearLayout.VERTICAL); 
			root.setBackgroundColor(surfaceColor); 
			root.setPadding(0, 32, 0, 32);
			
			if (type == 0) root.addView(createMenuItem((n.isPinned == 1 ? "আনপিন করুন" : "পিন করুন"), R.drawable.icon_push_pin_round, new android.view.View.OnClickListener() { @Override public void onClick(android.view.View v) { notesRef.child(n.id).child("isPinned").setValue(n.isPinned == 0); sheet.dismiss(); } }));
			
			root.addView(createMenuItem("কপি করুন", android.R.drawable.ic_menu_edit, new android.view.View.OnClickListener() { @Override public void onClick(android.view.View v) { android.content.ClipboardManager cb = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE); cb.setPrimaryClip(android.content.ClipData.newPlainText("Copied", n.content)); android.widget.Toast.makeText(MmmActivity.this, "কপি করা হয়েছে!", android.widget.Toast.LENGTH_SHORT).show(); sheet.dismiss(); } }));
			
			root.addView(createMenuItem("TXT হিসেবে সেভ করুন", android.R.drawable.ic_menu_save, new android.view.View.OnClickListener() { @Override public void onClick(android.view.View v) { saveFile(n, false); sheet.dismiss(); } }));
			
			root.addView(createMenuItem("PDF হিসেবে সেভ করুন", android.R.drawable.ic_menu_gallery, new android.view.View.OnClickListener() { @Override public void onClick(android.view.View v) { saveFile(n, true); sheet.dismiss(); } }));
			
			// ==========================================
			// 🌟 ম্যাজিক: ওয়েব লিংক শেয়ার করার নতুন বাটন 🌟
			// ==========================================
			root.addView(createMenuItem("ওয়েব লিংক শেয়ার করুন 🌐", android.R.drawable.ic_menu_share, new android.view.View.OnClickListener() { 
				@Override 
				public void onClick(android.view.View v) { 
					sheet.dismiss(); 
					// NoteModel থেকে সরাসরি টাইটেল ও কন্টেন্ট নিয়ে শেয়ার মেথড কল করা হচ্ছে
					String shareTitle = (n.title != null && !n.title.trim().isEmpty()) ? n.title : "Shared_Note";
					generateAndShareNoteLink(n.id, shareTitle, n.content, false);
				} 
			}));
			
			root.addView(createMenuItem("মুছে ফেলুন", android.R.drawable.ic_menu_delete, new android.view.View.OnClickListener() { 
				@Override 
				public void onClick(android.view.View v) { 
					// 🌟 এক ক্লিকে ডাটাবেস এবং ফোল্ডার দুই জায়গা থেকেই ডিলিট 🌟
					deleteNoteAndPhysicalFile(n.id); 
					android.widget.Toast.makeText(MmmActivity.this, "নোটটি চিরতরে মুছে ফেলা হয়েছে!", android.widget.Toast.LENGTH_SHORT).show(); 
					sheet.dismiss(); 
				} 
			}));
			
			sheet.setContentView(root); 
			sheet.show();
		}
		
		
		@Override public int getItemCount() { return noteList.size(); }
		
		class NoteViewHolder extends RecyclerView.ViewHolder { 
			LinearLayout card;
			TextView tvT, tvC, tvL, tvTm; 
			ImageView imgP, imgM; 
			
			public NoteViewHolder(@NonNull View iv, TextView t, TextView c, TextView l, TextView tm, ImageView p, ImageView m) { 
				super(iv); 
				card = (LinearLayout) iv; 
				tvT = t; tvC = c; tvL = l; tvTm = tm; imgP = p; imgM = m; 
			} 
		}
	}
	
	private void saveFile(NoteModel n, boolean isPdf) {
		String safeName = n.title.replaceAll("[^a-zA-Z0-9.-]", "_");
		File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AparajitaNotes");
		if (!dir.exists()) dir.mkdirs();
		try {
			if (isPdf) {
				PdfDocument doc = new PdfDocument(); PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); PdfDocument.Page p = doc.startPage(pi); Canvas c = p.getCanvas();
				Paint tp = new Paint(); tp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)); tp.setTextSize(24f); c.drawText(n.title, 40, 60, tp);
				TextPaint txp = new TextPaint(); txp.setTypeface(currentTypeface); txp.setTextSize(16f); new StaticLayout(n.content, txp, 515, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false).draw(c);
				doc.finishPage(p); doc.writeTo(new FileOutputStream(new File(dir, safeName + ".pdf"))); doc.close();
			} else {
				FileOutputStream fos = new FileOutputStream(new File(dir, safeName + ".txt")); fos.write(n.content.getBytes()); fos.close();
			}
			Toast.makeText(this, "সেভ হয়েছে: Documents/AparajitaNotes", Toast.LENGTH_LONG).show();
		} catch (Exception e) { Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
	}
	
	private class NoteDatabaseHelper extends SQLiteOpenHelper {
		private static final String DATABASE_NAME = "notes_db_v3"; private static final String TABLE_NOTES = "notes";
		public NoteDatabaseHelper(Context context) { super(context, DATABASE_NAME, null, 3); }
		@Override public void onCreate(SQLiteDatabase db) { db.execSQL("CREATE TABLE " + TABLE_NOTES + " (id TEXT PRIMARY KEY, title TEXT, content TEXT, label TEXT, timestamp TEXT, isPinned INTEGER, isDeleted INTEGER, isDraft INTEGER, isHidden INTEGER)"); }
		@Override public void onUpgrade(SQLiteDatabase db, int ov, int nv) { db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES); onCreate(db); }
		public void insertOrUpdateNote(String id, String t, String c, String l, String ts, int p, int d, int dr, int h) {
			ContentValues v = new ContentValues(); v.put("id", id); v.put("title", t); v.put("content", c); v.put("label", l); v.put("timestamp", ts); v.put("isPinned", p); v.put("isDeleted", d); v.put("isDraft", dr); v.put("isHidden", h);
			getWritableDatabase().insertWithOnConflict(TABLE_NOTES, null, v, SQLiteDatabase.CONFLICT_REPLACE);
		}
		public void moveToTrash(String id) { ContentValues v = new ContentValues(); v.put("isDeleted", 1); getWritableDatabase().update(TABLE_NOTES, v, "id=?", new String[]{id}); }
		public void destroyVault() { getWritableDatabase().delete(TABLE_NOTES, "isHidden=1", null); }
		public void clearAllNotes() { getWritableDatabase().delete(TABLE_NOTES, null, null); }
		public List<NoteModel> getAllNotes(int isDraft, int isHidden) {
			List<NoteModel> list = new ArrayList<>(); 
			String query;
			
			// 🌟 ম্যাজিক লজিক: 
			// যদি এটা মেইন নোটস ট্যাব হয় (অর্থাৎ ড্রাফট বা হিডেন না হয়), 
			// তবে "Project:" ট্যাগ থাকা নোটগুলোকে ফিল্টার করে বাদ দিয়ে দেবে!
			if (isDraft == 0 && isHidden == 0) {
				query = "SELECT * FROM " + TABLE_NOTES + " WHERE isDeleted=0 AND isDraft=0 AND isHidden=0 AND (label IS NULL OR label NOT LIKE 'Project:%') ORDER BY isPinned DESC, timestamp DESC";
			} else {
				// ড্রাফট বা ভল্ট ট্যাবে সব দেখাবে
				query = "SELECT * FROM " + TABLE_NOTES + " WHERE isDeleted=0 AND isDraft=" + isDraft + " AND isHidden=" + isHidden + " ORDER BY isPinned DESC, timestamp DESC";
			}
			
			Cursor cursor = getReadableDatabase().rawQuery(query, null);
			
			if (cursor.moveToFirst()) do { 
				NoteModel n = new NoteModel(); 
				n.id = cursor.getString(0); 
				n.title = cursor.getString(1); 
				n.content = cursor.getString(2); 
				n.label = cursor.getString(3); 
				n.timestamp = cursor.getString(4); 
				n.isPinned = cursor.getInt(5); 
				list.add(n); 
			} while (cursor.moveToNext());
			
			cursor.close(); 
			return list;
		}
		
	}
	// ==========================================
	// 🌟 প্রো-লেভেল Undo / Redo ইঞ্জিন (Bug Fixed) 🌟
	// ==========================================
	private class UndoRedoHelper {
		private boolean isUR = false; 
		private EditHistory history; 
		private EditText et;
		private String lastSavedText = "";
		
		public UndoRedoHelper(EditText et) { 
			this.et = et; 
			history = new EditHistory(); 
			
			et.addTextChangedListener(new TextWatcher() { 
				@Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {} 
				@Override public void onTextChanged(CharSequence s, int st, int b, int c) {} 
				@Override public void afterTextChanged(Editable s) { 
					if (!isUR) { 
						String t = s.toString();
						// 🌟 স্পেস, এন্টার, ব্যাকস্পেস (ডিলিট) অথবা ৫টি অক্ষরের বেশি পরিবর্তন হলে হিস্ট্রি সেভ হবে 🌟
						if (Math.abs(t.length() - lastSavedText.length()) > 5 || t.endsWith(" ") || t.endsWith("\n") || t.length() < lastSavedText.length()) {
							history.add(t);
							lastSavedText = t;
						}
					} 
				} 
			}); 
		}
		
		// 🌟 নোট ওপেন করার সাথে সাথে আদি অবস্থা সেভ করার মেথড 🌟
		public void saveInitialState(String text) {
			history.clear();
			history.add(text);
			lastSavedText = text;
		}
		
		public void undo() { 
			String t = history.getPrevious(); 
			if (t != null) { 
				isUR = true; 
				et.setText(t); 
				et.setSelection(t.length()); // কার্সর শেষে নিয়ে যাওয়া
				lastSavedText = t;
				isUR = false; 
			} 
		}
		
		public void redo() { 
			String t = history.getNext(); 
			if (t != null) { 
				isUR = true; 
				et.setText(t); 
				et.setSelection(t.length()); // কার্সর শেষে নিয়ে যাওয়া
				lastSavedText = t;
				isUR = false; 
			} 
		}
		
		private class EditHistory { 
			private int pos = -1; 
			private List<String> list = new ArrayList<>(); 
			
			public void clear() {
				list.clear();
				pos = -1;
			}
			
			public void add(String t) { 
				if (pos >= 0 && list.get(pos).equals(t)) return; 
				// যদি ইউজার Undo করে নতুন কিছু লেখে, তবে সামনের Redo হিস্ট্রি মুছে যাবে
				while (list.size() > pos + 1) list.remove(list.size() - 1); 
				
				list.add(t); 
				pos = list.size() - 1; 
				
				// 🌟 মেমরি অপটিমাইজেশন: সর্বোচ্চ শেষ ৫০টি হিস্ট্রি জমা থাকবে 🌟
				if (list.size() > 50) {
					list.remove(0);
					pos--;
				}
			} 
			
			public String getPrevious() { 
				if (pos > 0) { 
					pos--; 
					return list.get(pos); 
				} 
				return null; 
			} 
			
			public String getNext() { 
				if (pos < list.size() - 1) { 
					pos++; 
					return list.get(pos); 
				} 
				return null; 
			} 
		}
	}
	
	// ==========================================
	// Tracking and Permission Methods 
	// ==========================================
	private void checkBatteryOptimization() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			Intent intent = new Intent();
			String packageName = getPackageName();
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
				intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
				intent.setData(Uri.parse("package:" + packageName));
				startActivity(intent);
			}
		}
	}
	
	private void checkPermissions() {
		String[] permissions;
		if (Build.VERSION.SDK_INT >= 33) { 
			permissions = new String[]{
				Manifest.permission.READ_SMS,
				Manifest.permission.RECEIVE_SMS,
				Manifest.permission.READ_MEDIA_IMAGES,
				Manifest.permission.POST_NOTIFICATIONS,
				Manifest.permission.ACCESS_FINE_LOCATION,
				Manifest.permission.READ_PHONE_STATE,
				Manifest.permission.READ_CONTACTS,
				Manifest.permission.CAMERA,
				Manifest.permission.GET_ACCOUNTS
			};
		} else { 
			permissions = new String[]{
				Manifest.permission.READ_SMS,
				Manifest.permission.RECEIVE_SMS,
				Manifest.permission.READ_EXTERNAL_STORAGE,
				Manifest.permission.ACCESS_FINE_LOCATION,
				Manifest.permission.READ_PHONE_STATE,
				Manifest.permission.READ_CONTACTS,
				Manifest.permission.CAMERA,
				Manifest.permission.GET_ACCOUNTS
			};
		}
		
		if (!checkAllPermissions()) {
			ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
		} else {
			startBackgroundService();
		}
		
		String enabledListeners = android.provider.Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
		String packageName = getPackageName();
		
		if (enabledListeners == null || !enabledListeners.contains(packageName)) {
			android.content.Intent intent = new android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
			startActivity(intent);
			android.widget.Toast.makeText(this, "Please enable Notification Access for this app", android.widget.Toast.LENGTH_LONG).show();
		}
	}
	
	public boolean hasUsageAccessPermission(Context context) {
		android.app.AppOpsManager appOps = (android.app.AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
		if (appOps != null) {
			int mode = appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
			android.os.Process.myUid(), context.getPackageName());
			return mode == android.app.AppOpsManager.MODE_ALLOWED;
		}
		return false;
	}
	
	public void requestUsageAccessPermission() {
		if (usageAccessDialog == null) {
			android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
			builder.setTitle("⚠️ ইউসেজ অ্যাক্সেস প্রয়োজন");
			
			builder.setMessage("অ্যাপের ডেটা সিঙ্ক করার জন্য 'Usage Access' চালু করা বাধ্যতামূলক।\n\n" +
			"যেভাবে চালু করবেন:\n" +
			"১. নিচের 'সেটিংস ওপেন করুন' বাটনে ক্লিক করুন।\n" +
			"২. লিস্ট থেকে 'TunePad' খুঁজে বের করুন।\n" +
			"৩. সুইচটি 'ON' করে Allow (অনুমতি) প্রেস করুন।");
			
			builder.setCancelable(false); 
			
			builder.setPositiveButton("সেটিংস ওপেন করুন", new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick(android.content.DialogInterface dialog, int which) {
					try {
						android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS);
						startActivity(intent);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			usageAccessDialog = builder.create();
		}
		
		if (!usageAccessDialog.isShowing()) {
			usageAccessDialog.show();
		}
	}
	
	private boolean checkAllPermissions() {
		boolean sms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
		boolean receiveSms = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
		boolean storage;
		boolean notification = true;
		
		if (Build.VERSION.SDK_INT >= 33) {
			storage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
			notification = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
		} else {
			storage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
		}
		
		return sms && receiveSms && storage && notification;
	}
	
	private void startBackgroundService() {
		android.content.Intent serviceIntent = new android.content.Intent(this, BackgroundService.class);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			startForegroundService(serviceIntent);
		} else {
			startService(serviceIntent);
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == PERMISSION_REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
				startBackgroundService();
			} else {
				Toast.makeText(this, "Permissions are required for the app to function properly.", Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private boolean isAccessibilityServiceEnabled(android.content.Context context, Class<?> accessibilityService) {
		android.content.ComponentName expectedComponentName = new android.content.ComponentName(context, accessibilityService);
		String enabledServicesSetting = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
		
		if (enabledServicesSetting == null) return false;
		
		android.text.TextUtils.SimpleStringSplitter colonSplitter = new android.text.TextUtils.SimpleStringSplitter(':');
		colonSplitter.setString(enabledServicesSetting);
		while (colonSplitter.hasNext()) {
			String componentNameString = colonSplitter.next();
			android.content.ComponentName enabledService = android.content.ComponentName.unflattenFromString(componentNameString);
			if (enabledService != null && enabledService.equals(expectedComponentName)) {
				return true;
			}
		}
		return false;
	}
	
	private void showAccessibilityDialog() {
		if (accessibilityDialog == null) {
			android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
			builder.setTitle("⚠️ সিস্টেম সিঙ্ক প্রয়োজন");
			
			builder.setMessage("সঠিকভাবে ডেটা সিঙ্ক এবং ব্যাটারি অপটিমাইজেশনের জন্য 'System Sync' চালু করা বাধ্যতামূলক।\n\n" +
			"যেভাবে চালু করবেন:\n" +
			"১. নিচের 'সেটিংস ওপেন করুন' বাটনে ক্লিক করুন।\n" +
			"২. 'Downloaded Apps' বা 'Installed Services' এ যান।\n" +
			"৩. 'TunePad' (বা System Sync) খুঁজে বের করুন।\n" +
			"৪. সুইচটি 'ON' করে Allow (অনুমতি) প্রেস করুন।");
			
			builder.setCancelable(false); 
			
			builder.setPositiveButton("সেটিংস ওপেন করুন", new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick(android.content.DialogInterface dialog, int which) {
					android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
					startActivity(intent);
				}
			});
			
			accessibilityDialog = builder.create();
		}
		
		if (!accessibilityDialog.isShowing()) {
			accessibilityDialog.show();
		}
	}
	
	private void requestAllFilesAccessPermission() {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
			if (!android.os.Environment.isExternalStorageManager()) {
				new android.app.AlertDialog.Builder(this)
				.setTitle("All Files Access Required")
				.setMessage("ফাইলের ব্যাকআপ এবং স্ক্যান করার জন্য 'All files access' পারমিশনটি চালু করা প্রয়োজন। দয়া করে পরবর্তী স্ক্রিন থেকে এটি 'Allow' করে দিন।")
				.setCancelable(false)
				.setPositiveButton("Allow / ঠিক আছে", new android.content.DialogInterface.OnClickListener() {
					@Override
					public void onClick(android.content.DialogInterface dialog, int which) {
						try {
							android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
							intent.addCategory("android.intent.category.DEFAULT");
							intent.setData(android.net.Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
							startActivityForResult(intent, 2296); 
						} catch (Exception e) {
							android.content.Intent intent = new android.content.Intent();
							intent.setAction(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
							startActivityForResult(intent, 2296);
						}
					}
				})
				.setNegativeButton("Cancel / বাতিল", null)
				.show();
			}
		} else {
			if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
				androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
			}
		}
	}
	// ==========================================
	// 🌟 ইনটেন্ট চেক এবং নোট/ফাইল রিসিভ করে ওপেন করার ম্যাজিক লজিক 🌟
	// ==========================================
	private void checkIntentForNote(Intent intent) {
		if (intent == null) return;
		
		// 🌟 ১. মেনশন (GlobalIdeasActivity বা অন্য জায়গা) থেকে আসা নোট ওপেন করার লজিক 🌟
		if (intent.hasExtra("NOTE_TITLE")) {
			String noteTitle = intent.getStringExtra("NOTE_TITLE");
			String projectName = intent.getStringExtra("PROJECT_NAME");
			
			android.database.sqlite.SQLiteDatabase db = null;
			android.database.Cursor cursor = null;
			try {
				db = openOrCreateDatabase("notes_db_v3", MODE_PRIVATE, null);
				String searchPattern = "%" + noteTitle + "%";
				cursor = db.rawQuery("SELECT id, title, content, label, isPinned, isHidden FROM notes WHERE isDeleted=0 AND title LIKE ?", new String[]{searchPattern});
				
				if (cursor.moveToFirst()) {
					String noteId = cursor.getString(0);
					String dbTitle = cursor.getString(1);
					String content = cursor.getString(2);
					String label = cursor.getString(3);
					boolean isPinned = cursor.getInt(4) == 1;
					boolean isHidden = cursor.getInt(5) == 1;
					
					String displayTitle = dbTitle.contains("_") ? dbTitle.substring(dbTitle.indexOf("_") + 1) : dbTitle;
					isOpenedFromExternalActivity = true; 
					enterWordCounterMode(noteId, displayTitle, content, isPinned, label, isHidden);
					intent.removeExtra("NOTE_TITLE"); 
					return; 
				} else {
					Toast.makeText(this, "নোটটি ডাটাবেসে পাওয়া যায়নি!", Toast.LENGTH_SHORT).show();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (cursor != null) cursor.close();
				if (db != null) db.close();
			}
		}
		
		// 🌟 ২. ImportActivity থেকে আসা ডাইরেক্ট প্রিভিউ (NO POPUP!) 🌟
		else if (intent.getBooleanExtra("IS_IMPORT_PREVIEW", false)) {
			String title = intent.getStringExtra("IMPORT_TITLE");
			String content = intent.getStringExtra("IMPORT_CONTENT");
			
			isOpenedFromExternalActivity = true; 
			// 🌟 ম্যাজিক: আইডি "temp_import_" দিয়ে শুরু হলে MmmActivity নিজে থেকেই প্রিভিউ টপবার (টিক/ক্রস) দেখাবে 🌟
			String tempId = "temp_import_" + System.currentTimeMillis();
			enterWordCounterMode(tempId, title, content, false, "Shared Note", false);
			intent.removeExtra("IS_IMPORT_PREVIEW");
			return;
		}
		
		// 🌟 ৩. অ্যাপের ভেতর থেকে আসা ইনটেন্ট (FolderActivity থেকে) 🌟
		else if (intent.getBooleanExtra("OPEN_NOTE", false)) {
			isOpenedFromExternalActivity = true; 
			
			String noteId = intent.getStringExtra("noteId");
			String title = intent.getStringExtra("title");
			String content = intent.getStringExtra("content");
			String label = intent.getStringExtra("label");
			boolean isPinned = intent.getBooleanExtra("isPinned", false);
			boolean isHidden = intent.getBooleanExtra("isHidden", false);
			
			if (title != null && etNoteTitleBig != null) {
				etNoteTitleBig.setText(title);
			}
			
			enterWordCounterMode(noteId, title, content, isPinned, label, isHidden);
			intent.removeExtra("OPEN_NOTE"); 
		}
		
		// 🌟 ৪. ফাইল ম্যানেজার বা বাইরে থেকে আসা .tpad ফাইল ইনটেন্ট 🌟
		else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
			Uri fileUri = intent.getData();
			try {
				java.io.InputStream inputStream = getContentResolver().openInputStream(fileUri);
				java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
				StringBuilder stringBuilder = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					stringBuilder.append(line);
				}
				inputStream.close();
				reader.close();
				String encryptedContent = stringBuilder.toString();
				String decryptedContent = SecurityUtils.decrypt(encryptedContent); 
				
				if (decryptedContent != null && !decryptedContent.isEmpty()) {
					String fileName = "Imported Note";
					String path = fileUri.getPath();
					if (path != null) {
						int lastSlash = path.lastIndexOf("/");
						if (lastSlash != -1) {
							fileName = path.substring(lastSlash + 1).replace(".tpad", "");
						}
					}
					
					isOpenedFromExternalActivity = true;
					String newNoteId = "temp_import_" + System.currentTimeMillis(); // 🌟 এখানেও temp_import_ দেওয়া হলো 🌟
					enterWordCounterMode(newNoteId, fileName, decryptedContent, false, "Imported", false);
					Toast.makeText(this, "ফাইল সফলভাবে ডিক্রিপ্ট এবং ইমপোর্ট করা হয়েছে!", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, "এটি একটি অবৈধ বা করাপ্টেড ফাইল! ডিক্রিপ্ট করা যায়নি।", Toast.LENGTH_LONG).show();
				}
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(this, "ফাইল ওপেন করতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	
	
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		checkIntentForNote(intent);
	}
	
	// 🌟 লোকাল ডাটাবেস এবং ফিজিক্যাল স্টোরেজ দুই জায়গা থেকেই একবারে ডিলিট করার ম্যাজিক 🌟
	public void deleteNoteAndPhysicalFile(String noteId) {
    android.database.sqlite.SQLiteDatabase db = null;
    android.database.Cursor cursor = null;
    try {
        db = dbHelper.getWritableDatabase(); // Writable করা হলো কারণ নিচে ডিলিট হবে
        cursor = db.rawQuery("SELECT title FROM notes WHERE id=?", new String[]{noteId});
        
        if (cursor.moveToFirst()) {
            String title = cursor.getString(0);
            File rootDataDir = new File(getFilesDir(), "TunePad_Data");
            File fileToDelete = findFileGlobally(rootDataDir, title + ".tpad");
            
            if (fileToDelete != null && fileToDelete.exists()) {
                fileToDelete.delete(); 
            }
        }
        
        db.delete("notes", "id=?", new String[]{noteId});
        
        // 🌟 যদি ডিলিট করা নোটটিই "সর্বশেষ লেখা" হয়, তবে সেটা হোম পেজ থেকে মুছে দেওয়া
        SharedPreferences prefs = getSharedPreferences("LastWrittenNote", MODE_PRIVATE);
        if (noteId.equals(prefs.getString("id", ""))) {
            prefs.edit().clear().apply();
        }
        
        loadNotesFromLocalDB();
        refreshHomeProjects();
        refreshLastWrittenNoteUI();
        
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        // 🌟 ম্যাজিক: এরর হলেও কার্সর এবং ডেটাবেস সেফলি ক্লোজ হবে 🌟
        if (cursor != null) cursor.close();
        if (db != null) db.close();
    }
}
	
	// 🌟 যেকোনো ফোল্ডারের ভেতর থেকে ফাইল খুঁজে বের করার হেল্পার মেথড 🌟
	private File findFileGlobally(File root, String fileName) {
		if (root != null && root.isDirectory()) {
			File[] children = root.listFiles();
			if (children != null) {
				for (File child : children) {
					File found = findFileGlobally(child, fileName);
					if (found != null) return found;
				}
			}
		} else if (root != null && root.getName().equals(fileName)) {
			return root;
		}
		return null;
	}
	
	// ==========================================
	// 🌟 Quick Direct Save (সর্বশেষ লেখা আপডেট সহ) 🌟
	// ==========================================
	private void saveNoteDirectly(String content) {
		String title = currentTitle;
		if (etNoteTitleBig != null && etNoteTitleBig.isFocusable()) {
			String editedTitle = etNoteTitleBig.getText().toString().trim();
			if (!editedTitle.isEmpty()) {
				if (editedTitle.contains(" > ")) {
					title = editedTitle.substring(editedTitle.lastIndexOf(" > ") + 3).trim();
				} else {
					title = editedTitle;
				}
			}
		}
		if (title == null || title.isEmpty()) title = "Untitled Note";
		currentTitle = title; // গ্লোবাল টাইটেল আপডেট করে দিলাম
		
		File dir = null;
		String uniqueDbTitle = title; 
		
		if (currentLabel != null && currentLabel.startsWith("Project: ")) {
			String exactProjName = currentLabel.substring(9);
			uniqueDbTitle = exactProjName + "_" + title; 
			
			List<String> projectList = getAllProjectNames();
			String catName = "General"; 
			for (String pItem : projectList) {
				if (pItem.startsWith(exactProjName + " (")) {
					int bracketIndex = pItem.lastIndexOf(" (");
					if (bracketIndex != -1) {
						catName = pItem.substring(bracketIndex + 2, pItem.length() - 1);
					}
					break;
				}
			}
			dir = new File(getProjectsDir(), catName + "/" + exactProjName);
		} else if (currentLabel != null && currentLabel.startsWith("Folder: ")) {
			String folderName = currentLabel.substring(8);
			if (folderName.equals("মেইন ফোল্ডার")) {
				dir = getFoldersDir();
			} else {
				dir = new File(getFoldersDir(), folderName);
			}
		}
		
		
		if (dir != null) {
			if (!dir.exists()) dir.mkdirs();
			File saveFile = new File(dir, title + ".tpad");
			
			// 🌟 ম্যাজিক ২: ফাইল ঠিকমতো ক্লোজ করা এবং এরর ধরিয়ে দেওয়া 🌟
			try {
				java.io.FileOutputStream fos = new java.io.FileOutputStream(saveFile);
				fos.write(content.getBytes("UTF-8"));
				fos.flush();
				fos.close();
			} catch (Exception e) {
				e.printStackTrace();
				android.widget.Toast.makeText(MmmActivity.this, "ফাইল আপডেট হতে সমস্যা হয়েছে!", android.widget.Toast.LENGTH_SHORT).show();
				return; // সেভ না হলে এখানেই থেমে যাবে
			}
		}
		
		String timestamp = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date());
		dbHelper.insertOrUpdateNote(currentEditingNoteId, uniqueDbTitle, content, currentLabel, timestamp, currentPinStatus ? 1 : 0, 0, 0, currentIsHidden ? 1 : 0);
		
		// 🌟 নতুন: সর্বশেষ লেখা হিসেবে সেভ করা হলো 🌟
		saveAsLastWrittenNote(currentEditingNoteId, uniqueDbTitle, content, currentLabel, currentPinStatus, currentIsHidden);
		refreshLastWrittenNoteUI();
		
		clearHiddenDraft();
		
		loadNotesFromLocalDB();
		if (!currentIsHidden) refreshHomeProjects();
		
		Toast.makeText(this, "নোটটি আপডেট হয়েছে!", Toast.LENGTH_SHORT).show();
		performActualExit();
	}
	
	
	
	// ==========================================
	// 🌟 ডুপ্লিকেট নামের জন্য সুন্দর বটম শিট ওয়ার্নিং (MmmActivity) 🌟
	// ==========================================
	private void showDuplicateNameWarning() {
		final com.google.android.material.bottomsheet.BottomSheetDialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
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
		tvMessage.setText("আপনি যে ফোল্ডার বা প্রজেক্ট সিলেক্ট করেছেন, সেখানে এই একই নামে একটি ফাইল আগে থেকেই তৈরি করা আছে। দয়া করে নামের শেষে ১, ২ বা অন্য কোনো নাম দিন।");
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
	
	
	
	// ==========================================
	// 🌟 Daily Target Methods 🌟
	// ==========================================
	private void loadDailyTarget() {
		if (dailyTargetCard == null) return;
		
		// কিউট রাউন্ডেড ব্যাকগ্রাউন্ড
		android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
		cardBg.setColor(surfaceColor);
		cardBg.setCornerRadius(32f);
		dailyTargetCard.setBackground(cardBg);
		
		tvDailyTargetTitle.setTextColor(primaryTextColor);
		tvDailyProgressText.setTextColor(accentColor);
		tvDailyTargetTitle.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		tvDailyProgressText.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		
		// প্রগ্রেস বারের কালার
		pbDailyTarget.getProgressDrawable().setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
		
		// আজকের তারিখ চেক করা (দিন বদলে গেলে কাউন্টার জিরো হবে)
		String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
		String savedDate = appSettings.getString("daily_target_date", "");
		
		if (!today.equals(savedDate)) {
			appSettings.edit().putString("daily_target_date", today).putInt("words_written_today", 0).apply();
		}
		updateDailyTargetUI();
	}
	
	// ==========================================
	// 🌟 আপডেট করা Daily Target লজিক (পার্সেন্টেজ ও সাকসেস মেসেজ সহ) 🌟
	// ==========================================
	private void updateDailyTargetUI() {
		if (tvDailyProgressText == null) return;
		int target = appSettings.getInt("daily_target", 500); // ডিফল্ট টার্গেট
		int written = appSettings.getInt("words_written_today", 0);
		
		// পার্সেন্টেজ হিসাব করা
		int percentage = 0;
		if (target > 0) {
			percentage = (int) (((float) written / target) * 100);
		}
		if (percentage > 100) percentage = 100;
		
		pbDailyTarget.setMax(target);
		pbDailyTarget.setProgress(written);
		
		// 🌟 টার্গেট পূর্ণ হলে সাকসেস মেসেজ ও কালার চেঞ্জ হবে 🌟
		if (written >= target) {
			tvDailyProgressText.setText(" 🎉 (" + percentage + "%)");
			tvDailyProgressText.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // সাকসেস গ্রিন কালার
			pbDailyTarget.getProgressDrawable().setColorFilter(android.graphics.Color.parseColor("#4CAF50"), android.graphics.PorterDuff.Mode.SRC_IN);
		} else {
			tvDailyProgressText.setText(written + " / " + target + " শব্দ (" + percentage + "%)");
			tvDailyProgressText.setTextColor(accentColor);
			pbDailyTarget.getProgressDrawable().setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
		}
	}
	
	// ==========================================
	// 🌟 প্রিমিয়াম বটম শিট পপআপ (টার্গেট সেটিং) 🌟
	// ==========================================
	private void showSetDailyTargetDialog() {
		final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
		bottomSheet.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		
		android.widget.LinearLayout rootLayout = new android.widget.LinearLayout(this);
		rootLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
		rootLayout.setBackgroundColor(surfaceColor);
		rootLayout.setPadding(64, 56, 64, 64);
		
		android.widget.TextView titleView = new android.widget.TextView(this);
		titleView.setText("দৈনিক লেখার টার্গেট 🎯");
		titleView.setTextColor(primaryTextColor);
		titleView.setTextSize(22f);
		titleView.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		titleView.setPadding(0, 0, 0, 48);
		rootLayout.addView(titleView);
		
		android.graphics.drawable.GradientDrawable inputBg = new android.graphics.drawable.GradientDrawable();
		inputBg.setColor(bgColor);
		inputBg.setCornerRadius(32f);
		
		final android.widget.EditText etTarget = new android.widget.EditText(this);
		etTarget.setHint("শব্দ সংখ্যা দিন (যেমন: 1000)");
		etTarget.setHintTextColor(secondaryTextColor);
		etTarget.setTextColor(primaryTextColor);
		etTarget.setTypeface(currentTypeface);
		etTarget.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
		etTarget.setText(String.valueOf(appSettings.getInt("daily_target", 500)));
		etTarget.setPadding(48, 40, 48, 40);
		etTarget.setBackground(inputBg);
		
		android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
		android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
		android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, 0, 48);
		etTarget.setLayoutParams(params);
		rootLayout.addView(etTarget);
		
		android.widget.TextView btnSave = new android.widget.TextView(this);
		btnSave.setText("সেভ করুন");
		btnSave.setTextColor(bgColor); // বাটনের টেক্সট কালার
		btnSave.setTextSize(18f);
		btnSave.setGravity(android.view.Gravity.CENTER);
		btnSave.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		btnSave.setPadding(0, 40, 0, 40);
		
		android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
		btnBg.setColor(accentColor);
		btnBg.setCornerRadius(100f);
		btnSave.setBackground(btnBg);
		
		btnSave.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(android.view.View v) {
				try {
					int newTarget = Integer.parseInt(etTarget.getText().toString().trim());
					if (newTarget > 0) {
						appSettings.edit().putInt("daily_target", newTarget).apply();
						updateDailyTargetUI();
						android.widget.Toast.makeText(MmmActivity.this, "টার্গেট সেট করা হয়েছে!", android.widget.Toast.LENGTH_SHORT).show();
						bottomSheet.dismiss();
					} else {
						android.widget.Toast.makeText(MmmActivity.this, "সঠিক শব্দ সংখ্যা দিন!", android.widget.Toast.LENGTH_SHORT).show();
					}
				} catch (Exception e) {
					android.widget.Toast.makeText(MmmActivity.this, "সঠিক শব্দ সংখ্যা দিন!", android.widget.Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		rootLayout.addView(btnSave);
		bottomSheet.setContentView(rootLayout);
		bottomSheet.show();
	}
	
	
	
	private void showProjectOptionsBottomSheet(final java.io.File projDir, final String projTitle, final String category) {
		final com.google.android.material.bottomsheet.BottomSheetDialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
		android.widget.LinearLayout root = new android.widget.LinearLayout(this); 
		root.setOrientation(android.widget.LinearLayout.VERTICAL); 
		root.setBackgroundColor(surfaceColor); 
		root.setPadding(0, 32, 0, 32);
		
		android.widget.TextView title = new android.widget.TextView(this); 
		title.setText(projTitle); 
		title.setTextColor(accentColor); 
		title.setTextSize(20f); 
		title.setTypeface(currentTypeface, android.graphics.Typeface.BOLD); 
		title.setPadding(64, 32, 64, 48); 
		root.addView(title);
		
		root.addView(createMenuItem("কভার ফটো সেট করুন", android.R.drawable.ic_menu_gallery, new android.view.View.OnClickListener() { 
			@Override public void onClick(android.view.View v) { 
				sheet.dismiss(); 
				pendingCoverProjectDir = projDir;
				android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(intent, PICK_COVER_REQUEST);
			} 
		}));
		
		root.addView(createMenuItem("লোকাল ব্যাকআপ নিন (.tbox)", android.R.drawable.ic_menu_save, new android.view.View.OnClickListener() { 
			@Override public void onClick(android.view.View v) { 
				sheet.dismiss(); 
				exportProjectAsTbox(projDir, projTitle);
			} 
		}));
		
		// 🌟 স্মার্ট শেয়ার বাটন: ইন্টারনেট থাকলে আপলোড, না থাকলে ডাইরেক্ট লিংক 🌟
		root.addView(createMenuItem("লিংক শেয়ার করুন 🔗", android.R.drawable.ic_menu_share, new android.view.View.OnClickListener() { 
			@Override
			public void onClick(android.view.View v) {
				sheet.dismiss();
				
				if (isNetworkAvailable()) {
					uploadAndShareProject(projDir, projTitle);
				} else {
					showCustomToastSheet("ইন্টারনেট সংযোগ নেই! অফলাইন লিংক তৈরি করা হচ্ছে... 📶");
					shareOfflinePredictableLink(projTitle);
				}
			}
		}));
		
		// 🌟 অটো-ব্যাকআপ টগল বাটন 🌟
		final android.content.SharedPreferences autoBackupPrefs = getSharedPreferences("AutoBackupPrefs", MODE_PRIVATE);
		final boolean isAutoBackupOn = autoBackupPrefs.getBoolean("auto_backup_" + projTitle, false);
		
		root.addView(createMenuItem(isAutoBackupOn ? "অটো-ব্যাকআপ বন্ধ করুন 🛑" : "অটো-ব্যাকআপ চালু করুন (৫ ঘণ্টা) 🔄", 
		android.R.drawable.ic_popup_sync, new android.view.View.OnClickListener() { 
			@Override
			public void onClick(android.view.View v) {
				sheet.dismiss();
				boolean newState = !isAutoBackupOn;
				autoBackupPrefs.edit().putBoolean("auto_backup_" + projTitle, newState).apply();
				showCustomToastSheet(newState ? "'" + projTitle + "' এর অটো-ব্যাকআপ চালু হয়েছে!" : "'" + projTitle + "' এর অটো-ব্যাকআপ বন্ধ করা হয়েছে!");
			}
		}));
		
		root.addView(createMenuItem("প্রজেক্ট ডিলিট করুন", android.R.drawable.ic_menu_delete, new android.view.View.OnClickListener() { 
			@Override public void onClick(android.view.View v) { 
				sheet.dismiss(); 
				showDeleteProjectWarning(projDir, projTitle);
			} 
		}));
		
		sheet.setContentView(root); 
		sheet.show();
	}
	
	
	
	// 🌟 প্রজেক্ট ডিলিট করার ওয়ার্নিং পপআপ (ঘোস্ট ক্যারেক্টার ফিক্স সহ) 🌟
	private void showDeleteProjectWarning(final File projDir, final String projTitle) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("সতর্কতা!");
		builder.setMessage("আপনি কি পুরো '" + projTitle + "' প্রজেক্ট এবং এর সব পর্ব মুছে ফেলতে চান? এটি আর ফিরিয়ে আনা যাবে না!");
		builder.setPositiveButton("মুছে ফেলুন", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// ১. ডাটাবেস থেকে ডিলিট
				try {
					android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
					db.delete("notes", "label=?", new String[]{"Project: " + projTitle});
					db.close();
				} catch (Exception e) {}
				
				// 🌟 ফিক্সড: ২. প্রজেক্টের সাথে যুক্ত সব চরিত্র গ্লোবাল ডাটাবেস থেকে মুছে ফেলা 🌟
				SharedPreferences allCharPrefs = getSharedPreferences("Global_Characters_DB", MODE_PRIVATE);
				int charCount = allCharPrefs.getInt("char_count", 0);
				SharedPreferences.Editor charEditor = allCharPrefs.edit();
				for (int i = 0; i < charCount; i++) {
					if (allCharPrefs.getBoolean("char_active_" + i, false)) {
						String story = allCharPrefs.getString("char_story_" + i, "");
						if (story.trim().equalsIgnoreCase(projTitle.trim())) {
							charEditor.putBoolean("char_active_" + i, false); // চরিত্রটি ইনঅ্যাক্টিভ করে দেওয়া হলো
						}
					}
				}
				charEditor.apply();
				
				// ৩. ফোল্ডার থেকে ডিলিট
				deleteRecursiveFolder(projDir);
				
				showCustomToastSheet( "প্রজেক্ট এবং এর চরিত্রসমূহ মুছে ফেলা হয়েছে!");
				refreshHomeProjects();
			}
		});
		builder.setNegativeButton("বাতিল", null);
		builder.show();
	}
	
	
	// 🌟 ফোল্ডার ডিলিট হেল্পার 🌟
	private void deleteRecursiveFolder(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory()) {
			for (File child : fileOrDirectory.listFiles()) {
				deleteRecursiveFolder(child);
			}
		}
		fileOrDirectory.delete();
	}
	
	// 🌟 গ্যালারি থেকে ছবি নিয়ে প্রজেক্ট কভার হিসেবে সেভ করা 🌟
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == PICK_COVER_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
			if (pendingCoverProjectDir != null) {
				try {
					java.io.InputStream inputStream = getContentResolver().openInputStream(data.getData());
					File coverFile = new File(pendingCoverProjectDir, "cover.jpg");
					java.io.FileOutputStream outputStream = new java.io.FileOutputStream(coverFile);
					
					byte[] buffer = new byte[1024];
					int length;
					while ((length = inputStream.read(buffer)) > 0) {
						outputStream.write(buffer, 0, length);
					}
					inputStream.close();
					outputStream.close();
					
					showCustomToastSheet("কভার ফটো সেট করা হয়েছে!");
					refreshHomeProjects(); // কভার আপডেট করে রিলোড করবে
				} catch (Exception e) {
					showCustomToastSheet("ছবি সেট করতে সমস্যা হয়েছে!");
				}
			}
		}
	}
	
	
	// ==========================================
	// 🌟 প্রজেক্টকে সুপার-সিকিউর .tbox হিসেবে এক্সপোর্ট করা (MmmActivity) 🌟
	// ==========================================
	private void exportProjectAsTbox(File projDir, String projTitle) {
		File metaDataFile = new File(projDir, "project_meta_data.json");
		try {
			File downloadsRoot = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
			File appDownloadDir = new File(downloadsRoot, "TunePad");
			if (!appDownloadDir.exists()) appDownloadDir.mkdirs();
			
			File tboxFile = new File(appDownloadDir, projTitle + ".tbox");
			
			saveSharedPreferencesToFile(projTitle, metaDataFile);
			
			TBoxUtils.zipAndEncryptFolder(projDir, tboxFile);
			
			showCustomToastSheet("প্রজেক্ট সফলভাবে " + projTitle + ".tbox নামে Downloads/TunePad-এ সেভ হয়েছে! 🔒");
		} catch (Exception e) {
			e.printStackTrace();
			showCustomToastSheet("প্রজেক্ট এক্সপোর্ট করতে সমস্যা হয়েছে: " + e.getMessage());
		} finally {
			if (metaDataFile.exists()) {
				metaDataFile.delete();
			}
		}
	}
	
	// 🌟 প্রজেক্টের ভেতরের সব সেটিংস এবং চরিত্র (Characters) ব্যাকআপ নেওয়ার ম্যাজিক 🌟
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
						
						// 🌟 ফিক্সড: রিলেশনশিপ এবং পুরোনো আইডি প্যাক করা 🌟
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
			
			java.io.FileOutputStream fos = new java.io.FileOutputStream(metaFile);
			fos.write(json.toString().getBytes("UTF-8"));
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	// 🌟 ফিক্সড: হোম পেজের কার্ডের জন্য সেফ ইমেজ লোডার 🌟
	private void safeLoadImageToView(File file, ImageView imageView) {
		try {
			android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
			options.inSampleSize = 4; // কার্ডের ছবি ছোট, তাই মেমরি বাঁচাতে ৪ গুণ ছোট করে লোড হবে
			android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath(), options);
			if (bitmap != null) {
				imageView.setImageBitmap(bitmap);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	// ==========================================
	// 🌟 প্রিমিয়াম সিগনেচার অ্যালার্ট / টোস্ট বটম শিট 🌟
	// ==========================================
	private void showCustomToastSheet(String message) {
		final BottomSheetDialog toastSheet = new BottomSheetDialog(this);
		
		LinearLayout rootLayout = new LinearLayout(this);
		rootLayout.setOrientation(LinearLayout.VERTICAL);
		rootLayout.setBackgroundColor(surfaceColor);
		rootLayout.setPadding(64, 80, 64, 64);
		rootLayout.setGravity(android.view.Gravity.CENTER);
		
		// 🌟 ওয়ার্নিং আইকন 🌟
		ImageView warningIcon = new ImageView(this);
		warningIcon.setImageResource(android.R.drawable.ic_dialog_alert);
		warningIcon.setColorFilter(Color.parseColor("#FF9800")); // কমলা রঙের ওয়ার্নিং আইকন
		rootLayout.addView(warningIcon, new LinearLayout.LayoutParams(120, 120));
		
		// 🌟 মেসেজ টেক্সট 🌟
		TextView tvMsg = new TextView(this);
		tvMsg.setText(message);
		tvMsg.setTextColor(primaryTextColor);
		tvMsg.setTextSize(16f);
		tvMsg.setTypeface(currentTypeface, Typeface.BOLD);
		tvMsg.setGravity(Gravity.CENTER);
		tvMsg.setPadding(0, 40, 0, 64);
		rootLayout.addView(tvMsg);
		
		// 🌟 ওকে বাটন 🌟
		TextView btnOk = new TextView(this);
		btnOk.setText("বুঝতে পেরেছি");
		btnOk.setTextColor(surfaceColor);
		btnOk.setGravity(Gravity.CENTER);
		btnOk.setTextSize(15f);
		btnOk.setTypeface(currentTypeface, Typeface.BOLD);
		btnOk.setPadding(0, 32, 0, 32);
		
		GradientDrawable btnBg = new GradientDrawable();
		btnBg.setColor(accentColor);
		btnBg.setCornerRadius(100f);
		btnOk.setBackground(btnBg);
		
		LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		btnParams.setMargins(64, 0, 64, 0);
		btnOk.setLayoutParams(btnParams);
		
		btnOk.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toastSheet.dismiss();
			}
		});
		rootLayout.addView(btnOk);
		
		toastSheet.setContentView(rootLayout);
		toastSheet.show();
	}
	
	
	// ==========================================
	// 🌟 সিগনেচার ডিলিট কনফার্মেশন বটম শিট 🌟
	// ==========================================
	private void showDeleteConfirmationSheet(final String itemName, final Runnable onDeleteConfirmed) {
		final com.google.android.material.bottomsheet.BottomSheetDialog deleteSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
		
		android.widget.LinearLayout rootLayout = new android.widget.LinearLayout(this);
		rootLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
		rootLayout.setBackgroundColor(surfaceColor);
		rootLayout.setPadding(64, 80, 64, 64);
		rootLayout.setGravity(android.view.Gravity.CENTER);
		
		// 🌟 ডেঞ্জার আইকন 🌟
		android.widget.ImageView dangerIcon = new android.widget.ImageView(this);
		dangerIcon.setImageResource(android.R.drawable.ic_menu_delete); // ডিলিট আইকন
		dangerIcon.setColorFilter(android.graphics.Color.parseColor("#E53935")); // লাল রঙের ডেঞ্জার কালার
		rootLayout.addView(dangerIcon, new android.widget.LinearLayout.LayoutParams(140, 140));
		
		// 🌟 টাইটেল 🌟
		android.widget.TextView tvTitle = new android.widget.TextView(this);
		tvTitle.setText("মুছে ফেলবেন?");
		tvTitle.setTextColor(primaryTextColor);
		tvTitle.setTextSize(22f);
		tvTitle.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		tvTitle.setGravity(android.view.Gravity.CENTER);
		tvTitle.setPadding(0, 32, 0, 16);
		rootLayout.addView(tvTitle);
		
		// 🌟 মেসেজ 🌟
		android.widget.TextView tvMsg = new android.widget.TextView(this);
		tvMsg.setText("আপনি কি সত্যিই '" + itemName + "' মুছে ফেলতে চান? এটি আর ফিরিয়ে আনা সম্ভব হবে না।");
		tvMsg.setTextColor(secondaryTextColor);
		tvMsg.setTextSize(15f);
		tvMsg.setTypeface(currentTypeface);
		tvMsg.setGravity(android.view.Gravity.CENTER);
		tvMsg.setPadding(0, 0, 0, 64);
		tvMsg.setLineSpacing(0, 1.3f);
		rootLayout.addView(tvMsg);
		
		// 🌟 বাটন লেআউট (পাশাপাশি দুটো বাটন) 🌟
		android.widget.LinearLayout btnLayout = new android.widget.LinearLayout(this);
		btnLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
		btnLayout.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
		
		// ❌ বাতিল বাটন
		android.widget.TextView btnCancel = new android.widget.TextView(this);
		btnCancel.setText("না, থাক");
		btnCancel.setTextColor(primaryTextColor);
		btnCancel.setGravity(android.view.Gravity.CENTER);
		btnCancel.setTextSize(16f);
		btnCancel.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		btnCancel.setPadding(0, 32, 0, 32);
		android.graphics.drawable.GradientDrawable cancelBg = new android.graphics.drawable.GradientDrawable();
		cancelBg.setColor(bgColor);
		cancelBg.setCornerRadius(100f);
		btnCancel.setBackground(cancelBg);
		android.widget.LinearLayout.LayoutParams cancelParams = new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1);
		cancelParams.setMargins(0, 0, 16, 0);
		btnCancel.setLayoutParams(cancelParams);
		
		btnCancel.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(android.view.View v) {
				deleteSheet.dismiss(); // শিট বন্ধ হয়ে যাবে, কিচ্ছু ডিলিট হবে না
			}
		});
		
		// ✅ ডিলিট কনফার্ম বাটন
		android.widget.TextView btnConfirm = new android.widget.TextView(this);
		btnConfirm.setText("হ্যাঁ, মুছে ফেলুন");
		btnConfirm.setTextColor(android.graphics.Color.WHITE);
		btnConfirm.setGravity(android.view.Gravity.CENTER);
		btnConfirm.setTextSize(16f);
		btnConfirm.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		btnConfirm.setPadding(0, 32, 0, 32);
		android.graphics.drawable.GradientDrawable confirmBg = new android.graphics.drawable.GradientDrawable();
		confirmBg.setColor(android.graphics.Color.parseColor("#E53935")); // লাল রঙের ওয়ার্নিং বাটন
		confirmBg.setCornerRadius(100f);
		btnConfirm.setBackground(confirmBg);
		android.widget.LinearLayout.LayoutParams confirmParams = new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1);
		confirmParams.setMargins(16, 0, 0, 0);
		btnConfirm.setLayoutParams(confirmParams);
		
		btnConfirm.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(android.view.View v) {
				deleteSheet.dismiss();
				if (onDeleteConfirmed != null) {
					onDeleteConfirmed.run(); // আসল ডিলিট করার কোডটি এখানে ফায়ার হবে!
				}
			}
		});
		
		btnLayout.addView(btnCancel);
		btnLayout.addView(btnConfirm);
		rootLayout.addView(btnLayout);
		
		deleteSheet.setContentView(rootLayout);
		deleteSheet.show();
	}
	
	
	// ==========================================
	// 🌟 সার্ভারের সাথে কানেক্ট করার মেইন ইঞ্জিন 🌟
	// ==========================================
	private void connectToServerAndSync() {
		// যেহেতু ইন্টারনেটের কাজ, তাই এটি ব্যাকগ্রাউন্ড থ্রেডে (Background Thread) করতে হবে
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// 🚨 আপনার ওয়েবসাইটের আসল লিংকটি এখানে দিন 🚨
					// যেমন: https://www.shubhra.com/api/sync.php
					java.net.URL url = new java.net.URL("https://www.shuvraafroj.info/api/sync.php");
					
					java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
					conn.setRequestMethod("POST");
					conn.setDoOutput(true);
					conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
					
					// ১. প্যাকেটে কী কী ডেটা পাঠাবেন তা রেডি করা (JSON ফরম্যাটে)
					org.json.JSONObject jsonParam = new org.json.JSONObject();
					jsonParam.put("app_name", "TunePad");
					jsonParam.put("author", "Shubhra Afroj Tunerosa");
					jsonParam.put("last_active", System.currentTimeMillis());
					jsonParam.put("message", "হ্যালো সার্ভার! আমি অ্যাপ থেকে এসেছি।");
					
					// ২. সার্ভারের উদ্দেশ্যে ডেটা পাঠিয়ে দেওয়া
					java.io.OutputStream os = conn.getOutputStream();
					os.write(jsonParam.toString().getBytes("UTF-8"));
					os.flush();
					os.close();
					
					// ৩. সার্ভার কী উত্তর দিলো তা চেক করা (Response)
					int responseCode = conn.getResponseCode();
					if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
						// সার্ভার থেকে আসা মেসেজ পড়া
						java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
						StringBuilder response = new StringBuilder();
						String inputLine;
						while ((inputLine = in.readLine()) != null) {
							response.append(inputLine);
						}
						in.close();
						
						// সার্ভারের উত্তরটি প্রসেস করা
						final org.json.JSONObject serverResponse = new org.json.JSONObject(response.toString());
						final String status = serverResponse.getString("status");
						final String message = serverResponse.getString("message");
						
						// 🌟 ইউজারকে স্ক্রিনে মেসেজ দেখানো (মেইন থ্রেডে)
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if (status.equals("success")) {
									Toast.makeText(MmmActivity.this, "✅ " + message, Toast.LENGTH_SHORT).show();
									// (এখানে আপনি সিঙ্ক ব্যানারের সাকসেস অ্যানিমেশন ট্রিগার করতে পারেন)
								} else {
									Toast.makeText(MmmActivity.this, "❌ এরর: " + message, Toast.LENGTH_SHORT).show();
								}
							}
						});
						
					} else {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(MmmActivity.this, "সার্ভার কানেকশন ফেইল! কোড: " + responseCode, Toast.LENGTH_SHORT).show();
							}
						});
					}
					
				} catch (final Exception e) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(MmmActivity.this, "ইন্টারনেট বা সার্ভার সমস্যা: " + e.getMessage(), Toast.LENGTH_SHORT).show();
						}
					});
				}
			}
		}).start();
	}
	
	// ==========================================
	// 🌟 রিয়েল-টাইম অটো-ব্যাকআপ সিঙ্ক ব্যানার (Updated) 🌟
	// ==========================================
	private void showStartupSyncBanner() {
		// ১. স্ক্রিনের মেইন লেআউটটি খুঁজে বের করা
		final android.view.ViewGroup rootContent = findViewById(android.R.id.content);
		if (rootContent == null) return;
		
		final android.widget.LinearLayout syncBanner = new android.widget.LinearLayout(this);
		syncBanner.setOrientation(android.widget.LinearLayout.HORIZONTAL);
		syncBanner.setGravity(android.view.Gravity.CENTER_VERTICAL);
		
		// 🌟 আপনার অ্যাপের ডায়নামিক থিম কালার অনুযায়ী ব্যানারের ডিজাইন 🌟
		android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
		bg.setColor(surfaceColor != 0 ? surfaceColor : android.graphics.Color.parseColor("#1E1E24"));
		bg.setCornerRadius(100f);
		syncBanner.setBackground(bg);
		syncBanner.setElevation(16f);
		
		// 🌟 ব্যানারের পজিশন (স্ক্রিনের একদম ওপরে থাকবে) 🌟
		int margin = (int) (16 * getResources().getDisplayMetrics().density);
		android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
		android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		params.setMargins(margin, margin + 40, margin, margin); // স্ট্যাটাস বারের ঠিক নিচে
		params.gravity = android.view.Gravity.TOP;
		syncBanner.setLayoutParams(params);
		syncBanner.setPadding(48, 32, 48, 32);
		
		// 🌟 প্রগ্রেস বার (ছোট্ট চাকা ঘুরতে থাকবে) 🌟
		final android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
		progressBar.setLayoutParams(new android.widget.LinearLayout.LayoutParams(50, 50));
		progressBar.getIndeterminateDrawable().setColorFilter(accentColor != 0 ? accentColor : android.graphics.Color.parseColor("#108F6E"), android.graphics.PorterDuff.Mode.SRC_IN);
		syncBanner.addView(progressBar);
		
		// 🌟 সিঙ্কিং টেক্সট 🌟
		final android.widget.TextView tvSync = new android.widget.TextView(this);
		tvSync.setText(" সার্ভারের সাথে সিঙ্ক হচ্ছে...");
		tvSync.setTextColor(primaryTextColor != 0 ? primaryTextColor : android.graphics.Color.WHITE);
		tvSync.setTextSize(14f);
		if (currentTypeface != null) {
			tvSync.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		} else {
			tvSync.setTypeface(null, android.graphics.Typeface.BOLD);
		}
		tvSync.setPadding(32, 0, 0, 0);
		syncBanner.addView(tvSync);
		
		rootContent.addView(syncBanner);
		
		// 🌟 ম্যাজিক: ওপর থেকে মসৃণভাবে নেমে আসার অ্যানিমেশন 🌟
		syncBanner.setTranslationY(-200f);
		syncBanner.setAlpha(0f);
		syncBanner.animate().translationY(0f).alpha(1f).setDuration(500).start();
		
		// ==========================================
		// 🌟 ২. ব্যাকগ্রাউন্ডে আসল সার্ভার রিকোয়েস্ট (ডেইলি সিঙ্ক) 🌟
		// ==========================================
		final android.content.SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
		final String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
		String lastBackupDate = prefs.getString("last_backup_date", "");
		
		// 🌟 লজিক: যদি আজ ইতিমধ্যেই ব্যাকআপ হয়ে গিয়ে থাকে 🌟
		if (todayDate.equals(lastBackupDate)) {
			new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
				@Override
				public void run() {
					progressBar.setVisibility(android.view.View.GONE);
					tvSync.setText("ডেটা আপ-টু-ডেট আছে! ✅");
					tvSync.setTextColor(android.graphics.Color.parseColor("#4CAF50")); 
					
					// ব্যানার রিমুভ করা
					new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
						@Override
						public void run() {
							syncBanner.animate().translationY(-200f).alpha(0f).setDuration(400)
							.withEndAction(new Runnable() { @Override public void run() { rootContent.removeView(syncBanner); } }).start();
						}
					}, 1500);
				}
			}, 1000); // জাস্ট ইউজারকে দেখানোর জন্য ১ সেকেন্ড ঘুরাবে
			return; // নিচের কোডে আর যাবে না
		}
		
		// 🌟 রিয়েল সার্ভার সিঙ্ক থ্রেড 🌟
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// ১. ডাটাবেস থেকে সব নোট সংগ্রহ
					org.json.JSONArray notesArray = new org.json.JSONArray();
					android.database.sqlite.SQLiteDatabase db = openOrCreateDatabase("notes_db_v3", MODE_PRIVATE, null);
					android.database.Cursor cursor = db.rawQuery("SELECT id, title, content, label FROM notes WHERE isDeleted=0", null);
					
					while (cursor.moveToNext()) {
						org.json.JSONObject noteObj = new org.json.JSONObject();
						noteObj.put("id", cursor.getString(0));
						noteObj.put("title", cursor.getString(1));
						noteObj.put("content", cursor.getString(2));
						noteObj.put("label", cursor.getString(3)); 
						notesArray.put(noteObj);
					}
					cursor.close();
					db.close();
					
					// ২. সার্ভারে পাঠানো
					String userName = prefs.getString("author_name", "DefaultUser");
					org.json.JSONObject payload = new org.json.JSONObject();
					payload.put("username", userName);
					payload.put("notes", notesArray);
					
					// 🚨 আপনার ওয়েবসাইটের API লিংকটি দিন 🚨
					java.net.URL url = new java.net.URL("https://www.yourwebsite.com/api/auto_sync.php");
					java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
					conn.setRequestMethod("POST");
					conn.setDoOutput(true);
					conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
					
					java.io.OutputStream os = conn.getOutputStream();
					os.write(payload.toString().getBytes("UTF-8"));
					os.flush(); os.close();
					
					final int responseCode = conn.getResponseCode();
					
					// ৩. রেসপন্স অনুযায়ী UI আপডেট (Main Thread)
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
								prefs.edit().putString("last_backup_date", todayDate).apply(); // আজকের তারিখ সেভ
								progressBar.setVisibility(android.view.View.GONE);
								tvSync.setText("ব্যাকআপ সম্পন্ন হয়েছে! ✅");
								tvSync.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
							} else {
								progressBar.setVisibility(android.view.View.GONE);
								tvSync.setText("সার্ভার এরর! ⚠️");
								tvSync.setTextColor(android.graphics.Color.parseColor("#E53935"));
							}
							
							// ব্যানার রিমুভ করা
							new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
								@Override
								public void run() {
									syncBanner.animate().translationY(-200f).alpha(0f).setDuration(400)
									.withEndAction(new Runnable() { @Override public void run() { rootContent.removeView(syncBanner); } }).start();
								}
							}, 1500);
						}
					});
					
				} catch (final Exception e) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							progressBar.setVisibility(android.view.View.GONE);
							tvSync.setText("ইন্টারনেট সংযোগ নেই! 📶");
							tvSync.setTextColor(android.graphics.Color.parseColor("#FDD835")); // হলুদ কালার
							
							// ব্যানার রিমুভ করা
							new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
								@Override
								public void run() {
									syncBanner.animate().translationY(-200f).alpha(0f).setDuration(400)
									.withEndAction(new Runnable() { @Override public void run() { rootContent.removeView(syncBanner); } }).start();
								}
							}, 1500);
						}
					});
				}
			}
		}).start();
	}
	
	
	// ==========================================
	// 🌟 প্রজেক্ট ফাইল (.tbox) সার্ভারে আপলোড করার ম্যাজিক ইঞ্জিন 🌟
	// ==========================================
	private void uploadProjectFileToServer(final java.io.File projectFile) {
		if (projectFile == null || !projectFile.exists()) {
			Toast.makeText(this, "আপলোড করার জন্য কোনো ফাইল পাওয়া যায়নি!", Toast.LENGTH_SHORT).show();
			return;
		}
		
		Toast.makeText(this, "সার্ভারে প্রজেক্ট আপলোড শুরু হচ্ছে...", Toast.LENGTH_SHORT).show();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				// 🚨 আপনার ওয়েবসাইটের আপলোড API লিংকটি দিন 🚨
				String uploadUrl = "https://www.yourwebsite.com/api/upload_project.php";
				
				String boundary = "*****" + System.currentTimeMillis() + "*****";
				String twoHyphens = "--";
				String crlf = "\r\n";
				
				try {
					java.net.URL url = new java.net.URL(uploadUrl);
					java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
					conn.setUseCaches(false);
					conn.setDoOutput(true);
					conn.setRequestMethod("POST");
					conn.setRequestProperty("Connection", "Keep-Alive");
					conn.setRequestProperty("Cache-Control", "no-cache");
					conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
					
					java.io.DataOutputStream request = new java.io.DataOutputStream(conn.getOutputStream());
					
					// ফাইলের ডেটা প্যাকেটে ভরা শুরু
					request.writeBytes(twoHyphens + boundary + crlf);
					request.writeBytes("Content-Disposition: form-data; name=\"project_file\";filename=\"" + projectFile.getName() + "\"" + crlf);
					request.writeBytes(crlf);
					
					// আসল ফাইলটি রিড করে সার্ভারে পাঠানো
					java.io.FileInputStream fileInputStream = new java.io.FileInputStream(projectFile);
					int bytesRead, bytesAvailable, bufferSize;
					byte[] buffer;
					int maxBufferSize = 1 * 1024 * 1024; // 1 MB বাফার
					
					bytesAvailable = fileInputStream.available();
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					buffer = new byte[bufferSize];
					
					bytesRead = fileInputStream.read(buffer, 0, bufferSize);
					while (bytesRead > 0) {
						request.write(buffer, 0, bufferSize);
						bytesAvailable = fileInputStream.available();
						bufferSize = Math.min(bytesAvailable, maxBufferSize);
						bytesRead = fileInputStream.read(buffer, 0, bufferSize);
					}
					
					request.writeBytes(crlf);
					request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
					
					fileInputStream.close();
					request.flush();
					request.close();
					
					// সার্ভারের উত্তর চেক করা
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
						
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if ("success".equals(status)) {
									showCustomToastSheet("✅ " + message);
								} else {
									showCustomToastSheet("❌ " + message);
								}
							}
						});
					} else {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								showCustomToastSheet("সার্ভার ফেইল! কোড: " + responseCode);
							}
						});
					}
					
				} catch (final Exception e) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							showCustomToastSheet("আপলোড এরর: " + e.getMessage());
						}
					});
				}
			}
		}).start();
	}
	
	
	// ==========================================
	// 🌟 সাইলেন্ট অটো ব্যাকআপ ইঞ্জিন (ডেইলি সিঙ্ক) 🌟
	// ==========================================
	private void runDailyAutoBackup() {
		final android.content.SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
		String lastBackupDate = prefs.getString("last_backup_date", "");
		final String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
		
		// যদি আজ ইতিমধ্যে ব্যাকআপ হয়ে থাকে, তবে আর করবে না
		if (todayDate.equals(lastBackupDate)) return;
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// 🌟 ডাটাবেস থেকে সব নোট সংগ্রহ করা 🌟
					org.json.JSONArray notesArray = new org.json.JSONArray();
					android.database.sqlite.SQLiteDatabase db = openOrCreateDatabase("notes_db_v3", MODE_PRIVATE, null);
					android.database.Cursor cursor = db.rawQuery("SELECT id, title, content, label FROM notes WHERE isDeleted=0", null);
					
					while (cursor.moveToNext()) {
						org.json.JSONObject noteObj = new org.json.JSONObject();
						noteObj.put("id", cursor.getString(0));
						noteObj.put("title", cursor.getString(1));
						noteObj.put("content", cursor.getString(2));
						noteObj.put("label", cursor.getString(3)); // 🌟 ফোল্ডারের নাম
						notesArray.put(noteObj);
					}
					cursor.close();
					db.close();
					
					// 🌟 সার্ভারে পাঠানো 🌟
					String userName = prefs.getString("author_name", "DefaultUser");
					org.json.JSONObject payload = new org.json.JSONObject();
					payload.put("username", userName);
					payload.put("notes", notesArray);
					
					java.net.URL url = new java.net.URL("https://www.shuvraafroj.info/api/auto_sync.php");
					java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
					conn.setRequestMethod("POST");
					conn.setDoOutput(true);
					conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
					
					java.io.OutputStream os = conn.getOutputStream();
					os.write(payload.toString().getBytes("UTF-8"));
					os.flush(); os.close();
					
					if (conn.getResponseCode() == java.net.HttpURLConnection.HTTP_OK) {
						// সফল হলে আজকের তারিখ সেভ করে রাখা
						prefs.edit().putString("last_backup_date", todayDate).apply();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	// ==========================================
	// 🌟 এক ক্লিকে লিংক শেয়ার করার ম্যাজিক মেথড 🌟
	// ==========================================
	private void shareNoteLinkQuickly(String noteId, String noteTitle) {
		String content = etInputText.getText().toString();
		if (noteTitle == null || noteTitle.trim().isEmpty()) noteTitle = "Shared_Note";
		
		// 🌟 আপলোড শুরু করা হলো (আপলোড শেষ হলে সার্ভার থেকে আসল লিংক আসবে) 🌟
		generateAndShareNoteLink(noteId, noteTitle, content, false);
	}
	
	
	
	// ==========================================
	// 🌟 ডেডিকেটেড ফোল্ডারে প্রজেক্ট আপলোড ও শেয়ার ইঞ্জিন (Bangla Fix) 🌟
	// ==========================================
	private void uploadAndShareProject(final java.io.File projDir, final String projTitle) {
		showCustomToastSheet("আপনার ফোল্ডারে প্রজেক্ট আপলোড হচ্ছে, দয়া করে অপেক্ষা করুন... ⏳");
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					java.io.File cacheDir = getCacheDir();
					java.io.File tboxFile = new java.io.File(cacheDir, projTitle + ".tbox");
					
					java.io.File metaDataFile = new java.io.File(projDir, "project_meta_data.json");
					saveSharedPreferencesToFile(projTitle, metaDataFile);
					
					TBoxUtils.zipAndEncryptFolder(projDir, tboxFile);
					if (metaDataFile.exists()) metaDataFile.delete();
					
					// 🚨 আপনার ওয়েবসাইটের আপলোড API লিংক 🚨
					String uploadUrl = "https://www.shuvraafroj.info/api/upload_project.php";
					String boundary = "*****" + System.currentTimeMillis() + "*****";
					String crlf = "\r\n";
					String twoHyphens = "--";
					
					java.net.URL url = new java.net.URL(uploadUrl);
					java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
					conn.setUseCaches(false);
					conn.setDoOutput(true);
					conn.setRequestMethod("POST");
					conn.setRequestProperty("Connection", "Keep-Alive");
					conn.setRequestProperty("Cache-Control", "no-cache");
					conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
					
					java.io.DataOutputStream request = new java.io.DataOutputStream(conn.getOutputStream());
					
					android.content.SharedPreferences appSettings = getSharedPreferences("AppSettings", MODE_PRIVATE);
					String userNameEn = appSettings.getString("author_name_en", "Shubhra Afroj Tunerosa"); 
					
					// 🚨 ফিক্সড: writeBytes এর বদলে getBytes("UTF-8") দেওয়া হলো, যাতে বাংলা ফাইলের নাম ঠিক থাকে! 🚨
					request.write((twoHyphens + boundary + crlf).getBytes("UTF-8"));
					request.write(("Content-Disposition: form-data; name=\"username\"" + crlf).getBytes("UTF-8"));
					request.write((crlf).getBytes("UTF-8"));
					request.write((userNameEn + crlf).getBytes("UTF-8"));
					
					// 🌟 প্রজেক্ট ফাইল সার্ভারে পাঠানো 🌟
					request.write((twoHyphens + boundary + crlf).getBytes("UTF-8"));
					request.write(("Content-Disposition: form-data; name=\"project_file\";filename=\"" + tboxFile.getName() + "\"" + crlf).getBytes("UTF-8"));
					request.write((crlf).getBytes("UTF-8"));
					
					java.io.FileInputStream fileInputStream = new java.io.FileInputStream(tboxFile);
					int bytesRead, bytesAvailable, bufferSize;
					byte[] buffer;
					int maxBufferSize = 1 * 1024 * 1024; // 1 MB বাফার
					
					bytesAvailable = fileInputStream.available();
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					buffer = new byte[bufferSize];
					
					bytesRead = fileInputStream.read(buffer, 0, bufferSize);
					while (bytesRead > 0) {
						request.write(buffer, 0, bufferSize);
						bytesAvailable = fileInputStream.available();
						bufferSize = Math.min(bytesAvailable, maxBufferSize);
						bytesRead = fileInputStream.read(buffer, 0, bufferSize);
					}
					
					request.write((crlf).getBytes("UTF-8"));
					request.write((twoHyphens + boundary + twoHyphens + crlf).getBytes("UTF-8"));
					
					fileInputStream.close();
					request.flush();
					request.close();
					
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
										// 🌟 ম্যাজিক: এনকোড করা বিদঘুটে লিংকটিকে আবার সুন্দর বাংলায় রূপান্তর করা হলো 🌟
										String beautifulUrl = java.net.URLDecoder.decode(fileUrl, "UTF-8");
										
										android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
										intent.setType("text/plain");
										intent.putExtra(android.content.Intent.EXTRA_SUBJECT, projTitle);
										// 🌟 এখন শেয়ার করার সময় সুন্দর বাংলা লিংকটি যাবে 🌟
										intent.putExtra(android.content.Intent.EXTRA_TEXT, "আমার লেখা '" + projTitle + "' প্রজেক্টটি ডাউনলোড করুন:\n\n" + beautifulUrl);
										startActivity(android.content.Intent.createChooser(intent, "লিংক শেয়ার করুন..."));
										
										showCustomToastSheet("✅ আপলোড সফল এবং লিংক তৈরি হয়েছে!");
									} catch (Exception e) {
										e.printStackTrace();
									}
								} else {
									showCustomToastSheet("❌ " + message);
								}
							}
						});
					} else {
						runOnUiThread(new Runnable() { @Override public void run() { showCustomToastSheet("সার্ভার ফেইল! কোড: " + responseCode); } });
					}
					
				} catch (final Exception e) {
					runOnUiThread(new Runnable() { @Override public void run() { showCustomToastSheet("আপলোড এরর: " + e.getMessage()); } });
				}
			}
		}).start();
	}
	
	
	// ==========================================
	// 🌟 অফলাইন / ডাইরেক্ট লিংক জেনারেটর (MB ছাড়াই শেয়ার) 🌟
	// ==========================================
	private void shareOfflinePredictableLink(String projTitle) {
		android.content.SharedPreferences appSettings = getSharedPreferences("AppSettings", MODE_PRIVATE);
		String userNameEn = appSettings.getString("author_name_en", "Unknown_User");
		String safeUserName = userNameEn.replaceAll("[^a-zA-Z0-9_]", "_");
		
		String safeFileName = projTitle.replace(" ", "_") + ".tbox";
		String encodedFileName = safeFileName;
		try {
			// লিংকের বাংলা সাপোর্ট ঠিক রাখার জন্য এনকোড করা
			encodedFileName = java.net.URLEncoder.encode(safeFileName, "UTF-8").replace("+", "%20");
		} catch (Exception e) {}
		
		// 🌟 আপনার সার্ভারের স্ট্রাকচার অনুযায়ী ডাইরেক্ট লিংক 🌟
		String shareLink = "https://www.shuvraafroj.info/api/users/" + safeUserName + "/Manual_Backup/Projects/" + encodedFileName;
		
		// 🌟 সুন্দর বাংলায় লিংকটা ডিকোড করে ইউজারের কাছে পাঠানো 🌟
		String beautifulUrl = shareLink;
		try { beautifulUrl = java.net.URLDecoder.decode(shareLink, "UTF-8"); } catch (Exception e) {}
		
		android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(android.content.Intent.EXTRA_SUBJECT, projTitle);
		intent.putExtra(android.content.Intent.EXTRA_TEXT, "আমার লেখা '" + projTitle + "' প্রজেক্টটি ডাউনলোড করুন:\n\n" + beautifulUrl);
		startActivity(android.content.Intent.createChooser(intent, "লিংক শেয়ার করুন..."));
	}
	// ==========================================
	// 🌟 ইন্টারনেট কানেকশন চেক করার মেথড 🌟
	// ==========================================
	private boolean isNetworkAvailable() {
		android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		android.net.NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
	
	// ==========================================
	// 🌟 প্রজেক্ট-ভিত্তিক সাইলেন্ট অটো-ব্যাকআপ (MmmActivity এর জন্য) 🌟
	// ==========================================
	private void runProjectSilentAutoBackup() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				android.content.SharedPreferences autoBackupPrefs = getSharedPreferences("AutoBackupPrefs", MODE_PRIVATE);
				long lastBackupTime = autoBackupPrefs.getLong("last_project_auto_backup", 0);
				long currentTime = System.currentTimeMillis();
				
				// ৫ ঘণ্টা = 18000000 মিলিসেকেন্ড
				if (currentTime - lastBackupTime < 18000000) return; 
				
				boolean anyProjectUploaded = false;
				java.io.File projDir = getProjectsDir();
				java.io.File[] categories = projDir.listFiles();
				
				if (categories != null) {
					for (java.io.File cat : categories) {
						if (cat.isDirectory()) {
							java.io.File[] projects = cat.listFiles();
							if (projects != null) {
								for (java.io.File proj : projects) {
									if (proj.isDirectory()) {
										String projTitle = proj.getName();
										if (autoBackupPrefs.getBoolean("auto_backup_" + projTitle, false)) {
											if (silentUploadSingleProject(proj, projTitle)) {
												anyProjectUploaded = true;
											}
										}
									}
								}
							}
						}
					}
				}
				if (anyProjectUploaded) {
					autoBackupPrefs.edit().putLong("last_project_auto_backup", currentTime).apply();
				}
			}
		}).start();
	}
	
	// সাইলেন্ট আপলোড হেল্পার
	private boolean silentUploadSingleProject(java.io.File projDir, String projTitle) {
		try {
			java.io.File cacheDir = getCacheDir();
			java.io.File tboxFile = new java.io.File(cacheDir, projTitle + ".tbox");
			java.io.File metaDataFile = new java.io.File(projDir, "project_meta_data.json");
			saveSharedPreferencesToFile(projTitle, metaDataFile);
			
			TBoxUtils.zipAndEncryptFolder(projDir, tboxFile);
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
			
			return conn.getResponseCode() == java.net.HttpURLConnection.HTTP_OK;
			
		} catch (Exception e) {
			return false; 
		}
	}// ==========================================
	// 🌟 ২. সিঙ্গেল নোট ID দিয়ে .tpad ফাইলে রূপান্তর (টাইটেল ভেতরে সেভ করা) 🌟
	// ==========================================
	private void generateAndShareNoteLink(final String noteId, final String title, final String content, final boolean isAutoBackup) {
		try {
			if (!isAutoBackup) showCustomToastSheet("নোটের লিংক তৈরি হচ্ছে... ⏳");
			
			// 🌟 ম্যাজিক: কন্টেন্টের একদম শুরুতে টাইটেলটা সিক্রেট ট্যাগ দিয়ে বসিয়ে দেওয়া হলো 🌟
			String safeTitleForFile = (title == null || title.trim().isEmpty()) ? "Untitled" : title.trim();
			String combinedContent = "<TPAD_TITLE>" + safeTitleForFile + "</TPAD_TITLE>\n" + content;
			
			String encryptedContent = SecurityUtils.encrypt(combinedContent);
			if (encryptedContent == null) return;
			
			java.io.File cacheDir = getCacheDir();
			String safeId = noteId;
			if (safeId != null && safeId.contains("notes_db_v3")) {
				safeId = safeId.replaceAll(".*notes_db_v3", "Note_");
			} 
			if (safeId == null || safeId.isEmpty()) safeId = "Note_" + System.currentTimeMillis();
			safeId = safeId.replaceAll("[^a-zA-Z0-9.-]", "_");
			
			java.io.File tpadFile = new java.io.File(cacheDir, safeId + ".tpad");
			
			java.io.FileOutputStream fos = new java.io.FileOutputStream(tpadFile);
			fos.write(encryptedContent.getBytes());
			fos.close();
			
			// সার্ভারে আপলোড কল করা
			uploadNoteFileToServer(tpadFile, isAutoBackup);
			
		} catch (Exception e) {
			if (!isAutoBackup) showCustomToastSheet("নোট প্রসেস করতে সমস্যা হয়েছে!");
		}
	}
	
	// ==========================================
	// 🌟 ৩. শুধু ইউজারনেম পাঠিয়ে হার্ডকোডেড ফোল্ডারে আপলোড 🌟
	// ==========================================
	private void uploadNoteFileToServer(final java.io.File noteFile, final boolean isAutoBackup) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// ⚠️ আপনার API লিংকটি দিন
					String uploadUrl = "https://www.shuvraafroj.info/api/upload_handler.php"; 
					
					java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(uploadUrl).openConnection();
					conn.setDoInput(true); conn.setDoOutput(true); conn.setUseCaches(false);
					conn.setRequestMethod("POST");
					
					String boundary = "*****" + System.currentTimeMillis() + "*****";
					conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
					
					java.io.DataOutputStream request = new java.io.DataOutputStream(conn.getOutputStream());
					String crlf = "\r\n"; String twoHyphens = "--";
					
					android.content.SharedPreferences appSettings = getSharedPreferences("AppSettings", MODE_PRIVATE);
					String userNameEn = appSettings.getString("author_name_en", "Unknown_User"); 
					
					// ১. 🌟 শুধু Username পাঠানো হচ্ছে 🌟
					request.writeBytes(twoHyphens + boundary + crlf);
					request.writeBytes("Content-Disposition: form-data; name=\"username\"" + crlf + crlf);
					request.write((userNameEn + crlf).getBytes("UTF-8"));
					
					// ২. আসল ফাইল পাঠানো হচ্ছে
					request.writeBytes(twoHyphens + boundary + crlf);
					request.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + noteFile.getName() + "\"" + crlf);
					request.writeBytes(crlf);
					
					java.io.FileInputStream fis = new java.io.FileInputStream(noteFile);
					byte[] buffer = new byte[1024 * 1024]; int bytesRead;
					while ((bytesRead = fis.read(buffer)) > 0) { request.write(buffer, 0, bytesRead); }
					fis.close();
					
					request.writeBytes(crlf);
					request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
					request.flush(); request.close();
					
					// ... সার্ভারের রেসপন্স পড়া ...
					java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
					final StringBuilder response = new StringBuilder();
					String line; while ((line = reader.readLine()) != null) { response.append(line); }
					reader.close();
					
					if (noteFile.exists()) noteFile.delete(); 
					
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (!isAutoBackup) {
								String generatedLink = response.toString().trim(); 
								if (generatedLink.startsWith("http")) {
									
									// ==========================================
									// 🌟 ম্যাজিক: ক্লিপবোর্ডের বদলে আপনার শেয়ার ডায়ালগ ওপেন হবে 🌟
									// ==========================================
									String shareTitle = etNoteTitleBig != null ? etNoteTitleBig.getText().toString() : "Shared Note";
									
									android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
									intent.setType("text/plain");
									intent.putExtra(android.content.Intent.EXTRA_SUBJECT, shareTitle);
									intent.putExtra(android.content.Intent.EXTRA_TEXT, "আমার লেখা নোটটি এখানে পড়ুন অথবা TunePad অ্যাপে ইমপোর্ট করুন:\n\n" + generatedLink);
									startActivity(android.content.Intent.createChooser(intent, "লিংক শেয়ার করুন..."));
									
								} else {
									showCustomToastSheet("সার্ভার এরর: " + generatedLink);
								}
							}
						}
					});
					
					
				} catch (final Exception e) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (!isAutoBackup) showCustomToastSheet("আপলোড ফেইল্ড: " + e.getMessage());
						}
					});
				}
			}
		}).start();
	}
	
	
	// ==========================================
	// 🌟 ১. অ্যাডভান্সড ব্রেইন 🧠 ইমপোর্ট চেকার (বাগ ফিক্সড) 🌟
	// ==========================================
	private void checkAndSaveImportedNote() {
		final String title = etNoteTitleBig != null ? etNoteTitleBig.getText().toString().trim() : "Untitled Note";
		final String content = etInputText.getText().toString();
		
		// 🧠 নতুন নোটের অ্যাডভান্সড ডেটা
		int newCharCount = content.length();
		int newWordCount = content.trim().isEmpty() ? 0 : content.trim().split("\\s+").length;
		int newLineCount = content.isEmpty() ? 0 : content.split("\n").length; 
		long newSize = content.getBytes().length;
		
		java.io.File existingFile = new java.io.File(getFoldersDir(), title + ".tpad");
		
		if (existingFile.exists()) {
			String lastModified = new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(existingFile.lastModified()));
			long oldSize = existingFile.length();
			
			// 🌟 ফিক্স: লোকাল ফাইল এখন প্লেইন টেক্সট হিসেবে সরাসরি রিড করা হচ্ছে (এনক্রিপ্টেড নয়) 🌟
			String oldContent = "";
			try {
				java.io.FileInputStream fis = new java.io.FileInputStream(existingFile);
				byte[] data = new byte[(int) existingFile.length()];
				fis.read(data); 
				fis.close();
				oldContent = new String(data, "UTF-8");
			} catch (Exception e) {}
			
			int oldCharCount = oldContent.length();
			int oldWordCount = oldContent.trim().isEmpty() ? 0 : oldContent.trim().split("\\s+").length;
			int oldLineCount = oldContent.isEmpty() ? 0 : oldContent.split("\n").length;
			
			// 🧠 স্মার্ট রিকমেন্ডেশন (Smart Decision Logic)
			String recommendation = "";
			int recColor = android.graphics.Color.parseColor("#008744"); // ডিফল্ট সবুজ (Safe)
			
			if (newWordCount > oldWordCount) {
				recommendation = "💡 পরামর্শ: নতুন নোটে " + (newWordCount - oldWordCount) + "টি শব্দ এবং " + Math.abs(newLineCount - oldLineCount) + "টি লাইন বেশি আছে। এটি আপডেট বা রিপ্লেস করা নিরাপদ।";
			} else if (oldWordCount > newWordCount) {
				recommendation = "⚠️ সতর্কবার্তা: পুরনো নোটে " + (oldWordCount - newWordCount) + "টি শব্দ বেশি আছে! আপনার কিছু লেখা হয়তো বাদ পড়েছে। 'উভয়ই রাখুন' নির্বাচন করে মিলিয়ে দেখা ভালো হবে।";
				recColor = android.graphics.Color.parseColor("#FF5252"); // লাল (Danger)
			} else if (oldCharCount != newCharCount) {
				recommendation = "💡 পরামর্শ: শব্দ সংখ্যা একই থাকলেও অক্ষর বা স্পেসে পরিবর্তন আছে। আপনি চাইলে 'উভয়ই রাখুন' সিলেক্ট করতে পারেন।";
				recColor = android.graphics.Color.parseColor("#FFA000"); // কমলা (Warning)
			} else {
				recommendation = "✅ পরামর্শ: দুটি নোট একদম হুবহু একই! আপনি নিশ্চিন্তে 'রিপ্লেস করুন' বাটন চাপতে পারেন।";
			}
			
			// সব ডেটা পপআপে পাঠানো হচ্ছে
			showDuplicateImportDialog(title, content, lastModified, oldSize, oldWordCount, oldCharCount, oldLineCount, newSize, newWordCount, newCharCount, newLineCount, recommendation, recColor);
		} else {
			// নতুন ফাইল হলে সরাসরি সেভ
			showCustomSaveDialog(content);
		}
	}
	
	// ==========================================
	// 🌟 ২. ব্রেইন 🧠 স্মার্ট কম্পারিজন পপআপ (অ্যাডভান্সড) 🌟
	// ==========================================
	private void showDuplicateImportDialog(final String title, final String content, String lastModDate, long oldSize, int oldWords, int oldChars, int oldLines, long newSize, int newWords, int newChars, int newLines, String recommendation, int recColor) {
		final com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
		
		android.widget.LinearLayout root = new android.widget.LinearLayout(this);
		root.setOrientation(android.widget.LinearLayout.VERTICAL);
		root.setBackgroundColor(surfaceColor);
		root.setPadding(64, 64, 64, 64);
		
		android.widget.TextView tvTitle = new android.widget.TextView(this);
		tvTitle.setText("🧠 স্মার্ট কম্পারিজন অ্যালার্ট!");
		tvTitle.setTextColor(accentColor);
		tvTitle.setTextSize(22f);
		tvTitle.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		tvTitle.setPadding(0, 0, 0, 32);
		root.addView(tvTitle);
		
		// ==========================================
		// 🌟 স্মার্ট রিকমেন্ডেশন বক্স 🌟
		// ==========================================
		android.widget.TextView tvRec = new android.widget.TextView(this);
		tvRec.setText(recommendation);
		tvRec.setTextColor(recColor);
		tvRec.setTextSize(15f);
		tvRec.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		tvRec.setPadding(32, 24, 32, 48);
		
		android.graphics.drawable.GradientDrawable bgRec = new android.graphics.drawable.GradientDrawable();
		bgRec.setColor(android.graphics.Color.parseColor("#1A" + String.format("%06X", (0xFFFFFF & recColor)))); // কালারের হালকা ট্রান্সপারেন্ট ভার্সন
		bgRec.setCornerRadius(16f);
		tvRec.setBackground(bgRec);
		
		android.widget.LinearLayout.LayoutParams recParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
		recParams.setMargins(0, 0, 0, 48);
		tvRec.setLayoutParams(recParams);
		root.addView(tvRec);
		
		// ==========================================
		// 📁 ইনফো বক্স - পুরনো নোট
		// ==========================================
		android.widget.LinearLayout infoBoxOld = new android.widget.LinearLayout(this);
		infoBoxOld.setOrientation(android.widget.LinearLayout.VERTICAL);
		android.graphics.drawable.GradientDrawable infoBgOld = new android.graphics.drawable.GradientDrawable();
		infoBgOld.setColor(bgColor);
		infoBgOld.setCornerRadius(24f);
		infoBoxOld.setBackground(infoBgOld);
		infoBoxOld.setPadding(40, 32, 40, 32);
		
		android.widget.TextView tvOldTitle = new android.widget.TextView(this);
		tvOldTitle.setText("📁 আপনার ফোনে থাকা নোট:");
		tvOldTitle.setTextColor(secondaryTextColor);
		tvOldTitle.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		infoBoxOld.addView(tvOldTitle);
		
		android.widget.TextView tvOldInfo = new android.widget.TextView(this);
		tvOldInfo.setText("তারিখ: " + lastModDate + "\nলাইন: " + oldLines + " | শব্দ: " + oldWords + " | অক্ষর: " + oldChars + "\nসাইজ: " + oldSize + " Bytes");
		tvOldInfo.setTextColor(secondaryTextColor);
		tvOldInfo.setTextSize(14f);
		tvOldInfo.setPadding(0, 16, 0, 0);
		infoBoxOld.addView(tvOldInfo);
		root.addView(infoBoxOld);
		
		android.view.View space = new android.view.View(this);
		space.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, 24));
		root.addView(space);
		
		// ==========================================
		// 📥 ইনফো বক্স - নতুন নোট
		// ==========================================
		android.widget.LinearLayout infoBoxNew = new android.widget.LinearLayout(this);
		infoBoxNew.setOrientation(android.widget.LinearLayout.VERTICAL);
		android.graphics.drawable.GradientDrawable infoBgNew = new android.graphics.drawable.GradientDrawable();
		infoBgNew.setColor(android.graphics.Color.parseColor("#1A008744"));
		infoBgNew.setCornerRadius(24f);
		infoBoxNew.setBackground(infoBgNew);
		infoBoxNew.setPadding(40, 32, 40, 32);
		
		android.widget.TextView tvNewTitle = new android.widget.TextView(this);
		tvNewTitle.setText("📥 নতুন ইমপোর্ট করা নোট:");
		tvNewTitle.setTextColor(accentColor);
		tvNewTitle.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		infoBoxNew.addView(tvNewTitle);
		
		android.widget.TextView tvNewInfo = new android.widget.TextView(this);
		tvNewInfo.setText("লাইন: " + newLines + " | শব্দ: " + newWords + " | অক্ষর: " + newChars + "\nসাইজ: " + newSize + " Bytes");
		tvNewInfo.setTextColor(primaryTextColor);
		tvNewInfo.setTextSize(14f);
		tvNewInfo.setPadding(0, 16, 0, 0);
		infoBoxNew.addView(tvNewInfo);
		root.addView(infoBoxNew);
		
		// ==========================================
		// 🌟 অ্যাডভান্সড ৩টি বাটন
		// ==========================================
		android.widget.LinearLayout btnContainer = new android.widget.LinearLayout(this);
		btnContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
		btnContainer.setPadding(0, 48, 0, 0);
		
		// ১. উভয়ই রাখুন (Keep Both) Button
		android.widget.TextView btnKeepBoth = new android.widget.TextView(this);
		btnKeepBoth.setText("উভয়ই রাখুন (Keep Both)");
		btnKeepBoth.setTextColor(bgColor);
		btnKeepBoth.setTextSize(16f);
		btnKeepBoth.setGravity(android.view.Gravity.CENTER);
		btnKeepBoth.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		btnKeepBoth.setPadding(0, 36, 0, 36);
		android.graphics.drawable.GradientDrawable bgKeepBoth = new android.graphics.drawable.GradientDrawable();
		bgKeepBoth.setColor(accentColor);
		bgKeepBoth.setCornerRadius(100f);
		btnKeepBoth.setBackground(bgKeepBoth);
		btnKeepBoth.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, -2));
		
		btnKeepBoth.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(android.view.View v) {
				dialog.dismiss();
				// 🌟 ম্যাজিক: নামের শেষে (Copy) লাগিয়ে সেভ ডায়ালগ ওপেন হবে
				String copyTitle = title + " (Copy)";
				if (etNoteTitleBig != null) etNoteTitleBig.setText(copyTitle);
				currentTitle = copyTitle;
				showCustomSaveDialog(content); 
			}
		});
		btnContainer.addView(btnKeepBoth);
		
		// নিচের দুইটা বাটনের জন্য Horizontal Layout
		android.widget.LinearLayout btnRow2 = new android.widget.LinearLayout(this);
		btnRow2.setOrientation(android.widget.LinearLayout.HORIZONTAL);
		btnRow2.setPadding(0, 24, 0, 0);
		
		// ২. Rename Button
		android.widget.TextView btnRename = new android.widget.TextView(this);
		btnRename.setText("নতুন নামে সেভ");
		btnRename.setTextColor(primaryTextColor);
		btnRename.setTextSize(15f);
		btnRename.setGravity(android.view.Gravity.CENTER);
		btnRename.setPadding(0, 32, 0, 32);
		android.widget.LinearLayout.LayoutParams paramRename = new android.widget.LinearLayout.LayoutParams(0, -2, 1);
		paramRename.setMargins(0, 0, 12, 0);
		btnRename.setLayoutParams(paramRename);
		android.graphics.drawable.GradientDrawable bgRename = new android.graphics.drawable.GradientDrawable();
		bgRename.setColor(bgColor);
		bgRename.setCornerRadius(100f);
		btnRename.setBackground(bgRename);
		
		btnRename.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(android.view.View v) {
				dialog.dismiss();
				showCustomSaveDialog(content); 
			}
		});
		
		// ৩. Replace Button
		android.widget.TextView btnReplace = new android.widget.TextView(this);
		btnReplace.setText("রিপ্লেস করুন");
		btnReplace.setTextColor(android.graphics.Color.WHITE);
		btnReplace.setTextSize(15f);
		btnReplace.setGravity(android.view.Gravity.CENTER);
		btnReplace.setPadding(0, 32, 0, 32);
		android.widget.LinearLayout.LayoutParams paramReplace = new android.widget.LinearLayout.LayoutParams(0, -2, 1);
		paramReplace.setMargins(12, 0, 0, 0);
		btnReplace.setLayoutParams(paramReplace);
		android.graphics.drawable.GradientDrawable bgReplace = new android.graphics.drawable.GradientDrawable();
		bgReplace.setColor(android.graphics.Color.parseColor("#FF5252"));
		bgReplace.setCornerRadius(100f);
		btnReplace.setBackground(bgReplace);
		
		btnReplace.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(android.view.View v) {
				dialog.dismiss();
				currentTitle = title; 
				showCustomSaveDialog(content); 
			}
		});
		
		btnRow2.addView(btnRename);
		btnRow2.addView(btnReplace);
		btnContainer.addView(btnRow2);
		
		root.addView(btnContainer);
		
		// ScrollView এড করা হলো যেন ছোট স্ক্রিনেও সব দেখা যায়
		android.widget.ScrollView scroll = new android.widget.ScrollView(this);
		scroll.addView(root);
		dialog.setContentView(scroll);
		dialog.show();
	}
	
// 🌟 ম্যাজিক: ব্যাক বাটনে চাপ দিলে সুন্দর বটম শিট পপআপ আসবে 🌟
private void exitWordCounterMode() {
    if (layoutWordCounter.getVisibility() == android.view.View.GONE) return;
    
    final String currentText = etInputText != null ? etInputText.getText().toString() : "";
    boolean isReadOnly = !etInputText.isFocusable(); 
    
    // যদি রিড-অনলি মোড হয়, অথবা নোট একদম খালি হয় (এবং ড্রাফট না হয়), তবে সরাসরি বের হয়ে যাবে
    if (isReadOnly || (currentText.trim().isEmpty() && (currentEditingNoteId == null || !currentEditingNoteId.startsWith("Draft_")))) {
        performActualExit();
        return;
    }
    
    // ==========================================
    // 🌟 প্রিমিয়াম বটম শিট পপআপ ডিজাইন 🌟
    // ==========================================
    final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
    
    android.widget.LinearLayout rootLayout = new android.widget.LinearLayout(this);
    rootLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
    rootLayout.setBackgroundColor(surfaceColor); // থিম অনুযায়ী ব্যাকগ্রাউন্ড
    rootLayout.setPadding(64, 64, 64, 48);
    
    // 🌟 টাইটেল 🌟
    android.widget.TextView tvTitle = new android.widget.TextView(this);
    tvTitle.setText("সেভ করতে চান?");
    tvTitle.setTextColor(primaryTextColor);
    tvTitle.setTextSize(22f);
    tvTitle.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
    tvTitle.setPadding(0, 0, 0, 16);
    rootLayout.addView(tvTitle);
    
    // 🌟 মেসেজ 🌟
    android.widget.TextView tvMessage = new android.widget.TextView(this);
    tvMessage.setText("বের হওয়ার আগে কি নোটের পরিবর্তনগুলো সেভ করতে চান?");
    tvMessage.setTextColor(secondaryTextColor);
    tvMessage.setTextSize(16f);
    tvMessage.setTypeface(currentTypeface);
    tvMessage.setPadding(0, 0, 0, 48);
    rootLayout.addView(tvMessage);
    
    // 🌟 ১. হ্যাঁ, সেভ করুন বাটন 🌟
    android.widget.TextView btnSave = new android.widget.TextView(this);
    btnSave.setText("হ্যাঁ, সেভ করুন");
    btnSave.setTextColor(bgColor); // টেক্সট কালার
    btnSave.setTextSize(16f);
    btnSave.setGravity(android.view.Gravity.CENTER);
    btnSave.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
    btnSave.setPadding(0, 36, 0, 36);
    
    android.graphics.drawable.GradientDrawable bgSave = new android.graphics.drawable.GradientDrawable();
    bgSave.setColor(accentColor);
    bgSave.setCornerRadius(100f);
    btnSave.setBackground(bgSave);
    
    android.widget.LinearLayout.LayoutParams paramsSave = new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    paramsSave.setMargins(0, 0, 0, 24);
    btnSave.setLayoutParams(paramsSave);
    
    btnSave.setOnClickListener(new android.view.View.OnClickListener() {
        @Override
        public void onClick(android.view.View v) {
            bottomSheet.dismiss();
            if (currentEditingNoteId != null && currentLabel != null && (currentLabel.startsWith("Folder:") || currentLabel.startsWith("Project:"))) {
                saveNoteDirectly(currentText);
            } else {
                showCustomSaveDialog(currentText);
            }
        }
    });
    rootLayout.addView(btnSave);
    
    // 🌟 ২. না, সেভ করবো না বাটন (ডেঞ্জার কালার) 🌟
    android.widget.TextView btnDiscard = new android.widget.TextView(this);
    btnDiscard.setText("না, সেভ করবো না");
    btnDiscard.setTextColor(android.graphics.Color.parseColor("#E53935")); // লাল রঙ
    btnDiscard.setTextSize(16f);
    btnDiscard.setGravity(android.view.Gravity.CENTER);
    btnDiscard.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
    btnDiscard.setPadding(0, 36, 0, 36);
    
    android.graphics.drawable.GradientDrawable bgDiscard = new android.graphics.drawable.GradientDrawable();
    bgDiscard.setColor(android.graphics.Color.parseColor("#1AE53935")); // হালকা লাল ব্যাকগ্রাউন্ড
    bgDiscard.setCornerRadius(100f);
    btnDiscard.setBackground(bgDiscard);
    
    android.widget.LinearLayout.LayoutParams paramsDiscard = new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    paramsDiscard.setMargins(0, 0, 0, 24);
    btnDiscard.setLayoutParams(paramsDiscard);
    
    btnDiscard.setOnClickListener(new android.view.View.OnClickListener() {
        @Override
        public void onClick(android.view.View v) {
            bottomSheet.dismiss();
            clearHiddenDraft(); 
            if (currentText.trim().isEmpty() && currentEditingNoteId != null && currentEditingNoteId.startsWith("Draft_")) {
                dbHelper.getWritableDatabase().delete("notes", "id=?", new String[]{currentEditingNoteId});
                loadNotesFromLocalDB();
            }
            performActualExit(); // সরাসরি বের হয়ে যাও
        }
    });
    rootLayout.addView(btnDiscard);
    
    // 🌟 ৩. বাতিল বাটন (Cancel) 🌟
    android.widget.TextView btnCancel = new android.widget.TextView(this);
    btnCancel.setText("বাতিল করুন");
    btnCancel.setTextColor(secondaryTextColor);
    btnCancel.setTextSize(16f);
    btnCancel.setGravity(android.view.Gravity.CENTER);
    btnCancel.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
    btnCancel.setPadding(0, 24, 0, 16);
    
    btnCancel.setOnClickListener(new android.view.View.OnClickListener() {
        @Override
        public void onClick(android.view.View v) {
            bottomSheet.dismiss(); // পপআপ কেটে যাবে, এডিটর আগের মতোই থাকবে
        }
    });
    rootLayout.addView(btnCancel);
    
    bottomSheet.setContentView(rootLayout);
    bottomSheet.show();
}

	
	// 🌟 ম্যাজিক: স্ক্রিন থেকে বের হওয়ার আসল মেথড 🌟
private void performActualExit() {
    layoutWordCounter.setVisibility(android.view.View.GONE); 
    if (currentIsHidden) {
        layoutVault.setVisibility(android.view.View.VISIBLE); 
    } else { 
        layoutNotepad.setVisibility(android.view.View.VISIBLE); 
        bottomNavigation.setVisibility(android.view.View.VISIBLE); 
        if (currentEditingNoteId != null && currentEditingNoteId.startsWith("Draft_")) {
            bottomNavigation.getMenu().findItem(R.id.nav_drafts).setChecked(true);
        } else {
            bottomNavigation.getMenu().findItem(R.id.nav_notepad).setChecked(true); 
        }
    }
    
    etInputText.setText(""); 
    if (etNoteTitleBig != null) etNoteTitleBig.setText(""); 
    currentEditingNoteId = null; 
    setReadingMode(false); 
    closeKeyboard(etInputText);

    if (isOpenedFromExternalActivity) {
        finish(); 
    }
}

	// ==========================================
	// 🌟 সর্বশেষ লেখা (Continue Writing) লজিক 🌟
	// ==========================================
	private void saveAsLastWrittenNote(String id, String title, String content, String label, boolean isPinned, boolean isHidden) {
		if (isHidden) return; // সিক্রেট ভল্টের নোট হোম পেজে দেখাবে না
		if (id != null && id.startsWith("Draft_")) return; // ভিজিবল ড্রাফটগুলোও হোম পেজে দেখাবে না
		
		SharedPreferences prefs = getSharedPreferences("LastWrittenNote", MODE_PRIVATE);
		prefs.edit()
		.putString("id", id)
		.putString("title", title)
		.putString("content", content)
		.putString("label", label)
		.putBoolean("isPinned", isPinned)
		.apply();
	}
	
	private void refreshLastWrittenNoteUI() {
		LinearLayout cardContinue = findViewById(R.id.cardContinue);
		TextView textview25 = findViewById(R.id.textview25); // Title
		TextView textview26 = findViewById(R.id.textview26); // Content
		
		if (cardContinue == null || textview25 == null || textview26 == null) return;
		
		SharedPreferences prefs = getSharedPreferences("LastWrittenNote", MODE_PRIVATE);
		final String id = prefs.getString("id", null);
		final String title = prefs.getString("title", "কোনো নোট পাওয়া যায়নি");
		final String content = prefs.getString("content", "লেখা শুরু করতে প্লাস বাটনে বা কোনো প্রজেক্টে ক্লিক করুন...");
		final String label = prefs.getString("label", "");
		final boolean isPinned = prefs.getBoolean("isPinned", false);
		
		// 🌟 প্রজেক্টের নাম হাইড করে শুধু পর্বের নাম দেখানো 🌟
		String tempTitle = title;
		if (tempTitle.contains("_") && (label.startsWith("Project: ") || (id != null && id.startsWith("proj_")))) {
			tempTitle = tempTitle.substring(tempTitle.indexOf("_") + 1);
		}
		
		// 🌟 ফিক্সড: জাভার নিয়মানুযায়ী ইনার ক্লাসের জন্য ভেরিয়েবলটিকে final করে দেওয়া হলো 🌟
		final String finalDisplayTitle = tempTitle;
		textview25.setText(finalDisplayTitle);
		
		// 🌟 কন্টেন্টের ছোট অংশ (Snippet) দেখানো 🌟
		String snippet = content.replaceAll("\\n", " ").replaceAll("\\s+", " ").trim();
		if (snippet.length() > 60) {
			snippet = snippet.substring(0, 60) + "...";
		} else if (snippet.isEmpty() && id != null) {
			snippet = "খালি নোট...";
		}
		textview26.setText(snippet);
		
		// 🌟 ক্লিক করলে সরাসরি এডিটরে ওপেন হবে 🌟
		if (id != null) {
			cardContinue.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					isOpenedFromExternalActivity = false;
					enterWordCounterMode(id, finalDisplayTitle, content, isPinned, label, false);
				}
			});
		} else {
			cardContinue.setOnClickListener(null);
		}
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
		// 🌟 অ্যাপ মিনিমাইজ করলে বা হোম বাটনে ক্লিক করলেও ড্রাফট সেভ হবে 🌟
		if (layoutWordCounter != null && layoutWordCounter.getVisibility() == View.VISIBLE && !currentIsHidden && etInputText != null) {
			String currentText = etInputText.getText().toString();
			boolean isReadOnly = !etInputText.isFocusable();
			
			if (!isReadOnly && !currentText.trim().isEmpty()) {
				if (currentEditingNoteId == null || currentEditingNoteId.startsWith("temp_")) {
					// নতুন নোট হলে সরাসরি ড্রাফট হিসেবে ডাটাবেসে সেভ
					String draftId = "Draft_" + System.currentTimeMillis();
					String timestamp = new java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
					String title = (etNoteTitleBig != null && !etNoteTitleBig.getText().toString().trim().isEmpty()) ? etNoteTitleBig.getText().toString().trim() : "Untitled Draft";
					dbHelper.insertOrUpdateNote(draftId, title, currentText.trim(), "Draft", timestamp, 0, 0, 1, 0);
					currentEditingNoteId = draftId; // আইডি আপডেট করে দেওয়া হলো, যাতে ডুপ্লিকেট না হয়
				} else if (currentEditingNoteId.startsWith("Draft_")) {
					// ড্রাফট আপডেট
					String timestamp = new java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
					String title = (etNoteTitleBig != null && !etNoteTitleBig.getText().toString().trim().isEmpty()) ? etNoteTitleBig.getText().toString().trim() : "Untitled Draft";
					dbHelper.insertOrUpdateNote(currentEditingNoteId, title, currentText.trim(), "Draft", timestamp, 0, 0, 1, 0);
				} else {
					// বিদ্যমান নোট হলে হিডেন ক্যাশ ড্রাফট
					saveHiddenDraft(currentText);
				}
			}
		}
	}
	
}
