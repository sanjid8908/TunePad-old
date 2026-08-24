package com.megh.notepad;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class CharactersActivity extends AppCompatActivity {

    private String projectName;
    private SharedPreferences charPrefs;

    private LinearLayout rootCharactersView, toolbarLayout, searchContainer;
    private ImageView btnBack, btnAddCharacter, btnFilter, icSearchIcon, btnMoreHeader;
    private TextView tvToolbarTitle, tvEmptyCharacters;
    private EditText etSearchCharacter;
    private RecyclerView rvCharacters;

    private CharacterAdapter characterAdapter;
    private List<CharacterModel> allCharacterList = new ArrayList<>();
    private List<CharacterModel> filteredList = new ArrayList<>();

    private int bgColor, surfaceColor, accentColor, primaryTextColor, secondaryTextColor;
    private Typeface currentTypeface = Typeface.DEFAULT;
    
    private int currentSortIndex = 2; 
    private static final int IMPORT_CHAR_REQUEST = 4005;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.characters); 

        projectName = getIntent().getStringExtra("PROJECT_NAME");
        if (projectName == null) projectName = "Unknown";

        charPrefs = getSharedPreferences("Global_Characters_DB", MODE_PRIVATE);

        initViews();
        applyThemeColors();

        btnBack.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { finish(); } });
        
        btnAddCharacter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPremiumAddDialog();
            }
        });

        etSearchCharacter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCharacters(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPremiumSortBottomSheet(); 
            }
        });

        rvCharacters.setLayoutManager(new GridLayoutManager(this, 2));
        characterAdapter = new CharacterAdapter();
        rvCharacters.setAdapter(characterAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCharacters(); 
    }

    private void initViews() {
        rootCharactersView = findViewById(R.id.rootCharactersView);
        toolbarLayout = findViewById(R.id.toolbarLayout);
        btnBack = findViewById(R.id.btnBack);
        btnAddCharacter = findViewById(R.id.btnAddCharacter);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvEmptyCharacters = findViewById(R.id.tvEmptyCharacters);
        rvCharacters = findViewById(R.id.rvCharacters);
        searchContainer = findViewById(R.id.searchContainer);
        etSearchCharacter = findViewById(R.id.etSearchCharacter);
        btnFilter = findViewById(R.id.btnFilter);
        icSearchIcon = findViewById(R.id.icSearchIcon);
        
        btnMoreHeader = new ImageView(this);
        btnMoreHeader.setImageResource(android.R.drawable.ic_menu_more);
        LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(
                (int)(48 * getResources().getDisplayMetrics().density), 
                (int)(48 * getResources().getDisplayMetrics().density));
        moreParams.gravity = Gravity.CENTER_VERTICAL;
        btnMoreHeader.setLayoutParams(moreParams);
        btnMoreHeader.setPadding(24, 24, 24, 24);
        toolbarLayout.addView(btnMoreHeader);

        btnMoreHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHeaderMenu();
            }
        });
    }

       // ==========================================
    // 🌟 Theme & Font Setup Method 🌟
    // ==========================================
    private void applyThemeColors() {
        // 🌟 ম্যাজিক: ThemeHelper থেকে সরাসরি কাস্টম ফন্ট লোড করা হলো 🌟
        currentTypeface = ThemeHelper.getCustomTypeface(this);

        bgColor = ThemeHelper.getBgColor(this);
        surfaceColor = ThemeHelper.getSurfaceColor(this);
        accentColor = ThemeHelper.getAccentColor(this);
        primaryTextColor = ThemeHelper.getPrimaryTextColor(this);
        secondaryTextColor = ThemeHelper.getSecondaryTextColor(this);

        getWindow().setStatusBarColor(bgColor);
        rootCharactersView.setBackgroundColor(bgColor);
        
        tvToolbarTitle.setTextColor(primaryTextColor);
        tvToolbarTitle.setTypeface(currentTypeface, Typeface.BOLD);
        tvEmptyCharacters.setTextColor(secondaryTextColor);
        tvEmptyCharacters.setTypeface(currentTypeface);
        
        btnBack.setColorFilter(primaryTextColor);
        btnAddCharacter.setColorFilter(accentColor);
        btnFilter.setColorFilter(accentColor);
        btnMoreHeader.setColorFilter(primaryTextColor);
        
        icSearchIcon.setColorFilter(secondaryTextColor);
        etSearchCharacter.setTextColor(primaryTextColor);
        etSearchCharacter.setHintTextColor(secondaryTextColor);
        etSearchCharacter.setTypeface(currentTypeface);
        
        LinearLayout.LayoutParams searchIconParams = (LinearLayout.LayoutParams) icSearchIcon.getLayoutParams();
        searchIconParams.setMargins(16, 0, 16, 0); 
        icSearchIcon.setLayoutParams(searchIconParams);

        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(surfaceColor);
        searchBg.setCornerRadius(100f);
        searchContainer.setBackground(searchBg);
    }


    private void loadCharacters() {
        allCharacterList.clear();
        int count = charPrefs.getInt("char_count", 0);
        
        for (int i = 0; i < count; i++) {
            if (charPrefs.getBoolean("char_active_" + i, false)) {
                String story = charPrefs.getString("char_story_" + i, "");
                
                boolean isMatch = false;

                if (projectName == null || projectName.equals("Unknown") || projectName.equalsIgnoreCase("All")) {
                    isMatch = true;
                }
                else if (story.trim().equalsIgnoreCase(projectName.trim()) || 
                         story.toLowerCase().contains(projectName.toLowerCase()) || 
                         projectName.toLowerCase().contains(story.toLowerCase())) {
                    isMatch = true;
                }

                if (isMatch) {
                    CharacterModel cm = new CharacterModel();
                    cm.id = i;
                    cm.name = charPrefs.getString("char_name_" + i, "");
                    cm.role = charPrefs.getString("char_role_" + i, "ভূমিকা নেই");
                    cm.storyName = story.replace("Project: ", ""); 
                    cm.imageUri = charPrefs.getString("char_img_" + i, "");
                    allCharacterList.add(cm);
                }
            }
        }
        
        applyCurrentSorting();
        filterCharacters(etSearchCharacter.getText().toString());
    }

    private void applyCurrentSorting() {
        if (currentSortIndex == 0) {
            Collections.sort(allCharacterList, new Comparator<CharacterModel>() {
                @Override public int compare(CharacterModel o1, CharacterModel o2) { return o1.name.compareToIgnoreCase(o2.name); }
            });
        } else if (currentSortIndex == 1) {
            Collections.sort(allCharacterList, new Comparator<CharacterModel>() {
                @Override public int compare(CharacterModel o1, CharacterModel o2) { return o1.role.compareToIgnoreCase(o2.role); }
            });
        } else {
            Collections.sort(allCharacterList, new Comparator<CharacterModel>() {
                @Override public int compare(CharacterModel o1, CharacterModel o2) { return Integer.compare(o2.id, o1.id); }
            });
        }
    }

    private void filterCharacters(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(allCharacterList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (CharacterModel cm : allCharacterList) {
                if (cm.name.toLowerCase().contains(lowerQuery) || cm.storyName.toLowerCase().contains(lowerQuery)) {
                    filteredList.add(cm);
                }
            }
        }
        
        if (filteredList.isEmpty()) tvEmptyCharacters.setVisibility(View.VISIBLE);
        else tvEmptyCharacters.setVisibility(View.GONE);
        
        characterAdapter.notifyDataSetChanged();
    }

    private void showPremiumSortBottomSheet() {
        final BottomSheetDialog sortSheet = new BottomSheetDialog(this);
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(surfaceColor);
        rootLayout.setPadding(64, 64, 64, 64);

        TextView titleView = new TextView(this);
        titleView.setText("সাজান (Sort By)");
        titleView.setTextColor(primaryTextColor);
        titleView.setTextSize(20f);
        titleView.setTypeface(currentTypeface, Typeface.BOLD);
        titleView.setPadding(0, 0, 0, 48);
        rootLayout.addView(titleView);

        String[] options = {"নাম অনুযায়ী (A-Z)", "ভূমিকা অনুযায়ী", "নতুন আগে"};
        for (int i = 0; i < options.length; i++) {
            TextView tvOption = new TextView(this);
            tvOption.setText(options[i]);
            if (i == currentSortIndex) {
                tvOption.setTextColor(accentColor);
                tvOption.setTypeface(currentTypeface, Typeface.BOLD);
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
                    currentSortIndex = index; 
                    applyCurrentSorting();
                    filterCharacters(etSearchCharacter.getText().toString());
                    sortSheet.dismiss();
                }
            });
            rootLayout.addView(tvOption);
        }
        sortSheet.setContentView(rootLayout);
        sortSheet.show();
    }

    private void showPremiumAddDialog() {
        final BottomSheetDialog addSheet = new BottomSheetDialog(this);
        addSheet.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(surfaceColor);
        rootLayout.setPadding(64, 64, 64, 64);

        TextView titleView = new TextView(this);
        titleView.setText("নতুন চরিত্র");
        titleView.setTextColor(primaryTextColor);
        titleView.setTextSize(20f);
        titleView.setTypeface(currentTypeface, Typeface.BOLD);
        titleView.setPadding(0, 0, 0, 32);
        rootLayout.addView(titleView);

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(bgColor);
        inputBg.setCornerRadius(24f);

        final EditText etName = new EditText(this);
        etName.setHint("চরিত্রের নাম (যেমন: মেঘ)");
        etName.setTextColor(primaryTextColor);
        etName.setHintTextColor(secondaryTextColor);
        etName.setBackground(inputBg);
        etName.setPadding(40, 32, 40, 32);
        etName.setTypeface(currentTypeface);
        etName.setSingleLine(true);
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
                if(!name.isEmpty()){
                    int newId = charPrefs.getInt("char_count", 0);
                    charPrefs.edit()
                        .putBoolean("char_active_" + newId, true)
                        .putString("char_name_" + newId, name)
                        .putString("char_story_" + newId, projectName) 
                        .putInt("char_count", newId + 1).apply();
                    
                    Intent intent = new Intent(CharactersActivity.this, CharacterDetailsActivity.class);
                    intent.putExtra("PROJECT_NAME", projectName);
                    intent.putExtra("CHAR_ID", newId);
                    startActivity(intent);
                    addSheet.dismiss();
                } else {
                    Toast.makeText(CharactersActivity.this, "চরিত্রের নাম দিন!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        rootLayout.addView(btnCreate);

        addSheet.setContentView(rootLayout);
        addSheet.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                etName.requestFocus();
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(etName, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });
        addSheet.show();
    }

    private void showHeaderMenu() {
        final BottomSheetDialog sheet = new BottomSheetDialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(surfaceColor);
        root.setPadding(0, 32, 0, 32);
        
        root.addView(createMenuItem("চরিত্র ইমপোর্ট করুন (.tchar)", android.R.drawable.ic_menu_upload, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheet.dismiss();
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*"); 
                startActivityForResult(intent, IMPORT_CHAR_REQUEST);
            }
        }));
        
        sheet.setContentView(root);
        sheet.show();
    }

    class CharacterModel { int id; String name, role, storyName, imageUri; }

    private class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.CharViewHolder> {

        @NonNull
        @Override
        public CharViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            
            android.widget.FrameLayout cardRoot = new android.widget.FrameLayout(CharactersActivity.this);
            RecyclerView.LayoutParams rootLp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rootLp.setMargins(16, 16, 16, 16);
            cardRoot.setLayoutParams(rootLp);

            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(32f);
            shape.setColor(surfaceColor);
            cardRoot.setBackground(shape);
            cardRoot.setElevation(4f);

            LinearLayout card = new LinearLayout(CharactersActivity.this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setLayoutParams(new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            card.setPadding(24, 40, 24, 40);

            androidx.cardview.widget.CardView imgCard = new androidx.cardview.widget.CardView(CharactersActivity.this);
            int sizeInPx = (int) (70 * getResources().getDisplayMetrics().density); 
            imgCard.setRadius(sizeInPx / 2f); 
            imgCard.setCardElevation(0f);
            imgCard.setLayoutParams(new LinearLayout.LayoutParams(sizeInPx, sizeInPx));
            
            ImageView imgProfile = new ImageView(CharactersActivity.this);
            imgProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imgCard.addView(imgProfile);
            
            TextView tvInitials = new TextView(CharactersActivity.this);
            tvInitials.setGravity(Gravity.CENTER);
            tvInitials.setTextColor(Color.WHITE);
            tvInitials.setTextSize(32f);
            tvInitials.setTypeface(currentTypeface, Typeface.BOLD);
            tvInitials.setBackgroundColor(accentColor);
            imgCard.addView(tvInitials);

            card.addView(imgCard);

            TextView tvName = new TextView(CharactersActivity.this);
            tvName.setTextColor(primaryTextColor);
            tvName.setTextSize(16f);
            tvName.setTypeface(currentTypeface, Typeface.BOLD);
            tvName.setGravity(Gravity.CENTER);
            tvName.setPadding(0, 16, 0, 4);
            card.addView(tvName);

            TextView tvStory = new TextView(CharactersActivity.this);
            tvStory.setTextColor(secondaryTextColor);
            tvStory.setTextSize(11f);
            tvStory.setTypeface(currentTypeface);
            tvStory.setGravity(Gravity.CENTER);
            tvStory.setPadding(0, 0, 0, 8);
            card.addView(tvStory);

            TextView tvRole = new TextView(CharactersActivity.this);
            tvRole.setTextColor(accentColor);
            tvRole.setTextSize(12f);
            tvRole.setTypeface(currentTypeface, Typeface.BOLD);
            tvRole.setGravity(Gravity.CENTER);
            card.addView(tvRole);

            cardRoot.addView(card);

            TextView btnCardMore = new TextView(CharactersActivity.this);
            btnCardMore.setText("⋮");
            btnCardMore.setTextColor(primaryTextColor);
            btnCardMore.setTextSize(24f);
            btnCardMore.setTypeface(null, Typeface.BOLD);
            btnCardMore.setPadding(24, 8, 24, 24);
            
            android.widget.FrameLayout.LayoutParams moreParams = new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            moreParams.gravity = Gravity.TOP | Gravity.END;
            btnCardMore.setLayoutParams(moreParams);
            cardRoot.addView(btnCardMore);

            return new CharViewHolder(cardRoot, imgProfile, tvInitials, tvName, tvStory, tvRole, btnCardMore);
        }

        @Override
        public void onBindViewHolder(@NonNull final CharViewHolder holder, final int position) {
            final CharacterModel cm = filteredList.get(position);
            holder.tvName.setText(cm.name);
            holder.tvStory.setText("গল্প: " + cm.storyName);
            holder.tvRole.setText(cm.role.isEmpty() ? "ভূমিকা নেই" : cm.role);
            
            if (!cm.imageUri.isEmpty()) {
                holder.imgProfile.setVisibility(View.GONE);
                holder.tvInitials.setVisibility(View.VISIBLE);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(android.net.Uri.parse(cm.imageUri));
                            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                            options.inSampleSize = 4; 
                            final android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, options);
                            if (inputStream != null) inputStream.close();

                            if (bitmap != null) {
                                holder.imgProfile.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (holder.getAdapterPosition() == position) {
                                            holder.imgProfile.setImageBitmap(bitmap);
                                            holder.imgProfile.setVisibility(View.VISIBLE);
                                            holder.tvInitials.setVisibility(View.GONE);
                                        }
                                    }
                                });
                            }
                        } catch (Exception e) {}
                    }
                }).start();
            } else {
                holder.imgProfile.setVisibility(View.GONE);
                holder.tvInitials.setVisibility(View.VISIBLE);
            }

            if (holder.tvInitials.getVisibility() == View.VISIBLE) {
                String initials = "?";
                String cleanName = cm.name.trim(); 
                if(cleanName.length() > 0) {
                    initials = String.valueOf(cleanName.charAt(0));
                    if(cleanName.contains(" ")) {
                        String[] parts = cleanName.split("\\s+"); 
                        if(parts.length > 1 && parts[1].length() > 0) {
                            initials = String.valueOf(cleanName.charAt(0)) + String.valueOf(parts[1].charAt(0));
                        }
                    }
                }
                holder.tvInitials.setText(initials.toUpperCase());
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(CharactersActivity.this, CharacterDetailsActivity.class);
                    intent.putExtra("PROJECT_NAME", projectName);
                    intent.putExtra("CHAR_ID", cm.id);
                    startActivity(intent);
                }
            });

            holder.btnCardMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCardMenu(cm);
                }
            });
        }

        @Override
        public int getItemCount() { return filteredList.size(); }

        class CharViewHolder extends RecyclerView.ViewHolder {
            ImageView imgProfile; TextView tvInitials, tvName, tvStory, tvRole, btnCardMore;
            public CharViewHolder(@NonNull View itemView, ImageView img, TextView init, TextView name, TextView story, TextView role, TextView more) {
                super(itemView); imgProfile = img; tvInitials = init; tvName = name; tvStory = story; tvRole = role; btnCardMore = more;
            }
        }
    }

    private void showCardMenu(final CharacterModel cm) {
        final BottomSheetDialog sheet = new BottomSheetDialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(surfaceColor);
        root.setPadding(0, 32, 0, 32);

        TextView title = new TextView(this); 
        title.setText(cm.name); 
        title.setTextColor(accentColor); 
        title.setTextSize(20f); 
        title.setTypeface(currentTypeface, Typeface.BOLD); 
        title.setPadding(64, 32, 64, 48); 
        root.addView(title);
        
        root.addView(createMenuItem("ক্যারেক্টার এক্সপোর্ট করুন (.tchar)", android.R.drawable.ic_menu_save, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheet.dismiss();
                exportCharacter(cm);
            }
        }));

        // 🌟 নতুন ফিচার: PDF হিসেবে ডাউনলোড 🌟
        root.addView(createMenuItem("PDF হিসেবে সেভ করুন", android.R.drawable.ic_menu_agenda, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheet.dismiss();
                exportCharacterAsPDF(cm);
            }
        }));
        
        // showCardMenu() এর ভেতরে যেখানে এক্সপোর্ট বাটন আছে, তার ঠিক নিচেই এটি বসান:
