package com.example.b07demosummer2024.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.b07demosummer2024.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RoleSelectionFragment extends DialogFragment {

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
                    // Persist selected role to Firebase Realtime Database under users/{uid}/role
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference ref = FirebaseDatabase.getInstance("https://b07-demo-summer-2024-default-rtdb.firebaseio.com/")
                                .getReference("users").child(uid).child("role");
                        ref.setValue(roleToSave);

                        // Cache role locally for faster startup / offline
                        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                        prefs.edit().putString("user_role_" + uid, roleToSave).apply();
                    }

                    requireActivity().getSupportFragmentManager()
                            .popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, target)
                            .commit();
                })
                .setCancelable(false); // Prevent canceling - user MUST choose a role

        return builder.create();
    }
}
