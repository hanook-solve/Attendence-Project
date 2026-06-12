package com.example.attendencebeta.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendencebeta.R;
import com.example.attendencebeta.model.StaffModel;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class StaffAttendanceAdapter
        extends RecyclerView.Adapter<StaffAttendanceAdapter.VH> {

    private List<StaffModel> list;
    private final OnMarkPresentClick listener;

    public interface OnMarkPresentClick {
        void onMark(StaffModel staff);
    }

    public StaffAttendanceAdapter(List<StaffModel> list, OnMarkPresentClick listener) {
        this.list     = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_staff_attendance, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(list.get(position), listener);
    }

    @Override
    public int getItemCount() { return list.size(); }

    public void updateList(List<StaffModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    // ── ViewHolder ───────────────────────────────────────────
    static class VH extends RecyclerView.ViewHolder {

        private final LinearLayout  tvAvatar;
        private final TextView      tvInitial, tvName, tvStatus;
        private final MaterialButton btnMarkPresent;

        VH(View v) {
            super(v);
            tvAvatar       = v.findViewById(R.id.tvAvatar);
            tvInitial      = v.findViewById(R.id.tvInitial);
            tvName         = v.findViewById(R.id.tvName);
            tvStatus       = v.findViewById(R.id.tvStatus);
            btnMarkPresent = v.findViewById(R.id.btnMarkPresent);
        }

        void bind(StaffModel staff, OnMarkPresentClick listener) {
            tvName.setText(staff.getName());

            // Profile initial
            String initial = staff.getName().isEmpty()
                    ? "T"
                    : String.valueOf(staff.getName().charAt(0)).toUpperCase();
            tvInitial.setText(initial);

            // Status chip styling
            switch (staff.getStatus()) {
                case "present":
                    tvStatus.setText("Present");
                    tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_present);
                    btnMarkPresent.setVisibility(View.GONE);
                    break;

                case "late":
                    tvStatus.setText("Late");
                    tvStatus.setTextColor(Color.parseColor("#E65100"));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_late);
                    btnMarkPresent.setVisibility(View.GONE);
                    break;

                case "absent":
                    tvStatus.setText("Absent");
                    tvStatus.setTextColor(Color.parseColor("#C62828"));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_absent);
                    btnMarkPresent.setVisibility(View.VISIBLE);
                    btnMarkPresent.setOnClickListener(v -> listener.onMark(staff));
                    break;

                default:
                    // not_marked
                    tvStatus.setText("Not Marked");
                    tvStatus.setTextColor(Color.parseColor("#9AAABB"));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    btnMarkPresent.setVisibility(View.VISIBLE);
                    btnMarkPresent.setOnClickListener(v -> listener.onMark(staff));
                    break;
            }
        }
    }
}