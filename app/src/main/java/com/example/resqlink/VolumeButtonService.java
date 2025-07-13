package com.example.resqlink;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SmsManager;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class VolumeButtonService extends Service {

    private MediaSessionCompat mediaSession;
    private long lastVolumeUpTime = 0;
    private long lastVolumeDownTime = 0;
    private int upCount = 0;
    private int downCount = 0;
    private Handler resetHandler = new Handler(Looper.getMainLooper());
    private Runnable resetRunnable = this::resetCounts;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initMediaSession();
        startForeground(1, createNotification());
    }

    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, "VolumeService");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
                KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                    int keyCode = event.getKeyCode();

                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        upCount++;
                        Toast.makeText(getApplicationContext(), "Volume UP: " + upCount, Toast.LENGTH_SHORT).show();
                    }

                    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                        downCount++;
                        Toast.makeText(getApplicationContext(), "Volume DOWN: " + downCount, Toast.LENGTH_SHORT).show();
                    }

                    resetHandler.removeCallbacks(resetRunnable);
                    resetHandler.postDelayed(resetRunnable, 3000); // 3 sec

                    if (upCount >= 2 && downCount >= 2) {
                        triggerEmergencySmsWithLocation();
                        resetCounts();
                    }
                }
                return super.onMediaButtonEvent(mediaButtonIntent);
            }
        });
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setActive(true);
    }

    private void resetCounts() {
        upCount = 0;
        downCount = 0;
    }

    private void triggerEmergencySmsWithLocation() {
        String phoneNumber = "7972408401";
        String baseMessage = "HELP! I'm in an emergency. ";

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission missing", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission missing", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)
                .setFastestInterval(500)
                .setNumUpdates(1);

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                fusedLocationClient.removeLocationUpdates(this);

                String finalMessage = baseMessage;

                if (locationResult != null && locationResult.getLastLocation() != null) {
                    double lat = locationResult.getLastLocation().getLatitude();
                    double lng = locationResult.getLastLocation().getLongitude();
                    String locationLink = "https://www.google.com/maps?q=" + lat + "," + lng;
                    finalMessage += "Location: " + locationLink;
                } else {
                    finalMessage += "Location unavailable.";
                }

                EmergencyAlertManager alertManager = new EmergencyAlertManager(
                        getApplicationContext(),
                        phoneNumber,
                        finalMessage
                );
                if (alertManager.hasRequiredPermissions()) {
                    alertManager.triggerEmergencyAlert();
                }
                Toast.makeText(VolumeButtonService.this, "Emergency SMS Sent!", Toast.LENGTH_SHORT).show();
            }
        }, getMainLooper());
    }

    private Notification createNotification() {
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel("emergency_channel", "Emergency Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, "emergency_channel")
                .setContentTitle("Emergency Volume Button Listener")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
