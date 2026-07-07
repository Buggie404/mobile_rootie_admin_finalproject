package com.veganbeauty.admin.features.order;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.admin.R;
import com.veganbeauty.admin.data.local.entities.OrderEntity;
import com.veganbeauty.admin.data.local.entities.OrderItem;
import com.veganbeauty.admin.databinding.OrderItemBinding;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    public interface OnOrderSelectionToggledListener {
        void onOrderSelectionToggled(OrderEntity order);
    }

    public interface OnCancelClickListener {
        void onCancelClick(OrderEntity order);
    }

    public interface OnApproveClickListener {
        void onApproveClick(OrderEntity order);
    }

    public interface OnItemClickListener {
        void onItemClick(OrderEntity order);
    }

    private List<OrderEntity> items;
    private Set<String> selectedOrderIds;
    private final OnOrderSelectionToggledListener onOrderSelectionToggledListener;
    private final OnCancelClickListener onCancelClickListener;
    private final OnApproveClickListener onApproveClickListener;
    private final OnItemClickListener onItemClickListener;
    private boolean isSelectionAllowed = true;

    public OrderAdapter(
            List<OrderEntity> items,
            Set<String> selectedOrderIds,
            OnOrderSelectionToggledListener onOrderSelectionToggledListener,
            OnCancelClickListener onCancelClickListener,
            OnApproveClickListener onApproveClickListener,
            OnItemClickListener onItemClickListener) {
        this.items = items;
        this.selectedOrderIds = selectedOrderIds != null ? selectedOrderIds : new HashSet<>();
        this.onOrderSelectionToggledListener = onOrderSelectionToggledListener;
        this.onCancelClickListener = onCancelClickListener;
        this.onApproveClickListener = onApproveClickListener;
        this.onItemClickListener = onItemClickListener;
    }

    public OrderAdapter(
            List<OrderEntity> items,
            OnOrderSelectionToggledListener onOrderSelectionToggledListener,
            OnCancelClickListener onCancelClickListener,
            OnApproveClickListener onApproveClickListener,
            OnItemClickListener onItemClickListener) {
        this(items, new HashSet<>(), onOrderSelectionToggledListener, onCancelClickListener, onApproveClickListener, onItemClickListener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        OrderItemBinding binding = OrderItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderEntity item = items.get(position);
        OrderItemBinding binding = holder.binding;

        // Bind Order Code
        binding.txtOrderCode.setText(item.getId());

        // Bind Customer Name
        String shippingName = item.getShippingName();
        binding.txtCustomerName.setText(shippingName != null && !shippingName.trim().isEmpty() ? shippingName : "Khách hàng Rootie");

        // Bind Date/Time
        binding.txtOrderTime.setText(item.getOrderDate() + ", " + item.getOrderTime());

        // Bind Items count
        int totalQty = 0;
        if (item.getItems() != null) {
            for (OrderItem orderItem : item.getItems()) {
                totalQty += orderItem.getQuantity();
            }
        }
        binding.txtItemsCount.setText(totalQty + " sản phẩm");

        // Bind Total price formatted
        binding.txtOrderTotal.setText(formatCurrency(item.getTotalAmount()));

        // Bind Status Badge
        String statusClean = item.getStatus() != null ? item.getStatus().trim() : "";
        binding.txtOrderStatusBadge.setText(statusClean);
        setupStatusBadgeStyle(binding.txtOrderStatusBadge, statusClean);

        // Bind Checkbox
        if (isSelectionAllowed) {
            binding.imgSelectOrder.setVisibility(View.VISIBLE);
            boolean isSelected = selectedOrderIds.contains(item.getId());
            binding.imgSelectOrder.setImageResource(
                    isSelected ? R.drawable.ic_checkbox_checked
                            : R.drawable.ic_radio_primary_unchecked
            );
            binding.imgSelectOrder.setOnClickListener(v -> {
                if (onOrderSelectionToggledListener != null) {
                    onOrderSelectionToggledListener.onOrderSelectionToggled(item);
                }
            });
        } else {
            binding.imgSelectOrder.setVisibility(View.GONE);
            binding.imgSelectOrder.setOnClickListener(null);
        }

        // Setup buttons contextual texts and visibility based on status
        setupActionButtons(binding, item);

        // Setup click listeners
        binding.btnCancelOrder.setOnClickListener(v -> {
            if (onCancelClickListener != null) {
                onCancelClickListener.onCancelClick(item);
            }
        });

        binding.btnApproveOrder.setOnClickListener(v -> {
            if (onApproveClickListener != null) {
                onApproveClickListener.onApproveClick(item);
            }
        });

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

    public void updateData(List<OrderEntity> newItems, Set<String> newSelections, boolean selectionAllowed) {
        this.items = newItems;
        this.selectedOrderIds = newSelections != null ? newSelections : new HashSet<>();
        this.isSelectionAllowed = selectionAllowed;
        notifyDataSetChanged();
    }

    private String formatCurrency(long amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + "đ";
    }

    private void setupStatusBadgeStyle(TextView badgeView, String status) {
        String normalizedStatus = status.toLowerCase();
        int textColor;
        int bgColor;

        if (normalizedStatus.contains("chờ") || normalizedStatus.contains("xử lý")) {
            // Pending / Processing
            textColor = Color.parseColor("#4F6544");
            bgColor = Color.parseColor("#E5E8DA");
        } else if (normalizedStatus.contains("chuẩn bị")) {
            // Preparing
            textColor = Color.parseColor("#B0882E");
            bgColor = Color.parseColor("#FFF2D4");
        } else if (normalizedStatus.contains("giao")) {
            // Delivering
            textColor = Color.parseColor("#2B74B3");
            bgColor = Color.parseColor("#D4E9FF");
        } else if (normalizedStatus.contains("hoàn") || normalizedStatus.contains("tất") || normalizedStatus.contains("thành")) {
            // Completed
            textColor = Color.parseColor("#1B8756");
            bgColor = Color.parseColor("#D4FFE8");
        } else if (normalizedStatus.contains("hủy")) {
            // Cancelled
            textColor = Color.parseColor("#C92F2F");
            bgColor = Color.parseColor("#FFD4D4");
        } else {
            textColor = Color.parseColor("#677559");
            bgColor = Color.parseColor("#F2F3EC");
        }

        badgeView.setTextColor(textColor);
        badgeView.setBackgroundTintList(ColorStateList.valueOf(bgColor));
    }

    private void setupActionButtons(OrderItemBinding binding, OrderEntity item) {
        String status = item.getStatus();
        String normalizedStatus = status != null ? status.trim().toLowerCase() : "";

        if (normalizedStatus.contains("chờ xử lý") || normalizedStatus.contains("chờ xác nhận")) {
            binding.layoutOrderActions.setVisibility(View.VISIBLE);
            binding.btnCancelOrder.setVisibility(View.VISIBLE);
            binding.btnCancelOrder.setText("Hủy đơn");
            binding.btnApproveOrder.setVisibility(View.VISIBLE);
            binding.btnApproveOrder.setText("Xác nhận");
        } else if (normalizedStatus.contains("đang xử lý") || normalizedStatus.contains("đang chuẩn bị")) {
            binding.layoutOrderActions.setVisibility(View.VISIBLE);
            binding.btnCancelOrder.setVisibility(View.VISIBLE);
            binding.btnCancelOrder.setText("Hủy đơn");
            binding.btnApproveOrder.setVisibility(View.VISIBLE);
            binding.btnApproveOrder.setText("Giao hàng");
        } else if (normalizedStatus.contains("đang giao")) {
            binding.layoutOrderActions.setVisibility(View.VISIBLE);
            binding.btnCancelOrder.setVisibility(View.VISIBLE);
            binding.btnCancelOrder.setText("Thất bại");
            binding.btnApproveOrder.setVisibility(View.VISIBLE);
            binding.btnApproveOrder.setText("Hoàn tất");
        } else {
            // Completed / Cancelled -> hide action buttons
            binding.layoutOrderActions.setVisibility(View.GONE);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final OrderItemBinding binding;

        public ViewHolder(OrderItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
