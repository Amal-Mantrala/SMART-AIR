package com.example.b07demosummer2024.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.adapters.HealthHistoryAdapter;
import com.example.b07demosummer2024.auth.ProviderSharingService;
import com.example.b07demosummer2024.models.ChildSharingSettings;
import com.example.b07demosummer2024.models.DailyWellnessLog;
import com.example.b07demosummer2024.models.MedicineLog;
import com.example.b07demosummer2024.models.SharingSettings;
import com.example.b07demosummer2024.models.SymptomLog;
import com.example.b07demosummer2024.models.User;
import com.example.b07demosummer2024.models.ZoneLog;
import com.example.b07demosummer2024.services.ChildHealthService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HealthHistoryFragment extends ProtectedFragment {

    private static final String ARG_CHILD_ID = "child_id";

    private RecyclerView recyclerView;
    private TextView emptyView;
    private HealthHistoryAdapter adapter;

    private Spinner spinnerSymptoms;
    private Spinner spinnerTriggers;
    private Button buttonDateRange;
    private Button buttonApplyFilters;

    private String childId;

    // full unfiltered data
    private List<MedicineLog> fullMedicine = new ArrayList<>();
    private List<SymptomLog> fullSymptoms = new ArrayList<>();
    private List<DailyWellnessLog> fullWellness = new ArrayList<>();
    private List<ZoneLog> fullZone = new ArrayList<>();

    // filter state
    private Long startDate = null;
    private Long endDate = null;
    
    private Map<String, Boolean> providerSettings = null;

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

        if (childId == null) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                childId = auth.getCurrentUser().getUid();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_health_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewHistory);
        emptyView = view.findViewById(R.id.textEmptyView);

        spinnerSymptoms = view.findViewById(R.id.spinnerSymptomsFilter);
        spinnerTriggers = view.findViewById(R.id.spinnerTriggersFilter);
        buttonDateRange = view.findViewById(R.id.buttonSelectDateRange);
        buttonApplyFilters = view.findViewById(R.id.buttonApplyFilters);

        // Back button
        view.findViewById(R.id.buttonBack).setOnClickListener(v -> requireActivity().onBackPressed());

        adapter = new HealthHistoryAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        buttonDateRange.setOnClickListener(v -> openDatePicker());
        buttonApplyFilters.setOnClickListener(v -> applyFilters());

        loadHealthHistory();
    }

    private void loadHealthHistory() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return;
        }
        
        String userId = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                User user = task.getResult().toObject(User.class);
                if (user != null && "provider".equals(user.getRole())) {
                    checkProviderAccess(userId);
                } else {
                    loadData();
                }
            } else {
                loadData();
            }
        });
    }
    
    private void checkProviderAccess(String providerId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(childId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                User child = task.getResult().toObject(User.class);
                if (child != null && child.getParentId() != null) {
                    String parentId = child.getParentId();
                    ProviderSharingService service = new ProviderSharingService();
                    service.getSharingSettings(parentId, providerId, new ProviderSharingService.SettingsCallback() {
                        @Override
                        public void onResult(SharingSettings settings) {
                            if (settings != null && settings.getChildSettings() != null) {
                                ChildSharingSettings childSettings = settings.getChildSettings().get(childId);
                                if (childSettings != null && childSettings.getSharedFields() != null) {
                                    providerSettings = childSettings.getSharedFields();
                                } else {
                                    providerSettings = new HashMap<>();
                                }
                            } else {
                                providerSettings = new HashMap<>();
                            }
                            loadData();
                        }

                        @Override
                        public void onError(String error) {
                            providerSettings = new HashMap<>();
                            loadData();
                        }
                    });
                } else {
                    loadData();
                }
            } else {
                loadData();
            }
        });
    }
    
    private void loadData() {
        ChildHealthService healthService = new ChildHealthService();

        healthService.getAllHealthData(childId, 30, new ChildHealthService.AllHealthDataCallback() {
            @Override
            public void onSuccess(List<MedicineLog> medicineData,
                                  List<SymptomLog> symptomData,
                                  List<DailyWellnessLog> wellnessData,
                                  List<ZoneLog> zoneData) {

                fullMedicine = medicineData != null ? medicineData : new ArrayList<>();
                fullSymptoms = symptomData != null ? symptomData : new ArrayList<>();
                fullWellness = wellnessData != null ? wellnessData : new ArrayList<>();
                fullZone = zoneData != null ? zoneData : new ArrayList<>();

                if (providerSettings != null) {
                    applyProviderFilters();
                }

                populateFilters();
                adapter.updateData(fullMedicine, fullSymptoms, fullWellness, fullZone);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "Error loading health history: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void applyProviderFilters() {
        fullWellness.clear();
        
        Boolean rescueLogs = providerSettings.get("rescueLogs");
        if (rescueLogs == null || !rescueLogs) {
            fullMedicine.removeIf(m -> "rescue".equals(m.getMedicineType()));
        }
        
        Boolean controllerSummary = providerSettings.get("controllerSummary");
        if (controllerSummary == null || !controllerSummary) {
            fullMedicine.removeIf(m -> "controller".equals(m.getMedicineType()));
        }
        
        Boolean symptoms = providerSettings.get("symptoms");
        if (symptoms == null || !symptoms) {
            fullSymptoms.clear();
        } else {
            Boolean triggers = providerSettings.get("triggers");
            if (triggers == null || !triggers) {
                fullSymptoms.removeIf(s -> s.getTriggers() != null && !s.getTriggers().isEmpty());
            }
        }
        
        Boolean summaryCharts = providerSettings.get("summaryCharts");
        if (summaryCharts == null || !summaryCharts) {
            fullZone.clear();
        } else {
            Boolean peakFlow = providerSettings.get("peakFlow");
            if (peakFlow == null || !peakFlow) {
                fullZone.removeIf(z -> z.getPefValue() > 0);
            }
        }
    }

    private void populateFilters() {
        Set<String> symptomsSet = new HashSet<>();
        for (SymptomLog log : fullSymptoms) {
            if (log.getSymptoms() != null) symptomsSet.addAll(log.getSymptoms());
        }

        List<String> symptomsList = new ArrayList<>();
        symptomsList.add("All");
        symptomsList.addAll(symptomsSet);

        spinnerSymptoms.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                symptomsList
        ));

        // TRIGGERS
        Set<String> triggerSet = new HashSet<>();
        for (SymptomLog log : fullSymptoms) {
            if (log.getTriggers() != null) triggerSet.addAll(log.getTriggers());
        }

        List<String> triggerList = new ArrayList<>();
        triggerList.add("All");
        triggerList.addAll(triggerSet);

        spinnerTriggers.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                triggerList
        ));
    }

    private void openDatePicker() {
        Calendar c = Calendar.getInstance();

        DatePickerDialog startPicker = new DatePickerDialog(requireContext(),
                (view, y, m, d) -> {
                    Calendar start = Calendar.getInstance();
                    start.set(y, m, d, 0, 0);
                    startDate = start.getTimeInMillis();

                    DatePickerDialog endPicker = new DatePickerDialog(requireContext(),
                            (view2, y2, m2, d2) -> {
                                Calendar end = Calendar.getInstance();
                                end.set(y2, m2, d2, 23, 59);
                                endDate = end.getTimeInMillis();
                            },
                            y, m, d);

                    endPicker.show();
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH));

        startPicker.show();
    }

    private void applyFilters() {

        String selectedSymptom = spinnerSymptoms.getSelectedItem().toString();
        String selectedTrigger = spinnerTriggers.getSelectedItem().toString();

        List<MedicineLog> meds = new ArrayList<>(fullMedicine);
        List<SymptomLog> syms = new ArrayList<>(fullSymptoms);
        List<DailyWellnessLog> wells = new ArrayList<>(fullWellness);
        List<ZoneLog> zones = new ArrayList<>(fullZone);

        // symptom filter
        if (!selectedSymptom.equals("All")) {
            syms.removeIf(s -> s.getSymptoms() == null ||
                    !s.getSymptoms().contains(selectedSymptom));
            meds.clear();
            wells.clear();
            zones.clear();
        }

        // trigger filter
        if (!selectedTrigger.equals("All")) {
            syms.removeIf(s -> s.getTriggers() == null ||
                    !s.getTriggers().contains(selectedTrigger));
            meds.clear();
            wells.clear();
            zones.clear();
        }

        // date filter
        if (startDate != null && endDate != null) {
            meds.removeIf(m -> m.getTimestamp() < startDate || m.getTimestamp() > endDate);
            syms.removeIf(s -> s.getTimestamp() < startDate || s.getTimestamp() > endDate);
            wells.removeIf(w -> w.getTimestamp() < startDate || w.getTimestamp() > endDate);
        }

        adapter.updateData(meds, syms, wells, zones);

        if (meds.isEmpty() && syms.isEmpty() && wells.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
