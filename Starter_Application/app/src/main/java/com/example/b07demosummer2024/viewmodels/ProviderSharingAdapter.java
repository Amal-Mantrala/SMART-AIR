package com.example.b07demosummer2024.viewmodels;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.auth.ProviderSharingService;
import com.example.b07demosummer2024.models.ChildSharingSettings;
import com.example.b07demosummer2024.models.ProviderAccess;
import com.example.b07demosummer2024.models.SharingSettings;
import com.example.b07demosummer2024.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderSharingAdapter extends RecyclerView.Adapter<ProviderSharingAdapter.ProviderSharingViewHolder> {

    private List<ProviderAccess> providersList;
    private Map<String, List<User>> childrenMap;
    private Map<String, Map<String, String>> childIdMaps;
    private Map<String, SharingSettings> settingsMap;
    private ProviderSharingService sharingService;
    private String parentId;

    public ProviderSharingAdapter(List<ProviderAccess> providersList, ProviderSharingService sharingService, String parentId) {
        this.providersList = providersList != null ? providersList : new ArrayList<>();
        this.childrenMap = new HashMap<>();
        this.childIdMaps = new HashMap<>();
        this.settingsMap = new HashMap<>();
        this.sharingService = sharingService;
        this.parentId = parentId;
    }

    @NonNull
    @Override
    public ProviderSharingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_provider_sharing, parent, false);
        return new ProviderSharingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProviderSharingViewHolder holder, int position) {
        ProviderAccess provider = providersList.get(position);
        String providerId = provider.getProviderId();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (providerId != null) {
            db.collection("users").document(providerId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    User providerUser = task.getResult().toObject(User.class);
                    if (providerUser != null) {
                        holder.providerName.setText(providerUser.getName() != null ? providerUser.getName() : "Provider");
                    }
                }
                holder.providerId.setText("ID: " + (providerId != null ? providerId.substring(0, Math.min(8, providerId.length())) : "Unknown"));
            });
        }

        List<User> children = childrenMap.get(providerId);
        Map<String, String> childIdMap = childIdMaps.get(providerId);
        if (children == null) {
            children = new ArrayList<>();
            childIdMap = new HashMap<>();
            loadChildrenForProvider(providerId, children, childIdMap, holder);
        } else {
            SharingSettings settings = settingsMap.get(providerId);
            if (settings == null) {
                loadSharingSettings(providerId, holder, children, childIdMap);
            } else {
                setupChildrenRecyclerView(holder, providerId, children, childIdMap);
            }
        }

        holder.revokeButton.setOnClickListener(v -> {
            sharingService.revokeProviderAccess(parentId, providerId);
            Toast.makeText(v.getContext(), "Access revoked", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadChildrenForProvider(String providerId, List<User> children, Map<String, String> childIdMap, ProviderSharingViewHolder holder) {
        // Load sharingSettings for this parent/provider to determine which children are shared
        String currentParentId = this.parentId;
        sharingService.getSharingSettings(currentParentId, providerId, new ProviderSharingService.SettingsCallback() {
            @Override
            public void onResult(SharingSettings settings) {
                settingsMap.put(providerId, settings);

                Map<String, ChildSharingSettings> csMap = settings != null && settings.getChildSettings() != null ? settings.getChildSettings() : new HashMap<>();
                if (csMap.isEmpty()) {
                    // No shared children for this provider
                    childrenMap.put(providerId, new ArrayList<>());
                    childIdMaps.put(providerId, new HashMap<>());
                    setupChildrenRecyclerView(holder, providerId, new ArrayList<>(), new HashMap<>());
                    return;
                }

                List<String> childIds = new ArrayList<>(csMap.keySet());
                if (childIds.isEmpty()) {
                    childrenMap.put(providerId, new ArrayList<>());
                    childIdMaps.put(providerId, new HashMap<>());
                    setupChildrenRecyclerView(holder, providerId, new ArrayList<>(), new HashMap<>());
                    return;
                }

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                int[] completed = {0};
                int total = childIds.size();

                for (String childId : childIds) {
                    db.collection("users").document(childId).get().addOnCompleteListener(childTask -> {
                        if (childTask.isSuccessful() && childTask.getResult().exists()) {
                            User child = childTask.getResult().toObject(User.class);
                            if (child != null) {
                                children.add(child);
                                if (child.getName() != null) {
                                    childIdMap.put(child.getName(), childId);
                                }
                            }
                        }
                        completed[0]++;
                        if (completed[0] == total) {
                            childrenMap.put(providerId, children);
                            childIdMaps.put(providerId, childIdMap);
                            setupChildrenRecyclerView(holder, providerId, children, childIdMap);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                // On error, show empty children list rather than all parent children
                childrenMap.put(providerId, new ArrayList<>());
                childIdMaps.put(providerId, new HashMap<>());
                settingsMap.put(providerId, new SharingSettings());
                setupChildrenRecyclerView(holder, providerId, new ArrayList<>(), new HashMap<>());
            }
        });
    }

    private void loadSharingSettings(String providerId, ProviderSharingViewHolder holder, List<User> children, Map<String, String> childIdMap) {
        sharingService.getSharingSettings(parentId, providerId, new ProviderSharingService.SettingsCallback() {
            @Override
            public void onResult(SharingSettings settings) {
                settingsMap.put(providerId, settings);
                setupChildrenRecyclerView(holder, providerId, children, childIdMap);
            }

            @Override
            public void onError(String error) {
                settingsMap.put(providerId, new SharingSettings());
                setupChildrenRecyclerView(holder, providerId, children, childIdMap);
            }
        });
    }

    private void setupChildrenRecyclerView(ProviderSharingViewHolder holder, String providerId, List<User> children, Map<String, String> childIdMap) {
        SharingSettings settings = settingsMap.get(providerId);
        Map<String, ChildSharingSettings> childSettings = settings != null && settings.getChildSettings() != null ? settings.getChildSettings() : new HashMap<>();

        ChildSharingAdapter adapter = new ChildSharingAdapter(children, childIdMap, childSettings, parentId, providerId);
        adapter.setToggleListener((childId, fieldName, isEnabled) -> {
            ChildSharingSettings childSharingSettings = childSettings.get(childId);
            if (childSharingSettings == null) {
                childSharingSettings = new ChildSharingSettings();
                childSharingSettings.setChildId(childId);
                childSharingSettings.setSharedFields(new HashMap<>());
            }
            if (childSharingSettings.getSharedFields() == null) {
                childSharingSettings.setSharedFields(new HashMap<>());
            }
            childSharingSettings.getSharedFields().put(fieldName, isEnabled);
            sharingService.updateSharingSettings(parentId, providerId, childSharingSettings);
        });

        holder.childrenRecyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.childrenRecyclerView.setAdapter(adapter);
    }

    @Override
    public int getItemCount() {
        return providersList.size();
    }

    static class ProviderSharingViewHolder extends RecyclerView.ViewHolder {
        TextView providerName;
        TextView providerId;
        RecyclerView childrenRecyclerView;
        Button revokeButton;

        ProviderSharingViewHolder(View itemView) {
            super(itemView);
            providerName = itemView.findViewById(R.id.textProviderName);
            providerId = itemView.findViewById(R.id.textProviderId);
            childrenRecyclerView = itemView.findViewById(R.id.recyclerViewChildren);
            revokeButton = itemView.findViewById(R.id.buttonRevokeAccess);
        }
    }
}
