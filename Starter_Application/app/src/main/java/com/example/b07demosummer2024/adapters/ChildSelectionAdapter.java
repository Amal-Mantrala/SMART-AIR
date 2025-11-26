package com.example.b07demosummer2024.adapters;

import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.models.ChildSelection;

import java.util.List;

public class ChildSelectionAdapter extends BaseRecyclerAdapter<ChildSelection, ChildSelectionAdapter.ViewHolder> {

    public ChildSelectionAdapter(List<ChildSelection> children) {
        super(children);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.item_child_selection;
    }

    @Override
    protected ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    protected void bindViewHolder(ViewHolder holder, ChildSelection child, int position) {
        holder.textChildName.setText(child.getChildName());
        holder.checkboxChild.setChecked(child.isSelected());
        
        holder.checkboxChild.setOnCheckedChangeListener((buttonView, isChecked) -> {
            child.setSelected(isChecked);
        });
        
        holder.itemView.setOnClickListener(v -> {
            child.setSelected(!child.isSelected());
            holder.checkboxChild.setChecked(child.isSelected());
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textChildName;
        CheckBox checkboxChild;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textChildName = itemView.findViewById(R.id.textChildName);
            checkboxChild = itemView.findViewById(R.id.checkboxChild);
        }
    }
}