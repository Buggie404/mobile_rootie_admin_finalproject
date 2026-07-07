package com.veganbeauty.admin.features.product.list;

import android.app.AlertDialog;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
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

import com.google.firebase.firestore.ListenerRegistration;
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

    private static final String PREFS_FAB = "product_fab_prefs";
    private static final String KEY_FAB_X = "fab_x";
    private static final String KEY_FAB_Y = "fab_y";
    private static final String KEY_FAB_VISIBLE = "fab_visible";
    private static final String KEY_FAB_ON_RIGHT = "fab_on_right";
    private static final String KEY_FAB_TAB_Y = "fab_tab_y";

    private ProductFragmentListBinding binding;
    private ProductViewModel viewModel;
    private ProductAdapter adapter;
    private ListenerRegistration firestoreListener = null;

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
                        mainAct.loadFragmentHidingNav(ProductAddFragment.newInstance(product.getId()));
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
        binding.btnAddProduct.setOnClickListener(v -> openAddProduct());

        setupDraggableFab();

        // Sync and observe data
        viewModel.syncFromFirebase();

        // Start listening to Firestore remote changes in real-time
        firestoreListener = viewModel.startRealtimeSync();

        // Bind message button in header
        setupHeaderMessageButton(binding.btnMessage);
    }

    private void openAddProduct() {
        MainActivity mainAct = (MainActivity) getActivity();
        if (mainAct == null) return;
        mainAct.loadFragmentHidingNav(new ProductAddFragment());
    }

    private void setupDraggableFab() {
        View fabWrapper = binding.fabWrapper;
        View fab = binding.fabAddProduct;
        View edgeTab = binding.fabEdgeTab;
        View edgeBg = binding.viewEdgeTabBg;
        ImageView edgeChevron = binding.imvEdgeChevron;
        View host = binding.getRoot();
        Handler handler = new Handler(Looper.getMainLooper());
        int touchSlop = ViewConfiguration.get(requireContext()).getScaledTouchSlop();
        long longPressTimeout = ViewConfiguration.getLongPressTimeout();
        float margin = 20f * getResources().getDisplayMetrics().density;

        host.post(() -> {
            placeFabDefault(fabWrapper, host, margin);
            restoreFabState(host, fabWrapper, edgeTab, edgeBg, edgeChevron, margin);
        });

        final float[] touchOffsetX = {0f};
        final float[] touchOffsetY = {0f};
        final float[] downRawX = {0f};
        final float[] downRawY = {0f};
        final boolean[] dragged = {false};
        final boolean[] longPressHandled = {false};
        final Runnable longPressRunnable = () -> {
            if (!dragged[0]) {
                longPressHandled[0] = true;
                hideFab(fabWrapper, edgeTab, edgeBg, edgeChevron, host);
            }
        };

        fab.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    touchOffsetX[0] = fabWrapper.getX() - event.getRawX();
                    touchOffsetY[0] = fabWrapper.getY() - event.getRawY();
                    downRawX[0] = event.getRawX();
                    downRawY[0] = event.getRawY();
                    dragged[0] = false;
                    longPressHandled[0] = false;
                    handler.postDelayed(longPressRunnable, longPressTimeout);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float moveDx = event.getRawX() - downRawX[0];
                    float moveDy = event.getRawY() - downRawY[0];
                    if (!dragged[0] && (Math.abs(moveDx) > touchSlop || Math.abs(moveDy) > touchSlop)) {
                        dragged[0] = true;
                        handler.removeCallbacks(longPressRunnable);
                    }
                    if (dragged[0]) {
                        moveFabWithinHost(fabWrapper, host, event.getRawX() + touchOffsetX[0], event.getRawY() + touchOffsetY[0]);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(longPressRunnable);
                    if (longPressHandled[0]) {
                        return true;
                    }
                    if (!dragged[0]) {
                        openAddProduct();
                    } else {
                        saveFabPosition(fabWrapper, host);
                    }
                    return true;
                default:
                    return false;
            }
        });

        setupEdgeTabDrag(host, fabWrapper, edgeTab, edgeBg, edgeChevron, touchSlop, margin);
    }

    private void setupEdgeTabDrag(
            View host,
            View fabWrapper,
            View edgeTab,
            View edgeBg,
            ImageView edgeChevron,
            int touchSlop,
            float margin
    ) {
        final float[] touchOffsetY = {0f};
        final float[] downRawY = {0f};
        final boolean[] dragged = {false};

        edgeTab.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    touchOffsetY[0] = edgeTab.getY() - event.getRawY();
                    downRawY[0] = event.getRawY();
                    dragged[0] = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!dragged[0] && Math.abs(event.getRawY() - downRawY[0]) > touchSlop) {
                        dragged[0] = true;
                    }
                    if (dragged[0]) {
                        moveEdgeTabVertically(edgeTab, host, event.getRawY() + touchOffsetY[0]);
                        alignFabToEdgeTab(fabWrapper, edgeTab, host);
                        saveFabTabPosition(edgeTab, host, isFabOnRightEdge(fabWrapper, host));
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!dragged[0]) {
                        showFab(fabWrapper, edgeTab, host, margin);
                    }
                    return true;
                default:
                    return false;
            }
        });
    }

    private void placeFabDefault(View fabWrapper, View host, float margin) {
        fabWrapper.setX(Math.max(0f, host.getWidth() - fabWrapper.getWidth() - margin));
        fabWrapper.setY(Math.max(0f, host.getHeight() - fabWrapper.getHeight() - margin));
    }

    private boolean isFabOnRightEdge(View fabWrapper, View host) {
        return fabWrapper.getX() + fabWrapper.getWidth() / 2f >= host.getWidth() / 2f;
    }

    private void placeEdgeTab(
            View fabWrapper,
            View edgeTab,
            View edgeBg,
            ImageView edgeChevron,
            View host,
            boolean onRight,
            float tabY
    ) {
        tabY = clamp(tabY, 0f, Math.max(0f, host.getHeight() - edgeTab.getHeight()));
        edgeTab.setY(tabY);
        if (onRight) {
            edgeTab.setX(host.getWidth() - edgeTab.getWidth());
            edgeBg.setBackgroundResource(R.drawable.bg_fab_edge_tab_right);
            edgeChevron.setImageResource(R.drawable.ic_chevron_left);
        } else {
            edgeTab.setX(0f);
            edgeBg.setBackgroundResource(R.drawable.bg_fab_edge_tab_left);
            edgeChevron.setImageResource(R.drawable.ic_chevron_right);
        }
    }

    private void moveEdgeTabVertically(View edgeTab, View host, float y) {
        edgeTab.setY(clamp(y, 0f, Math.max(0f, host.getHeight() - edgeTab.getHeight())));
    }

    private void alignFabToEdgeTab(View fabWrapper, View edgeTab, View host) {
        float fabY = edgeTab.getY() + edgeTab.getHeight() / 2f - fabWrapper.getHeight() / 2f;
        fabWrapper.setY(clamp(fabY, 0f, Math.max(0f, host.getHeight() - fabWrapper.getHeight())));
        if (isFabOnRightEdge(fabWrapper, host)) {
            fabWrapper.setX(Math.max(0f, host.getWidth() - fabWrapper.getWidth() - 20f * getResources().getDisplayMetrics().density));
        } else {
            fabWrapper.setX(20f * getResources().getDisplayMetrics().density);
        }
    }

    private void moveFabWithinHost(View target, View host, float x, float y) {
        float maxX = Math.max(0f, host.getWidth() - target.getWidth());
        float maxY = Math.max(0f, host.getHeight() - target.getHeight());
        target.setX(clamp(x, 0f, maxX));
        target.setY(clamp(y, 0f, maxY));
    }

    private void hideFab(View fabWrapper, View edgeTab, View edgeBg, ImageView edgeChevron, View host) {
        boolean onRight = isFabOnRightEdge(fabWrapper, host);
        float tabY = fabWrapper.getY() + fabWrapper.getHeight() / 2f - edgeTab.getHeight() / 2f;
        placeEdgeTab(fabWrapper, edgeTab, edgeBg, edgeChevron, host, onRight, tabY);
        fabWrapper.setVisibility(View.GONE);
        edgeTab.setVisibility(View.VISIBLE);
        edgeTab.bringToFront();
        saveFabTabPosition(edgeTab, host, onRight);
        saveFabVisibility(false, fabWrapper.getX(), fabWrapper.getY(), onRight, edgeTab.getY());
    }

    private void showFab(View fabWrapper, View edgeTab, View host, float margin) {
        alignFabToEdgeTab(fabWrapper, edgeTab, host);
        edgeTab.setVisibility(View.GONE);
        fabWrapper.setVisibility(View.VISIBLE);
        fabWrapper.bringToFront();
        saveFabVisibility(true, fabWrapper.getX(), fabWrapper.getY(), isFabOnRightEdge(fabWrapper, host), edgeTab.getY());
    }

    private void restoreFabState(
            View host,
            View fabWrapper,
            View edgeTab,
            View edgeBg,
            ImageView edgeChevron,
            float margin
    ) {
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences(PREFS_FAB, android.content.Context.MODE_PRIVATE);
        boolean visible = prefs.getBoolean(KEY_FAB_VISIBLE, true);
        boolean onRight = prefs.getBoolean(KEY_FAB_ON_RIGHT, true);
        float savedX = prefs.getFloat(KEY_FAB_X, -1f);
        float savedY = prefs.getFloat(KEY_FAB_Y, -1f);
        float savedTabY = prefs.getFloat(KEY_FAB_TAB_Y, -1f);

        if (savedX >= 0f && savedY >= 0f) {
            moveFabWithinHost(fabWrapper, host, savedX, savedY);
        } else {
            placeFabDefault(fabWrapper, host, margin);
        }

        float tabY = savedTabY >= 0f
                ? savedTabY
                : fabWrapper.getY() + fabWrapper.getHeight() / 2f - edgeTab.getHeight() / 2f;
        placeEdgeTab(fabWrapper, edgeTab, edgeBg, edgeChevron, host, onRight, tabY);

        if (visible) {
            fabWrapper.setVisibility(View.VISIBLE);
            edgeTab.setVisibility(View.GONE);
        } else {
            fabWrapper.setVisibility(View.GONE);
            edgeTab.setVisibility(View.VISIBLE);
            edgeTab.bringToFront();
        }
    }

    private void saveFabPosition(View fabWrapper, View host) {
        saveFabVisibility(
                fabWrapper.getVisibility() == View.VISIBLE,
                fabWrapper.getX(),
                fabWrapper.getY(),
                isFabOnRightEdge(fabWrapper, host),
                fabWrapper.getY() + fabWrapper.getHeight() / 2f - binding.fabEdgeTab.getHeight() / 2f
        );
    }

    private void saveFabTabPosition(View edgeTab, View host, boolean onRight) {
        requireContext()
                .getSharedPreferences(PREFS_FAB, android.content.Context.MODE_PRIVATE)
                .edit()
                .putFloat(KEY_FAB_TAB_Y, edgeTab.getY())
                .putBoolean(KEY_FAB_ON_RIGHT, onRight)
                .apply();
    }

    private void saveFabVisibility(boolean visible, float x, float y, boolean onRight, float tabY) {
        requireContext()
                .getSharedPreferences(PREFS_FAB, android.content.Context.MODE_PRIVATE)
                .edit()
                .putFloat(KEY_FAB_X, x)
                .putFloat(KEY_FAB_Y, y)
                .putFloat(KEY_FAB_TAB_Y, tabY)
                .putBoolean(KEY_FAB_ON_RIGHT, onRight)
                .putBoolean(KEY_FAB_VISIBLE, visible)
                .apply();
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
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
                    mainAct.loadFragmentHidingNav(ProductAddFragment.newInstance(product.getId()));
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
            mainAct.ensureBottomNavVisible();
            View bottomNav = mainAct.findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                BottomNavHelper.highlightTab(bottomNav, R.id.nav_product);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
        binding = null;
    }
}
