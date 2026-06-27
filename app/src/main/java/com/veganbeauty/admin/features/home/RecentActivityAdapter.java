package com.veganbeauty.admin.features.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.utils.ImageUtils;
import com.veganbeauty.admin.databinding.ItemRecentActivityBinding;
import java.util.List;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(RecentActivity activity);
    }

    private final List<RecentActivity> items;
    private final OnItemClickListener onItemClickListener;

    public RecentActivityAdapter(List<RecentActivity> items, OnItemClickListener onItemClickListener) {
        this.items = items;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecentActivityBinding binding = ItemRecentActivityBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentActivity item = items.get(position);
        ItemRecentActivityBinding binding = holder.binding;

        binding.txtTitle.setText(item.getTitle());
        binding.txtSubtitle.setText("Đơn hàng #" + item.getOrderId() + " • " + item.getTimeAgo());
        binding.txtPrice.setText(item.getPrice());
        binding.txtStatus.setText(item.getStatus());

        if (item.getImageRes() != null) {
            ImageUtils.loadImage(binding.getRoot().getContext(), binding.imvProduct, item.getImageRes(), R.color.gray_light);
        } else if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            ImageUtils.loadImage(binding.getRoot().getContext(), binding.imvProduct, item.getImageUrl(), R.color.gray_light);
        } else {
            binding.imvProduct.setImageResource(R.color.gray_light);
        }

        binding.getRoot().setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemRecentActivityBinding binding;

        public ViewHolder(ItemRecentActivityBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
