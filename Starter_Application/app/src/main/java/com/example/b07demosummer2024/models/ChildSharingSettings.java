package com.example.b07demosummer2024.models;

import java.util.HashMap;
import java.util.Map;

public class ChildSharingSettings {
    private String childId;
    private Map<String, Boolean> sharedFields;

    public ChildSharingSettings() {
        this.sharedFields = new HashMap<>();
    }

    public ChildSharingSettings(String childId, Map<String, Boolean> sharedFields) {
        this.childId = childId;
        this.sharedFields = sharedFields != null ? sharedFields : new HashMap<>();
    }

    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    public Map<String, Boolean> getSharedFields() {
        return sharedFields;
    }

    public void setSharedFields(Map<String, Boolean> sharedFields) {
        this.sharedFields = sharedFields != null ? sharedFields : new HashMap<>();
    }
}
