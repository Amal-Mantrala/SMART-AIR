package com.example.b07demosummer2024.models;

public class ZoneLog {
    private String childId;
    private String zone;
    private int pefValue;
    private long timestamp;

    public ZoneLog() {}

    public ZoneLog(String childId, String zone, int pefValue, long timestamp) {
        this.childId = childId;
        this.zone = zone;
        this.pefValue = pefValue;
        this.timestamp = timestamp;
    }

    public String getChildId() { return childId; }
    public String getZone() { return zone; }
    public int getPefValue() { return pefValue; }
    public long getTimestamp() { return timestamp; }

    public void setChildId(String id) { this.childId = id; }
    public void setZone(String z) { this.zone = z; }
    public void setPefValue(int v) { this.pefValue = v; }
    public void setTimestamp(long t) { this.timestamp = t; }
}
