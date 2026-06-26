package com.veganbeauty.admin.features.product.list;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.data.local.entities.ProductEntity;
import com.veganbeauty.admin.features.product.ProductViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProductFilterBottomSheet extends BottomSheetDialogFragment {

    private ProductViewModel viewModel;

    // Temporary state to hold choices before Apply
    private String tempSelectedCategory = "";
    private final Set<String> tempSelectedSubcategories = new HashSet<>();
    private String tempStockStatus = "ALL";
    private String tempHiddenStatus = "ALL";

    // Keep track of all checkboxes to manage single category selection
    private final Map<String, CheckBox> categoryCheckboxes = new HashMap<>(); // Category -> "Tất cả" Checkbox
    private final Map<String, List<CheckBox>> subcategoryCheckboxes = new HashMap<>(); // Category -> list of Subcategory Checkboxes

    public ProductFilterBottomSheet() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);
        return inflater.inflate(R.layout.product_bottom_sheet_filter, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load current filters
        tempSelectedCategory = viewModel.getSelectedCategory().getValue();
        if (tempSelectedCategory == null) tempSelectedCategory = "";
        tempSelectedSubcategories.clear();
        if (viewModel.getSelectedSubcategories().getValue() != null) {
            tempSelectedSubcategories.addAll(viewModel.getSelectedSubcategories().getValue());
        }
        tempStockStatus = viewModel.getFilterStockStatus().getValue();
        if (tempStockStatus == null) tempStockStatus = "ALL";
        tempHiddenStatus = viewModel.getFilterHiddenStatus().getValue();
        if (tempHiddenStatus == null) tempHiddenStatus = "ALL";

        // Close button
        view.findViewById(R.id.ivClose).setOnClickListener(v -> dismiss());

        // Setup Stock Status RadioGroup
        RadioGroup rgStockStatus = view.findViewById(R.id.rgStockStatus);
        if ("IN_STOCK".equals(tempStockStatus)) {
            rgStockStatus.check(R.id.rbStockIn);
        } else if ("OUT_OF_STOCK".equals(tempStockStatus)) {
            rgStockStatus.check(R.id.rbStockOut);
        } else {
            rgStockStatus.check(R.id.rbStockAll);
        }
        rgStockStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbStockIn) {
                tempStockStatus = "IN_STOCK";
            } else if (checkedId == R.id.rbStockOut) {
                tempStockStatus = "OUT_OF_STOCK";
            } else {
                tempStockStatus = "ALL";
            }
        });

        // Setup Visibility Status RadioGroup
        RadioGroup rgVisibilityStatus = view.findViewById(R.id.rgVisibilityStatus);
        if ("HIDDEN".equals(tempHiddenStatus)) {
            rgVisibilityStatus.check(R.id.rbVisibilityHidden);
        } else if ("VISIBLE".equals(tempHiddenStatus)) {
            rgVisibilityStatus.check(R.id.rbVisibilityVisible);
        } else {
            rgVisibilityStatus.check(R.id.rbVisibilityAll);
        }
        rgVisibilityStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbVisibilityHidden) {
                tempHiddenStatus = "HIDDEN";
            } else if (checkedId == R.id.rbVisibilityVisible) {
                tempHiddenStatus = "VISIBLE";
            } else {
                tempHiddenStatus = "ALL";
            }
        });

        // Setup Categories Spinner
        setupCategoriesContainer(view);

        // Reset Button
        view.findViewById(R.id.btnReset).setOnClickListener(v -> {
            tempSelectedCategory = "";
            tempSelectedSubcategories.clear();
            tempStockStatus = "ALL";
            tempHiddenStatus = "ALL";

            rgStockStatus.check(R.id.rbStockAll);
            rgVisibilityStatus.check(R.id.rbVisibilityAll);

            // Clear checkboxes UI
            for (CheckBox cb : categoryCheckboxes.values()) {
                cb.setChecked(false);
            }
            for (List<CheckBox> cbs : subcategoryCheckboxes.values()) {
                for (CheckBox cb : cbs) {
                    cb.setChecked(false);
                }
            }
        });

        // Apply Button
        view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            viewModel.getSelectedCategory().setValue(tempSelectedCategory);
            viewModel.getSelectedSubcategories().setValue(new HashSet<>(tempSelectedSubcategories));
            viewModel.getFilterStockStatus().setValue(tempStockStatus);
            viewModel.getFilterHiddenStatus().setValue(tempHiddenStatus);
            dismiss();
        });
    }

    private void setupCategoriesContainer(View rootView) {
        LinearLayout container = rootView.findViewById(R.id.layoutCategoriesContainer);
        if (container != null) {
            container.removeAllViews();
        }

        List<ProductEntity> products = viewModel.getAllProducts().getValue();
        if (products == null) {
            products = new ArrayList<>();
        }

        Set<String> categorySet = new HashSet<>();
        for (ProductEntity product : products) {
            String cat = product.getCategory();
            if (cat != null && !cat.isEmpty()) {
                categorySet.add(cat);
            }
        }
        List<String> categories = new ArrayList<>(categorySet);

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (String category : categories) {
            View categoryView = inflater.inflate(R.layout.product_item_filter_category_header, container, false);
            TextView tvCategoryName = categoryView.findViewById(R.id.tvCategoryName);
            ImageView ivChevron = categoryView.findViewById(R.id.ivChevron);
            LinearLayout layoutCategoryHeader = categoryView.findViewById(R.id.layoutCategoryHeader);
            LinearLayout layoutSubcategoriesContainer = categoryView.findViewById(R.id.layoutSubcategoriesContainer);

            tvCategoryName.setText(category);

            // Expand/Collapse click listener
            layoutCategoryHeader.setOnClickListener(v -> {
                boolean isVisible = layoutSubcategoriesContainer.getVisibility() == View.VISIBLE;
                layoutSubcategoriesContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                ivChevron.setRotation(isVisible ? 0f : 180f);
            });

            // Get subcategories of this category
            Set<String> subcategorySet = new HashSet<>();
            for (ProductEntity p : products) {
                if (p.getCategory() != null && p.getCategory().equalsIgnoreCase(category)) {
                    String sub = p.getSubcategory();
                    if (sub != null) {
                        String[] parts = sub.split(",");
                        for (String part : parts) {
                            String clean = part.trim();
                            if (!clean.isEmpty()) {
                                subcategorySet.add(clean);
                            }
                        }
                    }
                }
            }
            List<String> subcategories = new ArrayList<>(subcategorySet);

            List<CheckBox> subCheckboxesList = new ArrayList<>();

            // 1. Add "Tất cả [Category]" checkbox
            CheckBox allCategoryCb = (CheckBox) inflater.inflate(R.layout.product_item_subcategory_checkbox, layoutSubcategoriesContainer, false);
            allCategoryCb.setText("Tất cả " + category);
            allCategoryCb.setChecked(tempSelectedCategory.equals(category) && tempSelectedSubcategories.isEmpty());
            categoryCheckboxes.put(category, allCategoryCb);

            allCategoryCb.setOnClickListener(v -> {
                boolean isChecked = allCategoryCb.isChecked();
                if (isChecked) {
                    // Select this category, clear others
                    tempSelectedCategory = category;
                    tempSelectedSubcategories.clear();
                    updateCategorySelectionUI(category, null);
                } else {
                    if (tempSelectedCategory.equals(category)) {
                        tempSelectedCategory = "";
                    }
                }
            });
            layoutSubcategoriesContainer.addView(allCategoryCb);

            // 2. Add each Subcategory checkbox
            for (String sub : subcategories) {
                CheckBox subCb = (CheckBox) inflater.inflate(R.layout.product_item_subcategory_checkbox, layoutSubcategoriesContainer, false);
                subCb.setText(sub);
                subCb.setChecked(tempSelectedSubcategories.contains(sub));
                subCheckboxesList.add(subCb);

                subCb.setOnClickListener(v -> {
                    boolean isChecked = subCb.isChecked();
                    if (isChecked) {
                        // Switch category if needed
                        if (!tempSelectedCategory.equals(category)) {
                            tempSelectedCategory = category;
                            tempSelectedSubcategories.clear();
                        }
                        tempSelectedSubcategories.add(sub);
                        updateCategorySelectionUI(category, subCb);
                    } else {
                        tempSelectedSubcategories.remove(sub);
                        if (tempSelectedSubcategories.isEmpty() && tempSelectedCategory.equals(category)) {
                            // If no subcategories checked, keep category selected
                            CheckBox cb = categoryCheckboxes.get(category);
                            if (cb != null) {
                                cb.setChecked(true);
                            }
                        }
                    }
                });
                layoutSubcategoriesContainer.addView(subCb);
            }

            subcategoryCheckboxes.put(category, subCheckboxesList);
            if (container != null) {
                container.addView(categoryView);
            }

            // If this category is currently selected, expand it by default
            if (tempSelectedCategory.equals(category)) {
                layoutSubcategoriesContainer.setVisibility(View.VISIBLE);
                ivChevron.setRotation(180f);
            }
        }
    }

    private void updateCategorySelectionUI(String selectedCategory, CheckBox activeSubCb) {
        // Uncheck all other categories and their subcategories
        for (Map.Entry<String, CheckBox> entry : categoryCheckboxes.entrySet()) {
            String cat = entry.getKey();
            CheckBox cb = entry.getValue();
            if (cb != null) {
                if (!cat.equals(selectedCategory)) {
                    cb.setChecked(false);
                } else {
                    cb.setChecked(activeSubCb == null);
                }
            }
        }

        for (Map.Entry<String, List<CheckBox>> entry : subcategoryCheckboxes.entrySet()) {
            String cat = entry.getKey();
            List<CheckBox> cbs = entry.getValue();
            if (cbs != null && !cat.equals(selectedCategory)) {
                for (CheckBox cb : cbs) {
                    cb.setChecked(false);
                }
            }
        }
    }
}
