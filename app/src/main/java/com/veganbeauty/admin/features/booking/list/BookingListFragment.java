package com.veganbeauty.admin.features.booking.list;

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
import com.veganbeauty.admin.core.base.RootieAdminFragment;
import com.veganbeauty.admin.data.local.SessionManager;
import com.veganbeauty.admin.databinding.BookingDialogCancelBinding;
import com.veganbeauty.admin.databinding.BookingFragmentListBinding;
import com.veganbeauty.admin.features.booking.BookingViewModel;
import java.util.HashMap;
import java.util.Map;

public class BookingListFragment extends RootieAdminFragment {

    private BookingFragmentListBinding binding;
    private BookingViewModel viewModel;
    private SessionManager sessionManager;
    private BookingAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BookingFragmentListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(View view) {
        viewModel = new ViewModelProvider(this).get(BookingViewModel.class);
        sessionManager = new SessionManager(requireContext());

        setupBranchTitle();
        setupRecyclerView();
        setupTabs();
        setupSwipeRefresh();

        // Sync initially
        viewModel.syncFromFirebase();
    }

    private void setupBranchTitle() {
        String role = sessionManager.getRole();
        if (role == null) role = "admin";
        String store = sessionManager.getAssignedStore();
        if (store == null) store = "";

        binding.tvBranchSubtitle.setVisibility(View.VISIBLE);
        if (("staff".equals(role) || "nhân viên".equals(role)) && !store.isEmpty()) {
            String shortName = store.contains("Cơ sở 1") ? "Cơ sở 1 (Q.1)" : "Cơ sở 5 (Q. Phú Nhuận)";
            binding.tvBranchSubtitle.setText("Chi nhánh: " + shortName);
        } else {
            binding.tvBranchSubtitle.setText("Chi nhánh: Tất cả (Hệ thống)");
        }
    }

    private void setupRecyclerView() {
        String role = sessionManager.getRole();
        if (role == null) role = "admin";
        boolean isAdmin = role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("business");

        adapter = new BookingAdapter(
            isAdmin,
            (booking, nextStatus) -> {
                viewModel.updateBookingStatus(booking.getId(), nextStatus);
                Toast.makeText(requireContext(), "Đang cập nhật lịch hẹn...", Toast.LENGTH_SHORT).show();
            },
            booking -> showCancelDialog(booking.getId())
        );
        binding.rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBookings.setAdapter(adapter);
    }

    private void setupTabs() {
        Map<String, TextView> tabs = new HashMap<>();
        tabs.put("PENDING", binding.tabPending);
        tabs.put("UPCOMING", binding.tabUpcoming);
        tabs.put("COMPLETED", binding.tabCompleted);
        tabs.put("CANCELLED", binding.tabCancelled);

        for (Map.Entry<String, TextView> entry : tabs.entrySet()) {
            String tabKey = entry.getKey();
            TextView tabView = entry.getValue();
            tabView.setOnClickListener(v -> viewModel.getActiveTabStatus().setValue(tabKey));
        }
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(Color.parseColor("#4F6544"));
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.syncFromFirebase());
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

            viewModel.updateBookingStatus(bookingId, "Đã huỷ", reason);
            dialog.dismiss();
            Toast.makeText(requireContext(), "Đã hủy lịch hẹn thành công", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getFilteredBookings().observe(getViewLifecycleOwner(), bookings -> {
            binding.swipeRefresh.setRefreshing(false);
            adapter.submitList(bookings);

            if (bookings.isEmpty()) {
                binding.llEmpty.setVisibility(View.VISIBLE);
                binding.rvBookings.setVisibility(View.GONE);
            } else {
                binding.llEmpty.setVisibility(View.GONE);
                binding.rvBookings.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getActiveTabStatus().observe(getViewLifecycleOwner(), activeKey -> updateTabStyles(activeKey));
    }

    private void updateTabStyles(String activeKey) {
        Map<String, TextView> tabs = new HashMap<>();
        tabs.put("PENDING", binding.tabPending);
        tabs.put("UPCOMING", binding.tabUpcoming);
        tabs.put("COMPLETED", binding.tabCompleted);
        tabs.put("CANCELLED", binding.tabCancelled);

        for (Map.Entry<String, TextView> entry : tabs.entrySet()) {
            String key = entry.getKey();
            TextView textView = entry.getValue();
            if (key.equals(activeKey)) {
                textView.setBackgroundResource(com.veganbeauty.admin.R.drawable.bg_nav_pill);
                textView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4F6544")));
                textView.setTextColor(Color.WHITE);
                textView.setTypeface(null, Typeface.BOLD);
            } else {
                textView.setBackgroundResource(com.veganbeauty.admin.R.drawable.bg_nav_pill);
                textView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F2F4EB")));
                textView.setTextColor(Color.parseColor("#677559"));
                textView.setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
