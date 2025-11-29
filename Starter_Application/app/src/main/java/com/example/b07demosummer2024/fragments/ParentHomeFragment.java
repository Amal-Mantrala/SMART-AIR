package com.example.b07demosummer2024.fragments;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.adapters.ChildSelectionAdapter;
import com.example.b07demosummer2024.auth.AuthService;
import com.example.b07demosummer2024.models.ChildSelection;
import com.example.b07demosummer2024.models.ProviderInvite;
import com.example.b07demosummer2024.services.ProviderInviteService;
import com.example.b07demosummer2024.models.User;
import com.example.b07demosummer2024.services.ProviderInviteService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

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

        Button signOut = view.findViewById(R.id.buttonSignOut);
        Button detailsButton = view.findViewById(R.id.buttonDetails);
        Button informationButton = view.findViewById(R.id.buttonInformation);
        Button newChildButton = view.findViewById(R.id.buttonAddChild);
        Button linkChildButton = view.findViewById(R.id.buttonLinkChild);
        Button manageChildrenButton = view.findViewById(R.id.buttonManageChildren);
        Button privacySharingButton = view.findViewById(R.id.buttonPrivacySharing);
        Button inviteProviderButton = view.findViewById(R.id.buttonInviteProvider);
        Button viewAlertsButton = view.findViewById(R.id.buttonViewAlerts);

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
        inviteProviderButton.setOnClickListener(v -> showInviteProviderDialog());
        viewAlertsButton.setOnClickListener(v -> showAlertsDialog());

        showTutorialIfFirstTime();
        checkForAlerts(viewAlertsButton);
    }

    private void checkForAlerts(Button alertButton) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String parentId = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("parentAlerts")
                .whereEqualTo("parentId", parentId)
                .whereEqualTo("read", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && isAdded()) {
                        int unreadCount = task.getResult().size();
                        if (unreadCount > 0) {
                            alertButton.setText(getString(R.string.parent_alerts) + " (" + unreadCount + ")");
                        } else {
                            alertButton.setText(getString(R.string.parent_alerts));
                        }
                    }
                });
    }

    private void showAlertsDialog() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String parentId = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("parentAlerts")
                .whereEqualTo("parentId", parentId)
                .limit(20)
                .get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    
                    if (task.isSuccessful()) {
                        List<Map<String, Object>> alerts = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> alert = document.getData();
                            alert.put("alertId", document.getId());
                            alerts.add(alert);
                        }

                        if (alerts.isEmpty()) {
                            Toast.makeText(requireContext(), getString(R.string.no_alerts), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        alerts.sort((a, b) -> {
                            Long timestampA = (Long) a.get("timestamp");
                            Long timestampB = (Long) b.get("timestamp");
                            if (timestampA == null && timestampB == null) return 0;
                            if (timestampA == null) return 1;
                            if (timestampB == null) return -1;
                            return timestampB.compareTo(timestampA);
                        });

                        StringBuilder alertText = new StringBuilder();
                        for (Map<String, Object> alert : alerts) {
                            String childName = (String) alert.get("childName");
                            String message = (String) alert.get("message");
                            Long timestamp = (Long) alert.get("timestamp");
                            
                            if (childName == null) childName = "Your child";
                            if (message == null) message = "Alert";
                            
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault());
                            String timeStr = timestamp != null ? sdf.format(new java.util.Date(timestamp)) : "Unknown time";
                            
                            alertText.append(childName).append(": ").append(message).append("\n");
                            alertText.append("Time: ").append(timeStr).append("\n\n");
                        }

                        AlertDialog alertDialog = new AlertDialog.Builder(requireContext())
                                .setTitle(getString(R.string.parent_alerts))
                                .setMessage(alertText.toString())
                                .setPositiveButton("Mark as Read", null)
                                .setNegativeButton("Close", null)
                                .create();
                        
                        alertDialog.setOnShowListener(dialog -> {
                            Button markAsReadButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            markAsReadButton.setOnClickListener(v -> {
                                for (Map<String, Object> alert : alerts) {
                                    String alertId = (String) alert.get("alertId");
                                    if (alertId != null) {
                                        db.collection("parentAlerts")
                                                .document(alertId)
                                                .update("read", true);
                                    }
                                }
                                
                                if (isAdded() && getView() != null) {
                                    Button alertButton = getView().findViewById(R.id.buttonViewAlerts);
                                    if (alertButton != null) {
                                        alertButton.setText(getString(R.string.parent_alerts));
                                        checkForAlerts(alertButton);
                                    }
                                }
                                
                                Toast.makeText(requireContext(), "Alerts marked as read", Toast.LENGTH_SHORT).show();
                                alertDialog.dismiss();
                            });
                        });
                        
                        alertDialog.show();
                    } else {
                        Exception e = task.getException();
                        String errorMsg = e != null ? e.getMessage() : "Unknown error";
                        Toast.makeText(requireContext(), "Error loading alerts: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
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

    private void showInviteProviderDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_invite_provider, null);
        EditText providerNameEdit = dialogView.findViewById(R.id.editProviderName);
        RecyclerView recyclerViewChildren = dialogView.findViewById(R.id.recyclerViewChildren);
        LinearLayout layoutInviteCode = dialogView.findViewById(R.id.layoutInviteCode);
        TextView textInviteCode = dialogView.findViewById(R.id.textInviteCode);
        Button generateButton = dialogView.findViewById(R.id.buttonGenerateInvite);
        Button copyButton = dialogView.findViewById(R.id.buttonCopyCode);
        Button shareButton = dialogView.findViewById(R.id.buttonShareCode);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        // Setup RecyclerView for children selection
        recyclerViewChildren.setLayoutManager(new LinearLayoutManager(requireContext()));
        List<ChildSelection> childrenList = new ArrayList<>();
        ChildSelectionAdapter adapter = new ChildSelectionAdapter(childrenList);
        recyclerViewChildren.setAdapter(adapter);

        // Load children
        loadChildrenForSelection(childrenList, adapter);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        final ProviderInvite[] generatedInvite = {null};

        generateButton.setOnClickListener(v -> {
            String providerName = providerNameEdit.getText().toString().trim();
            List<String> selectedChildrenIds = new ArrayList<>();
            
            for (ChildSelection child : childrenList) {
                if (child.isSelected()) {
                    selectedChildrenIds.add(child.getChildId());
                }
            }

            if (selectedChildrenIds.isEmpty()) {
                Toast.makeText(requireContext(), "Please select at least one child to share with the provider", Toast.LENGTH_SHORT).show();
                return;
            }

            generateButton.setEnabled(false);
            generateButton.setText("Generating...");

            FirebaseAuth auth = FirebaseAuth.getInstance();
            String parentId = auth.getCurrentUser().getUid();
            String parentName = auth.getCurrentUser().getDisplayName();
            if (parentName == null || parentName.isEmpty()) {
                // Try to get name from Firestore
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(parentId)
                        .get()
                        .addOnSuccessListener(document -> {
                            String name = document.exists() ? document.getString("name") : "Parent";
                            createInvite(parentId, name, providerName, selectedChildrenIds, 
                                       layoutInviteCode, textInviteCode, generateButton, generatedInvite);
                        });
            } else {
                createInvite(parentId, parentName, providerName, selectedChildrenIds, 
                           layoutInviteCode, textInviteCode, generateButton, generatedInvite);
            }
        });

        copyButton.setOnClickListener(v -> {
            if (generatedInvite[0] != null) {
                copyInviteCodeToClipboard(generatedInvite[0].getInviteCode());
            }
        });

        shareButton.setOnClickListener(v -> {
            if (generatedInvite[0] != null) {
                shareInviteCode(generatedInvite[0]);
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void loadChildrenForSelection(List<ChildSelection> childrenList, ChildSelectionAdapter adapter) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String parentId = auth.getCurrentUser().getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(parentId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.contains("children")) {
                        @SuppressWarnings("unchecked")
                        List<String> childrenIds = (List<String>) document.get("children");
                        if (childrenIds != null && !childrenIds.isEmpty()) {
                            loadChildrenDetails(childrenIds, childrenList, adapter);
                        }
                    }
                });
    }

    private void loadChildrenDetails(List<String> childrenIds, List<ChildSelection> childrenList, 
                                   ChildSelectionAdapter adapter) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        int[] completed = {0};
        int total = childrenIds.size();

        for (String childId : childrenIds) {
            db.collection("users")
                    .document(childId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String childName = document.getString("name");
                            if (childName == null || childName.isEmpty()) {
                                childName = "Child " + childId.substring(0, 6); // Fallback name
                            }
                            childrenList.add(new ChildSelection(childId, childName, false));
                        }
                        completed[0]++;
                        if (completed[0] == total) {
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    private void createInvite(String parentId, String parentName, String providerName, 
                             List<String> selectedChildrenIds, LinearLayout layoutInviteCode, 
                             TextView textInviteCode, Button generateButton, 
                             ProviderInvite[] generatedInvite) {
        ProviderInviteService inviteService = new ProviderInviteService();
        inviteService.createProviderInvite(parentId, parentName, providerName, selectedChildrenIds, 
                new ProviderInviteService.InviteCallback() {
                    @Override
                    public void onSuccess(ProviderInvite invite) {
                        if (isAdded()) {
                            generatedInvite[0] = invite;
                            textInviteCode.setText(invite.getInviteCode());
                            layoutInviteCode.setVisibility(View.VISIBLE);
                            generateButton.setText("Generate New Invite");
                            generateButton.setEnabled(true);
                            Toast.makeText(requireContext(), "Invite code generated successfully!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded()) {
                            generateButton.setText("Generate Invite");
                            generateButton.setEnabled(true);
                            Toast.makeText(requireContext(), "Failed to generate invite: " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void copyInviteCodeToClipboard(String inviteCode) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Provider Invite Code", inviteCode);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(requireContext(), "Invite code copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void shareInviteCode(ProviderInvite invite) {
        String shareText = "SMART-AIR Provider Invite\n\n" +
                "You have been invited to access patient data by " + invite.getParentName() + ".\n\n" +
                "Invite Code: " + invite.getInviteCode() + "\n\n" +
                "This code will expire in 7 days. Please enter this code in the SMART-AIR app to gain access to the shared patient information.";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "SMART-AIR Provider Invite");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share Invite Code"));
    }
}
