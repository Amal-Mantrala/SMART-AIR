package com.example.b07demosummer2024.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.auth.PrivacyService;

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
        Button backButton = view.findViewById(R.id.buttonBack);

        privacyDefaultsButton.setOnClickListener(v -> showPrivacyDefaultsDialog());
        sharingControlsButton.setOnClickListener(v -> showSharingControlsDialog());
        providerInvitesButton.setOnClickListener(v -> showProviderInvitesDialog());
        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void showPrivacyDefaultsDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Privacy Defaults")
                .setMessage(privacyService.getPrivacyDefaultsExplanation())
                .setPositiveButton("OK", null)
                .setCancelable(true)
                .show();
    }

    private void showSharingControlsDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Sharing Controls")
                .setMessage(privacyService.getSharingControlsExplanation())
                .setPositiveButton("OK", null)
                .setCancelable(true)
                .show();
    }

    private void showProviderInvitesDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Provider Invites")
                .setMessage(privacyService.getProviderInvitesExplanation())
                .setPositiveButton("OK", null)
                .setCancelable(true)
                .show();
    }
}
