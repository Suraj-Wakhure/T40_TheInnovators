package com.example.resqlink;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.example.resqlink.R;
import androidx.fragment.app.Fragment;
import java.util.HashMap;
import java.util.Map;
import com.google.android.material.bottomnavigation.BottomNavigationView;



import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HomeActivity extends AppCompatActivity {
    BottomNavigationView bottomNav;
    FloatingActionButton aiFab;


    private final Map<Integer, Fragment> fragmentMap = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bottomNav = findViewById(R.id.bottom_navigation);
        aiFab = findViewById(R.id.ai_fab);


        setupBottomNav(); // call the method

        /*// Fragment navigation
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            switch (item.getItemId()) {
                case R.id.nav_home:
                    selectedFragment = new HomeFragment();
                    break;
                case R.id.nav_contact:
                    selectedFragment = new ContactFragment();
                    break;
                case R.id.nav_emergency:
                    selectedFragment = new NearbyFragment();
                    break;
                case R.id.nav_profile:
                    selectedFragment = new ProfileFragment();
                    break;
            }
            if (selectedFragment != null)
                getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, selectedFragment).commit();

            return true;
        });

         */

        // AI FAB click – Show popup animation or go to AI Activity
        aiFab.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, AiChatActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out); // You can define animations in res/anim
        });

        // Default fragment
        bottomNav.setSelectedItemId(R.id.nav_home);
    }
    private void setupBottomNav() {
        fragmentMap.put(R.id.nav_home, new HomeFragment());
        fragmentMap.put(R.id.nav_contact, new ContactFragment());
        fragmentMap.put(R.id.nav_emergency, new NearbyFragment());
        fragmentMap.put(R.id.nav_profile, new ProfileFragment());

        // Default fragment on launch
        loadFragment(fragmentMap.get(R.id.nav_home));

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = fragmentMap.get(item.getItemId());
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment) // replace with your fragment container ID
                .commit();
    }

}