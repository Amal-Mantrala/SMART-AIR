package com.example.b07demosummer2024.models;

public class ProviderAccess {
    private String accessId;
    private String parentId;
    private String providerId;
    private String status;
    private String inviteCode;
    private long createdAt;

    public ProviderAccess() {}

    public ProviderAccess(String accessId, String parentId, String providerId, String status, String inviteCode, long createdAt) {
        this.accessId = accessId;
        this.parentId = parentId;
        this.providerId = providerId;
        this.status = status;
        this.inviteCode = inviteCode;
        this.createdAt = createdAt;
    }

    public String getAccessId() {
        return accessId;
    }

    public void setAccessId(String accessId) {
        this.accessId = accessId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
