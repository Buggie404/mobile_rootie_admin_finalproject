package com.veganbeauty.admin.features.order

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.veganbeauty.admin.MainActivity
import com.veganbeauty.admin.R
import com.veganbeauty.admin.core.base.RootieAdminFragment
import com.veganbeauty.admin.data.local.RootieAdminDatabase
import com.veganbeauty.admin.data.local.entities.OrderEntity
import com.veganbeauty.admin.data.local.entities.OrderItem
import com.veganbeauty.admin.data.remote.FirebaseService
import com.veganbeauty.admin.data.repository.OrderRepository
import com.veganbeauty.admin.databinding.OrderFragmentListBinding
import com.veganbeauty.admin.features.home.BottomNavHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrderListFragment : RootieAdminFragment() {

    private var _binding: OrderFragmentListBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: OrderRepository
    private lateinit var adapter: OrderAdapter

    private val allOrdersList = mutableListOf<OrderEntity>()
    private var filteredOrdersList = listOf<OrderEntity>()
    private val selectedOrderIds = mutableSetOf<String>()

    private var currentSearchQuery = ""
    private var currentSelectedTab = "tất cả"

    private var currentSort = "DEFAULT"
    private val filterPaymentMethods = mutableSetOf<String>()
    private val filterPriceRanges = mutableSetOf<String>()
    private val filterRegions = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = OrderFragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            BottomNavHelper.setup(
                activity = mainActivity,
                root = binding.root,
                activeTabId = R.id.nav_order
            ) { tabId ->
                BottomNavHelper.navigate(mainActivity, tabId)
            }
        }

        // Initialize Repository
        val database = RootieAdminDatabase.getDatabase(requireContext().applicationContext)
        repository = OrderRepository(database.orderDao(), FirebaseService())

        setupRecyclerView()
        setupListeners()
        selectTab("tất cả", binding.tabAll)
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = OrderAdapter(
            items = emptyList(),
            selectedOrderIds = emptySet(),
            onOrderSelectionToggled = { order ->
                toggleOrderSelection(order.orderId)
            },
            onCancelClick = { order ->
                updateSingleOrderStatus(order.orderId, "Đã hủy")
            },
            onApproveClick = { order ->
                val nextStatus = getNextStatus(order.status)
                updateSingleOrderStatus(order.orderId, nextStatus)
            },
            onItemClick = { order ->
                val detailFragment = OrderDetailFragment.newInstance(order.orderId)
                (activity as? MainActivity)?.loadFragment(detailFragment)
            }
        )

        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = adapter
    }

    private fun setupListeners() {
        // Notification bell click
        binding.btnNotification.setOnClickListener {
            Toast.makeText(requireContext(), "Mở thông báo", Toast.LENGTH_SHORT).show()
        }

        // Search Input
        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Tabs Click Listeners
        binding.tabAll.setOnClickListener { selectTab("tất cả", binding.tabAll) }
        binding.tabPending.setOnClickListener { selectTab("chờ xác nhận", binding.tabPending) }
        binding.tabPreparing.setOnClickListener { selectTab("đang chuẩn bị", binding.tabPreparing) }
        binding.tabDelivering.setOnClickListener { selectTab("đang giao", binding.tabDelivering) }
        binding.tabCompleted.setOnClickListener { selectTab("hoàn tất", binding.tabCompleted) }
        binding.tabCancelled.setOnClickListener { selectTab("đã hủy", binding.tabCancelled) }

        // Select All Click Listener
        binding.layoutSelectAll.setOnClickListener {
            toggleSelectAll()
        }

        // Bulk Actions
        binding.btnBulkCancel.setOnClickListener {
            performBulkStatusUpdate("Đã hủy")
        }

        binding.btnBulkApprove.setOnClickListener {
            performBulkApprove()
        }

        // Lọc & Sắp xếp
        binding.btnSort.setOnClickListener {
            val sortSheet = OrderSortBottomSheet(currentSort) { selectedSort ->
                currentSort = selectedSort
                applyFilters()
            }
            sortSheet.show(childFragmentManager, "OrderSortBottomSheet")
        }
        binding.btnFilter.setOnClickListener {
            val filterSheet = OrderFilterBottomSheet(filterPaymentMethods, filterPriceRanges, filterRegions) { payments, prices, regions ->
                filterPaymentMethods.clear()
                filterPaymentMethods.addAll(payments)
                filterPriceRanges.clear()
                filterPriceRanges.addAll(prices)
                filterRegions.clear()
                filterRegions.addAll(regions)
                applyFilters()
            }
            filterSheet.show(childFragmentManager, "OrderFilterBottomSheet")
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            var localOrders = withContext(Dispatchers.IO) {
                repository.allOrders.value ?: emptyList()
            }

            // Fallback: If DB is empty, parse from assets/orders.json and seed the local DB
            if (localOrders.isEmpty()) {
                val parsedOrders = withContext(Dispatchers.IO) {
                    parseOrdersFromAssets()
                }
                if (parsedOrders.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        val database = RootieAdminDatabase.getDatabase(requireContext().applicationContext)
                        database.orderDao().insertAllSync(parsedOrders)
                    }
                    localOrders = parsedOrders
                }
            }

            allOrdersList.clear()
            allOrdersList.addAll(localOrders)

            applyFilters()
        }
    }

    private fun parseOrdersFromAssets(): List<OrderEntity> {
        val list = mutableListOf<OrderEntity>()
        try {
            val jsonString = requireContext().assets.open("orders.json").bufferedReader().use { it.readText() }
            val root = org.json.JSONObject(jsonString)
            val jsonArray = root.getJSONArray("orders")
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val itemsRaw = obj.optJSONArray("items")
                val orderItems = mutableListOf<OrderItem>()
                if (itemsRaw != null) {
                    for (j in 0 until itemsRaw.length()) {
                        val itemObj = itemsRaw.getJSONObject(j)
                        orderItems.add(
                            OrderItem(
                                productId = itemObj.optString("productId", ""),
                                productName = itemObj.optString("productName", ""),
                                productImage = itemObj.optString("productImage", ""),
                                quantity = itemObj.optInt("quantity", 0),
                                price = itemObj.optLong("price", 0L)
                            )
                        )
                    }
                }
                list.add(
                    OrderEntity(
                        orderId = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        userId = obj.optString("userId", ""),
                        orderDate = obj.optString("orderDate", ""),
                        orderTime = obj.optString("orderTime", ""),
                        status = obj.optString("status", ""),
                        totalAmount = obj.optLong("totalAmount", 0L),
                        items = orderItems,
                        shippingName = obj.optString("shippingName", ""),
                        shippingPhone = obj.optString("shippingPhone", ""),
                        shippingAddress = obj.optString("shippingAddress", ""),
                        shippingCost = obj.optLong("shippingCost", 0L),
                        voucherDiscount = obj.optLong("voucherDiscount", 0L),
                        paymentMethod = obj.optString("paymentMethod", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun selectTab(tab: String, selectedView: TextView) {
        currentSelectedTab = tab
        selectedOrderIds.clear() // Clear selection on tab change

        // Reset all tabs styles
        val tabs = listOf(
            binding.tabAll,
            binding.tabPending,
            binding.tabPreparing,
            binding.tabDelivering,
            binding.tabCompleted,
            binding.tabCancelled
        )

        tabs.forEach {
            it.setBackgroundResource(R.drawable.bg_search_bar)
            it.setTextColor(Color.parseColor("#3E4D44"))
            it.backgroundTintList = null
            it.setTypeface(null, android.graphics.Typeface.NORMAL)
        }

        // Highlight selected tab
        selectedView.setBackgroundResource(R.drawable.bg_nav_pill)
        selectedView.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2C3B2D"))
        selectedView.setTextColor(Color.parseColor("#FFFFFF"))
        selectedView.setTypeface(null, android.graphics.Typeface.BOLD)

        applyFilters()
    }

    private fun applyFilters() {
        var result = allOrdersList.toList()

        // 1. Apply non-tab filters first (Search, Payment, Price, Region)
        // Search Query Filtering
        if (currentSearchQuery.isNotEmpty()) {
            result = result.filter { order ->
                order.orderId.contains(currentSearchQuery, ignoreCase = true) ||
                        order.shippingName.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        // Payment Filtering (COD, ATM Banking, MoMo, VNPay)
        if (filterPaymentMethods.isNotEmpty()) {
            result = result.filter { order ->
                val method = order.paymentMethod.lowercase()
                var match = false
                if (filterPaymentMethods.contains("COD") && (method.contains("cod") || method.contains("nhận hàng"))) match = true
                if (filterPaymentMethods.contains("ATM") && (method.contains("chuyển khoản") || method.contains("atm") || method.contains("ngân hàng"))) match = true
                if (filterPaymentMethods.contains("MOMO") && method.contains("momo")) match = true
                if (filterPaymentMethods.contains("VNPAY") && method.contains("vnpay")) match = true
                match
            }
        }

        // Price Range Filtering (UNDER_500K, 500K_TO_1500K, OVER_1500K)
        if (filterPriceRanges.isNotEmpty()) {
            result = result.filter { order ->
                var match = false
                if (filterPriceRanges.contains("UNDER_500K") && order.totalAmount < 500000L) match = true
                if (filterPriceRanges.contains("500K_TO_1500K") && order.totalAmount in 500000L..1500000L) match = true
                if (filterPriceRanges.contains("OVER_1500K") && order.totalAmount > 1500000L) match = true
                match
            }
        }

        // Region Filtering (HCMC, HN, OTHERS)
        if (filterRegions.isNotEmpty()) {
            result = result.filter { order ->
                val addr = order.shippingAddress.lowercase()
                var match = false
                if (filterRegions.contains("HCMC") && (addr.contains("hồ chí minh") || addr.contains("hcm"))) match = true
                if (filterRegions.contains("HN") && (addr.contains("hà nội") || addr.contains("hn"))) match = true
                if (filterRegions.contains("OTHERS") && !addr.contains("hồ chí minh") && !addr.contains("hcm") && !addr.contains("hà nội") && !addr.contains("hn")) match = true
                match
            }
        }

        // Update tab counts with search & filter results
        updateTabCounts(result)

        // 2. Apply Tab Filtering
        if (currentSelectedTab != "tất cả") {
            result = result.filter { order ->
                val statusLower = order.status.trim().lowercase()
                when (currentSelectedTab) {
                    "chờ xác nhận" -> statusLower.contains("chờ xử lý") || statusLower.contains("chờ xác nhận")
                    "đang chuẩn bị" -> statusLower.contains("đang xử lý") || statusLower.contains("đang chuẩn bị")
                    "đang giao" -> statusLower.contains("đang giao")
                    "hoàn tất" -> statusLower.contains("hoàn tất")
                    "đã hủy" -> statusLower.contains("đã hủy")
                    else -> true
                }
            }
        }

        // Sorting (DEFAULT, DATE_DESC, DATE_ASC, PRICE_DESC, PRICE_ASC)
        if (currentSort != "DEFAULT") {
            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.US)
            fun getOrderDateTime(order: OrderEntity): java.util.Date? {
                return try {
                    dateFormat.parse("${order.orderDate} ${order.orderTime}")
                } catch (e: Exception) {
                    null
                }
            }

            result = when (currentSort) {
                "DATE_DESC" -> result.sortedWith(compareByDescending<OrderEntity> { getOrderDateTime(it) }.thenByDescending { it.orderId })
                "DATE_ASC" -> result.sortedWith(compareBy<OrderEntity> { getOrderDateTime(it) }.thenBy { it.orderId })
                "PRICE_DESC" -> result.sortedByDescending { it.totalAmount }
                "PRICE_ASC" -> result.sortedBy { it.totalAmount }
                else -> result
            }
        }

        filteredOrdersList = result
        updateListUI()
    }

    private fun updateListUI() {
        val selectionAllowed = currentSelectedTab != "tất cả" && currentSelectedTab != "hoàn tất" && currentSelectedTab != "đã hủy"
        adapter.updateData(filteredOrdersList, selectedOrderIds, selectionAllowed)
        if (selectionAllowed) {
            binding.layoutSelectionRow.visibility = View.VISIBLE
        } else {
            binding.layoutSelectionRow.visibility = View.GONE
        }
        updateBulkActionsBarVisibility()
        updateSelectAllCheckboxUI()
    }

    private fun toggleOrderSelection(orderId: String) {
        if (selectedOrderIds.contains(orderId)) {
            selectedOrderIds.remove(orderId)
        } else {
            selectedOrderIds.add(orderId)
        }
        updateListUI()
    }

    private fun toggleSelectAll() {
        val allDisplayedIds = filteredOrdersList.map { it.orderId }
        val allDisplayedSelected = selectedOrderIds.containsAll(allDisplayedIds) && allDisplayedIds.isNotEmpty()

        if (allDisplayedSelected) {
            selectedOrderIds.removeAll(allDisplayedIds.toSet())
        } else {
            selectedOrderIds.addAll(allDisplayedIds)
        }
        updateListUI()
    }

    private fun updateSelectAllCheckboxUI() {
        val allDisplayedIds = filteredOrdersList.map { it.orderId }
        val isAllSelected = selectedOrderIds.containsAll(allDisplayedIds) && allDisplayedIds.isNotEmpty()

        binding.imgSelectAllCheckbox.setImageResource(
            if (isAllSelected) R.drawable.ic_checkbox_checked
            else R.drawable.ic_radio_primary_unchecked
        )

        binding.txtSelectedCount.text = "Đã chọn: ${selectedOrderIds.size}"
    }

    private fun updateBulkActionsBarVisibility() {
        val size = selectedOrderIds.size
        if (size > 0 && currentSelectedTab != "tất cả" && currentSelectedTab != "hoàn tất" && currentSelectedTab != "đã hủy") {
            binding.layoutBulkActions.visibility = View.VISIBLE
            when (currentSelectedTab) {
                "chờ xác nhận" -> {
                    binding.btnBulkCancel.text = "Hủy ($size)"
                    binding.btnBulkApprove.text = "Xác nhận ($size)"
                }
                "đang chuẩn bị" -> {
                    binding.btnBulkCancel.text = "Hủy ($size)"
                    binding.btnBulkApprove.text = "Giao hàng ($size)"
                }
                "đang giao" -> {
                    binding.btnBulkCancel.text = "Thất bại ($size)"
                    binding.btnBulkApprove.text = "Hoàn tất ($size)"
                }
            }
        } else {
            binding.layoutBulkActions.visibility = View.GONE
        }
    }

    private fun updateSingleOrderStatus(orderId: String, newStatus: String) {
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                repository.updateOrderStatus(orderId, newStatus)
            }
            if (success) {
                Toast.makeText(requireContext(), "Cập nhật trạng thái thành công!", Toast.LENGTH_SHORT).show()
                // Refresh local lists
                val index = allOrdersList.indexOfFirst { it.orderId == orderId }
                if (index != -1) {
                    val updated = allOrdersList[index].copy(status = newStatus)
                    allOrdersList[index] = updated
                }
                selectedOrderIds.remove(orderId)
                applyFilters()
            } else {
                Toast.makeText(requireContext(), "Cập nhật thất bại!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performBulkStatusUpdate(newStatus: String) {
        lifecycleScope.launch {
            val idsToUpdate = selectedOrderIds.toList()
            var failCount = 0

            withContext(Dispatchers.IO) {
                for (id in idsToUpdate) {
                    val success = repository.updateOrderStatus(id, newStatus)
                    if (!success) {
                        failCount++
                    }
                }
            }

            if (failCount == 0) {
                Toast.makeText(requireContext(), "Cập nhật hàng loạt thành công!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Cập nhật thành công. Thất bại $failCount đơn.", Toast.LENGTH_SHORT).show()
            }

            selectedOrderIds.clear()
            // Reload all from database to get fresh sync
            val localOrders = withContext(Dispatchers.IO) {
                val database = RootieAdminDatabase.getDatabase(requireContext().applicationContext)
                database.orderDao().getAllSync()
            }
            allOrdersList.clear()
            allOrdersList.addAll(localOrders)

            applyFilters()
        }
    }

    private fun performBulkApprove() {
        lifecycleScope.launch {
            val idsToUpdate = selectedOrderIds.toList()
            var failCount = 0

            withContext(Dispatchers.IO) {
                for (id in idsToUpdate) {
                    val order = allOrdersList.firstOrNull { it.orderId == id }
                    if (order != null) {
                        val nextStatus = getNextStatus(order.status)
                        val success = repository.updateOrderStatus(id, nextStatus)
                        if (!success) {
                            failCount++
                        }
                    }
                }
            }

            if (failCount == 0) {
                Toast.makeText(requireContext(), "Cập nhật hàng loạt thành công!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Hoàn thành cập nhật. Lỗi $failCount đơn.", Toast.LENGTH_SHORT).show()
            }

            selectedOrderIds.clear()
            val localOrders = withContext(Dispatchers.IO) {
                val database = RootieAdminDatabase.getDatabase(requireContext().applicationContext)
                database.orderDao().getAllSync()
            }
            allOrdersList.clear()
            allOrdersList.addAll(localOrders)

            applyFilters()
        }
    }

    private fun getNextStatus(currentStatus: String): String {
        val statusClean = currentStatus.trim().lowercase()
        return when {
            statusClean.contains("chờ xử lý") || statusClean.contains("chờ xác nhận") -> "Đang chuẩn bị"
            statusClean.contains("đang xử lý") || statusClean.contains("đang chuẩn bị") -> "Đang giao"
            statusClean.contains("đang giao") -> "Hoàn tất"
            else -> currentStatus
        }
    }

    private fun updateTabCounts(orders: List<OrderEntity>) {
        val countAll = orders.size
        var countPending = 0
        var countPreparing = 0
        var countDelivering = 0
        var countCompleted = 0
        var countCancelled = 0

        orders.forEach { order ->
            val statusLower = order.status.trim().lowercase()
            when {
                statusLower.contains("chờ xử lý") || statusLower.contains("chờ xác nhận") -> countPending++
                statusLower.contains("đang xử lý") || statusLower.contains("đang chuẩn bị") -> countPreparing++
                statusLower.contains("đang giao") -> countDelivering++
                statusLower.contains("hoàn tất") -> countCompleted++
                statusLower.contains("đã hủy") -> countCancelled++
            }
        }

        binding.tabAll.text = "Tất cả ($countAll)"
        binding.tabPending.text = "Chờ xác nhận ($countPending)"
        binding.tabPreparing.text = "Đang chuẩn bị ($countPreparing)"
        binding.tabDelivering.text = "Đang giao ($countDelivering)"
        binding.tabCompleted.text = "Hoàn tất ($countCompleted)"
        binding.tabCancelled.text = "Đã hủy ($countCancelled)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
