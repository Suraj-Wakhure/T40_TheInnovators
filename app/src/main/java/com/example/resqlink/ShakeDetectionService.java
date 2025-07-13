package com.example.resqlink;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;

import android.Manifest;
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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class ShakeDetectionService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        shakeDetector = new ShakeDetector();
        shakeDetector.setOnShakeListener(count -> {
            if (count >= 3) {
                sendShakeSMSWithLocation();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int foregroundTypes = FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
                .setSmallIcon(R.drawable.ic_launcher_foreground)
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

    private void sendShakeSMSWithLocation() {
        String number = "7972408401";
        String baseMessage = "HELP! I'm in an emergency. ";

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
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
                        number,
                        finalMessage
                );
                if (alertManager.hasRequiredPermissions()) {
                    alertManager.triggerEmergencyAlert();
                }
            }
        }, getMainLooper());
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
