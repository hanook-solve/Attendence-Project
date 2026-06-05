package com.example.attendencebeta.User;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.attendencebeta.Login.LoginActivity;
import com.example.attendencebeta.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    // ── Views ────────────────────────────────────────────────
    private TextView tvProfileInitial, tvName, tvRole;
    private TextView tvEmail, tvRoleValue, tvMemberSince;
    private LinearLayout btnChangePassword;
    private MaterialButton btnSignOut;

    // ── Firebase ─────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth       = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        bindViews(view);
        populateProfile();
        setupChangePassword();
        setupSignOut();
    }
    // ────────────────────────────────────────────────────────
    // BIND VIEWS
    // ────────────────────────────────────────────────────────
    private void bindViews(View view) {
        tvProfileInitial = view.findViewById(R.id.tvProfileInitial);
        tvName           = view.findViewById(R.id.tvName);
        tvRole           = view.findViewById(R.id.tvRole);
        tvEmail          = view.findViewById(R.id.tvEmail);
        tvRoleValue      = view.findViewById(R.id.tvRoleValue);
        tvMemberSince    = view.findViewById(R.id.tvMemberSince);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnSignOut       = view.findViewById(R.id.btnSignOut);
    }

    // ────────────────────────────────────────────────────────
    // POPULATE — name from SharedPrefs, email from FirebaseAuth
    // ────────────────────────────────────────────────────────
    private void populateProfile() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);

        String name = prefs.getString("name", "Teacher");
        String role = prefs.getString("role", "teacher");

        // Header
        tvName.setText(name);
        tvRole.setText(capitalize(role));
        tvProfileInitial.setText(
                name.isEmpty() ? "T" : String.valueOf(name.charAt(0)).toUpperCase()
        );

        // Info card
        tvRoleValue.setText(capitalize(role));

        // Email from Firebase Auth
        if (currentUser != null) {
            String email = currentUser.getEmail();
            tvEmail.setText(email != null ? email : "—");

            // Member since — from account creation timestamp
            if (currentUser.getMetadata() != null) {
                long creationMillis = currentUser.getMetadata().getCreationTimestamp();
                String date = new SimpleDateFormat("MMM yyyy", Locale.getDefault())
                        .format(new Date(creationMillis));
                tvMemberSince.setText(date);
            }
        }
    }

    // ────────────────────────────────────────────────────────
    // CHANGE PASSWORD — dialog with two fields
    // ────────────────────────────────────────────────────────
    private void setupChangePassword() {
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null);

        TextInputLayout tilNew      = dialogView.findViewById(R.id.tilNewPassword);
        TextInputLayout tilConfirm  = dialogView.findViewById(R.id.tilConfirmPassword);
        TextInputEditText etNew     = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = dialogView.findViewById(R.id.etConfirmPassword);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newPass     = etNew.getText().toString().trim();
                    String confirmPass = etConfirm.getText().toString().trim();

                    if (newPass.isEmpty()) {
                        tilNew.setError("Enter new password");
                        return;
                    }
                    if (newPass.length() < 6) {
                        tilNew.setError("Minimum 6 characters");
                        return;
                    }
                    if (!newPass.equals(confirmPass)) {
                        tilConfirm.setError("Passwords do not match");
                        return;
                    }

                    currentUser.updatePassword(newPass)
                            .addOnSuccessListener(v ->
                                    Toast.makeText(requireContext(),
                                            "Password updated successfully",
                                            Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(),
                                            "Failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show()
                            );
                })
                .show();
    }

    // ────────────────────────────────────────────────────────
    // SIGN OUT
    // ────────────────────────────────────────────────────────
    private void setupSignOut() {
        btnSignOut.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Sign Out")
                        .setMessage("Are you sure you want to sign out?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Sign Out", (dialog, which) -> signOut())
                        .show()
        );
    }

    private void signOut() {
        // Sign out from Firebase
        mAuth.signOut();

        // Clear all local data
        requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        // Go to login — clear entire back stack
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // ────────────────────────────────────────────────────────
    // HELPER
    // ────────────────────────────────────────────────────────
    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}
