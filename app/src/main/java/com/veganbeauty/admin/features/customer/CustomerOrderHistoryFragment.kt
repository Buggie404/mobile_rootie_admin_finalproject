package com.veganbeauty.admin.features.customer

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.admin.MainActivity
import com.veganbeauty.admin.R
import com.veganbeauty.admin.core.base.RootieAdminFragment
import com.veganbeauty.admin.data.local.RootieAdminDatabase
import com.veganbeauty.admin.data.local.entities.CustomerEntity
import com.veganbeauty.admin.data.local.entities.OrderEntity
import com.veganbeauty.admin.data.local.entities.OrderItem
import com.veganbeauty.admin.databinding.CustomerOrderHistoryBinding
import com.veganbeauty.admin.databinding.ItemOrderHistoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.NumberFormat
import java.util.Locale

class CustomerOrderHistoryFragment : RootieAdminFragment() {

    private var _binding: CustomerOrderHistoryBinding? = null
    private val binding get() = _binding!!

    private var customerId: String? = null
    private var fromStaff: Boolean = false
    
    private val allOrdersList = mutableListOf<OrderEntity>()
    private var filteredOrdersList = listOf<OrderEntity>()
    
    private lateinit var adapter: OrderHistoryAdapter
    
    private var currentFilter = "all" // all, completed, delivering
    private var showLimit = 5
    private var currentCustomer: CustomerEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            customerId = it.getString(ARG_CUSTOMER_ID)
            fromStaff = it.getBoolean(ARG_FROM_STAFF, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CustomerOrderHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        setupRecyclerView()
        setupListeners()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = OrderHistoryAdapter(emptyList())
        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            navigateBack()
        }

        binding.tabAll.setOnClickListener {
            selectTab("all")
        }
        binding.tabCompleted.setOnClickListener {
            selectTab("completed")
        }
        binding.tabDelivering.setOnClickListener {
            selectTab("delivering")
        }

