package com.megh.notepad;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class ConvertActivity extends AppCompatActivity {

    private EditText etInputText, etOutputText;
    private TextView btnConvertAction, btnClearAll;
    private ImageView btnConvBack, btnInPaste, btnInCopy, btnInClear, btnOutCopy, btnOutClear;

    private HashMap<String, String> charToMerosaMap = new HashMap<>();
    private HashMap<String, String> merosaToCharMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.convert);

        initViews();
        initMerosaDictionary();
        applyThemeColors();
        setupListeners();
    }

    private void initViews() {
        etInputText = findViewById(R.id.etInputText);
        etOutputText = findViewById(R.id.etOutputText);
        
        etInputText.setMovementMethod(new ScrollingMovementMethod());
        etOutputText.setMovementMethod(new ScrollingMovementMethod());
        
        btnConvertAction = findViewById(R.id.btnConvertAction);
        btnClearAll = findViewById(R.id.btnClearAll);
        
        btnConvBack = findViewById(R.id.btnConvBack);
        btnInPaste = findViewById(R.id.btnInPaste);
        btnInCopy = findViewById(R.id.btnInCopy);
        btnInClear = findViewById(R.id.btnInClear);
        btnOutCopy = findViewById(R.id.btnOutCopy);
        btnOutClear = findViewById(R.id.btnOutClear);
    }

    private void initMerosaDictionary() {
        charToMerosaMap.put("A", "-/_");    charToMerosaMap.put("N", "_/-_/_/");
        charToMerosaMap.put("B", "_/-");    charToMerosaMap.put("O", "/__/");
        charToMerosaMap.put("C", "/_-");    charToMerosaMap.put("P", "-/_/_");
        charToMerosaMap.put("D", "-/_/");   charToMerosaMap.put("Q", "_//-/");
        charToMerosaMap.put("E", "_/_/");   charToMerosaMap.put("R", "/_/__");
        charToMerosaMap.put("F", "/_-/");   charToMerosaMap.put("S", "-//-");
        charToMerosaMap.put("G", "-/__/");  charToMerosaMap.put("T", "/-_/");
        charToMerosaMap.put("H", "-/-/-");  charToMerosaMap.put("U", "_/_/_/");
        charToMerosaMap.put("I", "/_/");    charToMerosaMap.put("V", "/-__/");
        charToMerosaMap.put("J", "-/-/_/"); charToMerosaMap.put("W", "-/_/__");
        charToMerosaMap.put("K", "_/-/");   charToMerosaMap.put("X", "_/_/_/_");
        charToMerosaMap.put("L", "/_-/-");  charToMerosaMap.put("Y", "/_/-/__");
        charToMerosaMap.put("M", "-/___");  charToMerosaMap.put("Z", "-/__/_");

        charToMerosaMap.put("0", "/___/");  charToMerosaMap.put("5", "_-/-_/");
        charToMerosaMap.put("1", "-/-_");   charToMerosaMap.put("6", "/__/__");
        charToMerosaMap.put("2", "_/-/_");  charToMerosaMap.put("7", "-/_/__/");
        charToMerosaMap.put("3", "/_-_");   charToMerosaMap.put("8", "_/__/__");
        charToMerosaMap.put("4", "-/_-/__"); charToMerosaMap.put("9", "/_/__/");

        charToMerosaMap.put(" ", "__");
        charToMerosaMap.put(".", "-/_/-");
        charToMerosaMap.put(",", "/-/_/");
        charToMerosaMap.put("'", "-/--/");

        for (Map.Entry<String, String> entry : charToMerosaMap.entrySet()) {
            merosaToCharMap.put(entry.getValue(), entry.getKey());
        }
    }

    private void applyThemeColors() {
        int bgColor = ThemeHelper.getBgColor(this);
        int surfaceColor = ThemeHelper.getSurfaceColor(this);
        int accentColor = ThemeHelper.getAccentColor(this);
        int primaryTextColor = ThemeHelper.getPrimaryTextColor(this);
        int secondaryTextColor = ThemeHelper.getSecondaryTextColor(this);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(surfaceColor);
        }

        findViewById(R.id.rootConverter).setBackgroundColor(bgColor);
        findViewById(R.id.toolbarConverter).setBackgroundColor(surfaceColor);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(surfaceColor);
        cardBg.setCornerRadius(32f);
        findViewById(R.id.cardInput).setBackground(cardBg);
        findViewById(R.id.cardOutput).setBackground(cardBg);

        GradientDrawable btnConvBg = new GradientDrawable();
        btnConvBg.setColor(accentColor);
        btnConvBg.setCornerRadius(100f);
        btnConvertAction.setBackground(btnConvBg);
        btnConvertAction.setTextColor(Color.WHITE);

        GradientDrawable btnClearBg = new GradientDrawable();
        btnClearBg.setColor(Color.parseColor("#E53935"));
        btnClearBg.setCornerRadius(100f);
        btnClearAll.setBackground(btnClearBg);
        btnClearAll.setTextColor(Color.WHITE);

        ((TextView) findViewById(R.id.tvConvTitle)).setTextColor(primaryTextColor);
        ((TextView) findViewById(R.id.tvInputLabel)).setTextColor(accentColor);
        ((TextView) findViewById(R.id.tvOutputLabel)).setTextColor(accentColor);
        
        etInputText.setTextColor(primaryTextColor);
        etInputText.setHintTextColor(secondaryTextColor);
        etOutputText.setTextColor(primaryTextColor);
        etOutputText.setHintTextColor(secondaryTextColor);

        btnConvBack.setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN);
        btnInPaste.setColorFilter(secondaryTextColor, android.graphics.PorterDuff.Mode.SRC_IN);
        btnInCopy.setColorFilter(secondaryTextColor, android.graphics.PorterDuff.Mode.SRC_IN);
        btnOutCopy.setColorFilter(secondaryTextColor, android.graphics.PorterDuff.Mode.SRC_IN);
        
        btnInClear.setColorFilter(Color.parseColor("#E53935"), android.graphics.PorterDuff.Mode.SRC_IN);
        btnOutClear.setColorFilter(Color.parseColor("#E53935"), android.graphics.PorterDuff.Mode.SRC_IN);
    }

    private void setupListeners() {
        btnConvBack.setOnClickListener(v -> finish());

        btnInPaste.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
                CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
                if (text != null) {
                    etInputText.setText(etInputText.getText().toString() + text.toString());
                    Toast.makeText(this, "পেস্ট করা হয়েছে", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnInCopy.setOnClickListener(v -> copyToClipboard(etInputText.getText().toString()));
        btnInClear.setOnClickListener(v -> etInputText.setText(""));
        btnOutCopy.setOnClickListener(v -> copyToClipboard(etOutputText.getText().toString()));
        btnOutClear.setOnClickListener(v -> etOutputText.setText(""));

        btnClearAll.setOnClickListener(v -> {
            etInputText.setText("");
            etOutputText.setText("");
            Toast.makeText(this, "সব মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show();
        });

        btnConvertAction.setOnClickListener(v -> {
            String rawInput = etInputText.getText().toString();
            String input = rawInput.replace("\r", "");
            
            if (input.trim().isEmpty()) {
                Toast.makeText(this, "আগে কিছু লিখুন!", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] testTokens = input.trim().split("\\s+");
            int matchCount = 0;
            for (String t : testTokens) {
                if (merosaToCharMap.containsKey(t)) {
                    matchCount++;
                }
            }

            boolean isPureSymbols = input.matches("^[\\-_/\\s\n]+$");
            boolean isMerosaCode = isPureSymbols || (matchCount > 0 && matchCount >= testTokens.length / 2);

            if (isMerosaCode) {
                String safeInput = input.replace("\n", " \n ");
                String[] tokens = safeInput.split(" ");
                StringBuilder decodedText = new StringBuilder();
                
                for (String token : tokens) {
                    if (token.isEmpty()) continue;
                    if (token.equals("\n")) {
                        decodedText.append("\n"); 
                    } else if (merosaToCharMap.containsKey(token)) {
                        decodedText.append(merosaToCharMap.get(token));
                    } else {
                        decodedText.append(token); 
                    }
                }
                
                // 🌟 এখানে স্মার্ট কেস (Smart Casing) অ্যাপ্লাই করা হচ্ছে 🌟
                String finalText = formatProperCase(decodedText.toString());
                
                etOutputText.setText(finalText);
                Toast.makeText(this, "টেক্সটে কনভার্ট করা হয়েছে!", Toast.LENGTH_SHORT).show();

            } else {
                String upperInput = input.toUpperCase();
                StringBuilder encodedText = new StringBuilder();
                
                for (char c : upperInput.toCharArray()) {
                    if (c == '\n') {
                        encodedText.append("\n"); 
                    } else {
                        String key = String.valueOf(c);
                        if (charToMerosaMap.containsKey(key)) {
                            encodedText.append(charToMerosaMap.get(key)).append(" ");
                        } else {
                            // যদি অক্ষরটি ম্যাপে না থাকে তবে যেমন আছে তেমনই বসিয়ে দেবে
                            encodedText.append(c).append(" "); 
                        }
                    }
                }
                etOutputText.setText(encodedText.toString().trim());
                Toast.makeText(this, "মেরোসা কোডে কনভার্ট করা হয়েছে!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==========================================
    // 🌟 Smart Sentence Case Formatter 🌟
    // ==========================================
    private String formatProperCase(String text) {
        if (text == null || text.isEmpty()) return "";
        
        // প্রথমে পুরো লেখাকে ছোট হাতের (lowercase) করে নেওয়া হচ্ছে
        text = text.toLowerCase(); 
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true; // প্রথম অক্ষর বড় হাতের হবে

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (capitalizeNext && Character.isLetter(c)) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false; // একবার বড় হাতের হয়ে গেলে বন্ধ করে দেবে
            } else {
                result.append(c);
            }

            // যদি ফুলস্টপ, প্রশ্নবোধক চিহ্ন, বিস্ময়সূচক চিহ্ন বা নতুন লাইন আসে, তবে পরের অক্ষরটি আবার বড় হাতের হবে
            if (c == '.' || c == '?' || c == '!' || c == '\n') {
                capitalizeNext = true;
            }
        }
        
        // একা বসে থাকা "i" কে বড় হাতের "I" করে দেওয়ার জন্য রেগুলার এক্সপ্রেশন
        String finalText = result.toString();
        finalText = finalText.replaceAll("\\b[iI]\\b", "I");
        
        return finalText;
    }

    private void copyToClipboard(String text) {
        if (text.isEmpty()) {
            Toast.makeText(this, "কপি করার মতো কিছু নেই!", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied Text", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "কপি করা হয়েছে!", Toast.LENGTH_SHORT).show();
    }
}
