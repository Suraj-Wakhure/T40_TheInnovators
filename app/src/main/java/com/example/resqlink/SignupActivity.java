package com.example.resqlink;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {

    EditText signupUsername, signupPassword;
    Button signupBtn;
    TextView goToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize views
        signupUsername = findViewById(R.id.signup_username);
        signupPassword = findViewById(R.id.signup_password);
        signupBtn = findViewById(R.id.signup_btn);
        goToLogin = findViewById(R.id.go_to_login);

        // Dummy Signup Logic (can be replaced with DB or Firebase)
        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = signupUsername.getText().toString().trim();
                String password = signupPassword.getText().toString().trim();

                if (!username.isEmpty() && !password.isEmpty()) {
                    // Sign-up success (in real app, save to DB or Firebase)
                    SharedPreferences sharedPreferences = getSharedPreferences("account_data_pref",MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("username",username);
                    editor.putString("password",password);
                    editor.apply();
                    Toast.makeText(SignupActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(SignupActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Redirect to Login
        goToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
}
