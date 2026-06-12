package com.example.attendencebeta.Admin;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendencebeta.R;
import com.example.attendencebeta.adapter.StaffAttendanceAdapter;
import com.example.attendencebeta.model.StaffModel;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminHomeFragment extends Fragment {

    // ── Views ────────────────────────────────────────────────
    private TextView tvGreeting, tvAdminName, tvDate;
    private TextView tvPresentCount, tvLateCount, tvAbsentCount;
    private TextView tvListTitle, tvEmpty;
    private ChipGroup chipGroupFilter;
    private LinearProgressIndicator progressBar;
    private RecyclerView rvStaff;

    // ── Firebase ─────────────────────────────────────────────
    private FirebaseFirestore db;

    // ── Data ─────────────────────────────────────────────────
    private List<StaffModel> allStaffList      = new ArrayList<>();
    private List<StaffModel> filteredList      = new ArrayList<>();
    private StaffAttendanceAdapter adapter;
    private String currentFilter = "all";

    // ────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        bindViews(view);
        setupHeader();
        setupRecyclerView();
        setupFilterChips();
        loadTodayAttendance();
    }

    // ────────────────────────────────────────────────────────
    // BIND VIEWS
    // ────────────────────────────────────────────────────────
    private void bindViews(View view) {
        tvGreeting      = view.findViewById(R.id.tvGreeting);
        tvAdminName     = view.findViewById(R.id.tvAdminName);
        tvDate          = view.findViewById(R.id.tvDate);
        tvPresentCount  = view.findViewById(R.id.tvPresentCount);
        tvLateCount     = view.findViewById(R.id.tvLateCount);
        tvAbsentCount   = view.findViewById(R.id.tvAbsentCount);
        tvListTitle     = view.findViewById(R.id.tvListTitle);
        tvEmpty         = view.findViewById(R.id.tvEmpty);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        progressBar     = view.findViewById(R.id.progressBar);
        rvStaff         = view.findViewById(R.id.rvStaff);
    }

    // ────────────────────────────────────────────────────────
    // HEADER
    // ────────────────────────────────────────────────────────
    private void setupHeader() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String name = prefs.getString("name", "Admin");

        // Greeting based on time
        int hour = Integer.parseInt(
                new SimpleDateFormat("HH", Locale.getDefault()).format(new Date())
        );
        String greeting = hour < 12 ? "Good morning,"
                : hour < 17 ? "Good afternoon,"
                : "Good evening,";

        tvGreeting.setText(greeting);
        tvAdminName.setText(name);
        tvDate.setText(
                new SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault()).format(new Date())
        );
    }

    // ────────────────────────────────────────────────────────
    // RECYCLER VIEW
    // ────────────────────────────────────────────────────────
    private void setupRecyclerView() {
        adapter = new StaffAttendanceAdapter(filteredList, staff ->
                showMarkPresentDialog(staff)
        );
        rvStaff.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvStaff.setAdapter(adapter);
    }

    // ────────────────────────────────────────────────────────
    // FILTER CHIPS
    // ────────────────────────────────────────────────────────
    private void setupFilterChips() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);

            if      (id == R.id.chipAll)     currentFilter = "all";
            else if (id == R.id.chipPresent) currentFilter = "present";
            else if (id == R.id.chipLate)    currentFilter = "late";
            else if (id == R.id.chipAbsent)  currentFilter = "absent";

            applyFilter();
        });
    }

    private void applyFilter() {
        filteredList.clear();

        for (StaffModel staff : allStaffList) {
            if (currentFilter.equals("all")) {
                filteredList.add(staff);
            } else if (currentFilter.equals("absent")) {
                // Absent filter shows both absent and not_marked
                if ("absent".equals(staff.getStatus())
                        || "not_marked".equals(staff.getStatus())) {
                    filteredList.add(staff);
                }
            } else if (currentFilter.equals(staff.getStatus())) {
                filteredList.add(staff);
            }
        }

        adapter.updateList(filteredList);
        tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);

        // Update list title
        switch (currentFilter) {
            case "present": tvListTitle.setText("Present Today"); break;
            case "late":    tvListTitle.setText("Late Arrivals Today"); break;
            case "absent":  tvListTitle.setText("Absent / Not Marked Today"); break;
            default:        tvListTitle.setText("All Staff — Today"); break;
        }
    }

    // ────────────────────────────────────────────────────────
    // LOAD TODAY'S ATTENDANCE
    // Step 1: fetch all teachers
    // Step 2: for each teacher fetch today's record
    // ────────────────────────────────────────────────────────
    private void loadTodayAttendance() {
        progressBar.setVisibility(View.VISIBLE);
        allStaffList.clear();
        String today = todayKey();

        // Step 1 — get all teachers
        db.collection("users")
                .whereEqualTo("role", "teacher")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    int total = querySnapshot.size();
                    final int[] fetched = {0};

                    for (var doc : querySnapshot.getDocuments()) {
                        String uid  = doc.getId();
                        String name = doc.getString("name") != null
                                ? doc.getString("name") : "Unknown";

                        // Step 2 — get attendance record for this teacher
                        db.collection("attendance")
                                .document(today)
                                .collection("records")
                                .document(uid)
                                .get()
                                .addOnCompleteListener(task -> {
                                    String status = "not_marked";
                                    if (task.isSuccessful() && task.getResult().exists()) {
                                        String s = task.getResult().getString("status");
                                        status = s != null ? s : "not_marked";
                                    }

                                    allStaffList.add(new StaffModel(uid, name, status));
                                    fetched[0]++;

                                    if (fetched[0] == total && getActivity() != null) {
                                        requireActivity().runOnUiThread(() -> {
                                            progressBar.setVisibility(View.GONE);
                                            updateSummaryCards();
                                            applyFilter();
                                        });
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Failed to load. Check connection.");
                });
    }

    // ────────────────────────────────────────────────────────
    // SUMMARY CARDS
    // ────────────────────────────────────────────────────────
    private void updateSummaryCards() {
        int present = 0, late = 0, absent = 0;

        for (StaffModel staff : allStaffList) {
            switch (staff.getStatus()) {
                case "present":   present++; break;
                case "late":      late++;    break;
                case "absent":
                case "not_marked": absent++; break;
            }
        }

        tvPresentCount.setText(String.valueOf(present));
        tvLateCount.setText(String.valueOf(late));
        tvAbsentCount.setText(String.valueOf(absent));
    }

    // ────────────────────────────────────────────────────────
    // MARK PRESENT DIALOG
    // ────────────────────────────────────────────────────────
    private void showMarkPresentDialog(StaffModel staff) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Mark as Late Present")
                .setMessage("Mark " + staff.getName() + " as present (late) for today?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Mark Present", (dialog, which) ->
                        markStaffPresent(staff)
                )
                .show();
    }

    private void markStaffPresent(StaffModel staff) {
        String today = todayKey();

        Map<String, Object> record = new HashMap<>();
        record.put("status",   "late");
        record.put("markedAt", FieldValue.serverTimestamp());
        record.put("markedBy", "admin");

        db.collection("attendance")
                .document(today)
                .collection("records")
                .document(staff.getUid())
                .set(record)
                .addOnSuccessListener(v -> {
                    // Update locally — no need to re-fetch everything
                    staff.setStatus("late");
                    updateSummaryCards();
                    applyFilter();
                })
                .addOnFailureListener(e ->
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Error")
                                .setMessage("Failed to mark attendance. Try again.")
                                .setPositiveButton("OK", null)
                                .show()
                );
    }

    // ────────────────────────────────────────────────────────
    // HELPER
    // ────────────────────────────────────────────────────────
    private String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }
}