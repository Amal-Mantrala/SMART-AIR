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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import com.google.firebase.firestore.QuerySnapshot;

public class ParentHomeFragment extends ProtectedFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parent_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        TextView greetingText = view.findViewById(R.id.textGreeting);
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
        Button informationButton = view.findViewById(R.id.buttonInformation);
        Button newChildButton = view.findViewById(R.id.buttonAddChild);
        Button linkChildButton = view.findViewById(R.id.buttonLinkChild);
        Button manageChildrenButton = view.findViewById(R.id.buttonManageChildren);
        Button privacySharingButton = view.findViewById(R.id.buttonPrivacySharing);

        // Load user name and set greeting
        loadUserNameAndSetGreeting(greetingText);

        signOut.setOnClickListener(v -> {
            signOutAndReturnToLogin();
        });

        detailsButton.setOnClickListener(v -> showUserDetailsDialog());
        informationButton.setOnClickListener(v -> showTutorial());
        newChildButton.setOnClickListener(v -> showAddChildDialog());
        linkChildButton.setOnClickListener(v -> showLinkChildDialog());
        manageChildrenButton.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ManageChildrenFragment())
                    .addToBackStack(null)
                    .commit();
        });
        privacySharingButton.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new PrivacySettingsFragment())
                    .addToBackStack(null)
                    .commit();
        });


        showTutorialIfFirstTime();
    }

    private void showLinkChildDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_link_child, null);
        EditText childEmailEdit = dialogView.findViewById(R.id.editChildEmail);
        Button sendButton = dialogView.findViewById(R.id.buttonSendInvitation);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        sendButton.setOnClickListener(v -> {
            String childEmail = childEmailEdit.getText().toString().trim();
            if (childEmail.isEmpty()) {
                childEmailEdit.setError("Email cannot be empty");
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").whereEqualTo("email", childEmail).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot.isEmpty()) {
                        childEmailEdit.setError("No user found with this email");
                        return;
                    }
                    String role = snapshot.getDocuments().get(0).getString("role");
                    if (!"child".equals(role)) {
                        childEmailEdit.setError("This user is not a child");
                        return;
                    }

                    FirebaseAuth auth = FirebaseAuth.getInstance();
                    String parentId = auth.getCurrentUser().getUid();
                    String parentName = auth.getCurrentUser().getDisplayName(); // Or fetch from your DB

                    Map<String, Object> invitation = new HashMap<>();
                    invitation.put("parentUid", parentId);
                    invitation.put("parentName", parentName);
                    invitation.put("childEmail", childEmail);
                    invitation.put("status", "pending");

                    db.collection("invitations").add(invitation).addOnSuccessListener(documentReference -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Invitation sent!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    }).addOnFailureListener(e -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to send invitation.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showAddChildDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_child, null);
        EditText childNameEdit = dialogView.findViewById(R.id.editChildName);
        EditText childUsernameEdit = dialogView.findViewById(R.id.editChildUsername);
        EditText childPasswordEdit = dialogView.findViewById(R.id.editChildPassword);
        Button saveButton = dialogView.findViewById(R.id.buttonSave);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        saveButton.setOnClickListener(v -> {
            String childName = childNameEdit.getText().toString().trim();
            String childUsername = childUsernameEdit.getText().toString().trim();
            String childPassword = childPasswordEdit.getText().toString().trim();

            if (childName.isEmpty()) {
                childNameEdit.setError("Child\'s name cannot be empty");
                return;
            }
            if (childUsername.isEmpty()) {
                childUsernameEdit.setError("Child\'s username cannot be empty");
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

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").whereEqualTo("username", childUsername).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    childUsernameEdit.setError("Username is already taken");
                    return;
                }

                FirebaseAuth auth = FirebaseAuth.getInstance();
                String parentId = auth.getCurrentUser().getUid();
                String childEmail = childUsername + "@smart-air-child.com";

                AuthService authService = new AuthService();
                authService.createUser(childEmail, childPassword, (success, message) -> {
                    if (success) {
                        String childId = auth.getCurrentUser().getUid(); // This is now the child's UID
                        Map<String, Object> childData = new HashMap<>();
                        childData.put("name", childName);
                        childData.put("username", childUsername);
                        childData.put("role", "child");
                        childData.put("parentId", parentId);
                        db.collection("users").document(childId)
                                .set(childData)
                                .addOnSuccessListener(aVoid -> {
                                    db.collection("users").document(parentId)
                                            .update("children", FieldValue.arrayUnion(childId))
                                            .addOnSuccessListener(aVoid2 -> {
                                                if (isAdded()) {
                                                    Toast.makeText(requireContext(), "Child account created. For security, please log in again.", Toast.LENGTH_LONG).show();
                                                    dialog.dismiss();
                                                    signOutAndReturnToLogin();
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

    private void signOutAndReturnToLogin() {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            prefs.edit().remove("user_role_" + auth.getCurrentUser().getUid()).apply();
        }

        new AuthService().signOut();

        if (getParentFragmentManager() == null) return;
        FragmentManager fm = getParentFragmentManager();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fm.beginTransaction()
                .replace(R.id.fragment_container, new LoginFragment())
                .commit();
    }

    private void loadUserNameAndSetGreeting(TextView greetingText) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists() && isAdded()) {
                            String name = document.getString("name");
                            if (name != null && !name.isEmpty()) {
                                String greeting = getString(R.string.parent_greeting, name);
                                greetingText.setText(greeting);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Keep default greeting if Firestore fails
                    });
        }
    }

    private void showTutorialIfFirstTime() {
        SharedPreferences prefs = requireContext().getSharedPreferences("tutorial_prefs", Context.MODE_PRIVATE);
        String key = "tutorial_seen_parent";
        if (!prefs.getBoolean(key, false)) {
            showTutorial();
            prefs.edit().putBoolean(key, true).apply();
        }
    }

    private void showTutorial() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.tutorial_title)
                .setMessage(R.string.parent_tutorial_content)
                .setPositiveButton(R.string.tutorial_got_it, null)
                .setCancelable(true)
                .show();
    }

    private void showUserDetailsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_details, null);
        TextView emailText = dialogView.findViewById(R.id.textUserEmail);
        EditText nameEdit = dialogView.findViewById(R.id.editUserName);
        Button saveButton = dialogView.findViewById(R.id.buttonSave);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            // Set email
            emailText.setText(auth.getCurrentUser().getEmail());

            // Load name from Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
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
            String name = nameEdit.getText().toString().trim();
            if (!name.isEmpty()) {
                // Save name to Firestore
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users")
                        .document(auth.getCurrentUser().getUid())
                        .update("name", name)
                        .addOnSuccessListener(aVoid -> {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), R.string.name_saved, Toast.LENGTH_SHORT).show();
                                // Refresh the greeting with new name
                                TextView greetingText = getView().findViewById(R.id.textGreeting);
                                if (greetingText != null) {
                                    String greeting = getString(R.string.parent_greeting, name);
                                    greetingText.setText(greeting);
                                }
                                dialog.dismiss();
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), "Failed to save name", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                nameEdit.setError("Name cannot be empty");
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
