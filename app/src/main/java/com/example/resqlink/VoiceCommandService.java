package com.example.resqlink;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class VoiceCommandService extends Service {
    private static final String CHANNEL_ID = "VoiceCommandServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private PowerManager.WakeLock wakeLock;
    private String presetContactNumber;
    private SharedPreferences sharedPreferences;
    private boolean isListening = false;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        presetContactNumber = sharedPreferences.getString("preset_contact", "");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech Recognition not available", Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        initializeSpeechRecognizer();
        setupWakeLock();
        startListening();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Voice Command Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) { isListening = true; }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { isListening = false; }

            @Override
            public void onError(int error) {
                isListening = false;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (!isListening) startListening();
                }, 1000);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    processVoiceCommand(matches.get(0));
                }
                startListening();
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
    }

    private void setupWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VoiceCommandService::WakeLock");
            wakeLock.acquire(10 * 60 * 1000L);
        }
    }

    private void startListening() {
        if (speechRecognizer != null && !isListening) {
            try {
                speechRecognizer.startListening(recognizerIntent);
                isListening = true;
            } catch (SecurityException e) {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processVoiceCommand(String command) {
        if (command == null) return;

        String lowerCommand = command.toLowerCase();
        if (lowerCommand.contains("send sms") || lowerCommand.contains("text message") ||
                lowerCommand.contains("send text") || lowerCommand.contains("send message")) {

            String message = extractMessageFromCommand(command);
            if (message != null && !message.isEmpty()) {
                if (presetContactNumber == null || presetContactNumber.isEmpty()) {
                    Toast.makeText(this, "No contact number set", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (checkSmsPermission()) {
                    appendLocationAndSendSMS(presetContactNumber, message);
                } else {
                    Toast.makeText(this, "SMS permission required", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String extractMessageFromCommand(String command) {
        String[] triggerPhrases = {"send sms", "text message", "send text", "send message"};
        for (String phrase : triggerPhrases) {
            if (command.toLowerCase().contains(phrase)) {
                String[] parts = command.split(phrase, 2);
                if (parts.length > 1) {
                    return parts[1].trim();
                }
            }
        }
        return command.trim();
    }

    private boolean checkSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void appendLocationAndSendSMS(String phoneNumber, String baseMessage) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    String finalMessage = baseMessage;
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();
                        String locationUrl = "https://www.google.com/maps?q=" + lat + "," + lng;
                        finalMessage += "\n\n\uD83D\uDCCD My Live Location:\n" + locationUrl +
                                "\n(Lat: " + lat + ", Long: " + lng + ")";
                    } else {
                        finalMessage += "\n\n\uD83D\uDCCD Location not available (please enable GPS)";
                    }
                    sendSMS(phoneNumber, finalMessage);
                })
                .addOnFailureListener(e -> {
                    sendSMS(phoneNumber, baseMessage + "\n\n\uD83D\uDCCD Failed to get location.");
                });
    }

    private void sendSMS(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            showSMSNotification(phoneNumber, message);
            Toast.makeText(this, "SMS sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void showSMSNotification(String phoneNumber, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("SMS Sent Successfully")
                .setContentText("To: " + phoneNumber)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Message: " + message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID + 1, builder.build());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Voice Command Service")
                .setContentText("Listening for SMS commands...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
