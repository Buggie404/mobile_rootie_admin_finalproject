package com.veganbeauty.admin.features.product.list;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.base.RootieAdminFragment;
import com.veganbeauty.admin.data.local.entities.ProductEntity;
import com.veganbeauty.admin.databinding.ProductFragmentListBinding;
import com.veganbeauty.admin.features.home.BottomNavHelper;
import com.veganbeauty.admin.features.product.ProductViewModel;
import com.veganbeauty.admin.features.product.add.ProductAddFragment;

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
        android.widget.TextView tvName = dialogView.findViewById(R.id.tvProductName);
        android.widget.TextView tvPrice = dialogView.findViewById(R.id.tvProductPrice);
        android.widget.TextView tvStock = dialogView.findViewById(R.id.tvProductStock);
        android.widget.TextView tvSku = dialogView.findViewById(R.id.tvProductSku);

        if (tvName != null) {
            tvName.setText(product.getName());
        }
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
        if (tvPrice != null) {
            tvPrice.setText("Giá bán: " + formatter.format(product.getPrice()) + "đ");
        }
        if (tvStock != null) {
            tvStock.setText("Số lượng kho: " + product.getStock());
        }
        if (tvSku != null) {
            tvSku.setText("Mã sản phẩm (SKU): " + product.getSku());
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                .setView(dialogView)
                .create();

        View btnClose = dialogView.findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
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
