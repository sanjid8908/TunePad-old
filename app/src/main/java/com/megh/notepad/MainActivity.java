package com.megh.notepad;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Intent;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.arch.core.*;
import androidx.asynclayoutinflater.*;
import androidx.coordinatorlayout.*;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.*;
import androidx.cursoradapter.*;
import androidx.customview.*;
import androidx.documentfile.*;
import androidx.drawerlayout.*;
import androidx.exifinterface.*;
import androidx.fragment.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.interpolator.*;
import androidx.legacy.coreui.*;
import androidx.legacy.coreutils.*;
import androidx.lifecycle.*;
import androidx.lifecycle.livedata.*;
import androidx.lifecycle.livedata.core.*;
import androidx.lifecycle.viewmodel.*;
import androidx.loader.*;
import androidx.localbroadcastmanager.*;
import androidx.print.*;
import androidx.slidingpanelayout.*;
import androidx.swiperefreshlayout.*;
import androidx.versionedparcelable.*;
import androidx.viewpager.*;
import com.google.android.datatransport.*;
import com.google.android.datatransport.backend.cct.*;
import com.google.android.datatransport.runtime.*;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.odml.image.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.*;
import com.google.mlkit.common.*;
import com.google.mlkit.vision.common.*;
import com.google.mlkit.vision.interfaces.*;
import com.google.mlkit.vision.text.bundled.common.*;
import com.google.mlkit.vision.text.bundled.latin.*;
import com.megh.notepad.databinding.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.*;
import org.json.*;

public class MainActivity extends AppCompatActivity {
	
	private Timer _timer = new Timer();
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private MainBinding binding;
	private String timeStamp = "";
	
