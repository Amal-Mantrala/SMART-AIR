package com.example.b07demosummer2024.viewmodels;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.adapters.BaseRecyclerAdapter;
import com.example.b07demosummer2024.models.User;
import java.util.List;

public class ChildrenAdapter extends BaseRecyclerAdapter<User, ChildrenAdapter.ChildViewHolder> {

    public ChildrenAdapter(List<User> childrenList) {
        super(childrenList);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.item_child;
    }

    @Override
    protected ChildViewHolder createViewHolder(View view) {
        return new ChildViewHolder(view);
    }

    @Override
    protected void bindViewHolder(ChildViewHolder holder, User child, int position) {
        holder.childName.setText(child.getName());
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView childName;

        ChildViewHolder(View itemView) {
            super(itemView);
            childName = itemView.findViewById(R.id.childName);
        }
    }
}
