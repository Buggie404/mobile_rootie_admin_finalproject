package com.veganbeauty.admin.features.product.list;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.admin.R;

import java.util.HashMap;
import java.util.Map;

public class ProductSortBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_CURRENT_SORT = "arg_current_sort";

    public interface OnSortSelectedListener {
        void onSortSelected(String sortType);
    }

    private String currentSort;
    private OnSortSelectedListener onSortSelectedListener;
    private final Map<String, ImageView> radios = new HashMap<>();

    public static ProductSortBottomSheet newInstance(String currentSort, OnSortSelectedListener listener) {
        ProductSortBottomSheet fragment = new ProductSortBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_CURRENT_SORT, currentSort);
        fragment.setArguments(args);
        fragment.onSortSelectedListener = listener;
        return fragment;
    }

    public ProductSortBottomSheet() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentSort = getArguments().getString(ARG_CURRENT_SORT);
        }
        if (currentSort == null) {
            currentSort = "DEFAULT";
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.product_bottom_sheet_sort, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.ivClose).setOnClickListener(v -> dismiss());

        ImageView ivRadioDefault = view.findViewById(R.id.ivRadioDefault);
        ImageView ivRadioPriceAsc = view.findViewById(R.id.ivRadioPriceAsc);
        ImageView ivRadioPriceDesc = view.findViewById(R.id.ivRadioPriceDesc);
        ImageView ivRadioNameAsc = view.findViewById(R.id.ivRadioNameAsc);
        ImageView ivRadioNameDesc = view.findViewById(R.id.ivRadioNameDesc);

        radios.put("DEFAULT", ivRadioDefault);
        radios.put("PRICE_ASC", ivRadioPriceAsc);
        radios.put("PRICE_DESC", ivRadioPriceDesc);
        radios.put("NAME_ASC", ivRadioNameAsc);
        radios.put("NAME_DESC", ivRadioNameDesc);

        updateUI(currentSort);

        Map<Integer, String> layouts = new HashMap<>();
        layouts.put(R.id.layoutSortDefault, "DEFAULT");
        layouts.put(R.id.layoutSortPriceAsc, "PRICE_ASC");
        layouts.put(R.id.layoutSortPriceDesc, "PRICE_DESC");
        layouts.put(R.id.layoutSortNameAsc, "NAME_ASC");
        layouts.put(R.id.layoutSortNameDesc, "NAME_DESC");

        for (Map.Entry<Integer, String> entry : layouts.entrySet()) {
            LinearLayout layout = view.findViewById(entry.getKey());
            if (layout != null) {
                layout.setOnClickListener(v -> {
                    String sortType = entry.getValue();
                    updateUI(sortType);
                    if (onSortSelectedListener != null) {
                        onSortSelectedListener.onSortSelected(sortType);
                    }
                    dismiss();
                });
            }
        }
    }

    private void updateUI(String selected) {
        for (Map.Entry<String, ImageView> entry : radios.entrySet()) {
            ImageView imageView = entry.getValue();
            if (imageView != null) {
                if (entry.getKey().equals(selected)) {
                    imageView.setImageResource(R.drawable.ic_radio_primary_checked);
                } else {
                    imageView.setImageResource(R.drawable.ic_radio_primary_unchecked);
                }
            }
        }
    }
}
