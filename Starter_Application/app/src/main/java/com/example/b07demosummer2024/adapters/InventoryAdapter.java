package com.example.b07demosummer2024.adapters;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.models.MedicineCanister;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InventoryAdapter extends BaseRecyclerAdapter<MedicineCanister, InventoryAdapter.ViewHolder> {

    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(MedicineCanister canister);
    }

    public InventoryAdapter(List<MedicineCanister> canisters, OnItemClickListener listener) {
        super(canisters);
        this.listener = listener;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.item_inventory;
    }

    @Override
    protected ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    protected void bindViewHolder(ViewHolder holder, MedicineCanister canister, int position) {
        holder.medicineName.setText(canister.getMedicineName());
        holder.dosesLeft.setText("Doses Left: " + canister.getDosesLeft() + "/" + canister.getTotalDoses());

        SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        holder.expiryDate.setText("Expires: " + sdf.format(new Date(canister.getExpiryDate())));

        // Handle alerts
        boolean isExpired = System.currentTimeMillis() > canister.getExpiryDate();
        boolean isLow = canister.getTotalDoses() > 0 && (double) canister.getDosesLeft() / canister.getTotalDoses() <= 0.20;

        if (isExpired) {
            holder.alertText.setText("Alert: Expired!");
            holder.alertText.setTextColor(Color.RED);
            holder.alertText.setVisibility(View.VISIBLE);
        } else if (isLow) {
            holder.alertText.setText("Alert: Low on doses!");
            holder.alertText.setTextColor(Color.parseColor("#FFA500")); // Orange
            holder.alertText.setVisibility(View.VISIBLE);
        } else {
            holder.alertText.setVisibility(View.GONE);
        }

        String lastUpdater = canister.getLastMarkedBy();
        if (lastUpdater != null && !lastUpdater.isEmpty()) {
            // Capitalize the first letter for better display (e.g., "parent" -> "Parent")
            String displayUpdater = lastUpdater.substring(0, 1).toUpperCase() + lastUpdater.substring(1);
            holder.lastUpdatedBy.setText("Last updated by: " + displayUpdater);
            holder.lastUpdatedBy.setVisibility(View.VISIBLE);
        } else {
            // Hide the TextView if there's no information
            holder.lastUpdatedBy.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(canister));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView medicineName, dosesLeft, expiryDate, alertText, lastUpdatedBy;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            medicineName = itemView.findViewById(R.id.textMedicineName);
            dosesLeft = itemView.findViewById(R.id.textDosesLeft);
            expiryDate = itemView.findViewById(R.id.textExpiryDate);
            alertText = itemView.findViewById(R.id.textAlert);
            lastUpdatedBy = itemView.findViewById(R.id.textlastUpdatedBy);
        }
    }
}
