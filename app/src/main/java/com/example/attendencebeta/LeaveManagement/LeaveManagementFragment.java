package com.example.attendencebeta.Admin;

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
import com.example.attendencebeta.adapter.LeaveRequestAdapter;
import com.example.attendencebeta.model.LeaveRequestModel;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LeaveManagementFragment extends Fragment {

    // ── Views ────────────────────────────────────────────────
    private TextView tvPendingCount, tvApprovedCount, tvRejectedCount;
    private TextView tvEmpty;
    private ChipGroup chipGroupFilter;
    private LinearProgressIndicator progressBar;
    private RecyclerView rvLeaveRequests;

    // ── Firebase ─────────────────────────────────────────────
    private FirebaseFirestore db;

    // ── Data ─────────────────────────────────────────────────
    private List<LeaveRequestModel> allRequests      = new ArrayList<>();
    private List<LeaveRequestModel> filteredRequests = new ArrayList<>();
    private LeaveRequestAdapter adapter;
    private String currentFilter = "all";

    // ────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_leave_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        bindViews(view);
        setupRecyclerView();
        setupFilterChips();
        loadAllLeaveRequests();
    }

    // ────────────────────────────────────────────────────────
    // BIND VIEWS
    // ────────────────────────────────────────────────────────
    private void bindViews(View view) {
        tvPendingCount  = view.findViewById(R.id.tvPendingCount);
        tvApprovedCount = view.findViewById(R.id.tvApprovedCount);
        tvRejectedCount = view.findViewById(R.id.tvRejectedCount);
        tvEmpty         = view.findViewById(R.id.tvEmpty);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        progressBar     = view.findViewById(R.id.progressBar);
        rvLeaveRequests = view.findViewById(R.id.rvLeaveRequests);
    }

    // ────────────────────────────────────────────────────────
    // RECYCLER VIEW
    // ────────────────────────────────────────────────────────
    private void setupRecyclerView() {
        adapter = new LeaveRequestAdapter(filteredRequests, new LeaveRequestAdapter.OnActionClick() {
            @Override
            public void onApprove(LeaveRequestModel request) {
                showActionDialog(request, "approve");
            }

            @Override
            public void onReject(LeaveRequestModel request) {
                showActionDialog(request, "reject");
            }
        });
        rvLeaveRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLeaveRequests.setAdapter(adapter);
    }

    // ────────────────────────────────────────────────────────
    // FILTER CHIPS
    // ────────────────────────────────────────────────────────
    private void setupFilterChips() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);

            if      (id == R.id.chipAll)      currentFilter = "all";
            else if (id == R.id.chipPending)  currentFilter = "pending";
            else if (id == R.id.chipApproved) currentFilter = "approved";
            else if (id == R.id.chipRejected) currentFilter = "rejected";

            applyFilter();
        });
    }

    private void applyFilter() {
        filteredRequests.clear();

        for (LeaveRequestModel req : allRequests) {
            if (currentFilter.equals("all") || currentFilter.equals(req.getStatus())) {
                filteredRequests.add(req);
            }
        }

        adapter.updateList(filteredRequests);
        tvEmpty.setVisibility(filteredRequests.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ────────────────────────────────────────────────────────
    // LOAD ALL LEAVE REQUESTS
    // Step 1: fetch all teachers
    // Step 2: for each teacher fetch their leave requests
    // ────────────────────────────────────────────────────────
    private void loadAllLeaveRequests() {
        progressBar.setVisibility(View.VISIBLE);
        allRequests.clear();

        // Step 1 — get all teachers
        db.collection("users")
                .whereEqualTo("role", "teacher")
                .get()
                .addOnSuccessListener(usersSnapshot -> {
                    if (usersSnapshot.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    int totalTeachers = usersSnapshot.size();
                    final int[] processed = {0};

                    for (QueryDocumentSnapshot userDoc : usersSnapshot) {
                        String uid  = userDoc.getId();
                        String name = userDoc.getString("name") != null
                                ? userDoc.getString("name") : "Unknown";

                        // Step 2 — fetch this teacher's leave requests
                        db.collection("leaveRequests")
                                .document(uid)
                                .collection("requests")
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        for (QueryDocumentSnapshot reqDoc : task.getResult()) {
                                            String dateKey = reqDoc.getId();
                                            String type    = reqDoc.getString("type");
                                            String status  = reqDoc.getString("status");
                                            long reqAt     = 0;

                                            if (reqDoc.getTimestamp("requestedAt") != null) {
                                                reqAt = reqDoc.getTimestamp("requestedAt")
                                                        .toDate().getTime();
                                            }

                                            if (type != null && status != null) {
                                                allRequests.add(new LeaveRequestModel(
                                                        uid, name, dateKey, type, status, reqAt
                                                ));
                                            }
                                        }
                                    }

                                    processed[0]++;

                                    if (processed[0] == totalTeachers && getActivity() != null) {
                                        requireActivity().runOnUiThread(() -> {
                                            // Sort by date descending — most recent first
                                            allRequests.sort((a, b) ->
                                                    b.getDateKey().compareTo(a.getDateKey())
                                            );
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
        int pending = 0, approved = 0, rejected = 0;

        for (LeaveRequestModel req : allRequests) {
            switch (req.getStatus()) {
                case "pending":  pending++;  break;
                case "approved": approved++; break;
                case "rejected": rejected++; break;
            }
        }

        tvPendingCount.setText(String.valueOf(pending));
        tvApprovedCount.setText(String.valueOf(approved));
        tvRejectedCount.setText(String.valueOf(rejected));
    }

    // ────────────────────────────────────────────────────────
    // ACTION DIALOG — Approve or Reject
    // ────────────────────────────────────────────────────────
    private void showActionDialog(LeaveRequestModel request, String action) {
        String title   = "approve".equals(action) ? "Approve Leave" : "Reject Leave";
        String message = ("approve".equals(action) ? "Approve " : "Reject ")
                + request.getTeacherName() + "'s "
                + request.getTypeLabel().replaceAll("[^a-zA-Z ]", "").trim()
                + " on " + request.getDateKey() + "?";
        String btnText = "approve".equals(action) ? "Approve" : "Reject";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(btnText, (dialog, which) ->
                        updateLeaveStatus(request, "approve".equals(action) ? "approved" : "rejected")
                )
                .show();
    }

    private void updateLeaveStatus(LeaveRequestModel request, String newStatus) {
        db.collection("leaveRequests")
                .document(request.getUid())
                .collection("requests")
                .document(request.getDateKey())
                .update("status", newStatus)
                .addOnSuccessListener(v -> {
                    // Update locally — no need to reload everything
                    request.setStatus(newStatus);

                    // If approved — also write to attendance as "leave"
                    if ("approved".equals(newStatus)) {
                        writeLeaveToAttendance(request);
                    }

                    updateSummaryCards();
                    applyFilter();
                })
                .addOnFailureListener(e ->
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Error")
                                .setMessage("Failed to update. Try again.")
                                .setPositiveButton("OK", null)
                                .show()
                );
    }

    // ────────────────────────────────────────────────────────
    // WRITE APPROVED LEAVE TO ATTENDANCE COLLECTION
    // So it shows as "leave" on teacher's calendar
    // ────────────────────────────────────────────────────────
    private void writeLeaveToAttendance(LeaveRequestModel request) {
        java.util.Map<String, Object> record = new java.util.HashMap<>();
        record.put("status",   "leave");
        record.put("markedBy", "admin");
        record.put("type",     request.getType());
        record.put("markedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("attendance")
                .document(request.getDateKey())
                .collection("records")
                .document(request.getUid())
                .set(record);
        // No listener needed — background write
    }
}