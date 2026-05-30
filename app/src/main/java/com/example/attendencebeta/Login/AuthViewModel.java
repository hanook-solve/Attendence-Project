package com.example.attendencebeta.Login;

import android.app.Application;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthViewModel extends AndroidViewModel {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // LiveData that LoginActivity observes
    private final MutableLiveData<Boolean> loginSuccess  = new MutableLiveData<>();
    private final MutableLiveData<String>  errorMessage  = new MutableLiveData<>();
    private final MutableLiveData<Boolean> roleMismatch  = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
    }

    // ─────────────────────────────────────────────────────────────
    public void login(String email, String password, String selectedRole) {

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    verifyRole(uid, selectedRole);
                })
                .addOnFailureListener(e -> {
                    // Make Firebase errors human-readable
                    String msg = friendlyError(e.getMessage());
                    errorMessage.setValue(msg);
                });
    }

    // ─────────────────────────────────────────────────────────────
    // Fetch the user's role from Firestore and cross-check it
    // with what they selected on the toggle.
    // ─────────────────────────────────────────────────────────────
    private void verifyRole(String uid, String selectedRole) {

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        errorMessage.setValue("Account not found in the system. Contact your admin.");
                        auth.signOut();
                        return;
                    }

                    String firestoreRole = doc.getString("role");  // "teacher" or "admin"
                    String name          = doc.getString("name");

                    // Role mismatch — e.g. teacher trying to log in as admin
                    if (!selectedRole.equals(firestoreRole)) {
                        roleMismatch.setValue(true);
                        return;
                    }

                    // All good — cache user data locally
                    saveToPrefs(uid, firestoreRole, name);
                    loginSuccess.setValue(true);
                })
                .addOnFailureListener(e -> {
                    auth.signOut();
                    errorMessage.setValue("Could not verify account. Check your connection.");
                });
    }

    // ─────────────────────────────────────────────────────────────
    private void saveToPrefs(String uid, String role, String name) {
        SharedPreferences prefs = getApplication()
                .getSharedPreferences("app_prefs", Application.MODE_PRIVATE);

        prefs.edit()
                .putString("uid",  uid)
                .putString("role", role)
                .putString("name", name != null ? name : "")
                .apply();
    }

    // ─────────────────────────────────────────────────────────────
    private String friendlyError(String raw) {

        android.util.Log.e("AuthViewModel", "Firebase error: " + raw);
        if (raw == null) return "An unexpected error occurred.";
        if (raw.contains("no user record"))     return "No account found with this email.";
        if (raw.contains("password is invalid")) return "Incorrect password. Please try again.";
        if (raw.contains("badly formatted"))     return "Please enter a valid email address.";
        if (raw.contains("network"))             return "No internet connection. Check your network.";
        if (raw.contains("too-many-requests"))   return "Too many attempts. Try again later.";
        return "Login failed. Please try again.";
    }

    // ─────────────────────────────────────────────────────────────
    // Getters
    // ─────────────────────────────────────────────────────────────
    public MutableLiveData<Boolean> getLoginSuccess() { return loginSuccess; }
    public MutableLiveData<String>  getErrorMessage()  { return errorMessage; }
    public MutableLiveData<Boolean> getRoleMismatch()  { return roleMismatch; }


}
