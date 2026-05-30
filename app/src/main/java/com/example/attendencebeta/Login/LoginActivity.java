package com.example.attendencebeta.Login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.attendencebeta.MainActivity;
import com.example.attendencebeta.R;
import com.example.attendencebeta.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;

    private String selectedRole = "teacher";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // If already logged in skip login screen entirely
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            goToMain();
            return;
        }

        setupRoleToggle();
        setupLoginButton();
        observeViewModel();

    }
    // ────────────────────────────────────────────────────────
    // ROLE TOGGLE
    // ────────────────────────────────────────────────────────
    private void setupRoleToggle() {

        // Teacher selected by default
        binding.toggleRole.check(R.id.btnTeacher);
        //updateRoleChip("teacher");

        binding.toggleRole.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            if (checkedId == R.id.btnTeacher) {
                selectedRole = "teacher";
            } else if (checkedId == R.id.btnAdmin) {
                selectedRole = "admin";
            }

            //updateRoleChip(selectedRole);
            clearError();
        });
    }

//    private void updateRoleChip(String role) {
//        if ("admin".equals(role)) {
//            binding.tvRoleChip.setText("● Signing in as Admin");
//        } else {
//            binding.tvRoleChip.setText("● Signing in as Teacher");
//        }
//    }

    // ────────────────────────────────────────────────────────
    // LOGIN BUTTON
    // ────────────────────────────────────────────────────────
    private void setupLoginButton() {
        binding.btnLogin.setOnClickListener(v -> {

            String email    = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (!validateInputs(email, password)) return;

            setLoading(true);
            viewModel.login(email, password, selectedRole);
        });
    }

    // ────────────────────────────────────────────────────────
    // VALIDATION
    // ────────────────────────────────────────────────────────
    private boolean validateInputs(String email, String password) {
        boolean valid = true;

        // Email check
        if (email.isEmpty()) {
            binding.tilEmail.setError("Email is required");
            valid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Enter a valid email");
            valid = false;
        } else {
            binding.tilEmail.setError(null);
        }

        // Password check
        if (password.isEmpty()) {
            binding.tilPassword.setError("Password is required");
            valid = false;
        } else if (password.length() < 6) {
            binding.tilPassword.setError("Must be at least 6 characters");
            valid = false;
        } else {
            binding.tilPassword.setError(null);
        }

        return valid;
    }

    // ────────────────────────────────────────────────────────
    // OBSERVE VIEWMODEL
    // ────────────────────────────────────────────────────────
    private void observeViewModel() {

        // Success — move to MainActivity
        viewModel.getLoginSuccess().observe(this, success -> {
            if (!Boolean.TRUE.equals(success)) return;
            setLoading(false);
            goToMain();
        });

        // Error — show message
        viewModel.getErrorMessage().observe(this, error -> {
            if (error == null || error.isEmpty()) return;
            setLoading(false);
            showError(error);
        });

        // Role mismatch — teacher trying admin toggle or vice versa
        viewModel.getRoleMismatch().observe(this, mismatch -> {
            if (!Boolean.TRUE.equals(mismatch)) return;
            setLoading(false);
            showError("Access denied. This account is not registered as " + selectedRole + ".");
        });
    }

    // ────────────────────────────────────────────────────────
    // NAVIGATION — only destination is MainActivity
    // ────────────────────────────────────────────────────────
    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // ────────────────────────────────────────────────────────
    // UI HELPERS
    // ────────────────────────────────────────────────────────
    private void setLoading(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.btnLogin.setText(loading ? "Signing in..." : "Sign In");
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
        binding.toggleRole.setEnabled(!loading);
        binding.tilEmail.setEnabled(!loading);
        binding.tilPassword.setEnabled(!loading);
    }

    private void showError(String message) {
        binding.tvError.setVisibility(View.VISIBLE);
        binding.tvError.setText(message);
        binding.btnLogin.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.shake)
        );
    }

    private void clearError() {
        binding.tvError.setVisibility(View.GONE);
        binding.tvError.setText("");
    }

}