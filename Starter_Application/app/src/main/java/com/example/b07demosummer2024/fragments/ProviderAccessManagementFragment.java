package com.example.b07demosummer2024.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.services.InviteService;
import com.example.b07demosummer2024.services.PermissionService;
import com.example.b07demosummer2024.services.ShareableDataFields;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderAccessManagementFragment extends Fragment {
    private TextView privacyDefaultsText;
    private LinearLayout activeProvidersLayout;
    private LinearLayout activeInvitesLayout;
    private PermissionService permissionService;
    private InviteService inviteService;
    private String parentId;
    private List<String> childrenIds;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_provider_access_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            parentId = auth.getCurrentUser().getUid();
        }

        permissionService = new PermissionService();
        inviteService = new InviteService();

        privacyDefaultsText = view.findViewById(R.id.textPrivacyDefaults);
        activeProvidersLayout = view.findViewById(R.id.layoutActiveProviders);
        activeInvitesLayout = view.findViewById(R.id.layoutActiveInvites);
        Button backButton = view.findViewById(R.id.buttonBack);
        Button helpButton = view.findViewById(R.id.buttonHowItWorks);
        Button generateInviteButton = view.findViewById(R.id.buttonGenerateInvite);
        Button addProviderButton = view.findViewById(R.id.buttonAddProvider);

        backButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        helpButton.setOnClickListener(v -> showHelpDialog());
        generateInviteButton.setOnClickListener(v -> showGenerateInviteDialog());
        addProviderButton.setOnClickListener(v -> showAddProviderDialog());

        loadPrivacyDefaults();
        loadChildren();
        loadActiveProviders();
        loadActiveInvites();
    }

    private void loadPrivacyDefaults() {
        permissionService.getPrivacyDefaults(parentId, defaults -> {
            if (isAdded()) {
                boolean shareByDefault = Boolean.TRUE.equals(defaults.get("shareByDefault"));
                String defaultLevel = (String) defaults.get("defaultShareLevel");
                String text = "Privacy Defaults:\n• Share by default: " + (shareByDefault ? "Yes" : "No") + "\n• Default share level: " + (defaultLevel != null ? defaultLevel : "none");
                privacyDefaultsText.setText(text);
            }
        });
    }

    private void loadChildren() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .whereEqualTo("parentId", parentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        childrenIds = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                            childrenIds.add(doc.getId());
                        }
                    } else {
                        childrenIds = new ArrayList<>();
                    }
                });
    }

    private void loadActiveProviders() {
        activeProvidersLayout.removeAllViews();
        permissionService.getActiveProviders(parentId, providers -> {
            if (isAdded()) {
                for (Map<String, Object> provider : providers) {
                    String providerId = (String) provider.get("providerId");
                    List<String> children = (List<String>) provider.get("children");
                    addProviderView(providerId, children);
                }
            }
        });
    }

    private void addProviderView(String providerId, List<String> children) {
        View providerView = getLayoutInflater().inflate(R.layout.item_provider_access, activeProvidersLayout, false);
        TextView providerNameText = providerView.findViewById(R.id.textProviderName);
        TextView childrenText = providerView.findViewById(R.id.textChildren);
        Button manageSharingButton = providerView.findViewById(R.id.buttonManageSharing);
        Button revokeButton = providerView.findViewById(R.id.buttonRevoke);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(providerId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && isAdded()) {
                        String name = document.getString("name");
                        String email = document.getString("email");
                        if (email == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
                            email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                        }
                        providerNameText.setText((name != null ? name : "Provider") + " (" + (email != null ? email : providerId) + ")");
                    }
                });

        if (children != null && !children.isEmpty()) {
            StringBuilder childrenList = new StringBuilder("Access to: ");
            for (String childId : children) {
                childrenList.append(childId.substring(0, Math.min(8, childId.length()))).append(", ");
            }
            childrenText.setText(childrenList.substring(0, childrenList.length() - 2));
        } else {
            childrenText.setText("No children shared");
        }

        manageSharingButton.setOnClickListener(v -> showManageSharingDialog(providerId, children));

        revokeButton.setOnClickListener(v -> {
            permissionService.revokeAccess(parentId, providerId, (success, message) -> {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    if (success) {
                        loadActiveProviders();
                    }
                }
            });
        });

        activeProvidersLayout.addView(providerView);
    }

    private void loadActiveInvites() {
        activeInvitesLayout.removeAllViews();
        inviteService.getActiveInvites(parentId, invites -> {
            if (isAdded()) {
                for (Map<String, Object> invite : invites) {
                    String code = (String) invite.get("code");
                    List<String> children = (List<String>) invite.get("children");
                    Long expiresAt = (Long) invite.get("expiresAt");
                    addInviteView(code, children, expiresAt);
                }
            }
        });
    }

    private void addInviteView(String code, List<String> children, Long expiresAt) {
        View inviteView = getLayoutInflater().inflate(R.layout.item_invite, activeInvitesLayout, false);
        TextView codeText = inviteView.findViewById(R.id.textInviteCode);
        TextView expiryText = inviteView.findViewById(R.id.textExpiry);
        Button revokeButton = inviteView.findViewById(R.id.buttonRevokeInvite);

        codeText.setText("Code: " + code);
        if (expiresAt != null) {
            long daysLeft = (expiresAt - System.currentTimeMillis()) / (24 * 60 * 60 * 1000);
            expiryText.setText("Expires in: " + daysLeft + " days");
        }

        revokeButton.setOnClickListener(v -> {
            inviteService.revokeInvite(code, (success, message) -> {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    if (success) {
                        loadActiveInvites();
                    }
                }
            });
        });

        activeInvitesLayout.addView(inviteView);
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("How Provider Sharing Works")
                .setMessage("Privacy Defaults:\n• By default, nothing is shared with providers\n• You control what each provider can see\n\nSharing Controls:\n• Generate invite codes to share with providers\n• Select which children to include in each invite\n• Invite codes expire in 7 days\n• You can revoke access anytime\n\nProvider Access:\n• Providers can only VIEW data (read-only)\n• They cannot edit or modify anything\n• All changes take effect immediately")
                .setPositiveButton("Got it", null)
                .show();
    }

    private void showGenerateInviteDialog() {
        if (childrenIds == null || childrenIds.isEmpty()) {
            Toast.makeText(requireContext(), "No children found", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_generate_invite, null);
        LinearLayout childrenLayout = dialogView.findViewById(R.id.layoutChildren);
        Button generateButton = dialogView.findViewById(R.id.buttonGenerate);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        List<CheckBox> checkBoxes = new ArrayList<>();
        for (String childId : childrenIds) {
            CheckBox checkBox = new CheckBox(requireContext());
            checkBox.setText("Child: " + childId.substring(0, Math.min(8, childId.length())));
            checkBox.setTag(childId);
            childrenLayout.addView(checkBox);
            checkBoxes.add(checkBox);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setTitle("Generate Invite Code")
                .create();

        generateButton.setOnClickListener(v -> {
            List<String> selectedChildren = new ArrayList<>();
            for (CheckBox checkBox : checkBoxes) {
                if (checkBox.isChecked()) {
                    selectedChildren.add((String) checkBox.getTag());
                }
            }

            if (selectedChildren.isEmpty()) {
                Toast.makeText(requireContext(), "Select at least one child", Toast.LENGTH_SHORT).show();
                return;
            }

            inviteService.generateInviteCode(parentId, selectedChildren, 7, (code, success, message) -> {
                if (isAdded()) {
                    if (success && code != null) {
                        Toast.makeText(requireContext(), "Invite code: " + code, Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        loadActiveInvites();
                    } else {
                        Toast.makeText(requireContext(), "Failed: " + message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showAddProviderDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_provider, null);
        EditText emailInput = dialogView.findViewById(R.id.editProviderEmail);
        LinearLayout childrenLayout = dialogView.findViewById(R.id.layoutChildren);
        Button grantButton = dialogView.findViewById(R.id.buttonGrant);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        if (childrenIds == null || childrenIds.isEmpty()) {
            Toast.makeText(requireContext(), "No children found", Toast.LENGTH_SHORT).show();
            return;
        }

        List<CheckBox> checkBoxes = new ArrayList<>();
        for (String childId : childrenIds) {
            CheckBox checkBox = new CheckBox(requireContext());
            checkBox.setText("Child: " + childId.substring(0, Math.min(8, childId.length())));
            checkBox.setTag(childId);
            childrenLayout.addView(checkBox);
            checkBoxes.add(checkBox);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setTitle("Add Provider by Email")
                .create();

        grantButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty()) {
                emailInput.setError("Enter provider email");
                return;
            }

            List<String> selectedChildren = new ArrayList<>();
            for (CheckBox checkBox : checkBoxes) {
                if (checkBox.isChecked()) {
                    selectedChildren.add((String) checkBox.getTag());
                }
            }

            if (selectedChildren.isEmpty()) {
                Toast.makeText(requireContext(), "Select at least one child", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .whereEqualTo("email", email)
                    .whereEqualTo("role", "provider")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                            String providerId = task.getResult().getDocuments().get(0).getId();
                            // Show field selection dialog after selecting children
                            showFieldSelectionDialog(providerId, selectedChildren, dialog);
                        } else {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), "Provider not found", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showFieldSelectionDialog(String providerId, List<String> selectedChildren, AlertDialog parentDialog) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_fields, null);
        LinearLayout fieldsLayout = dialogView.findViewById(R.id.layoutFields);
        Button saveButton = dialogView.findViewById(R.id.buttonSave);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        // Create checkboxes for each shareable field
        List<CheckBox> fieldCheckBoxes = new ArrayList<>();
        for (String field : ShareableDataFields.ALL_FIELDS) {
            CheckBox checkBox = new CheckBox(requireContext());
            checkBox.setText(ShareableDataFields.getFieldLabel(field));
            checkBox.setTag(field);
            checkBox.setHint(ShareableDataFields.getFieldDescription(field));
            fieldsLayout.addView(checkBox);
            fieldCheckBoxes.add(checkBox);
        }

        AlertDialog fieldDialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setTitle("Select Data Fields to Share")
                .create();

        saveButton.setOnClickListener(v -> {
            List<String> selectedFields = new ArrayList<>();
            for (CheckBox checkBox : fieldCheckBoxes) {
                if (checkBox.isChecked()) {
                    selectedFields.add((String) checkBox.getTag());
                }
            }

            // Create shared fields map for each child
            Map<String, List<String>> sharedFieldsPerChild = new HashMap<>();
            for (String childId : selectedChildren) {
                sharedFieldsPerChild.put(childId, new ArrayList<>(selectedFields));
            }

            permissionService.grantAccessWithFields(parentId, providerId, selectedChildren, sharedFieldsPerChild, (success, message) -> {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    if (success) {
                        fieldDialog.dismiss();
                        if (parentDialog != null) {
                            parentDialog.dismiss();
                        }
                        loadActiveProviders();
                    }
                }
            });
        });

        cancelButton.setOnClickListener(v -> fieldDialog.dismiss());
        fieldDialog.show();
    }

    private void showManageSharingDialog(String providerId, List<String> children) {
        if (children == null || children.isEmpty()) {
            Toast.makeText(requireContext(), "No children to manage", Toast.LENGTH_SHORT).show();
            return;
        }

        // For simplicity, show field selection for all children at once
        // In a more advanced version, you could show per-child selection
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_fields, null);
        LinearLayout fieldsLayout = dialogView.findViewById(R.id.layoutFields);
        Button saveButton = dialogView.findViewById(R.id.buttonSave);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        // Load current shared fields for the first child (as reference)
        permissionService.getSharedFields(parentId, providerId, children.get(0), currentFields -> {
            if (isAdded()) {
                // Create checkboxes for each shareable field
                List<CheckBox> fieldCheckBoxes = new ArrayList<>();
                for (String field : ShareableDataFields.ALL_FIELDS) {
                    CheckBox checkBox = new CheckBox(requireContext());
                    checkBox.setText(ShareableDataFields.getFieldLabel(field));
                    checkBox.setTag(field);
                    checkBox.setHint(ShareableDataFields.getFieldDescription(field));
                    // Check if this field is currently shared
                    if (currentFields.contains(field)) {
                        checkBox.setChecked(true);
                    }
                    fieldsLayout.addView(checkBox);
                    fieldCheckBoxes.add(checkBox);
                }

                AlertDialog fieldDialog = new AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .setTitle("Manage Sharing Preferences")
                        .create();

                saveButton.setOnClickListener(v -> {
                    List<String> selectedFields = new ArrayList<>();
                    for (CheckBox checkBox : fieldCheckBoxes) {
                        if (checkBox.isChecked()) {
                            selectedFields.add((String) checkBox.getTag());
                        }
                    }

                    // Update shared fields for all children
                    Map<String, List<String>> sharedFieldsPerChild = new HashMap<>();
                    for (String childId : children) {
                        sharedFieldsPerChild.put(childId, new ArrayList<>(selectedFields));
                    }

                    permissionService.updateSharedFieldsForChildren(parentId, providerId, sharedFieldsPerChild, (success, message) -> {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                            if (success) {
                                fieldDialog.dismiss();
                                loadActiveProviders();
                            }
                        }
                    });
                });

                cancelButton.setOnClickListener(v -> fieldDialog.dismiss());
                fieldDialog.show();
            }
        });
    }
}

