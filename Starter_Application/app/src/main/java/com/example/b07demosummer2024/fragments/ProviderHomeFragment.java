package com.example.b07demosummer2024.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.auth.AuthService;
import com.example.b07demosummer2024.auth.ProviderSharingService;
import com.example.b07demosummer2024.services.ProviderInviteService;
import com.example.b07demosummer2024.models.ProviderInvite;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProviderHomeFragment extends ProtectedFragment {
    private ProviderSharingService sharingService;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_provider_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharingService = new ProviderSharingService();

        // Initialize views
        TextView greetingText = view.findViewById(R.id.textGreeting);

        Button signOut = view.findViewById(R.id.buttonSignOut);
        Button detailsButton = view.findViewById(R.id.buttonDetails);
        Button informationButton = view.findViewById(R.id.buttonInformation);
        Button acceptInviteButton = view.findViewById(R.id.buttonAcceptInvite);
        Button viewPatientDataButton = view.findViewById(R.id.buttonViewPatientData);
        
        // Load user name and set greeting
        loadUserNameAndSetGreeting(greetingText);
        
        signOut.setOnClickListener(v -> signOutAndReturnToLogin());

        detailsButton.setOnClickListener(v -> showUserDetailsDialog());
        informationButton.setOnClickListener(v -> showTutorial());
        acceptInviteButton.setOnClickListener(v -> showAcceptInviteDialog());
        viewPatientDataButton.setOnClickListener(v -> showPatientSelection());

        showTutorialIfFirstTime();
    }

    /**
     * Load user name from Firestore and set greeting text
     */
    private void loadUserNameAndSetGreeting(TextView greetingText) {
        if (auth.getCurrentUser() != null) {
            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists() && isAdded()) {
                            String name = document.getString("name");
                            if (name != null && !name.isEmpty()) {
                                String greeting = getString(R.string.provider_greeting, name);
                                greetingText.setText(greeting);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Keep default greeting if Firestore fails
                    });
        }
    }

    /**
     * Show tutorial dialog with provider-specific content
     */
    private void showTutorial() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.tutorial_title)
                .setMessage(R.string.provider_tutorial_content)
                .setPositiveButton(R.string.tutorial_got_it, null)
                .setCancelable(true)
                .show();
    }

    /**
     * Show tutorial if it's the first time for provider role
     */
    private void showTutorialIfFirstTime() {
        SharedPreferences prefs = requireContext().getSharedPreferences("tutorial_prefs", Context.MODE_PRIVATE);
        String key = "tutorial_seen_provider";
        if (!prefs.getBoolean(key, false)) {
            showTutorial();
            prefs.edit().putBoolean(key, true).apply();
        }
    }

    /**
     * Common sign out functionality
     */
    private void signOutAndReturnToLogin() {
        // Clear cached role data before signing out
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        if (auth.getCurrentUser() != null) {
            prefs.edit().remove("user_role_" + auth.getCurrentUser().getUid()).apply();
        }
        
        new AuthService().signOut();
        FragmentManager fm = getParentFragmentManager();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fm.beginTransaction()
                .replace(R.id.fragment_container, new LoginFragment())
                .commit();
    }

    /**
     * Show user details dialog for editing profile
     */
    private void showUserDetailsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_details, null);
        TextView emailText = dialogView.findViewById(R.id.textUserEmail);
        EditText nameEdit = dialogView.findViewById(R.id.editUserName);
        Button saveButton = dialogView.findViewById(R.id.buttonSave);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        if (auth.getCurrentUser() != null) {
            // Set email
            emailText.setText(auth.getCurrentUser().getEmail());
            
            // Load name from Firestore
            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String name = document.getString("name");
                            if (name != null) {
                                nameEdit.setText(name);
                            }
                        }
                    });
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        saveButton.setOnClickListener(v -> {
            String newName = nameEdit.getText().toString().trim();
            if (!newName.isEmpty() && auth.getCurrentUser() != null) {
                db.collection("users")
                        .document(auth.getCurrentUser().getUid())
                        .update("name", newName)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(requireContext(), R.string.name_saved, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            // Refresh greeting
                            View fragmentView = getView();
                            if (fragmentView != null) {
                                TextView greetingText = fragmentView.findViewById(R.id.textGreeting);
                                if (greetingText != null) {
                                    loadUserNameAndSetGreeting(greetingText);
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "Failed to save name", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showAcceptInviteDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_accept_invite, null);
        EditText inviteCodeEdit = dialogView.findViewById(R.id.editInviteCode);
        Button acceptButton = dialogView.findViewById(R.id.buttonAccept);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        acceptButton.setOnClickListener(v -> {
            String inviteCode = inviteCodeEdit.getText().toString().trim().toUpperCase();

            if (inviteCode.isEmpty()) {
                inviteCodeEdit.setError("Please enter the invite code");
                return;
            }

            if (inviteCode.length() != 8) {
                inviteCodeEdit.setError("Invite code must be 8 characters");
                return;
            }

            acceptButton.setEnabled(false);
            acceptButton.setText("Accepting...");

            String providerId = auth.getCurrentUser().getUid();

            ProviderInviteService inviteService = new ProviderInviteService();
            inviteService.acceptInvite(inviteCode, providerId, new ProviderInviteService.BooleanCallback() {
                @Override
                public void onResult(boolean success) {
                    if (isAdded()) {
                        if (success) {
                            Toast.makeText(requireContext(), "Invite accepted! You now have access to the shared patient data.", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        } else {
                            acceptButton.setEnabled(true);
                            acceptButton.setText("Accept Invite");
                            inviteCodeEdit.setError("Failed to accept invite. Please try again.");
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    if (isAdded()) {
                        acceptButton.setEnabled(true);
                        acceptButton.setText("Accept Invite");
                        inviteCodeEdit.setError(error);
                        Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
    
    private void showPatientSelection() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ProviderPatientSelectionFragment())
                .addToBackStack(null)
                .commit();
    }
}