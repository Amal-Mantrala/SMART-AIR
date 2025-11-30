package com.example.b07demosummer2024.models;

public class MedicineCanister {
    private String canisterId;
    private String parentId;
    private String childId;
    private String medicineName;
    private String medicineType; // "controller" or "rescue"
    private long purchaseDate;
    private long expiryDate;
    private int totalDoses;
    private int dosesLeft;
    private String lastMarkedBy; // "parent" or "child"

    public MedicineCanister() {}

    // Getters and Setters
    public String getCanisterId() { return canisterId; }
    public void setCanisterId(String canisterId) { this.canisterId = canisterId; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getChildId() { return childId; }
    public void setChildId(String childId) { this.childId = childId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getMedicineType() { return medicineType; }
    public void setMedicineType(String medicineType) { this.medicineType = medicineType; }

    public long getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(long purchaseDate) { this.purchaseDate = purchaseDate; }

    public long getExpiryDate() { return expiryDate; }
    public void setExpiryDate(long expiryDate) { this.expiryDate = expiryDate; }

    public int getTotalDoses() { return totalDoses; }
    public void setTotalDoses(int totalDoses) { this.totalDoses = totalDoses; }

    public int getDosesLeft() { return dosesLeft; }
    public void setDosesLeft(int dosesLeft) { this.dosesLeft = dosesLeft; }

    public String getLastMarkedBy() { return lastMarkedBy; }
    public void setLastMarkedBy(String lastMarkedBy) { this.lastMarkedBy = lastMarkedBy; }
}
