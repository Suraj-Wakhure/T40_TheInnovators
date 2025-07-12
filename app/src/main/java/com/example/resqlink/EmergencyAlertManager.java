package com.example.resqlink;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
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
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void triggerEmergencyAlert() {
        // First get location if possible
        String finalMessage = emergencyMessage;

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    finalMessage += "https://www.google.com/maps?q="
                    + location.getLatitude() + "," + location.getLongitude() +
                            "\nLat: " + String.format("%.6f", location.getLatitude()) +
                            ", Long: " + String.format("%.6f", location.getLongitude());
                }
            }
        } catch (Exception e) {
            finalMessage += "\nLocation unavailable";
        }

        // Then send SMS
        try {
            // Validate phone number
            if (emergencyContactNumber == null || emergencyContactNumber.trim().isEmpty()) {
                Toast.makeText(context, "Emergency contact number not set", Toast.LENGTH_LONG).show();
                return;
            }

            // Check SMS permission
            if (!hasRequiredPermissions()) {
                Toast.makeText(context, "Required permissions not granted", Toast.LENGTH_LONG).show();
                return;
            }

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(emergencyContactNumber, null, finalMessage, null, null);
            Toast.makeText(context, "Emergency alert sent to " + emergencyContactNumber, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(context, "Failed to send alert: " + e.toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}