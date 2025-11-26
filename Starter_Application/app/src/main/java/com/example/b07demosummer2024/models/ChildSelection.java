package com.example.b07demosummer2024.models;

public class ChildSelection {
    private String childId;
    private String childName;
    private boolean isSelected;

    public ChildSelection() {}

    public ChildSelection(String childId, String childName, boolean isSelected) {
        this.childId = childId;
        this.childName = childName;
        this.isSelected = isSelected;
    }

    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    public String getChildName() {
        return childName;
    }

    public void setChildName(String childName) {
        this.childName = childName;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}