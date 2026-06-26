package com.veganbeauty.admin.features.order;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.admin.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class OrderFilterBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PAYMENTS = "arg_payments";
    private static final String ARG_PRICES = "arg_prices";
    private static final String ARG_REGIONS = "arg_regions";

    public interface OnFiltersAppliedListener {
        void onFiltersApplied(Set<String> payments, Set<String> prices, Set<String> regions);
    }

    private Set<String> initialPayments;
    private Set<String> initialPrices;
    private Set<String> initialRegions;
    private OnFiltersAppliedListener onFiltersAppliedListener;

    public static OrderFilterBottomSheet newInstance(
            Set<String> payments,
            Set<String> prices,
            Set<String> regions,
            OnFiltersAppliedListener listener) {
        OrderFilterBottomSheet fragment = new OrderFilterBottomSheet();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_PAYMENTS, new ArrayList<>(payments));
        args.putStringArrayList(ARG_PRICES, new ArrayList<>(prices));
        args.putStringArrayList(ARG_REGIONS, new ArrayList<>(regions));
        fragment.setArguments(args);
        fragment.onFiltersAppliedListener = listener;
        return fragment;
    }

    public OrderFilterBottomSheet() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            initialPayments = new HashSet<>(getArguments().getStringArrayList(ARG_PAYMENTS));
            initialPrices = new HashSet<>(getArguments().getStringArrayList(ARG_PRICES));
            initialRegions = new HashSet<>(getArguments().getStringArrayList(ARG_REGIONS));
        } else {
            initialPayments = new HashSet<>();
            initialPrices = new HashSet<>();
            initialRegions = new HashSet<>();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.order_bottom_sheet_filter, container, false);
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

        view.findViewById(R.id.ivClose).setOnClickListener(v -> dismiss());

        LinearLayout layoutPaymentHeader = view.findViewById(R.id.layoutPaymentHeader);
        LinearLayout layoutPaymentContent = view.findViewById(R.id.layoutPaymentContent);
        ImageView ivPaymentChevron = view.findViewById(R.id.ivPaymentChevron);

        LinearLayout layoutPriceHeader = view.findViewById(R.id.layoutPriceHeader);
        LinearLayout layoutPriceContent = view.findViewById(R.id.layoutPriceContent);
        ImageView ivPriceChevron = view.findViewById(R.id.ivPriceChevron);

        LinearLayout layoutRegionHeader = view.findViewById(R.id.layoutRegionHeader);
        LinearLayout layoutRegionContent = view.findViewById(R.id.layoutRegionContent);
        ImageView ivRegionChevron = view.findViewById(R.id.ivRegionChevron);

        CheckBox cbPaymentCOD = view.findViewById(R.id.cbPaymentCOD);
        CheckBox cbPaymentATM = view.findViewById(R.id.cbPaymentATM);
        CheckBox cbPaymentMoMo = view.findViewById(R.id.cbPaymentMoMo);
        CheckBox cbPaymentVNPay = view.findViewById(R.id.cbPaymentVNPay);

        CheckBox cbPriceUnder500k = view.findViewById(R.id.cbPriceUnder500k);
        CheckBox cbPrice500kTo1500k = view.findViewById(R.id.cbPrice500kTo1500k);
        CheckBox cbPriceOver1500k = view.findViewById(R.id.cbPriceOver1500k);

        CheckBox cbRegionHCMC = view.findViewById(R.id.cbRegionHCMC);
        CheckBox cbRegionHN = view.findViewById(R.id.cbRegionHN);
        CheckBox cbRegionOthers = view.findViewById(R.id.cbRegionOthers);

        // Expand/Collapse click listeners
        layoutPaymentHeader.setOnClickListener(v -> {
            boolean isVisible = layoutPaymentContent.getVisibility() == View.VISIBLE;
            layoutPaymentContent.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            ivPaymentChevron.setRotation(isVisible ? 0f : 180f);
        });

        layoutPriceHeader.setOnClickListener(v -> {
            boolean isVisible = layoutPriceContent.getVisibility() == View.VISIBLE;
            layoutPriceContent.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            ivPriceChevron.setRotation(isVisible ? 0f : 180f);
        });

        layoutRegionHeader.setOnClickListener(v -> {
            boolean isVisible = layoutRegionContent.getVisibility() == View.VISIBLE;
            layoutRegionContent.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            ivRegionChevron.setRotation(isVisible ? 0f : 180f);
        });

        // Set initial state
        cbPaymentCOD.setChecked(initialPayments.contains("COD"));
        cbPaymentATM.setChecked(initialPayments.contains("ATM"));
        cbPaymentMoMo.setChecked(initialPayments.contains("MOMO"));
        cbPaymentVNPay.setChecked(initialPayments.contains("VNPAY"));

        cbPriceUnder500k.setChecked(initialPrices.contains("UNDER_500K"));
        cbPrice500kTo1500k.setChecked(initialPrices.contains("500K_TO_1500K"));
        cbPriceOver1500k.setChecked(initialPrices.contains("OVER_1500K"));

        cbRegionHCMC.setChecked(initialRegions.contains("HCMC"));
        cbRegionHN.setChecked(initialRegions.contains("HN"));
        cbRegionOthers.setChecked(initialRegions.contains("OTHERS"));

        // Payment Method is expanded by default
        layoutPaymentContent.setVisibility(View.VISIBLE);
        ivPaymentChevron.setRotation(180f);

        // Auto-expand other sections that have active filters
        if (!initialPrices.isEmpty()) {
            layoutPriceContent.setVisibility(View.VISIBLE);
            ivPriceChevron.setRotation(180f);
        }
        if (!initialRegions.isEmpty()) {
            layoutRegionContent.setVisibility(View.VISIBLE);
            ivRegionChevron.setRotation(180f);
        }

        // Reset click listener
        view.findViewById(R.id.btnReset).setOnClickListener(v -> {
            cbPaymentCOD.setChecked(false);
            cbPaymentATM.setChecked(false);
            cbPaymentMoMo.setChecked(false);
            cbPaymentVNPay.setChecked(false);

            cbPriceUnder500k.setChecked(false);
            cbPrice500kTo1500k.setChecked(false);
            cbPriceOver1500k.setChecked(false);

            cbRegionHCMC.setChecked(false);
            cbRegionHN.setChecked(false);
            cbRegionOthers.setChecked(false);
        });

        // Confirm click listener
        view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            Set<String> selectedPayments = new HashSet<>();
            if (cbPaymentCOD.isChecked()) selectedPayments.add("COD");
            if (cbPaymentATM.isChecked()) selectedPayments.add("ATM");
            if (cbPaymentMoMo.isChecked()) selectedPayments.add("MOMO");
            if (cbPaymentVNPay.isChecked()) selectedPayments.add("VNPAY");

            Set<String> selectedPrices = new HashSet<>();
            if (cbPriceUnder500k.isChecked()) selectedPrices.add("UNDER_500K");
            if (cbPrice500kTo1500k.isChecked()) selectedPrices.add("500K_TO_1500K");
            if (cbPriceOver1500k.isChecked()) selectedPrices.add("OVER_1500K");

            Set<String> selectedRegions = new HashSet<>();
            if (cbRegionHCMC.isChecked()) selectedRegions.add("HCMC");
            if (cbRegionHN.isChecked()) selectedRegions.add("HN");
            if (cbRegionOthers.isChecked()) selectedRegions.add("OTHERS");

            if (onFiltersAppliedListener != null) {
                onFiltersAppliedListener.onFiltersApplied(selectedPayments, selectedPrices, selectedRegions);
            }
            dismiss();
        });
    }
}
