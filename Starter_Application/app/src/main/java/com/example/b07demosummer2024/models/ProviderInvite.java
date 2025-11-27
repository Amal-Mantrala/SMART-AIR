package com.example.b07demosummer2024.models;

import java.util.List;

public class ProviderInvite {
    private String inviteId;
    private String parentId;
    private String parentName;
    private String providerName;
    private String inviteCode;
    private String status; // pending, accepted, expired, revoked
    private List<String> sharedChildrenIds;
    private long createdAt;
    private long expiresAt;
    private String acceptedByProviderId;

    public ProviderInvite() {}

    public ProviderInvite(String inviteId, String parentId, String parentName, String providerName, 
                         String inviteCode, String status, List<String> sharedChildrenIds, 
                         long createdAt, long expiresAt) {
        this.inviteId = inviteId;
        this.parentId = parentId;
        this.parentName = parentName;
        this.providerName = providerName;
        this.inviteCode = inviteCode;
        this.status = status;
        this.sharedChildrenIds = sharedChildrenIds;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters
    public String getInviteId() {
        return inviteId;
    }

    public void setInviteId(String inviteId) {
        this.inviteId = inviteId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getSharedChildrenIds() {
        return sharedChildrenIds;
    }

    public void setSharedChildrenIds(List<String> sharedChildrenIds) {
        this.sharedChildrenIds = sharedChildrenIds;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getAcceptedByProviderId() {
        return acceptedByProviderId;
    }

    public void setAcceptedByProviderId(String acceptedByProviderId) {
        this.acceptedByProviderId = acceptedByProviderId;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}