package com.example.resqlink;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            SharedPreferences sharedPreferences = getSharedPreferences("login_pref",MODE_PRIVATE);
            Boolean isloggedin = sharedPreferences.getBoolean("isloggedin",false);

            new Handler().postDelayed(() -> {
               if(isloggedin){
                   startActivity(new Intent(MainActivity.this,LoginActivity.class));
                   finish();
               }
               else{
                   startActivity(new Intent(MainActivity.this,HomeActivity.class));
                   finish();
               }
            }, 3000); // 3-second splash screen


            return insets;
        });
    }
}