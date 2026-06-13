package com.example.attendencebeta.model;

public class LeaveRequestModel {

    private String uid;           // teacher's uid
    private String teacherName;   // fetched from users collection
    private String dateKey;       // yyyy-MM-dd — the leave date
    private String type;          // sick | marriage | home_emergency
    private String status;        // pending | approved | rejected
    private long   requestedAt;   // timestamp millis for display

    public LeaveRequestModel() {}

    public LeaveRequestModel(String uid, String teacherName, String dateKey,
                             String type, String status, long requestedAt) {
        this.uid         = uid;
        this.teacherName = teacherName;
        this.dateKey     = dateKey;
        this.type        = type;
        this.status      = status;
        this.requestedAt = requestedAt;
    }

    // ── Getters ──────────────────────────────────────────────
    public String getUid()         { return uid; }
    public String getTeacherName() { return teacherName; }
    public String getDateKey()     { return dateKey; }
    public String getType()        { return type; }
    public String getStatus()      { return status; }
    public long   getRequestedAt() { return requestedAt; }

    // ── Setters ──────────────────────────────────────────────
    public void setUid(String uid)               { this.uid         = uid; }
    public void setTeacherName(String name)      { this.teacherName = name; }
    public void setDateKey(String dateKey)       { this.dateKey     = dateKey; }
    public void setType(String type)             { this.type        = type; }
    public void setStatus(String status)         { this.status      = status; }
    public void setRequestedAt(long requestedAt) { this.requestedAt = requestedAt; }

    // ── Helpers ──────────────────────────────────────────────
    public String getTypeLabel() {
        if (type == null) return "Leave";
        switch (type) {
            case "sick":           return "🤒  Sick Leave";
            case "marriage":       return "💍  Marriage Leave";
            case "home_emergency": return "🏠  Home Emergency";
            default:               return "Leave";
        }
    }
}