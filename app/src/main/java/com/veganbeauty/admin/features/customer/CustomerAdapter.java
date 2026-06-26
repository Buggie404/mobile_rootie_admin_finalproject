package com.veganbeauty.admin.features.customer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.data.local.entities.CustomerEntity;
import com.veganbeauty.admin.core.utils.ImageUtils;
import com.veganbeauty.admin.databinding.ItemCustomerBinding;
import java.util.List;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.ViewHolder> {

    public interface OnDetailClickListener {
        void onDetailClick(CustomerEntity customer);
    }

    public interface OnNoteClickListener {
        void onNoteClick(CustomerEntity customer);
    }

    private List<CustomerEntity> items;
    private final OnDetailClickListener onDetailClickListener;
    private final OnNoteClickListener onNoteClickListener;

    public CustomerAdapter(
        List<CustomerEntity> items,
        OnDetailClickListener onDetailClickListener,
        OnNoteClickListener onNoteClickListener
    ) {
        this.items = items;
        this.onDetailClickListener = onDetailClickListener;
        this.onNoteClickListener = onNoteClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCustomerBinding binding = ItemCustomerBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CustomerEntity item = items.get(position);
        ItemCustomerBinding binding = holder.binding;

        binding.txtName.setText(item.getName());
        binding.txtSpendingInfo.setText("Chi tiêu: " + formatSpending(item.getSpending()) + " • Cuối: " + item.getLastActive());

        // Set tier badge styling
        binding.badgeTier.setText(item.getTier());
        String tier = item.getTier() != null ? item.getTier().toLowerCase() : "";
        switch (tier) {
            case "vip":
                binding.badgeTier.setTextColor(Color.parseColor("#4F6544"));
                binding.badgeTier.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5E8DA")));
                break;
            case "vàng":
                binding.badgeTier.setTextColor(Color.parseColor("#FFFFFF"));
                binding.badgeTier.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C69C2C")));
                break;
            case "bạc":
                binding.badgeTier.setTextColor(Color.parseColor("#FFFFFF"));
                binding.badgeTier.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                break;
            default: // Thường / Normal
                binding.badgeTier.setTextColor(Color.parseColor("#616161"));
                binding.badgeTier.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F5F5")));
                break;
        }

        // Load avatar
        if (item.getAvatar() != null && !item.getAvatar().isEmpty()) {
            ImageUtils.loadImage(binding.getRoot().getContext(), binding.imgAvatar, item.getAvatar(), R.drawable.imv_avatar);
        } else {
            binding.imgAvatar.setImageResource(R.drawable.imv_avatar);
        }

        binding.btnDetail.setOnClickListener(v -> {
            if (onDetailClickListener != null) {
                onDetailClickListener.onDetailClick(item);
            }
        });
        binding.btnNote.setOnClickListener(v -> {
            if (onNoteClickListener != null) {
                onNoteClickListener.onNoteClick(item);
            }
        });
        binding.getRoot().setOnClickListener(v -> {
            if (onDetailClickListener != null) {
                onDetailClickListener.onDetailClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public void updateData(List<CustomerEntity> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    private String formatSpending(long spending) {
        double million = spending / 1000000.0;
        String formatted = String.format(java.util.Locale.US, "%.1f", million);
        if (formatted.endsWith(".0")) {
            return formatted.substring(0, formatted.length() - 2) + "M";
        } else {
            return formatted + "M";
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemCustomerBinding binding;

        public ViewHolder(ItemCustomerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
