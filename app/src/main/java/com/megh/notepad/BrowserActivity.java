package com.megh.notepad;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class BrowserActivity extends AppCompatActivity {
    
    private WebView webView;
    private EditText etUrl;
    private ProgressBar progressBar;
    private LinearLayout topBar;
    
    // 🌟 ফুল-স্ক্রিন ভিডিও সাপোর্টের জন্য ভেরিয়েবল 🌟
    private FrameLayout customViewContainer;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    
    private int bgColor, surfaceColor, accentColor, primaryTextColor, secondaryTextColor;
    private Typeface currentTypeface = Typeface.DEFAULT;
    
    private String currentUrl = "";
    private String currentTitle = "";
    private boolean isDesktopMode = false;
    private boolean isFullScreenMode = false;
    
    private static final String HOME_URL = "https://www.google.com";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bgColor = ThemeHelper.getBgColor(this);
        surfaceColor = ThemeHelper.getSurfaceColor(this);
        accentColor = ThemeHelper.getAccentColor(this);
        primaryTextColor = ThemeHelper.getPrimaryTextColor(this);
        secondaryTextColor = ThemeHelper.getSecondaryTextColor(this);

        int fontStyle = getSharedPreferences("AppSettings", MODE_PRIVATE).getInt("font_style", 0);
        if (fontStyle == 3) {
            try { currentTypeface = Typeface.createFromAsset(getAssets(), "fonts/bangla.ttf"); } catch (Exception e) {}
        }

        FrameLayout rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(bgColor);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 🌟 টপ বার 🌟
        topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(surfaceColor);
        topBar.setPadding(24, 32, 24, 32);
        topBar.setElevation(8f);

        // ==========================================
        // 🌟 ফিক্স ১: হোম বাটনের বদলে ইমোজি আইকন ব্যবহার (সব ফোনে সাপোর্ট করবে) 🌟
        // ==========================================
        TextView btnHome = new TextView(this);
        btnHome.setText("🏠");
        btnHome.setTextSize(22f);
        btnHome.setGravity(Gravity.CENTER);
        btnHome.setPadding(16, 16, 16, 16);
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { webView.loadUrl(HOME_URL); }
        });
        topBar.addView(btnHome, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 🔍 স্মার্ট সার্চ/URL বক্স
        etUrl = new EditText(this);
        etUrl.setHint("Google এ খুঁজুন বা ঠিকানা দিন...");
        etUrl.setHintTextColor(secondaryTextColor);
        etUrl.setTextColor(primaryTextColor);
        etUrl.setTextSize(14f);
        etUrl.setSingleLine(true);
        etUrl.setImeOptions(EditorInfo.IME_ACTION_GO);
        etUrl.setPadding(40, 24, 40, 24);
        etUrl.setTypeface(currentTypeface);
        etUrl.setSelectAllOnFocus(true);
        
        GradientDrawable urlBg = new GradientDrawable();
        urlBg.setColor(bgColor);
        urlBg.setCornerRadius(100f);
        etUrl.setBackground(urlBg);

        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        urlParams.setMargins(16, 0, 16, 0);
        topBar.addView(etUrl, urlParams);

        etUrl.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    etUrl.setText(currentUrl);
                } else {
                    etUrl.setText(currentTitle != null && !currentTitle.isEmpty() ? currentTitle : currentUrl);
                }
            }
        });

        // ==========================================
        // 🌟 ফিক্স ২: থ্রি-ডট মেনুর জন্য কাস্টম টেক্সট আইকন 🌟
        // ==========================================
        TextView btnMenu = new TextView(this);
        btnMenu.setText("⋮"); // থ্রি ডট সিম্বল
        btnMenu.setTextColor(primaryTextColor);
        btnMenu.setTextSize(26f);
        btnMenu.setTypeface(null, Typeface.BOLD);
        btnMenu.setGravity(Gravity.CENTER);
        btnMenu.setPadding(24, 8, 24, 8);
        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showBrowserMenu(); }
        });
        topBar.addView(btnMenu, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mainLayout.addView(topBar);

        // ⏳ লোডিং প্রোগ্রেস বার
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.getProgressDrawable().setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
        mainLayout.addView(progressBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 6));

        // 🕸️ মেইন ওয়েব ভিউ
        webView = new WebView(this);
        mainLayout.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        rootFrame.addView(mainLayout);

        customViewContainer = new FrameLayout(this);
        customViewContainer.setBackgroundColor(Color.BLACK);
        customViewContainer.setVisibility(View.GONE);
        rootFrame.addView(customViewContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(rootFrame);

        // ==========================================
        // 🌟 ৩. প্রো-লেভেল ব্রাউজার সেটিংস (Link Bug Fixed) 🌟
        // ==========================================
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        // 🚨 ফিক্স ৩: মাল্টিপল উইন্ডো 'false' করা হলো। এতে সব লিংক জোর করে একই পেজে ওপেন হবে 🚨
        webSettings.setSupportMultipleWindows(false); 
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // 🚨 ফিক্স ৪: false রিটার্ন করা হলো, যাতে ওয়েবভিউ নিজেই লিংকগুলো হ্যান্ডেল করে
                return false; 
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                currentUrl = url;
                if (!etUrl.hasFocus()) etUrl.setText(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                currentUrl = url;
                currentTitle = view.getTitle();
                if (!etUrl.hasFocus()) etUrl.setText(currentTitle);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                }
            }

            // 🌟 ভিডিও ফুল-স্ক্রিন প্লেয়ার 🌟
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (mCustomView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                mCustomView = view;
                customViewContainer.addView(mCustomView);
                customViewContainer.setVisibility(View.VISIBLE);
                customViewCallback = callback;
                topBar.setVisibility(View.GONE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); 
            }

            @Override
            public void onHideCustomView() {
                if (mCustomView == null) return;
                customViewContainer.removeView(mCustomView);
                customViewContainer.setVisibility(View.GONE);
                mCustomView = null;
                customViewCallback.onCustomViewHidden();
                if (!isFullScreenMode) {
                    topBar.setVisibility(View.VISIBLE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
            }
        });

        String targetUrl = getIntent().getStringExtra("TARGET_URL");
        if (targetUrl != null && !targetUrl.isEmpty()) {
            loadQuery(targetUrl);
        } else {
            loadQuery(HOME_URL);
        }

        etUrl.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    loadQuery(etUrl.getText().toString().trim());
                    etUrl.clearFocus();
                    return true;
                }
                return false;
            }
        });
    }

    private void loadQuery(String query) {
        if (query.isEmpty()) return;
        
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etUrl.getWindowToken(), 0);

        if (query.startsWith("http://") || query.startsWith("https://")) {
            webView.loadUrl(query);
        } else if (query.contains(".") && !query.contains(" ")) {
            webView.loadUrl("https://" + query);
        } else {
            webView.loadUrl("https://www.google.com/search?q=" + query);
        }
    }

    private void showBrowserMenu() {
        final BottomSheetDialog menuSheet = new BottomSheetDialog(this);
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(surfaceColor);
        rootLayout.setPadding(0, 32, 0, 32);

        rootLayout.addView(createMenuItem("রিলোড করুন (Refresh)", android.R.drawable.ic_popup_sync, new View.OnClickListener() {
            @Override public void onClick(View v) { webView.reload(); menuSheet.dismiss(); }
        }));
        
        if (webView.canGoForward()) {
            rootLayout.addView(createMenuItem("সামনে যান (Forward)", android.R.drawable.ic_media_next, new View.OnClickListener() {
                @Override public void onClick(View v) { webView.goForward(); menuSheet.dismiss(); }
            }));
        }

        rootLayout.addView(createMenuItem(isDesktopMode ? "মোবাইল সাইট" : "ডেস্কটপ সাইট", android.R.drawable.ic_menu_mapmode, new View.OnClickListener() {
            @Override public void onClick(View v) {
                isDesktopMode = !isDesktopMode;
                String userAgent = webView.getSettings().getUserAgentString();
                if (isDesktopMode) {
                    webView.getSettings().setUserAgentString(userAgent.replace("Mobile", "eliboM").replace("Android", "diordnA"));
                } else {
                    webView.getSettings().setUserAgentString(userAgent.replace("eliboM", "Mobile").replace("diordnA", "Android"));
                }
                webView.reload();
                menuSheet.dismiss();
            }
        }));

        rootLayout.addView(createMenuItem(isFullScreenMode ? "স্বাভাবিক স্ক্রিন" : "ফুল স্ক্রিন (Hide Bar)", android.R.drawable.ic_menu_crop, new View.OnClickListener() {
            @Override public void onClick(View v) {
                isFullScreenMode = !isFullScreenMode;
                if (isFullScreenMode) {
                    topBar.setVisibility(View.GONE);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    Toast.makeText(BrowserActivity.this, "স্ক্রিনের নিচ থেকে সোয়াইপ করে ব্যাক বাটন আনতে পারবেন", Toast.LENGTH_SHORT).show();
                } else {
                    topBar.setVisibility(View.VISIBLE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
                menuSheet.dismiss();
            }
        }));

        rootLayout.addView(createMenuItem("লিংক কপি করুন", android.R.drawable.ic_menu_share, new View.OnClickListener() {
            @Override public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("URL", currentUrl);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(BrowserActivity.this, "লিংক কপি হয়েছে!", Toast.LENGTH_SHORT).show();
                menuSheet.dismiss();
            }
        }));

        rootLayout.addView(createMenuItem("Google Chrome-এ খুলুন", android.R.drawable.ic_menu_set_as, new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl));
                startActivity(browserIntent);
                menuSheet.dismiss();
            }
        }));

        menuSheet.setContentView(rootLayout);
        menuSheet.show();
    }

    private LinearLayout createMenuItem(String title, int iconRes, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(64, 40, 64, 40);
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
        text.setPadding(40, 0, 0, 0);
        item.addView(text);

        return item;
    }

    @Override
    public void onBackPressed() {
        if (mCustomView != null) {
            customViewCallback.onCustomViewHidden();
            return;
        }
        if (isFullScreenMode) {
            isFullScreenMode = false;
            topBar.setVisibility(View.VISIBLE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            return;
        }
        if (webView.canGoBack()) {
            webView.goBack();
        } 
        else {
            super.onBackPressed();
        }
    }
}
