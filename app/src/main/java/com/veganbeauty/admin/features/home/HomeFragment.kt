package com.veganbeauty.admin.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.veganbeauty.admin.MainActivity
import com.veganbeauty.admin.R
import com.veganbeauty.admin.core.base.RootieAdminFragment
import com.veganbeauty.admin.databinding.HomeFragmentBinding

class HomeFragment : RootieAdminFragment() {

    private var _binding: HomeFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = HomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {

        // Bind sparkline data points
        // Custom points to reflect the beautiful trend graphs in the screenshot
        binding.sparklineRevenue.setData(listOf(35f, 25f, 28f, 32f, 28f, 30f, 40f, 36f, 45f))
        binding.sparklineRevenue.setLineColor(0xFF677559.toInt())

        binding.sparklineOrders.setData(listOf(10f, 11f, 13f, 15f, 16f, 20f, 25f))
        binding.sparklineOrders.setLineColor(0xFF677559.toInt())

        // Build recent activities list matching the user's screenshot
        val activities = listOf(
            RecentActivity(
                title = "Nước sen Hậu Giang 140ml",
                orderId = "9283",
                timeAgo = "2 phút trước",
                price = "$45.00",
                status = "HOÀN THÀNH",
                imageRes = R.drawable.nuoc_sen_hau_giang
            ),
            RecentActivity(
                title = "Sáp dưỡng ẩm đa năng sen Hậu Giang 30ml",
                orderId = "9282",
                timeAgo = "15 phút trước",
                price = "$82.50",
                status = "HOÀN THÀNH",
                imageRes = R.drawable.sap_duong_am_sen_hau_giang
            ),
            RecentActivity(
                title = "Serum nghệ Hưng Yên",
                orderId = "9281",
                timeAgo = "1 giờ trước",
                price = "$120.00",
                status = "HOÀN THÀNH"
            ),
            RecentActivity(
                title = "Tư vấn da",
                orderId = "9280",
                timeAgo = "2 giờ trước",
                price = "$15.00",
                status = "HOÀN THÀNH"
            )
        )

        val adapter = RecentActivityAdapter(activities) { activityItem ->
            Toast.makeText(
                requireContext(),
                "Chi tiết đơn hàng #${activityItem.orderId}",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.rvRecentActivities.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentActivities.adapter = adapter

        // Setup actions
        binding.btnSeeAll.setOnClickListener {
            Toast.makeText(requireContext(), "Xem tất cả hoạt động", Toast.LENGTH_SHORT).show()
        }

        binding.fabAdd.setOnClickListener {
            Toast.makeText(requireContext(), "Thêm mới", Toast.LENGTH_SHORT).show()
        }

        // Bind header message icon
        setupHeaderMessageButton(binding.header.homeHeaderMessageBtn)

        // Set role switcher on avatar click
        binding.header.homeHeaderAvatarContainer.setOnClickListener {
            val context = requireContext()
            val currentRole = com.veganbeauty.admin.core.base.UserSession.getRole(context)
            val roles = arrayOf("admin", "staff")
            val checkedItem = if (currentRole == "admin") 0 else 1

            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Chọn Role để Test")
                .setSingleChoiceItems(roles, checkedItem) { dialog, which ->
                    val selectedRole = roles[which]
                    com.veganbeauty.admin.core.base.UserSession.setRole(context, selectedRole)
                    Toast.makeText(context, "Đã chuyển sang role: $selectedRole", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    activity?.recreate()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

