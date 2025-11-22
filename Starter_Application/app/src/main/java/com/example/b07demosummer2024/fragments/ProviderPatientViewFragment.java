package com.example.b07demosummer2024.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.services.DataAccessService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class ProviderPatientViewFragment extends Fragment {
    private String childId;
    private DataAccessService dataAccessService;

    public static ProviderPatientViewFragment newInstance(String childId) {
        ProviderPatientViewFragment fragment = new ProviderPatientViewFragment();
        Bundle args = new Bundle();
        args.putString("childId", childId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_provider_patient_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            childId = getArguments().getString("childId");
        }

        dataAccessService = new DataAccessService();

        TextView patientNameText = view.findViewById(R.id.textPatientName);
        TextView readOnlyNotice = view.findViewById(R.id.textReadOnlyNotice);
        Button backButton = view.findViewById(R.id.buttonBack);

        readOnlyNotice.setText("Read-only view - You cannot edit this data");

        backButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        loadPatientData(patientNameText);
    }

    private void loadPatientData(TextView nameText) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String providerId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (providerId == null || childId == null) {
            Toast.makeText(requireContext(), "Error loading patient data", Toast.LENGTH_SHORT).show();
            return;
        }

        dataAccessService.getReadOnlyData(childId, providerId, data -> {
            if (isAdded()) {
                if (data.isEmpty()) {
                    Toast.makeText(requireContext(), "Access denied or no data available", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                    return;
                }

                // Display shared data
                StringBuilder displayText = new StringBuilder();
                displayText.append("Patient Information:\n\n");

                // Use ShareableDataFields to get proper labels
                for (String field : com.example.b07demosummer2024.services.ShareableDataFields.ALL_FIELDS) {
                    if (data.containsKey(field)) {
                        Object value = data.get(field);
                        if (value != null) {
                            displayText.append(com.example.b07demosummer2024.services.ShareableDataFields.getFieldLabel(field))
                                    .append(": ")
                                    .append(value.toString())
                                    .append("\n\n");
                        }
                    }
                }

                // If no shared fields (except role), show message
                int sharedFieldCount = 0;
                for (String field : data.keySet()) {
                    if (!field.equals("role")) {
                        sharedFieldCount++;
                    }
                }

                if (sharedFieldCount == 0) {
                    displayText.append("No additional information is currently shared.\n");
                    displayText.append("The parent has not selected any data fields to share with you.");
                }

                nameText.setText(displayText.toString());
            }
        });
    }
}