root.addView(createMenuItem("লিংক শেয়ার করুন 🔗", android.R.drawable.ic_menu_share, new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        sheet.dismiss();
        uploadAndShareCharacter(cm);
    }
}));

        root.addView(createMenuItem("মুছে ফেলুন", android.R.drawable.ic_menu_delete, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheet.dismiss();
                showDeleteConfirmationSheet(cm.name, new Runnable() {
                    @Override
                    public void run() {
                        charPrefs.edit().putBoolean("char_active_" + cm.id, false).apply();
                        Toast.makeText(CharactersActivity.this, "চরিত্র মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show();
                        loadCharacters();
                    }
                });
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

  // 🌟 ক্যারেক্টার এক্সপোর্ট লজিক (ছবি সহ) 🌟
private void exportCharacter(CharacterModel cm) {
    try {
        org.json.JSONObject json = new org.json.JSONObject();
        java.util.Map<String, ?> allEntries = charPrefs.getAll();
        for (java.util.Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            
            if (key.endsWith("_" + cm.id) && !key.startsWith("char_active_")) {
                String cleanKey = key.replace("_" + cm.id, "");
                
                // 🌟 ম্যাজিক ফিক্স: এক্সপোর্টের সময়ও ছবি এনকোড করা হচ্ছে 🌟
                if (cleanKey.equals("char_img")) {
                    String imgUri = String.valueOf(entry.getValue());
                    String base64Image = imageUriToBase64(imgUri);
                    json.put("char_img_base64", base64Image); 
                    json.put("char_img", imgUri); 
                } else {
                    json.put(cleanKey, entry.getValue());
                }
            }
        }

        // ফাইল ম্যানেজার থেকে লুকাতে Base64 এনকোডিং 
        String encodedData = android.util.Base64.encodeToString(json.toString().getBytes("UTF-8"), android.util.Base64.DEFAULT);

        File dir = new File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "TunePad");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, cm.name + ".tchar");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
        fos.write(encodedData.getBytes());
        fos.close();

        Toast.makeText(this, "সফলভাবে " + cm.name + ".tchar এক্সপোর্ট হয়েছে! 🔒", Toast.LENGTH_LONG).show();
    } catch (Exception e) {
        Toast.makeText(this, "এক্সপোর্ট করতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show();
    }
}

    // ==========================================
    // 🌟 ক্যারেক্টার ইমপোর্ট লজিক (স্মার্ট অ্যাসাইনমেন্ট) 🌟
    // ==========================================
    private void processImportedCharacter(android.net.Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String encodedData = new String(buffer);
            String decodedStr = new String(Base64.decode(encodedData, Base64.DEFAULT), "UTF-8");
            JSONObject json = new JSONObject(decodedStr);

            int newId = charPrefs.getInt("char_count", 0);
            SharedPreferences.Editor editor = charPrefs.edit();
            
            editor.putBoolean("char_active_" + newId, true);
            
            // 🌟 ফিক্সড: গল্পের নাম ইমপোর্টের স্মার্ট লজিক 🌟
            boolean isGlobalView = projectName == null || projectName.equals("Unknown") || projectName.equalsIgnoreCase("All");
            String importedStoryName = json.optString("char_story", "অজানা গল্প");
            
            // "সব চরিত্র" থেকে ইমপোর্ট হলে অরিজিনাল নাম থাকবে, আর প্রজেক্ট থেকে হলে ওই প্রজেক্টের নাম নেবে
            String finalStoryName = isGlobalView ? importedStoryName : projectName;
            editor.putString("char_story_" + newId, finalStoryName);

            java.util.Iterator<String> keys = json.keys();
            while(keys.hasNext()) {
                String key = keys.next();
                if (key.equals("char_story")) continue; // এটা আমরা আগেই হ্যান্ডেল করেছি
                
                Object val = json.get(key);
                if (val instanceof String) editor.putString(key + "_" + newId, (String)val);
                else if (val instanceof Integer) editor.putInt(key + "_" + newId, (Integer)val);
                else if (val instanceof Boolean) editor.putBoolean(key + "_" + newId, (Boolean)val);
            }
            editor.putInt("char_count", newId + 1);
            editor.apply();

            Toast.makeText(this, "চরিত্র সফলভাবে ইমপোর্ট হয়েছে! 🚀", Toast.LENGTH_SHORT).show();
            loadCharacters();
        } catch (Exception e) {
            Toast.makeText(this, "ভুল ফাইল বা করাপ্টেড ডেটা!", Toast.LENGTH_SHORT).show();
        }
    }

    // ==========================================
    // 🌟 নতুন লজিক: ক্যারেক্টার প্রোফাইল PDF হিসেবে সেভ 🌟
    // ==========================================
    private void exportCharacterAsPDF(CharacterModel cm) {
        try {
            // PDF ডকুমেন্ট তৈরি
            android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
            android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 Size
            android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);
            android.graphics.Canvas canvas = page.getCanvas();
            android.graphics.Paint paint = new android.graphics.Paint();

            int startX = 50;
            int startY = 70;

            // হেডার (ক্যারেক্টারের নাম)
            paint.setTypeface(currentTypeface != null ? currentTypeface : Typeface.DEFAULT_BOLD);
            paint.setTextSize(26f);
            paint.setColor(Color.parseColor("#1A1A1A"));
            canvas.drawText("চরিত্রের বিস্তারিত তথ্য: " + cm.name, startX, startY, paint);
            startY += 40;
            
            // আন্ডারলাইন
            paint.setStrokeWidth(2f);
            canvas.drawLine(startX, startY, 545, startY, paint);
            startY += 40;

            paint.setTextSize(16f);
            paint.setTypeface(currentTypeface != null ? currentTypeface : Typeface.DEFAULT);
            paint.setColor(Color.parseColor("#333333"));

            // ডাটাবেস থেকে সব তথ্য আনা
            String role = charPrefs.getString("char_role_" + cm.id, "অজানা");
            String age = charPrefs.getString("char_age_" + cm.id, "অজানা");
            String location = charPrefs.getString("char_location_" + cm.id, "অজানা");
            String personality = charPrefs.getString("char_personality_" + cm.id, "অজানা");
            String backstory = charPrefs.getString("char_backstory_" + cm.id, "কোনো প্রেক্ষাপট নেই");

            // বেসিক ইনফো প্রিন্ট
            canvas.drawText("গল্পের নাম: " + cm.storyName, startX, startY, paint); startY += 35;
            canvas.drawText("চরিত্রের ভূমিকা: " + role, startX, startY, paint); startY += 35;
            canvas.drawText("বয়স: " + age, startX, startY, paint); startY += 35;
            canvas.drawText("বাসস্থান/স্থান: " + location, startX, startY, paint); startY += 35;
            canvas.drawText("ব্যক্তিত্ব: " + personality, startX, startY, paint); startY += 50;

            paint.setTypeface(currentTypeface != null ? currentTypeface : Typeface.DEFAULT_BOLD);
            canvas.drawText("প্রেক্ষাপট (Backstory):", startX, startY, paint); startY += 20;

            // ব্যাকস্টোরি মাল্টি-লাইন হতে পারে, তাই StaticLayout ব্যবহার
            android.text.TextPaint textPaint = new android.text.TextPaint();
            textPaint.setColor(Color.parseColor("#444444"));
            textPaint.setTextSize(14f);
            textPaint.setTypeface(currentTypeface != null ? currentTypeface : Typeface.DEFAULT);

            android.text.StaticLayout staticLayout = new android.text.StaticLayout(
                    backstory, textPaint, 495, // 595 (A4 Width) - 100 (Margins)
                    android.text.Layout.Alignment.ALIGN_NORMAL, 1.2f, 0.0f, false
            );
            
            canvas.save();
            canvas.translate(startX, startY);
            staticLayout.draw(canvas);
            canvas.restore();

            // পেজ সেভ করা
            document.finishPage(page);

            File dir = new File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "TunePad");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, cm.name + "_Profile.pdf");
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

            Toast.makeText(this, "সফলভাবে " + cm.name + "_Profile.pdf ডাউনলোড ফোল্ডারে সেভ হয়েছে!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "PDF তৈরি করতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMPORT_CHAR_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            processImportedCharacter(data.getData());
        }
    }

    

    private void showDeleteConfirmationSheet(final String itemName, final Runnable onDeleteConfirmed) {
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
        tvMsg.setText("আপনি কি সত্যিই '" + itemName + "' চরিত্রটি মুছে ফেলতে চান?");
        tvMsg.setTextColor(secondaryTextColor);
        tvMsg.setTextSize(15f);
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
                if (onDeleteConfirmed != null) {
                    onDeleteConfirmed.run(); 
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
    // 🌟 চরিত্রের লিংক জেনারেট ও শেয়ার করার ম্যাজিক (JSON Fix) 🌟
    // ==========================================
    private void uploadAndShareCharacter(final CharacterModel cm) {
        Toast.makeText(this, "চরিত্রের লিংক তৈরি হচ্ছে... ⏳", Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // ১. JSON এবং Base64 এনকোডিং
                    org.json.JSONObject json = new org.json.JSONObject();
                    java.util.Map<String, ?> allEntries = charPrefs.getAll();
                    for (java.util.Map.Entry<String, ?> entry : allEntries.entrySet()) {
                        String key = entry.getKey();
                        if (key.endsWith("_" + cm.id) && !key.startsWith("char_active_")) {
                            String cleanKey = key.replace("_" + cm.id, "");
                            json.put(cleanKey, entry.getValue());
                        }
                    }
                    String encodedData = android.util.Base64.encodeToString(json.toString().getBytes("UTF-8"), android.util.Base64.DEFAULT);

                    // ২. ক্যাশ মেমরিতে টেম্পোরারি .tchar ফাইল তৈরি
                    File cacheDir = getCacheDir();
                    String safeName = cm.name.replaceAll("[^a-zA-Z0-9_\\-\\u0980-\\u09FF ]", "_");
                    if (safeName.trim().isEmpty()) safeName = "Character";
                    File tcharFile = new File(cacheDir, "Char_" + cm.id + "_" + safeName + ".tchar");

                    java.io.FileOutputStream fos = new java.io.FileOutputStream(tcharFile);
                    fos.write(encodedData.getBytes("UTF-8"));
                    fos.close();

                    // ৩. সার্ভারে আপলোড (UTF-8 সেফটি সহ)
                    String uploadUrl = "https://www.shuvraafroj.info/api/upload_character.php";
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(uploadUrl).openConnection();
                    conn.setDoInput(true); conn.setDoOutput(true); conn.setUseCaches(false);
                    conn.setRequestMethod("POST");

                    String boundary = "*****" + System.currentTimeMillis() + "*****";
                    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                    java.io.DataOutputStream request = new java.io.DataOutputStream(conn.getOutputStream());
                    String crlf = "\r\n"; String twoHyphens = "--";

                    android.content.SharedPreferences appSettings = getSharedPreferences("AppSettings", MODE_PRIVATE);
                    String userNameEn = appSettings.getString("author_name_en", "Unknown_User");

                    request.write((twoHyphens + boundary + crlf).getBytes("UTF-8"));
                    request.write(("Content-Disposition: form-data; name=\"username\"" + crlf + crlf).getBytes("UTF-8"));
                    request.write((userNameEn + crlf).getBytes("UTF-8"));

                    request.write((twoHyphens + boundary + crlf).getBytes("UTF-8"));
                    request.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + tcharFile.getName() + "\"" + crlf).getBytes("UTF-8"));
                    request.write(("Content-Type: application/octet-stream" + crlf + crlf).getBytes("UTF-8"));

                    java.io.FileInputStream fis = new java.io.FileInputStream(tcharFile);
                    byte[] buffer = new byte[1024 * 1024]; int bytesRead;
                    while ((bytesRead = fis.read(buffer)) > 0) { request.write(buffer, 0, bytesRead); }
                    fis.close();

                    request.write((crlf).getBytes("UTF-8"));
                    request.write((twoHyphens + boundary + twoHyphens + crlf).getBytes("UTF-8"));
                    request.flush(); request.close();

                    final int responseCode = conn.getResponseCode();
                    java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) response.append(inputLine);
                    in.close();

                    if (tcharFile.exists()) tcharFile.delete(); // কাজ শেষে ডিলিট

                    // 🌟 ম্যাজিক ফিক্স: JSON রেসপন্স পার্স করা হচ্ছে 🌟
                    final String responseString = response.toString();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                                try {
                                    org.json.JSONObject serverResponse = new org.json.JSONObject(responseString);
                                    String status = serverResponse.optString("status");
                                    String message = serverResponse.optString("message");
                                    String fileUrl = serverResponse.optString("file_url");

                                    if ("success".equals(status) && fileUrl != null && !fileUrl.isEmpty()) {
                                        
                                        // লিংকের বাংলা লেখাগুলো ডিকোড করে সুন্দর করা হচ্ছে
                                        String beautifulUrl = java.net.URLDecoder.decode(fileUrl, "UTF-8");
                                        
                                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                                        intent.setType("text/plain");
                                        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, cm.name);
                                        intent.putExtra(android.content.Intent.EXTRA_TEXT, "TunePad-এ আমার তৈরি করা চরিত্র '" + cm.name + "' ইমপোর্ট করুন:\n\n" + beautifulUrl);
                                        startActivity(android.content.Intent.createChooser(intent, "লিংক শেয়ার করুন..."));
                                        
                                    } else {
                                        Toast.makeText(CharactersActivity.this, "❌ " + message, Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(CharactersActivity.this, "সার্ভার এরর! রেসপন্স পড়া যাচ্ছে না।", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(CharactersActivity.this, "সার্ভার ফেইল! কোড: " + responseCode, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() { @Override public void run() { Toast.makeText(CharactersActivity.this, "আপলোড ফেইল্ড: " + e.getMessage(), Toast.LENGTH_SHORT).show(); } });
                }
            }
        }).start();
    }

	// 🌟 ছবিকে টেক্সটে (Base64) রূপান্তর করার সম্পূর্ণ হেল্পার মেথড 🌟
private String imageUriToBase64(String uriString) {
    if (uriString == null || uriString.isEmpty()) return "";
    try {
        android.graphics.Bitmap bitmap = null;
        android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
        options.inSampleSize = 2; // শেয়ার করার জন্য ছবির সাইজ একটু ছোট ও ফাস্ট করা হলো

        if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
            java.io.InputStream is = getContentResolver().openInputStream(android.net.Uri.parse(uriString));
            bitmap = android.graphics.BitmapFactory.decodeStream(is, null, options);
            if (is != null) is.close();
        } else {
            java.io.File f = new java.io.File(uriString);
            if (f.exists()) bitmap = android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath(), options);
        }

        if (bitmap != null) {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos); // 70% কোয়ালিটি
            byte[] imageBytes = baos.toByteArray();
            return android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return "";
}


}
