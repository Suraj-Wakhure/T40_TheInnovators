package com.example.resqlink;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class EmergencyCountdownActivity extends Activity {

    private TextView countdownText;
    private Button cancelButton;
    private CountDownTimer countDownTimer;

    private final String emergencyNumber = "7972408401";
    private final String message = "🚨 Crash detected! Please send help. Location: <Add location if needed>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_countdown);

        countdownText = findViewById(R.id.countdown_text);
        cancelButton = findViewById(R.id.cancel_button);

        CrashDetectionService.isCancelled = false; // reset

        countDownTimer = new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
                countdownText.setText("Sending alert in " + (millisUntilFinished / 1000) + " seconds...");
            }

            public void onFinish() {
                if (!CrashDetectionService.isCancelled) {
                    sendSMS();
                }
                finish(); // close activity
            }
        }.start();

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrashDetectionService.isCancelled = true;
                countDownTimer.cancel();
                finish(); // close activity
            }
        });
    }

    private void sendSMS() {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(emergencyNumber, null, message, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
