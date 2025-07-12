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

public class LoginActivity extends AppCompatActivity {

    EditText loginUsername, loginPassword;
    Button loginBtn;
    TextView goToSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        loginUsername = findViewById(R.id.login_username);
        loginPassword = findViewById(R.id.login_password);
        loginBtn = findViewById(R.id.login_btn);
        goToSignup = findViewById(R.id.go_to_signup);

        // Dummy Login Check (can be replaced with Firebase or local DB)
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = loginUsername.getText().toString().trim();
                String password = loginPassword.getText().toString().trim();
                SharedPreferences sharedPreferences2 = getSharedPreferences("account_data_pref",MODE_PRIVATE);
                String actualusername = sharedPreferences2.getString("username","");
                String actualpassword = sharedPreferences2.getString("password","");

                if (username.equals(actualusername) && password.equals(actualpassword)) {
                    // Successful login
                    SharedPreferences sharedPreferences = getSharedPreferences("login_pref",MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("isloogedin",true);
                    editor.apply();
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    finish();
                } else {
                    // Invalid credentials
                    Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Redirect to Signup
        goToSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this,SignupActivity.class);
                startActivity(intent);
            }
        });
    }
}
