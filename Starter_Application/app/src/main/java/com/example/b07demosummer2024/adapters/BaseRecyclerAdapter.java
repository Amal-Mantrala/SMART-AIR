package com.example.b07demosummer2024.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Base adapter class to reduce code duplication across RecyclerView adapters
 */
public abstract class BaseRecyclerAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    protected List<T> items;

    public BaseRecyclerAdapter(List<T> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(getLayoutResourceId(), parent, false);
        return createViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        T item = items.get(position);
        bindViewHolder(holder, item, position);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    /**
     * Update the list of items and notify the adapter
     */
    public void updateItems(List<T> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    /**
     * Get the current list of items
     */
    public List<T> getItems() {
        return items;
    }

    /**
     * Abstract method to provide the layout resource ID
     */
    protected abstract int getLayoutResourceId();

    /**
     * Abstract method to create the ViewHolder
     */
    protected abstract VH createViewHolder(View view);

    /**
     * Abstract method to bind data to the ViewHolder
     */
    protected abstract void bindViewHolder(VH holder, T item, int position);
}