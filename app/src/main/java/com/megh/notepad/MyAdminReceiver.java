package com.megh.notepad;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MyAdminReceiver extends DeviceAdminReceiver {
    
    @Override
    public void onEnabled(Context context, Intent intent) {
        // ইউজার যখন পারমিশন অ্যালাউ করবে
        Toast.makeText(context, "System Secured. Protection Activated!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        // ইউজার যদি কোনোভাবে পারমিশন অফ করে দেয়
        Toast.makeText(context, "Warning: Protection Disabled!", Toast.LENGTH_SHORT).show();
    }
}
