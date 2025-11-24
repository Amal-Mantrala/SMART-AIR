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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.auth.ProviderSharingService;
import com.example.b07demosummer2024.models.ProviderAccess;
import com.example.b07demosummer2024.viewmodels.ProviderSharingAdapter;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ManageProviderSharingFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProviderSharingAdapter adapter;
    private List<ProviderAccess> providersList;
    private ProviderSharingService sharingService;
    private TextView emptyStateText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_provider_sharing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharingService = new ProviderSharingService();
        providersList = new ArrayList<>();

        recyclerView = view.findViewById(R.id.recyclerViewProviders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ProviderSharingAdapter(providersList, sharingService, getParentId());
        recyclerView.setAdapter(adapter);

        Button backButton = view.findViewById(R.id.buttonBack);
        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        loadProviders();
    }

    private String getParentId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
    }

    private void loadProviders() {
        String parentId = getParentId();
        if (parentId.isEmpty()) {
            Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        sharingService.getActiveProvidersForParent(parentId, new ProviderSharingService.ProviderListCallback() {
            @Override
            public void onResult(List<ProviderAccess> providers) {
                providersList.clear();
                providersList.addAll(providers);
                adapter.notifyDataSetChanged();

                if (providers.isEmpty()) {
                    Toast.makeText(getContext(), "No active providers found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Error loading providers: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
