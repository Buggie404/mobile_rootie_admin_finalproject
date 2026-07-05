package com.veganbeauty.admin.features.product.list;

import android.app.AlertDialog;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.base.RootieAdminFragment;
import com.veganbeauty.admin.core.utils.ImageUtils;
import com.veganbeauty.admin.data.local.entities.ProductEntity;
import com.veganbeauty.admin.databinding.ProductFragmentListBinding;
import com.veganbeauty.admin.features.home.BottomNavHelper;
import com.veganbeauty.admin.features.product.ProductViewModel;
import com.veganbeauty.admin.features.product.add.ProductAddFragment;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductListFragment extends RootieAdminFragment {

    private ProductFragmentListBinding binding;
    private ProductViewModel viewModel;
    private ProductAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ProductFragmentListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(View view) {
        viewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        // Setup RecyclerView with Grid Layout
        adapter = new ProductAdapter(
                this::showProductDetails,
                this::confirmDelete,
                product -> {
                    MainActivity mainAct = (MainActivity) getActivity();
                    if (mainAct != null) {
                        View bottomNav = mainAct.findViewById(R.id.bottom_nav);
                        if (bottomNav != null) {
                            bottomNav.setVisibility(View.GONE);
                        }
                        ProductAddFragment editFragment = ProductAddFragment.newInstance(product.getId());
                        mainAct.getSupportFragmentManager().beginTransaction()
                                .replace(R.id.main_container, editFragment)
                                .addToBackStack(null)
                                .commit();
                    }
                },
                product -> {
                    viewModel.toggleProductVisibility(product);
                    String statusMessage = product.isHidden() ? "Đã ẩn sản phẩm" : "Đã hiện sản phẩm";
                    Toast.makeText(getContext(), statusMessage, Toast.LENGTH_SHORT).show();
                }
        );

        binding.rvProducts.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvProducts.setAdapter(adapter);

        // Search text listener
        binding.edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.getSearchQuery().setValue(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Sort button click listener
        binding.btnSort.setOnClickListener(v -> {
            String sortOrder = viewModel.getSortOrder().getValue();
            ProductSortBottomSheet sortSheet = ProductSortBottomSheet.newInstance(
                    sortOrder != null ? sortOrder : "DEFAULT",
                    selectedSort -> viewModel.getSortOrder().setValue(selectedSort)
            );
            sortSheet.show(getChildFragmentManager(), "SortBottomSheet");
        });

        // Filter button click listener
        binding.btnFilter.setOnClickListener(v -> {
            ProductFilterBottomSheet filterSheet = new ProductFilterBottomSheet();
            filterSheet.show(getChildFragmentManager(), "FilterBottomSheet");
        });

        // Add Product button click listener
        binding.btnAddProduct.setOnClickListener(v -> {
            MainActivity mainAct = (MainActivity) getActivity();
            if (mainAct != null) {
                View bottomNav = mainAct.findViewById(R.id.bottom_nav);
                if (bottomNav != null) {
                    bottomNav.setVisibility(View.GONE);
                }
                mainAct.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new ProductAddFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        // Sync and observe data
        viewModel.syncFromFirebase();

        // Bind message button in header
        setupHeaderMessageButton(binding.btnMessage);
    }

    @Override
    protected void observeViewModel() {
        viewModel.getFilteredProducts().observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                adapter.submitList(products);
                binding.txtTotalProducts.setText("Tổng cộng " + products.size() + " sản phẩm");
            }
        });
    }

    private void confirmDelete(ProductEntity product) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_confirm_delete, null);
        android.widget.TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        if (tvMessage != null) {
            tvMessage.setText("Bạn chắc chắn muốn xoá sản phẩm " + product.getName() + "?");
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                .setView(dialogView)
                .create();

        View btnCancel = dialogView.findViewById(R.id.btnCancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        View btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                viewModel.deleteProduct(product);
                Toast.makeText(getContext(), "Đã xóa " + product.getName(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void showProductDetails(ProductEntity product) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_product_detail, null);

        TextView tvName = dialogView.findViewById(R.id.tvProductName);
        TextView tvPrice = dialogView.findViewById(R.id.tvProductPrice);
        TextView tvOriginalPrice = dialogView.findViewById(R.id.tvProductOriginalPrice);
        TextView tvStock = dialogView.findViewById(R.id.tvProductStock);
        TextView tvSku = dialogView.findViewById(R.id.tvProductSku);
        TextView tvBrand = dialogView.findViewById(R.id.tvProductBrand);
        TextView tvSuitableFor = dialogView.findViewById(R.id.tvProductSuitableFor);
        TextView tvOrigin = dialogView.findViewById(R.id.tvProductOrigin);
        TextView tvDescription = dialogView.findViewById(R.id.tvProductDescription);
        TextView tvRating = dialogView.findViewById(R.id.tvProductRating);
        TextView tvSold = dialogView.findViewById(R.id.tvProductSold);
        TextView tvHiddenBadge = dialogView.findViewById(R.id.tvHiddenBadge);
        LinearLayout layoutTags = dialogView.findViewById(R.id.layoutTags);
        LinearLayout badgeStock = dialogView.findViewById(R.id.badgeStock);
        ImageView imvStockIcon = dialogView.findViewById(R.id.imvStockIcon);
        ImageView imvProduct = dialogView.findViewById(R.id.imvProduct);

        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

        if (tvName != null) {
            tvName.setText(product.getName());
        }
        if (tvPrice != null) {
            tvPrice.setText(formatter.format(product.getPrice()) + "đ");
        }
        if (tvOriginalPrice != null) {
            Long origPrice = product.getOriginalPrice();
            long originalPrice = (origPrice != null && origPrice > 0)
                    ? origPrice
                    : (long) (product.getPrice() * 1.35);
            if (originalPrice > product.getPrice()) {
                tvOriginalPrice.setVisibility(View.VISIBLE);
                tvOriginalPrice.setText(formatter.format(originalPrice) + "đ");
                tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                tvOriginalPrice.setVisibility(View.GONE);
            }
        }
        if (tvRating != null) {
            tvRating.setText(String.format(Locale.getDefault(), "⭐ %.1f", product.getRating()));
        }
        if (tvSold != null) {
            tvSold.setText(formatter.format(product.getSold()) + " đã bán");
        }
        if (tvStock != null && badgeStock != null && imvStockIcon != null) {
            tvStock.setText("Kho: " + product.getStock());
            if (product.getStock() <= 5) {
                badgeStock.setBackgroundResource(R.drawable.bg_stock_low);
                imvStockIcon.setImageResource(R.drawable.ic_warning_triangle);
            } else {
                badgeStock.setBackgroundResource(R.drawable.bg_stock_normal);
                imvStockIcon.setImageResource(R.drawable.ic_warehouse);
            }
        }
        if (tvSku != null) {
            tvSku.setText("SKU: " + safeText(product.getSku(), "—"));
        }
        if (tvBrand != null) {
            tvBrand.setText("Thương hiệu: " + safeText(product.getBrand(), "Rootie"));
        }
        if (tvSuitableFor != null) {
            tvSuitableFor.setText("Phù hợp: " + safeText(product.getSuitableFor(), "—"));
        }
        if (tvOrigin != null) {
            tvOrigin.setText("Xuất xứ: " + safeText(product.getOrigin(), "—"));
        }
        if (tvDescription != null) {
            String description = product.getDescription();
            if (TextUtils.isEmpty(description)) {
                description = product.getMainIngredientsSummary();
            }
            tvDescription.setText(TextUtils.isEmpty(description) ? "Chưa có mô tả" : description);
        }
        if (tvHiddenBadge != null) {
            tvHiddenBadge.setVisibility(product.isHidden() ? View.VISIBLE : View.GONE);
        }
        if (layoutTags != null) {
            bindProductTags(layoutTags, product);
        }
        if (imvProduct != null) {
            loadProductImage(imvProduct, product);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                .setView(dialogView)
                .create();

        View btnClose = dialogView.findViewById(R.id.btnClose);
        View btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);
        View btnEdit = dialogView.findViewById(R.id.btnEdit);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnCloseDialog != null) {
            btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                dialog.dismiss();
                MainActivity mainAct = (MainActivity) getActivity();
                if (mainAct != null) {
                    View bottomNav = mainAct.findViewById(R.id.bottom_nav);
                    if (bottomNav != null) {
                        bottomNav.setVisibility(View.GONE);
                    }
                    mainAct.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_container, ProductAddFragment.newInstance(product.getId()))
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            float density = getResources().getDisplayMetrics().density;
            int maxWidth = (int) (320 * density);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int width = Math.min(maxWidth, (int) (screenWidth * 0.82f));
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void bindProductTags(LinearLayout layoutTags, ProductEntity product) {
        layoutTags.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        List<String> tags = new ArrayList<>();

        if (!TextUtils.isEmpty(product.getCategory())) {
            tags.add(product.getCategory());
        }
        if (!TextUtils.isEmpty(product.getSubcategory())) {
            for (String part : product.getSubcategory().split(",")) {
                String clean = part.trim();
                if (!clean.isEmpty() && !tags.contains(clean)) {
                    tags.add(clean);
                }
            }
        }
        if (tags.isEmpty()) {
            tags.add("Thuần chay");
        }

        for (int i = 0; i < tags.size(); i++) {
            TextView tagView = (TextView) inflater.inflate(R.layout.item_tag_pill, layoutTags, false);
            tagView.setText(tags.get(i));
            if (i > 0) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tagView.getLayoutParams();
                params.setMarginStart((int) (6 * getResources().getDisplayMetrics().density));
                tagView.setLayoutParams(params);
            }
            layoutTags.addView(tagView);
        }
    }

    private void loadProductImage(ImageView imageView, ProductEntity product) {
        String mainImage = product.getMainImage();
        if (mainImage != null && !mainImage.isEmpty()) {
            int resourceId = requireContext().getResources().getIdentifier(
                    mainImage,
                    "drawable",
                    requireContext().getPackageName()
            );
            if (resourceId != 0) {
                ImageUtils.loadImage(requireContext(), imageView, resourceId, R.color.gray_light);
            } else {
                ImageUtils.loadImage(requireContext(), imageView, mainImage, R.color.gray_light);
            }
        } else {
            imageView.setImageResource(R.color.gray_light);
        }
    }

    private String safeText(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value.trim();
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity mainAct = (MainActivity) getActivity();
        if (mainAct != null) {
            View bottomNav = mainAct.findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.VISIBLE);
                BottomNavHelper.highlightTab(bottomNav, R.id.nav_product);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
