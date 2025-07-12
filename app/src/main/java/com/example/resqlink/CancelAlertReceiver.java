package com.example.resqlink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CancelAlertReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("CANCEL_ALERT".equals(intent.getAction())) {
            CrashDetectionService.isCancelled = true;
            Log.d("CrashDetection", "User cancelled the emergency alert.");
        }
    }
}
