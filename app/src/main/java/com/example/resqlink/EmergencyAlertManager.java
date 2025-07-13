package com.example.resqlink;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

public class EmergencyAlertManager {
    private Context context;
    private String emergencyContactNumber;
    private String emergencyMessage;

    public EmergencyAlertManager(Context context, String contactNumber, String message) {
        this.context = context;
        this.emergencyContactNumber = contactNumber;
        this.emergencyMessage = message;
    }

    public boolean hasRequiredPermissions() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    public void triggerEmergencyAlert() {
        if (emergencyContactNumber == null || emergencyContactNumber.trim().isEmpty()) {
            Toast.makeText(context, "Emergency contact number not set", Toast.LENGTH_LONG).show();
            return;
        }

        if (!hasRequiredPermissions()) {
            Toast.makeText(context, "SEND_SMS permission not granted", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(emergencyContactNumber, null, emergencyMessage, null, null);
            Toast.makeText(context, "Emergency SMS sent to " + emergencyContactNumber, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Failed to send alert: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
