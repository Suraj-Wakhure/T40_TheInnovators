package com.example.resqlink;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import android.telephony.SmsManager;

public class EmergencyAccessibilityService extends AccessibilityService {

    private long volumeUpPressedTime = 0;
    private long volumeDownPressedTime = 0;
    private static final long LONG_PRESS_THRESHOLD = 2000; // 2 seconds

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                volumeDownPressedTime = System.currentTimeMillis();
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                long pressDuration = System.currentTimeMillis() - volumeDownPressedTime;

                if (pressDuration >= 5000) { // 5 seconds
                    triggerEmergencySMS();
                }
            }
        }
        return super.onKeyEvent(event);
    }


    private void triggerEmergencySMS() {
        String phoneNumber = "7972408401"; // Replace with actual number
        String message = "🚨 Emergency! I need help.";

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);

        Toast.makeText(this, "Emergency SMS Sent!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

    }

    @Override
    public void onInterrupt() {}

    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = getServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        setServiceInfo(info);

        Toast.makeText(this, "Emergency Accessibility Service Connected", Toast.LENGTH_SHORT).show();
    }


}
