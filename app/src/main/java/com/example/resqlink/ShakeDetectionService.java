package com.example.resqlink;


import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
//import com.example.shakingfeature.ShakeDetector;


public class ShakeDetectionService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        shakeDetector = new ShakeDetector();
        shakeDetector.setOnShakeListener(count -> {
            if (count >= 3) {
                EmergencyAlertManager alertManager = new EmergencyAlertManager(
                        getApplicationContext(),
                        "7972408401",
                        "HELP! I'm in an emergency. My location is being shared."
                );
                if (alertManager.hasRequiredPermissions()) {
                    alertManager.triggerEmergencyAlert();
                }
            }
        });

    }

    // Update onStartCommand
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int foregroundTypes = FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;

            // Only add location type if permission granted
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                foregroundTypes |= FOREGROUND_SERVICE_TYPE_LOCATION;
            }

            startForeground(1, createNotification(), foregroundTypes);
        } else {
            startForeground(1, createNotification());
        }

        registerSensor();
        return START_STICKY;
    }

    private void registerSensor() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, "shake_channel")
                .setContentTitle("Shake Detection Active")
                .setContentText("Monitoring for emergency shakes")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Add your icon
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "shake_channel",
                    "Shake Detection",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        shakeDetector.onSensorChanged(event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public IBinder onBind(Intent intent) { return null; }
}