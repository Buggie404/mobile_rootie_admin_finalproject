package com.veganbeauty.admin.features.customer

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
import com.veganbeauty.admin.data.local.entities.CustomerEntity
import com.veganbeauty.admin.databinding.CustomerStaffMainFragmentBinding
import com.veganbeauty.admin.features.home.BottomNavHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.ceil
import kotlin.math.min

class CustomerStaffFragment : RootieAdminFragment() {

    private var _binding: CustomerStaffMainFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CustomerAdapter
    private val allCustomersList = mutableListOf<CustomerEntity>()
    private var filteredCustomersList = listOf<CustomerEntity>()

    // Filter states
    private var currentSearchQuery = ""
    private var currentSelectedTab = "tất cả"

    // Pagination states
    private var currentPage = 1
    private val itemsPerPage = 5

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CustomerStaffMainFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            BottomNavHelper.setup(
                activity = mainActivity,
                root = binding.root,
                activeTabId = R.id.nav_customer
            ) { tabId ->
                BottomNavHelper.navigate(mainActivity, tabId)
            }
        }

        setupRecyclerView()
        setupListeners()
        loadData()

        // Bind message button in header
        setupHeaderMessageButton(binding.btnMessage)
    }

    private fun setupRecyclerView() {
        adapter = CustomerAdapter(
            items = emptyList(),
            onDetailClick = { customer ->
                val detailFragment = CustomerDetailFragment.newInstance(customer.id, fromStaff = true)
                (activity as? MainActivity)?.loadFragment(detailFragment)
            },
            onNoteClick = { customer ->
                val bottomSheet = CustomerNoteBottomSheet.newInstance(customer.id)
                bottomSheet.onNoteSaved = {
                    loadData()
                }
                bottomSheet.show(parentFragmentManager, "CustomerNoteBottomSheet")
            }
        )
        binding.rvCustomers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCustomers.adapter = adapter
    }

    private fun setupListeners() {
        // Back Button
        binding.btnBack.setOnClickListener {
            val mainActivity = activity as? MainActivity
            mainActivity?.let {
                BottomNavHelper.navigate(it, R.id.nav_home)
            }
        }

        // Notification Button
        binding.btnNotification.setOnClickListener {
            Toast.makeText(requireContext(), "Mở thông báo", Toast.LENGTH_SHORT).show()
        }

        // Search Input
        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                currentPage = 1
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Tabs Selection
        binding.tabAll.setOnClickListener { selectTab("tất cả", binding.tabAll) }
        binding.tabVip.setOnClickListener { selectTab("vip", binding.tabVip) }
        binding.tabGold.setOnClickListener { selectTab("vàng", binding.tabGold) }
        binding.tabSilver.setOnClickListener { selectTab("bạc", binding.tabSilver) }
        binding.tabNormal.setOnClickListener { selectTab("thường", binding.tabNormal) }

        // Pagination buttons
        binding.btnPagePrev.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                updatePaginationUI()
            }
        }

        binding.btnPageNext.setOnClickListener {
            val totalPages = ceil(filteredCustomersList.size.toDouble() / itemsPerPage).toInt()
            if (currentPage < totalPages) {
                currentPage++
                updatePaginationUI()
            }
        }
    }

    private fun selectTab(tab: String, selectedView: TextView) {
        currentSelectedTab = tab
        currentPage = 1

        // Reset all tabs styles
        val tabs = listOf(binding.tabAll, binding.tabVip, binding.tabGold, binding.tabSilver, binding.tabNormal)
        tabs.forEach {
            it.setBackgroundResource(R.drawable.bg_search_bar)
            it.setTextColor(Color.parseColor("#3E4D44"))
            it.backgroundTintList = null
        }

        // Highlight selected tab
        selectedView.setBackgroundResource(R.drawable.bg_nav_pill)
        selectedView.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2C3B2D"))
        selectedView.setTextColor(Color.parseColor("#FFFFFF"))

        applyFilters()
    }

    private fun loadData() {
        lifecycleScope.launch {
            val database = RootieAdminDatabase.getDatabase(requireContext().applicationContext)
            val customerDao = database.customerDao()

            // 1. Fetch from local DB
            var localCustomers = withContext(Dispatchers.IO) {
                customerDao.getAllSync()
            }

            // 2. If local DB is empty, parse from assets/users.json
            if (localCustomers.isEmpty()) {
                val parsedCustomers = withContext(Dispatchers.IO) {
                    parseCustomersFromAssets()
                }
                if (parsedCustomers.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        customerDao.insertAllSync(parsedCustomers)
                    }
                    localCustomers = parsedCustomers
                }
            }

            // 3. Filter out admin and staff roles
            val activeCustomers = localCustomers.filter {
                val r = it.role.lowercase()
                r != "admin" && r != "employee" && r != "staff"
            }

            allCustomersList.clear()
            allCustomersList.addAll(activeCustomers)

            applyFilters()
        }
    }

    private fun parseCustomersFromAssets(): List<CustomerEntity> {
        val list = mutableListOf<CustomerEntity>()
        try {
            val inputStream = requireContext().assets.open("users.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    CustomerEntity(
                        id = obj.optString("user_id", java.util.UUID.randomUUID().toString()),
                        name = if (obj.has("full_name") && !obj.getString("full_name").isNullOrEmpty()) obj.getString("full_name") else obj.optString("username", ""),
                        email = obj.optString("email", ""),
                        phone = obj.optString("phone", ""),
                        address = obj.optString("bio", ""),
                        avatar = obj.optString("avatar", ""),
                        spending = obj.optLong("spending", 0L),
                        tier = obj.optString("tier", "Thường"),
                        lastActive = obj.optString("last_active", ""),
                        notes = obj.optString("notes", ""),
                        role = obj.optString("role", "customer"),
                        birthday = obj.optString("birthday", ""),
                        region = obj.optString("region", ""),
                        joinYear = obj.optInt("join_year", 1),
                        orderCount = obj.optInt("order_count", 0),
                        recentPurchase = obj.optString("recent_purchase", ""),
                        spendingYear = obj.optLong("spending_year", 0L),
                        spendingMonth = obj.optLong("spending_month", 0L),
                        points = obj.optInt("points", 0)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun applyFilters() {
        var result = allCustomersList.toList()

        // Apply Tab Filter
        if (currentSelectedTab != "tất cả") {
            result = result.filter { it.tier.lowercase() == currentSelectedTab }
        }

        // Apply Search Query
        if (currentSearchQuery.isNotEmpty()) {
            result = result.filter {
                it.name.contains(currentSearchQuery, ignoreCase = true) ||
                        it.phone.contains(currentSearchQuery)
            }
        }

        filteredCustomersList = result
        updatePaginationUI()
    }

    private fun updatePaginationUI() {
        val totalItems = filteredCustomersList.size
        val totalPages = ceil(totalItems.toDouble() / itemsPerPage).toInt()

        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages
        } else if (currentPage < 1) {
            currentPage = 1
        }

        // Slice items for current page
        val startIndex = (currentPage - 1) * itemsPerPage
        val endIndex = min(startIndex + itemsPerPage, totalItems)

        val pageItems = if (totalItems > 0 && startIndex < totalItems) {
            filteredCustomersList.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        adapter.updateData(pageItems)

        // Update footer text
        val displayedCount = pageItems.size
        binding.txtCountFooter.text = "Hiển thị $displayedCount / $totalItems khách hàng"

        // Update pagination buttons container visibility
        if (totalPages <= 1) {
            binding.paginationContainer.visibility = View.GONE
        } else {
            binding.paginationContainer.visibility = View.VISIBLE
            setupPageNumbers(totalPages)
        }
    }

    private fun setupPageNumbers(totalPages: Int) {
        // Enable/Disable prev button
        binding.btnPagePrev.isEnabled = currentPage > 1
        binding.btnPagePrev.alpha = if (currentPage > 1) 1.0f else 0.4f

        // Enable/Disable next button
        binding.btnPageNext.isEnabled = currentPage < totalPages
        binding.btnPageNext.alpha = if (currentPage < totalPages) 1.0f else 0.4f

        val pageViews = listOf(binding.btnPage1, binding.btnPage2, binding.btnPage3)

        // Reset all page styles
        pageViews.forEach { view ->
            view.visibility = View.GONE
            view.setBackgroundResource(R.drawable.bg_search_bar)
            view.setTextColor(Color.parseColor("#3E4D44"))
            view.backgroundTintList = null
        }

        // Determine which page numbers to show
        val startPage = when {
            totalPages <= 3 -> 1
            currentPage == 1 -> 1
            currentPage == totalPages -> totalPages - 2
            else -> currentPage - 1
        }

        for (i in 0 until min(3, totalPages)) {
            val pageNum = startPage + i
            val view = pageViews[i]
            view.text = pageNum.toString()
            view.visibility = View.VISIBLE

            if (pageNum == currentPage) {
                view.setBackgroundResource(R.drawable.bg_nav_pill)
                view.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2C3B2D"))
                view.setTextColor(Color.parseColor("#FFFFFF"))
            }

            view.setOnClickListener {
                currentPage = pageNum
                updatePaginationUI()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
