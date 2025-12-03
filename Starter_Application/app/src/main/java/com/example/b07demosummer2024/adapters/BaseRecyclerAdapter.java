package com.example.b07demosummer2024.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

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

    public void updateItems(List<T> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    public List<T> getItems() {
        return items;
    }

    protected abstract int getLayoutResourceId();

    protected abstract VH createViewHolder(View view);

    protected abstract void bindViewHolder(VH holder, T item, int position);
}