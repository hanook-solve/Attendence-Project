package com.example.attendencebeta;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.attendencebeta.Admin.AdminHomeFragment;
import com.example.attendencebeta.User.CalendarFragment;
import com.example.attendencebeta.User.ProfileFragment;
import com.example.attendencebeta.User.UserHomeFragment;
import com.example.attendencebeta.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (savedInstanceState == null) {
            loadStartFragment();
        }
        setupBottomNav();

    }
    private void setupBottomNav() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String role = prefs.getString("role", "teacher");

        // Inflate different menu based on role
        if ("admin".equals(role)) {
            binding.bottomNav.inflateMenu(R.menu.bottom_nav_admin);
        } else {
            binding.bottomNav.inflateMenu(R.menu.bottom_nav_teacher);
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            int id = item.getItemId();

            // Teacher tabs
            if (id == R.id.nav_home)    selected = new UserHomeFragment();
            if (id == R.id.nav_history) selected = new CalendarFragment();
            if (id == R.id.nav_profile) selected = new ProfileFragment();

            // Admin tabs
            if (id == R.id.nav_today)    selected = new AdminHomeFragment();
            //if (id == R.id.nav_settings) selected = new AdminSettingsFragment();

            if (selected != null) {
                navigateTo(selected, false); // false = don't add to back stack for bottom nav
            }
            return true;
        });
    }
    // Add this method to MainActivity
    private void loadStartFragment() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String role = prefs.getString("role", "teacher");

        Fragment startFragment = "admin".equals(role)
                ? new AdminHomeFragment()   // create this when ready
                : new UserHomeFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, startFragment)
                .commit();
    }
    public void navigateTo(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction tx = getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragmentContainer, fragment);
        if (addToBackStack) tx.addToBackStack(null);
        tx.commit();
    }
}