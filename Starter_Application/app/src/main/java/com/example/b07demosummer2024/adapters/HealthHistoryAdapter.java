package com.example.b07demosummer2024.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.models.MedicineLog;
import com.example.b07demosummer2024.models.SymptomLog;
import com.example.b07demosummer2024.models.DailyWellnessLog;
import com.example.b07demosummer2024.models.ZoneLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HealthHistoryAdapter extends RecyclerView.Adapter<HealthHistoryAdapter.ViewHolder> {

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_health_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
        HealthHistoryItem item = allItems.get(pos);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return allItems.size();
    }

    public List<HealthHistoryItem> getItems() {
        return allItems;
    }

    // Method to update sharing settings for items
    public void setSharingSettings(java.util.Map<String, Boolean> sharingSettings) {
        for (HealthHistoryItem item : allItems) {
            // Determine field name based on item title
            String fieldName = null;
            if (item.title.equals("Medicine Log")) {
                fieldName = "medicine";
            } else if (item.title.equals("Symptom Log")) {
                fieldName = "symptoms";
            } else if (item.title.equals("Daily Check-in")) {
                fieldName = "dailyWellness";
            } else if (item.title.equals("Zone Change")) {
                fieldName = "zone";
            }
            
            if (fieldName != null && sharingSettings.containsKey(fieldName)) {
                item.isShared = sharingSettings.get(fieldName);
            }
        }
        notifyDataSetChanged();
    }

    private final List<HealthHistoryItem> allItems = new ArrayList<>();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public void updateData(List<MedicineLog> medicineData,
                           List<SymptomLog> symptomData,
                           List<DailyWellnessLog> wellnessData,
                           List<ZoneLog> zoneData) {

        allItems.clear();

        if (medicineData != null) {
            for (MedicineLog m : medicineData) {
                allItems.add(new HealthHistoryItem(
                        "Medicine Log",
                        m.getTimestamp(),
                        formatMedicine(m),
                        "none"
                ));
            }
        }

        if (symptomData != null) {
            for (SymptomLog s : symptomData) {
                allItems.add(new HealthHistoryItem(
                        "Symptom Log",
                        s.getTimestamp(),
                        formatSymptom(s),
                        "none"
                ));
            }
        }

        if (wellnessData != null) {
            for (DailyWellnessLog w : wellnessData) {
                allItems.add(new HealthHistoryItem(
                        "Daily Check-in",
                        w.getTimestamp(),
                        formatWellness(w),
                        w.getEnteredBy()
                ));
            }
        }

        if (zoneData != null) {
            for (ZoneLog z : zoneData) {
                allItems.add(new HealthHistoryItem(
                        "Zone Change",
                        z.getTimestamp(),
                        "Zone: " + z.getZone() + "\nPEF: " + z.getPefValue(),
                        "none"
                ));
            }
        }

        allItems.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        notifyDataSetChanged();
    }

    public void addAdherenceItem(HealthHistoryItem item) {
        allItems.add(0, item);
        allItems.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        notifyDataSetChanged();
    }

    // Expose the currently displayed list (used for PDF export)
    public List<HealthHistoryItem> getDisplayedItems() {
        return new ArrayList<>(allItems);
    }

    private String formatMedicine(MedicineLog m) {
        StringBuilder sb = new StringBuilder();

        if (m.getMedicineType() != null) sb.append(m.getMedicineType()).append(" - ");
        if (m.getMedicineName() != null) sb.append(m.getMedicineName());
        if (m.getDosage() != null && !m.getDosage().isEmpty())
            sb.append(" - ").append(m.getDosage());
        if (m.getNotes() != null && !m.getNotes().isEmpty())
            sb.append("\n").append(m.getNotes());

        return sb.toString();
    }

    private String formatSymptom(SymptomLog s) {
        StringBuilder sb = new StringBuilder();

        if (s.getSymptoms() != null && !s.getSymptoms().isEmpty())
            sb.append("Symptoms: ").append(String.join(", ", s.getSymptoms()));

        if (sb.length() > 0) sb.append(" ");
        sb.append("(Severity: ").append(s.getOverallSeverity()).append("/10)");

        if (s.getTags() != null && !s.getTags().isEmpty())
            sb.append("\nTags: ").append(String.join(", ", s.getTags()));

        if (s.getNotes() != null && !s.getNotes().isEmpty())
            sb.append("\n").append(s.getNotes());

        return sb.toString();
    }

    private String formatWellness(DailyWellnessLog w) {
        StringBuilder sb = new StringBuilder();

        if (w.getMood() != null && !w.getMood().isEmpty())
            sb.append("Mood: ").append(w.getMood());

        if (w.getEnergyLevel() > 0)
            sb.append(", Energy: ").append(w.getEnergyLevel()).append("/5");

        if (w.getSleepQuality() > 0)
            sb.append(", Sleep: ").append(w.getSleepQuality()).append("/5");

        if (w.getOverallFeeling() > 0)
            sb.append(", Feeling: ").append(w.getOverallFeeling()).append("/5");

        if (w.getNotes() != null && !w.getNotes().isEmpty())
            sb.append("\n").append(w.getNotes());

        return sb.toString();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, dateText, detailText;
        TextView sourceTag;
        TextView sharedTag;

        ViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.textTitle);
            dateText = itemView.findViewById(R.id.textDate);
            detailText = itemView.findViewById(R.id.textDetail);
            sourceTag = itemView.findViewById(R.id.textEntrySource);
            sharedTag = itemView.findViewById(R.id.textSharedTag);
        }

        void bind(HealthHistoryItem item) {
            titleText.setText(item.title);
            dateText.setText(
                    new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                            .format(new Date(item.timestamp))
            );
            detailText.setText(item.details);

            if (item.source != null) {
                sourceTag.setVisibility(View.VISIBLE);

                if (item.source.equals("child")) {
                    sourceTag.setText("child-entered");
                    sourceTag.setBackgroundResource(R.drawable.tag_background); // green
                }
                if (item.source.equals("none")) {
                    sourceTag.setVisibility(View.GONE);
                }
            } else {
                sourceTag.setVisibility(View.GONE);
            }

            // Show shared tag if this item is shared with provider
            if (item.isShared) {
                sharedTag.setVisibility(View.VISIBLE);
            } else {
                sharedTag.setVisibility(View.GONE);
            }
        }
    }

    public static class HealthHistoryItem {
        public final String title;
        public final long timestamp;
        public final String details;
        public final String source;
        public boolean isShared;

        public HealthHistoryItem(String title, long timestamp, String details, String source) {
            this.title = title;
            this.timestamp = timestamp;
            this.details = details;
            this.source = source;
            this.isShared = false;
        }
        public String getFormattedDate() {
            return new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    .format(new Date(timestamp));
        }
    }
}
