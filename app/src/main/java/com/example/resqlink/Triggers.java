package com.example.resqlink;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;

public class Triggers extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String PREFS_NAME = "TriggerPrefs";

    private static String[] REQUIRED_PERMISSIONS;

    private SwitchMaterial switchShake, switchVoice, switchVolume, switchCrash;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECORD_AUDIO
            };
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_triggers);

        switchShake = findViewById(R.id.switchShake);
        switchVoice = findViewById(R.id.switchVoice);
        switchVolume = findViewById(R.id.switchVolume);
        switchCrash = findViewById(R.id.switchCrash);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        switchShake.setChecked(prefs.getBoolean("shake_enabled", false));
        switchVoice.setChecked(prefs.getBoolean("voice_enabled", false));
        switchVolume.setChecked(prefs.getBoolean("volume_enabled", false));
        switchCrash.setChecked(prefs.getBoolean("crash_enabled", false));

        if (checkPermissions()) {
            initializeSpeechRecognizer();
        } else {
            requestPermissions();
        }

        switchShake.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveToggleState("shake_enabled", isChecked);
            if (isChecked) {
                startShakeDetectionService();
            } else {
                stopService(new Intent(this, ShakeDetectionService.class));
            }
        });

        switchVoice.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveToggleState("voice_enabled", isChecked);
            if (isChecked) {
                startVoiceListening();
            } else {
                stopVoiceListening();
            }
        });

        switchVolume.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveToggleState("volume_enabled", isChecked);
            if (isChecked) {
                if (!isAccessibilityServiceEnabled()) {
                    Toast.makeText(this, "Enable Accessibility Service manually", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Volume trigger already active", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "To disable, please turn off Accessibility Service manually", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });

        switchCrash.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveToggleState("crash_enabled", isChecked);
            if (isChecked) {
                Intent intent = new Intent(this, CrashDetectionService.class);
                ContextCompat.startForegroundService(this, intent);
                Toast.makeText(this, "Crash detection enabled", Toast.LENGTH_SHORT).show();
            } else {
                stopService(new Intent(this, CrashDetectionService.class));
                Toast.makeText(this, "Crash detection disabled", Toast.LENGTH_SHORT).show();
            }
        });

        if (switchShake.isChecked()) startShakeDetectionService();
        if (switchVoice.isChecked()) startVoiceListening();
        if (switchCrash.isChecked()) {
            Intent intent = new Intent(this, CrashDetectionService.class);
            ContextCompat.startForegroundService(this, intent);
        }
    }

    private void saveToggleState(String key, boolean value) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onError(int error) {
                    if (isListening) startVoiceListening();
                }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String command = matches.get(0);
                        processVoiceCommand(command);
                    }
                    if (isListening) startVoiceListening();
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        } else {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void startVoiceListening() {
        if (speechRecognizer != null) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            try {
                speechRecognizer.startListening(intent);
                isListening = true;
                Toast.makeText(this, "Voice command ON", Toast.LENGTH_SHORT).show();
            } catch (SecurityException e) {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopVoiceListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            isListening = false;
            Toast.makeText(this, "Voice command OFF", Toast.LENGTH_SHORT).show();
        }
    }

    private void processVoiceCommand(String command) {
        if (command.toLowerCase().contains("send sms")) {
            String message = command.replaceFirst("(?i)send sms", "").trim();
            if (!message.isEmpty()) {
                sendSMS(message);
            } else {
                Toast.makeText(this, "Please include a message after 'send sms'", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendSMS(String message) {
        sendSMSWithLocation(message);
    }

    public void sendSMSWithLocation(String message) {
        String number = "7972408401";

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 102);
            return;
        }

        com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient =
                com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setNumUpdates(1);

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                fusedLocationClient.removeLocationUpdates(this);

                String finalMessage = message;

                if (locationResult != null && locationResult.getLastLocation() != null) {
                    double lat = locationResult.getLastLocation().getLatitude();
                    double lng = locationResult.getLastLocation().getLongitude();
                    String locationLink = "https://www.google.com/maps?q=" + lat + "," + lng;
                    finalMessage += "\nMy Location: " + locationLink;
                } else {
                    finalMessage += "\nLocation unavailable";
                }

                EmergencyAlertManager alertManager = new EmergencyAlertManager(
                        getApplicationContext(),
                        number,
                        finalMessage
                );

                if (alertManager.hasRequiredPermissions()) {
                    alertManager.triggerEmergencyAlert();
                    Toast.makeText(Triggers.this, "SMS sent to " + number, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Triggers.this, "Permissions not granted", Toast.LENGTH_SHORT).show();
                }
            }
        }, getMainLooper());
    }

    // ✅ For SHAKE Service to call:
    public static void sendShakeSMSWithLocation(AppCompatActivity activity) {
        activity.runOnUiThread(() -> {
            ((Triggers) activity).sendSMSWithLocation("HELP! I'm in an emergency.");
        });
    }

    private void startShakeDetectionService() {
        Intent intent = new Intent(this, ShakeDetectionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent);
        } else {
            startService(intent);
        }
    }

    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            if (granted) {
                initializeSpeechRecognizer();
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (am != null) {
            for (AccessibilityServiceInfo service : am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
                if (service.getId().contains("com.example.volumnbtnservice/.VolumeButtonService")) {
                    return true;
                }
            }
        }
        return false;
    }
}