	private Intent i = new Intent();
	private TimerTask timer;
	private DatabaseReference data = _firebase.getReference("db");
	private ChildEventListener _data_child_listener;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		binding = MainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		setSupportActionBar(binding.Toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		binding.Toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _v) {
				onBackPressed();
			}
		});
		
		_data_child_listener = new ChildEventListener() {
			@Override
			public void onChildAdded(DataSnapshot _param1, String _param2) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onChildChanged(DataSnapshot _param1, String _param2) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onChildMoved(DataSnapshot _param1, String _param2) {
				
			}
			
			@Override
			public void onChildRemoved(DataSnapshot _param1) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onCancelled(DatabaseError _param1) {
				final int _errorCode = _param1.getCode();
				final String _errorMessage = _param1.getMessage();
				
			}
		};
		data.addChildEventListener(_data_child_listener);
	}
	
	private void initializeLogic() {
		
		getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
		android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		// আপনার লেআউটের নাম দিন (main বা activity_splash)
		setContentView(R.layout.main); 
		
		// ২. ভিউ ফাইন্ড করা
		final android.widget.RelativeLayout rootLayout = findViewById(R.id.splash_root_layout);
		androidx.cardview.widget.CardView cardView = findViewById(R.id.splash_card_view);
		android.widget.ImageView logoIcon = findViewById(R.id.splash_logo);
		android.widget.TextView appNameText = findViewById(R.id.splash_app_name);
		android.widget.TextView subtitleText = findViewById(R.id.splash_subtitle);
		android.widget.ProgressBar progressBar = findViewById(R.id.splash_progress_bar);
		android.widget.TextView versionText = findViewById(R.id.splash_version);
		android.widget.TextView developerText = findViewById(R.id.splash_developer);
		
		// ==========================================
		// 🎨 ৩. ThemeHelper দিয়ে কালার ও ফন্ট ম্যাজিক 🎨
		// ==========================================
		try {
			// ভার্সন কোড সেট করা
			android.content.pm.PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			versionText.setText("Version " + pInfo.versionName);
			
			// থিম অনুযায়ী ব্যাকগ্রাউন্ড ও কার্ডের কালার
			rootLayout.setBackgroundColor(ThemeHelper.getBgColor(this));
			cardView.setCardBackgroundColor(ThemeHelper.getSurfaceColor(this));
			logoIcon.setColorFilter(ThemeHelper.getAccentColor(this), android.graphics.PorterDuff.Mode.SRC_IN);
			
			// টেক্সট কালার
			appNameText.setTextColor(ThemeHelper.getPrimaryTextColor(this));
			subtitleText.setTextColor(ThemeHelper.getAccentColor(this)); // মেঘবালিকা
			versionText.setTextColor(ThemeHelper.getSecondaryTextColor(this));
			developerText.setTextColor(ThemeHelper.getAccentColor(this)); // Megh
			
			// প্রোগ্রেস বার কালার
			progressBar.getIndeterminateDrawable().setColorFilter(
			ThemeHelper.getAccentColor(this), android.graphics.PorterDuff.Mode.SRC_IN
			);
			
			// 🔤 পুরো পেজের ফন্ট এক ক্লিকে চেঞ্জ
			ThemeHelper.applyFontToAllViews(this, rootLayout);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// ==========================================
		// 🌟 ৪. স্প্ল্যাশ টাইমার + সিগনেচার বটমশীট ম্যাজিক 🌟
		// ==========================================
		new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
			@Override
			public void run() {
				final android.content.SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
				String savedName = sharedPreferences.getString("UserName", "");
				
				if (savedName.isEmpty()) {
					// 🚨 সিগনেচার বটমশীট (ThemeHelper অনুযায়ী) 🚨
					final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
					new com.google.android.material.bottomsheet.BottomSheetDialog(MainActivity.this);
					
					android.widget.LinearLayout bottomSheetLayout = new android.widget.LinearLayout(MainActivity.this);
					bottomSheetLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
					bottomSheetLayout.setPadding(60, 60, 60, 80);
					
					// বটমশীটের ব্যাকগ্রাউন্ড (Surface Color) এবং রাউন্ডেড কর্নার
					android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
					shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
					shape.setColor(ThemeHelper.getSurfaceColor(MainActivity.this)); // Surface কালার
					shape.setCornerRadii(new float[] { 60, 60, 60, 60, 0, 0, 0, 0 }); 
					bottomSheetLayout.setBackground(shape);
					
					// টাইটেল
					android.widget.TextView title = new android.widget.TextView(MainActivity.this);
					title.setText("মেঘবালিকা ইজ কুইন 😁");
					title.setTextSize(22);
					title.setTextColor(ThemeHelper.getPrimaryTextColor(MainActivity.this));
					title.setTypeface(null, android.graphics.Typeface.BOLD);
					bottomSheetLayout.addView(title);
					
					// মেসেজ
					android.widget.TextView message = new android.widget.TextView(MainActivity.this);
					message.setText("আপনার নাম লিখুন মেঘবালিকা 😊");
					message.setTextSize(15);
					message.setTextColor(ThemeHelper.getSecondaryTextColor(MainActivity.this));
					message.setPadding(0, 10, 0, 40);
					bottomSheetLayout.addView(message);
					
					// ইংরেজি ইনপুট
					final android.widget.EditText inputEn = new android.widget.EditText(MainActivity.this);
					inputEn.setHint("Your Name (English) - Required");
					inputEn.setTextColor(ThemeHelper.getPrimaryTextColor(MainActivity.this));
					inputEn.setHintTextColor(ThemeHelper.getSecondaryTextColor(MainActivity.this));
					inputEn.setSingleLine(true);
					android.widget.LinearLayout.LayoutParams enParams = new android.widget.LinearLayout.LayoutParams(
					android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
					enParams.setMargins(0, 0, 0, 30);
					inputEn.setLayoutParams(enParams);
					bottomSheetLayout.addView(inputEn);
					
					// বাংলা ইনপুট
					final android.widget.EditText inputBn = new android.widget.EditText(MainActivity.this);
					inputBn.setHint("আপনার নাম (বাংলায়)");
					inputBn.setTextColor(ThemeHelper.getPrimaryTextColor(MainActivity.this));
					inputBn.setHintTextColor(ThemeHelper.getSecondaryTextColor(MainActivity.this));
					inputBn.setSingleLine(true);
					android.widget.LinearLayout.LayoutParams bnParams = new android.widget.LinearLayout.LayoutParams(
					android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
					bnParams.setMargins(0, 0, 0, 50);
					inputBn.setLayoutParams(bnParams);
					bottomSheetLayout.addView(inputBn);
					
					// প্রিমিয়াম Save বাটন
					android.widget.Button saveButton = new android.widget.Button(MainActivity.this);
					saveButton.setText("Save & Continue");
					saveButton.setTextColor(android.graphics.Color.WHITE); // বাটনের লেখা সাদাই ভালো লাগে
					saveButton.setAllCaps(false);
					saveButton.setTextSize(16);
					
					android.graphics.drawable.GradientDrawable btnShape = new android.graphics.drawable.GradientDrawable();
					btnShape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
					btnShape.setColor(ThemeHelper.getAccentColor(MainActivity.this)); // Accent Color বাটন
					btnShape.setCornerRadius(30); 
					saveButton.setBackground(btnShape);
					
					bottomSheetLayout.addView(saveButton);
					
					// 🔤 বটমশীটের ফন্টও চেঞ্জ করে দেওয়া হলো 🔤
					ThemeHelper.applyFontToAllViews(MainActivity.this, bottomSheetLayout);
					
					bottomSheetDialog.setContentView(bottomSheetLayout);
					bottomSheetDialog.setCancelable(false); 
					
					// বাটন ক্লিক লিসেনার
					saveButton.setOnClickListener(new android.view.View.OnClickListener() {
						@Override
						public void onClick(android.view.View v) {
							String userNameEn = inputEn.getText().toString().trim();
							String userNameBn = inputBn.getText().toString().trim();
							
							if (!userNameEn.isEmpty() && !userNameBn.isEmpty()) {
								sharedPreferences.edit().putString("UserName", userNameEn).apply();
								sharedPreferences.edit().putString("UserNameBangla", userNameBn).apply();
								
								getSharedPreferences("AppSettings", MODE_PRIVATE).edit()
								.putString("author_name_en", userNameEn)
								.putString("author_name", userNameBn)
								.apply();
								
								android.widget.Toast.makeText(getApplicationContext(), "নাম সেভ হয়েছে!", android.widget.Toast.LENGTH_SHORT).show();
								bottomSheetDialog.dismiss();
								
								// সার্ভিস চালু
								android.content.Intent serviceIntent = new android.content.Intent(MainActivity.this, BackgroundService.class);
								if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
									startForegroundService(serviceIntent);
								} else {
									startService(serviceIntent);
								}
								
								// MmmActivity তে যাওয়া
								android.content.Intent i = new android.content.Intent(MainActivity.this, MmmActivity.class);
								startActivity(i);
								finish();
							} else {
								android.widget.Toast.makeText(getApplicationContext(), "দয়া করে দুটি নামই দিন!", android.widget.Toast.LENGTH_SHORT).show();
							}
						}
					});
					
					bottomSheetDialog.show();
					
				} else {
					// 🚨 নাম থাকলে সরাসরি ড্যাশবোর্ডে 🚨
					android.content.Intent serviceIntent = new android.content.Intent(MainActivity.this, BackgroundService.class);
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
						startForegroundService(serviceIntent);
					} else {
						startService(serviceIntent);
					}
					
					android.content.Intent i = new android.content.Intent(MainActivity.this, MmmActivity.class);
					startActivity(i);
					finish();
				}
			}
		}, 1500);
		
		
	}
	
}