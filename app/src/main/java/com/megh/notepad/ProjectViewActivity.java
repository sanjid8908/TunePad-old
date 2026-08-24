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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectViewActivity extends AppCompatActivity {
	
	private String projectName, categoryName;
	private File projectDir;
	private SharedPreferences projectDataPrefs;
	
	private ImageView btnBack, btnProjectOptions, imgCover;
	private TextView tvProjectTitle, tvProjectCategory, tvProjectDesc, tvToolbarTitle, tvChapterHeader;
	private LinearLayout rootProjectView, toolbarLayout, customFieldsContainer;
	private RecyclerView rvChapters;
	private TextView tvChapterCount;
	private ImageView btnAddChapter;
	private RecyclerView rvProjectCharacters;
	private ImageView btnAddCharacterTab;
	private TextView tvCharTabHeader;
	private EditText etNewIdea;
	private ImageView btnAddIdea;
	// 🌟 প্রিভিউ মোডের ভেরিয়েবল 🌟
	private boolean isPreviewMode = false;
	private String previewDirPath = "";
	private ImageView btnSavePreview, btnDiscardPreview;
	
	
	
	private TextView tvIdeaStats;
	private ImageView btnIdeaSearch, btnIdeaFilter, btnIdeaAdd;
	private RecyclerView rvIdeas;
	private IdeaAdapter ideaAdapter;
	private List<IdeaModel> ideaList = new ArrayList<>();
	class IdeaModel { int id; String text; boolean isDone; String timestamp; }
	private LinearLayout searchContainerIdea;
	private EditText etIdeaSearch;
	private boolean isDedicationExpanded = false;
	
	// 🌟 1. নতুন ট্যাব এবং কন্টেইনার ভেরিয়েবল 🌟
	private TextView tabChapters, tabCharacters, tabIdeas, tabSynopsis, tabGoal;
	private LinearLayout containerChapters, containerCharacters, containerIdeas, containerSynopsis, containerGoal;
	private TextView tvTotalWordCount;
	
	private ChapterAdapter chapterAdapter;
	private List<File> chapterFiles = new ArrayList<>();
	
	private int bgColor, surfaceColor, accentColor, primaryTextColor, secondaryTextColor;
	private Typeface currentTypeface = Typeface.DEFAULT;
	
	private static final int PICK_COVER_IMAGE_REQUEST = 1001;
	private ImageView previewCoverImg;
	private String tempCoverUri = ""; 
	
	private String ideaSearchQuery = "";
	private int ideaSortMode = 0; // 0=নতুন আগে, 1=পুরোনো আগে, 2=শুধু সম্পন্ন, 3=শুধু বাকি
	// 🌟 সিনোপসিস ট্যাবের ভেরিয়েবল 🌟
	private LinearLayout layoutSynopsisEmpty, layoutSynopsisContent;
	private TextView tvSynopsisTagline, tvSynopsisBody, tvSynopsisEmptyText, btnAddSynopsisEmpty;
	private ImageView btnEditSynopsis, imgSynopsisEmptyIcon;
	
	
	// 🌟 গোল ট্যাবের ভেরিয়েবল 🌟
	private LinearLayout layoutGoalEmpty, layoutGoalDashboard;
	private TextView tvGoalEmptyText, btnSetNewGoal, tvDashboardTitle, tvProgressTitle;
	private ImageView btnEditGoal;
	private android.widget.ProgressBar pbGoalProgress;
	
	
	private java.util.HashMap<String, Integer> chapterWordCounts = new java.util.HashMap<>(); // 🌟 নতুন
	
	
	
	// কার্ড ভেরিয়েবল
	private LinearLayout cardWords, cardChapters, cardStreak, cardPredictor;
	private TextView tvWordsLabel, tvWordsCount, tvChaptersLabel, tvChaptersCount;
	private TextView tvStreakCount, tvStreakLabel, tvPredictorLabel, tvPredictorMsg;
	
	// ব্যাজ ভেরিয়েবল
	private TextView tvBadgeTitle, tvBadge1Name, tvBadge2Name, tvBadge3Name;
	private LinearLayout badge1, badge2, badge3;
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.project_view);
		
		
		
		projectName = getIntent().getStringExtra("PROJECT_NAME");
		categoryName = getIntent().getStringExtra("CATEGORY_NAME");
		
		// 🌟 প্রিভিউ মোড চেক 🌟
		isPreviewMode = getIntent().getBooleanExtra("IS_PREVIEW_MODE", false);
		previewDirPath = getIntent().getStringExtra("PREVIEW_DIR_PATH");
		
		if (isPreviewMode && previewDirPath != null) {
			projectDir = new File(previewDirPath);
			
			// "Unknown" নাম ফিক্স করা
			if (projectName == null || projectName.equals("Unknown")) {
				projectName = "Imported_Project_" + (System.currentTimeMillis() % 1000); 
			}
			if (categoryName == null) categoryName = "General";
			
			preparePreviewSharedPreferences(); // মেটা-ডেটা টেম্পোরারি ফাইলে লোড করবে
			projectDataPrefs = getSharedPreferences("PreviewData_" + projectName, MODE_PRIVATE);
		} else {
			if (projectName == null) projectName = "Unknown";
			if (categoryName == null) categoryName = "General";
			projectDir = new File(getFilesDir(), "TunePad_Data/Projects/" + categoryName + "/" + projectName);
			projectDataPrefs = getSharedPreferences("ProjectData_" + projectName, MODE_PRIVATE);
		}
		
		initViews();
		applyThemeColors();
		setupPreviewUI(); // 🌟 প্রিভিউ মোড হলে আইকন চেঞ্জ করবে
		
		setupTabs();
		loadProjectDetails();
		
		btnBack.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { finish(); } });
		
		btnProjectOptions.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showProjectOptionsMenu();
			}
		});
		
		// 🌟 ফিক্সড: এই ৩টি লাইন ভুল করে মুছে গিয়েছিল! এখন লিস্ট সুন্দরভাবে রেন্ডার হবে 🌟
		rvChapters.setLayoutManager(new LinearLayoutManager(this));
		chapterAdapter = new ChapterAdapter();
		rvChapters.setAdapter(chapterAdapter);
		
		// ==========================================
		// 🌟 ম্যাজিক ১: চ্যাপ্টার রি-অর্ডারিং (Drag & Drop) ইঞ্জিন 🌟
		// ==========================================
		androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback simpleCallback = new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
		androidx.recyclerview.widget.ItemTouchHelper.UP | androidx.recyclerview.widget.ItemTouchHelper.DOWN, 0) {
			@Override
			public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
				int fromPosition = viewHolder.getAdapterPosition();
				int toPosition = target.getAdapterPosition();
				
				// লিস্টে আইটেমের পজিশন অদল-বদল করা
				java.util.Collections.swap(chapterFiles, fromPosition, toPosition);
				chapterAdapter.notifyItemMoved(fromPosition, toPosition);
				return true;
			}
			
			@Override
			public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }
			
			// 🌟 যখন ইউজার কার্ড চেপে ধরবে, তখন কার্ডটি একটু ভেসে উঠবে (প্রিমিয়াম ফিল) 🌟
			@Override
			public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
				super.onSelectedChanged(viewHolder, actionState);
				if (actionState == androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG) {
					viewHolder.itemView.setAlpha(0.7f);
					viewHolder.itemView.setScaleX(1.02f);
					viewHolder.itemView.setScaleY(1.02f);
				}
			}
			
			// 🌟 ড্রপ করার পর নতুন সিরিয়াল সেভ করা হবে 🌟
			@Override
			public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
				super.clearView(recyclerView, viewHolder);
				viewHolder.itemView.setAlpha(1.0f);
				viewHolder.itemView.setScaleX(1.0f);
				viewHolder.itemView.setScaleY(1.0f);
				
				saveChapterOrder(); // নতুন সিরিয়াল সেভ করার মেথড কল
			}
		};
		
		androidx.recyclerview.widget.ItemTouchHelper itemTouchHelper = new androidx.recyclerview.widget.ItemTouchHelper(simpleCallback);
		itemTouchHelper.attachToRecyclerView(rvChapters);
		// ==========================================
		
		
		loadChapters();
		setupCharacterTab(); // 🌟 চরিত্র ট্যাবের ম্যাজিক শুরু
		setupIdeaTab();
		setupSynopsisTab();
		setupGoalTab();
	}
	
	
	private void initViews() {
		rootProjectView = findViewById(R.id.rootProjectView);
		toolbarLayout = findViewById(R.id.toolbarLayout);
		btnBack = findViewById(R.id.btnBack);
		btnProjectOptions = findViewById(R.id.btnProjectOptions);
		imgCover = findViewById(R.id.imgCover);
		tvProjectTitle = findViewById(R.id.tvProjectTitle);
		tvProjectCategory = findViewById(R.id.tvProjectCategory);
		tvProjectDesc = findViewById(R.id.tvProjectDesc);
		tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
		tvChapterHeader = findViewById(R.id.tvChapterHeader);
		customFieldsContainer = findViewById(R.id.customFieldsContainer);
		rvChapters = findViewById(R.id.rvChapters);
		tvChapterCount = findViewById(R.id.tvChapterCount);
		btnAddChapter = findViewById(R.id.btnAddChapter);
		
		// 🌟 2. ট্যাব এবং কন্টেইনার ফাইন্ড করা 🌟
		tabChapters = findViewById(R.id.tabChapters);
		tabCharacters = findViewById(R.id.tabCharacters);
		tabIdeas = findViewById(R.id.tabIdeas);
		tabSynopsis = findViewById(R.id.tabSynopsis);
		tabGoal = findViewById(R.id.tabGoal);
		
		
		tvIdeaStats = findViewById(R.id.tvIdeaStats);
		btnIdeaSearch = findViewById(R.id.btnIdeaSearch);
		btnIdeaFilter = findViewById(R.id.btnIdeaFilter);
		btnIdeaAdd = findViewById(R.id.btnIdeaAdd);
		rvIdeas = findViewById(R.id.rvIdeas);
		
		
		containerChapters = findViewById(R.id.containerChapters);
		containerCharacters = findViewById(R.id.containerCharacters);
		containerIdeas = findViewById(R.id.containerIdeas);
		containerSynopsis = findViewById(R.id.containerSynopsis);
		containerGoal = findViewById(R.id.containerGoal);
		tvTotalWordCount = findViewById(R.id.tvTotalWordCount);
		rvProjectCharacters = findViewById(R.id.rvProjectCharacters);
		btnAddCharacterTab = findViewById(R.id.btnAddCharacterTab);
		tvCharTabHeader = findViewById(R.id.tvCharTabHeader);
		// initViews() এর ভেতরে বসাবেন:
		
		
		// অ্যাড বাটনে ক্লিক করলে CharactersActivity তে চলে যাবে (বা নতুন ডায়ালগ ওপেন হবে)
		btnAddCharacterTab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// আপাতত আমরা সরাসরি CharactersActivity তে পাঠিয়ে দিচ্ছি
				Intent intent = new Intent(ProjectViewActivity.this, CharactersActivity.class);
				intent.putExtra("PROJECT_NAME", projectName);
				startActivity(intent);
			}
		});
		
		
		btnAddChapter.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showCreateChapterDialog();
			}
		});
		searchContainerIdea = findViewById(R.id.searchContainerIdea);
		etIdeaSearch = findViewById(R.id.etIdeaSearch);
		
		layoutSynopsisEmpty = findViewById(R.id.layoutSynopsisEmpty);
		layoutSynopsisContent = findViewById(R.id.layoutSynopsisContent);
		tvSynopsisTagline = findViewById(R.id.tvSynopsisTagline);
		tvSynopsisBody = findViewById(R.id.tvSynopsisBody);
		tvSynopsisEmptyText = findViewById(R.id.tvSynopsisEmptyText);
		btnAddSynopsisEmpty = findViewById(R.id.btnAddSynopsisEmpty);
		btnEditSynopsis = findViewById(R.id.btnEditSynopsis);
		imgSynopsisEmptyIcon = findViewById(R.id.imgSynopsisEmptyIcon);
		layoutGoalEmpty = findViewById(R.id.layoutGoalEmpty);
		layoutGoalDashboard = findViewById(R.id.layoutGoalDashboard);
		tvGoalEmptyText = findViewById(R.id.tvGoalEmptyText);
		btnSetNewGoal = findViewById(R.id.btnSetNewGoal);
		tvDashboardTitle = findViewById(R.id.tvDashboardTitle);
		btnEditGoal = findViewById(R.id.btnEditGoal);
		tvProgressTitle = findViewById(R.id.tvProgressTitle);
		pbGoalProgress = findViewById(R.id.pbGoalProgress);
		
		cardWords = findViewById(R.id.cardWords);
		cardChapters = findViewById(R.id.cardChapters);
		cardStreak = findViewById(R.id.cardStreak);
		cardPredictor = findViewById(R.id.cardPredictor);
		
		tvWordsLabel = findViewById(R.id.tvWordsLabel);
		tvWordsCount = findViewById(R.id.tvWordsCount);
		tvChaptersLabel = findViewById(R.id.tvChaptersLabel);
		tvChaptersCount = findViewById(R.id.tvChaptersCount);
		tvStreakCount = findViewById(R.id.tvStreakCount);
		tvStreakLabel = findViewById(R.id.tvStreakLabel);
		tvPredictorLabel = findViewById(R.id.tvPredictorLabel);
		tvPredictorMsg = findViewById(R.id.tvPredictorMsg);
		
		tvBadgeTitle = findViewById(R.id.tvBadgeTitle);
		tvBadge1Name = findViewById(R.id.tvBadge1Name);
		tvBadge2Name = findViewById(R.id.tvBadge2Name);
		tvBadge3Name = findViewById(R.id.tvBadge3Name);
		badge1 = findViewById(R.id.badge1);
		badge2 = findViewById(R.id.badge2);
		badge3 = findViewById(R.id.badge3);
		
		ImageView btnShare = findViewById(R.id.btnShare);
		if (btnShare != null) {
			btnShare.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showShareProjectSheet();
				}
			});
		}
		
	}
	
	
