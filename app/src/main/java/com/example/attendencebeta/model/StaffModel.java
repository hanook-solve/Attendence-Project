package com.example.attendencebeta.model;

public class StaffModel {
    private String uid;
    private String name;
    private String status; // "present" | "late" | "absent" | "not_marked"

    public StaffModel() {}

    public StaffModel(String uid, String name, String status) {
        this.uid    = uid;
        this.name   = name;
        this.status = status;
    }

    public String getUid()    { return uid; }
    public String getName()   { return name; }
    public String getStatus() { return status; }

    public void setUid(String uid)       { this.uid    = uid; }
    public void setName(String name)     { this.name   = name; }
    public void setStatus(String status) { this.status = status; }
}
