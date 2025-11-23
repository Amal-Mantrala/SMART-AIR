package com.example.b07demosummer2024.models;

import java.util.HashMap;
import java.util.Map;

public class SharingSettings {
    private String parentId;
    private String providerId;
    private Map<String, ChildSharingSettings> childSettings;
    private long lastUpdated;

    public SharingSettings() {
        this.childSettings = new HashMap<>();
    }

    public SharingSettings(String parentId, String providerId, Map<String, ChildSharingSettings> childSettings, long lastUpdated) {
        this.parentId = parentId;
        this.providerId = providerId;
        this.childSettings = childSettings != null ? childSettings : new HashMap<>();
        this.lastUpdated = lastUpdated;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Map<String, ChildSharingSettings> getChildSettings() {
        return childSettings;
    }

    public void setChildSettings(Map<String, ChildSharingSettings> childSettings) {
        this.childSettings = childSettings != null ? childSettings : new HashMap<>();
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
