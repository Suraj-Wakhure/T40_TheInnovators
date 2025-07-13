package com.example.resqlink;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import android.telephony.SmsManager;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import android.Manifest;

public class EmergencyAccessibilityService extends AccessibilityService {

    private long volumeDownPressedTime = 0;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                volumeDownPressedTime = System.currentTimeMillis();
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                long pressDuration = System.currentTimeMillis() - volumeDownPressedTime;

                if (pressDuration >= 5000) { // 5 seconds
                    fetchLocationAndSendSMS();
                }
            }
        }
        return super.onKeyEvent(event);
    }

    private void fetchLocationAndSendSMS() {
        getLocationLinkWithTimeout(5000, new LocationResultCallback() {
            @Override
            public void onLocationResult(String locationLink) {
                String phoneNumber = "7972408401"; // Replace with actual number
                String message = "🚨 Emergency! I need help. Location: " + locationLink;

                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);

                Toast.makeText(EmergencyAccessibilityService.this, "Emergency SMS Sent!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getLocationLinkWithTimeout(long timeoutMillis, LocationResultCallback callback) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback.onLocationResult("Location permission missing!");
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)
                .setFastestInterval(500)
                .setNumUpdates(1);

        Handler handler = new Handler(Looper.getMainLooper());

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                fusedLocationClient.removeLocationUpdates(this);
                handler.removeCallbacksAndMessages(null); // cancel timeout
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    Location loc = locationResult.getLastLocation();
                    String link = "https://www.google.com/maps?q=" + loc.getLatitude() + "," + loc.getLongitude();
                    callback.onLocationResult(link);
                } else {
                    callback.onLocationResult("Location unavailable");
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        // Timeout fallback
        handler.postDelayed(() -> {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            callback.onLocationResult("Location request timed out!");
        }, timeoutMillis);
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

    public interface LocationResultCallback {
        void onLocationResult(String locationLink);
    }
}
