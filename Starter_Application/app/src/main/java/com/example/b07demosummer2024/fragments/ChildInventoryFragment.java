package com.example.b07demosummer2024.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.adapters.InventoryAdapter;
import com.example.b07demosummer2024.models.MedicineCanister;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChildInventoryFragment extends Fragment implements InventoryAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private List<MedicineCanister> canisterList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_child_inventory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewChildInventory);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        canisterList = new ArrayList<>();
        adapter = new InventoryAdapter(canisterList, this);
        recyclerView.setAdapter(adapter);

        loadChildInventory();
    }

    private void loadChildInventory() {
        String childId = com.example.b07demosummer2024.auth.ImpersonationService.getActiveChildId(requireContext());
        FirebaseFirestore.getInstance().collection("inventory")
                .whereEqualTo("childId", childId)
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

    @Override
    public void onItemClick(MedicineCanister canister) {
        showEditDosesDialog(canister);
    }

    private void showEditDosesDialog(MedicineCanister canister) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_doses_child, null);
        EditText dosesLeftEdit = dialogView.findViewById(R.id.editDosesLeft);
        Button saveButton = dialogView.findViewById(R.id.buttonSave);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        dosesLeftEdit.setText(String.valueOf(canister.getDosesLeft()));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

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

            if (newDosesLeft == 0) {
                // If doses are 0, DELETE the canister from Firestore
                FirebaseFirestore.getInstance().collection("inventory").document(canister.getCanisterId())
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
                FirebaseFirestore.getInstance().collection("inventory").document(canister.getCanisterId())
                        .update("dosesLeft", newDosesLeft, "lastMarkedBy", "child")
                        .addOnSuccessListener(aVoid -> {
                            canister.setDosesLeft(newDosesLeft);
                            canister.setLastMarkedBy("child");
                            adapter.notifyDataSetChanged();
                            dialog.dismiss();
                        });
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
