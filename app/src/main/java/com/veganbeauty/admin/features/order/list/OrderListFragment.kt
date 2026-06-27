package com.veganbeauty.admin.features.order.list

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.veganbeauty.admin.core.base.RootieAdminFragment

import com.veganbeauty.admin.databinding.OrderFragmentListBinding
import com.veganbeauty.admin.features.order.OrderViewModel

class OrderListFragment : RootieAdminFragment() {

    private var _binding: OrderFragmentListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: OrderViewModel
    private lateinit var adapter: OrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = OrderFragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        viewModel = ViewModelProvider(this)[OrderViewModel::class.java]

        setupRecyclerView()
        setupTabs()
        // setupSwipeRefresh() // Disabled as swipeRefresh is missing in layout

        // Sync data initially
        viewModel.syncFromFirebase()

        setupHeaderNotification(binding.btnNotification, binding.notificationBadge)
    }

    private fun setupRecyclerView() {
        adapter = OrderAdapter { order, nextStatus ->
            viewModel.updateOrderStatus(order.orderId, nextStatus)
            Toast.makeText(requireContext(), "Đang cập nhật đơn hàng ${order.orderId}...", Toast.LENGTH_SHORT).show()
        }
        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = adapter
    }

    private fun setupTabs() {
        val tabs = mapOf(
            "ALL" to binding.tabAll,
            "PENDING" to binding.tabPending,
            "PROCESSING" to binding.tabPreparing,
            "SHIPPING" to binding.tabDelivering,
            "COMPLETED" to binding.tabCompleted,
            "CANCELLED" to binding.tabCancelled
        )

        tabs.forEach { (tabKey, tabView) ->
            tabView.setOnClickListener {
                viewModel.activeTabStatus.value = tabKey
            }
        }
    }

    /* private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(Color.parseColor("#4F6544"))
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.syncFromFirebase()
        }
    } */

    override fun observeViewModel() {
        viewModel.filteredOrders.observe(viewLifecycleOwner) { orders ->
            // binding.swipeRefresh.isRefreshing = false
            adapter.submitList(orders)
            
            /* if (orders.isEmpty()) {
                binding.llEmpty.visibility = View.VISIBLE
                binding.rvOrders.visibility = View.GONE
            } else {
                binding.llEmpty.visibility = View.GONE
                binding.rvOrders.visibility = View.VISIBLE
            } */
        }

        viewModel.activeTabStatus.observe(viewLifecycleOwner) { activeKey ->
            updateTabStyles(activeKey)
        }
    }

    private fun updateTabStyles(activeKey: String) {
        val tabs = mapOf(
            "ALL" to binding.tabAll,
            "PENDING" to binding.tabPending,
            "PROCESSING" to binding.tabPreparing,
            "SHIPPING" to binding.tabDelivering,
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
