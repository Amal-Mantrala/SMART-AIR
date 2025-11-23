package com.example.b07demosummer2024.viewmodels;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.models.User;
import java.util.List;

public class ChildrenAdapter extends RecyclerView.Adapter<ChildrenAdapter.ChildViewHolder> {

    private List<User> childrenList;

    public ChildrenAdapter(List<User> childrenList) {
        this.childrenList = childrenList;
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_child, parent, false);
        return new ChildViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        User child = childrenList.get(position);
        holder.childName.setText(child.getName());
    }

    @Override
    public int getItemCount() {
        return childrenList.size();
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView childName;

        ChildViewHolder(View itemView) {
            super(itemView);
            childName = itemView.findViewById(R.id.childName);
        }
    }
}
