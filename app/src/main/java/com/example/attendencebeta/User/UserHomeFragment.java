package com.example.attendencebeta.User;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.attendencebeta.Login.LoginActivity;
import com.example.attendencebeta.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UserHomeFragment extends Fragment {

    // ── Views ────────────────────────────────────────────────
    private TextView tvProfileInitial, tvUserName, tvDesignation;
    private TextView tvDate, tvTodayStatus;
    private View dotLocation, dotTime;
    private View swipeThumb, swipeContainer;
    private TextView tvSwipeLabel, tvTimeWindow, tvConditionMessage;
    private LinearLayout llHistoryCards;

    // ── Firebase ─────────────────────────────────────────────
    private FirebaseFirestore db;
    private String uid;

    // ── Location ─────────────────────────────────────────────
    private FusedLocationProviderClient locationClient;
    private static final int LOCATION_CODE = 101;

    // ── School coordinates — update to real values ───────────
    private static final double SCHOOL_LAT    = 33.6844;
    private static final double SCHOOL_LNG    = 73.0479;
    private static final float  RADIUS_METERS = 100f;

    // ── Attendance window ────────────────────────────────────
    private static final String WINDOW_START = "07:45";
    private static final String WINDOW_END   = "08:15";

    // ── State ────────────────────────────────────────────────
    private boolean conditionLocation = false;
    private boolean conditionTime     = false;
    private boolean alreadyMarked     = false;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db  = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        locationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        bindViews(view);
        setupHeader();
        setupDate();
        setupSwipeButton();
        checkTimeWindow();
        checkIfAlreadyMarked();
        requestLocation();
        loadHistory();
    }
    // ────────────────────────────────────────────────────────
    // BIND VIEWS
    // ────────────────────────────────────────────────────────
    private void bindViews(View view) {
        tvProfileInitial   = view.findViewById(R.id.tvProfileInitial);
        tvUserName         = view.findViewById(R.id.tvUserName);
        tvDesignation      = view.findViewById(R.id.tvDesignation);
        tvDate             = view.findViewById(R.id.tvDate);
        tvTodayStatus      = view.findViewById(R.id.tvTodayStatus);
        dotLocation        = view.findViewById(R.id.dotLocation);
        dotTime            = view.findViewById(R.id.dotTime);
        swipeContainer     = view.findViewById(R.id.swipeContainer);
        swipeThumb         = view.findViewById(R.id.swipeThumb);
        tvSwipeLabel       = view.findViewById(R.id.tvSwipeLabel);
        tvTimeWindow       = view.findViewById(R.id.tvTimeWindow);
        tvConditionMessage = view.findViewById(R.id.tvConditionMessage);
        llHistoryCards     = view.findViewById(R.id.llHistoryCards);
    }

    // ────────────────────────────────────────────────────────
    // HEADER — name + initial from SharedPrefs
    // ────────────────────────────────────────────────────────
    private void setupHeader() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);

        String name = prefs.getString("name", "Teacher");
        tvUserName.setText(name);
        tvDesignation.setText("Teacher");

        // Profile initial — first letter of name
        if (!name.isEmpty()) {
            tvProfileInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }
    }

    // ────────────────────────────────────────────────────────
    // DATE
    // ────────────────────────────────────────────────────────
    private void setupDate() {
        String date = new SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault()).format(new Date());
        tvDate.setText(date);
    }

    // ────────────────────────────────────────────────────────
    // SWIPE BUTTON
    // ────────────────────────────────────────────────────────
    private void setupSwipeButton() {
        // Disable by default — enabled only when both conditions pass
        swipeThumb.setAlpha(0.4f);

        swipeThumb.setOnTouchListener((v, event) -> {

            // Block swipe if conditions not met or already marked
            if (!conditionLocation || !conditionTime || alreadyMarked) {
                showConditionMessage();
                return true;
            }

            switch (event.getAction()) {

                case MotionEvent.ACTION_MOVE:
                    // Calculate new X position of thumb
                    float newX = event.getRawX()
                            - swipeContainer.getX()
                            - swipeContainer.getPaddingLeft()
                            - (swipeThumb.getWidth() / 2f);

                    // Clamp within container bounds
                    float maxX = swipeContainer.getWidth()
                            - swipeThumb.getWidth()
                            - (swipeContainer.getPaddingLeft() * 2);

                    newX = Math.max(0, Math.min(newX, maxX));
                    swipeThumb.setX(newX);

                    // Fade out label as thumb moves right
                    float progress = newX / maxX;
                    tvSwipeLabel.setAlpha(1f - progress);
                    break;

                case MotionEvent.ACTION_UP:
                    float threshold = swipeContainer.getWidth()
                            - swipeThumb.getWidth()
                            - (swipeContainer.getPaddingLeft() * 2);

                    if (swipeThumb.getX() >= threshold * 0.85f) {
                        // ✅ Swiped far enough — mark attendance
                        onSwipeCompleted();
                    } else {
                        // ❌ Not far enough — reset thumb
                        swipeThumb.animate().x(0).setDuration(250).start();
                        tvSwipeLabel.animate().alpha(1f).setDuration(250).start();
                    }
                    break;
            }
            return true;
        });
    }

    private void showConditionMessage() {
        tvConditionMessage.setVisibility(View.VISIBLE);
        if (alreadyMarked) {
            tvConditionMessage.setText("✓ Attendance already marked today");
            tvConditionMessage.setTextColor(Color.parseColor("#2E7D32"));
        } else if (!conditionLocation && !conditionTime) {
            tvConditionMessage.setText("⚠ You must be inside school during the attendance window");
            tvConditionMessage.setTextColor(Color.parseColor("#E65100"));
        } else if (!conditionLocation) {
            tvConditionMessage.setText("📍 You are not inside school premises");
            tvConditionMessage.setTextColor(Color.parseColor("#E65100"));
        } else {
            tvConditionMessage.setText("🕐 Attendance window is closed");
            tvConditionMessage.setTextColor(Color.parseColor("#E65100"));
        }
    }

    private void onSwipeCompleted() {
        // Snap thumb to end
        float maxX = swipeContainer.getWidth()
                - swipeThumb.getWidth()
                - (swipeContainer.getPaddingLeft() * 2);
        swipeThumb.animate().x(maxX).setDuration(150).start();
        tvSwipeLabel.setAlpha(0f);

        // Write to Firestore
        markAttendance();
    }

    // ────────────────────────────────────────────────────────
    // MARK ATTENDANCE — write to Firestore
    // ────────────────────────────────────────────────────────
    private void markAttendance() {
        // Get location one more time at the moment of marking
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationClient.getLastLocation().addOnSuccessListener(location -> {
                double lat = location != null ? location.getLatitude()  : SCHOOL_LAT;
                double lng = location != null ? location.getLongitude() : SCHOOL_LNG;

                String today = todayKey();
                Map<String, Object> record = new HashMap<>();
                record.put("status",   "present");
                record.put("markedAt", FieldValue.serverTimestamp());
                record.put("markedBy", "self");
                record.put("location", new GeoPoint(lat, lng));

                db.collection("attendance")
                        .document(today)
                        .collection("records")
                        .document(uid)
                        .set(record)
                        .addOnSuccessListener(v -> lockAsMarked())
                        .addOnFailureListener(e -> {
                            // Reset swipe on failure
                            swipeThumb.animate().x(0).setDuration(250).start();
                            tvSwipeLabel.animate().alpha(1f).setDuration(250).start();
                        });
            });
        }
    }

    private void lockAsMarked() {
        alreadyMarked = true;

        // Lock the swipe container
        swipeContainer.setBackgroundResource(R.drawable.bg_swipe_done);
        tvSwipeLabel.setVisibility(View.VISIBLE);
        tvSwipeLabel.setAlpha(1f);
        tvSwipeLabel.setText("✓  Attendance Marked");
        tvSwipeLabel.setTextColor(Color.parseColor("#FFFFFF"));
        swipeThumb.setVisibility(View.GONE);

        // Update status chip
        tvTodayStatus.setText("Present");
        tvTodayStatus.setTextColor(Color.parseColor("#2E7D32"));
        tvTodayStatus.setBackgroundResource(R.drawable.bg_status_present);

        // Update condition message
        tvConditionMessage.setVisibility(View.VISIBLE);
        tvConditionMessage.setText("✓ Attendance marked successfully");
        tvConditionMessage.setTextColor(Color.parseColor("#2E7D32"));

        // Refresh history
        llHistoryCards.removeAllViews();
        loadHistory();
    }

    // ────────────────────────────────────────────────────────
    // CHECK IF ALREADY MARKED TODAY
    // ────────────────────────────────────────────────────────
    private void checkIfAlreadyMarked() {
        db.collection("attendance")
                .document(todayKey())
                .collection("records")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String status = doc.getString("status");
                        alreadyMarked = true;
                        lockAsMarked();
                        if ("late".equals(status)) {
                            tvTodayStatus.setText("Late");
                            tvTodayStatus.setTextColor(Color.parseColor("#E65100"));
                            tvTodayStatus.setBackgroundResource(R.drawable.bg_status_late);
                        }
                    } else {
                        swipeThumb.setAlpha(1f); // unlock thumb visually
                    }
                });
    }

    // ────────────────────────────────────────────────────────
    // TIME WINDOW CHECK
    // ────────────────────────────────────────────────────────
    private void checkTimeWindow() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date now   = sdf.parse(sdf.format(new Date()));
            Date start = sdf.parse(WINDOW_START);
            Date end   = sdf.parse(WINDOW_END);
            conditionTime = !now.before(start) && !now.after(end);
        } catch (Exception e) {
            conditionTime = false;
        }

        if (conditionTime) {
            dotTime.setBackgroundResource(R.drawable.dot_green);
            tvTimeWindow.setText("⏰  Window open: " + WINDOW_START + " – " + WINDOW_END);
            tvTimeWindow.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            dotTime.setBackgroundResource(R.drawable.dot_red);
            tvTimeWindow.setText("⏰  Window closed: " + WINDOW_START + " – " + WINDOW_END);
            tvTimeWindow.setTextColor(Color.parseColor("#9AAABB"));
        }

        updateSwipeState();
    }

    // ────────────────────────────────────────────────────────
    // LOCATION
    // ────────────────────────────────────────────────────────
    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_CODE
            );
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                float[] result = new float[1];
                android.location.Location.distanceBetween(
                        SCHOOL_LAT, SCHOOL_LNG,
                        location.getLatitude(), location.getLongitude(),
                        result
                );
                conditionLocation = result[0] <= RADIUS_METERS;
            } else {
                conditionLocation = false;
            }

            dotLocation.setBackgroundResource(
                    conditionLocation ? R.drawable.dot_green : R.drawable.dot_red
            );
            updateSwipeState();
        });
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == LOCATION_CODE && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        } else {
            dotLocation.setBackgroundResource(R.drawable.dot_red);
        }
    }

    // ────────────────────────────────────────────────────────
    // SWIPE STATE
    // ────────────────────────────────────────────────────────
    private void updateSwipeState() {
        if (alreadyMarked) return;
        boolean canSwipe = conditionLocation && conditionTime;
        swipeThumb.setAlpha(canSwipe ? 1.0f : 0.4f);
    }

    // ────────────────────────────────────────────────────────
    // 7 DAY HISTORY — horizontal scroll cards
    // ────────────────────────────────────────────────────────
    private void loadHistory() {
        SimpleDateFormat keyFmt  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dayFmt  = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd", Locale.getDefault());

        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            String dateKey  = keyFmt.format(cal.getTime());
            String dayName  = dayFmt.format(cal.getTime());
            String dayNum   = dateFmt.format(cal.getTime());
            boolean isToday = i == 0;

            db.collection("attendance")
                    .document(dateKey)
                    .collection("records")
                    .document(uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        String status = "—";
                        if (task.isSuccessful() && task.getResult().exists()) {
                            String s = task.getResult().getString("status");
                            status = s != null ? s : "—";
                        }

                        final String finalStatus = status;

                        if (getActivity() != null) {
                            requireActivity().runOnUiThread(() ->
                                    addHistoryCard(dayName, dayNum, finalStatus, isToday)
                            );
                        }
                    });

            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
    }

    private void addHistoryCard(String day, String date, String status, boolean isToday) {
        // Card container
        CardView card = new CardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                dpToPx(80), dpToPx(100)
        );
        cardParams.setMarginEnd(dpToPx(10));
        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(12));
        card.setCardElevation(dpToPx(3));
        card.setCardBackgroundColor(isToday
                ? Color.parseColor("#1A3C8F")
                : Color.parseColor("#FFFFFF")
        );

        // Inner layout
        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(android.view.Gravity.CENTER);
        inner.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12));

        // Day name — Mon, Tue etc
        TextView tvDay = new TextView(requireContext());
        tvDay.setText(day);
        tvDay.setTextSize(11);
        tvDay.setGravity(android.view.Gravity.CENTER);
        tvDay.setTextColor(isToday
                ? Color.parseColor("#A8C0E8")
                : Color.parseColor("#7A8BB0")
        );

        // Date number
        TextView tvDate = new TextView(requireContext());
        tvDate.setText(date);
        tvDate.setTextSize(20);
        tvDate.setGravity(android.view.Gravity.CENTER);
        tvDate.setTypeface(null, android.graphics.Typeface.BOLD);
        tvDate.setTextColor(isToday
                ? Color.parseColor("#FFFFFF")
                : Color.parseColor("#0D1B3E")
        );

        // Status chip
        TextView tvStatus = new TextView(requireContext());
        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        chipParams.topMargin = dpToPx(8);
        chipParams.bottomMargin = dpToPx(10);
        tvStatus.setLayoutParams(chipParams);
        tvStatus.setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2));
        tvStatus.setTextSize(9);
        tvStatus.setGravity(android.view.Gravity.CENTER);

        switch (status) {
            case "present":
                tvStatus.setText("Present");
                tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                tvStatus.setBackgroundResource(R.drawable.bg_status_present);
                break;
            case "late":
                tvStatus.setText("Late");
                tvStatus.setTextColor(Color.parseColor("#E65100"));
                tvStatus.setBackgroundResource(R.drawable.bg_status_late);
                break;
            case "absent":
                tvStatus.setText("Absent");
                tvStatus.setTextColor(Color.parseColor("#C62828"));
                tvStatus.setBackgroundResource(R.drawable.bg_status_absent);
                break;
            default:
                tvStatus.setText("—");
                tvStatus.setTextColor(isToday
                        ? Color.parseColor("#A8C0E8")
                        : Color.parseColor("#9AAABB")
                );
                break;
        }

        inner.addView(tvDay);
        inner.addView(tvDate);
        inner.addView(tvStatus);
        card.addView(inner);
        llHistoryCards.addView(card);
    }

    // ────────────────────────────────────────────────────────
    // HELPERS
    // ────────────────────────────────────────────────────────
    private String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
