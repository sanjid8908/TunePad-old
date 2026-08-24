package com.megh.notepad;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class SecurityUtils {

    // 🌟 আপনার নিজস্ব ১৬ অক্ষরের গোপন চাবি (যেকোনো কিছু দিতে পারেন, তবে ঠিক ১৬ অক্ষর হতে হবে) 🌟
    private static final String MY_SECRET_KEY = "MeghSecretKey123"; 
    private static final String ALGORITHM = "AES";

    // সাধারণ লেখাকে হিজিবিজি কোডে রূপান্তর করা (ডাউনলোডের বা এক্সপোর্টের সময়)
    public static String encrypt(String plainText) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(MY_SECRET_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
            // Base64 দিয়ে স্ট্রিংয়ে কনভার্ট করে দিচ্ছি যেন ফাইলে সেভ করতে সুবিধা হয়
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // হিজিবিজি কোডকে আবার আসল লেখায় রূপান্তর করা (ওপেন বা ইমপোর্টের সময়)
    public static String decrypt(String encryptedText) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(MY_SECRET_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null; // পাসওয়ার্ড না মিললে বা ফাইল অন্য কোনো অ্যাপের হলে null আসবে
        }
    }
}
