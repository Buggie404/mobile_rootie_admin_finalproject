package com.veganbeauty.admin.features.product.list;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.utils.ImageUtils;
import com.veganbeauty.admin.data.local.entities.ProductEntity;
import com.veganbeauty.admin.databinding.ProductItemBinding;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends ListAdapter<ProductEntity, ProductAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ProductEntity product);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(ProductEntity product);
    }

    public interface OnEditClickListener {
        void onEditClick(ProductEntity product);
    }

    public interface OnViewClickListener {
        void onViewClick(ProductEntity product);
    }

    private final OnItemClickListener onItemClickListener;
    private final OnDeleteClickListener onDeleteClickListener;
    private final OnEditClickListener onEditClickListener;
    private final OnViewClickListener onViewClickListener;

    public ProductAdapter(
            OnItemClickListener onItemClickListener,
            OnDeleteClickListener onDeleteClickListener,
            OnEditClickListener onEditClickListener,
            OnViewClickListener onViewClickListener) {
        super(new ProductDiffCallback());
        this.onItemClickListener = onItemClickListener;
        this.onDeleteClickListener = onDeleteClickListener;
        this.onEditClickListener = onEditClickListener;
        this.onViewClickListener = onViewClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ProductItemBinding binding = ProductItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductEntity product = getItem(position);
        android.content.Context context = holder.binding.getRoot().getContext();
        DecimalFormat formatter = new DecimalFormat("#,###");

        ProductItemBinding binding = holder.binding;

        binding.txtProductName.setText(product.getName());
        binding.txtPrice.setText(formatter.format(product.getPrice()) + "đ");

        // Strike-through original price for visuals
        Long origPrice = product.getOriginalPrice();
        long originalPrice = (origPrice != null && origPrice > 0) ? origPrice : (long) (product.getPrice() * 1.35);
        binding.txtOriginalPrice.setText(formatter.format(originalPrice) + "đ");
        binding.txtOriginalPrice.setPaintFlags(binding.txtOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        // Set up tags dynamically
        binding.layoutTags.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(context);

        String category = product.getCategory();
        if (category != null && !category.isEmpty()) {
            TextView tagTextView = (TextView) inflater.inflate(R.layout.item_tag_pill, binding.layoutTags, false);
            tagTextView.setText(category);
            binding.layoutTags.addView(tagTextView);
        }

        String subcategory = product.getSubcategory();
        if (subcategory != null && !subcategory.isEmpty()) {
            String[] parts = subcategory.split(",");
            List<String> subcategories = new ArrayList<>();
            for (String p : parts) {
                String clean = p.trim();
                if (!clean.isEmpty()) {
                    subcategories.add(clean);
                }
            }

            for (String sub : subcategories) {
                TextView tagTextView = (TextView) inflater.inflate(R.layout.item_tag_pill, binding.layoutTags, false);
                tagTextView.setText(sub);

                if (binding.layoutTags.getChildCount() > 0) {
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tagTextView.getLayoutParams();
                    lp.setMarginStart((int) (4 * context.getResources().getDisplayMetrics().density));
                    tagTextView.setLayoutParams(lp);
                }
                binding.layoutTags.addView(tagTextView);
            }
        } else {
            TextView tagTextView = (TextView) inflater.inflate(R.layout.item_tag_pill, binding.layoutTags, false);
            tagTextView.setText("Thuần chay");
            if (binding.layoutTags.getChildCount() > 0) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tagTextView.getLayoutParams();
                lp.setMarginStart((int) (4 * context.getResources().getDisplayMetrics().density));
                tagTextView.setLayoutParams(lp);
            }
            binding.layoutTags.addView(tagTextView);
        }

        // Stock badge logic
        if (product.getStock() <= 5) {
            binding.badgeStock.setBackgroundResource(R.drawable.bg_stock_low);
            binding.imvStockIcon.setImageResource(R.drawable.ic_warning_triangle);
            binding.txtStockQuantity.setText("Kho: " + product.getStock());
        } else {
            binding.badgeStock.setBackgroundResource(R.drawable.bg_stock_normal);
            binding.imvStockIcon.setImageResource(R.drawable.ic_warehouse);
            binding.txtStockQuantity.setText("Kho: " + product.getStock());
        }

        // Load Image safely
        String mainImage = product.getMainImage();
        if (mainImage != null && !mainImage.isEmpty()) {
            int resourceId = context.getResources().getIdentifier(
                    mainImage,
                    "drawable",
                    context.getPackageName()
            );
            if (resourceId != 0) {
                ImageUtils.loadImage(context, binding.imvProduct, resourceId, R.color.gray_light);
            } else {
                ImageUtils.loadImage(context, binding.imvProduct, mainImage, R.color.gray_light);
            }
        } else {
            binding.imvProduct.setImageResource(R.color.gray_light);
        }

        // Eye icon hidden/visible state
        if (product.isHidden()) {
            binding.btnView.setImageResource(R.drawable.ic_eye_off);
            binding.btnView.setAlpha(0.35f);
            binding.btnView.setColorFilter(android.graphics.Color.parseColor("#7E8A83"));
        } else {
            binding.btnView.setImageResource(R.drawable.ic_eye);
            binding.btnView.setAlpha(1.0f);
            binding.btnView.setColorFilter(android.graphics.Color.parseColor("#677559"));
        }

        // Click listeners
        binding.btnDelete.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(product);
            }
        });
        binding.btnEdit.setOnClickListener(v -> {
            if (onEditClickListener != null) {
                onEditClickListener.onEditClick(product);
            }
        });
        binding.btnView.setOnClickListener(v -> {
            if (onViewClickListener != null) {
                onViewClickListener.onViewClick(product);
            }
        });
        binding.getRoot().setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(product);
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ProductItemBinding binding;

        public ViewHolder(ProductItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static class ProductDiffCallback extends DiffUtil.ItemCallback<ProductEntity> {
        @Override
        public boolean areItemsTheSame(@NonNull ProductEntity oldItem, @NonNull ProductEntity newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ProductEntity oldItem, @NonNull ProductEntity newItem) {
            return oldItem.getId().equals(newItem.getId()) &&
                    oldItem.getName().equals(newItem.getName()) &&
                    oldItem.getPrice() == newItem.getPrice() &&
                    oldItem.getStock() == newItem.getStock() &&
                    oldItem.isHidden() == newItem.isHidden() &&
                    (oldItem.getMainImage() != null && oldItem.getMainImage().equals(newItem.getMainImage()));
        }
    }
}
