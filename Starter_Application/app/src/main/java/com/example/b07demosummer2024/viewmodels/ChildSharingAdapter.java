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
        boolean isNameShared = sharedFields.getOrDefault("name", false);

        holder.fieldName.setText("Name");
        holder.fieldSwitch.setChecked(isNameShared);
        holder.fieldSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (toggleListener != null && childId != null) {
                toggleListener.onToggleChanged(childId, "name", isChecked);
            }
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
        TextView fieldName;
        Switch fieldSwitch;

        ChildSharingViewHolder(View itemView) {
            super(itemView);
            childName = itemView.findViewById(R.id.textChildName);
            fieldName = itemView.findViewById(R.id.textFieldName);
            fieldSwitch = itemView.findViewById(R.id.switchField);
        }
    }
}
