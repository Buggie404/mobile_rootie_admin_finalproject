package com.veganbeauty.admin.features.customer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.base.RootieAdminFragment;
import com.veganbeauty.admin.core.utils.ImageUtils;
import com.veganbeauty.admin.data.local.RootieAdminDatabase;
import com.veganbeauty.admin.data.local.entities.CustomerEntity;
import com.veganbeauty.admin.databinding.CustomerDetailFragmentBinding;
import java.text.NumberFormat;
import java.util.Locale;

public class CustomerDetailFragment extends RootieAdminFragment {

    private static final String ARG_CUSTOMER_ID = "arg_customer_id";
    private static final String ARG_FROM_STAFF = "arg_from_staff";

    private CustomerDetailFragmentBinding binding;
    private String customerId;
    private boolean fromStaff = false;

    public static CustomerDetailFragment newInstance(String customerId, boolean fromStaff) {
        CustomerDetailFragment fragment = new CustomerDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CUSTOMER_ID, customerId);
        args.putBoolean(ARG_FROM_STAFF, fromStaff);
        fragment.setArguments(args);
        return fragment;
    }

    public static CustomerDetailFragment newInstance(String customerId) {
        return newInstance(customerId, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            customerId = getArguments().getString(ARG_CUSTOMER_ID);
            fromStaff = getArguments().getBoolean(ARG_FROM_STAFF, false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = CustomerDetailFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(View view) {
        setupListeners();
        loadCustomerData();
    }

    private void setupListeners() {
        // Toolbar Back navigation
        binding.btnBack.setOnClickListener(v -> navigateBack());

        // Quick Actions
        binding.btnSpendingDetail.setOnClickListener(v -> {
            if (customerId == null) return;
            CustomerSpendingChartFragment chartFragment = CustomerSpendingChartFragment.newInstance(customerId, fromStaff);
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.loadFragment(chartFragment);
            }
        });

        binding.btnPurchaseHistory.setOnClickListener(v -> {
            if (customerId == null) return;
            CustomerOrderHistoryFragment historyFragment = CustomerOrderHistoryFragment.newInstance(customerId, fromStaff);
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.loadFragment(historyFragment);
            }
        });

        binding.btnSpendingChart.setOnClickListener(v -> {
            if (customerId == null) return;
            CustomerSpendingChartFragment chartFragment = CustomerSpendingChartFragment.newInstance(customerId, fromStaff);
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.loadFragment(chartFragment);
            }
        });
    }

    private void loadCustomerData() {
        if (customerId == null) return;
        if (getActivity() == null) return;
        RootieAdminDatabase db = RootieAdminDatabase.getDatabase(requireContext().getApplicationContext());
        new Thread(() -> {
            try {
                CustomerEntity customer = db.customerDao().getByIdSync(customerId);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (customer != null) {
                            bindCustomer(customer);
                        } else {
                            Toast.makeText(requireContext(), "Không tìm thấy khách hàng!", Toast.LENGTH_SHORT).show();
                            navigateBack();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void bindCustomer(CustomerEntity customer) {
        binding.txtName.setText(customer.getName());
        binding.txtCustomerSince.setText("Khách hàng từ " + customer.getJoinYear() + " năm trước");

        // Tier Badge Text
        String tier = customer.getTier() != null ? customer.getTier() : "Thường";
        binding.txtTierBadge.setText(tier.toUpperCase(Locale.getDefault()));

        // Tier Badge styling
        switch (tier.toLowerCase()) {
            case "vip":
                binding.txtTierBadge.setTextColor(Color.parseColor("#FFFFFF"));
                binding.txtTierBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2C3B2D")));
                break;
            case "vàng":
                binding.txtTierBadge.setTextColor(Color.parseColor("#FFFFFF"));
                binding.txtTierBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C69C2C")));
                break;
            case "bạc":
                binding.txtTierBadge.setTextColor(Color.parseColor("#FFFFFF"));
                binding.txtTierBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                break;
            default: // Thường / Normal
                binding.txtTierBadge.setTextColor(Color.parseColor("#3E4D44"));
                binding.txtTierBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5E8DA")));
                break;
        }

        // Load Avatar Image
        if (customer.getAvatar() != null && !customer.getAvatar().isEmpty()) {
            ImageUtils.loadImage(requireContext(), binding.imgAvatar, customer.getAvatar(), R.drawable.imv_avatar);
        } else {
            binding.imgAvatar.setImageResource(R.drawable.imv_avatar);
        }

        // Metrics
        binding.txtMetricOrders.setText(String.valueOf(customer.getOrderCount()));
        binding.txtMetricRecent.setText(customer.getRecentPurchase());
        binding.txtMetricSpending.setText(formatSpending(customer.getSpending()));

        // Spending analysis values
        binding.txtSpendingYear.setText(formatCurrency(customer.getSpendingYear()));
        binding.txtSpendingMonth.setText(formatCurrency(customer.getSpendingMonth()));

        // Setup trend indicators
        if ("rootie_vn".equals(customer.getId())) {
            binding.txtSpendingYearTrend.setText("↓ -12%");
            binding.txtSpendingYearTrend.setTextColor(Color.parseColor("#D32F2F"));
            binding.txtSpendingMonthTrend.setText("↑ +5%");
            binding.txtSpendingMonthTrend.setTextColor(Color.parseColor("#388E3C"));
        } else {
            binding.txtSpendingYearTrend.setText("↑ +8%");
            binding.txtSpendingYearTrend.setTextColor(Color.parseColor("#388E3C"));
            binding.txtSpendingMonthTrend.setText("↑ +3%");
            binding.txtSpendingMonthTrend.setTextColor(Color.parseColor("#388E3C"));
        }

        // Membership details card
        binding.txtInfoTier.setText(customer.getTier());
        binding.txtInfoPoints.setText(formatPoints(customer.getPoints()));
        binding.txtInfoBirthday.setText(customer.getBirthday() != null && !customer.getBirthday().isEmpty() ? customer.getBirthday() : "Chưa cập nhật");
        binding.txtInfoRegion.setText(customer.getRegion() != null && !customer.getRegion().isEmpty() ? customer.getRegion() : "Chưa cập nhật");

        // Consultation notes bottom sheet
        binding.btnConsultationNotes.setOnClickListener(v -> {
            CustomerNoteBottomSheet bottomSheet = CustomerNoteBottomSheet.newInstance(customer.getId());
            bottomSheet.setOnNoteSavedListener(() -> loadCustomerData()); // Reload to reflect new notes in UI
            bottomSheet.show(getParentFragmentManager(), "CustomerNoteBottomSheet");
        });
    }

    private String formatSpending(long spending) {
        double million = spending / 1000000.0;
        String formatted = String.format(Locale.US, "%.1f", million);
        if (formatted.endsWith(".0")) {
            return formatted.substring(0, formatted.length() - 2) + "M";
        } else {
            return formatted + "M";
        }
    }

    private String formatCurrency(long amount) {
        NumberFormat formatter = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + "đ";
    }

    private String formatPoints(int points) {
        NumberFormat formatter = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));
        return formatter.format(points) + " xu";
    }

    private void navigateBack() {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) return;
        RootieAdminFragment targetFragment = fromStaff ? new CustomerStaffFragment() : new CustomerAdminFragment();
        mainActivity.loadFragment(targetFragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
