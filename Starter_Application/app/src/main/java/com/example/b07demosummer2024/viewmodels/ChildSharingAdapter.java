package com.example.b07demosummer2024.viewmodels;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.models.ChildSharingSettings;
import com.example.b07demosummer2024.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChildSharingAdapter extends RecyclerView.Adapter<ChildSharingAdapter.ChildSharingViewHolder> {

    private List<User> childrenList;
    private Map<String, String> childIdMap;
    private Map<String, ChildSharingSettings> sharingSettings;
    private String providerId;
    private String parentId;
    private OnToggleChangeListener toggleListener;

    public interface OnToggleChangeListener {
        void onToggleChanged(String childId, String fieldName, boolean isEnabled);
    }

    public ChildSharingAdapter(List<User> childrenList, Map<String, String> childIdMap, Map<String, ChildSharingSettings> sharingSettings, String parentId, String providerId) {
        this.childrenList = childrenList != null ? childrenList : new ArrayList<>();
        this.childIdMap = childIdMap != null ? childIdMap : new HashMap<>();
        this.sharingSettings = sharingSettings != null ? sharingSettings : new HashMap<>();
        this.parentId = parentId;
        this.providerId = providerId;
    }

    public void setToggleListener(OnToggleChangeListener listener) {
        this.toggleListener = listener;
    }

    @NonNull
    @Override
    public ChildSharingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_child_sharing, parent, false);
        return new ChildSharingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildSharingViewHolder holder, int position) {
        User child = childrenList.get(position);
        String childId = child.getName() != null ? childIdMap.get(child.getName()) : null;

        holder.childName.setText(child.getName() != null ? child.getName() : "Unknown");

        ChildSharingSettings settings = childId != null ? sharingSettings.get(childId) : null;

        Map<String, Boolean> sharedFields = settings != null && settings.getSharedFields() != null ? settings.getSharedFields() : new HashMap<>();

        // Set up switches for granular fields
        holder.switchRescueLogs.setChecked(sharedFields.getOrDefault("rescueLogs", false));
        holder.switchRescueLogs.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (toggleListener != null && childId != null) toggleListener.onToggleChanged(childId, "rescueLogs", isChecked);
        });

        holder.switchControllerSummary.setChecked(sharedFields.getOrDefault("controllerSummary", false));
        holder.switchControllerSummary.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (toggleListener != null && childId != null) toggleListener.onToggleChanged(childId, "controllerSummary", isChecked);
        });

        holder.switchSymptoms.setChecked(sharedFields.getOrDefault("symptoms", false));
        holder.switchSymptoms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (toggleListener != null && childId != null) toggleListener.onToggleChanged(childId, "symptoms", isChecked);
        });

        holder.switchTriggers.setChecked(sharedFields.getOrDefault("triggers", false));
        holder.switchTriggers.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (toggleListener != null && childId != null) toggleListener.onToggleChanged(childId, "triggers", isChecked);
        });

        holder.switchPeakFlow.setChecked(sharedFields.getOrDefault("peakFlow", false));
        holder.switchPeakFlow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (toggleListener != null && childId != null) toggleListener.onToggleChanged(childId, "peakFlow", isChecked);
        });

        holder.switchTriageIncidents.setChecked(sharedFields.getOrDefault("triageIncidents", false));
        holder.switchTriageIncidents.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (toggleListener != null && childId != null) toggleListener.onToggleChanged(childId, "triageIncidents", isChecked);
        });

        holder.switchSummaryCharts.setChecked(sharedFields.getOrDefault("summaryCharts", false));
        holder.switchSummaryCharts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (toggleListener != null && childId != null) toggleListener.onToggleChanged(childId, "summaryCharts", isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return childrenList.size();
    }

    public void updateSharingSettings(Map<String, ChildSharingSettings> newSettings) {
        this.sharingSettings = newSettings != null ? newSettings : new HashMap<>();
        notifyDataSetChanged();
    }

    static class ChildSharingViewHolder extends RecyclerView.ViewHolder {
        TextView childName;
        TextView textRescueLogs;
        Switch switchRescueLogs;
        TextView textControllerSummary;
        Switch switchControllerSummary;
        TextView textSymptoms;
        Switch switchSymptoms;
        TextView textTriggers;
        Switch switchTriggers;
        TextView textPeakFlow;
        Switch switchPeakFlow;
        TextView textTriageIncidents;
        Switch switchTriageIncidents;
        TextView textSummaryCharts;
        Switch switchSummaryCharts;

        ChildSharingViewHolder(View itemView) {
            super(itemView);
            childName = itemView.findViewById(R.id.textChildName);
            textRescueLogs = itemView.findViewById(R.id.textRescueLogs);
            switchRescueLogs = itemView.findViewById(R.id.switchRescueLogs);

            textControllerSummary = itemView.findViewById(R.id.textControllerSummary);
            switchControllerSummary = itemView.findViewById(R.id.switchControllerSummary);

            textSymptoms = itemView.findViewById(R.id.textSymptoms);
            switchSymptoms = itemView.findViewById(R.id.switchSymptoms);

            textTriggers = itemView.findViewById(R.id.textTriggers);
            switchTriggers = itemView.findViewById(R.id.switchTriggers);

            textPeakFlow = itemView.findViewById(R.id.textPeakFlow);
            switchPeakFlow = itemView.findViewById(R.id.switchPeakFlow);

            textTriageIncidents = itemView.findViewById(R.id.textTriageIncidents);
            switchTriageIncidents = itemView.findViewById(R.id.switchTriageIncidents);

            textSummaryCharts = itemView.findViewById(R.id.textSummaryCharts);
            switchSummaryCharts = itemView.findViewById(R.id.switchSummaryCharts);
        }
    }
}
