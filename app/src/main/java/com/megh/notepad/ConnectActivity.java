import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectActivity extends AppCompatActivity {

    // 🔴 আপনার হোস্টিংয়ের PHP ফাইলের লিংক 🔴
    private static final String API_URL = "https://shuvraafroj.info/TunePad/sync/sync_connect.php"; 

    // ব্যাকগ্রাউন্ডে কাজ করার জন্য ExecutorService
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // আপনার লেআউট সেট করবেন
        // setContentView(R.layout.activity_connect);
    }

    // ==========================================
    // 👑 ১. প্যারেন্ট হিসেবে টোকেন তৈরি করা 👑
    // ==========================================
    public void generateParentToken() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String userName = prefs.getString("UserName", "মেঘ"); 
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("action", "generate_token");
            jsonBody.put("device_id", deviceId);
            jsonBody.put("user_name", userName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // ব্যাকগ্রাউন্ড থ্রেডে নেটওয়ার্ক কল
        executor.execute(() -> {
            try {
                String responseStr = sendPostRequest(API_URL, jsonBody);
                
                // মেইন থ্রেডে UI আপডেট করা
                handler.post(() -> {
                    try {
                        JSONObject response = new JSONObject(responseStr);
                        if (response.getString("status").equals("success")) {
                            String familyToken = response.getString("token");
                            
                            prefs.edit().putString("family_token", familyToken)
                                        .putString("user_role", "parent").apply();

                            Toast.makeText(ConnectActivity.this, "প্যারেন্ট কোড: " + familyToken, Toast.LENGTH_LONG).show();
                            // generateQRCode(familyToken);
                        } else {
                            Toast.makeText(ConnectActivity.this, response.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(ConnectActivity.this, "সার্ভার এরর! ইন্টারনেট চেক করুন।", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ==========================================
    // 👶 ২. চাইল্ড হিসেবে স্ক্যান করে জয়েন করা 👶
    // ==========================================
    public void joinAsChild(String scannedToken) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String userName = prefs.getString("UserName", "মেঘবালিকা"); 
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("action", "join_family");
            jsonBody.put("device_id", deviceId);
            jsonBody.put("user_name", userName);
            jsonBody.put("family_token", scannedToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        executor.execute(() -> {
            try {
                String responseStr = sendPostRequest(API_URL, jsonBody);
                
                handler.post(() -> {
                    try {
                        JSONObject response = new JSONObject(responseStr);
                        if (response.getString("status").equals("success")) {
                            prefs.edit().putString("family_token", scannedToken)
                                        .putString("user_role", "child").apply();

                            Toast.makeText(ConnectActivity.this, "সফলভাবে প্যারেন্টের সাথে যুক্ত হয়েছেন!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ConnectActivity.this, response.getString("message"), Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(ConnectActivity.this, "সার্ভার এরর! ইন্টারনেট চেক করুন।", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ==========================================
    // 🌐 হেল্পার মেথড: পিওর নেটওয়ার্ক কানেকশন 🌐
    // ==========================================
    private String sendPostRequest(String apiUrl, JSONObject jsonBody) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true); // ডেটা পাঠানোর পারমিশন
        conn.setDoInput(true);  // ডেটা রিসিভ করার পারমিশন
        conn.setConnectTimeout(10000); // ১০ সেকেন্ড টাইমআউট
        conn.setReadTimeout(10000);

        // JSON ডেটা সার্ভারে পাঠানো
        DataOutputStream os = new DataOutputStream(conn.getOutputStream());
        os.writeBytes(jsonBody.toString());
        os.flush();
        os.close();

        // সার্ভার থেকে রেসপন্স পড়া
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        } else {
            throw new Exception("HTTP Error: " + responseCode);
        }
    }
}
