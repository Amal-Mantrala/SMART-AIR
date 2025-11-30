package com.example.b07demosummer2024.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.adapters.InventoryAdapter;
import com.example.b07demosummer2024.models.MedicineCanister;
import com.example.b07demosummer2024.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryFragment extends Fragment implements InventoryAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private List<MedicineCanister> canisterList;
    private List<User> childrenList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewInventory);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        canisterList = new ArrayList<>();
        adapter = new InventoryAdapter(canisterList, this);
        recyclerView.setAdapter(adapter);

        childrenList = new ArrayList<>();

        view.findViewById(R.id.buttonAddInventory).setOnClickListener(v -> showAddInventoryDialog());

        loadChildrenAndInventory();
    }

    private void loadChildrenAndInventory() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String parentId = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(parentId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> childIds = (List<String>) documentSnapshot.get("children");
                if (childIds != null && !childIds.isEmpty()) {
                    for (String id : childIds) {
                        db.collection("users").document(id).get().addOnSuccessListener(childDoc -> {
                            if (childDoc.exists()) {
                                User user = childDoc.toObject(User.class);
                                if (user != null) {
                                    user.setParentId(childDoc.getId()); // Using parentId field to store child's own ID
                                    childrenList.add(user);
                                }
                            }
                        });
                    }
                }
                loadInventory(parentId);
            }
        });
    }

    private void loadInventory(String parentId) {
        FirebaseFirestore.getInstance().collection("inventory")
                .whereEqualTo("parentId", parentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        canisterList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            canisterList.add(document.toObject(MedicineCanister.class));
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void showAddInventoryDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_inventory, null);
        Spinner childSpinner = dialogView.findViewById(R.id.spinnerChildSelection);
        EditText medicineNameEdit = dialogView.findViewById(R.id.editMedicineName);
        EditText totalDosesEdit = dialogView.findViewById(R.id.editTotalDoses);
        DatePicker expiryDatePicker = dialogView.findViewById(R.id.datePickerExpiry);
        RadioGroup typeGroup = dialogView.findViewById(R.id.radioGroupInventoryMedicineType);
        Button saveButton = dialogView.findViewById(R.id.buttonSave);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        List<String> childNames = new ArrayList<>();
        for (User child : childrenList) {
            childNames.add(child.getName());
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, childNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        childSpinner.setAdapter(spinnerAdapter);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        saveButton.setOnClickListener(v -> {
            int selectedChildPosition = childSpinner.getSelectedItemPosition();
            if (selectedChildPosition < 0 || selectedChildPosition >= childrenList.size()) {
                Toast.makeText(getContext(), "Please select a child", Toast.LENGTH_SHORT).show();
                return;
            }
            String childId = childrenList.get(selectedChildPosition).getParentId();

            String medicineName = medicineNameEdit.getText().toString().trim();
            String totalDosesStr = totalDosesEdit.getText().toString().trim();
            int selectedTypeId = typeGroup.getCheckedRadioButtonId();

            if (medicineName.isEmpty() || totalDosesStr.isEmpty() || selectedTypeId == -1) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            String medicineType = (selectedTypeId == R.id.radioInventoryController) ? "controller" : "rescue";

            // Check for existing canister of the same type for this child
            for (MedicineCanister existingCanister : canisterList) {
                if (existingCanister.getChildId().equals(childId) && existingCanister.getMedicineType().equals(medicineType)) {
                    Toast.makeText(getContext(), "An inventory item of this type already exists for " + childNames.get(selectedChildPosition) + ". Please edit the existing one.", Toast.LENGTH_LONG).show();
                    return; // Stop the save process
                }
            }

            int totalDoses = Integer.parseInt(totalDosesStr);

            Calendar calendar = Calendar.getInstance();
            calendar.set(expiryDatePicker.getYear(), expiryDatePicker.getMonth(), expiryDatePicker.getDayOfMonth());
            long expiryDate = calendar.getTimeInMillis();

            FirebaseAuth auth = FirebaseAuth.getInstance();
            String parentId = auth.getCurrentUser().getUid();

            MedicineCanister canister = new MedicineCanister();
            canister.setParentId(parentId);
            canister.setChildId(childId);
            canister.setMedicineName(medicineName);
            canister.setMedicineType(medicineType);
            canister.setTotalDoses(totalDoses);
            canister.setDosesLeft(totalDoses);
            canister.setExpiryDate(expiryDate);
            canister.setPurchaseDate(System.currentTimeMillis());
            canister.setLastMarkedBy("parent");

            FirebaseFirestore.getInstance().collection("inventory").add(canister).addOnSuccessListener(documentReference -> {
                canister.setCanisterId(documentReference.getId());
                documentReference.update("canisterId", documentReference.getId());
                canisterList.add(canister);
                adapter.notifyDataSetChanged();
                dialog.dismiss();
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onItemClick(MedicineCanister canister) {
        showEditDosesDialog(canister);
    }

    private void showEditDosesDialog(MedicineCanister canister) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_doses, null);
        EditText dosesLeftEdit = dialogView.findViewById(R.id.editDosesLeft);
        Button saveButton = dialogView.findViewById(R.id.buttonSave);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        dosesLeftEdit.setText(String.valueOf(canister.getDosesLeft()));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        saveButton.setOnClickListener(v -> {
            String dosesLeftStr = dosesLeftEdit.getText().toString().trim();
            if (dosesLeftStr.isEmpty()) {
                dosesLeftEdit.setError("Cannot be empty");
                return;
            }

            int newDosesLeft = Integer.parseInt(dosesLeftStr);
            if (newDosesLeft > canister.getTotalDoses()) {
                dosesLeftEdit.setError("Cannot be more than total doses");
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            if (newDosesLeft == 0) {
                // If doses are 0, DELETE the canister from Firestore
                db.collection("inventory").document(canister.getCanisterId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            // Remove from local list and update UI
                            int position = canisterList.indexOf(canister);
                            if (position != -1) {
                                canisterList.remove(position);
                                adapter.notifyItemRemoved(position);
                            }
                            Toast.makeText(getContext(), canister.getMedicineName() + " removed from inventory.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to remove canister.", Toast.LENGTH_SHORT).show();
                        });
            } else {
                // Otherwise, UPDATE the canister as before
                db.collection("inventory").document(canister.getCanisterId())
                        .update("dosesLeft", newDosesLeft, "lastMarkedBy", "parent")
                        .addOnSuccessListener(aVoid -> {
                            canister.setDosesLeft(newDosesLeft);
                            canister.setLastMarkedBy("parent"); // Mark that the parent made the change
                            adapter.notifyDataSetChanged();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to update canister.", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void alertParentForLowInventory(MedicineCanister canister, String childName) {
        String parentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> alert = new HashMap<>();
        alert.put("parentId", parentId);
        alert.put("childId", canister.getChildId());
        alert.put("childName", childName);
        alert.put("message", "Low inventory for " + canister.getMedicineName() + " (" + canister.getDosesLeft() + " doses left)");
        alert.put("timestamp", System.currentTimeMillis());
        alert.put("type", "inventory_low");
        alert.put("read", false);

        db.collection("parentAlerts").add(alert);
    }
}
