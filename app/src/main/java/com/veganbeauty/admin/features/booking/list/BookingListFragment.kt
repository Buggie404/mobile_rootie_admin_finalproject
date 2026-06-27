package com.veganbeauty.admin.features.booking.list

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.veganbeauty.admin.core.base.RootieAdminFragment
import com.veganbeauty.admin.data.local.SessionManager
import com.veganbeauty.admin.databinding.BookingDialogCancelBinding
import com.veganbeauty.admin.databinding.BookingFragmentListBinding
import com.veganbeauty.admin.features.booking.BookingViewModel
import com.veganbeauty.admin.features.booking.list.BookingAdapter

class BookingListFragment : RootieAdminFragment() {

    private var _binding: BookingFragmentListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BookingViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: BookingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BookingFragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        viewModel = ViewModelProvider(this)[BookingViewModel::class.java]
        sessionManager = SessionManager(requireContext())

        setupBranchTitle()
        setupRecyclerView()
        setupTabs()
        setupSwipeRefresh()

        // Sync initially
        viewModel.syncFromFirebase()

        setupHeaderNotification(binding.btnNotification, binding.notificationBadge)
    }

    private fun setupBranchTitle() {
        val role = sessionManager.getRole() ?: "admin"
        val store = sessionManager.getAssignedStore() ?: ""
        
        binding.tvBranchSubtitle.visibility = View.VISIBLE
        if ((role == "staff" || role == "nhân viên") && store.isNotEmpty()) {
            val shortName = if (store.contains("Cơ sở 1")) "Cơ sở 1 (Q.1)" else "Cơ sở 5 (Q. Phú Nhuận)"
            binding.tvBranchSubtitle.text = "Chi nhánh: $shortName"
        } else {
            binding.tvBranchSubtitle.text = "Chi nhánh: Tất cả (Hệ thống)"
        }
    }

    private fun setupRecyclerView() {
        val role = sessionManager.getRole() ?: "admin"
        val isAdmin = role.equals("admin", ignoreCase = true) || role.equals("business", ignoreCase = true)

        adapter = BookingAdapter(
            isAdmin = isAdmin,
            onActionClick = { booking, nextStatus ->
                viewModel.updateBookingStatus(booking.id, nextStatus)
                Toast.makeText(requireContext(), "Đang cập nhật lịch hẹn...", Toast.LENGTH_SHORT).show()
            },
            onCancelClick = { booking ->
                showCancelDialog(booking.id)
            }
        )
        binding.rvBookings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBookings.adapter = adapter
    }

    private fun setupTabs() {
        val tabs = mapOf(
            "PENDING" to binding.tabPending,
            "UPCOMING" to binding.tabUpcoming,
            "COMPLETED" to binding.tabCompleted,
            "CANCELLED" to binding.tabCancelled
        )

        tabs.forEach { (tabKey, tabView) ->
            tabView.setOnClickListener {
                viewModel.activeTabStatus.value = tabKey
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(Color.parseColor("#4F6544"))
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.syncFromFirebase()
        }
    }

    private fun showCancelDialog(bookingId: String) {
        val dialogBinding = BookingDialogCancelBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // Đặt nền trong suốt để bo góc từ bg_dialog_confirm hiển thị đúng
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Lắng nghe khi chọn "Lý do khác"
        dialogBinding.rgReasons.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == com.veganbeauty.admin.R.id.rb_other) {
                dialogBinding.edtCancelReason.visibility = View.VISIBLE
            } else {
                dialogBinding.edtCancelReason.visibility = View.GONE
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val reason = when (dialogBinding.rgReasons.checkedRadioButtonId) {
                com.veganbeauty.admin.R.id.rb_no_show -> "Khách hàng không đến (No-show)"
                com.veganbeauty.admin.R.id.rb_customer_request -> "Khách gọi điện yêu cầu hủy"
                com.veganbeauty.admin.R.id.rb_store_issue -> "Cửa hàng bận đột xuất / Sự cố hệ thống"
                com.veganbeauty.admin.R.id.rb_other -> dialogBinding.edtCancelReason.text.toString().trim()
                else -> "Không rõ lý do"
            }

            if (reason.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập lý do hủy chi tiết", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.updateBookingStatus(bookingId, "Đã huỷ", reason)
            dialog.dismiss()
            Toast.makeText(requireContext(), "Đã hủy lịch hẹn thành công", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    override fun observeViewModel() {
        viewModel.filteredBookings.observe(viewLifecycleOwner) { bookings ->
            binding.swipeRefresh.isRefreshing = false
            adapter.submitList(bookings)

            if (bookings.isEmpty()) {
                binding.llEmpty.visibility = View.VISIBLE
                binding.rvBookings.visibility = View.GONE
            } else {
                binding.llEmpty.visibility = View.GONE
                binding.rvBookings.visibility = View.VISIBLE
            }
        }

        viewModel.activeTabStatus.observe(viewLifecycleOwner) { activeKey ->
            updateTabStyles(activeKey)
        }
    }

    private fun updateTabStyles(activeKey: String) {
        val tabs = mapOf(
            "PENDING" to binding.tabPending,
            "UPCOMING" to binding.tabUpcoming,
            "COMPLETED" to binding.tabCompleted,
            "CANCELLED" to binding.tabCancelled
        )

        tabs.forEach { (key, textView) ->
            if (key == activeKey) {
                textView.setBackgroundResource(com.veganbeauty.admin.R.drawable.bg_nav_pill)
                textView.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4F6544"))
                textView.setTextColor(Color.WHITE)
                textView.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                textView.setBackgroundResource(com.veganbeauty.admin.R.drawable.bg_nav_pill)
                textView.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F2F4EB"))
                textView.setTextColor(Color.parseColor("#677559"))
                textView.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
