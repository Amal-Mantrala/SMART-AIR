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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.services.ProviderAccessService;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class ProviderPatientSelectionFragment extends ProtectedFragment {
    private RecyclerView recyclerView;
    private TextView emptyView;
    private PatientSelectionAdapter adapter;
    private ProviderAccessService accessService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_provider_patient_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        recyclerView = view.findViewById(R.id.recyclerViewPatients);
        emptyView = view.findViewById(R.id.textEmptyView);
        Button backButton = view.findViewById(R.id.buttonBack);
        
        // Set up back button
        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                requireActivity().onBackPressed();
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PatientSelectionAdapter(this::onPatientSelected);
        recyclerView.setAdapter(adapter);
        
        accessService = new ProviderAccessService();
        loadAccessiblePatients();
    }
    
    private void loadAccessiblePatients() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String providerId = auth.getCurrentUser().getUid();
        
        accessService.getAccessibleChildren(providerId, new ProviderAccessService.AccessibleChildrenCallback() {
            @Override
            public void onSuccess(List<ProviderAccessService.AccessibleChild> children) {
                if (isAdded()) {
                    adapter.updatePatients(children);
                    if (children.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error loading patients: " + error, Toast.LENGTH_SHORT).show();
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
            }
        });
    }
    
    private void onPatientSelected(ProviderAccessService.AccessibleChild child) {
        // Navigate to health history for this child
        HealthHistoryFragment historyFragment = HealthHistoryFragment.newInstance(child.getChildId());
        
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, historyFragment)
                .addToBackStack(null)
                .commit();
    }
    
    // Adapter for displaying accessible patients
    private static class PatientSelectionAdapter extends RecyclerView.Adapter<PatientSelectionAdapter.ViewHolder> {
        private List<ProviderAccessService.AccessibleChild> patients;
        private PatientClickListener clickListener;
        
        interface PatientClickListener {
            void onPatientClick(ProviderAccessService.AccessibleChild child);
        }
        
        PatientSelectionAdapter(PatientClickListener clickListener) {
            this.clickListener = clickListener;
        }
        
        void updatePatients(List<ProviderAccessService.AccessibleChild> patients) {
            this.patients = patients;
            notifyDataSetChanged();
        }
        
        @Override
        public int getItemCount() {
            return patients != null ? patients.size() : 0;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_patient_selection, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProviderAccessService.AccessibleChild child = patients.get(position);
            holder.bind(child, clickListener);
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameText, parentText;
            
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.textPatientName);
                parentText = itemView.findViewById(R.id.textParentName);
            }
            
            void bind(ProviderAccessService.AccessibleChild child, PatientClickListener clickListener) {
                nameText.setText(child.getChildName() != null ? child.getChildName() : "Unknown Patient");
                parentText.setText("Parent: " + (child.getParentName() != null ? child.getParentName() : "Unknown Parent"));
                
                itemView.setOnClickListener(v -> clickListener.onPatientClick(child));
            }
        }
    }
}