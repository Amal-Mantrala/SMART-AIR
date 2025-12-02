package com.example.b07demosummer2024.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.models.User;
import com.example.b07demosummer2024.viewmodels.ChildrenAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class ManageChildrenFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChildrenAdapter adapter;
    private List<User> childrenList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_children, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewChildren);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        childrenList = new ArrayList<>();
        adapter = new ChildrenAdapter(childrenList);
        // Allow parent to open a child's UI without needing their password
        adapter.setOnOpenChildListener(child -> {
            if (getContext() == null || child == null || child.getUid() == null) return;
            // set impersonation and navigate into the child home
            com.example.b07demosummer2024.auth.ImpersonationService.setImpersonatedChild(getContext(), child.getUid());
            // navigate to child home - keep transaction on back stack so parent can return
            getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new com.example.b07demosummer2024.fragments.ChildHomeFragment())
                .addToBackStack(null)
                .commit();
        });
        recyclerView.setAdapter(adapter);

        // Open schedule dialog when parent taps Set Schedule
        adapter.setOnSetScheduleListener(child -> {
            if (getContext() == null || child == null || child.getUid() == null) return;
            com.example.b07demosummer2024.fragments.WeekScheduleDialogFragment dialog = com.example.b07demosummer2024.fragments.WeekScheduleDialogFragment.newInstance(child.getUid());
            dialog.show(getParentFragmentManager(), "week_schedule_dialog");
        });

        loadChildren();
    }

    private void loadChildren() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String parentId = auth.getCurrentUser().getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(parentId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    List<String> childIds = (List<String>) document.get("children");
                    if (childIds != null) {
                        childrenList.clear();
                        for (String childId : childIds) {
                            db.collection("users").document(childId).get().addOnCompleteListener(childTask -> {
                                if (childTask.isSuccessful()) {
                                    DocumentSnapshot childDocument = childTask.getResult();
                                        if (childDocument.exists()) {
                                            User child = childDocument.toObject(User.class);
                                            if (child != null) {
                                                // persist document id so adapter/actions can use child id
                                                child.setUid(childDocument.getId());
                                                childrenList.add(child);
                                            }
                                        adapter.notifyDataSetChanged();
                                    }
                                }
                            });
                        }
                    }
                }
            }
        });
    }
}
