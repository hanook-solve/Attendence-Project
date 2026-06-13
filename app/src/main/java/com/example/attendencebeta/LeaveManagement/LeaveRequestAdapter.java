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
import com.example.attendencebeta.model.LeaveRequestModel;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LeaveRequestAdapter
        extends RecyclerView.Adapter<LeaveRequestAdapter.VH> {

    private List<LeaveRequestModel> list;

    public interface OnActionClick {
        void onApprove(LeaveRequestModel request);
        void onReject(LeaveRequestModel request);
    }

    private final OnActionClick listener;

    public LeaveRequestAdapter(List<LeaveRequestModel> list, OnActionClick listener) {
        this.list     = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leave_request, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(list.get(position), listener);
    }

    @Override
    public int getItemCount() { return list.size(); }

    public void updateList(List<LeaveRequestModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    // ── ViewHolder ───────────────────────────────────────────
    static class VH extends RecyclerView.ViewHolder {

        private final TextView       tvInitial, tvTeacherName, tvLeaveType;
        private final TextView       tvStatus, tvLeaveDate, tvRequestedAt;
        private final LinearLayout   llActionButtons;
        private final MaterialButton btnApprove, btnReject;

        VH(View v) {
            super(v);
            tvInitial       = v.findViewById(R.id.tvInitial);
            tvTeacherName   = v.findViewById(R.id.tvTeacherName);
            tvLeaveType     = v.findViewById(R.id.tvLeaveType);
            tvStatus        = v.findViewById(R.id.tvStatus);
            tvLeaveDate     = v.findViewById(R.id.tvLeaveDate);
            tvRequestedAt   = v.findViewById(R.id.tvRequestedAt);
            llActionButtons = v.findViewById(R.id.llActionButtons);
            btnApprove      = v.findViewById(R.id.btnApprove);
            btnReject       = v.findViewById(R.id.btnReject);
        }

        void bind(LeaveRequestModel req, OnActionClick listener) {
            // Avatar initial
            String name = req.getTeacherName();
            tvInitial.setText(
                    name.isEmpty() ? "T" : String.valueOf(name.charAt(0)).toUpperCase()
            );

            tvTeacherName.setText(name);
            tvLeaveType.setText(req.getTypeLabel());

            // Leave date formatted
            try {
                SimpleDateFormat sdf     = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat display = new SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault());
                Date date = sdf.parse(req.getDateKey());
                tvLeaveDate.setText(display.format(date));
            } catch (Exception e) {
                tvLeaveDate.setText(req.getDateKey());
            }

            // Requested at timestamp
            if (req.getRequestedAt() > 0) {
                String reqDate = new SimpleDateFormat("MMM dd", Locale.getDefault())
                        .format(new Date(req.getRequestedAt()));
                tvRequestedAt.setText("Requested: " + reqDate);
            }

            // Status chip + action buttons
            switch (req.getStatus()) {
                case "pending":
                    tvStatus.setText("Pending");
                    tvStatus.setTextColor(Color.parseColor("#E65100"));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    llActionButtons.setVisibility(View.VISIBLE);
                    btnApprove.setOnClickListener(v -> listener.onApprove(req));
                    btnReject.setOnClickListener(v -> listener.onReject(req));
                    break;

                case "approved":
                    tvStatus.setText("Approved");
                    tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_present);
                    llActionButtons.setVisibility(View.GONE);
                    break;

                case "rejected":
                    tvStatus.setText("Rejected");
                    tvStatus.setTextColor(Color.parseColor("#C62828"));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_absent);
                    llActionButtons.setVisibility(View.GONE);
                    break;
            }
        }
    }
}