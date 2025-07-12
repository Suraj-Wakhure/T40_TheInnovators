package com.example.resqlink;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SmsManager;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;



public class VolumeButtonService extends Service {

    private MediaSessionCompat mediaSession;
    private long lastVolumeUpTime = 0;
    private long lastVolumeDownTime = 0;
    private int upCount = 0;
    private int downCount = 0;
    private Handler resetHandler = new Handler(Looper.getMainLooper());
    private Runnable resetRunnable = this::resetCounts;

    @Override
    public void onCreate() {
        super.onCreate();
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

                    // Start/reset timer to reset counts after 3 seconds
                    resetHandler.removeCallbacks(resetRunnable);
                    resetHandler.postDelayed(resetRunnable, 3000); // 3 seconds

                    // Trigger if both pressed twice
                    if (upCount >= 2 && downCount >= 2) {
                        triggerEmergencySms();
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

    private void triggerEmergencySms() {
       /* String phoneNumber = "7972408401"; // Replace with actual contact
        String message = "Emergency! I need help. This is an automated alert.";

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        */
        EmergencyAlertManager alertManager = new EmergencyAlertManager(
                getApplicationContext(),
                "7972408401",
                "HELP! I'm in an emergency. My location is being shared."
        );
        if (alertManager.hasRequiredPermissions()) {
            alertManager.triggerEmergencyAlert();
        }
        Toast.makeText(this, "Emergency SMS Sent!", Toast.LENGTH_SHORT).show();
    }

    private Notification createNotification() {
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel("emergency_channel", "Emergency Service", NotificationManager.IMPORTANCE_LOW);
        }
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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


