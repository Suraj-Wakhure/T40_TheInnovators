package com.example.resqlink;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class CrashDetectionService extends Service implements SensorEventListener {



    private static final String CHANNEL_ID = "CrashAlertChannel";
    private static final int NOTIFICATION_ID = 101;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final float CRASH_THRESHOLD = 25.0f;
    private long lastMovementTime = 0;
    private boolean crashDetected = false;
    public static boolean isCancelled = false;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        Log.d("CrashDetection", "Service created");
    }

    private void showCrashAlertNotification() {
        createNotificationChannel();

        Intent fullScreenIntent = new Intent(this, EmergencyCountdownActivity.class);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this, 0, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("⚠️ Crash Detected")
                .setContentText("Preparing emergency response...")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.crash_icon))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL) // important for full-screen
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setOngoing(true);

        startForeground(NOTIFICATION_ID, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Crash Detection Running")
                .setContentText("Monitoring for crash events...")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        startForeground(NOTIFICATION_ID, builder.build());

        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float acceleration = (float) Math.sqrt(x * x + y * y + z * z);

        long currentTime = System.currentTimeMillis();

        if (acceleration > CRASH_THRESHOLD && !crashDetected) {
            Log.d("CrashDetection", "Crash force detected!");
            crashDetected = true;
            lastMovementTime = currentTime;

            new Handler().postDelayed(() -> {
                long now = System.currentTimeMillis();
                if ((now - lastMovementTime) >= 15000) {
                    triggerEmergencyAlert();
                } else {
                    crashDetected = false;
                }
            }, 15000);
        }

        if (acceleration < 2.0f && crashDetected) {
            lastMovementTime = currentTime;
        }
    }

    private void triggerEmergencyAlert() {
        Log.d("CrashDetection", "⚠️ EMERGENCY TRIGGER SEQUENCE INITIATED ⚠️");
        showCrashAlertNotification();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Crash Alert Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
