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
import com.example.b07demosummer2024.models.ZoneLog;
import com.example.b07demosummer2024.models.TriageIncident;
import com.example.b07demosummer2024.services.ChildHealthService;
import com.example.b07demosummer2024.services.TriageService;

import android.content.Intent;
import android.graphics.Color;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private List<MedicineLog> fullMedicine = new ArrayList<>();
    private List<SymptomLog> fullSymptoms = new ArrayList<>();
    private List<DailyWellnessLog> fullWellness = new ArrayList<>();
    private List<ZoneLog> fullZone = new ArrayList<>();

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
        Button exportProvider = view.findViewById(R.id.buttonExportProviderReport);
        exportProvider.setOnClickListener(v -> exportProviderReportPdf());
    }

    private void loadData() {
        ChildHealthService service = new ChildHealthService();

        service.getAllHealthData(childId, 180, new ChildHealthService.AllHealthDataCallback() {
            @Override
            public void onSuccess(List<MedicineLog> meds,
                                  List<SymptomLog> symptoms,
                                  List<DailyWellnessLog> wellness,
                                  List<ZoneLog> zones) {

                fullMedicine = meds != null ? meds : new ArrayList<>();
                fullSymptoms = symptoms != null ? symptoms : new ArrayList<>();
                fullWellness = wellness != null ? wellness : new ArrayList<>();
                fullZone = zones != null ? zones : new ArrayList<>();

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
        List<ZoneLog> zones = new ArrayList<>(fullZone);

        if (!selectedSymptom.equals("All")) {
            syms.removeIf(s -> s.getSymptoms() == null ||
                    !s.getSymptoms().contains(selectedSymptom));
            meds.clear();
            wells.clear();
            zones.clear();
        }

        if (!selectedTrigger.equals("All")) {
            syms.removeIf(s -> s.getTriggers() == null ||
                    !s.getTriggers().contains(selectedTrigger));
            meds.clear();
            wells.clear();
            zones.clear();
        }

        if (startDate != null && endDate != null) {
            meds.removeIf(m -> m.getTimestamp() < startDate || m.getTimestamp() > endDate);
            syms.removeIf(s -> s.getTimestamp() < startDate || s.getTimestamp() > endDate);
            wells.removeIf(w -> w.getTimestamp() < startDate || w.getTimestamp() > endDate);
            zones.removeIf(z -> z.getTimestamp() < startDate || z.getTimestamp() > endDate);
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

    private void exportPdf() {
        List<HealthHistoryAdapter.HealthHistoryItem> items = adapter.getItems();
        if (items.isEmpty()) {
            Toast.makeText(requireContext(), "No history data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            PdfDocument pdf = new PdfDocument();
            Paint paint = new Paint();
            Paint titlePaint = new Paint();
            titlePaint.setTextSize(18);
            titlePaint.setFakeBoldText(true);

            int pageNum = 1;
            int y = 60;
            int pageHeight = 842;
            int pageWidth = 595;
            int margin = 40;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            canvas.drawText("Health History", margin, y, titlePaint);
            y += 30;

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

            for (HealthHistoryAdapter.HealthHistoryItem item : items) {
                if (y > pageHeight - 100) {
                    pdf.finishPage(page);
                    pageNum++;
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                    page = pdf.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 60;
                }

                canvas.drawText(item.title, margin, y, titlePaint);
                y += 20;
                canvas.drawText(sdf.format(new java.util.Date(item.timestamp)), margin, y, paint);
                y += 20;
                String[] lines = item.details.split("\n");
                for (String line : lines) {
                    if (y > pageHeight - 80) {
                        pdf.finishPage(page);
                        pageNum++;
                        pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                        page = pdf.startPage(pageInfo);
                        canvas = page.getCanvas();
                        y = 60;
                    }
                    canvas.drawText(line, margin + 20, y, paint);
                    y += 20;
                }
                y += 15;
            }

            pdf.finishPage(page);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = dateFormat.format(new java.util.Date());
            String filename = "health_history_" + timestamp + ".pdf";

            File file;
            try {
                File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                file = new File(downloadsDir, filename);
            } catch (Exception e) {
                file = new File(requireContext().getExternalFilesDir(null), filename);
            }

            FileOutputStream out = new FileOutputStream(file);
            pdf.writeTo(out);
            pdf.close();
            out.close();

            Toast.makeText(requireContext(), "PDF saved to Downloads!\nFile: " + filename, Toast.LENGTH_LONG).show();

            try {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/pdf");
                shareIntent.putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(file));
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Health History");
                startActivity(Intent.createChooser(shareIntent, "Share Health History"));
            } catch (Exception shareError) {
            }

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportProviderReportPdf() {
        if (startDate == null || endDate == null) {
            Toast.makeText(requireContext(), "Please select date range first", Toast.LENGTH_SHORT).show();
            return;
        }

        long daysDiff = (endDate - startDate) / (24 * 60 * 60 * 1000);
        if (daysDiff < 1 || daysDiff > 180) {
            Toast.makeText(requireContext(), "Date range must be 1 day to 6 months", Toast.LENGTH_SHORT).show();
            return;
        }

        final List<DailyWellnessLog> filteredWellness = new ArrayList<>();
        final List<SymptomLog> filteredSymptoms = new ArrayList<>();
        final List<ZoneLog> filteredZones = new ArrayList<>();

        for (DailyWellnessLog w : fullWellness) {
            if (w.getTimestamp() >= startDate && w.getTimestamp() <= endDate) {
                filteredWellness.add(w);
            }
        }
        for (SymptomLog s : fullSymptoms) {
            if (s.getTimestamp() >= startDate && s.getTimestamp() <= endDate) {
                filteredSymptoms.add(s);
            }
        }
        for (ZoneLog z : fullZone) {
            if (z.getTimestamp() >= startDate && z.getTimestamp() <= endDate) {
                filteredZones.add(z);
            }
        }

        int totalDays = (int) daysDiff + 1;
        int completedDays = 0;
        int totalRescueUses = 0;
        Set<String> problemDaySet = new HashSet<>();
        final Map<String, Integer> zoneCounts = new HashMap<>();
        zoneCounts.put("Green", 0);
        zoneCounts.put("Yellow", 0);
        zoneCounts.put("Red", 0);
        final List<Map.Entry<Long, String>> zoneTimeSeries = new ArrayList<>();

        for (DailyWellnessLog w : filteredWellness) {
            if (w.isMorningController() && w.isEveningController()) {
                completedDays++;
            }
            totalRescueUses += w.getRescueInhalerUses();
            if (w.getRescueInhalerUses() > 0) {
                long dayKey = w.getTimestamp() / (24 * 60 * 60 * 1000);
                problemDaySet.add(String.valueOf(dayKey));
            }
        }

        for (SymptomLog s : filteredSymptoms) {
            long dayKey = s.getTimestamp() / (24 * 60 * 60 * 1000);
            problemDaySet.add(String.valueOf(dayKey));
        }

        for (ZoneLog z : filteredZones) {
            String zone = z.getZone();
            if (zone != null && zoneCounts.containsKey(zone)) {
                zoneCounts.put(zone, zoneCounts.get(zone) + 1);
            }
            zoneTimeSeries.add(new java.util.AbstractMap.SimpleEntry<>(z.getTimestamp(), zone));
        }
        
        zoneTimeSeries.sort((a, b) -> Long.compare(a.getKey(), b.getKey()));

        final int problemDays = problemDaySet.size();
        final double adherence = totalDays > 0 ? (completedDays * 100.0) / totalDays : 0;
        final double avgRescue = totalDays > 0 ? (double) totalRescueUses / totalDays : 0;
        final int finalTotalRescueUses = totalRescueUses;

        TriageService triageService = new TriageService();
        int daysForTriage = totalDays;
        triageService.getTriageHistory(childId, daysForTriage, new TriageService.TriageHistoryCallback() {
            @Override
            public void onSuccess(List<TriageIncident> incidents) {
                List<TriageIncident> filteredTriage = new ArrayList<>();
                for (TriageIncident incident : incidents) {
                    if (incident.getTimestamp() >= startDate && incident.getTimestamp() <= endDate) {
                        if (incident.hasRedFlags() || "emergency".equals(incident.getDecision())) {
                            filteredTriage.add(incident);
                        }
                    }
                }
                createProviderReportPdf(filteredWellness, filteredSymptoms, adherence, avgRescue, finalTotalRescueUses, problemDays, zoneCounts, zoneTimeSeries, filteredTriage);
            }

            @Override
            public void onError(String error) {
                createProviderReportPdf(filteredWellness, filteredSymptoms, adherence, avgRescue, finalTotalRescueUses, problemDays, zoneCounts, zoneTimeSeries, new ArrayList<>());
            }
        });
    }

    private void createProviderReportPdf(List<DailyWellnessLog> wellness, List<SymptomLog> symptoms,
                                         double adherence, double avgRescue, int totalRescue,
                                         int problemDays, Map<String, Integer> zoneCounts,
                                         List<Map.Entry<Long, String>> zoneTimeSeries, List<TriageIncident> triageIncidents) {
        try {
            PdfDocument pdf = new PdfDocument();
            Paint paint = new Paint();
            Paint titlePaint = new Paint();
            titlePaint.setTextSize(18);
            titlePaint.setFakeBoldText(true);

            int pageNum = 1;
            int y = 60;
            int pageHeight = 842;
            int pageWidth = 595;
            int margin = 40;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            canvas.drawText("Provider Report", margin, y, titlePaint);
            y += 30;

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateRange = sdf.format(new java.util.Date(startDate)) + " to " + sdf.format(new java.util.Date(endDate));
            canvas.drawText("Date Range: " + dateRange, margin, y, paint);
            y += 30;

            canvas.drawText("Rescue Frequency and Controller Adherence", margin, y, titlePaint);
            y += 25;
            canvas.drawText("Controller Adherence: " + String.format("%.1f", adherence) + "%", margin, y, paint);
            y += 20;
            canvas.drawText("Average Rescue Uses per Day: " + String.format("%.2f", avgRescue), margin, y, paint);
            y += 20;
            canvas.drawText("Total Rescue Uses: " + totalRescue, margin, y, paint);
            y += 30;

            canvas.drawText("Symptom Burden", margin, y, titlePaint);
            y += 25;
            canvas.drawText("Problem Days: " + problemDays, margin, y, paint);
            y += 30;

            canvas.drawText("Zone Distribution", margin, y, titlePaint);
            y += 25;

            int chartStartY = y;
            int chartHeight = 120;
            int totalZoneDays = zoneCounts.get("Green") + zoneCounts.get("Yellow") + zoneCounts.get("Red");
            int maxCount = Math.max(1, Math.max(zoneCounts.get("Green"), Math.max(zoneCounts.get("Yellow"), zoneCounts.get("Red"))));

            Paint greenPaint = new Paint();
            greenPaint.setColor(Color.parseColor("#2ecc71"));
            Paint yellowPaint = new Paint();
            yellowPaint.setColor(Color.parseColor("#f1c40f"));
            Paint redPaint = new Paint();
            redPaint.setColor(Color.parseColor("#e74c3c"));

            int barWidth = 60;
            int spacing = 30;
            int startX = margin + 80;

            int greenHeight = totalZoneDays > 0 ? (int) ((zoneCounts.get("Green") * chartHeight) / maxCount) : 0;
            int yellowHeight = totalZoneDays > 0 ? (int) ((zoneCounts.get("Yellow") * chartHeight) / maxCount) : 0;
            int redHeight = totalZoneDays > 0 ? (int) ((zoneCounts.get("Red") * chartHeight) / maxCount) : 0;

            canvas.drawRect(startX, chartStartY + chartHeight - greenHeight, startX + barWidth, chartStartY + chartHeight, greenPaint);
            canvas.drawText("Green: " + zoneCounts.get("Green"), startX, chartStartY + chartHeight + 20, paint);

            canvas.drawRect(startX + barWidth + spacing, chartStartY + chartHeight - yellowHeight, startX + barWidth + spacing + barWidth, chartStartY + chartHeight, yellowPaint);
            canvas.drawText("Yellow: " + zoneCounts.get("Yellow"), startX + barWidth + spacing, chartStartY + chartHeight + 20, paint);

            canvas.drawRect(startX + (barWidth + spacing) * 2, chartStartY + chartHeight - redHeight, startX + (barWidth + spacing) * 2 + barWidth, chartStartY + chartHeight, redPaint);
            canvas.drawText("Red: " + zoneCounts.get("Red"), startX + (barWidth + spacing) * 2, chartStartY + chartHeight + 20, paint);

            Paint axisPaint = new Paint();
            axisPaint.setStrokeWidth(2);
            axisPaint.setColor(Color.BLACK);
            canvas.drawLine(startX - 10, chartStartY, startX - 10, chartStartY + chartHeight, axisPaint);
            canvas.drawLine(startX - 10, chartStartY + chartHeight, startX + (barWidth + spacing) * 2 + barWidth + 20, chartStartY + chartHeight, axisPaint);

            y = chartStartY + chartHeight + 50;

            canvas.drawText("Zone Over Time", margin, y, titlePaint);
            y += 25;

            int lineChartY = y;
            int lineChartHeight = 120;
            int lineChartWidth = 450;
            int lineStartX = margin + 30;

            Paint axisLinePaint = new Paint();
            axisLinePaint.setStrokeWidth(2);
            axisLinePaint.setColor(Color.BLACK);
            canvas.drawLine(lineStartX - 10, lineChartY, lineStartX - 10, lineChartY + lineChartHeight, axisLinePaint);
            canvas.drawLine(lineStartX - 10, lineChartY + lineChartHeight, lineStartX + lineChartWidth + 10, lineChartY + lineChartHeight, axisLinePaint);

            canvas.drawText("Green", lineStartX + lineChartWidth + 20, lineChartY + 20, greenPaint);
            canvas.drawText("Yellow", lineStartX + lineChartWidth + 20, lineChartY + 60, yellowPaint);
            canvas.drawText("Red", lineStartX + lineChartWidth + 20, lineChartY + 100, redPaint);

            if (!zoneTimeSeries.isEmpty()) {
                Paint linePaint = new Paint();
                linePaint.setStrokeWidth(3);
                linePaint.setColor(Color.BLACK);

                int prevX = -1;
                int prevY = -1;
                long minTime = zoneTimeSeries.get(0).getKey();
                long maxTime = zoneTimeSeries.get(zoneTimeSeries.size() - 1).getKey();
                long timeRange = maxTime - minTime;
                if (timeRange == 0) timeRange = 1;

                for (int i = 0; i < zoneTimeSeries.size() && i < 50; i++) {
                    Map.Entry<Long, String> entry = zoneTimeSeries.get(i);
                    Long timestamp = entry.getKey();
                    String zone = entry.getValue();
                    int zoneValue = zone.equals("Green") ? 0 : (zone.equals("Yellow") ? 1 : 2);

                    int x = lineStartX + (int) ((timestamp - minTime) * lineChartWidth / timeRange);
                    int yPos = lineChartY + lineChartHeight - (zoneValue * (lineChartHeight / 2));

                    Paint pointPaint = new Paint();
                    pointPaint.setStyle(Paint.Style.FILL);
                    if (zone.equals("Green")) {
                        pointPaint.setColor(Color.parseColor("#2ecc71"));
                    } else if (zone.equals("Yellow")) {
                        pointPaint.setColor(Color.parseColor("#f1c40f"));
                    } else {
                        pointPaint.setColor(Color.parseColor("#e74c3c"));
                    }

                    canvas.drawCircle(x, yPos, 6, pointPaint);

                    if (prevX != -1) {
                        canvas.drawLine(prevX, prevY, x, yPos, linePaint);
                    }

                    prevX = x;
                    prevY = yPos;
                }
            } else {
                Paint placeholderPaint = new Paint();
                placeholderPaint.setStrokeWidth(1);
                placeholderPaint.setColor(Color.GRAY);
                placeholderPaint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(lineStartX, lineChartY, lineStartX + lineChartWidth, lineChartY + lineChartHeight, placeholderPaint);
                canvas.drawText("No zone data in date range", lineStartX + 50, lineChartY + lineChartHeight / 2, paint);
            }

            y = lineChartY + lineChartHeight + 30;

            if (!triageIncidents.isEmpty()) {
                if (y > pageHeight - 100) {
                    pdf.finishPage(page);
                    pageNum++;
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                    page = pdf.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 60;
                }

                canvas.drawText("Notable Triage Incidents", margin, y, titlePaint);
                y += 25;

                for (TriageIncident incident : triageIncidents) {
                    if (y > pageHeight - 80) {
                        pdf.finishPage(page);
                        pageNum++;
                        pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                        page = pdf.startPage(pageInfo);
                        canvas = page.getCanvas();
                        y = 60;
                    }

                    canvas.drawText(sdf.format(new java.util.Date(incident.getTimestamp())), margin, y, paint);
                    y += 20;

                    String decision = incident.getDecision();
                    if (decision != null) {
                        canvas.drawText("Decision: " + decision, margin, y, paint);
                        y += 20;
                    }

                    if (incident.getPeakFlowReading() != null && !incident.getPeakFlowReading().isEmpty()) {
                        canvas.drawText("Peak Flow: " + incident.getPeakFlowReading(), margin, y, paint);
                        y += 20;
                    }

                    y += 10;
                }
            }

            pdf.finishPage(page);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = dateFormat.format(new java.util.Date());
            String filename = "provider_report_" + timestamp + ".pdf";

            File file;
            try {
                File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                file = new File(downloadsDir, filename);
            } catch (Exception e) {
                file = new File(requireContext().getExternalFilesDir(null), filename);
            }

            FileOutputStream out = new FileOutputStream(file);
            pdf.writeTo(out);
            pdf.close();
            out.close();

            Toast.makeText(requireContext(), "PDF saved to Downloads!\nFile: " + filename, Toast.LENGTH_LONG).show();

            try {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/pdf");
                shareIntent.putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(file));
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Provider Report");
                startActivity(Intent.createChooser(shareIntent, "Share Provider Report"));
            } catch (Exception shareError) {
            }

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
