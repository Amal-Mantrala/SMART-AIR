package com.example.b07demosummer2024.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.auth.AuthService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ChildrenManagementFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_children_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button addChildButton = view.findViewById(R.id.buttonAddChild);
        Button manageChildrenButton = view.findViewById(R.id.buttonManageChildren);
        Button backButton = view.findViewById(R.id.buttonBack);

        addChildButton.setOnClickListener(v -> showAddChildDialog());

        manageChildrenButton.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ManageChildrenFragment())
                    .addToBackStack(null)
                    .commit();
        });

        backButton.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });
    }

    private void showAddChildDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_child, null);
        EditText childNameEdit = dialogView.findViewById(R.id.editChildName);
        EditText childUsernameEdit = dialogView.findViewById(R.id.editChildUsername);
        EditText childPasswordEdit = dialogView.findViewById(R.id.editChildPassword);
        android.widget.DatePicker dobPicker = dialogView.findViewById(R.id.datePickerExpiry);
        EditText childNoteEdit = dialogView.findViewById(R.id.editChildNote);
        Button saveButton = dialogView.findViewById(R.id.buttonSave);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        saveButton.setOnClickListener(v -> {
            String childName = childNameEdit.getText().toString().trim();
            String childUsername = childUsernameEdit.getText().toString().trim();
            String childPassword = childPasswordEdit.getText().toString().trim();
            String childNote = childNoteEdit.getText().toString().trim();

            // Get the selected date from the DatePicker
            int day = dobPicker.getDayOfMonth();
            int month = dobPicker.getMonth();
            int year = dobPicker.getYear();

            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day);
            java.util.Date dobDate = calendar.getTime();

            // Calculate age in years
            double years = (double) (new java.util.Date().getTime() - dobDate.getTime()) / (365.25 * 24 * 60 * 60 * 1000.0);

            // Validate age: must be over 6 years and under 17 years
            if (!(years > 6.0 && years < 17.0)) {
                String errMsg = "Child must be older than 6 and younger than 17";
                if (isAdded()) Toast.makeText(requireContext(), errMsg, Toast.LENGTH_LONG).show();
                return;
            }

            if (childName.isEmpty()) {
                childNameEdit.setError("Child's name cannot be empty");
                return;
            }
            if (childUsername.isEmpty()) {
                childUsernameEdit.setError("Child's username cannot be empty");
                return;
            }
            if (childUsername.contains("@") || childUsername.contains(".")) {
                childUsernameEdit.setError("Username cannot contain '@' or '.'");
                return;
            }
            if (childPassword.isEmpty() || childPassword.length() < 6) {
                childPasswordEdit.setError("Password must be at least 6 characters");
                return;
            }

            // Check if username is already taken
            FirebaseFirestore.getInstance().collection("users").whereEqualTo("username", childUsername)
                    .get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    childUsernameEdit.setError("Username is already taken");
                    return;
                }

                String parentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String childEmail = childUsername + "@smart-air-child.com";

                new AuthService().createUserSilently(childEmail, childPassword, (success, message, createdUid) -> {
                if (success) {
                    // Use the uid returned by the silent create (don't rely on default auth currentUser)
                    String childId = createdUid;
                    if (childId == null) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to retrieve new child id", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    Map<String, Object> childData = new HashMap<>();
                    childData.put("name", childName);
                    childData.put("username", childUsername);
                    childData.put("role", "child");
                    childData.put("parentId", parentId);
                    // Save date of birth as ISO yyyy-MM-dd string
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                    childData.put("dob", sdf.format(dobDate));
                    if (!childNote.isEmpty()) childData.put("note", childNote);

                    FirebaseFirestore.getInstance().collection("users").document(childId)
                            .set(childData)
                            .addOnSuccessListener(aVoid -> {
                                FirebaseFirestore.getInstance().collection("users").document(parentId)
                                        .update("children", FieldValue.arrayUnion(childId))
                                        .addOnSuccessListener(aVoid2 -> {
                                            if (isAdded()) {
                                                Toast.makeText(requireContext(), "Child account created successfully.", Toast.LENGTH_LONG).show();
                                                dialog.dismiss();
                                            }
                                        })
                                        .addOnFailureListener(e2 -> {
                                            if (isAdded()) {
                                                Toast.makeText(requireContext(), "Failed to update parent data.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            })
                            .addOnFailureListener(e -> {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), "Failed to save child data", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to create child account: " + message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showChildSelectionForHistoryDialog() {
        // First, we need to load the parent's children
        String parentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("parentId", parentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(requireContext(), "No children found. Add a child first.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Create a simple selection dialog
                    String[] childNames = new String[queryDocumentSnapshots.size()];
                    String[] childIds = new String[queryDocumentSnapshots.size()];
                    
                    int i = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        childNames[i] = document.getString("name");
                        childIds[i] = document.getId();
                        i++;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle("Select Child")
                            .setItems(childNames, (dialog, which) -> {
                                // Navigate to ParentHealthHistoryFragment with selected child
                                getParentFragmentManager().beginTransaction()
                                        .replace(R.id.fragment_container, ParentHealthHistoryFragment.newInstance(childIds[which]))
                                        .addToBackStack(null)
                                        .commit();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load children", Toast.LENGTH_SHORT).show();
                });
    }
}