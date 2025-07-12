package com.example.resqlink;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
public class Tutorial1 extends AppCompatActivity {

    private View dot1, dot2, dot3;
    private int currentStep = 0; // Tracks current tutorial step
    private final int TOTAL_STEPS = 3; // Total number of tutorial steps

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial1);

        // Initialize dots
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);

        // Initialize buttons
        Button nextButton = findViewById(R.id.next_button);
        Button skipButton = findViewById(R.id.skip_button);

        // Set click listeners
        nextButton.setOnClickListener(v -> {
            if (currentStep < TOTAL_STEPS - 1) {
                currentStep++;
                updateDots();
                updateTutorialContent();
            } else {
                finish(); // End tutorial on last step
            }
        });

        skipButton.setOnClickListener(v -> finish());
    }

    private void updateDots() {
        // Reset all dots to inactive first
        dot1.setBackgroundResource(R.drawable.dot_inactive);
        dot2.setBackgroundResource(R.drawable.dot_inactive);
        dot3.setBackgroundResource(R.drawable.dot_inactive);

        // Activate the current step dot
        switch (currentStep) {
            case 0:
                dot1.setBackgroundResource(R.drawable.dot_active);
                break;
            case 1:
                dot2.setBackgroundResource(R.drawable.dot_active);
                break;
            case 2:
                dot3.setBackgroundResource(R.drawable.dot_active);
                break;
        }
    }

    private void updateTutorialContent() {
        // Here you would update the tutorial content (text, images)
        // based on the currentStep value
        // Example:
        TextView title = findViewById(R.id.tutorial_title);
        TextView description = findViewById(R.id.tutorial_description);
        ImageView image = findViewById(R.id.tutorial_image);

        switch (currentStep) {
            case 0:
                title.setText("What is ResQlink? Why You Need It?");
                description.setText("We model insects a cycle to find herry run over.");
                image.setImageResource(R.drawable.tuto_1);
                break;
            case 1:
                title.setText("Getting Started – First-Time Setup");
                description.setText("Register or Login to your account.\n" +
                        "Set up your emergency contact.\n" +
                        "Allow necessary permissions.");
                image.setImageResource(R.drawable.tuto_2);
                break;
            case 2:
                title.setText("Using Triggers to Send Emergency Alert");
                description.setText("Press the SOS button.\n" +
                        "Say: “Send SMS I’m in danger”.\n" +
                        "Shake the phone or press volume buttons.");
                image.setImageResource(R.drawable.tuto_3);
                break;
        }
    }
}