package com.example.b07demosummer2024.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.auth.AuthService;
import com.google.firebase.auth.FirebaseAuth;

public class ParentHomeFragment extends ProtectedFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parent_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Spinner spinner = view.findViewById(R.id.dropdownMenu);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.placeholder_menu,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        Button signOut = view.findViewById(R.id.buttonSignOut);
        Button detailsButton = view.findViewById(R.id.buttonDetails);
        
        signOut.setOnClickListener(v -> {
            // Clear cached role data before signing out
            SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                prefs.edit().remove("user_role_" + auth.getCurrentUser().getUid()).apply();
            }
            
            new AuthService().signOut();
            FragmentManager fm = getParentFragmentManager();
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fm.beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .commit();
        });

        detailsButton.setOnClickListener(v -> showUserDetailsDialog());

        showTutorialIfNeeded("parent", R.string.parent_homepage);
    }

    private void showTutorialIfNeeded(String roleKey, int roleNameRes) {
        SharedPreferences prefs = requireContext().getSharedPreferences("tutorial_prefs", Context.MODE_PRIVATE);
        String key = "tutorial_seen_" + roleKey;
        if (!prefs.getBoolean(key, false)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.tutorial_title)
                    .setMessage(getString(R.string.tutorial_placeholder, getString(roleNameRes)))
                    .setPositiveButton(R.string.tutorial_got_it, (d, w) -> {
                        prefs.edit().putBoolean(key, true).apply();
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    private void showUserDetailsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_details, null);
        TextView emailText = dialogView.findViewById(R.id.textUserEmail);
        EditText nameEdit = dialogView.findViewById(R.id.editUserName);
        Button saveButton = dialogView.findViewById(R.id.buttonSave);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        // Get current user email
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            emailText.setText(auth.getCurrentUser().getEmail());
        }

        // Load saved name
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String savedName = prefs.getString("user_name_" + auth.getCurrentUser().getUid(), "");
        nameEdit.setText(savedName);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        saveButton.setOnClickListener(v -> {
            String name = nameEdit.getText().toString().trim();
            if (!name.isEmpty()) {
                // Save name to SharedPreferences
                prefs.edit().putString("user_name_" + auth.getCurrentUser().getUid(), name).apply();
                Toast.makeText(requireContext(), R.string.name_saved, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                nameEdit.setError("Name cannot be empty");
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