private void applyThemeColors() {
    // 🌟 ১. সবার আগে থিম থেকে কালার এবং ফন্ট লোড করা 🌟
    bgColor = ThemeHelper.getBgColor(this);
    surfaceColor = ThemeHelper.getSurfaceColor(this);
    accentColor = ThemeHelper.getAccentColor(this);
    primaryTextColor = ThemeHelper.getPrimaryTextColor(this);
    secondaryTextColor = ThemeHelper.getSecondaryTextColor(this);

    // 🌟 ফিক্স: পুরোনো bangla.ttf লজিক বাদ দিয়ে আপনার নতুন ইউনিভার্সাল ফন্ট লোডার 🌟
    currentTypeface = ThemeHelper.getCustomTypeface(this);

    // ২. এবার সেই লোড করা কালারগুলো দিয়ে ভিউগুলোকে সাজানো
    
    // --- লেখিকার নাম এবং উৎসর্গপত্র ---
    LinearLayout layoutDedication = findViewById(R.id.layoutDedication);
    TextView tvAuthorName = findViewById(R.id.tvAuthorName);
    TextView tvDedicationLabel = findViewById(R.id.tvDedicationLabel);
    TextView tvDedicationText = findViewById(R.id.tvDedicationText);
    
    if (tvAuthorName != null) {
        tvAuthorName.setTextColor(primaryTextColor);
        tvAuthorName.setTypeface(currentTypeface, Typeface.BOLD);
    }
    if (tvDedicationLabel != null) {
        tvDedicationLabel.setTextColor(accentColor);
        tvDedicationLabel.setTypeface(currentTypeface, Typeface.BOLD);
    }
    if (tvDedicationText != null) {
        tvDedicationText.setTextColor(secondaryTextColor);
        tvDedicationText.setTypeface(currentTypeface);
    }
    
    if (layoutDedication != null) {
        android.graphics.drawable.GradientDrawable dedicationBg = new android.graphics.drawable.GradientDrawable();
        dedicationBg.setColor(surfaceColor); 
        dedicationBg.setCornerRadius(16f); 
        dedicationBg.setStroke(2, android.graphics.Color.argb(50, 128, 128, 128)); 
        layoutDedication.setBackground(dedicationBg);
    }
    
    // --- টপবার এবং মূল প্রজেক্ট ডিটেইলস ---
    rootProjectView.setBackgroundColor(bgColor);
    toolbarLayout.setBackgroundColor(bgColor);
    
    tvToolbarTitle.setTextColor(primaryTextColor);
    tvToolbarTitle.setTypeface(currentTypeface, Typeface.BOLD);
    tvProjectTitle.setTextColor(primaryTextColor);
    tvProjectTitle.setTypeface(currentTypeface, Typeface.BOLD);
    
    // 🌟 ফিক্স: ডেসক্রিপশনের কালার আপনি নিচে accentColor দিয়েছিলেন, সেটা এখানে ঠিক করে দিলাম
    tvProjectDesc.setTextColor(secondaryTextColor);
    tvProjectDesc.setTypeface(currentTypeface);
    
    tvChapterHeader.setTextColor(primaryTextColor);
    tvChapterHeader.setTypeface(currentTypeface, Typeface.BOLD);
    
    GradientDrawable catBg = new GradientDrawable();
    catBg.setColor(Color.argb(30, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))); 
    catBg.setCornerRadius(50f);
    tvProjectCategory.setBackground(catBg);
    tvProjectCategory.setTextColor(accentColor);
    tvProjectCategory.setTypeface(currentTypeface, Typeface.BOLD);
    
    btnBack.setColorFilter(primaryTextColor);
    btnProjectOptions.setColorFilter(primaryTextColor);
    imgCover.setBackgroundColor(surfaceColor);
    getWindow().setStatusBarColor(bgColor);
    
    tvToolbarTitle.setText(projectName);
    tvProjectTitle.setText(projectName);
    
    tvChapterCount.setTextColor(secondaryTextColor);
    tvChapterCount.setTypeface(currentTypeface);
    btnAddChapter.setColorFilter(accentColor);
    
    tvTotalWordCount.setTextColor(accentColor);
    tvTotalWordCount.setTypeface(currentTypeface, Typeface.BOLD);
    
    tvCharTabHeader.setTextColor(primaryTextColor);
    tvCharTabHeader.setTypeface(currentTypeface, Typeface.BOLD);
    btnAddCharacterTab.setColorFilter(accentColor);
    
    // --- আইডিয়া ট্যাব ---
    if(tvIdeaStats != null) {
        tvIdeaStats.setTextColor(secondaryTextColor);
        tvIdeaStats.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
        btnIdeaSearch.setColorFilter(primaryTextColor);
        btnIdeaFilter.setColorFilter(primaryTextColor);
        btnIdeaAdd.setColorFilter(accentColor);
    }
    
    if(searchContainerIdea != null) {
        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(surfaceColor);
        searchBg.setCornerRadius(100f);
        searchContainerIdea.setBackground(searchBg);
        etIdeaSearch.setTextColor(primaryTextColor);
        etIdeaSearch.setHintTextColor(secondaryTextColor);
        etIdeaSearch.setTypeface(currentTypeface);
    }
    
    // --- সিনোপসিস ট্যাব ---
    if(tvSynopsisTagline != null) {
        tvSynopsisTagline.setTextColor(accentColor);
        tvSynopsisTagline.setTypeface(currentTypeface, android.graphics.Typeface.BOLD_ITALIC);
        tvSynopsisBody.setTextColor(primaryTextColor);
        tvSynopsisBody.setTypeface(currentTypeface);
        tvSynopsisEmptyText.setTextColor(secondaryTextColor);
        tvSynopsisEmptyText.setTypeface(currentTypeface);
        imgSynopsisEmptyIcon.setColorFilter(secondaryTextColor);
        btnEditSynopsis.setColorFilter(primaryTextColor);
        
        btnAddSynopsisEmpty.setTextColor(bgColor); // বাটন টেক্সট কালার থিম অনুযায়ী
        btnAddSynopsisEmpty.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setColor(accentColor);
        btnBg.setCornerRadius(50f);
        btnAddSynopsisEmpty.setBackground(btnBg);
    }
    
    // --- গোল ট্যাব ---
    if(tvGoalEmptyText != null) {
        tvGoalEmptyText.setTextColor(secondaryTextColor);
        tvGoalEmptyText.setTypeface(currentTypeface);
        
        btnSetNewGoal.setTextColor(bgColor);
        btnSetNewGoal.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setColor(accentColor);
        btnBg.setCornerRadius(100f);
        btnSetNewGoal.setBackground(btnBg);
        
        tvDashboardTitle.setTextColor(primaryTextColor);
        tvDashboardTitle.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
        btnEditGoal.setColorFilter(primaryTextColor);
        tvProgressTitle.setTextColor(primaryTextColor);
        tvProgressTitle.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
        tvBadgeTitle.setTextColor(primaryTextColor);
        tvBadgeTitle.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
        
        android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
        cardBg.setColor(surfaceColor);
        cardBg.setCornerRadius(32f);
        cardBg.setStroke(2, android.graphics.Color.argb(20, android.graphics.Color.red(primaryTextColor), android.graphics.Color.green(primaryTextColor), android.graphics.Color.blue(primaryTextColor)));
        
        cardWords.setBackground(cardBg);
        cardChapters.setBackground(cardBg);
        cardStreak.setBackground(cardBg);
        cardPredictor.setBackground(cardBg);
        badge1.setBackground(cardBg);
        badge2.setBackground(cardBg);
        badge3.setBackground(cardBg);
        
        tvWordsLabel.setTextColor(secondaryTextColor);
        tvChaptersLabel.setTextColor(secondaryTextColor);
        tvStreakLabel.setTextColor(secondaryTextColor);
        tvPredictorLabel.setTextColor(secondaryTextColor);
        
        tvWordsCount.setTextColor(primaryTextColor);
        tvChaptersCount.setTextColor(primaryTextColor);
        tvStreakCount.setTextColor(primaryTextColor);
        tvPredictorMsg.setTextColor(primaryTextColor);
        
        tvWordsLabel.setTypeface(currentTypeface);
        tvChaptersLabel.setTypeface(currentTypeface);
        tvStreakLabel.setTypeface(currentTypeface);
        tvPredictorLabel.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
        tvPredictorMsg.setTypeface(currentTypeface);
        
        tvWordsCount.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
        tvChaptersCount.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
        tvStreakCount.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
        
        tvBadge1Name.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
        tvBadge1Name.setTextColor(primaryTextColor);
        tvBadge2Name.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
        tvBadge2Name.setTextColor(primaryTextColor);
        tvBadge3Name.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
        tvBadge3Name.setTextColor(primaryTextColor);
    }

    TextView btnReadMoreDedication = findViewById(R.id.btnReadMoreDedication);
    if (btnReadMoreDedication != null) btnReadMoreDedication.setTextColor(accentColor);
    
    TextView btnReadMoreDesc = findViewById(R.id.btnReadMoreDesc);
    if (btnReadMoreDesc != null) btnReadMoreDesc.setTextColor(accentColor);
    
    ImageView btnShare = findViewById(R.id.btnShare);
    if (btnShare != null) btnShare.setColorFilter(primaryTextColor);
    
    // 🌟 ৩. শেষ ম্যাজিক: পুরো স্ক্রিনের সব জায়গায় নতুন ফন্ট অ্যাপ্লাই করা 🌟
    applyFontToAllViews(getWindow().getDecorView(), currentTypeface);
}

	
	// ==========================================
	// 🌟 প্রিমিয়াম ট্যাব লজিক 🌟
	// ==========================================
	private void setupTabs() {
		selectTab(1); // ডিফল্ট পর্বসমূহ ওপেন থাকবে
		
		tabChapters.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { selectTab(1); } });
		tabCharacters.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { selectTab(2); } });
		tabIdeas.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { selectTab(3); } });
		tabSynopsis.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { selectTab(4); } });
		tabGoal.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { selectTab(5); } });
	}
	
	private void selectTab(int tabIndex) {
		containerChapters.setVisibility(View.GONE);
		containerCharacters.setVisibility(View.GONE);
		containerIdeas.setVisibility(View.GONE);
		containerSynopsis.setVisibility(View.GONE);
		containerGoal.setVisibility(View.GONE);
		
		resetTabStyle(tabChapters);
		resetTabStyle(tabCharacters);
		resetTabStyle(tabIdeas);
		resetTabStyle(tabSynopsis);
		resetTabStyle(tabGoal);
		
		switch (tabIndex) {
			case 1: containerChapters.setVisibility(View.VISIBLE); setActiveTabStyle(tabChapters); break;
			case 2: containerCharacters.setVisibility(View.VISIBLE); setActiveTabStyle(tabCharacters); break;
			case 3: containerIdeas.setVisibility(View.VISIBLE); setActiveTabStyle(tabIdeas); break;
			case 4: containerSynopsis.setVisibility(View.VISIBLE); setActiveTabStyle(tabSynopsis); break;
			case 5: containerGoal.setVisibility(View.VISIBLE); setActiveTabStyle(tabGoal); break;
		}
	}
	
	private void setActiveTabStyle(TextView tab) {
		tab.setTextColor(surfaceColor); 
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(accentColor); 
		bg.setCornerRadius(100f);
		tab.setBackground(bg);
	}
	
	private void resetTabStyle(TextView tab) {
		tab.setTextColor(secondaryTextColor); 
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(surfaceColor); // 🌟 ট্রান্সপারেন্টের বদলে সারফেস কালার (প্রিমিয়াম লুক)
		bg.setCornerRadius(100f);
		tab.setBackground(bg);
	}
	
	
	private String getNoteContentFromDB(String title) {
		// 🌟 প্রিভিউ মোডের ম্যাজিক: ডাটাবেসের বদলে সরাসরি টেম্প ফাইল পড়বে 🌟
		if (isPreviewMode) {
			File f = new File(projectDir, title + ".tpad");
			if (f.exists()) {
				try {
					java.io.FileInputStream fis = new java.io.FileInputStream(f);
					byte[] data = new byte[(int) f.length()];
					fis.read(data); fis.close();
					return new String(data, "UTF-8");
				} catch(Exception e){}
			}
			return "";
		}
		
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
		return content == null ? "" : content;
	}
	
	
	private int countWordsInString(String text) {
		if (text == null || text.trim().isEmpty()) return 0;
		return text.trim().split("\\s+").length;
	}
		private void loadChapters() {
		if (projectDir.exists()) {
			final File[] files = projectDir.listFiles();
			if (files != null) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						int tempTotalWords = 0;
						final List<File> tempChapterFiles = new ArrayList<>();
						final java.util.HashMap<String, Integer> tempWordCounts = new java.util.HashMap<>();
						
						for (File f : files) {
							if (f.isFile() && f.getName().endsWith(".tpad")) {
								tempChapterFiles.add(f);
								String fileName = f.getName().replace(".tpad", "");
								
								// 🌟 ফিক্সড: ProjectViewActivity.this ব্যবহার করে মেথড কল করা হলো 🌟
								int words = ProjectViewActivity.this.countWordsInString(ProjectViewActivity.this.getNoteContentFromDB(fileName));
								tempWordCounts.put(fileName, words);
								tempTotalWords += words;
							}
						}
						
						// ==========================================
						// 🌟 ম্যাজিক ৩: সেভ করা কাস্টম সিরিয়াল (Drag & Drop Order) অনুযায়ী সাজানো 🌟
						// ==========================================
						String savedOrder = projectDataPrefs.getString("chapter_custom_order", "");
						if (!savedOrder.isEmpty()) {
							final java.util.List<String> orderList = java.util.Arrays.asList(savedOrder.split(";;"));
							
							java.util.Collections.sort(tempChapterFiles, new java.util.Comparator<File>() {
								@Override
								public int compare(File f1, File f2) {
									int index1 = orderList.indexOf(f1.getName());
									int index2 = orderList.indexOf(f2.getName());
									
									// যদি দুটোর কোনোটিই লিস্টে না থাকে (যেমন: একদম নতুন পর্ব)
									if (index1 == -1 && index2 == -1) return f1.getName().compareTo(f2.getName());
									// নতুন ফাইল হলে সবার শেষে দেখাবে
									if (index1 == -1) return 1;
									if (index2 == -1) return -1;
									
									return Integer.compare(index1, index2);
								}
							});
						}
						// ==========================================
						
						final int finalTotalWords = tempTotalWords;
						
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								// 🌟 সেফটি চেক ১: অ্যাক্টিভিটি বন্ধ হয়ে গেলে UI আপডেট করবে না 🌟
								if (isFinishing() || isDestroyed()) return;
								
								try {
									// 🌟 সেফটি চেক ২: প্রতিটি ভিউ এবং অবজেক্ট Null কি না তা চেক করে কাজ করবে 🌟
									if (chapterFiles != null) chapterFiles.clear();
									if (chapterWordCounts != null) chapterWordCounts.clear(); 
									else chapterWordCounts = new java.util.HashMap<>();
									
									if (tempChapterFiles != null) chapterFiles.addAll(tempChapterFiles);
									if (tempWordCounts != null) chapterWordCounts.putAll(tempWordCounts);
									
									if (chapterAdapter != null) chapterAdapter.notifyDataSetChanged(); 
									
									if (tvChapterCount != null) tvChapterCount.setText("(" + chapterFiles.size() + ")");
									if (tvTotalWordCount != null) tvTotalWordCount.setText("মোট: " + finalTotalWords + " শব্দ");
									
									if (projectDataPrefs != null) {
										projectDataPrefs.edit().putInt("total_words", finalTotalWords).apply();
									}
									
									loadGoalData(); 
								} catch (Exception e) {
									e.printStackTrace(); // ক্র্যাশ না করে শুধু লগে এরর দেখাবে
								}
							}
						});
					}
				}).start(); 
			}
		} else {
			// যদি ফোল্ডার না থাকে তবে সব ক্লিয়ার করে জিরো দেখাবে
			if (chapterFiles != null) chapterFiles.clear();
			if (chapterWordCounts != null) chapterWordCounts.clear(); 
			if (chapterAdapter != null) chapterAdapter.notifyDataSetChanged();
			if (tvChapterCount != null) tvChapterCount.setText("(0)");
			if (tvTotalWordCount != null) tvTotalWordCount.setText("মোট: 0 শব্দ");
		}
	}

	
	
	
	
	
	
	private void exportSuperSecureProject() {
		File metaDataFile = new File(projectDir, "project_meta_data.json"); // উপরে ডিক্লেয়ার করা হলো
		try {
			File downloadsRoot = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
			File appDownloadDir = new File(downloadsRoot, "TunePad");
			if (!appDownloadDir.exists()) appDownloadDir.mkdirs();
			
			File finalTboxFile = new File(appDownloadDir, projectName + ".tbox");
			
			saveSharedPreferencesToFile(metaDataFile);
			
			TBoxUtils.zipAndEncryptFolder(projectDir, finalTboxFile); 
			
			Toast.makeText(this, "সুপার-সিকিউর প্রজেক্ট সফলভাবে " + projectName + ".tbox নামে Downloads-এ সেভ হয়েছে! 🔒", Toast.LENGTH_LONG).show();
			
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "এক্সপোর্ট করতে সমস্যা হয়েছে: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		} finally {
			// 🌟 সিকিউরিটি ফিক্স: এরর আসুক বা না আসুক, এই ফাইলটি মুছে ফেলা হবেই 🌟
			if (metaDataFile.exists()) {
				metaDataFile.delete();
			}
		}
	}
	// ==========================================
	// 🌟 প্রজেক্টের ভেতরের সব সেটিংস এবং চরিত্র ব্যাকআপ নেওয়ার ম্যাজিক (ProjectViewActivity) 🌟
	// ==========================================
	private void saveSharedPreferencesToFile(File metaFile) {
		try {
			org.json.JSONObject json = new org.json.JSONObject();
			java.util.Map<String, ?> allEntries = projectDataPrefs.getAll();
			
			for (java.util.Map.Entry<String, ?> entry : allEntries.entrySet()) {
				json.put(entry.getKey(), entry.getValue());
			}
			
			// 🌟 ম্যাজিক: ক্যারেক্টার ও রিলেশনশিপ এক্সপোর্ট লজিক যুক্ত করা হলো 🌟
			SharedPreferences charPrefs = getSharedPreferences("Global_Characters_DB", MODE_PRIVATE);
			int charCount = charPrefs.getInt("char_count", 0);
			org.json.JSONArray charArray = new org.json.JSONArray();
			
			for (int i = 0; i < charCount; i++) {
				if (charPrefs.getBoolean("char_active_" + i, false)) {
					String story = charPrefs.getString("char_story_" + i, "");
					
					// বর্তমান প্রজেক্টের নামের সাথে মিলিয়ে চেক করা (projectName ভেরিয়েবল)
					if (story.trim().equalsIgnoreCase(projectName.trim())) {
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
			
			// JSON ডেটা ফাইলে লেখা
			java.io.FileOutputStream fos = new java.io.FileOutputStream(metaFile);
			fos.write(json.toString().getBytes("UTF-8"));
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private void showProjectEditBottomSheet() {
		final com.google.android.material.bottomsheet.BottomSheetDialog editSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
		editSheet.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		
		LinearLayout rootLayout = new LinearLayout(this);
		rootLayout.setOrientation(LinearLayout.VERTICAL);
		rootLayout.setBackgroundColor(surfaceColor);
		rootLayout.setPadding(48, 56, 48, 64);
		
		TextView titleView = new TextView(this);
		titleView.setText("প্রজেক্ট এডিট করুন");
		titleView.setTextColor(primaryTextColor);
		titleView.setTextSize(20f);
		titleView.setTypeface(currentTypeface, Typeface.BOLD);
		titleView.setPadding(0, 0, 0, 32);
		rootLayout.addView(titleView);
		
		LinearLayout imgLayout = new LinearLayout(this);
		imgLayout.setOrientation(LinearLayout.HORIZONTAL);
		imgLayout.setGravity(Gravity.CENTER_VERTICAL);
		imgLayout.setPadding(0, 0, 0, 32);
		
		androidx.cardview.widget.CardView imgCard = new androidx.cardview.widget.CardView(this);
		imgCard.setRadius(24f);
		imgCard.setCardElevation(0f);
		imgCard.setLayoutParams(new LinearLayout.LayoutParams(180, 240)); 
		
		previewCoverImg = new ImageView(this);
		previewCoverImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
		previewCoverImg.setBackgroundColor(bgColor);
		tempCoverUri = projectDataPrefs.getString("cover_uri", "");
		
		File coverFile = new File(projectDir, "cover.jpg");
		if (coverFile.exists()) {
			previewCoverImg.setImageURI(android.net.Uri.fromFile(coverFile));
		} else if(!tempCoverUri.isEmpty()) {
			previewCoverImg.setImageURI(android.net.Uri.parse(tempCoverUri));
		} else {
			previewCoverImg.setImageResource(android.R.drawable.ic_menu_gallery);
			previewCoverImg.setColorFilter(Color.parseColor("#9CA8AE"));
			previewCoverImg.setScaleType(ImageView.ScaleType.CENTER);
		}
		
		imgCard.addView(previewCoverImg);
		imgLayout.addView(imgCard);
		
		TextView btnUploadCover = new TextView(this);
		btnUploadCover.setText("কভার ছবি পরিবর্তন করুন");
		btnUploadCover.setTextColor(accentColor);
		btnUploadCover.setTextSize(14f);
		btnUploadCover.setTypeface(currentTypeface, Typeface.BOLD);
		btnUploadCover.setPadding(32, 16, 16, 16);
		btnUploadCover.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("image/*");
				startActivityForResult(intent, PICK_COVER_IMAGE_REQUEST);
			}
		});
		imgLayout.addView(btnUploadCover);
		rootLayout.addView(imgLayout);
		
		GradientDrawable inputBg = new GradientDrawable();
		inputBg.setColor(bgColor);
		inputBg.setCornerRadius(24f);
		
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, 0, 24);
		
		final EditText etAuthorName = new EditText(this);
		etAuthorName.setHint("লেখিকার নাম");
		etAuthorName.setHintTextColor(secondaryTextColor);
		etAuthorName.setTextColor(primaryTextColor);
		etAuthorName.setBackground(inputBg);
		etAuthorName.setPadding(40, 32, 40, 32);
		etAuthorName.setText(projectDataPrefs.getString("author_name", "Shubhra Afroj Tunerosa")); 
		etAuthorName.setLayoutParams(params);
		etAuthorName.setTypeface(currentTypeface);
		rootLayout.addView(etAuthorName);
		
		final EditText etGenre = new EditText(this);
		etGenre.setHint("জঁনরা (যেমন: রোমান্টিক)");
		etGenre.setHintTextColor(secondaryTextColor);
		etGenre.setTextColor(primaryTextColor);
		etGenre.setBackground(inputBg);
		etGenre.setPadding(40, 32, 40, 32);
		etGenre.setText(projectDataPrefs.getString("genre", ""));
		etGenre.setLayoutParams(params);
		etGenre.setTypeface(currentTypeface);
		rootLayout.addView(etGenre);
		
		// 🌟 সারাংশ TextArea (মোমেন্টাম স্ক্রোলিং সহ) 🌟
		final EditText etSummary = new EditText(this);
		etSummary.setHint("প্রজেক্টের সারাংশ বা ডেসক্রিপশন...");
		etSummary.setHintTextColor(secondaryTextColor);
		etSummary.setTextColor(primaryTextColor);
		etSummary.setBackground(inputBg);
		etSummary.setPadding(40, 32, 40, 32);
		etSummary.setText(projectDataPrefs.getString("desc", ""));
		etSummary.setLayoutParams(params);
		etSummary.setTypeface(currentTypeface);
		
		etSummary.setSingleLine(false);
		etSummary.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		etSummary.setLines(5);
		etSummary.setMaxLines(8);
		etSummary.setGravity(Gravity.TOP | Gravity.START);
		etSummary.setVerticalScrollBarEnabled(true);
		etSummary.setMovementMethod(new android.text.method.ScrollingMovementMethod());
		etSummary.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, android.view.MotionEvent event) {
				v.getParent().requestDisallowInterceptTouchEvent(true);
				if ((event.getAction() & android.view.MotionEvent.ACTION_MASK) == android.view.MotionEvent.ACTION_UP) {
					v.getParent().requestDisallowInterceptTouchEvent(false);
				}
				return false;
			}
		});
		rootLayout.addView(etSummary);
		
		// 🌟 উৎসর্গপত্র TextArea (মোমেন্টাম স্ক্রোলিং সহ) 🌟
		final EditText etDedication = new EditText(this);
		etDedication.setHint("উৎসর্গপত্র লিখুন...");
		etDedication.setHintTextColor(secondaryTextColor);
		etDedication.setTextColor(primaryTextColor);
		etDedication.setBackground(inputBg);
		etDedication.setPadding(40, 32, 40, 32);
		String defDedication = "যাদের অনুপ্রেরণায় এই বইটি আজ আলোর মুখ দেখলো...\n\nআমার বাবা-মা এবং সেইসব পাঠকদের, যারা সব সময় আমার পাশে থেকেছেন। আপনাদের ভালোবাসা ছাড়া এই পথচলা অসম্ভব ছিল।";
		etDedication.setText(projectDataPrefs.getString("dedication_text", defDedication));
		etDedication.setLayoutParams(params);
		etDedication.setTypeface(currentTypeface);
		
		etDedication.setSingleLine(false);
		etDedication.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		etDedication.setLines(4);
		etDedication.setMaxLines(7);
		etDedication.setGravity(Gravity.TOP | Gravity.START);
		etDedication.setVerticalScrollBarEnabled(true);
		etDedication.setMovementMethod(new android.text.method.ScrollingMovementMethod());
		etDedication.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, android.view.MotionEvent event) {
				v.getParent().requestDisallowInterceptTouchEvent(true);
				if ((event.getAction() & android.view.MotionEvent.ACTION_MASK) == android.view.MotionEvent.ACTION_UP) {
					v.getParent().requestDisallowInterceptTouchEvent(false);
				}
				return false;
			}
		});
		rootLayout.addView(etDedication);
		
		TextView tvCustomTitle = new TextView(this);
		tvCustomTitle.setText("নতুন ফিল্ড যুক্ত করুন (ঐচ্ছিক):");
		tvCustomTitle.setTextColor(secondaryTextColor);
		tvCustomTitle.setTypeface(currentTypeface);
		tvCustomTitle.setPadding(0, 16, 0, 16);
		rootLayout.addView(tvCustomTitle);
		
		LinearLayout customInputLayout = new LinearLayout(this);
		customInputLayout.setOrientation(LinearLayout.HORIZONTAL);
		customInputLayout.setLayoutParams(params);
		
		final EditText etFieldKey = new EditText(this);
		etFieldKey.setHint("ফিল্ড");
		etFieldKey.setHintTextColor(secondaryTextColor);
		etFieldKey.setTextColor(primaryTextColor);
		etFieldKey.setBackground(inputBg);
		etFieldKey.setPadding(40, 32, 40, 32);
		etFieldKey.setTypeface(currentTypeface);
		etFieldKey.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
		
		final EditText etFieldVal = new EditText(this);
		etFieldVal.setHint("ভ্যালু");
		etFieldVal.setHintTextColor(secondaryTextColor);
		etFieldVal.setTextColor(primaryTextColor);
		etFieldVal.setBackground(inputBg);
		etFieldVal.setPadding(40, 32, 40, 32);
		etFieldVal.setTypeface(currentTypeface);
		LinearLayout.LayoutParams valParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
		valParams.setMargins(16, 0, 16, 0);
		etFieldVal.setLayoutParams(valParams);
		
		TextView btnAddField = new TextView(this);
		btnAddField.setText("Add");
		btnAddField.setTextColor(surfaceColor);
		btnAddField.setTextSize(12f);
		btnAddField.setTypeface(currentTypeface, Typeface.BOLD);
		btnAddField.setPadding(32, 32, 32, 32);
		btnAddField.setGravity(Gravity.CENTER);
		GradientDrawable addBg = new GradientDrawable();
		addBg.setColor(accentColor);
		addBg.setCornerRadius(24f);
		btnAddField.setBackground(addBg);
		
		customInputLayout.addView(etFieldKey);
		customInputLayout.addView(etFieldVal);
		customInputLayout.addView(btnAddField);
		rootLayout.addView(customInputLayout);
		
		btnAddField.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String key = etFieldKey.getText().toString().trim();
				String val = etFieldVal.getText().toString().trim();
				if (!key.isEmpty() && !val.isEmpty()) {
					int count = projectDataPrefs.getInt("custom_field_count", 0);
					projectDataPrefs.edit().putString("custom_key_" + count, key).putString("custom_val_" + count, val).putInt("custom_field_count", count + 1).apply();
					Toast.makeText(ProjectViewActivity.this, "ফিল্ড যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show();
					etFieldKey.setText(""); etFieldVal.setText("");
				} else { Toast.makeText(ProjectViewActivity.this, "ফিল্ড এবং ভ্যালু দিন!", Toast.LENGTH_SHORT).show(); }
			}
		});
		
		TextView btnSave = new TextView(this);
		btnSave.setText("সেভ করুন");
		btnSave.setTextColor(surfaceColor);
		btnSave.setGravity(Gravity.CENTER);
		btnSave.setTextSize(16f);
		btnSave.setTypeface(currentTypeface, Typeface.BOLD);
		btnSave.setPadding(0, 40, 0, 40);
		GradientDrawable saveBg = new GradientDrawable();
		saveBg.setColor(accentColor);
		saveBg.setCornerRadius(100f);
		btnSave.setBackground(saveBg);
		
		btnSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String authorName = etAuthorName.getText().toString().trim();
				String genre = etGenre.getText().toString().trim();
				String desc = etSummary.getText().toString().trim();
				String dedication = etDedication.getText().toString().trim();
				
				if(!tempCoverUri.isEmpty() && tempCoverUri.startsWith("content://")) {
					try {
						java.io.InputStream inputStream = getContentResolver().openInputStream(android.net.Uri.parse(tempCoverUri));
						File coverFile = new File(projectDir, "cover.jpg");
						
						if(coverFile.exists()) {
							coverFile.delete();
						}
						
						java.io.FileOutputStream outputStream = new java.io.FileOutputStream(coverFile);
						byte[] buffer = new byte[1024];
						int length;
						while ((length = inputStream.read(buffer)) > 0) {
							outputStream.write(buffer, 0, length);
						}
						outputStream.flush();
						inputStream.close();
						outputStream.close();
						
						tempCoverUri = coverFile.getAbsolutePath(); 
					} catch (Exception e) {
						Toast.makeText(ProjectViewActivity.this, "ছবি সেভ হতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show();
					}
				}
				
				projectDataPrefs.edit()
				.putString("author_name", authorName)
				.putString("genre", genre)
				.putString("desc", desc)
				.putString("dedication_text", dedication)
				.putString("cover_uri", tempCoverUri)
				.apply();
				
				loadProjectDetails(); 
				editSheet.dismiss();
				Toast.makeText(ProjectViewActivity.this, "প্রজেক্ট আপডেট হয়েছে!", Toast.LENGTH_SHORT).show();
			}
		});
		
		rootLayout.addView(btnSave);
		
		// 🌟 ফুলস্ক্রিন করার জন্য হাইট MATCH_PARENT করা হলো 🌟
		android.widget.ScrollView scrollContainer = new android.widget.ScrollView(this);
		scrollContainer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		rootLayout.setLayoutParams(new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		
		scrollContainer.addView(rootLayout);
		editSheet.setContentView(scrollContainer);
         
		editSheet.show();
		
		// 🌟 বটম শিটকে জোর করে ফুলস্ক্রিনে এক্সপ্যান্ড করার ম্যাজিক (Crash Fixed) 🌟
		android.view.View bottomSheetInternal = editSheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
		if (bottomSheetInternal != null) {
			// ১. শিটটাকে এক্সপ্যান্ডেড মোডে ওপেন করা
			com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheetInternal).setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
			
			// ২. ক্র্যাশ ফিক্স: লেআউট প্যারামিটার নতুন করে না বানিয়ে শুধু হাইটটা আপডেট করা
			android.view.ViewGroup.LayoutParams layoutParams = bottomSheetInternal.getLayoutParams();
			layoutParams.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
			bottomSheetInternal.setLayoutParams(layoutParams);
		}
		
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == PICK_COVER_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
			android.net.Uri imageUri = data.getData();
			try { getContentResolver().takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception e) {}
			tempCoverUri = imageUri.toString();
			if(previewCoverImg != null) {
				previewCoverImg.setImageURI(imageUri);
			}
		}
	}// ==========================================
	// 🌟 ডাইনামিক কিউট এডাপ্টার (মিনিমাল ড্রপডাউন এবং শব্দসংখ্যা সহ) 🌟
	// ==========================================
	private class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {
		
		private int expandedPosition = -1;
		
		@NonNull
		@Override
		public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LinearLayout card = new LinearLayout(ProjectViewActivity.this);
			card.setOrientation(LinearLayout.VERTICAL);
			RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			lp.setMargins(40, 16, 40, 16);
			card.setLayoutParams(lp);
			card.setPadding(40, 40, 40, 40);
			
			GradientDrawable shape = new GradientDrawable();
			shape.setCornerRadius(32f);
			shape.setColor(surfaceColor); 
			card.setBackground(shape);
			
			LinearLayout topLayout = new LinearLayout(ProjectViewActivity.this);
			topLayout.setOrientation(LinearLayout.HORIZONTAL);
			topLayout.setGravity(Gravity.CENTER_VERTICAL);
			
			TextView tvTitle = new TextView(ProjectViewActivity.this);
			tvTitle.setTextColor(primaryTextColor);
			tvTitle.setTextSize(16f);
			tvTitle.setTypeface(currentTypeface, Typeface.BOLD);
			tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
			
			// 🌟 নতুন: প্রতিটি পর্বের শব্দ সংখ্যা দেখানোর জন্য 🌟
			TextView tvWordCount = new TextView(ProjectViewActivity.this);
			tvWordCount.setTextColor(secondaryTextColor);
			tvWordCount.setTextSize(12f);
			tvWordCount.setTypeface(currentTypeface);
			tvWordCount.setPadding(16, 0, 16, 0);
			
			int iconSize = (int) (28 * getResources().getDisplayMetrics().density);
			int iconPadding = (int) (4 * getResources().getDisplayMetrics().density);
			
			ImageView imgExpand = new ImageView(ProjectViewActivity.this);
			imgExpand.setImageResource(R.drawable.arrow_down_float); // আপনার অ্যারো আইকন
			imgExpand.setColorFilter(secondaryTextColor);
			LinearLayout.LayoutParams paramsE = new LinearLayout.LayoutParams(iconSize, iconSize);
			imgExpand.setLayoutParams(paramsE);
			imgExpand.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
			
			ImageView imgMore = new ImageView(ProjectViewActivity.this);
			imgMore.setImageResource(R.drawable.ic_menu_more); // আপনার মোর আইকন
			imgMore.setColorFilter(secondaryTextColor);
			LinearLayout.LayoutParams paramsM = new LinearLayout.LayoutParams(iconSize, iconSize);
			paramsM.setMarginStart(16);
			imgMore.setLayoutParams(paramsM);
			imgMore.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
			
			topLayout.addView(tvTitle);
			topLayout.addView(tvWordCount); // 🌟 শব্দ সংখ্যা যুক্ত করা হলো
			topLayout.addView(imgExpand);
			topLayout.addView(imgMore);
			card.addView(topLayout);
			
			// 🌟 কিউট ড্রপডাউন সারাংশ বক্স 🌟
			LinearLayout expandLayout = new LinearLayout(ProjectViewActivity.this);
			expandLayout.setOrientation(LinearLayout.VERTICAL);
			expandLayout.setVisibility(View.GONE);
			LinearLayout.LayoutParams expParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			expParams.setMargins(0, 24, 0, 0);
			expandLayout.setLayoutParams(expParams);
			expandLayout.setPadding(32, 24, 32, 24);
			
			GradientDrawable summaryBg = new GradientDrawable();
			summaryBg.setColor(bgColor); 
			summaryBg.setCornerRadius(20f);
			expandLayout.setBackground(summaryBg);
			
			TextView tvSummary = new TextView(ProjectViewActivity.this);
			tvSummary.setTextColor(secondaryTextColor);
			tvSummary.setTextSize(13f);
			tvSummary.setTypeface(currentTypeface);
			tvSummary.setLineSpacing(0, 1.2f);
			expandLayout.addView(tvSummary);
			
			LinearLayout editBtnLayout = new LinearLayout(ProjectViewActivity.this);
			editBtnLayout.setOrientation(LinearLayout.HORIZONTAL);
			editBtnLayout.setGravity(Gravity.CENTER_VERTICAL);
			editBtnLayout.setPadding(0, 16, 0, 0);
			
			ImageView imgEditPen = new ImageView(ProjectViewActivity.this);
			imgEditPen.setImageResource(R.drawable.ic_menu_edit); 
			imgEditPen.setColorFilter(accentColor);
			imgEditPen.setLayoutParams(new LinearLayout.LayoutParams(40, 40));
			editBtnLayout.addView(imgEditPen);
			
			TextView btnEditSummary = new TextView(ProjectViewActivity.this);
			btnEditSummary.setText(" এডিট করুন");
			btnEditSummary.setTextColor(accentColor);
			btnEditSummary.setTextSize(12f);
			btnEditSummary.setTypeface(currentTypeface, Typeface.BOLD);
			editBtnLayout.addView(btnEditSummary);
			
			expandLayout.addView(editBtnLayout);
			card.addView(expandLayout);
			
			return new ChapterViewHolder(card, tvTitle, tvWordCount, imgExpand, imgMore, expandLayout, tvSummary, editBtnLayout);
		}
		
		@Override
		public void onBindViewHolder(@NonNull final ChapterViewHolder holder, final int position) {
			final File file = chapterFiles.get(position);
			final String fileName = file.getName().replace(".tpad", "");
			
			// ১. পর্বের নাম সেট করা
			holder.tvTitle.setText(fileName);
			
			// 🌟 ২. পারফরম্যান্স ফিক্স: সরাসরি ডাটাবেস কল না করে ম্যাপ থেকে শব্দসংখ্যা নেওয়া হচ্ছে 🌟
			int words = 0;
			if (chapterWordCounts != null && chapterWordCounts.containsKey(fileName)) {
				words = chapterWordCounts.get(fileName);
			}
			holder.tvWordCount.setText(words + " শব্দ");
			
			// ৩. ড্রপডাউন (Expand/Collapse) লজিক
			final boolean isExpanded = position == expandedPosition;
			holder.expandLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
			
			// ৪. সারাংশ সেট করা
			final String savedSummary = projectDataPrefs.getString("summary_" + fileName, "এই পর্বের কোনো সারাংশ নেই।");
			holder.tvSummary.setText(savedSummary);
			
			// ৫. অ্যারো বাটনে ক্লিক করলে ড্রপডাউন খুলবে/বন্ধ হবে
			holder.imgExpand.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					expandedPosition = isExpanded ? -1 : position;
					notifyDataSetChanged(); 
				}
			});
			
			// ৬. সারাংশ এডিট করার বাটন
			holder.btnEditSummary.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showEditDialog(fileName + " এর সারাংশ", savedSummary, new OnTextSavedListener() {
						@Override
						public void onSaved(String text) {
							projectDataPrefs.edit().putString("summary_" + fileName, text).apply();
							notifyItemChanged(position); // শুধু ওই নির্দিষ্ট কার্ডটাই রিফ্রেশ হবে
						}
					});
				}
			});
			
			// ৭. পুরো কার্ডে ক্লিক করলে রিড/এডিট মোডে ফাইল ওপেন হবে (প্রিভিউ মোড ফিক্স সহ)
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// 🌟 প্রিভিউ মোড চেক: এডিটরের বদলে সরাসরি রিড-অনলি ডায়ালগ দেখাবো 🌟
					if (isPreviewMode) {
						showPreviewChapterDialog(fileName);
					} else {
						// নরমাল মোডে এডিটর ওপেন হবে
						openNoteInEditor(fileName);
					}
				}
			});
			
			// ৮. থ্রি-ডট (More) বাটনে ক্লিক করলে অপশন শিট আসবে
			holder.imgMore.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showNoteOptions(fileName, file);
				}
			});
		}
		
		
		@Override
		public int getItemCount() { return chapterFiles.size(); }
		
		class ChapterViewHolder extends RecyclerView.ViewHolder {
			LinearLayout card, expandLayout, topLayout;
			TextView tvTitle, tvWordCount, tvSummary;
			View btnEditSummary;
			ImageView imgExpand, imgMore;
			
			public ChapterViewHolder(@NonNull View itemView, TextView t, TextView wc, ImageView e, ImageView m, LinearLayout exp, TextView sum, View editSum) {
				super(itemView);
				card = (LinearLayout) itemView;
				tvTitle = t; tvWordCount = wc; imgExpand = e; imgMore = m;
				expandLayout = exp; tvSummary = sum; btnEditSummary = editSum;
			}
		}
	}
	private void openNoteInEditor(String fileName) {
		String uniqueDbTitle = projectName + "_" + fileName;
		android.database.sqlite.SQLiteDatabase db = null;
		android.database.Cursor cursor = null;
		
		try {
			db = openOrCreateDatabase("notes_db_v3", MODE_PRIVATE, null);
			cursor = db.rawQuery("SELECT id, content, label, isPinned, isHidden FROM notes WHERE title=?", new String[]{uniqueDbTitle});
			
			if (!cursor.moveToFirst()) {
				cursor.close();
				cursor = db.rawQuery("SELECT id, content, label, isPinned, isHidden FROM notes WHERE title=?", new String[]{fileName});
			}
			
			if (cursor.moveToFirst()) {
				Intent intent = new Intent(ProjectViewActivity.this, MmmActivity.class);
				intent.putExtra("OPEN_NOTE", true);
				intent.putExtra("noteId", cursor.getString(0));
				intent.putExtra("title", fileName); 
				intent.putExtra("content", cursor.getString(1));
				intent.putExtra("label", cursor.getString(2));
				intent.putExtra("isPinned", cursor.getInt(3) == 1);
				intent.putExtra("isHidden", cursor.getInt(4) == 1);
				startActivity(intent);
			} else { 
				Toast.makeText(this, "নোটটি ডাটাবেসে পাওয়া যায়নি!", Toast.LENGTH_SHORT).show(); 
			}
		} catch (Exception e) {
			Toast.makeText(this, "নোট ওপেন করতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show(); 
		} finally {
			// 🌟 ফিক্সড: মেমরি লিক থেকে বাঁচতে ১০০% ক্লোজ করা হচ্ছে 🌟
			if (cursor != null && !cursor.isClosed()) cursor.close(); 
			if (db != null && db.isOpen()) db.close();
		}
	}
	// ==========================================
	// 🌟 প্রজেক্ট অপশন মেনু (ভেরিয়েবল ফিক্সড) 🌟
	// ==========================================
	private void showProjectOptionsMenu() {
		final BottomSheetDialog sheet = new BottomSheetDialog(this);
		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setBackgroundColor(surfaceColor);
		root.setPadding(0, 32, 0, 32);
		
		root.addView(createMenuItem("ইডিট প্রজেক্ট", android.R.drawable.ic_menu_edit, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sheet.dismiss();
				showProjectEditBottomSheet(); 
			}
		}));
		
		root.addView(createMenuItem("পড়ুন 📖", android.R.drawable.ic_menu_info_details, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sheet.dismiss();
				Intent intent = new Intent(ProjectViewActivity.this, ProjectReadActivity.class);
				intent.putExtra("PROJECT_NAME", projectName);
				intent.putExtra("CATEGORY_NAME", categoryName);
				startActivity(intent);
			}
		}));
		
		root.addView(createMenuItem("প্রজেক্ট এক্সপোর্ট করুন (.tbox)", android.R.drawable.ic_menu_save, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sheet.dismiss();
				exportSuperSecureProject(); 
			}
		}));
		
		// 🌟 PDF এক্সপোর্ট বাটন 🌟
		root.addView(createMenuItem("PDF হিসেবে সেভ করুন 📄", android.R.drawable.ic_menu_gallery, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sheet.dismiss();
				exportProjectAsPDF(); 
			}
		}));
		
		// 🌟 Word (DOC) এক্সপোর্ট বাটন 🌟
		root.addView(createMenuItem("Word (DOC) হিসেবে সেভ করুন 📝", android.R.drawable.ic_menu_edit, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sheet.dismiss();
				exportProjectAsDoc(); 
			}
		}));
		
		// 🌟 স্মার্ট শেয়ার বাটন (projectDir এবং projectName ফিক্স করা হয়েছে) 🌟
		root.addView(createMenuItem("লিংক শেয়ার করুন 🔗", android.R.drawable.ic_menu_share, new android.view.View.OnClickListener() { 
			@Override
			public void onClick(android.view.View v) {
				sheet.dismiss();
				if (isNetworkAvailable()) {
					uploadAndShareProject(projectDir, projectName); // 🌟 ফিক্সড
				} else {
					android.widget.Toast.makeText(getApplicationContext(), "ইন্টারনেট নেই! অফলাইন লিংক তৈরি করা হচ্ছে... 📶", android.widget.Toast.LENGTH_SHORT).show();
					shareOfflinePredictableLink(projectName); // 🌟 ফিক্সড
				}
			}
		}));
		
		// 🌟 অটো-ব্যাকআপ টগল বাটন (projectName ফিক্স করা হয়েছে) 🌟
		final android.content.SharedPreferences autoBackupPrefs = getSharedPreferences("AutoBackupPrefs", android.content.Context.MODE_PRIVATE);
		final boolean isAutoBackupOn = autoBackupPrefs.getBoolean("auto_backup_" + projectName, false); // 🌟 ফিক্সড
		
		root.addView(createMenuItem(isAutoBackupOn ? "অটো-ব্যাকআপ বন্ধ করুন 🛑" : "অটো-ব্যাকআপ চালু করুন (৫ ঘণ্টা) 🔄", 
		android.R.drawable.ic_popup_sync, new android.view.View.OnClickListener() { 
			@Override
			public void onClick(android.view.View v) {
				sheet.dismiss();
				boolean newState = !isAutoBackupOn;
				autoBackupPrefs.edit().putBoolean("auto_backup_" + projectName, newState).apply(); // 🌟 ফিক্সড
				android.widget.Toast.makeText(getApplicationContext(), newState ? "'" + projectName + "' এর অটো-ব্যাকআপ চালু হয়েছে!" : "'" + projectName + "' এর অটো-ব্যাকআপ বন্ধ করা হয়েছে!", android.widget.Toast.LENGTH_SHORT).show(); // 🌟 ফিক্সড
			}
		}));
		
		sheet.setContentView(root);
		sheet.show();
	}
	
	// ==========================================
	// 🌟 নির্দিষ্ট পর্ব বা নোট অপশন মেনু (WhatsApp Limit Bypass - .tpad Format) 🌟
	// ==========================================
	private void showNoteOptions(final String fileName, final File file) {
		final BottomSheetDialog sheet = new BottomSheetDialog(this);
		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setBackgroundColor(surfaceColor);
		root.setPadding(0, 32, 0, 32);
		
		// 🌟 ডাটাবেস থেকে অরিজিনাল লেখা এবং ডিপ ক্লিন লেখা দুটোই তৈরি করা হলো 🌟
		final String rawContent = getNoteContentFromDB(fileName);
		String processedContent = rawContent != null ? rawContent : "";
		processedContent = processedContent.replace("<br>", "\n")
		.replace("&nbsp;", " ")
		.replace("\uFFFC", " ")
		.replaceAll("<[^>]*>", "")
		.replaceAll("[\\x00-\\x09\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
		final String cleanContent = processedContent.trim();
		
		// 🌟 ১. কপি বাটন 🌟
		root.addView(createMenuItem("কপি করুন", android.R.drawable.ic_menu_edit, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
				android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Chapter", cleanContent);
				clipboard.setPrimaryClip(clip);
				
				showCustomToastSheet("পর্বটি কপি করা হয়েছে!"); 
				sheet.dismiss();
			}
		}));
		
		// 🌟 ২. শেয়ার বাটন (TunePad এর নিজস্ব .tpad ফাইল শেয়ার) 🌟
		root.addView(createMenuItem("শেয়ার করুন", android.R.drawable.ic_menu_share, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sheet.dismiss();
				
				if (cleanContent.length() > 3000) {
					// 🌟 লেখা অনেক বড় হলে WhatsApp কেটে দেবে, তাই .tpad ফাইল বানিয়ে পাঠানো হচ্ছে 🌟
					try {
						File cacheDir = new File(getCacheDir(), "TunePad_Shared");
						if (!cacheDir.exists()) cacheDir.mkdirs();
						
						// 🌟 ফিক্সড: আপনার অ্যাপের নিজস্ব .tpad এক্সটেনশন 🌟
						File tpadFile = new File(cacheDir, fileName + ".tpad");
						
						// .tpad ফাইলের ভেতরে অরিজিনাল rawContent সেভ করা হলো (যাতে অ্যাপের ফরম্যাট ঠিক থাকে)
						java.io.FileOutputStream fos = new java.io.FileOutputStream(tpadFile);
						fos.write((rawContent != null ? rawContent : "").getBytes("UTF-8"));
						fos.close();
						
						android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(ProjectViewActivity.this, getPackageName() + ".provider", tpadFile);
						
						Intent shareIntent = new Intent(Intent.ACTION_SEND);
						shareIntent.setType("*/*"); // 🌟 যেকোনো অ্যাপে ডকুমেন্ট হিসেবে সাপোর্ট করানোর জন্য 🌟
						shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
						shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						startActivity(Intent.createChooser(shareIntent, "TunePad ফাইল হিসেবে শেয়ার করুন..."));
					} catch (Exception e) {
						Toast.makeText(ProjectViewActivity.this, "শেয়ার করতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show();
					}
				} else {
					// 🌟 লেখা ছোট হলে সরাসরি মেসেজ হিসেবে যাবে 🌟
					Intent shareIntent = new Intent(Intent.ACTION_SEND);
					shareIntent.setType("text/plain");
					shareIntent.putExtra(Intent.EXTRA_TEXT, cleanContent);
					startActivity(Intent.createChooser(shareIntent, "শেয়ার করুন..."));
				}
			}
		}));
		
		// 🌟 ৩. ডিলিট বাটন 🌟
		root.addView(createMenuItem("মুছে ফেলুন", android.R.drawable.ic_menu_delete, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (file != null && file.exists()) file.delete(); 
				
				android.database.sqlite.SQLiteDatabase db = openOrCreateDatabase("notes_db_v3", MODE_PRIVATE, null);
				String uniqueDbTitle = projectName + "_" + fileName;
				db.delete("notes", "title=? OR title=?", new String[]{uniqueDbTitle, fileName}); 
				db.close();
				
				projectDataPrefs.edit().remove("summary_" + fileName).apply();
				showCustomToastSheet("পর্বটি মুছে ফেলা হয়েছে!"); 
				
				new android.os.Handler().postDelayed(new Runnable() {
					@Override
					public void run() { loadChapters(); }
				}, 300);
				sheet.dismiss();
			}
		}));
		
		sheet.setContentView(root); 
		sheet.show();
	}
	
	
	
	
	
	
	private LinearLayout createMenuItem(String title, int iconRes, View.OnClickListener listener) {
		LinearLayout item = new LinearLayout(this);
		item.setOrientation(LinearLayout.HORIZONTAL);
		item.setPadding(64, 32, 64, 32);
		item.setGravity(Gravity.CENTER_VERTICAL);
		item.setOnClickListener(listener);
		
		ImageView icon = new ImageView(this);
		icon.setImageResource(iconRes);
		icon.setColorFilter(primaryTextColor);
		item.addView(icon, new LinearLayout.LayoutParams(60, 60));
		
		TextView text = new TextView(this);
		text.setText(title);
		text.setTextColor(primaryTextColor);
		text.setTextSize(16f);
		text.setTypeface(currentTypeface, Typeface.BOLD);
		text.setPadding(32, 0, 0, 0);
		item.addView(text);
		return item;
	}
	
	private void showEditDialog(String title, String currentText, final OnTextSavedListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title);
		final EditText input = new EditText(this);
		input.setText(currentText);
		input.setPadding(40, 40, 40, 40);
		input.setLines(4);
		input.setGravity(Gravity.TOP | Gravity.START);
		input.setBackgroundColor(surfaceColor);
		input.setTextColor(primaryTextColor);
		input.setTypeface(currentTypeface);
		builder.setView(input);
		builder.setPositiveButton("সেভ করুন", new DialogInterface.OnClickListener() {
			@Override public void onClick(DialogInterface dialog, int which) { listener.onSaved(input.getText().toString().trim()); }
		});
		builder.setNegativeButton("বাতিল", null);
		builder.show();
	}
	
	private interface OnTextSavedListener { void onSaved(String text); }
	
	
	
	// ==========================================
	// 🌟 কাস্টম ফিল্ড ও প্রজেক্ট ডিটেইলস লোড 🌟
	// ==========================================
	private void loadProjectDetails() {
		String desc = projectDataPrefs.getString("desc", "এই প্রজেক্টের কোনো ডেসক্রিপশন নেই...");
		String genre = projectDataPrefs.getString("genre", categoryName);
		
		// 🌟 ডেসক্রিপশন এবং 'আরো দেখুন' ম্যাজিক 🌟
		if(tvProjectDesc != null) {
			tvProjectDesc.setText(desc);
			
			TextView btnReadMoreDesc = findViewById(R.id.btnReadMoreDesc);
			if (btnReadMoreDesc != null) {
				setupReadMoreLogic(tvProjectDesc, btnReadMoreDesc, 3);
			}
		}
		
		if(tvProjectCategory != null) {
			tvProjectCategory.setText(genre);
		}
		
		// 🌟 কভার ছবি লোড (মেমরি লিক ফিক্সড) 🌟
		File coverFile = new File(projectDir, "cover.jpg");
		if (coverFile.exists()) {
			android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
			options.inSampleSize = 2; // ছবি কম্প্রেস করে লোড হবে
			android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(coverFile.getAbsolutePath(), options);
			imgCover.setImageBitmap(bitmap);
			imgCover.setColorFilter(null);
			imgCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
		} else {
			imgCover.setImageResource(android.R.drawable.ic_menu_gallery);
			imgCover.setColorFilter(Color.parseColor("#9CA8AE"));
			imgCover.setBackgroundColor(Color.parseColor("#2A3439"));
			imgCover.setScaleType(ImageView.ScaleType.CENTER);
		}
		
		// 🌟 কাস্টম ফিল্ড ডায়নামিকভাবে লোড করা 🌟
		if (customFieldsContainer != null) {
			customFieldsContainer.removeAllViews();
			int fieldCount = projectDataPrefs.getInt("custom_field_count", 0);
			
			for (int i = 0; i < fieldCount; i++) {
				final int currentIndex = i;
				final String key = projectDataPrefs.getString("custom_key_" + i, "");
				final String val = projectDataPrefs.getString("custom_val_" + i, "");
				
				if (!key.isEmpty() && !val.isEmpty()) {
					TextView tvField = new TextView(this);
					tvField.setText(key + " : " + val);
					tvField.setTextColor(accentColor);
					tvField.setTextSize(13f);
					tvField.setTypeface(currentTypeface);
					tvField.setPadding(0, 12, 0, 12);
					
					android.util.TypedValue outValue = new android.util.TypedValue();
					getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
					tvField.setBackgroundResource(outValue.resourceId);
					
					tvField.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							showCustomFieldOptionsDialog(currentIndex, key, val);
						}
					});
					customFieldsContainer.addView(tvField);
				}
			}
		}
		
		// 🌟 হেল্পার মেথড কল করে নাম ও উৎসর্গ রিফ্রেশ করা 🌟
		refreshAuthorAndDedication();
	}
	
	// ==========================================
	// 🌟 লেখক এবং উৎসর্গপত্র রিফ্রেশ ও 'আরো দেখুন' ম্যাজিক 🌟
	// ==========================================
	private void refreshAuthorAndDedication() {
		String authorName = projectDataPrefs.getString("author_name", "Shubhra Afroj Tunerosa");
		String dedicationText = projectDataPrefs.getString("dedication_text", "").trim();
		
		TextView tvAuthor = findViewById(R.id.tvAuthorName);
		if (tvAuthor != null) {
			tvAuthor.setText("লেখিকা: " + authorName);
			tvAuthor.setVisibility(View.VISIBLE); 
		}
		
		LinearLayout layoutDedication = findViewById(R.id.layoutDedication);
		TextView tvDedText = findViewById(R.id.tvDedicationText);
		TextView btnReadMoreDed = findViewById(R.id.btnReadMoreDedication); // 🌟 বাটনটি খোঁজা হচ্ছে
		
		if (layoutDedication != null && tvDedText != null) {
			if (dedicationText.isEmpty()) {
				layoutDedication.setVisibility(View.GONE);
			} else {
				layoutDedication.setVisibility(View.VISIBLE); 
				tvDedText.setText(dedicationText);
				tvDedText.setVisibility(View.VISIBLE);
				
				// 🌟 আসল ম্যাজিক: উৎসর্গপত্রের জন্য ৩ লাইনের ইঞ্জিন কল করা হলো 🌟
				if(btnReadMoreDed != null) {
					btnReadMoreDed.setTextColor(accentColor); // থিমের কালার দিলাম
					setupReadMoreLogic(tvDedText, btnReadMoreDed, 3);
				}
			}
		}
	}
	
	// ==========================================
	// 🌟 সিনোপসিস লোড 🌟
	// ==========================================
	private void loadSynopsisData() {
		String tagline = projectDataPrefs.getString("synopsis_tagline", "");
		String body = projectDataPrefs.getString("synopsis_body", ""); 
		
		refreshAuthorAndDedication();
		
		if (tagline.isEmpty() && body.isEmpty()) {
			if(layoutSynopsisEmpty != null) layoutSynopsisEmpty.setVisibility(View.VISIBLE);
			if(layoutSynopsisContent != null) layoutSynopsisContent.setVisibility(View.GONE);
		} else {
			if(layoutSynopsisEmpty != null) layoutSynopsisEmpty.setVisibility(View.GONE);
			if(layoutSynopsisContent != null) layoutSynopsisContent.setVisibility(View.VISIBLE);
			
			if(tvSynopsisTagline != null) {
				if(tagline.isEmpty()) tvSynopsisTagline.setVisibility(View.GONE);
				else {
					tvSynopsisTagline.setVisibility(View.VISIBLE);
					tvSynopsisTagline.setText(tagline);
				}
			}
			if(tvSynopsisBody != null) {
				tvSynopsisBody.setVisibility(View.VISIBLE);
				tvSynopsisBody.setText(body);
				
			}
		}
	}
	
	
	// ==========================================
	// 🌟 সিনোপসিস এডিট শিট 🌟
	// ==========================================
	private void showEditSynopsisSheet() {
		final com.google.android.material.bottomsheet.BottomSheetDialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
		sheet.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		
		LinearLayout rootLayout = new LinearLayout(this);
		rootLayout.setOrientation(LinearLayout.VERTICAL);
		rootLayout.setBackgroundColor(surfaceColor);
		rootLayout.setPadding(48, 64, 48, 48);
		rootLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		
		TextView titleView = new TextView(this);
		titleView.setText("সিনোপসিস এডিট করুন");
		titleView.setTextColor(primaryTextColor);
		titleView.setTextSize(20f);
		titleView.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		titleView.setPadding(0, 0, 0, 32);
		rootLayout.addView(titleView);
		
		android.graphics.drawable.GradientDrawable inputBg = new android.graphics.drawable.GradientDrawable();
		inputBg.setColor(bgColor);
		inputBg.setCornerRadius(24f);
		
		final EditText etTagline = new EditText(this);
		etTagline.setHint("ট্যাগলাইন বা লগলাইন (এক লাইনে)...");
		etTagline.setText(projectDataPrefs.getString("synopsis_tagline", ""));
		etTagline.setTextColor(accentColor);
		etTagline.setHintTextColor(secondaryTextColor);
		etTagline.setBackground(inputBg);
		etTagline.setPadding(40, 32, 40, 32);
		etTagline.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		etTagline.setSingleLine(true);
		LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		tagParams.setMargins(0, 0, 0, 24);
		etTagline.setLayoutParams(tagParams);
		rootLayout.addView(etTagline);
		
		androidx.core.widget.NestedScrollView scrollView = new androidx.core.widget.NestedScrollView(this);
		LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
		scrollParams.setMargins(0, 0, 0, 48);
		scrollView.setLayoutParams(scrollParams);
		scrollView.setFillViewport(true);
		scrollView.setVerticalScrollBarEnabled(true);
		
		final EditText etBody = new EditText(this);
		etBody.setHint("গল্পের বিস্তারিত সারাংশ এখানে লিখুন...");
		// 🌟 ফিক্স ২: "synopsis_body" ব্যবহার করা হলো 🌟
		etBody.setText(projectDataPrefs.getString("synopsis_body", ""));
		etBody.setTextColor(primaryTextColor);
		etBody.setHintTextColor(secondaryTextColor);
		etBody.setBackground(inputBg);
		etBody.setPadding(40, 40, 40, 40);
		etBody.setTypeface(currentTypeface);
		etBody.setGravity(Gravity.TOP);
		etBody.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		etBody.setLineSpacing(0, 1.4f);
		etBody.setMinLines(8);
		
		LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		etBody.setLayoutParams(bodyParams);
		
		scrollView.addView(etBody);
		rootLayout.addView(scrollView);
		
		TextView btnSave = new TextView(this);
		btnSave.setText("সেভ করুন");
		btnSave.setTextColor(surfaceColor);
		btnSave.setGravity(Gravity.CENTER);
		btnSave.setTextSize(16f);
		btnSave.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		btnSave.setPadding(0, 40, 0, 40);
		android.graphics.drawable.GradientDrawable saveBg = new android.graphics.drawable.GradientDrawable();
		saveBg.setColor(accentColor);
		saveBg.setCornerRadius(100f);
		btnSave.setBackground(saveBg);
		
		btnSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String tagline = etTagline.getText().toString().trim();
				String body = etBody.getText().toString().trim();
				// 🌟 ফিক্স ৩: "synopsis_body" হিসেবে ডাটা সেভ হবে 🌟
				projectDataPrefs.edit().putString("synopsis_tagline", tagline).putString("synopsis_body", body).apply();
				
				loadSynopsisData();
				sheet.dismiss();
				Toast.makeText(ProjectViewActivity.this, "সিনোপসিস আপডেট হয়েছে!", Toast.LENGTH_SHORT).show();
			}
		});
		rootLayout.addView(btnSave);
		sheet.setContentView(rootLayout);
		
		sheet.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
			@Override
			public void onShow(android.content.DialogInterface dialog) {
				com.google.android.material.bottomsheet.BottomSheetDialog d = (com.google.android.material.bottomsheet.BottomSheetDialog) dialog;
				View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
				if (bottomSheetInternal != null) {
					com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheetInternal).setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
					com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheetInternal).setSkipCollapsed(true);
					bottomSheetInternal.setLayoutParams(new androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
				}
			}
		});
		
		sheet.show();
	}
	
	private void showCustomFieldOptionsDialog(final int index, final String oldKey, final String oldVal) {
		String[] options = {"এডিট করুন", "ডিলিট করুন"};
		new AlertDialog.Builder(this)
		.setTitle("অপশন (" + oldKey + ")")
		.setItems(options, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == 0) {
					Toast.makeText(ProjectViewActivity.this, "এডিট অপশনটি শীঘ্রই আসছে!", Toast.LENGTH_SHORT).show();
				} else if (which == 1) {
					deleteCustomField(index);
				}
			}
		}).show();
	}
	
	private void deleteCustomField(int indexToDelete) {
		int fieldCount = projectDataPrefs.getInt("custom_field_count", 0);
		List<String[]> activeFields = new ArrayList<>();
		
		for (int i = 0; i < fieldCount; i++) {
			if (i != indexToDelete) {
				String k = projectDataPrefs.getString("custom_key_" + i, "");
				String v = projectDataPrefs.getString("custom_val_" + i, "");
				if(!k.isEmpty()) activeFields.add(new String[]{k, v});
			}
		}
		
		SharedPreferences.Editor editor = projectDataPrefs.edit();
		// 🌟 ফিক্সড: আগে সবগুলো ক্লিয়ার করতে হবে 🌟
		for (int i = 0; i < fieldCount; i++) {
			editor.remove("custom_key_" + i).remove("custom_val_" + i);
		}
		
		// তারপর নতুনগুলো বসাতে হবে
		for (int i = 0; i < activeFields.size(); i++) {
			editor.putString("custom_key_" + i, activeFields.get(i)[0]);
			editor.putString("custom_val_" + i, activeFields.get(i)[1]);
		}
		editor.putInt("custom_field_count", activeFields.size());
		editor.apply();
		
		loadProjectDetails(); 
		showCustomToastSheet("ফিল্ডটি মুছে ফেলা হয়েছে!"); // 🌟 আপনার সিগনেচার টোস্ট 🌟
	}
	
	
		// ==========================================
	// 🌟 নতুন পর্ব তৈরি করার লজিক (Premium Bottom Sheet) 🌟
	// ==========================================
	private void showCreateChapterDialog() {
		final BottomSheetDialog addSheet = new BottomSheetDialog(this);
		// কিবোর্ড ওপেন হলে শিট যেন উপরে উঠে যায় তার জন্য
		addSheet.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		
		LinearLayout rootLayout = new LinearLayout(this);
		rootLayout.setOrientation(LinearLayout.VERTICAL);
		rootLayout.setBackgroundColor(surfaceColor);
		rootLayout.setPadding(64, 64, 64, 64);
		
		TextView titleView = new TextView(this);
		titleView.setText("নতুন পর্ব তৈরি করুন");
		titleView.setTextColor(primaryTextColor);
		titleView.setTextSize(20f);
		titleView.setTypeface(currentTypeface, Typeface.BOLD);
		titleView.setPadding(0, 0, 0, 32);
		rootLayout.addView(titleView);
		
		GradientDrawable inputBg = new GradientDrawable();
		inputBg.setColor(bgColor);
		inputBg.setCornerRadius(24f);
		
		final EditText etName = new EditText(this);
		etName.setHint("পর্বের নাম দিন...");
		etName.setTextColor(primaryTextColor);
		etName.setHintTextColor(secondaryTextColor);
		etName.setBackground(inputBg);
		etName.setPadding(40, 32, 40, 32);
		etName.setTypeface(currentTypeface);
		etName.setSingleLine(true);
		etName.requestFocus();
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, 0, 48);
		etName.setLayoutParams(params);
		rootLayout.addView(etName);
		
		TextView btnCreate = new TextView(this);
		btnCreate.setText("তৈরি করুন");
		btnCreate.setTextColor(surfaceColor);
		btnCreate.setGravity(Gravity.CENTER);
		btnCreate.setTextSize(16f);
		btnCreate.setTypeface(currentTypeface, Typeface.BOLD);
		btnCreate.setPadding(0, 40, 0, 40);
		GradientDrawable btnBg = new GradientDrawable();
		btnBg.setColor(accentColor);
		btnBg.setCornerRadius(100f);
		btnCreate.setBackground(btnBg);
		
		btnCreate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String name = etName.getText().toString().trim();
				if (name.isEmpty()) return;
				
				String safeName = name.replaceAll("[^a-zA-Z0-9_\\-\\u0980-\\u09FF ]", "").trim();
				
				if (safeName.isEmpty()) {
					showCustomToastSheet("অনুগ্রহ করে পর্বের সঠিক নাম দিন!"); 
					return;
				}
				
				File newFile = new File(projectDir, safeName + ".tpad");
				
				if (!newFile.exists()) {
					try {
						java.io.FileOutputStream fos = new java.io.FileOutputStream(newFile);
						fos.write("".getBytes());
						fos.flush();
						fos.close(); 
						
						String noteId = "proj_" + System.currentTimeMillis();
						String timestamp = new java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
						String saveLabel = "Project: " + projectName;
						
						android.database.sqlite.SQLiteDatabase db = openOrCreateDatabase("notes_db_v3", MODE_PRIVATE, null);
						android.content.ContentValues cv = new android.content.ContentValues();
						cv.put("id", noteId);
						cv.put("title", projectName + "_" + safeName); 
						cv.put("content", "");
						cv.put("label", saveLabel);
						cv.put("timestamp", timestamp);
						cv.put("isPinned", 0);
						cv.put("isDeleted", 0);
						cv.put("isDraft", 0);
						cv.put("isHidden", 0);
						db.insertWithOnConflict("notes", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
						db.close();
						
						Toast.makeText(ProjectViewActivity.this, "নতুন পর্ব তৈরি হয়েছে!", Toast.LENGTH_SHORT).show();
						loadChapters();
						addSheet.dismiss(); // কাজ শেষ হলে শিট বন্ধ হয়ে যাবে
						openNoteInEditor(safeName); 
						
					} catch (Exception e) {
						Toast.makeText(ProjectViewActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
					}
				} else {
					Toast.makeText(ProjectViewActivity.this, "এই নামে পর্ব আগে থেকেই আছে!", Toast.LENGTH_SHORT).show();
				}
			}
		});
		rootLayout.addView(btnCreate);
		addSheet.setContentView(rootLayout);
		
		// 🌟 ম্যাজিক: শিট ওপেন হওয়ার সাথে সাথেই কিবোর্ড চলে আসবে 🌟
		etName.postDelayed(new Runnable() {
			@Override
			public void run() {
				android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(etName, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
			}
		}, 200);
        
         
		
		addSheet.show();
	}

	
	@Override
	protected void onResume() {
		super.onResume();
		loadChapters(); 
		// 🌟 নতুন: ডিটেইলস পেজ থেকে ব্যাক করলে চরিত্রের লিস্টও অটো রিফ্রেশ হবে 🌟
		if (projectCharacterAdapter != null) {
			loadProjectCharacters(); 
		}
	}
	
	
	// ==========================================
	// 🌟 চরিত্র ট্যাবের লজিক (গল্পের নাম দিয়ে ফিল্টার) 🌟
	// ==========================================
	private List<CharacterModel> projectCharacterList = new ArrayList<>();
	private ProjectCharacterAdapter projectCharacterAdapter;
	
	class CharacterModel { int id; String name, role, storyName, imageUri; }
	
	private void setupCharacterTab() {
		rvProjectCharacters.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
		projectCharacterAdapter = new ProjectCharacterAdapter();
		rvProjectCharacters.setAdapter(projectCharacterAdapter);
		loadProjectCharacters();
		
		// 🌟 এই প্রজেক্টের জন্য নতুন চরিত্র অ্যাড করার বাটন 🌟
		btnAddCharacterTab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showAddCharacterToProjectDialog();
			}
		});
	}
	
	private void loadProjectCharacters() {
		projectCharacterList.clear();
		
		// 🌟 প্রিভিউ মোডের ক্যারেক্টার ম্যাজিক 🌟
		if (isPreviewMode) {
			try {
				String charJsonStr = projectDataPrefs.getString("temp_characters_json", "[]");
				org.json.JSONArray charArray = new org.json.JSONArray(charJsonStr);
				for(int i=0; i<charArray.length(); i++){
					org.json.JSONObject obj = charArray.getJSONObject(i);
					CharacterModel cm = new CharacterModel();
					cm.id = i;
					cm.name = obj.optString("name", "অজানা চরিত্র");
					cm.role = obj.optString("role", "ভূমিকা নেই");
					cm.storyName = projectName;
					cm.imageUri = ""; 
					projectCharacterList.add(cm);
				}
			} catch(Exception e){}
		} else {
			SharedPreferences allCharPrefs = getSharedPreferences("Global_Characters_DB", MODE_PRIVATE);
			int count = allCharPrefs.getInt("char_count", 0);
			for (int i = 0; i < count; i++) {
				if (allCharPrefs.getBoolean("char_active_" + i, false)) {
					String story = allCharPrefs.getString("char_story_" + i, "");
					if (story.trim().equalsIgnoreCase(projectName.trim())) {
						CharacterModel cm = new CharacterModel();
						cm.id = i;
						cm.name = allCharPrefs.getString("char_name_" + i, "");
						cm.role = allCharPrefs.getString("char_role_" + i, "ভূমিকা নেই");
						cm.storyName = story;
						cm.imageUri = allCharPrefs.getString("char_img_" + i, "");
						projectCharacterList.add(cm);
					}
				}
			}
		}
		if (projectCharacterAdapter != null) projectCharacterAdapter.notifyDataSetChanged();
	}
	
	// ==========================================
	// 🌟 ফিক্সড: চরিত্র অ্যাড করার প্রিমিয়াম বটম শিট 🌟
	// ==========================================
	private void showAddCharacterToProjectDialog() {
		final BottomSheetDialog addSheet = new BottomSheetDialog(this);
		addSheet.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		
		LinearLayout rootLayout = new LinearLayout(this);
		rootLayout.setOrientation(LinearLayout.VERTICAL);
		rootLayout.setBackgroundColor(surfaceColor);
		rootLayout.setPadding(64, 64, 64, 64);
		
		TextView titleView = new TextView(this);
		titleView.setText("এই গল্পের জন্য নতুন চরিত্র");
		titleView.setTextColor(primaryTextColor);
		titleView.setTextSize(20f);
		titleView.setTypeface(currentTypeface, Typeface.BOLD);
		titleView.setPadding(0, 0, 0, 32);
		rootLayout.addView(titleView);
		
		GradientDrawable inputBg = new GradientDrawable();
		inputBg.setColor(bgColor);
		inputBg.setCornerRadius(24f);
		
		final EditText etName = new EditText(this);
		etName.setHint("চরিত্রের নাম দিন...");
		etName.setTextColor(primaryTextColor);
		etName.setHintTextColor(secondaryTextColor);
		etName.setBackground(inputBg);
		etName.setPadding(40, 32, 40, 32);
		etName.setTypeface(currentTypeface);
		etName.setSingleLine(true);
		etName.requestFocus();
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, 0, 48);
		etName.setLayoutParams(params);
		rootLayout.addView(etName);
		
		TextView btnCreate = new TextView(this);
		btnCreate.setText("তৈরি করুন");
		btnCreate.setTextColor(surfaceColor);
		btnCreate.setGravity(Gravity.CENTER);
		btnCreate.setTextSize(16f);
		btnCreate.setTypeface(currentTypeface, Typeface.BOLD);
		btnCreate.setPadding(0, 40, 0, 40);
		GradientDrawable btnBg = new GradientDrawable();
		btnBg.setColor(accentColor);
		btnBg.setCornerRadius(100f);
		btnCreate.setBackground(btnBg);
		
		btnCreate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String name = etName.getText().toString().trim();
				if (!name.isEmpty()) {
					SharedPreferences allCharPrefs = getSharedPreferences("Global_Characters_DB", MODE_PRIVATE);
					int newId = allCharPrefs.getInt("char_count", 0);
					
					allCharPrefs.edit()
					.putBoolean("char_active_" + newId, true)
					.putString("char_name_" + newId, name)
					.putString("char_story_" + newId, projectName) 
					.putInt("char_count", newId + 1).apply();
					
					Intent intent = new Intent(ProjectViewActivity.this, CharacterDetailsActivity.class);
					intent.putExtra("PROJECT_NAME", projectName);
					intent.putExtra("CHAR_ID", newId);
					startActivity(intent);
					addSheet.dismiss();
				} else {
					Toast.makeText(ProjectViewActivity.this, "নাম দিন!", Toast.LENGTH_SHORT).show();
				}
			}
		});
		rootLayout.addView(btnCreate);
		addSheet.setContentView(rootLayout);
		
		etName.postDelayed(new Runnable() {
			@Override
			public void run() {
				android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(etName, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
			}
		}, 200);
        
         
		
		addSheet.show();
	}
	// ==========================================
	// 🌟 চরিত্রের ডাইনামিক গ্রিড কার্ড (অ্যাভাটার সহ) 🌟
	// ==========================================
	private class ProjectCharacterAdapter extends RecyclerView.Adapter<ProjectCharacterAdapter.CharViewHolder> {
		
		@NonNull
		@Override
		public CharViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LinearLayout card = new LinearLayout(ProjectViewActivity.this);
			card.setOrientation(LinearLayout.VERTICAL);
			card.setGravity(Gravity.CENTER);
			RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			lp.setMargins(16, 16, 16, 16);
			card.setLayoutParams(lp);
			card.setPadding(24, 40, 24, 40);
			
			GradientDrawable shape = new GradientDrawable();
			shape.setCornerRadius(32f);
			shape.setColor(surfaceColor);
			card.setBackground(shape);
			
			androidx.cardview.widget.CardView imgCard = new androidx.cardview.widget.CardView(ProjectViewActivity.this);
			imgCard.setRadius(80f); 
			imgCard.setCardElevation(0f);
			imgCard.setLayoutParams(new LinearLayout.LayoutParams(160, 160));
			
			ImageView imgProfile = new ImageView(ProjectViewActivity.this);
			imgProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imgCard.addView(imgProfile);
			
			TextView tvInitials = new TextView(ProjectViewActivity.this);
			tvInitials.setGravity(Gravity.CENTER);
			tvInitials.setTextColor(Color.WHITE);
			tvInitials.setTextSize(32f);
			tvInitials.setTypeface(currentTypeface, Typeface.BOLD);
			tvInitials.setBackgroundColor(accentColor);
			imgCard.addView(tvInitials);
			
			card.addView(imgCard);
			
			TextView tvName = new TextView(ProjectViewActivity.this);
			tvName.setTextColor(primaryTextColor);
			tvName.setTextSize(16f);
			tvName.setTypeface(currentTypeface, Typeface.BOLD);
			tvName.setGravity(Gravity.CENTER);
			tvName.setPadding(0, 16, 0, 4);
			card.addView(tvName);
			
			TextView tvRole = new TextView(ProjectViewActivity.this);
			tvRole.setTextColor(accentColor);
			tvRole.setTextSize(12f);
			tvRole.setTypeface(currentTypeface, Typeface.BOLD);
			tvRole.setGravity(Gravity.CENTER);
			card.addView(tvRole);
			
			return new CharViewHolder(card, imgProfile, tvInitials, tvName, tvRole);
		}
		
		@Override
		public void onBindViewHolder(@NonNull CharViewHolder holder, final int position) {
			final CharacterModel cm = projectCharacterList.get(position);
			holder.tvName.setText(cm.name);
			holder.tvRole.setText(cm.role.isEmpty() ? "ভূমিকা নেই" : cm.role);
			
			if(!cm.imageUri.isEmpty()) {
				holder.imgProfile.setImageURI(android.net.Uri.parse(cm.imageUri));
				holder.imgProfile.setVisibility(View.VISIBLE);
				holder.tvInitials.setVisibility(View.GONE);
			} else {
				holder.imgProfile.setVisibility(View.GONE);
				holder.tvInitials.setVisibility(View.VISIBLE);
				String initials = "?";
				if(cm.name.length() > 0) initials = String.valueOf(cm.name.charAt(0));
				if(cm.name.contains(" ")) {
					String[] parts = cm.name.split(" ");
					if(parts.length > 1 && parts[1].length() > 0) initials = String.valueOf(cm.name.charAt(0)) + String.valueOf(parts[1].charAt(0));
				}
				holder.tvInitials.setText(initials.toUpperCase());
			}
			
			
			
			
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// 🌟 লিস্ট থেকে বর্তমান পজিশনের চরিত্রটি বের করা হলো 🌟
					final CharacterModel currentCharacter = projectCharacterList.get(position);
					
					// 🌟 প্রিভিউ মোড চেক: এডিটরের বদলে রিড-অনলি ক্যারেক্টার ডায়ালগ দেখাবে 🌟
					if (isPreviewMode) {
						showPreviewCharacterDialog(currentCharacter); // 🌟 ফিক্সড 🌟
					} else {
						// আপনার আগে থেকে থাকা নরমাল ক্লিক লিসেনারের কোডগুলো এখানে থাকবে
						// যেমন: showCharacterOptions(currentCharacter) বা এডিট করার কোড
						Intent intent = new Intent(ProjectViewActivity.this, CharacterDetailsActivity.class);
						intent.putExtra("PROJECT_NAME", projectName);
						intent.putExtra("CHAR_ID", cm.id);
						startActivity(intent);
					}
				}
			});
			
			
		}
		
		@Override
		public int getItemCount() { return projectCharacterList.size(); }
		
		class CharViewHolder extends RecyclerView.ViewHolder {
			ImageView imgProfile; TextView tvInitials, tvName, tvRole;
			public CharViewHolder(@NonNull View itemView, ImageView img, TextView init, TextView name, TextView role) {
				super(itemView); imgProfile = img; tvInitials = init; tvName = name; tvRole = role;
			}
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private void loadIdeas() {
		ideaList.clear();
		int count = projectDataPrefs.getInt("idea_count", 0);
		int total = 0, applied = 0;
		
		List<IdeaModel> tempList = new ArrayList<>();
		
		for (int i = 0; i < count; i++) {
			if (projectDataPrefs.getBoolean("idea_active_" + i, false)) {
				IdeaModel idea = new IdeaModel();
				idea.id = i;
				idea.text = projectDataPrefs.getString("idea_text_" + i, "");
				idea.isDone = projectDataPrefs.getBoolean("idea_done_" + i, false);
				idea.timestamp = projectDataPrefs.getString("idea_time_" + i, "সময় অজানা");
				
				total++;
				if (idea.isDone) applied++;
				
				// 🌟 সার্চ এবং ফিল্টার লজিক 🌟
				if (ideaSearchQuery.isEmpty() || idea.text.toLowerCase().contains(ideaSearchQuery.toLowerCase())) {
					if (ideaSortMode == 2 && !idea.isDone) continue; // শুধু সম্পন্ন দেখাবে
					if (ideaSortMode == 3 && idea.isDone) continue;  // শুধু বাকিগুলো দেখাবে
					
					tempList.add(idea);
				}
			}
		}
		int remaining = total - applied;
		
		if(tvIdeaStats != null) {
			tvIdeaStats.setText("মোট: " + total + " • এপ্লাই: " + applied + " • বাকি: " + remaining);
		}
		
		// 🌟 সর্টিং (Sorting) লজিক 🌟
		if (ideaSortMode == 1) {
			// পুরোনো আগে (যেভাবে অ্যাড হয়েছে)
			ideaList.addAll(tempList);
		} else {
			// নতুন আগে (ডিফল্ট)
			java.util.Collections.reverse(tempList);
			ideaList.addAll(tempList);
		}
		
		ideaAdapter.notifyDataSetChanged();
	}
	
	private void setupIdeaTab() {
		rvIdeas.setLayoutManager(new LinearLayoutManager(this));
		ideaAdapter = new IdeaAdapter();
		rvIdeas.setAdapter(ideaAdapter);
		loadIdeas();
		
		// 🌟 ১. রিয়েল-টাইম সার্চ বক্স ওপেন/ক্লোজ 🌟
		btnIdeaSearch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (searchContainerIdea.getVisibility() == View.VISIBLE) {
					searchContainerIdea.setVisibility(View.GONE);
					ideaSearchQuery = "";
					etIdeaSearch.setText("");
					loadIdeas();
					// কিবোর্ড হাইড করা
					android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(etIdeaSearch.getWindowToken(), 0);
				} else {
					searchContainerIdea.setVisibility(View.VISIBLE);
					etIdeaSearch.requestFocus();
					// কিবোর্ড শো করা
					android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
					imm.showSoftInput(etIdeaSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
				}
			}
		});
		
		// 🌟 অটোমেটিক সার্চ হতে থাকবে (Real-time Filtering) 🌟
		etIdeaSearch.addTextChangedListener(new android.text.TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
				ideaSearchQuery = s.toString().trim();
				loadIdeas();
			}
			@Override public void afterTextChanged(android.text.Editable s) {}
		});
		
		// 🌟 ২. সিগনেচার বটম শিট ফিল্টার 🌟
		btnIdeaFilter.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showIdeaFilterBottomSheet();
			}
		});
		
		// 🌟 ৩. নতুন আইডিয়া (ফুল স্ক্রিন বটম শিট) 🌟
		btnIdeaAdd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showAddIdeaFullScreenSheet();
			}
		});
	}
	
	// ==========================================
	// 🌟 প্রিমিয়াম ফিল্টার/সর্ট বটম শিট (No Alert Dialog) 🌟
	// ==========================================
	private void showIdeaFilterBottomSheet() {
		final BottomSheetDialog filterSheet = new BottomSheetDialog(this);
		LinearLayout rootLayout = new LinearLayout(this);
		rootLayout.setOrientation(LinearLayout.VERTICAL);
		rootLayout.setBackgroundColor(surfaceColor);
		rootLayout.setPadding(64, 64, 64, 64);
		
		TextView titleView = new TextView(this);
		titleView.setText("সাজান এবং ফিল্টার");
		titleView.setTextColor(primaryTextColor);
		titleView.setTextSize(20f);
		titleView.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		titleView.setPadding(0, 0, 0, 32);
		rootLayout.addView(titleView);
		
		String[] options = {"নতুন আগে (ডিফল্ট)", "পুরোনো আগে", "শুধু সম্পন্নগুলো", "শুধু বাকিগুলো"};
		for (int i = 0; i < options.length; i++) {
			TextView tvOption = new TextView(this);
			tvOption.setText(options[i]);
			// অ্যাক্টিভ অপশনটি কালারফুল থাকবে
			if (ideaSortMode == i) {
				tvOption.setTextColor(accentColor);
				tvOption.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
			} else {
				tvOption.setTextColor(primaryTextColor);
				tvOption.setTypeface(currentTypeface);
			}
			tvOption.setTextSize(16f);
			tvOption.setPadding(0, 32, 0, 32);
			
			final int index = i;
			tvOption.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ideaSortMode = index;
					loadIdeas();
					filterSheet.dismiss();
				}
			});
			rootLayout.addView(tvOption);
			
			if (i < options.length - 1) {
				View divider = new View(this);
				divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
				divider.setBackgroundColor(bgColor);
				rootLayout.addView(divider);
			}
		}
		filterSheet.setContentView(rootLayout);
         
		filterSheet.show();
	}
	
	// ==========================================
	// 🌟 ফুল-স্ক্রিন আইডিয়া এডিটর (অরিজিনাল মোমেন্টাম স্ক্রল সহ) 🌟
	// ==========================================
	private void showAddIdeaFullScreenSheet() {
		final com.google.android.material.bottomsheet.BottomSheetDialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
		sheet.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		
		LinearLayout rootLayout = new LinearLayout(this);
		rootLayout.setOrientation(LinearLayout.VERTICAL);
		rootLayout.setBackgroundColor(surfaceColor);
		rootLayout.setPadding(48, 64, 48, 48);
		rootLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		
		TextView titleView = new TextView(this);
		titleView.setText("নতুন আইডিয়া লিখুন");
		titleView.setTextColor(primaryTextColor);
		titleView.setTextSize(20f);
		titleView.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		titleView.setPadding(0, 0, 0, 32);
		rootLayout.addView(titleView);
		
		// 🌟 আসল ম্যাজিক: NestedScrollView (মোমেন্টাম স্ক্রলের জন্য) 🌟
		androidx.core.widget.NestedScrollView scrollView = new androidx.core.widget.NestedScrollView(this);
		LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
		scrollParams.setMargins(0, 0, 0, 48);
		scrollView.setLayoutParams(scrollParams);
		scrollView.setFillViewport(true);
		scrollView.setVerticalScrollBarEnabled(true);
		
		android.graphics.drawable.GradientDrawable inputBg = new android.graphics.drawable.GradientDrawable();
		inputBg.setColor(bgColor);
		inputBg.setCornerRadius(32f);
		
		final EditText etIdeaContent = new EditText(this);
		etIdeaContent.setHint("হঠাৎ মাথায় আসা প্লট বা আইডিয়া এখানে বিস্তারিত লিখুন...");
		etIdeaContent.setTextColor(primaryTextColor);
		etIdeaContent.setHintTextColor(secondaryTextColor);
		etIdeaContent.setBackground(inputBg);
		etIdeaContent.setPadding(40, 40, 40, 40);
		etIdeaContent.setTypeface(currentTypeface);
		etIdeaContent.setGravity(Gravity.TOP);
		etIdeaContent.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		// maxLines রিমুভ করে দিয়েছি! এখন লেখা বাড়লে বক্স নিজে বড় হবে এবং NestedScrollView মোমেন্টাম স্ক্রল করবে।
		etIdeaContent.setMinLines(8); 
		
		LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		etIdeaContent.setLayoutParams(etParams);
		
		// ❌ আগের টাচ ইন্টারসেপ্টর হ্যাক বাদ! ❌
		
		scrollView.addView(etIdeaContent);
		rootLayout.addView(scrollView);
		
		TextView btnSave = new TextView(this);
		btnSave.setText("আইডিয়া সেভ করুন");
		btnSave.setTextColor(surfaceColor);
		btnSave.setGravity(Gravity.CENTER);
		btnSave.setTextSize(16f);
		btnSave.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		btnSave.setPadding(0, 40, 0, 40);
		android.graphics.drawable.GradientDrawable saveBg = new android.graphics.drawable.GradientDrawable();
		saveBg.setColor(accentColor);
		saveBg.setCornerRadius(100f);
		btnSave.setBackground(saveBg);
		
		btnSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String text = etIdeaContent.getText().toString().trim();
				if (!text.isEmpty()) {
					int count = projectDataPrefs.getInt("idea_count", 0);
					String currentTime = new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
					projectDataPrefs.edit().putBoolean("idea_active_" + count, true).putString("idea_text_" + count, text).putBoolean("idea_done_" + count, false).putString("idea_time_" + count, currentTime).putInt("idea_count", count + 1).apply();
					loadIdeas();
					sheet.dismiss();
				} else {
					Toast.makeText(ProjectViewActivity.this, "কিছু তো লিখুন!", Toast.LENGTH_SHORT).show();
				}
			}
		});
		rootLayout.addView(btnSave);
		sheet.setContentView(rootLayout);
		
		sheet.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
			@Override
			public void onShow(android.content.DialogInterface dialog) {
				com.google.android.material.bottomsheet.BottomSheetDialog d = (com.google.android.material.bottomsheet.BottomSheetDialog) dialog;
				View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
				if (bottomSheetInternal != null) {
					com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheetInternal).setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
					com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheetInternal).setSkipCollapsed(true);
					bottomSheetInternal.setLayoutParams(new androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
				}
			}
		});
		
		etIdeaContent.postDelayed(new Runnable() {
			@Override
			public void run() {
				android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(etIdeaContent, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
			}
		}, 200);
		
		sheet.show();
	}
	
	private java.util.Set<Integer> expandedIdeaIds = new java.util.HashSet<>();
	
	private class IdeaAdapter extends RecyclerView.Adapter<IdeaAdapter.IdeaViewHolder> {
		@NonNull
		@Override
		public IdeaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LinearLayout itemLayout = new LinearLayout(ProjectViewActivity.this);
			itemLayout.setOrientation(LinearLayout.HORIZONTAL);
			itemLayout.setGravity(Gravity.TOP); 
			RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			lp.setMargins(24, 8, 24, 16); 
			itemLayout.setLayoutParams(lp);
			itemLayout.setPadding(32, 32, 32, 32);
			
			GradientDrawable bg = new GradientDrawable();
			bg.setColor(surfaceColor);
			bg.setCornerRadius(32f); 
			itemLayout.setBackground(bg);
			
			ImageView imgCheck = new ImageView(ProjectViewActivity.this);
			LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(56, 56);
			checkParams.topMargin = 4; 
			imgCheck.setLayoutParams(checkParams);
			imgCheck.setPadding(8, 8, 8, 8);
			
			LinearLayout textContainer = new LinearLayout(ProjectViewActivity.this);
			textContainer.setOrientation(LinearLayout.VERTICAL);
			LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
			textParams.setMargins(24, 0, 24, 0);
			textContainer.setLayoutParams(textParams);
			
			final TextView tvText = new TextView(ProjectViewActivity.this);
			tvText.setTextColor(primaryTextColor);
			tvText.setTextSize(15f); 
			tvText.setTypeface(currentTypeface);
			tvText.setLineSpacing(0, 1.3f);
			tvText.setMaxLines(3); 
			tvText.setEllipsize(android.text.TextUtils.TruncateAt.END);
			
			TextView tvTime = new TextView(ProjectViewActivity.this);
			tvTime.setTextColor(secondaryTextColor);
			tvTime.setTextSize(11f); 
			tvTime.setTypeface(currentTypeface);
			tvTime.setPadding(0, 16, 0, 0); 
			
			textContainer.addView(tvText);
			textContainer.addView(tvTime);
			
			ImageView imgDelete = new ImageView(ProjectViewActivity.this);
			imgDelete.setImageResource(android.R.drawable.ic_menu_delete);
			imgDelete.setColorFilter(secondaryTextColor); 
			LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(50, 50);
			deleteParams.topMargin = 8;
			imgDelete.setLayoutParams(deleteParams);
			
			itemLayout.addView(imgCheck);
			itemLayout.addView(textContainer);
			itemLayout.addView(imgDelete); 
			
			return new IdeaViewHolder(itemLayout, imgCheck, tvText, tvTime, imgDelete);
		}
		
		@Override
		public void onBindViewHolder(@NonNull final IdeaViewHolder holder, final int position) {
			final IdeaModel idea = ideaList.get(position);
			holder.tvText.setText(idea.text);
			holder.tvTime.setText(idea.timestamp);
			
			GradientDrawable bg = (GradientDrawable) holder.card.getBackground();
			
			if (idea.isDone) {
				holder.imgCheck.setImageResource(android.R.drawable.checkbox_on_background);
				holder.imgCheck.setColorFilter(accentColor);
				holder.tvText.setTextColor(secondaryTextColor);
				holder.tvTime.setText("সম্পন্ন • " + idea.timestamp);
				holder.tvTime.setTextColor(accentColor);
				bg.setColor(Color.argb(40, Color.red(secondaryTextColor), Color.green(secondaryTextColor), Color.blue(secondaryTextColor)));
			} else {
				holder.imgCheck.setImageResource(android.R.drawable.checkbox_off_background);
				holder.imgCheck.setColorFilter(secondaryTextColor);
				holder.tvText.setTextColor(primaryTextColor);
				holder.tvTime.setText(idea.timestamp);
				holder.tvTime.setTextColor(secondaryTextColor);
				bg.setColor(surfaceColor);
			}
			
			final boolean isExpanded = expandedIdeaIds.contains(idea.id);
			if (isExpanded) {
				holder.tvText.setMaxLines(Integer.MAX_VALUE); 
			} else {
				holder.tvText.setMaxLines(3); 
			}
			
			holder.tvText.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isExpanded) expandedIdeaIds.remove(idea.id);
					else expandedIdeaIds.add(idea.id);
					notifyItemChanged(position); 
				}
			});
			
			holder.imgCheck.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean newStatus = !idea.isDone;
					projectDataPrefs.edit().putBoolean("idea_done_" + idea.id, newStatus).apply();
					loadIdeas();
				}
			});
			
			holder.imgDelete.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final BottomSheetDialog deleteSheet = new BottomSheetDialog(ProjectViewActivity.this);
					
					LinearLayout sheetRoot = new LinearLayout(ProjectViewActivity.this);
					sheetRoot.setOrientation(LinearLayout.VERTICAL);
					sheetRoot.setBackgroundColor(surfaceColor);
					sheetRoot.setPadding(64, 64, 64, 64);
					
					TextView tvDelTitle = new TextView(ProjectViewActivity.this);
					tvDelTitle.setText("মুছে ফেলবেন?");
					tvDelTitle.setTextColor(primaryTextColor);
					tvDelTitle.setTextSize(20f);
					tvDelTitle.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
					tvDelTitle.setPadding(0, 0, 0, 16);
					sheetRoot.addView(tvDelTitle);
					
					TextView tvDelMsg = new TextView(ProjectViewActivity.this);
					tvDelMsg.setText("এই আইডিয়াটি কি সত্যি ডিলিট করতে চান?");
					tvDelMsg.setTextColor(secondaryTextColor);
					tvDelMsg.setTextSize(14f);
					tvDelMsg.setTypeface(currentTypeface);
					tvDelMsg.setLineSpacing(0, 1.2f);
					tvDelMsg.setPadding(0, 0, 0, 48);
					sheetRoot.addView(tvDelMsg);
					
					LinearLayout btnRow = new LinearLayout(ProjectViewActivity.this);
					btnRow.setOrientation(LinearLayout.HORIZONTAL);
					
					TextView btnCancel = new TextView(ProjectViewActivity.this);
					btnCancel.setText("না, থাক");
					btnCancel.setTextColor(primaryTextColor);
					btnCancel.setGravity(Gravity.CENTER);
					btnCancel.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
					GradientDrawable cancelBg = new GradientDrawable();
					cancelBg.setColor(bgColor);
					cancelBg.setCornerRadius(50f);
					btnCancel.setBackground(cancelBg);
					btnCancel.setPadding(0, 32, 0, 32);
					LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
					cancelParams.setMarginEnd(16);
					btnCancel.setLayoutParams(cancelParams);
					btnCancel.setOnClickListener(new View.OnClickListener() {
						@Override public void onClick(View v) { deleteSheet.dismiss(); }
					});
					
					TextView btnConfirm = new TextView(ProjectViewActivity.this);
					btnConfirm.setText("ডিলিট করুন");
					btnConfirm.setTextColor(Color.WHITE);
					btnConfirm.setGravity(Gravity.CENTER);
					btnConfirm.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
					GradientDrawable confirmBg = new GradientDrawable();
					confirmBg.setColor(Color.parseColor("#E53935")); 
					confirmBg.setCornerRadius(50f);
					btnConfirm.setBackground(confirmBg);
					btnConfirm.setPadding(0, 32, 0, 32);
					LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
					confirmParams.setMarginStart(16);
					btnConfirm.setLayoutParams(confirmParams);
					btnConfirm.setOnClickListener(new View.OnClickListener() {
						@Override public void onClick(View v) {
							projectDataPrefs.edit().putBoolean("idea_active_" + idea.id, false).apply();
							loadIdeas();
							deleteSheet.dismiss();
						}
					});
					
					btnRow.addView(btnCancel);
					btnRow.addView(btnConfirm);
					sheetRoot.addView(btnRow);
                     
					
					deleteSheet.setContentView(sheetRoot);
					deleteSheet.show();
				}
			});
		}
		
		@Override
		public int getItemCount() { return ideaList.size(); }
		
		class IdeaViewHolder extends RecyclerView.ViewHolder {
			LinearLayout card; 
			ImageView imgCheck, imgDelete; 
			TextView tvText, tvTime;
			
			public IdeaViewHolder(@NonNull View itemView, ImageView c, TextView t, TextView time, ImageView d) {
				super(itemView); 
				card = (LinearLayout) itemView;
				imgCheck = c; 
				tvText = t; 
				tvTime = time; 
				imgDelete = d;
			}
		}
	}
	
	
	
	// 🌟 আইডিয়া সার্চ ডায়ালগ 🌟
	private void showIdeaSearchDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("আইডিয়া খুঁজুন");
		
		final EditText input = new EditText(this);
		input.setHint("সার্চ কিওয়ার্ড লিখুন...");
		input.setText(ideaSearchQuery); // আগে সার্চ করা থাকলে সেটা দেখাবে
		input.setTextColor(primaryTextColor);
		input.setPadding(50, 40, 50, 40);
		builder.setView(input);
		
		builder.setPositiveButton("খুঁজুন", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ideaSearchQuery = input.getText().toString().trim();
				loadIdeas();
			}
		});
		builder.setNegativeButton("বাতিল", null);
		builder.setNeutralButton("ক্লিয়ার", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ideaSearchQuery = "";
				loadIdeas();
			}
		});
		builder.show();
	}
	
	
	
	// ==========================================
	// 🌟 সিনোপসিস ট্যাবের মূল লজিক 🌟
	// ==========================================
	private void setupSynopsisTab() {
		loadSynopsisData();
		
		View.OnClickListener editListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showEditSynopsisSheet();
			}
		};
		
		btnAddSynopsisEmpty.setOnClickListener(editListener);
		btnEditSynopsis.setOnClickListener(editListener);
	}
	
	
	
	
	
	
	
	
	// ==========================================
	// 🌟 গোল ট্যাবের স্মার্ট ড্যাশবোর্ড ও প্রেডিক্টর 🌟
	// ==========================================
	private void setupGoalTab() {
		loadGoalData();
		
		View.OnClickListener editListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showEditGoalSheet();
			}
		};
		
		btnSetNewGoal.setOnClickListener(editListener);
		btnEditGoal.setOnClickListener(editListener);
	}
	
	private void loadGoalData() {
		int targetWords = projectDataPrefs.getInt("goal_target_words", 0);
		long deadlineMillis = projectDataPrefs.getLong("goal_deadline_millis", 0);
		
		// 🌟 যদি গোল সেট করা না থাকে (এম্পটি স্টেট) 🌟
		if (targetWords == 0) {
			layoutGoalEmpty.setVisibility(View.VISIBLE);
			layoutGoalDashboard.setVisibility(View.GONE);
			return;
		}
		
		// 🌟 গোল সেট করা থাকলে (ড্যাশবোর্ড স্টেট) 🌟
		layoutGoalEmpty.setVisibility(View.GONE);
		layoutGoalDashboard.setVisibility(View.VISIBLE);
		
		// 💡 আপনার আগের সেভ করা লজিক থেকে ডাটাগুলো টেনে আনা হচ্ছে:
		// (আপনার শেয়ার্ড প্রেফারেন্সে যে নামে সেভ করা আছে, প্রয়োজনে নামগুলো মিলিয়ে নেবেন)
		int currentWords = projectDataPrefs.getInt("total_words", 0); // আপনার অটোমেটিক গোনা শব্দ
		int currentChapters = chapterFiles.size(); // সরাসরি লিস্টের সাইজটাই হলো বর্তমান পর্বসংখ্যা!
		// আপনার অটোমেটিক গোনা পর্ব
		int currentStreak = projectDataPrefs.getInt("goal_current_streak", 0); // ফায়ার স্ট্রিক
		
		// 🌟 ১. প্রোগ্রেস বার লজিক 🌟
		int progress = 0;
		if (targetWords > 0) {
			progress = (int) (((float) currentWords / targetWords) * 100);
		}
		if (progress > 100) progress = 100;
		
		pbGoalProgress.setProgress(progress);
		tvProgressTitle.setText("মোট অগ্রগতি (" + progress + "%)");
		
		// 🌟 ২. স্ট্যাটাস কার্ড আপডেট 🌟
		tvWordsCount.setText(currentWords + " / " + targetWords);
		tvChaptersCount.setText(String.valueOf(currentChapters)); // এখানে টার্গেট পর্বের দরকার নেই, শুধু মোট পর্ব দেখালেই জোস লাগবে
		checkAndUpdateStreakLogic(currentWords);
		tvStreakCount.setText(currentStreak + " দিন");
		
		// 🌟 ৩. স্মার্ট প্রেডিক্টর লজিক (The Brain) 🧠 🌟
		long currentMillis = System.currentTimeMillis();
		long diffMillis = deadlineMillis - currentMillis;
		int daysLeft = (int) (diffMillis / (1000 * 60 * 60 * 24));
		int wordsLeft = targetWords - currentWords;
		
		if (wordsLeft <= 0) {
			tvPredictorMsg.setText("অভিনন্দন! 🎉 আপনি আপনার লক্ষ্য পূরণ করেছেন!");
			tvPredictorMsg.setTextColor(accentColor);
			tvPredictorLabel.setText("🧠 লক্ষ্য অর্জিত");
		} else if (daysLeft < 0) {
			tvPredictorMsg.setText("ডেডলাইন পার হয়ে গেছে! 😢 এখনই লেখা শেষ করুন।");
			tvPredictorMsg.setTextColor(android.graphics.Color.RED);
			tvPredictorLabel.setText("⚠️ ওভারডিউ");
		} else if (daysLeft == 0) {
			tvPredictorMsg.setText("আজকেই ডেডলাইন! আর মাত্র " + wordsLeft + " শব্দ বাকি।");
			tvPredictorMsg.setTextColor(android.graphics.Color.parseColor("#FF9800")); // Orange warning
			tvPredictorLabel.setText("🔥 শেষ দিন");
		} else {
			int reqPerDay = wordsLeft / daysLeft;
			tvPredictorMsg.setText("আর " + daysLeft + " দিন বাকি। লক্ষ্য ছুঁতে আজ " + reqPerDay + " শব্দ লিখতে হবে।");
			tvPredictorMsg.setTextColor(primaryTextColor);
			tvPredictorLabel.setText("🧠 আজকের লক্ষ্য");
		}
		
		// 🌟 ৪. গ্যামিফিকেশন / ব্যাজ লজিক 🏆 🌟
		// ব্যাজ ১: শুরু হলো (১টি শব্দ লিখলেই আনলক)
		if (currentWords > 0) badge1.setAlpha(1.0f);
		else badge1.setAlpha(0.4f);
		
		// ব্যাজ ২: 10K ওয়ার্ডস (১০ হাজার শব্দ লিখলে আনলক)
		if (currentWords >= 10000) badge2.setAlpha(1.0f);
		else badge2.setAlpha(0.4f);
		
		// ব্যাজ ৩: টানা ৭ দিন (স্ট্রিক ৭ দিন হলে আনলক)
		if (currentStreak >= 7) badge3.setAlpha(1.0f);
		else badge3.setAlpha(0.4f);
	}
	
	// ==========================================
	// 🌟 লক্ষ্য নির্ধারণের স্মার্ট বটম শিট 🌟
	// ==========================================
	private void showEditGoalSheet() {
		final com.google.android.material.bottomsheet.BottomSheetDialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
		sheet.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		
		LinearLayout rootLayout = new LinearLayout(this);
		rootLayout.setOrientation(LinearLayout.VERTICAL);
		rootLayout.setBackgroundColor(surfaceColor);
		rootLayout.setPadding(48, 64, 48, 48);
		
		TextView titleView = new TextView(this);
		titleView.setText("আপনার লক্ষ্য সেট করুন 🎯");
		titleView.setTextColor(primaryTextColor);
		titleView.setTextSize(20f);
		titleView.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		titleView.setPadding(0, 0, 0, 32);
		rootLayout.addView(titleView);
		
		android.graphics.drawable.GradientDrawable inputBg = new android.graphics.drawable.GradientDrawable();
		inputBg.setColor(bgColor);
		inputBg.setCornerRadius(24f);
		
		// 🌟 শুধু ১টি ইনপুট: টার্গেট শব্দসংখ্যা 🌟
		final EditText etTargetWords = new EditText(this);
		etTargetWords.setHint("টার্গেট শব্দসংখ্যা (যেমন: 50000)");
		int savedTarget = projectDataPrefs.getInt("goal_target_words", 0);
		if (savedTarget > 0) {
			etTargetWords.setText(String.valueOf(savedTarget));
		}
		etTargetWords.setTextColor(primaryTextColor);
		etTargetWords.setHintTextColor(secondaryTextColor);
		etTargetWords.setBackground(inputBg);
		etTargetWords.setPadding(40, 40, 40, 40);
		etTargetWords.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
		etTargetWords.setTypeface(currentTypeface);
		LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		p1.setMargins(0, 0, 0, 32);
		etTargetWords.setLayoutParams(p1);
		rootLayout.addView(etTargetWords);
		
		// 🌟 শুধু ডেডলাইন বা শেষ করার তারিখ (Date Picker) 🌟
		final TextView tvTargetDate = new TextView(this);
		// ডিফল্টভাবে আজকের থেকে ৩০ দিন পরের ডেট সিলেক্ট করা থাকবে
		final long[] selectedDeadline = {projectDataPrefs.getLong("goal_deadline_millis", System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000))}; 
		tvTargetDate.setText("ডেডলাইন: " + new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(new java.util.Date(selectedDeadline[0])));
		tvTargetDate.setTextColor(accentColor);
		tvTargetDate.setBackground(inputBg);
		tvTargetDate.setPadding(40, 40, 40, 40);
		tvTargetDate.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		tvTargetDate.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		dateParams.setMargins(0, 0, 0, 48);
		tvTargetDate.setLayoutParams(dateParams);
		
		tvTargetDate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final java.util.Calendar c = java.util.Calendar.getInstance();
				c.setTimeInMillis(selectedDeadline[0]);
				android.app.DatePickerDialog dpd = new android.app.DatePickerDialog(ProjectViewActivity.this, new android.app.DatePickerDialog.OnDateSetListener() {
					@Override
					public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
						java.util.Calendar newDate = java.util.Calendar.getInstance();
						newDate.set(year, month, dayOfMonth);
						selectedDeadline[0] = newDate.getTimeInMillis();
						tvTargetDate.setText("ডেডলাইন: " + new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(newDate.getTime()));
					}
				}, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH));
				dpd.show();
			}
		});
		rootLayout.addView(tvTargetDate);
		
		// সেভ বাটন
		TextView btnSave = new TextView(this);
		btnSave.setText("সেভ করুন");
		btnSave.setTextColor(surfaceColor);
		btnSave.setGravity(Gravity.CENTER);
		btnSave.setTextSize(16f);
		btnSave.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		btnSave.setPadding(0, 40, 0, 40);
		android.graphics.drawable.GradientDrawable saveBg = new android.graphics.drawable.GradientDrawable();
		saveBg.setColor(accentColor);
		saveBg.setCornerRadius(100f);
		btnSave.setBackground(saveBg);
		
		btnSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					String targetText = etTargetWords.getText().toString().trim();
					if(targetText.isEmpty()) {
						Toast.makeText(ProjectViewActivity.this, "টার্গেট শব্দসংখ্যা দিন!", Toast.LENGTH_SHORT).show();
						return;
					}
					int tWords = Integer.parseInt(targetText);
					
					// শুধু টার্গেট এবং ডেডলাইন সেভ হবে
					projectDataPrefs.edit()
					.putInt("goal_target_words", tWords)
					.putLong("goal_deadline_millis", selectedDeadline[0])
					.apply();
					
					loadGoalData();
					sheet.dismiss();
					Toast.makeText(ProjectViewActivity.this, "লক্ষ্য সেট করা হয়েছে! 🚀", Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Toast.makeText(ProjectViewActivity.this, "সঠিক সংখ্যা দিন!", Toast.LENGTH_SHORT).show();
				}
			}
		});
		rootLayout.addView(btnSave);
		sheet.setContentView(rootLayout);
		sheet.show();
	}
	
	
	// ==========================================
	// 🌟 অটোমেটিক স্ট্রিক এবং ওয়ার্নিং লজিক (Final Version) 🌟
	// ==========================================
	private void checkAndUpdateStreakLogic(int currentTotalWords) {
		int lastKnownWords = projectDataPrefs.getInt("goal_last_known_words", 0);
		long lastWriteMillis = projectDataPrefs.getLong("goal_last_write_time_millis", 0);
		int currentStreak = projectDataPrefs.getInt("goal_current_streak", 0);
		
		// আজকের রাত ১২টার সময় (Midnight) বের করা
		java.util.Calendar currentCal = java.util.Calendar.getInstance();
		currentCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
		currentCal.set(java.util.Calendar.MINUTE, 0);
		currentCal.set(java.util.Calendar.SECOND, 0);
		currentCal.set(java.util.Calendar.MILLISECOND, 0);
		long todayMidnight = currentCal.getTimeInMillis();
		
		// সর্বশেষ লেখার রাত ১২টার সময়
		long lastMidnight = 0;
		if (lastWriteMillis > 0) {
			java.util.Calendar lastWriteCal = java.util.Calendar.getInstance();
			lastWriteCal.setTimeInMillis(lastWriteMillis);
			lastWriteCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
			lastWriteCal.set(java.util.Calendar.MINUTE, 0);
			lastWriteCal.set(java.util.Calendar.SECOND, 0);
			lastWriteCal.set(java.util.Calendar.MILLISECOND, 0);
			lastMidnight = lastWriteCal.getTimeInMillis();
		} else {
			lastMidnight = todayMidnight; // প্রথমবার ওপেন করলে আজকের দিনটাই ধরবে
		}
		
		// সর্বশেষ লেখার পর কত দিন পার হয়েছে?
		int daysDiff = (int) ((todayMidnight - lastMidnight) / (1000 * 60 * 60 * 24));
		boolean dataChanged = false;
		
		// 🌟 স্পেশাল ফিক্স: যদি ইউজারের প্রজেক্টে আগে থেকেই লেখা থাকে, কিন্তু স্ট্রিক ০ হয় 🌟
		if (currentStreak == 0 && currentTotalWords > 0 && lastKnownWords == 0) {
			currentStreak = 1;
			lastWriteMillis = System.currentTimeMillis();
			lastKnownWords = currentTotalWords;
			dataChanged = true;
		}
		// 🌟 শর্ত ১: ইউজার নতুন কিছু লিখেছে (শব্দসংখ্যা বেড়েছে) 🌟
		else if (currentTotalWords > lastKnownWords) {
			
			if (lastWriteMillis == 0 || daysDiff > 3) {
				// অনেকদিন পর লিখলে বা প্রথমবার লিখলে আবার ১ থেকে শুরু
				currentStreak = 1; 
			} 
			else if (daysDiff >= 1 && daysDiff <= 3) {
				// ১, ২ বা ৩ দিন পর ফিরে এসে লিখলো! স্ট্রিক ১ দিন বাড়বে।
				currentStreak++; 
			}
			else if (daysDiff == 0) {
				// 💡 আজকেই আবার লিখেছে। তাই স্ট্রিক বাড়বে না, যা ছিল তাই থাকবে। 
				// আমরা শুধু ইউজারের নতুন শব্দসংখ্যাটা নিচে সেভ করে রাখব।
			}
			
			lastWriteMillis = System.currentTimeMillis(); // লেখার সময় আপডেট
			lastKnownWords = currentTotalWords; // শব্দ আপডেট
			dataChanged = true;
		} 
		// 🌟 শর্ত ২: ইউজার কোনো লেখা ডিলিট করেছে (শব্দ কমে গেছে) 🌟
		else if (currentTotalWords < lastKnownWords) {
			lastKnownWords = currentTotalWords; // শুধু গোনা শব্দটা আপডেট করে রাখব
			dataChanged = true;
		}
		// 🌟 শর্ত ৩: ইউজার নতুন কিচ্ছু লেখেনি 🌟
		else {
			if (daysDiff > 3 && currentStreak > 0) {
				// ৩ দিন পার হয়ে গেছে, কিচ্ছু লেখেনি। স্ট্রিক জিরো!
				currentStreak = 0;
				dataChanged = true;
				Toast.makeText(this, "ইশ! আপনার স্ট্রিক ভেঙে গেছে। নতুন করে শুরু করুন!", Toast.LENGTH_LONG).show();
			}
		}
		
		// ব্যাকগ্রাউন্ডে ডাটা সেভ করা
		if (dataChanged) {
			projectDataPrefs.edit()
			.putInt("goal_current_streak", currentStreak)
			.putLong("goal_last_write_time_millis", lastWriteMillis)
			.putInt("goal_last_known_words", lastKnownWords)
			.apply();
		}
		
		// ==========================================
		// 🌟 ড্যাশবোর্ডে স্ট্রিক কার্ড আপডেট ও ওয়ার্নিং 🌟
		// ==========================================
		if (currentStreak == 0) {
			tvStreakCount.setText("০ দিন");
			tvStreakLabel.setText("স্ট্রিক শুরু করুন");
			tvStreakLabel.setTextColor(secondaryTextColor);
		} else {
			tvStreakCount.setText(currentStreak + " দিন");
			
			// ওয়ার্নিং মেসেজগুলো (Grace Period)
			if (daysDiff == 1 && currentTotalWords <= lastKnownWords) {
				tvStreakLabel.setText("🔥 স্ট্রিক বাঁচাতে আজ লিখুন!");
				tvStreakLabel.setTextColor(android.graphics.Color.parseColor("#FF9800")); // Orange
			} else if (daysDiff == 2 && currentTotalWords <= lastKnownWords) {
				tvStreakLabel.setText("⚠️ ২ দিন ধরে লেখেননি!");
				tvStreakLabel.setTextColor(android.graphics.Color.parseColor("#FF5722")); // Deep Orange
			} else if (daysDiff == 3 && currentTotalWords <= lastKnownWords) {
				tvStreakLabel.setText("🚨 শেষ সুযোগ! আজ জিরো হবে");
				tvStreakLabel.setTextColor(android.graphics.Color.RED);
			} else {
				tvStreakLabel.setText("রাইটিং স্ট্রিক");
				tvStreakLabel.setTextColor(secondaryTextColor);
			}
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
		warningIcon.setColorFilter(Color.parseColor("#FF9800")); // কমলা রঙের ওয়ার্নিং
		rootLayout.addView(warningIcon, new LinearLayout.LayoutParams(120, 120));
		
		// 🌟 মেসেজ টেক্সট 🌟
		TextView tvMsg = new TextView(this);
		tvMsg.setText(message);
		tvMsg.setTextColor(primaryTextColor);
		tvMsg.setTextSize(16f);
		tvMsg.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		tvMsg.setGravity(android.view.Gravity.CENTER);
		tvMsg.setPadding(0, 40, 0, 64);
		rootLayout.addView(tvMsg);
		
		// 🌟 ওকে বাটন 🌟
		TextView btnOk = new TextView(this);
		btnOk.setText("বুঝতে পেরেছি");
		btnOk.setTextColor(surfaceColor);
		btnOk.setGravity(android.view.Gravity.CENTER);
		btnOk.setTextSize(15f);
		btnOk.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		btnOk.setPadding(0, 32, 0, 32);
		
		android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
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
	// 🌟 ম্যাজিক ২: ড্র্যাগ অ্যান্ড ড্রপ সিরিয়াল সেভ করা 🌟
	// ==========================================
	private void saveChapterOrder() {
		StringBuilder orderBuilder = new StringBuilder();
		for (int i = 0; i < chapterFiles.size(); i++) {
			orderBuilder.append(chapterFiles.get(i).getName());
			if (i < chapterFiles.size() - 1) {
				orderBuilder.append(";;"); // সেপারেটর হিসেবে ';;' ব্যবহার
			}
		}
		projectDataPrefs.edit().putString("chapter_custom_order", orderBuilder.toString()).apply();
	}
	// ==========================================
	// 🌟 ProjectViewActivity থেকে ডাউনলোড করার ম্যাজিক মেথড 🌟
	// ==========================================
	private void exportProjectAsPDF() {
		Toast.makeText(this, "ফাইল প্রস্তুত করা হচ্ছে...", Toast.LENGTH_SHORT).show();
		
		// 🌟 ডাটাবেস থেকে লেখিকার নাম এবং উৎসর্গপত্র পড়া 🌟
		final String authorNameForExport = projectDataPrefs.getString("author_name", "Shubhra Afroj Tunerosa");
		final String dedicationForExport = projectDataPrefs.getString("dedication_text", "যাদের অনুপ্রেরণায় এই বইটি আজ আলোর মুখ দেখলো...\n\nআমার বাবা-মা এবং সেইসব পাঠকদের, যারা সব সময় আমার পাশে থেকেছেন। আপনাদের ভালোবাসা ছাড়া এই পথচলা অসম্ভব ছিল।");
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				final List<String[]> chaptersToExport = new ArrayList<>();
				
				// chapterFiles আগে থেকেই কাস্টম সিরিয়ালে সাজানো আছে
				for (File file : chapterFiles) {
					String chapterName = file.getName().replace(".tpad", "");
					String content = getNoteContentFromDB(chapterName);
					if (!content.trim().isEmpty()) {
						chaptersToExport.add(new String[]{chapterName, content});
					}
				}
				
				// মেইন থ্রেডে এসে গ্লোবাল হেল্পারকে কল করা
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// 🌟 ম্যাজিক: এবার authorName এবং dedication পাঠানো হচ্ছে 🌟
						ExportHelper.exportToPdf(ProjectViewActivity.this, projectName, authorNameForExport, dedicationForExport, chaptersToExport, currentTypeface, projectDir);
					}
				});
			}
		}).start();
	}
	
	private void exportProjectAsDoc() {
		Toast.makeText(this, "ফাইল প্রস্তুত করা হচ্ছে...", Toast.LENGTH_SHORT).show();
		
		// 🌟 ডাটাবেস থেকে লেখিকার নাম এবং উৎসর্গপত্র পড়া 🌟
		final String authorNameForExport = projectDataPrefs.getString("author_name", "Shubhra Afroj Tunerosa");
		final String dedicationForExport = projectDataPrefs.getString("dedication_text", "যাদের অনুপ্রেরণায় এই বইটি আজ আলোর মুখ দেখলো...\n\nআমার বাবা-মা এবং সেইসব পাঠকদের, যারা সব সময় আমার পাশে থেকেছেন। আপনাদের ভালোবাসা ছাড়া এই পথচলা অসম্ভব ছিল।");
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				final List<String[]> chaptersToExport = new ArrayList<>();
				
				for (File file : chapterFiles) {
					String chapterName = file.getName().replace(".tpad", "");
					String content = getNoteContentFromDB(chapterName);
					if (!content.trim().isEmpty()) {
						chaptersToExport.add(new String[]{chapterName, content});
					}
				}
				
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// 🌟 ম্যাজিক: এবার authorName এবং dedication পাঠানো হচ্ছে 🌟
						ExportHelper.exportToRealDocx(ProjectViewActivity.this, projectName, authorNameForExport, dedicationForExport, chaptersToExport, projectDir);
					}
				});
			}
		}).start();
	}// ==========================================
	// 🌟 ৩ লাইনের স্মার্ট ম্যাজিক ইঞ্জিন (Hidden Tab Bug Fixed) 🌟
	// ==========================================
	private void setupReadMoreLogic(final TextView tvContent, final TextView btnReadMore, final int maxLines) {
		tvContent.setMaxLines(Integer.MAX_VALUE);
		
		tvContent.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				// 🌟 আসল ম্যাজিক: লেআউট তৈরি হয়ে গেলেই কাজ করবে, ট্যাব হাইড থাকুক বা না থাকুক! 🌟
				if (tvContent.getLayout() != null && tvContent.getLineCount() > 0) {
					tvContent.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					
					if (tvContent.getLineCount() > maxLines) {
						btnReadMore.setVisibility(View.VISIBLE);
						tvContent.setMaxLines(maxLines);
						tvContent.setEllipsize(android.text.TextUtils.TruncateAt.END);
						btnReadMore.setText("আরো দেখুন");
						
						View.OnClickListener toggleListener = new View.OnClickListener() {
							boolean isExpanded = false;
							@Override
							public void onClick(View v) {
								if (isExpanded) {
									tvContent.setMaxLines(maxLines);
									btnReadMore.setText("আরো দেখুন");
								} else {
									tvContent.setMaxLines(Integer.MAX_VALUE);
									btnReadMore.setText("সংক্ষিপ্ত করুন");
								}
								isExpanded = !isExpanded;
							}
						};
						
						btnReadMore.setOnClickListener(toggleListener);
						tvContent.setOnClickListener(toggleListener); 
					} else {
						btnReadMore.setVisibility(View.GONE);
						tvContent.setOnClickListener(null); 
					}
				}
			}
		});
		
		// 🌟 লং-ক্লিক করে কপি করার লজিক 🌟
		tvContent.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
				android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", tvContent.getText().toString());
				clipboard.setPrimaryClip(clip);
				Toast.makeText(ProjectViewActivity.this, "লেখাটি কপি হয়েছে!", Toast.LENGTH_SHORT).show();
				return true;
			}
		});
	}
	
	
	// ==========================================
	// 🌟 প্রজেক্ট শেয়ার করার বটম শিট (PDF / Word) 🌟
	// ==========================================
	private void showShareProjectSheet() {
		final BottomSheetDialog shareSheet = new BottomSheetDialog(this);
		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setBackgroundColor(surfaceColor);
		root.setPadding(0, 32, 0, 32);
		
		root.addView(createMenuItem("PDF হিসেবে শেয়ার করুন 📄", android.R.drawable.ic_menu_share, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				shareSheet.dismiss();
				exportAndShareProject("pdf");
			}
		}));
		
		root.addView(createMenuItem("Word হিসেবে শেয়ার করুন 📝", android.R.drawable.ic_menu_share, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				shareSheet.dismiss();
				exportAndShareProject("doc");
			}
		}));
        
         
		
		shareSheet.setContentView(root);
		shareSheet.show();
	}
	// ==========================================
	// 🌟 ফাইল এক্সপোর্ট এবং সিকিউর অটো-শেয়ার (FileProvider) 🌟
	// ==========================================
	private void exportAndShareProject(final String type) {
		Toast.makeText(this, "ফাইল প্রস্তুত হচ্ছে, দয়া করে ৩ সেকেন্ড অপেক্ষা করুন...", Toast.LENGTH_SHORT).show();
		
		final String authorNameForExport = projectDataPrefs.getString("author_name", "Shubhra Afroj Tunerosa");
		final String dedicationForExport = projectDataPrefs.getString("dedication_text", "");
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				final List<String[]> chaptersToExport = new ArrayList<>();
				for (File file : chapterFiles) {
					String chapterName = file.getName().replace(".tpad", "");
					String content = getNoteContentFromDB(chapterName);
					if (!content.trim().isEmpty()) {
						chaptersToExport.add(new String[]{chapterName, content});
					}
				}
				
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						final File downloadsRoot = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
						final File appDownloadDir = new File(downloadsRoot, "TunePad");
						
						// এক্সপোর্ট কল
						if (type.equals("pdf")) {
							ExportHelper.exportToPdf(ProjectViewActivity.this, projectName, authorNameForExport, dedicationForExport, chaptersToExport, currentTypeface, projectDir);
						} else {
							ExportHelper.exportToRealDocx(ProjectViewActivity.this, projectName, authorNameForExport, dedicationForExport, chaptersToExport, projectDir);
						}
						
						// 🌟 ফাইল রাইট হওয়ার জন্য ৩.৫ সেকেন্ড অপেক্ষা 🌟
						new android.os.Handler().postDelayed(new Runnable() {
							@Override
							public void run() {
								File shareFile = null;
								if (type.equals("pdf")) {
									shareFile = new File(appDownloadDir, projectName + " - PDF.pdf");
								} else {
									shareFile = new File(appDownloadDir, projectName + " - TunePad.docx");
								}
								
								if (shareFile != null && shareFile.exists()) {
									Intent shareIntent = new Intent(Intent.ACTION_SEND);
									
									// 🌟 ফিক্সড: Word ফাইলের সঠিক MimeType 🌟
									shareIntent.setType(type.equals("pdf") ? "application/pdf" : "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
									
									// 🌟 আসল ম্যাজিক: FileProvider দিয়ে সিকিউর URI তৈরি করা 🌟
									android.net.Uri fileUri;
									try {
										fileUri = androidx.core.content.FileProvider.getUriForFile(ProjectViewActivity.this, getPackageName() + ".provider", shareFile);
									} catch (Exception e) {
										// FileProvider সেটআপ না থাকলে সাধারণ লিংক পাঠাবে
										fileUri = android.net.Uri.fromFile(shareFile);
									}
									
									shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
									shareIntent.putExtra(Intent.EXTRA_SUBJECT, projectName);
									shareIntent.putExtra(Intent.EXTRA_TEXT, projectName + " প্রজেক্টের ফাইল শেয়ার করা হলো।");
									
									// 🌟 হোয়াটসঅ্যাপকে ফাইল পড়ার পারমিশন দেওয়া হলো 🌟
									shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); 
									
									startActivity(Intent.createChooser(shareIntent, "শেয়ার করুন..."));
								} else {
									Toast.makeText(ProjectViewActivity.this, "ফাইল শেয়ার করতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show();
								}
							}
						}, 3500); 
					}
				});
			}
		}).start();
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
	/*	// ==========================================
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
}*/	
	
	
	
	
		// ==========================================
	// 🌟 ডেডিকেটেড ফোল্ডারে প্রজেক্ট আপলোড ও শেয়ার ইঞ্জিন 🌟
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
					saveSharedPreferencesToFile(metaDataFile); 
					
					TBoxUtils.zipAndEncryptFolder(projDir, tboxFile);
					
					if (metaDataFile.exists()) metaDataFile.delete();
					
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
					
					request.write((twoHyphens + boundary + crlf).getBytes("UTF-8"));
					request.write(("Content-Disposition: form-data; name=\"username\"" + crlf).getBytes("UTF-8"));
					request.write((crlf).getBytes("UTF-8"));
					request.write((userNameEn + crlf).getBytes("UTF-8"));
					
					request.write((twoHyphens + boundary + crlf).getBytes("UTF-8"));
					request.write(("Content-Disposition: form-data; name=\"project_file\";filename=\"" + tboxFile.getName() + "\"" + crlf).getBytes("UTF-8"));
					request.write((crlf).getBytes("UTF-8"));
					
					java.io.FileInputStream fileInputStream = new java.io.FileInputStream(tboxFile);
					int bytesRead, bytesAvailable, bufferSize;
					byte[] buffer;
					int maxBufferSize = 1 * 1024 * 1024;
					
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
										String beautifulUrl = java.net.URLDecoder.decode(fileUrl, "UTF-8");
										android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
										intent.setType("text/plain");
										intent.putExtra(android.content.Intent.EXTRA_SUBJECT, projTitle);
										intent.putExtra(android.content.Intent.EXTRA_TEXT, "আমার লেখা '" + projTitle + "' প্রজেক্টটি ডাউনলোড করুন:\n\n" + beautifulUrl);
										startActivity(android.content.Intent.createChooser(intent, "লিংক শেয়ার করুন..."));
										showCustomToastSheet("✅ আপলোড সফল এবং লিংক তৈরি হয়েছে!");
									} catch (Exception e) {}
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
	// 🌟 প্রিভিউ মোডের টপ-বার UI (Invisible Icon Bug Fixed) 🌟
	// ==========================================
	private void setupPreviewUI() {
		if (!isPreviewMode) return;
		
		btnProjectOptions.setVisibility(View.GONE);
		android.widget.ImageView btnShare = findViewById(R.id.btnShare);
		if (btnShare != null) btnShare.setVisibility(View.GONE);
		
		// 🌟 ফিক্সড ১: আইকনের জন্য নির্দিষ্ট সাইজ (48dp) সেট করা হলো 🌟
		int iconSize = (int) (48 * getResources().getDisplayMetrics().density);
		int padding = (int) (12 * getResources().getDisplayMetrics().density);
		
		// 🌟 টিক (Save) বাটন 🌟
		btnSavePreview = new android.widget.ImageView(this);
		btnSavePreview.setImageResource(android.R.drawable.ic_menu_save); 
		// 🌟 ফিক্সড ২: SRC_IN মোড দেওয়া হলো যাতে থিমের কালারটা ১০০% বসে 🌟
		btnSavePreview.setColorFilter(primaryTextColor, android.graphics.PorterDuff.Mode.SRC_IN); 
		btnSavePreview.setPadding(padding, padding, padding, padding);
		btnSavePreview.setScaleType(ImageView.ScaleType.FIT_CENTER); // আইকন যেন কেটে না যায়
		
		// 🌟 ক্রস (Discard) বাটন 🌟
		btnDiscardPreview = new android.widget.ImageView(this);
		btnDiscardPreview.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); 
		btnDiscardPreview.setColorFilter(primaryTextColor, android.graphics.PorterDuff.Mode.SRC_IN); 
		btnDiscardPreview.setPadding(padding, padding, padding, padding);
		btnDiscardPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
		
		// 🌟 ফিক্সড ৩: MATCH_PARENT এর বদলে নির্দিষ্ট সাইজ (iconSize) 🌟
		android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(iconSize, iconSize);
		params.gravity = Gravity.CENTER_VERTICAL;
		params.rightMargin = padding / 2;
		
		toolbarLayout.addView(btnDiscardPreview, params); 
		toolbarLayout.addView(btnSavePreview, params);    
		
		btnSavePreview.setOnClickListener(new android.view.View.OnClickListener() { @Override public void onClick(android.view.View v) { checkOverwriteAndSave(); } });
		btnDiscardPreview.setOnClickListener(new android.view.View.OnClickListener() { @Override public void onClick(android.view.View v) { discardPreview(); } });
		
		if (btnAddChapter != null) btnAddChapter.setVisibility(View.GONE);
		if (btnAddCharacterTab != null) btnAddCharacterTab.setVisibility(View.GONE);
		if (btnIdeaAdd != null) btnIdeaAdd.setVisibility(View.GONE);
		
		if (btnAddSynopsisEmpty != null) btnAddSynopsisEmpty.setVisibility(View.GONE);
		if (btnEditSynopsis != null) btnEditSynopsis.setVisibility(View.GONE);
		if (btnSetNewGoal != null) btnSetNewGoal.setVisibility(View.GONE);
		if (btnEditGoal != null) btnEditGoal.setVisibility(View.GONE);
	}

	
	
	
	
	
	// ==========================================
	// 🌟 হেল্পার ১: ছোট টেক্সট ভিউ তৈরি 🌟
	// ==========================================
	private android.widget.TextView createSmallLabel(String text, int color, boolean isBold) {
		android.widget.TextView tv = new android.widget.TextView(this);
		tv.setText(text);
		tv.setTextColor(color);
		tv.setTextSize(isBold ? 14f : 12f);
		tv.setTypeface(currentTypeface, isBold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
		tv.setPadding(0, 4, 0, 4);
		return tv;
	}
	
	
	
	// ==========================================
	// 🌟 ফোল্ডার ডিলিট হেল্পার 🌟
	// ==========================================
	private void deleteRecursiveFolder(File fileOrDirectory) {
		if (fileOrDirectory != null && fileOrDirectory.isDirectory()) {
			File[] children = fileOrDirectory.listFiles();
			if (children != null) {
				for (File child : children) {
					deleteRecursiveFolder(child);
				}
			}
		}
		if (fileOrDirectory != null) {
			fileOrDirectory.delete();
		}
	}
	
	// ==========================================
	// 🌟 ১. ফুল স্ক্রিন চ্যাপ্টার রিডার (মোমেন্টাম স্ক্রল সহ) 🌟
	// ==========================================
	private void showPreviewChapterDialog(final String chapterTitle) {
		// ফুল স্ক্রিন ডায়ালগ তৈরি
		final android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
		
		android.widget.LinearLayout root = new android.widget.LinearLayout(this);
		root.setOrientation(android.widget.LinearLayout.VERTICAL);
		root.setBackgroundColor(bgColor); // থিমের ব্যাকগ্রাউন্ড
		
		// 🌟 কাস্টম টুলবার (উপরে) 🌟
		android.widget.LinearLayout header = new android.widget.LinearLayout(this);
		header.setOrientation(android.widget.LinearLayout.HORIZONTAL);
		header.setPadding(32, 48, 32, 48);
		header.setGravity(android.view.Gravity.CENTER_VERTICAL);
		header.setBackgroundColor(surfaceColor);
		header.setElevation(8f);
		
		android.widget.ImageView btnBack = new android.widget.ImageView(this);
		btnBack.setImageResource(android.R.drawable.ic_menu_revert); // ব্যাক আইকন
		btnBack.setColorFilter(accentColor);
		btnBack.setPadding(16, 16, 16, 16);
		btnBack.setOnClickListener(new android.view.View.OnClickListener() { @Override public void onClick(android.view.View v) { dialog.dismiss(); } });
		
		android.widget.TextView titleTv = new android.widget.TextView(this);
		titleTv.setText(chapterTitle);
		titleTv.setTextColor(primaryTextColor);
		titleTv.setTextSize(20f);
		titleTv.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		titleTv.setPadding(32, 0, 0, 0);
		
		header.addView(btnBack);
		header.addView(titleTv);
		root.addView(header);
		
		// 🌟 মোমেন্টাম স্ক্রলিংয়ের জন্য NestedScrollView 🌟
		androidx.core.widget.NestedScrollView scroller = new androidx.core.widget.NestedScrollView(this);
		scroller.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
		scroller.setFillViewport(true); // পুরো স্ক্রিন জুড়ে থাকবে
		
		android.widget.LinearLayout contentLayout = new android.widget.LinearLayout(this);
		contentLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
		contentLayout.setPadding(64, 64, 64, 120);
		
		android.widget.TextView contentTv = new android.widget.TextView(this);
		contentTv.setText(getNoteContentFromDB(chapterTitle)); 
		contentTv.setTextColor(primaryTextColor);
		contentTv.setTextSize(17f); // পড়ার সুবিধার জন্য একটু বড়
		contentTv.setLineSpacing(0, 1.5f); // লাইনের মাঝে গ্যাপ
		contentTv.setTypeface(currentTypeface);
		
		contentLayout.addView(contentTv);
		scroller.addView(contentLayout);
		root.addView(scroller);
		
		dialog.setContentView(root);
		dialog.show();
	}
	
	// ==========================================
	// 🌟 ২. ফুল স্ক্রিন ক্যারেক্টার ডিটেইলস (সম্পর্ক ও সব তথ্য সহ) 🌟
	// ==========================================
	private void showPreviewCharacterDialog(final CharacterModel cm) {
		final android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
		
		android.widget.LinearLayout root = new android.widget.LinearLayout(this);
		root.setOrientation(android.widget.LinearLayout.VERTICAL);
		root.setBackgroundColor(bgColor);
		
		// হেডার
		android.widget.LinearLayout header = new android.widget.LinearLayout(this);
		header.setOrientation(android.widget.LinearLayout.HORIZONTAL);
		header.setPadding(32, 48, 32, 48);
		header.setGravity(android.view.Gravity.CENTER_VERTICAL);
		header.setBackgroundColor(surfaceColor);
		header.setElevation(8f);
		
		android.widget.ImageView btnBack = new android.widget.ImageView(this);
		btnBack.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); // ক্রস আইকন
		btnBack.setColorFilter(accentColor);
		btnBack.setPadding(16, 16, 16, 16);
		btnBack.setOnClickListener(new android.view.View.OnClickListener() { @Override public void onClick(android.view.View v) { dialog.dismiss(); } });
		
		android.widget.TextView titleTv = new android.widget.TextView(this);
		titleTv.setText("👤 " + cm.name);
		titleTv.setTextColor(primaryTextColor);
		titleTv.setTextSize(20f);
		titleTv.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		titleTv.setPadding(32, 0, 0, 0);
		
		header.addView(btnBack);
		header.addView(titleTv);
		root.addView(header);
		
		// 🌟 মোমেন্টাম স্ক্রলিংয়ের জন্য NestedScrollView 🌟
		androidx.core.widget.NestedScrollView scroller = new androidx.core.widget.NestedScrollView(this);
		scroller.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
		
		android.widget.LinearLayout contentLayout = new android.widget.LinearLayout(this);
		contentLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
		contentLayout.setPadding(64, 64, 64, 120);
		
		// 🌟 JSON থেকে চরিত্রের সমস্ত গোপন ও সাধারণ তথ্য বের করা 🌟
		org.json.JSONObject charObj = null;
		try {
			String charJsonStr = projectDataPrefs.getString("temp_characters_json", "[]");
			org.json.JSONArray charArray = new org.json.JSONArray(charJsonStr);
			for(int i=0; i<charArray.length(); i++){
				org.json.JSONObject obj = charArray.getJSONObject(i);
				if (obj.optString("name", "").equals(cm.name)) {
					charObj = obj;
					break;
				}
			}
		} catch(Exception e){}
		
		if (charObj != null) {
			// সিরিয়ালি সমস্ত ডেটা লিস্টে যোগ করা হচ্ছে (যেগুলো ফাঁকা সেগুলো নিজে থেকেই হাইড হয়ে যাবে)
			addDetailView(contentLayout, "ভূমিকা (Role)", charObj.optString("role", ""));
			addDetailView(contentLayout, "সম্পর্ক (Relationships)", charObj.optString("rels", ""));
			addDetailView(contentLayout, "ডাকনাম (Nickname)", charObj.optString("nickname", ""));
			addDetailView(contentLayout, "বয়স (Age)", charObj.optString("age", ""));
			addDetailView(contentLayout, "জন্মতারিখ (DOB)", charObj.optString("dob", ""));
			addDetailView(contentLayout, "পেশা (Occupation)", charObj.optString("occupation", ""));
			addDetailView(contentLayout, "দেশ (Country)", charObj.optString("country", ""));
			addDetailView(contentLayout, "লোকেশন (Location)", charObj.optString("location", ""));
			addDetailView(contentLayout, "উচ্চতা (Height)", charObj.optString("height", ""));
			addDetailView(contentLayout, "গড়ন (Build)", charObj.optString("build", ""));
			addDetailView(contentLayout, "চোখ ও চুল (Eye & Hair)", charObj.optString("eye_hair", ""));
			addDetailView(contentLayout, "পোশাক (Clothing)", charObj.optString("clothing", ""));
			addDetailView(contentLayout, "ব্যক্তিত্ব (Personality)", charObj.optString("personality", ""));
			addDetailView(contentLayout, "শক্তি (Strengths)", charObj.optString("strengths", ""));
			addDetailView(contentLayout, "দুর্বলতা (Flaws)", charObj.optString("flaws", ""));
			addDetailView(contentLayout, "অভ্যাস (Habits)", charObj.optString("habits", ""));
			addDetailView(contentLayout, "লক্ষ্য (Goal)", charObj.optString("goal", ""));
			addDetailView(contentLayout, "ভয় (Fear)", charObj.optString("fear", ""));
			addDetailView(contentLayout, "গোপন তথ্য (Secrets)", charObj.optString("secrets", ""));
			addDetailView(contentLayout, "দ্বন্দ্ব (Conflict)", charObj.optString("conflict", ""));
			addDetailView(contentLayout, "অতীত (Backstory)", charObj.optString("backstory", ""));
			
			// কাস্টম ফিল্ডগুলো যোগ করা
			int customCount = charObj.optInt("custom_count", 0);
			for (int j = 0; j < customCount; j++) {
				String key = charObj.optString("custom_key_" + j, "");
				String val = charObj.optString("custom_val_" + j, "");
				addDetailView(contentLayout, key, val);
			}
		} else {
			addDetailView(contentLayout, "ভূমিকা (Role)", cm.role);
			addDetailView(contentLayout, "তথ্য", "এই চরিত্রের বিস্তারিত কোনো তথ্য পাওয়া যায়নি।");
		}
		
		scroller.addView(contentLayout);
		root.addView(scroller);
		
		dialog.setContentView(root);
		dialog.show();
	}
	
	// ==========================================
	// 🌟 ৩. ক্যারেক্টার ডিটেইলস বানানোর হেল্পার মেথড 🌟
	// ==========================================
	private void addDetailView(android.widget.LinearLayout parent, String title, String value) {
		// যদি ওই ফিল্ডে কোনো ডেটা না থাকে, তবে সেটা লিস্টে দেখাবে না
		if (value == null || value.trim().isEmpty()) return;
		
		android.widget.TextView tvTitle = new android.widget.TextView(this);
		tvTitle.setText(title);
		tvTitle.setTextColor(accentColor);
		tvTitle.setTextSize(14f);
		tvTitle.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
		tvTitle.setPadding(0, 32, 0, 8);
		
		android.widget.TextView tvValue = new android.widget.TextView(this);
		tvValue.setText(value);
		tvValue.setTextColor(primaryTextColor);
		tvValue.setTextSize(16f);
		tvValue.setLineSpacing(0, 1.3f);
		tvValue.setTypeface(currentTypeface);
		tvValue.setPadding(0, 0, 0, 24);
		
		parent.addView(tvTitle);
		parent.addView(tvValue);
	}
	
	
	
	// ==========================================
	// 🌟 JSON থেকে আইডিয়া, সিনোপসিস ও চরিত্র রিস্টোর করা 🌟
	// ==========================================
	private void restoreMetaData(File metaFile, String targetProjName) {
		try {
			// ১. ফাইল থেকে JSON পড়া
			java.io.FileInputStream fis = new java.io.FileInputStream(metaFile);
			byte[] data = new byte[(int) metaFile.length()];
			fis.read(data); fis.close();
			org.json.JSONObject json = new org.json.JSONObject(new String(data, "UTF-8"));
			
			// ২. প্রজেক্ট সেটিংস ও সিনোপসিস রিস্টোর
			SharedPreferences projPrefs = getSharedPreferences("ProjectData_" + targetProjName, MODE_PRIVATE);
			SharedPreferences.Editor editor = projPrefs.edit();
			java.util.Iterator<String> keys = json.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				if (!key.equals("project_characters")) {
					Object val = json.get(key);
					if (val instanceof String) editor.putString(key, (String) val);
					else if (val instanceof Integer) editor.putInt(key, (Integer) val);
					else if (val instanceof Boolean) editor.putBoolean(key, (Boolean) val);
				}
			}
			editor.apply();
			
			// ৩. চরিত্র (Characters) রিস্টোর লজিক
			if (json.has("project_characters")) {
				org.json.JSONArray charArray = json.getJSONArray("project_characters");
				SharedPreferences charDB = getSharedPreferences("Global_Characters_DB", MODE_PRIVATE);
				int globalCount = charDB.getInt("char_count", 0);
				
				for (int i = 0; i < charArray.length(); i++) {
					org.json.JSONObject charObj = charArray.getJSONObject(i);
					int newCharId = globalCount++; // নতুন আইডি জেনারেট
					
					SharedPreferences.Editor charEditor = charDB.edit();
					java.util.Iterator<String> charKeys = charObj.keys();
					while (charKeys.hasNext()) {
						String cKey = charKeys.next();
						if (cKey.equals("rels")) continue; // রিলেশনশিপ নতুন আইডিতে মিলবে না, তাই বাদ
						Object cVal = charObj.get(cKey);
						if (cVal instanceof String) charEditor.putString(cKey + "_" + newCharId, (String) cVal);
						else if (cVal instanceof Integer) charEditor.putInt(cKey + "_" + newCharId, (Integer) cVal);
					}
					charEditor.putString("char_story_" + newCharId, targetProjName); // নতুন প্রজেক্টের সাথে কানেক্ট
					charEditor.putBoolean("char_active_" + newCharId, true);
					charEditor.putInt("char_count", globalCount);
					charEditor.apply();
				}
			}
		} catch (Exception e) { e.printStackTrace(); }
	}
	
    
    
    
    
    
    //---------------------------------------------------------------------------------------
    // ==========================================
    // 🌟 সেভ বাটনের প্রাথমিক চেক এবং মেইন পপআপ 🌟 
    // ==========================================
    private void checkOverwriteAndSave() {
        File mainProjectDir = new File(getFilesDir(), "TunePad_Data/Projects/" + categoryName + "/" + projectName);
        showSaveOptionsPopup(mainProjectDir, mainProjectDir.exists());
    }

    private void showSaveOptionsPopup(final File mainProjectDir, final boolean isExists) {
        final com.google.android.material.bottomsheet.BottomSheetDialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(surfaceColor);
        root.setPadding(64, 64, 64, 64);

        TextView title = new TextView(this);
        title.setText("প্রজেক্ট সেভ অপশন");
        title.setTextColor(primaryTextColor);
        title.setTextSize(20f);
        title.setTypeface(currentTypeface, Typeface.BOLD);
        title.setPadding(0, 0, 0, 48);
        root.addView(title);

        // 🌟 ওভাররাইট / সরাসরি সেভ বাটন 🌟
        TextView btnOverwrite = createActionBtn(isExists ? "ওভাররাইট করুন" : "সরাসরি সেভ করুন", isExists ? Color.parseColor("#E53935") : accentColor);
        btnOverwrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheet.dismiss();
                if (isExists) deleteRecursiveFolder(mainProjectDir); // আগেরটা ডিলিট
                saveImportedProject(mainProjectDir, projectName, categoryName); // সেভ
            }
        });

        // 🌟 ডুপ্লিকেট বাটন 🌟
        TextView btnDuplicate = createActionBtn("ডুপ্লিকেট করুন", Color.parseColor("#FF9800"));
        btnDuplicate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheet.dismiss();
                showDuplicateDetailsDialog(mainProjectDir);
            }
        });

        root.addView(btnOverwrite);
        root.addView(btnDuplicate);
        sheet.setContentView(root);
        sheet.show();
    }

    // ========================================== 
    // 🌟 ডুপ্লিকেট প্রজেক্টের নাম ও ক্যাটাগরি ইনপুট ডায়ালগ 🌟
    // ==========================================
    private void showDuplicateDetailsDialog(final File originalDir) {
        final com.google.android.material.bottomsheet.BottomSheetDialog dupSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        dupSheet.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(surfaceColor);
        root.setPadding(64, 64, 64, 64);

        TextView title = new TextView(this);
        title.setText("ডুপ্লিকেট প্রজেক্টের তথ্য");
        title.setTextColor(primaryTextColor);
        title.setTextSize(20f);
        title.setTypeface(currentTypeface, Typeface.BOLD);
        title.setPadding(0, 0, 0, 32);
        root.addView(title);

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(bgColor);
        inputBg.setCornerRadius(24f);

        // 🌟 ইনপুট: নতুন নাম 🌟
        final EditText etName = new EditText(this);
        etName.setHint("প্রজেক্টের নতুন নাম");
        etName.setText(projectName + " (Copy)");
        etName.setTextColor(primaryTextColor);
        etName.setHintTextColor(secondaryTextColor);
        etName.setBackground(inputBg);
        etName.setPadding(40, 40, 40, 40);
        etName.setTypeface(currentTypeface);
        etName.setSingleLine(true);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p1.setMargins(0, 0, 0, 32);
        etName.setLayoutParams(p1);
        root.addView(etName);

        // 🌟 ইনপুট: নতুন ক্যাটাগরি 🌟
        final EditText etCategory = new EditText(this);
        etCategory.setHint("ক্যাটাগরি / ফোল্ডার পাথ");
        etCategory.setText(categoryName);
        etCategory.setTextColor(primaryTextColor);
        etCategory.setHintTextColor(secondaryTextColor);
        etCategory.setBackground(inputBg);
        etCategory.setPadding(40, 40, 40, 40);
        etCategory.setTypeface(currentTypeface);
        etCategory.setSingleLine(true);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p2.setMargins(0, 0, 0, 48);
        etCategory.setLayoutParams(p2);
        root.addView(etCategory);

        TextView btnSave = createActionBtn("সেভ করুন", accentColor);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newName = etName.getText().toString().trim();
                String newCat = etCategory.getText().toString().trim();
                
                if (newName.isEmpty() || newCat.isEmpty()) {
                    Toast.makeText(ProjectViewActivity.this, "নাম এবং ক্যাটাগরি দিন!", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                dupSheet.dismiss();
                // 🌟 ইউজার ইনপুট অনুযায়ী নতুন ডিরেক্টরি তৈরি 🌟
                File newDestDir = new File(getFilesDir(), "TunePad_Data/Projects/" + newCat + "/" + newName);
                saveImportedProject(newDestDir, newName, newCat); 
            }
        });
		
         
        root.addView(btnSave);
        dupSheet.setContentView(root);
        dupSheet.show();
    }

    // ==========================================
    // 🌟 প্রিভিউ ডেটা প্রস্তুত করা 🌟 
    // ==========================================
    private void preparePreviewSharedPreferences() {
        File metaFile = new File(projectDir, "project_meta_data.json");
        SharedPreferences tempPrefs = getSharedPreferences("PreviewData_" + projectName, MODE_PRIVATE);
        tempPrefs.edit().clear().apply(); 

        if (metaFile.exists()) {
            try {
                java.io.FileInputStream fis = new java.io.FileInputStream(metaFile);
                byte[] data = new byte[(int) metaFile.length()];
                fis.read(data); fis.close();
                org.json.JSONObject json = new org.json.JSONObject(new String(data, "UTF-8"));

                SharedPreferences.Editor editor = tempPrefs.edit();
                java.util.Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (key.equals("project_characters")) {
                        editor.putString("temp_characters_json", json.getJSONArray(key).toString());
                        continue;
                    }
                    Object value = json.get(key);
                    if (value instanceof String) editor.putString(key, (String) value);
                    else if (value instanceof Integer) editor.putInt(key, (Integer) value);
                    else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
                    else if (value instanceof Long) editor.putLong(key, (Long) value);
                }
                editor.apply();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // ==========================================
    // 🌟 প্রজেক্ট সেভ ও মেটা-ডেটা রিস্টোর ইঞ্জিন (Dynamic Mapping) 🌟 
    // ==========================================
    private void saveImportedProject(File destDir, String finalProjName, String finalCategoryName) {
        destDir.getParentFile().mkdirs();
        projectDir.renameTo(destDir); // ফোল্ডার রিনেম করে নতুন জায়গায় নেওয়া হলো

        // 🌟 ১. প্রজেক্ট মেটা-ডেটা আপডেট 🌟
        SharedPreferences mainPrefs = getSharedPreferences("ProjectData_" + finalProjName, MODE_PRIVATE);
        SharedPreferences.Editor editor = mainPrefs.edit();
        
        java.util.Map<String, ?> allEntries = projectDataPrefs.getAll();
        for (java.util.Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().equals("temp_characters_json")) continue; 
            Object value = entry.getValue();
            if (value instanceof String) editor.putString(entry.getKey(), (String) value);
            else if (value instanceof Integer) editor.putInt(entry.getKey(), (Integer) value);
            else if (value instanceof Boolean) editor.putBoolean(entry.getKey(), (Boolean) value);
            else if (value instanceof Long) editor.putLong(entry.getKey(), (Long) value);
        }
        editor.putString("genre", finalCategoryName); // ক্যাটাগরি আপডেট করা হলো
        editor.apply();

        // 🌟 ২. চরিত্র (Characters) ও সম্পর্ক আপডেট 🌟
        try {
            String charJsonStr = projectDataPrefs.getString("temp_characters_json", "[]");
            org.json.JSONArray charArray = new org.json.JSONArray(charJsonStr);
            SharedPreferences globalCharPrefs = getSharedPreferences("Global_Characters_DB", MODE_PRIVATE);
            SharedPreferences.Editor charEditor = globalCharPrefs.edit();
            int currentGlobalCount = globalCharPrefs.getInt("char_count", 0);

            // ওভাররাইট করার সময় আগের চরিত্র হাইড করা (শুধুমাত্র যদি অরিজিনাল নামেই সেভ হয়)
            if (finalProjName.equals(projectName)) {
                for (int i = 0; i < currentGlobalCount; i++) {
                    if (globalCharPrefs.getBoolean("char_active_" + i, false)) {
                        String story = globalCharPrefs.getString("char_story_" + i, "");
                        if (story.trim().equalsIgnoreCase(finalProjName.trim())) {
                            charEditor.putBoolean("char_active_" + i, false);
                        }
                    }
                }
            }

            java.util.HashMap<Integer, Integer> idMapping = new java.util.HashMap<>();

            for(int i = 0; i < charArray.length(); i++) {
                org.json.JSONObject obj = charArray.getJSONObject(i);
                int newId = currentGlobalCount + i;
                int oldId = obj.optInt("old_id", -1);
                if (oldId != -1) idMapping.put(oldId, newId);

                charEditor.putBoolean("char_active_" + newId, true);
                charEditor.putString("char_story_" + newId, finalProjName); // নতুন প্রজেক্টের নাম বসবে
                
                java.util.Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (key.equals("old_id") || key.equals("rels") || key.equals("char_story") || key.equals("story")) continue; 
                    
                    Object val = obj.get(key);
                    if (val instanceof String) charEditor.putString("char_" + key + "_" + newId, (String) val);
                    else if (val instanceof Integer) charEditor.putInt("char_" + key + "_" + newId, (Integer) val);
                }
            }
            
            // রিলেশনশিপ (Relationships) আইডি ম্যাপিং করে আপডেট করা
            for(int i = 0; i < charArray.length(); i++) {
                org.json.JSONObject obj = charArray.getJSONObject(i);
                int newId = currentGlobalCount + i;
                String oldRels = obj.optString("rels", "");
                
                if (!oldRels.isEmpty()) {
                    StringBuilder newRels = new StringBuilder();
                    String[] relList = oldRels.split(";;");
                    for (String rel : relList) {
                        if (rel.contains("|")) {
                            String[] parts = rel.split("\\|");
                            int oldTargetId = Integer.parseInt(parts[0]);
                            String relationType = parts[1];
                            
                            if (idMapping.containsKey(oldTargetId)) {
                                int newTargetId = idMapping.get(oldTargetId);
                                if (newRels.length() > 0) newRels.append(";;");
                                newRels.append(newTargetId).append("|").append(relationType);
                            }
                        }
                    }
                    charEditor.putString("char_rels_" + newId, newRels.toString());
                }
            }

            charEditor.putInt("char_count", currentGlobalCount + charArray.length());
            charEditor.apply();
        } catch(Exception e){ e.printStackTrace(); }

        // 🌟 ৩. নোটসমূহ নতুন নাম ও ফোল্ডার অনুযায়ী ডাটাবেসে সেভ করা 🌟
        android.database.sqlite.SQLiteDatabase db = openOrCreateDatabase("notes_db_v3", MODE_PRIVATE, null);
        File[] files = destDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".tpad")) {
                    String chapName = f.getName().replace(".tpad", "");
                    String uniqueTitle = finalProjName + "_" + chapName; // নতুন নাম দিয়ে ইউনিক টাইটেল
                    
                    String content = "";
                    try {
                        java.io.FileInputStream fis = new java.io.FileInputStream(f);
                        byte[] data = new byte[(int) f.length()];
                        fis.read(data); fis.close();
                        content = new String(data, "UTF-8");
                    } catch(Exception e){}
                    
                    android.content.ContentValues cv = new android.content.ContentValues();
                    cv.put("id", "imp_" + System.currentTimeMillis() + "_" + chapName);
                    cv.put("title", uniqueTitle);
                    cv.put("content", content);
                    cv.put("label", "Project: " + finalProjName); // নতুন লেবেল
                    cv.put("timestamp", new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date()));
                    cv.put("isPinned", 0); cv.put("isDeleted", 0); cv.put("isDraft", 0); cv.put("isHidden", 0);
                    
                    db.delete("notes", "title=?", new String[]{uniqueTitle});
                    db.insertWithOnConflict("notes", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
                }
            }
        }
        db.close();

        // অরিজিনাল নামের টেম্পোরারি ডেটা মুছে ফেলা
        getSharedPreferences("PreviewData_" + projectName, MODE_PRIVATE).edit().clear().apply();
        Toast.makeText(this, "প্রজেক্ট সফলভাবে সেভ হয়েছে! 🎉", Toast.LENGTH_LONG).show();
        
        Intent intent = new Intent(this, MmmActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // ==========================================
    // 🌟 বাতিল করার লজিক (Discard) 🌟
    // ==========================================
    private void discardPreview() {
        deleteRecursiveFolder(projectDir);
        getSharedPreferences("PreviewData_" + projectName, MODE_PRIVATE).edit().clear().apply();
        Toast.makeText(this, "প্রজেক্ট ইমপোর্ট বাতিল করা হয়েছে।", Toast.LENGTH_SHORT).show();
        
        Intent intent = new Intent(this, MmmActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // ==========================================
    // 🌟 হেল্পার: বড় অ্যাকশন বাটন তৈরি 🌟
    // ==========================================
    private android.widget.TextView createActionBtn(String text, int color) {
        android.widget.TextView btn = new android.widget.TextView(this);
        btn.setText(text);
        btn.setTextColor(android.graphics.Color.WHITE);
        btn.setGravity(android.view.Gravity.CENTER);
        btn.setPadding(0, 40, 0, 40);
        
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(50f);
        btn.setBackground(bg);
        
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 16, 0, 16);
        btn.setLayoutParams(lp);
        btn.setTypeface(currentTypeface, android.graphics.Typeface.BOLD);
        
        return btn;
    }
    
	
  private void applyFontToAllViews(android.view.View view, android.graphics.Typeface typeface) {
    if (view == null || typeface == null) return;
    
    if (view instanceof android.view.ViewGroup) {
        android.view.ViewGroup vg = (android.view.ViewGroup) view;
        for (int i = 0; i < vg.getChildCount(); i++) {
            applyFontToAllViews(vg.getChildAt(i), typeface);
        }
    } else if (view instanceof android.widget.TextView) {
        android.widget.TextView tv = (android.widget.TextView) view;
        
        // ফিক্স: বোল্ড স্টাইল বজায় রেখে ফন্ট বসানো
        if (tv.getTypeface() != null && tv.getTypeface().isBold()) {
            tv.setTypeface(typeface, android.graphics.Typeface.BOLD);
        } else {
            tv.setTypeface(typeface);
        }
    }
}

}
