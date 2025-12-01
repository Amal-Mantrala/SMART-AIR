package com.example.b07demosummer2024.fragments;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.example.b07demosummer2024.models.TriageIncident;
import com.example.b07demosummer2024.services.ProviderInviteService;
import com.example.b07demosummer2024.services.TriageService;
import com.example.b07demosummer2024.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class ParentHomeFragment extends ProtectedFragment {

    private List<String> childIds = new ArrayList<>();
    private List<String> childNames = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private String selectedChildUid;
    TextView zoneText;
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
        Button manageChildrenButton = view.findViewById(R.id.buttonManageChildren);
        Button privacySharingButton = view.findViewById(R.id.buttonPrivacySharing);
        Button inviteProviderButton = view.findViewById(R.id.buttonInviteProvider);
        Button viewAlertsButton = view.findViewById(R.id.buttonViewAlerts);
        Button viewTriageLogsButton = view.findViewById(R.id.buttonViewTriageLogs);
        Button setPB = view.findViewById(R.id.buttonSetPB);
        Spinner childSpinner = view.findViewById(R.id.dropdownMenu);
        Button inventoryButton = view.findViewById(R.id.buttonInventory);
        zoneText = view.findViewById(R.id.textZoneValue);

        spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                childNames
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        childSpinner.setAdapter(spinnerAdapter);

        loadChildrenForParent();

        childSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos < childIds.size()) {
                    selectedChildUid = childIds.get(pos);
                    loadChildZone(selectedChildUid);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                zoneText.setText("Zone: --");
                selectedChildUid = null;
            }
        });

        // Load user name and set greeting
        loadUserNameAndSetGreeting(greetingText);

        signOut.setOnClickListener(v -> {
            signOutAndReturnToLogin();
        });

        setPB.setOnClickListener( v -> showSetPBDialog());
        detailsButton.setOnClickListener(v -> showUserDetailsDialog());
        informationButton.setOnClickListener(v -> showTutorial());
        newChildButton.setOnClickListener(v -> showAddChildDialog());
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
        viewTriageLogsButton.setOnClickListener(v -> showTriageLogsDialog());
        inventoryButton.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new InventoryFragment())
                    .addToBackStack(null)
                    .commit();
        });
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

    private void loadChildrenForParent() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String parentUid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(parentUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    List<String> children = (List<String>) snapshot.get("children");
                    if (children == null || children.isEmpty()) {
                        childNames.clear();
                        childIds.clear();
                        spinnerAdapter.notifyDataSetChanged();
                        zoneText.setText("Zone: --");
                        return;
                    }

                    childIds.clear();
                    childNames.clear();

                    for (String childUid : children) {
                        loadSingleChildInfo(childUid);
                    }

                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed loading children.", Toast.LENGTH_SHORT).show()
                );
    };

    private void loadSingleChildInfo(String childUid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(childUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String name = doc.getString("name");
                    if (name == null) name = "Child";

                    childIds.add(childUid);
                    childNames.add(name);
                    spinnerAdapter.notifyDataSetChanged();
                });
    }
    private void loadChildZone(String childUid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(childUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        zoneText.setText("Zone: --");
                        return;
                    }

                    String zone = doc.getString("zone");
                    String lastZoneDate = doc.getString("lastZoneDate");

                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(new Date());

                    if (lastZoneDate == null || !lastZoneDate.equals(today)) {
                        // New day — reset zone
                        zoneText.setText("Zone: --");
                        updateZoneUI("none");

                        // reset value in Firestore too
                        db.collection("users")
                                .document(childUid)
                                .update("zone", null,
                                        "lastZoneDate", today);
                    } else {
                        // Same day — show stored zone
                        if (zone != null) {
                            zoneText.setText("Zone: " + zone);
                            updateZoneUI(zone);
                        } else {
                            zoneText.setText("Zone: --");
                            updateZoneUI("none");
                        }
                    }
                });
    }
    private void updateZoneUI(String zone) {
        switch (zone) {
            case "Green":
                zoneText.setTextColor(Color.parseColor("#2ecc71")); // green
                break;
            case "Yellow":
                zoneText.setTextColor(Color.parseColor("#f1c40f")); // yellow
                break;
            case "Red":
                zoneText.setTextColor(Color.parseColor("#e74c3c")); // red
                break;
            case "none" :
                zoneText.setTextColor(Color.parseColor("#000000"));
        }
    }

    private void showSetPBDialog() {
        if (selectedChildUid == null) {
            Toast.makeText(getContext(), "Please select a child first", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_set_pb, null);
        EditText pbInput = dialogView.findViewById(R.id.editPBValue);

        new AlertDialog.Builder(requireContext())
                .setTitle("Set Personal Best (PB)")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String text = pbInput.getText().toString().trim();

                    if (text.isEmpty()) {
                        Toast.makeText(getContext(), "PB cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int pbValue;
                    try {
                        pbValue = Integer.parseInt(text);
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "PB must be a number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (pbValue <=0 || pbValue > 800) {
                        Toast.makeText(getContext(), "PB must be a number between 0 and 800", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    savePBToFirestore(selectedChildUid, pbValue);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void savePBToFirestore(String childUid, int pbValue) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(childUid)
                .update("pb", pbValue)
                .addOnSuccessListener(a -> {
                    Toast.makeText(getContext(), "PB updated!", Toast.LENGTH_SHORT).show();
                    loadChildZone(childUid);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to update PB", Toast.LENGTH_SHORT).show());
    }

    private void showTriageLogsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_triage_list, null);
        Spinner childSpinner = dialogView.findViewById(R.id.spinnerChild);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerViewTriage);
        TextView emptyView = dialogView.findViewById(R.id.textEmpty);
        Button closeButton = dialogView.findViewById(R.id.buttonClose);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        TriageListAdapter adapter = new TriageListAdapter();
        recyclerView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        closeButton.setOnClickListener(v -> dialog.dismiss());

        List<String> dialogChildIds = new ArrayList<>();
        List<String> dialogChildNames = new ArrayList<>();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                dialogChildNames
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        childSpinner.setAdapter(spinnerAdapter);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String parentUid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(parentUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
                    
                    if (snapshot.exists()) {
                        List<String> children = (List<String>) snapshot.get("children");
                        if (children != null && !children.isEmpty()) {
                            dialogChildIds.clear();
                            dialogChildNames.clear();
                            
                            Map<String, String> childNameMap = new HashMap<>();
                            int[] completed = {0};
                            int total = children.size();
                            
                            for (String childUid : children) {
                                db.collection("users")
                                        .document(childUid)
                                        .get()
                                        .addOnSuccessListener(doc -> {
                                            if (!isAdded()) return;
                                            
                                            String name = doc.exists() ? doc.getString("name") : null;
                                            if (name == null) name = "Child";
                                            
                                            childNameMap.put(childUid, name);
                                            completed[0]++;
                                            
                                            if (completed[0] == total) {
                                                for (String childUid2 : children) {
                                                    dialogChildIds.add(childUid2);
                                                    dialogChildNames.add(childNameMap.get(childUid2));
                                                }
                                                
                                                spinnerAdapter.notifyDataSetChanged();
                                                
                                                if (!dialogChildIds.isEmpty()) {
                                                    String firstChildId = dialogChildIds.get(0);
                                                    loadTriageForChild(firstChildId, recyclerView, emptyView, adapter, dialog);
                                                }
                                            }
                                        });
                            }
                        } else {
                            emptyView.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                            emptyView.setText("No children found");
                        }
                    }
                });

        childSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < dialogChildIds.size()) {
                    String selectedChildId = dialogChildIds.get(position);
                    loadTriageForChild(selectedChildId, recyclerView, emptyView, adapter, dialog);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        dialog.show();
    }

    private void loadTriageForChild(String childId, RecyclerView recyclerView, TextView emptyView, 
                                    TriageListAdapter adapter, AlertDialog dialog) {
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        TriageService triageService = new TriageService();
        triageService.getTriageHistory(childId, 30, new TriageService.TriageHistoryCallback() {
            @Override
            public void onSuccess(List<TriageIncident> incidents) {
                if (!isAdded()) return;
                
                if (incidents.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText("No triage incidents found");
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                    adapter.updateIncidents(incidents, incident -> {
                        dialog.dismiss();
                        showTriageIncidentDetails(incident);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText("Error loading triage logs");
                }
            }
        });
    }

    private void showTriageIncidentDetails(TriageIncident incident) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_triage_details, null);
        
        TextView dateText = dialogView.findViewById(R.id.textDate);
        TextView redFlagsText = dialogView.findViewById(R.id.textRedFlags);
        TextView rescueAttemptsText = dialogView.findViewById(R.id.textRescueAttempts);
        TextView peakFlowText = dialogView.findViewById(R.id.textPeakFlow);
        TextView decisionText = dialogView.findViewById(R.id.textDecision);
        TextView userResponseText = dialogView.findViewById(R.id.textUserResponse);
        TextView escalationText = dialogView.findViewById(R.id.textEscalation);
        Button closeButton = dialogView.findViewById(R.id.buttonClose);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        dateText.setText(sdf.format(new Date(incident.getTimestamp())));

        List<String> redFlags = new ArrayList<>();
        if (incident.isCannotSpeakFullSentences()) redFlags.add("Cannot speak full sentences");
        if (incident.isChestRetractions()) redFlags.add("Chest retractions");
        if (incident.isBlueGrayLipsNails()) redFlags.add("Blue/gray lips or nails");
        
        if (redFlags.isEmpty()) {
            redFlagsText.setText("None");
        } else {
            redFlagsText.setText(String.join(", ", redFlags));
        }

        rescueAttemptsText.setText(String.valueOf(incident.getRecentRescueAttempts()));

        if (incident.getPeakFlowReading() != null && !incident.getPeakFlowReading().isEmpty()) {
            peakFlowText.setText(incident.getPeakFlowReading());
        } else {
            peakFlowText.setText("Not recorded");
        }

        String decision = incident.getDecision();
        if (decision != null) {
            if (decision.equals("emergency")) {
                decisionText.setText("Emergency");
                decisionText.setTextColor(Color.parseColor("#F44336"));
            } else {
                decisionText.setText("Home Steps");
                decisionText.setTextColor(Color.parseColor("#4CAF50"));
            }
        }

        if (incident.getUserResponse() != null && !incident.getUserResponse().isEmpty()) {
            userResponseText.setText(incident.getUserResponse());
        } else {
            userResponseText.setText("Not available");
        }

        if (incident.isEscalated()) {
            escalationText.setVisibility(View.VISIBLE);
            String escalationInfo = "Escalated";
            if (incident.getEscalationReason() != null && !incident.getEscalationReason().isEmpty()) {
                escalationInfo += ": " + incident.getEscalationReason();
            }
            escalationText.setText(escalationInfo);
        } else {
            escalationText.setVisibility(View.GONE);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private static class TriageListAdapter extends RecyclerView.Adapter<TriageListAdapter.ViewHolder> {
        private List<TriageIncident> incidents = new ArrayList<>();
        private OnIncidentClickListener listener;

        interface OnIncidentClickListener {
            void onClick(TriageIncident incident);
        }

        void updateIncidents(List<TriageIncident> newIncidents, OnIncidentClickListener clickListener) {
            this.incidents = newIncidents;
            this.listener = clickListener;
            incidents.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_triage_incident, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TriageIncident incident = incidents.get(position);
            holder.bind(incident);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(incident);
                }
            });
        }

        @Override
        public int getItemCount() {
            return incidents.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView dateText;
            TextView decisionText;
            TextView summaryText;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                dateText = itemView.findViewById(R.id.textDate);
                decisionText = itemView.findViewById(R.id.textDecision);
                summaryText = itemView.findViewById(R.id.textSummary);
            }

            void bind(TriageIncident incident) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                dateText.setText(sdf.format(new Date(incident.getTimestamp())));

                String decision = incident.getDecision();
                if (decision != null && decision.equals("emergency")) {
                    decisionText.setText("Emergency");
                    decisionText.setTextColor(Color.parseColor("#F44336"));
                } else {
                    decisionText.setText("Home Steps");
                    decisionText.setTextColor(Color.parseColor("#4CAF50"));
                }

                List<String> flags = new ArrayList<>();
                if (incident.isCannotSpeakFullSentences()) flags.add("Cannot speak");
                if (incident.isChestRetractions()) flags.add("Chest retractions");
                if (incident.isBlueGrayLipsNails()) flags.add("Blue/gray lips");
                
                if (flags.isEmpty()) {
                    summaryText.setText("No red flags");
                } else {
                    summaryText.setText(String.join(", ", flags));
                }
            }
        }
    }
}
