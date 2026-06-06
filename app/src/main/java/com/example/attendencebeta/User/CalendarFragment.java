package com.example.attendencebeta.User;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.attendencebeta.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CalendarFragment extends Fragment {
    // ── Views ────────────────────────────────────────────────
    private TextView tvHeaderTitle, tvProfileInitial;
    private TextView tvMonthYear, tvPerformanceTitle;
    private TextView tvPresentCount, tvLateCount, tvLeaveCount;
    private CardView btnPrevMonth, btnNextMonth;
    private LinearLayout calendarContainer;
    private LinearLayout btnSickLeave, btnMarriageLeave, btnEmergencyLeave;

    // ── Firebase ─────────────────────────────────────────────
    private FirebaseFirestore db;
    private String uid;

    // ── State ────────────────────────────────────────────────
    private Calendar displayedMonth = Calendar.getInstance();

    // dateKey → status map for displayed month
    private final Map<String, String> monthData = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db  = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        bindViews(view);
        setupHeader();
        setupMonthNavigation();
        setupLeaveButtons();
        loadMonthData();
    }
    // ────────────────────────────────────────────────────────
    // BIND VIEWS
    // ────────────────────────────────────────────────────────
    private void bindViews(View view) {
        tvHeaderTitle      = view.findViewById(R.id.tvHeaderTitle);
        tvProfileInitial   = view.findViewById(R.id.tvProfileInitial);
        tvMonthYear        = view.findViewById(R.id.tvMonthYear);
        tvPerformanceTitle = view.findViewById(R.id.tvPerformanceTitle);
        tvPresentCount     = view.findViewById(R.id.tvPresentCount);
        tvLateCount        = view.findViewById(R.id.tvLateCount);
        tvLeaveCount       = view.findViewById(R.id.tvLeaveCount);
        btnPrevMonth       = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth       = view.findViewById(R.id.btnNextMonth);
        calendarContainer  = view.findViewById(R.id.calendarContainer);
        btnSickLeave       = view.findViewById(R.id.btnSickLeave);
        btnMarriageLeave   = view.findViewById(R.id.btnMarriageLeave);
        btnEmergencyLeave  = view.findViewById(R.id.btnEmergencyLeave);
    }

    // ────────────────────────────────────────────────────────
    // HEADER
    // ────────────────────────────────────────────────────────
    private void setupHeader() {
        android.content.SharedPreferences prefs = requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String name = prefs.getString("name", "T");
        tvProfileInitial.setText(
                name.isEmpty() ? "T" : String.valueOf(name.charAt(0)).toUpperCase()
        );
    }

    private void updateHeaderTitle() {
        String monthYear = new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                .format(displayedMonth.getTime());
        tvHeaderTitle.setText("Calendar Overview | " + monthYear);
        tvMonthYear.setText(monthYear);
        tvPerformanceTitle.setText(monthYear + " Performance");
    }

    // ────────────────────────────────────────────────────────
    // MONTH NAVIGATION
    // ────────────────────────────────────────────────────────
    private void setupMonthNavigation() {
        btnPrevMonth.setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, -1);
            loadMonthData();
        });

        btnNextMonth.setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, 1);
            loadMonthData();
        });
    }

    // ────────────────────────────────────────────────────────
    // LEAVE BUTTONS — show date picker dialog on tap
    // ────────────────────────────────────────────────────────
    private void setupLeaveButtons() {
        btnSickLeave.setOnClickListener(v ->
                showLeaveDateDialog("sick", "Sick Leave"));
        btnMarriageLeave.setOnClickListener(v ->
                showLeaveDateDialog("marriage", "Marriage Leave"));
        btnEmergencyLeave.setOnClickListener(v ->
                showLeaveDateDialog("home_emergency", "Home Emergency"));
    }

    private void showLeaveDateDialog(String leaveType, String leaveLabel) {
        // Build a date picker using Android's built-in DatePickerDialog
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.DAY_OF_YEAR, 1); // at least tomorrow

        android.app.DatePickerDialog dialog = new android.app.DatePickerDialog(
                requireContext(),
                (picker, year, month, day) -> {
                    // Build the date key
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, day);
                    String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(selected.getTime());
                    String dateLabel = new SimpleDateFormat("EEEE, MMM dd yyyy", Locale.getDefault())
                            .format(selected.getTime());

                    // Confirm before submitting
                    new AlertDialog.Builder(requireContext())
                            .setTitle(leaveLabel)
                            .setMessage("Request leave on:\n" + dateLabel + "?")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Submit", (d, w) ->
                                    submitLeaveRequest(dateKey, leaveType)
                            )
                            .show();
                },
                minDate.get(Calendar.YEAR),
                minDate.get(Calendar.MONTH),
                minDate.get(Calendar.DAY_OF_MONTH)
        );

        // Restrict to future dates only
        dialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        dialog.setTitle("Select date for " + leaveLabel);
        dialog.show();
    }

    private void submitLeaveRequest(String dateKey, String leaveType) {
        Map<String, Object> request = new HashMap<>();
        request.put("type",        leaveType);
        request.put("status",      "pending");
        request.put("requestedAt", FieldValue.serverTimestamp());
        request.put("uid",         uid);

        db.collection("leaveRequests")
                .document(uid)
                .collection("requests")
                .document(dateKey)
                .set(request)
                .addOnSuccessListener(v -> {
                    Toast.makeText(requireContext(),
                            "✓ Leave request submitted", Toast.LENGTH_SHORT).show();
                    // If this date is in displayed month, refresh
                    String displayedMonthStr = new SimpleDateFormat("yyyy-MM",
                            Locale.getDefault()).format(displayedMonth.getTime());
                    if (dateKey.startsWith(displayedMonthStr)) {
                        monthData.put(dateKey, "pending_leave");
                        rebuildCalendar();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ────────────────────────────────────────────────────────
    // LOAD MONTH DATA
    // ────────────────────────────────────────────────────────
    private void loadMonthData() {
        updateHeaderTitle();
        monthData.clear();

        int daysInMonth = displayedMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar today  = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Count days that have already passed
        Calendar cal = (Calendar) displayedMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int toFetch = 0;
        for (int i = 1; i <= daysInMonth; i++) {
            cal.set(Calendar.DAY_OF_MONTH, i);
            if (!cal.after(today)) toFetch++;
        }

        if (toFetch == 0) {
            rebuildCalendar();
            updateSummaryCounts();
            return;
        }

        final int[] fetched = {0};
        final int totalToFetch = toFetch;

        cal.set(Calendar.DAY_OF_MONTH, 1);
        for (int i = 1; i <= daysInMonth; i++) {
            cal.set(Calendar.DAY_OF_MONTH, i);
            if (cal.after(today)) continue;

            String dateKey = sdf.format(cal.getTime());

            // Fetch attendance
            db.collection("attendance").document(dateKey)
                    .collection("records").document(uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            String status = task.getResult().getString("status");
                            if (status != null) monthData.put(dateKey, status);
                        }
                        fetched[0]++;
                        if (fetched[0] == totalToFetch) {
                            fetchLeaveRequests(daysInMonth, sdf, today);
                        }
                    });
        }
    }

    private void fetchLeaveRequests(int daysInMonth, SimpleDateFormat sdf, Calendar today) {
        Calendar cal = (Calendar) displayedMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        final int[] done = {0};

        for (int i = 1; i <= daysInMonth; i++) {
            cal.set(Calendar.DAY_OF_MONTH, i);
            String dateKey = sdf.format(cal.getTime());

            db.collection("leaveRequests").document(uid)
                    .collection("requests").document(dateKey)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            String status = task.getResult().getString("status");
                            if ("approved".equals(status)) {
                                monthData.put(dateKey, "leave");
                            } else if ("pending".equals(status)) {
                                monthData.put(dateKey, "pending_leave");
                            }
                        }
                        done[0]++;
                        if (done[0] == daysInMonth && getActivity() != null) {
                            requireActivity().runOnUiThread(() -> {
                                rebuildCalendar();
                                updateSummaryCounts();
                            });
                        }
                    });
        }
    }

    // ────────────────────────────────────────────────────────
    // BUILD CALENDAR — rows of 7 CardView cells
    // ────────────────────────────────────────────────────────
    private void rebuildCalendar() {
        calendarContainer.removeAllViews();

        Calendar cal = (Calendar) displayedMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        Calendar today = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Offset: Mon=0 ... Sun=6
        int firstDay = cal.get(Calendar.DAY_OF_WEEK);
        int offset   = (firstDay + 5) % 7;

        int daysInMonth = displayedMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        int totalCells  = offset + daysInMonth;
        int rows        = (int) Math.ceil(totalCells / 7.0);

        // Build empty+day array
        String[] cells = new String[rows * 7];
        int dayNum = 1;
        for (int i = 0; i < cells.length; i++) {
            if (i < offset || dayNum > daysInMonth) {
                cells[i] = "";
            } else {
                cells[i] = String.valueOf(dayNum++);
            }
        }

        // Build rows
        for (int row = 0; row < rows; row++) {
            LinearLayout rowLayout = new LinearLayout(requireContext());
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rowParams.bottomMargin = dpToPx(6);
            rowLayout.setLayoutParams(rowParams);

            for (int col = 0; col < 7; col++) {
                int idx   = row * 7 + col;
                String d  = cells[idx];

                if (d.isEmpty()) {
                    // Empty cell
                    View empty = new View(requireContext());
                    LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
                            0, dpToPx(62)
                    );
                    ep.weight = 1;
                    ep.setMarginEnd(dpToPx(4));
                    empty.setLayoutParams(ep);
                    rowLayout.addView(empty);
                } else {
                    int day = Integer.parseInt(d);
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    String dateKey  = sdf.format(cal.getTime());
                    String status   = monthData.getOrDefault(dateKey, "");
                    boolean isToday = sdf.format(today.getTime()).equals(dateKey);
                    boolean isPast  = !cal.after(today);

                    rowLayout.addView(buildDayCell(day, dateKey, status, isToday, isPast, col == 5 || col == 6));
                }
            }
            calendarContainer.addView(rowLayout);
        }
    }

    private View buildDayCell(int day, String dateKey, String status,
                              boolean isToday, boolean isPast, boolean isWeekend) {

        CardView card = new CardView(requireContext());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dpToPx(62));
        cp.weight = 1;
        cp.setMarginEnd(dpToPx(4));
        card.setLayoutParams(cp);
        card.setRadius(dpToPx(10));
        card.setCardElevation(isToday ? dpToPx(3) : dpToPx(1));
        card.setCardBackgroundColor(isToday
                ? Color.parseColor("#EBF0FF")
                : Color.parseColor("#FFFFFF")
        );

        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setPadding(dpToPx(2), dpToPx(6), dpToPx(2), dpToPx(6));

        // Date number
        TextView tvDay = new TextView(requireContext());
        String dayStr = day < 10 ? "0" + day : String.valueOf(day);
        tvDay.setText(dayStr);
        tvDay.setTextSize(15);
        tvDay.setGravity(Gravity.CENTER);
        tvDay.setTypeface(null, isToday ? Typeface.BOLD : Typeface.NORMAL);

        if (!isPast) {
            tvDay.setTextColor(Color.parseColor("#C8D4F0")); // future — light
        } else if (isWeekend) {
            tvDay.setTextColor(Color.parseColor("#C62828"));
        } else {
            tvDay.setTextColor(Color.parseColor("#0D1B3E"));
        }

        // Status icon
        TextView tvIcon = new TextView(requireContext());
        tvIcon.setTextSize(13);
        tvIcon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        ip.topMargin = dpToPx(2);
        tvIcon.setLayoutParams(ip);

        switch (status) {
            case "present":
                tvIcon.setText("✓");
                tvIcon.setTextColor(Color.parseColor("#2E7D32"));
                break;
            case "late":
                tvIcon.setText("⏱");
                tvIcon.setTextColor(Color.parseColor("#E65100"));
                break;
            case "absent":
                tvIcon.setText("✕");
                tvIcon.setTextColor(Color.parseColor("#C62828"));
                break;
            case "leave":
                tvIcon.setText("✓");
                tvIcon.setTextColor(Color.parseColor("#1565C0"));
                break;
            case "pending_leave":
                tvIcon.setText("…");
                tvIcon.setTextColor(Color.parseColor("#E65100"));
                break;
            default:
                tvIcon.setText("");
                break;
        }

        inner.addView(tvDay);
        inner.addView(tvIcon);
        card.addView(inner);
        return card;
    }

    // ────────────────────────────────────────────────────────
    // SUMMARY COUNTS
    // ────────────────────────────────────────────────────────
    private void updateSummaryCounts() {
        int present = 0, late = 0, leave = 0;
        for (String s : monthData.values()) {
            switch (s) {
                case "present": present++; break;
                case "late":    late++;    break;
                case "leave":   leave++;   break;
            }
        }
        tvPresentCount.setText("✓ " + present);
        tvLateCount.setText("⏱ " + late);
        tvLeaveCount.setText("✕ " + leave);
    }

    // ────────────────────────────────────────────────────────
    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