        binding.btnLoadMore.setOnClickListener {
            // Expand to show all orders
            showLimit = filteredOrdersList.size
            updateListUI()
        }
    }

    private fun selectTab(filter: String) {
        currentFilter = filter
        showLimit = 5 // Reset page limit when switching tabs
        
        // Reset tabs style
        val tabs = listOf(binding.tabAll, binding.tabCompleted, binding.tabDelivering)
        tabs.forEach {
            it.setBackgroundResource(R.drawable.bg_search_bar)
            it.setTextColor(Color.parseColor("#3E4D44"))
            it.backgroundTintList = null
        }

        // Apply active styling to selected tab matching mockup (light green background, dark text)
        val selectedView = when(filter) {
            "all" -> binding.tabAll
            "completed" -> binding.tabCompleted
            "delivering" -> binding.tabDelivering
            else -> binding.tabAll
        }
        selectedView.setBackgroundResource(R.drawable.bg_nav_pill)
        selectedView.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E5E8DA"))
        selectedView.setTextColor(Color.parseColor("#4F6544"))

        applyFilters()
    }

    private fun loadData() {
        val uId = customerId ?: return
        lifecycleScope.launch {
            val db = RootieAdminDatabase.getDatabase(requireContext().applicationContext)
            
            // 1. Fetch customer
            currentCustomer = withContext(Dispatchers.IO) {
                db.customerDao().getByIdSync(uId)
            }
            
            currentCustomer?.let {
                bindCustomerHeader(it)
            } ?: run {
                Toast.makeText(requireContext(), "Không tìm thấy khách hàng!", Toast.LENGTH_SHORT).show()
                navigateBack()
                return@launch
            }

            // 2. Fetch orders, populate if empty
            var orders = withContext(Dispatchers.IO) {
                db.orderDao().getAllSync()
            }
            
            if (orders.isEmpty()) {
                val parsedOrders = withContext(Dispatchers.IO) {
                    parseOrdersFromAssets()
                }
                if (parsedOrders.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        db.orderDao().insertAllSync(parsedOrders)
                    }
                    orders = parsedOrders
                }
            }

            // 3. Filter orders belonging to this user
            val userOrders = orders.filter { it.userId == uId }
            allOrdersList.clear()
            allOrdersList.addAll(userOrders)

            // Setup default filter and render list
            selectTab("all")
        }
    }

    private fun bindCustomerHeader(customer: CustomerEntity) {
        binding.txtCustomerName.text = customer.name
        binding.txtCustomerId.text = "KH-${customer.id.uppercase()}"
        binding.txtTotalOrders.text = customer.orderCount.toString()
        binding.txtTotalSpending.text = formatSpending(customer.spending)
    }

    private fun applyFilters() {
        filteredOrdersList = when(currentFilter) {
            "completed" -> allOrdersList.filter { 
                it.status.lowercase() in listOf("hoàn thành", "hoàn tất") 
            }
            "delivering" -> allOrdersList.filter { 
                it.status.lowercase() == "đang giao" 
            }
            else -> allOrdersList
        }
        updateListUI()
    }

    private fun updateListUI() {
        val totalCount = filteredOrdersList.size
        val itemsToShow = filteredOrdersList.take(showLimit)
        adapter.updateData(itemsToShow)

        if (totalCount > showLimit) {
            val remaining = totalCount - showLimit
            binding.btnLoadMore.visibility = View.VISIBLE
            binding.txtLoadMore.text = "Xem thêm (còn $remaining đơn hàng)"
        } else {
            binding.btnLoadMore.visibility = View.GONE
        }
    }

    private fun parseOrdersFromAssets(): List<OrderEntity> {
        val list = mutableListOf<OrderEntity>()
        try {
            val inputStream = requireContext().assets.open("orders.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val jsonArray = jsonObject.getJSONArray("orders")

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val itemsArray = obj.getJSONArray("items")
                val orderItems = mutableListOf<OrderItem>()
                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    orderItems.add(
                        OrderItem(
                            productId = itemObj.getString("productId"),
                            productName = itemObj.getString("productName"),
                            productImage = itemObj.getString("productImage"),
                            quantity = itemObj.getInt("quantity"),
                            price = itemObj.getLong("price")
                        )
                    )
                }
                list.add(
                    OrderEntity(
                        orderId = obj.getString("id"),
                        userId = obj.getString("userId"),
                        orderDate = obj.getString("orderDate"),
                        orderTime = obj.getString("orderTime"),
                        status = obj.getString("status"),
                        totalAmount = obj.getLong("totalAmount"),
                        items = orderItems,
                        shippingName = obj.optString("shippingName", ""),
                        shippingPhone = obj.optString("shippingPhone", ""),
                        shippingAddress = obj.optString("shippingAddress", ""),
                        shippingCost = obj.optLong("shippingCost", 0L),
                        voucherDiscount = obj.optLong("voucherDiscount", 0L),
                        paymentMethod = obj.optString("paymentMethod", ""),
                        expectedDeliveryTime = if (obj.has("expectedDeliveryTime") && !obj.isNull("expectedDeliveryTime")) obj.getString("expectedDeliveryTime") else null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun formatSpending(spending: Long): String {
        val million = spending / 1000000.0
        val formatted = "%.1f".format(million).replace(",", ".")
        return if (formatted.endsWith(".0")) {
            "${formatted.substring(0, formatted.length - 2)}M"
        } else {
            "${formatted}M"
        }
    }

    private fun navigateBack() {
        val mainActivity = activity as? MainActivity ?: return
        customerId?.let { id ->
            mainActivity.loadFragment(CustomerDetailFragment.newInstance(id, fromStaff))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CUSTOMER_ID = "arg_customer_id"
        private const val ARG_FROM_STAFF = "arg_from_staff"

        fun newInstance(customerId: String, fromStaff: Boolean = false): CustomerOrderHistoryFragment {
            val fragment = CustomerOrderHistoryFragment()
            val args = Bundle()
            args.putString(ARG_CUSTOMER_ID, customerId)
            args.putBoolean(ARG_FROM_STAFF, fromStaff)
            fragment.arguments = args
            return fragment
        }
    }

    // RecyclerView Adapter
    private class OrderHistoryAdapter(private var list: List<OrderEntity>) : 
        RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemOrderHistoryBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemOrderHistoryBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val order = list[position]
            with(holder.binding) {
                txtOrderId.text = "#${order.orderId}"
                txtOrderDatetime.text = "${order.orderTime}, ${order.orderDate}"
                
                val totalQty = order.items.sumOf { it.quantity }
                txtProductsCount.text = "%02d Sản phẩm".format(totalQty)
                
                // Format price
                val formatter = NumberFormat.getIntegerInstance(Locale("vi", "VN"))
                txtTotalAmount.text = "${formatter.format(order.totalAmount)}đ"

                // Status styling
                badgeStatus.text = order.status.uppercase()
                when (order.status.lowercase()) {
                    "hoàn thành", "hoàn tất" -> {
                        badgeStatus.setTextColor(Color.parseColor("#4F6544"))
                        badgeStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E5E8DA"))
                    }
                    "đang giao" -> {
                        badgeStatus.setTextColor(Color.parseColor("#FFFFFF"))
                        badgeStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2C3B2D"))
                    }
                    "huỷ", "đã huỷ" -> {
                        badgeStatus.setTextColor(Color.parseColor("#D32F2F"))
                        badgeStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
                    }
                    else -> { // Đang xử lý / Chờ xử lý
                        badgeStatus.setTextColor(Color.parseColor("#C69C2C"))
                        badgeStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFFDE7"))
                    }
                }
            }
        }

        override fun getItemCount() = list.size

        fun updateData(newList: List<OrderEntity>) {
            list = newList
            notifyDataSetChanged()
        }
    }
}
