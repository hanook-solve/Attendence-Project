package com.example.attendencebeta.Admin;

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

public class AdminProfileFragment extends Fragment {

    // ── Views ────────────────────────────────────────────────
    private TextView tvProfileInitial, tvName;
    private TextView tvEmail, tvMemberSince;
    private LinearLayout btnChangePassword;
    private MaterialButton btnSignOut;

    // ── Firebase ─────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // ────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_profile, container, false);
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
        tvProfileInitial  = view.findViewById(R.id.tvProfileInitial);
        tvName            = view.findViewById(R.id.tvName);
        tvEmail           = view.findViewById(R.id.tvEmail);
        tvMemberSince     = view.findViewById(R.id.tvMemberSince);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnSignOut        = view.findViewById(R.id.btnSignOut);
    }

    // ────────────────────────────────────────────────────────
    // POPULATE
    // ────────────────────────────────────────────────────────
    private void populateProfile() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);

        String name = prefs.getString("name", "Admin");

        tvName.setText(name);
        tvProfileInitial.setText(
                name.isEmpty() ? "A" : String.valueOf(name.charAt(0)).toUpperCase()
        );

        if (currentUser != null) {
            String email = currentUser.getEmail();
            tvEmail.setText(email != null ? email : "—");

            if (currentUser.getMetadata() != null) {
                long created = currentUser.getMetadata().getCreationTimestamp();
                tvMemberSince.setText(
                        new SimpleDateFormat("MMM yyyy", Locale.getDefault())
                                .format(new Date(created))
                );
            }
        }
    }

    // ────────────────────────────────────────────────────────
    // CHANGE PASSWORD
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
        mAuth.signOut();

        requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}