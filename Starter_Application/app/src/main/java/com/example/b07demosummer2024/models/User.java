package com.example.b07demosummer2024.models;

public class User {
    private String uid;
    private String name;
    private String role;
    private String parentId;

    public User() {}

    public String getUid() { return uid; }

    public void setUid(String uid) { this.uid = uid; }

    public User(String name, String role, String parentId) {
        this.name = name;
        this.role = role;
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}
