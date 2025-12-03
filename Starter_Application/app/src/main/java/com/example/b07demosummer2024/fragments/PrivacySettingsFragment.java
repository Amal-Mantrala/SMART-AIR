package com.example.b07demosummer2024.fragments;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.adapters.ChildSelectionAdapter;
import com.example.b07demosummer2024.auth.PrivacyService;
import com.example.b07demosummer2024.models.ChildSelection;
import com.example.b07demosummer2024.models.ProviderInvite;
import com.example.b07demosummer2024.services.ProviderInviteService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrivacySettingsFragment extends Fragment {
    private PrivacyService privacyService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_privacy_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        privacyService = new PrivacyService();

        Button privacyDefaultsButton = view.findViewById(R.id.buttonPrivacyDefaults);
        Button sharingControlsButton = view.findViewById(R.id.buttonSharingControls);
        Button providerInvitesButton = view.findViewById(R.id.buttonProviderInvites);
        Button manageSharingButton = view.findViewById(R.id.buttonManageSharing);
        Button inviteProviderButton = view.findViewById(R.id.buttonInviteProvider);
        Button backButton = view.findViewById(R.id.buttonBack);

        privacyDefaultsButton.setOnClickListener(v -> showPrivacyDefaultsDialog());
        sharingControlsButton.setOnClickListener(v -> showSharingControlsDialog());
        providerInvitesButton.setOnClickListener(v -> showProviderInvitesDialog());
        manageSharingButton.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ManageProviderSharingFragment())
                    .addToBackStack(null)
                    .commit();
        });
        inviteProviderButton.setOnClickListener(v -> showInviteProviderDialog());
        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void showPrivacyDefaultsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_privacy_defaults, null);
        TextView contentText = dialogView.findViewById(R.id.textPrivacyContent);
        Button okButton = dialogView.findViewById(R.id.buttonOK);

        String content = privacyService.getPrivacyDefaultsExplanation();
        content = content.replace("\n", "<br>");
        contentText.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        okButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showSharingControlsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sharing_controls, null);
        TextView contentText = dialogView.findViewById(R.id.textSharingContent);
        Button okButton = dialogView.findViewById(R.id.buttonOK);

        String content = privacyService.getSharingControlsExplanation();
        content = content.replace("\n", "<br>");
        contentText.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        okButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showProviderInvitesDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_provider_invites_info, null);
        TextView contentText = dialogView.findViewById(R.id.textInvitesContent);
        Button okButton = dialogView.findViewById(R.id.buttonOK);

        String content = privacyService.getProviderInvitesExplanation();
        content = content.replace("\n", "<br>");
        contentText.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        okButton.setOnClickListener(v -> dialog.dismiss());
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

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

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
