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

    /**
     * Listener interface invoked when the parent chooses to open a child profile
     * from the Manage Children UI.
     */
    public interface OnOpenChildListener {
        void onOpenChild(User child);
    }

    private OnOpenChildListener openChildListener;

    @Override
    protected void bindViewHolder(ChildViewHolder holder, User child, int position) {
        holder.childName.setText(child.getName());
        holder.openButton.setOnClickListener(v -> {
            if (openChildListener != null && child != null) {
                openChildListener.onOpenChild(child);
            }
        });
    }

    public void setOnOpenChildListener(OnOpenChildListener listener) {
        this.openChildListener = listener;
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView childName;
        android.widget.Button openButton;

        ChildViewHolder(View itemView) {
            super(itemView);
            childName = itemView.findViewById(R.id.childName);
            openButton = itemView.findViewById(R.id.buttonOpenAsChild);
        }
    }
}
