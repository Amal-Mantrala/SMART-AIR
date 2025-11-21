package com.example.b07demosummer2024.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.b07demosummer2024.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RoleSelectionFragment extends DialogFragment {

    private static final String ARG_USER_NAME = "user_name";
    private String userName;

    public static RoleSelectionFragment newInstance(String name) {
        RoleSelectionFragment fragment = new RoleSelectionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_NAME, name);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userName = getArguments().getString(ARG_USER_NAME);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String[] roles = new String[]{
                getString(R.string.role_child),
                getString(R.string.role_parent),
                getString(R.string.role_provider)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.choose_role)
                .setItems(roles, (dialog, which) -> {
                    Fragment target;
                    String roleToSave;
                    if (which == 0) {
                        target = new ChildHomeFragment();
                        roleToSave = "child";
                    } else if (which == 1) {
                        target = new ParentHomeFragment();
                        roleToSave = "parent";
                    } else {
                        target = new ProviderHomeFragment();
                        roleToSave = "provider";
                    }
                    // Save both role and name to Firestore
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        
                        // Create/update user document with role
                        java.util.Map<String, Object> userData = new java.util.HashMap<>();
                        userData.put("role", roleToSave);
                        if (userName != null && !userName.isEmpty()) {
                            userData.put("name", userName);
                        }
                        
                        db.collection("users").document(uid)
                            .set(userData, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                android.widget.Toast.makeText(getContext(), "Role saved: " + roleToSave, android.widget.Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                android.widget.Toast.makeText(getContext(), "Error saving role: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                            });
                    }

                    requireActivity().getSupportFragmentManager()
                            .popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, target)
                            .commit();
                })
                .setCancelable(false); // Prevent canceling - user MUST choose a role

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false); // Prevent dismissing by touching outside
        return dialog;
    }
}
