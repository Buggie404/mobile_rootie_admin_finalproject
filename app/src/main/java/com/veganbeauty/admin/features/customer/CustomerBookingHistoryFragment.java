package com.veganbeauty.admin.features.customer;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.base.RootieAdminFragment;
import com.veganbeauty.admin.data.local.RootieAdminDatabase;
import com.veganbeauty.admin.data.local.SessionManager;
import com.veganbeauty.admin.data.local.entities.BookingEntity;
import com.veganbeauty.admin.data.local.entities.CustomerEntity;
import com.veganbeauty.admin.databinding.BookingDialogCancelBinding;
import com.veganbeauty.admin.databinding.CustomerBookingHistoryBinding;
import com.veganbeauty.admin.features.booking.BookingViewModel;
import com.veganbeauty.admin.features.booking.list.BookingAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerBookingHistoryFragment extends RootieAdminFragment {

    private static final String ARG_CUSTOMER_ID = "arg_customer_id";
    private static final String ARG_FROM_STAFF = "arg_from_staff";

    private CustomerBookingHistoryBinding binding;
    private BookingViewModel bookingViewModel;
    private SessionManager sessionManager;
    private String customerId;
    private boolean fromStaff = false;

    private final List<BookingEntity> allUserBookings = new ArrayList<>();
    private final List<BookingEntity> filteredUserBookings = new ArrayList<>();
    private BookingAdapter adapter;

    private String currentFilter = "all"; // all, pending, upcoming, completed, cancelled
    private int showLimit = 5;
    private CustomerEntity currentCustomer;

    public static CustomerBookingHistoryFragment newInstance(String customerId, boolean fromStaff) {
        CustomerBookingHistoryFragment fragment = new CustomerBookingHistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CUSTOMER_ID, customerId);
        args.putBoolean(ARG_FROM_STAFF, fromStaff);
        fragment.setArguments(args);
        return fragment;
    }

    public static CustomerBookingHistoryFragment newInstance(String customerId) {
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
        binding = CustomerBookingHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(View view) {
        bookingViewModel = new ViewModelProvider(this).get(BookingViewModel.class);
        sessionManager = new SessionManager(requireContext());

        setupRecyclerView();
        setupListeners();
        loadCustomerData();

        // Sync initially
        bookingViewModel.syncFromFirebase();
    }

    private void setupRecyclerView() {
        String role = sessionManager.getRole();
        if (role == null) role = "admin";
        boolean isAdmin = role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("business");

        adapter = new BookingAdapter(
            isAdmin,
            (booking, nextStatus) -> {
                bookingViewModel.updateBookingStatus(booking.getId(), nextStatus);
                Toast.makeText(requireContext(), "Đang cập nhật lịch hẹn...", Toast.LENGTH_SHORT).show();
            },
            booking -> showCancelDialog(booking.getId())
        );
        binding.rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBookings.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> navigateBack());

        binding.tabAll.setOnClickListener(v -> selectTab("all"));
        binding.tabPending.setOnClickListener(v -> selectTab("pending"));
        binding.tabUpcoming.setOnClickListener(v -> selectTab("upcoming"));
        binding.tabCompleted.setOnClickListener(v -> selectTab("completed"));
        binding.tabCancelled.setOnClickListener(v -> selectTab("cancelled"));

        binding.btnLoadMore.setOnClickListener(v -> {
            showLimit = filteredUserBookings.size();
            updateListUI();
        });
    }

    private void selectTab(String filter) {
        currentFilter = filter;
        showLimit = 5;

        Map<String, TextView> tabs = new HashMap<>();
        tabs.put("all", binding.tabAll);
        tabs.put("pending", binding.tabPending);
        tabs.put("upcoming", binding.tabUpcoming);
        tabs.put("completed", binding.tabCompleted);
        tabs.put("cancelled", binding.tabCancelled);

        for (Map.Entry<String, TextView> entry : tabs.entrySet()) {
            String key = entry.getKey();
            TextView tabView = entry.getValue();
            if (key.equals(filter)) {
                tabView.setBackgroundResource(R.drawable.bg_nav_pill);
                tabView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4F6544")));
                tabView.setTextColor(Color.WHITE);
                tabView.setTypeface(null, Typeface.BOLD);
            } else {
                tabView.setBackgroundResource(R.drawable.bg_nav_pill);
                tabView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F2F4EB")));
                tabView.setTextColor(Color.parseColor("#677559"));
                tabView.setTypeface(null, Typeface.NORMAL);
            }
        }

        applyFilters();
    }

    private void loadCustomerData() {
        if (customerId == null) return;
        if (getActivity() == null) return;
        RootieAdminDatabase db = RootieAdminDatabase.getDatabase(requireContext().getApplicationContext());
        new Thread(() -> {
            try {
                currentCustomer = db.customerDao().getByIdSync(customerId);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (currentCustomer != null) {
                            bindCustomerHeader(currentCustomer);
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

    private void bindCustomerHeader(CustomerEntity customer) {
        binding.txtCustomerName.setText(customer.getName());
        binding.txtCustomerId.setText("KH-" + customer.getId().toUpperCase());
    }

    @Override
    protected void observeViewModel() {
        bookingViewModel.getAllBookings().observe(getViewLifecycleOwner(), bookings -> {
            if (bookings == null) return;

            // 1. Filter bookings belonging to this customer
            List<BookingEntity> userBookings = new ArrayList<>();
            int completedCount = 0;
            for (BookingEntity be : bookings) {
                if (customerId != null && customerId.equals(be.getUserId())) {
                    userBookings.add(be);
                    String status = be.getStatus();
                    if (status != null && (status.equalsIgnoreCase("Đã hoàn thành") || status.equalsIgnoreCase("completed"))) {
                        completedCount++;
                    }
                }
            }

            allUserBookings.clear();
            allUserBookings.addAll(userBookings);

            binding.txtTotalBookings.setText(String.valueOf(allUserBookings.size()));
            binding.txtCompletedBookings.setText(String.valueOf(completedCount));

            selectTab(currentFilter);
        });
    }

    private void applyFilters() {
        List<BookingEntity> temp = new ArrayList<>();
        for (BookingEntity be : allUserBookings) {
            String status = be.getStatus() != null ? be.getStatus().toLowerCase() : "";
            switch (currentFilter) {
                case "pending":
                    if (status.equals("chờ xác nhận") || status.equals("pending")) {
                        temp.add(be);
                    }
                    break;
                case "upcoming":
                    if (status.equals("sắp diễn ra") || status.equals("confirmed") || status.equals("upcoming")) {
                        temp.add(be);
                    }
                    break;
                case "completed":
                    if (status.equals("đã hoàn thành") || status.equals("completed")) {
                        temp.add(be);
                    }
                    break;
                case "cancelled":
                    if (status.equals("đã huỷ") || status.equals("đã hủy") || status.equals("cancelled")) {
                        temp.add(be);
                    }
                    break;
                default:
                    temp.add(be);
                    break;
            }
        }

        filteredUserBookings.clear();
        filteredUserBookings.addAll(temp);
        updateListUI();
    }

    private void updateListUI() {
        int totalCount = filteredUserBookings.size();
        List<BookingEntity> itemsToShow = new ArrayList<>();
        for (int i = 0; i < Math.min(showLimit, totalCount); i++) {
            itemsToShow.add(filteredUserBookings.get(i));
        }
        adapter.submitList(itemsToShow);

        if (totalCount > showLimit) {
            int remaining = totalCount - showLimit;
            binding.btnLoadMore.setVisibility(View.VISIBLE);
            binding.txtLoadMore.setText("Xem thêm (còn " + remaining + " lịch hẹn)");
        } else {
            binding.btnLoadMore.setVisibility(View.GONE);
        }
    }

    private void showCancelDialog(String bookingId) {
        BookingDialogCancelBinding dialogBinding = BookingDialogCancelBinding.inflate(LayoutInflater.from(requireContext()));
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setView(dialogBinding.getRoot())
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogBinding.rgReasons.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == com.veganbeauty.admin.R.id.rb_other) {
                dialogBinding.edtCancelReason.setVisibility(View.VISIBLE);
            } else {
                dialogBinding.edtCancelReason.setVisibility(View.GONE);
            }
        });

        dialogBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnConfirm.setOnClickListener(v -> {
            int checkedId = dialogBinding.rgReasons.getCheckedRadioButtonId();
            String reason = "";
            if (checkedId == com.veganbeauty.admin.R.id.rb_no_show) {
                reason = "Khách hàng không đến (No-show)";
            } else if (checkedId == com.veganbeauty.admin.R.id.rb_customer_request) {
                reason = "Khách gọi điện yêu cầu hủy";
            } else if (checkedId == com.veganbeauty.admin.R.id.rb_store_issue) {
                reason = "Cửa hàng bận đột xuất / Sự cố hệ thống";
            } else if (checkedId == com.veganbeauty.admin.R.id.rb_other) {
                reason = dialogBinding.edtCancelReason.getText().toString().trim();
            } else {
                reason = "Không rõ lý do";
            }

            if (reason.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập lý do hủy chi tiết", Toast.LENGTH_SHORT).show();
                return;
            }

            bookingViewModel.updateBookingStatus(bookingId, "Đã huỷ", reason);
            dialog.dismiss();
            Toast.makeText(requireContext(), "Đã hủy lịch hẹn thành công", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void navigateBack() {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) return;
        if (customerId != null) {
            mainActivity.loadFragment(CustomerDetailFragment.newInstance(customerId, fromStaff));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
