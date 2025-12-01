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
import com.example.b07demosummer2024.models.MedicineLog;
import com.example.b07demosummer2024.models.SymptomLog;
import com.example.b07demosummer2024.models.DailyWellnessLog;
import com.example.b07demosummer2024.services.ChildHealthService;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

public class HealthHistoryFragment extends ProtectedFragment {
    private static final String ARG_CHILD_ID = "child_id";
    private RecyclerView recyclerView;
    private TextView emptyView;
    private HealthHistoryAdapter adapter;
    private String childId;
    
    public static HealthHistoryFragment newInstance(String childId) {
        HealthHistoryFragment fragment = new HealthHistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHILD_ID, childId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            childId = getArguments().getString(ARG_CHILD_ID);
        }
        // Fallback to current user if no argument provided
        if (childId == null) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                childId = auth.getCurrentUser().getUid();
            }
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_health_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        recyclerView = view.findViewById(R.id.recyclerViewHistory);
        emptyView = view.findViewById(R.id.textEmptyView);
        Button backButton = view.findViewById(R.id.buttonBack);
        
        // Set up back button
        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                // Fallback navigation
                requireActivity().onBackPressed();
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HealthHistoryAdapter();
        recyclerView.setAdapter(adapter);
        
        loadHealthHistory();
    }
    
    private void loadHealthHistory() {
        if (childId == null) {
            Toast.makeText(requireContext(), "Error: No child ID provided", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ChildHealthService healthService = new ChildHealthService();
        healthService.getAllHealthData(childId, 30, new ChildHealthService.AllHealthDataCallback() {
            @Override
            public void onSuccess(List<MedicineLog> medicineData, List<SymptomLog> symptomData, List<DailyWellnessLog> wellnessData) {
                if (isAdded()) {
                    adapter.updateData(medicineData, symptomData, wellnessData);
                    if (medicineData.isEmpty() && symptomData.isEmpty() && wellnessData.isEmpty()) {
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
                    Toast.makeText(requireContext(), "Error loading health history: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    // Adapter for displaying health history with real data
    private static class HealthHistoryAdapter extends RecyclerView.Adapter<HealthHistoryAdapter.ViewHolder> {
        private List<HealthHistoryItem> allItems = new ArrayList<>();
        private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        
        public void updateData(List<MedicineLog> medicineData, List<SymptomLog> symptomData, List<DailyWellnessLog> wellnessData) {
            allItems.clear();
            
            // Convert all data to HealthHistoryItem and add to list
            if (medicineData != null) {
                for (MedicineLog medicine : medicineData) {
                    allItems.add(new HealthHistoryItem("Medicine Log", medicine.getTimestamp(), 
                            formatMedicineDetails(medicine)));
                }
            }
            if (symptomData != null) {
                for (SymptomLog symptom : symptomData) {
                    allItems.add(new HealthHistoryItem("Symptom Log", symptom.getTimestamp(), 
                            formatSymptomDetails(symptom)));
                }
            }
            if (wellnessData != null) {
                for (DailyWellnessLog wellness : wellnessData) {
                    allItems.add(new HealthHistoryItem("Daily Check-in", wellness.getTimestamp(), 
                            formatWellnessDetails(wellness)));
                }
            }
            
            // Sort by timestamp (newest first)
            allItems.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
            notifyDataSetChanged();
        }
        
        private String formatMedicineDetails(MedicineLog medicine) {
            StringBuilder details = new StringBuilder();
            
            // Add medicine type first (Controller/Rescue)
            if (medicine.getMedicineType() != null) {
                details.append(medicine.getMedicineType()).append(" - ");
            }
            
            if (medicine.getMedicineName() != null) {
                details.append(medicine.getMedicineName());
            }
            if (medicine.getDosage() != null && !medicine.getDosage().isEmpty()) {
                details.append(" - ").append(medicine.getDosage());
            }
            if (medicine.getNotes() != null && !medicine.getNotes().isEmpty()) {
                details.append("\n").append(medicine.getNotes());
            }
            return details.toString();
        }
        
        private String formatSymptomDetails(SymptomLog symptom) {
            StringBuilder details = new StringBuilder();
            if (symptom.getSymptoms() != null && !symptom.getSymptoms().isEmpty()) {
                details.append("Symptoms: ").append(String.join(", ", symptom.getSymptoms()));
            }
            if (symptom.getOverallSeverity() > 0) {
                details.append(" (Severity: ").append(symptom.getOverallSeverity()).append("/10)");
            }
            if (symptom.getTags() != null && !symptom.getTags().isEmpty()) {
                details.append("\nTags: ").append(String.join(", ", symptom.getTags()));
            }
            if (symptom.getNotes() != null && !symptom.getNotes().isEmpty()) {
                details.append("\n").append(symptom.getNotes());
            }
            return details.toString();
        }
        
        private String formatWellnessDetails(DailyWellnessLog wellness) {
            StringBuilder details = new StringBuilder();
            if (wellness.getMood() != null && !wellness.getMood().isEmpty()) {
                details.append("Mood: ").append(wellness.getMood());
            }
            if (wellness.getEnergyLevel() > 0) {
                if (details.length() > 0) details.append(", ");
                details.append("Energy: ").append(wellness.getEnergyLevel()).append("/5");
            }
            if (wellness.getSleepQuality() > 0) {
                if (details.length() > 0) details.append(", ");
                details.append("Sleep: ").append(wellness.getSleepQuality()).append("/5");
            }
            if (wellness.getOverallFeeling() > 0) {
                if (details.length() > 0) details.append(", ");
                details.append("Feeling: ").append(wellness.getOverallFeeling()).append("/5");
            }
            if (wellness.getNotes() != null && !wellness.getNotes().isEmpty()) {
                details.append("\n").append(wellness.getNotes());
            }
            return details.toString();
        }
        
        @Override
        public int getItemCount() {
            return allItems.size();
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_health_history, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HealthHistoryItem item = allItems.get(position);
            holder.bind(item.title, item.timestamp, item.details);
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleText, dateText, detailText;
            
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.textTitle);
                dateText = itemView.findViewById(R.id.textDate);
                detailText = itemView.findViewById(R.id.textDetail);
            }
            
            void bind(String title, long timestamp, String details) {
                titleText.setText(title);
                dateText.setText(new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date(timestamp)));
                detailText.setText(details.isEmpty() ? "No additional details" : details);
            }
        }
        
        // Helper class to hold combined health data
        private static class HealthHistoryItem {
            String title;
            long timestamp;
            String details;
            
            HealthHistoryItem(String title, long timestamp, String details) {
                this.title = title;
                this.timestamp = timestamp;
                this.details = details;
            }
        }
    }
}