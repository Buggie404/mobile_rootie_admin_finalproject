package com.veganbeauty.admin.features.product.add;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.admin.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProductGenericBottomSheetFragment extends BottomSheetDialogFragment {

    public interface OnSingleItemSelectedListener {
        void onSingleItemSelected(int position);
    }

    public interface OnMultiItemsSelectedListener {
        void onMultiItemsSelected(Set<String> selectedItems);
    }

    private String title = "";
    private List<String> options = new ArrayList<>();
    private boolean isMultiSelect = false;
    private Set<String> currentSelected = new HashSet<>();

    private OnSingleItemSelectedListener onSingleItemSelectedListener;
    private OnMultiItemsSelectedListener onMultiItemsSelectedListener;

    public static ProductGenericBottomSheetFragment newSingleSelectInstance(
            String title,
            List<String> options,
            String currentSelectedVal,
            OnSingleItemSelectedListener listener) {
        ProductGenericBottomSheetFragment fragment = new ProductGenericBottomSheetFragment();
        fragment.title = title;
        fragment.options = options != null ? options : new ArrayList<>();
        fragment.isMultiSelect = false;
        fragment.currentSelected = (currentSelectedVal != null && !currentSelectedVal.isEmpty())
                ? new HashSet<>(Collections.singletonList(currentSelectedVal))
                : new HashSet<>();
        fragment.onSingleItemSelectedListener = listener;
        return fragment;
    }

    public static ProductGenericBottomSheetFragment newMultiSelectInstance(
            String title,
            List<String> options,
            Set<String> currentSelected,
            OnMultiItemsSelectedListener listener) {
        ProductGenericBottomSheetFragment fragment = new ProductGenericBottomSheetFragment();
        fragment.title = title;
        fragment.options = options != null ? options : new ArrayList<>();
        fragment.isMultiSelect = true;
        fragment.currentSelected = currentSelected != null ? new HashSet<>(currentSelected) : new HashSet<>();
        fragment.onMultiItemsSelectedListener = listener;
        return fragment;
    }

    public ProductGenericBottomSheetFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.product_bottom_sheet_generic, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup Header Title
        TextView txtTitle = view.findViewById(R.id.txt_title);
        if (txtTitle != null) {
            txtTitle.setText(title);
        }

        // Close button click listener
        ImageView btnClose = view.findViewById(R.id.btn_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        LinearLayout container = view.findViewById(R.id.layout_options_container);
        if (container != null) {
            container.removeAllViews();
        }

        Button confirmBtn = view.findViewById(R.id.btn_confirm);

        if (isMultiSelect) {
            if (confirmBtn != null) {
                confirmBtn.setVisibility(View.VISIBLE);
            }
            final Set<String> tempSelection = new HashSet<>(currentSelected);

            for (String option : options) {
                if (container != null) {
                    CheckBox checkbox = (CheckBox) getLayoutInflater().inflate(R.layout.product_item_subcategory_checkbox, container, false);
                    checkbox.setText(option);
                    checkbox.setChecked(tempSelection.contains(option));
                    checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) {
                            tempSelection.add(option);
                        } else {
                            tempSelection.remove(option);
                        }
                    });
                    container.addView(checkbox);
                }
            }

            if (confirmBtn != null) {
                confirmBtn.setOnClickListener(v -> {
                    if (onMultiItemsSelectedListener != null) {
                        onMultiItemsSelectedListener.onMultiItemsSelected(tempSelection);
                    }
                    dismiss();
                });
            }
        } else {
            if (confirmBtn != null) {
                confirmBtn.setVisibility(View.GONE);
            }

            for (int i = 0; i < options.size(); i++) {
                final int index = i;
                String option = options.get(i);
                if (container != null) {
                    View itemView = getLayoutInflater().inflate(R.layout.item_bottom_sheet_option, container, false);
                    TextView txtName = itemView.findViewById(R.id.txt_option_name);
                    ImageView ivRadio = itemView.findViewById(R.id.iv_radio);

                    txtName.setText(option);
                    boolean isSelected = currentSelected.contains(option);
                    if (currentSelected.isEmpty() && !options.isEmpty()) {
                        String firstSelected = "";
                        for (String s : currentSelected) {
                            firstSelected = s;
                            break;
                        }
                        if (option.equalsIgnoreCase(firstSelected)) {
                            isSelected = true;
                        }
                    }

                    if (isSelected) {
                        ivRadio.setImageResource(R.drawable.ic_radio_primary_checked);
                    } else {
                        ivRadio.setImageResource(R.drawable.ic_radio_primary_unchecked);
                    }

                    itemView.setOnClickListener(v -> {
                        if (onSingleItemSelectedListener != null) {
                            onSingleItemSelectedListener.onSingleItemSelected(index);
                        }
                        dismiss();
                    });
                    container.addView(itemView);
                }
            }
        }
    }
}
