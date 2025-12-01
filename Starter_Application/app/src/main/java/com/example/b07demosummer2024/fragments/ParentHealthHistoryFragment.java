package com.example.b07demosummer2024.fragments;

import android.app.DatePickerDialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
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
import com.example.b07demosummer2024.models.DailyWellnessLog;
import com.example.b07demosummer2024.models.MedicineLog;
import com.example.b07demosummer2024.models.SymptomLog;
import com.example.b07demosummer2024.services.ChildHealthService;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParentHealthHistoryFragment extends ProtectedFragment {

    private static final String ARG_CHILD_ID = "child_id";

    private Spinner spinnerSymptoms;
    private Spinner spinnerTriggers;
    private Button buttonDateRange;
    private Button buttonApplyFilters;

    private RecyclerView recyclerView;
    private TextView emptyView;
    private HealthHistoryAdapter adapter;

    private String childId;

    // Full unfiltered data
    private List<MedicineLog> fullMedicine = new ArrayList<>();
    private List<SymptomLog> fullSymptoms = new ArrayList<>();
    private List<DailyWellnessLog> fullWellness = new ArrayList<>();

    // Date range
    private Long startDate = null;
    private Long endDate = null;

    public static ParentHealthHistoryFragment newInstance(String childId) {
        ParentHealthHistoryFragment frag = new ParentHealthHistoryFragment();
        Bundle b = new Bundle();
        b.putString(ARG_CHILD_ID, childId);
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            childId = getArguments().getString(ARG_CHILD_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parent_health_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerSymptoms = view.findViewById(R.id.spinnerSymptomsFilter);
        spinnerTriggers = view.findViewById(R.id.spinnerTriggersFilter);
        buttonDateRange = view.findViewById(R.id.buttonSelectDateRange);
        buttonApplyFilters = view.findViewById(R.id.buttonApplyFilters);
        Button export = view.findViewById(R.id.buttonExportPdf);

        recyclerView = view.findViewById(R.id.recyclerViewParentHistory);
        emptyView = view.findViewById(R.id.textEmptyViewParent);

        adapter = new HealthHistoryAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        loadData();

        buttonDateRange.setOnClickListener(v -> openDatePicker());
        buttonApplyFilters.setOnClickListener(v -> applyFilters());
        Button back = view.findViewById(R.id.buttonBackParent);
        back.setOnClickListener(v -> requireActivity().onBackPressed());
        export.setOnClickListener(v -> exportPdf());
    }

    private void loadData() {
        ChildHealthService service = new ChildHealthService();

        service.getAllHealthData(childId, 180, new ChildHealthService.AllHealthDataCallback() {
            @Override
            public void onSuccess(List<MedicineLog> meds,
                                  List<SymptomLog> symptoms,
                                  List<DailyWellnessLog> wellness) {

                fullMedicine = meds != null ? meds : new ArrayList<>();
                fullSymptoms = symptoms != null ? symptoms : new ArrayList<>();
                fullWellness = wellness != null ? wellness : new ArrayList<>();

                populateFilters();
                applyFilters();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(),
                        "Error loading data: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateFilters() {
        // Collect unique symptoms
        Set<String> allSymptoms = new HashSet<>();
        for (SymptomLog log : fullSymptoms) {
            if (log.getSymptoms() != null) allSymptoms.addAll(log.getSymptoms());
        }

        List<String> symptomOptions = new ArrayList<>();
        symptomOptions.add("All");
        symptomOptions.addAll(allSymptoms);

        spinnerSymptoms.setAdapter(
                new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        symptomOptions));

        // Collect unique triggers
        Set<String> allTriggers = new HashSet<>();
        for (SymptomLog log : fullSymptoms) {
            if (log.getTriggers() != null) allTriggers.addAll(log.getTriggers());
        }

        List<String> triggerOptions = new ArrayList<>();
        triggerOptions.add("All");
        triggerOptions.addAll(allTriggers);

        spinnerTriggers.setAdapter(
                new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        triggerOptions));
    }

    private void openDatePicker() {
        final Calendar c = Calendar.getInstance();

        DatePickerDialog startPicker = new DatePickerDialog(
                requireContext(),
                (view, y, m, d) -> {
                    Calendar start = Calendar.getInstance();
                    start.set(y, m, d, 0, 0);
                    startDate = start.getTimeInMillis();

                    DatePickerDialog endPicker = new DatePickerDialog(
                            requireContext(),
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
                c.get(Calendar.DAY_OF_MONTH)
        );

        startPicker.show();
    }

    private void applyFilters() {
        String selectedSymptom = spinnerSymptoms.getSelectedItem().toString();
        String selectedTrigger = spinnerTriggers.getSelectedItem().toString();

        List<MedicineLog> meds = new ArrayList<>(fullMedicine);
        List<SymptomLog> syms = new ArrayList<>(fullSymptoms);
        List<DailyWellnessLog> wells = new ArrayList<>(fullWellness);

        // filter symptoms
        if (!selectedSymptom.equals("All")) {
            syms.removeIf(s -> s.getSymptoms() == null ||
                    !s.getSymptoms().contains(selectedSymptom));
        }

        // filter triggers
        if (!selectedTrigger.equals("All")) {
            syms.removeIf(s -> s.getTriggers() == null ||
                    !s.getTriggers().contains(selectedTrigger));
        }

        // filter date range
        if (startDate != null && endDate != null) {
            meds.removeIf(m -> m.getTimestamp() < startDate || m.getTimestamp() > endDate);
            syms.removeIf(s -> s.getTimestamp() < startDate || s.getTimestamp() > endDate);
            wells.removeIf(w -> w.getTimestamp() < startDate || w.getTimestamp() > endDate);
        }

        adapter.updateData(meds, syms, wells);

        if (meds.isEmpty() && syms.isEmpty() && wells.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void exportPdf() {
        try {
            List<HealthHistoryAdapter.HealthHistoryItem> items = adapter.getItems();

            if (items == null || items.isEmpty()) {
                Toast.makeText(requireContext(), "Nothing to export", Toast.LENGTH_SHORT).show();
                return;
            }

            PdfDocument pdf = new PdfDocument();
            Paint paint = new Paint();
            int y = 60;

            PdfDocument.PageInfo info =
                    new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdf.startPage(info);
            Canvas canvas = page.getCanvas();

            for (HealthHistoryAdapter.HealthHistoryItem item : items) {
                canvas.drawText(item.title, 40, y, paint); y += 20;
                canvas.drawText(item.getFormattedDate(), 40, y, paint); y += 20;
                canvas.drawText(item.details, 40, y, paint); y += 40;
            }

            pdf.finishPage(page);

            File file = new File(requireContext().getExternalFilesDir(null),
                    "health_history.pdf");

            FileOutputStream out = new FileOutputStream(file);
            pdf.writeTo(out);
            pdf.close();

            Toast.makeText(requireContext(),
                    "PDF saved: " + file.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Export failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

}
