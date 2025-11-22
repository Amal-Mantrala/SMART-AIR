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

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(childId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        com.google.firebase.firestore.DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            String parentId = doc.getString("parentId");
                            if (parentId != null) {
                                dataAccessService.canRead(parentId, providerId, childId, canAccess -> {
                                    if (canAccess && isAdded()) {
                                        String name = doc.getString("name");
                                        nameText.setText("Patient: " + (name != null ? name : "Unknown"));
                                    } else {
                                        if (isAdded()) {
                                            Toast.makeText(requireContext(), "Access denied", Toast.LENGTH_SHORT).show();
                                            getParentFragmentManager().popBackStack();
                                        }
                                    }
                                });
                            }
                        }
                    }
                });
    }
}

