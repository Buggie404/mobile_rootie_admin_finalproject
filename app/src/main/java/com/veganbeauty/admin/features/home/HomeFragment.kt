package com.veganbeauty.admin.features.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.veganbeauty.admin.MainActivity
import com.veganbeauty.admin.R
import com.veganbeauty.admin.core.base.RootieAdminFragment
import com.veganbeauty.admin.data.local.RootieAdminDatabase
import com.veganbeauty.admin.data.local.SessionManager
import com.veganbeauty.admin.databinding.HomeFragmentBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : RootieAdminFragment() {

    private var _binding: HomeFragmentBinding? = null
    private val binding get() = _binding!!

    private val vndFormat = NumberFormat.getNumberInstance(Locale("vi", "VN"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = HomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        val sessionManager = SessionManager(requireContext())
        val name = sessionManager.getFullName() ?: "Xuân"
        binding.tvGreeting.text = "Chào buổi sáng, $name"

        binding.cardSpaBooking.setOnClickListener {
            (requireActivity() as MainActivity).loadFragment(
                com.veganbeauty.admin.features.booking.list.BookingListFragment()
            )
        }

        // Bind sparkline data points
        binding.sparklineRevenue.setData(listOf(35f, 25f, 28f, 32f, 28f, 30f, 40f, 36f, 45f))
        binding.sparklineRevenue.setLineColor(0xFF677559.toInt())

        binding.sparklineOrders.setData(listOf(10f, 11f, 13f, 15f, 16f, 20f, 25f))
        binding.sparklineOrders.setLineColor(0xFF677559.toInt())

        // Build recent activities list
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

        binding.btnSeeAll.setOnClickListener {
            Toast.makeText(requireContext(), "Xem tất cả hoạt động", Toast.LENGTH_SHORT).show()
        }

        binding.fabAdd.setOnClickListener {
            Toast.makeText(requireContext(), "Thêm mới", Toast.LENGTH_SHORT).show()
        }

        // Bind header message icon
        setupHeaderMessageButton(binding.header.homeHeaderMessageBtn)
        setupHeaderNotification(binding.header.homeHeaderNotificationBtn, binding.header.homeHeaderNotificationBadge)

        // Role switcher (test)
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

        // === DASHBOARD THEO ROLE ===
        setupDashboardByRole(sessionManager)
    }

    // -------------------------------------------------------------------------
    // Dashboard dispatcher
    // -------------------------------------------------------------------------

    private fun setupDashboardByRole(sessionManager: SessionManager) {
        val role = sessionManager.getRole() ?: "admin"
        val isAdmin = role.equals("admin", ignoreCase = true) ||
                      role.equals("business", ignoreCase = true)

        if (isAdmin) {
            binding.llAdminDashboard.visibility = View.VISIBLE
            binding.llStaffDashboard.visibility = View.GONE
            loadAdminDashboard()
        } else {
            binding.llAdminDashboard.visibility = View.GONE
            binding.llStaffDashboard.visibility = View.VISIBLE
            loadStaffDashboard()
        }
    }

    // -------------------------------------------------------------------------
    // ADMIN Dashboard
    // -------------------------------------------------------------------------

    private fun loadAdminDashboard() {
        viewLifecycleOwner.lifecycleScope.launch {
            val orders = withContext(Dispatchers.IO) {
                RootieAdminDatabase.getDatabase(requireContext()).orderDao().getAllSync()
            }

            val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val cal = Calendar.getInstance()

            // --- Stat cards ---
            val newOrders     = orders.filter { it.status == "Chờ xử lý" }.size
            val pendingOrders = orders.filter { it.status == "Chờ xử lý" || it.status == "Đang xử lý" }.size
            val shippingOrders = orders.filter { it.status == "Đang giao" }.size
            val avgValue = if (orders.isNotEmpty()) orders.sumOf { it.totalAmount } / orders.size else 0L

            binding.tvStatNewOrders.text    = newOrders.toString()
            binding.tvStatPendingOrders.text = pendingOrders.toString()
            binding.tvStatShippingOrders.text = shippingOrders.toString()
            binding.tvStatAvgOrderValue.text = formatShort(avgValue)

            // --- Doanh thu & Tỷ lệ hủy theo tab ---
            setupRevenueTabs(orders)

            // --- Sản phẩm bán chạy (top 5 theo sold từ products.json) ---
            loadTopSellingProducts()
        }
    }

    private fun setupRevenueTabs(orders: List<com.veganbeauty.admin.data.local.entities.OrderEntity>) {
        fun calcRevenue(filter: (String) -> Boolean): Long {
            return orders
                .filter { filter(it.orderDate) && it.status == "Hoàn tất" }
                .sumOf { it.totalAmount }
        }

        fun calcCancelRate(filter: (String) -> Boolean): Float {
            val total = orders.filter { filter(it.orderDate) }.size
            val cancelled = orders.filter { filter(it.orderDate) &&
                (it.status == "Đã hủy" || it.status == "Hoàn hàng") }.size
            return if (total == 0) 0f else cancelled.toFloat() / total * 100f
        }

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = Calendar.getInstance()

        fun isToday(d: String): Boolean = try {
            val cal = Calendar.getInstance().apply { time = sdf.parse(d)!! }
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        } catch (e: Exception) { false }

        fun isThisWeek(d: String): Boolean = try {
            val cal = Calendar.getInstance().apply { time = sdf.parse(d)!! }
            cal.get(Calendar.WEEK_OF_YEAR) == today.get(Calendar.WEEK_OF_YEAR) &&
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        } catch (e: Exception) { false }

        fun isThisMonth(d: String): Boolean = try {
            val cal = Calendar.getInstance().apply { time = sdf.parse(d)!! }
            cal.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        } catch (e: Exception) { false }

        fun isThisQuarter(d: String): Boolean = try {
            val cal = Calendar.getInstance().apply { time = sdf.parse(d)!! }
            (cal.get(Calendar.MONTH) / 3) == (today.get(Calendar.MONTH) / 3) &&
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        } catch (e: Exception) { false }

        val periods = listOf(
            Triple(binding.tabRevenueToday,   ::isToday,      "Hôm nay"),
            Triple(binding.tabRevenueWeek,    ::isThisWeek,   "Tuần"),
            Triple(binding.tabRevenueMonth,   ::isThisMonth,  "Tháng"),
            Triple(binding.tabRevenueQuarter, ::isThisQuarter,"Quý")
        )

        fun selectTab(selected: TextView, filter: (String) -> Boolean) {
            periods.forEach { (tab, _, _) ->
                tab.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F2F4EB"))
                tab.setTextColor(Color.parseColor("#677559"))
                tab.setTypeface(null, Typeface.NORMAL)
            }
            selected.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4F6544"))
            selected.setTextColor(Color.WHITE)
            selected.setTypeface(null, Typeface.BOLD)

            val revenue = calcRevenue(filter)
            val rate    = calcCancelRate(filter)
            binding.tvRevenueValue.text = vndFormat.format(revenue)
            binding.tvCancelRate.text   = String.format("%.1f%%", rate)
        }

        // Default: hôm nay
        selectTab(binding.tabRevenueToday, ::isToday)

        periods.forEach { (tab, filter, _) ->
            tab.setOnClickListener { selectTab(tab, filter) }
        }
    }

    private fun loadTopSellingProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
            val top5 = withContext(Dispatchers.IO) {
                parseProductsJson()
                    .sortedByDescending { it.optLong("sold", 0) }
                    .take(5)
            }
            val container = binding.llTopSelling
            container.removeAllViews()
            top5.forEachIndexed { index, product ->
                val name = product.optString("name", "")
                val sold = product.optLong("sold", 0)
                container.addView(buildRankRow(index + 1, name, "${vndFormat.format(sold)} đã bán", "#4F6544"))
            }
        }
    }

    // -------------------------------------------------------------------------
    // STAFF Dashboard
    // -------------------------------------------------------------------------

    private fun loadStaffDashboard() {
        viewLifecycleOwner.lifecycleScope.launch {
            val products = withContext(Dispatchers.IO) { parseProductsJson() }

            // Top 5 quan tâm (theo sold)
            val topViewed = products.sortedByDescending { it.optLong("sold", 0) }.take(5)
            binding.llTopViewed.removeAllViews()
            topViewed.forEachIndexed { i, p ->
                binding.llTopViewed.addView(
                    buildRankRow(i + 1, p.optString("name"), "Đã bán: ${vndFormat.format(p.optLong("sold", 0))}", "#4F6544")
                )
            }

            // Top 5 rating cao
            val topRated = products.sortedByDescending { it.optDouble("rating", 0.0) }.take(5)
            binding.llTopRated.removeAllViews()
            topRated.forEachIndexed { i, p ->
                val rating = p.optDouble("rating", 0.0)
                binding.llTopRated.addView(
                    buildRankRow(i + 1, p.optString("name"), "⭐ ${"%.1f".format(rating)}", "#59AE7B")
                )
            }

            // Sản phẩm phản hồi xấu (rating < 3.5)
            val badProducts = products.filter { it.optDouble("rating", 5.0) < 3.5 }.take(5)
            binding.llBadFeedback.removeAllViews()
            if (badProducts.isEmpty()) {
                binding.llBadFeedback.addView(buildInfoText("Không có sản phẩm nào có phản hồi xấu 🎉", "#1B5E20"))
            } else {
                badProducts.forEach { p ->
                    val rating = p.optDouble("rating", 0.0)
                    binding.llBadFeedback.addView(
                        buildInfoText("⚠ ${p.optString("name")} — rating ${"%.1f".format(rating)}", "#B71C1C")
                    )
                }
            }

            // Biểu đồ loại da
            buildSkinChart(products)

            // FAQ tư vấn
            buildFaqSection()
        }
    }

    private fun buildSkinChart(products: List<JSONObject>) {
        val skinMap = linkedMapOf(
            "Da dầu" to 0, "Da hỗn hợp" to 0, "Da khô" to 0,
            "Da thường" to 0, "Da nhạy cảm" to 0, "Mọi loại da" to 0
        )
        products.forEach { p ->
            val suitable = p.optString("suitableFor", "")
            skinMap.keys.forEach { key ->
                if (suitable.contains(key, ignoreCase = true)) skinMap[key] = (skinMap[key] ?: 0) + 1
            }
        }
        val total = skinMap.values.sum().coerceAtLeast(1)
        // Màu đồng bộ với color palette của app
        val barColors = listOf("#4F6544", "#677559", "#59AE7B", "#95A192", "#AACFAA", "#3A5234")
        val skinEmojis = mapOf(
            "Da dầu" to "💧", "Da hỗn hợp" to "🌿", "Da khô" to "🌵",
            "Da thường" to "✨", "Da nhạy cảm" to "🌸", "Mọi loại da" to "🌍"
        )

        binding.llSkinChart.removeAllViews()
        // Header
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(16) }
        }
        val hLabel = TextView(requireContext()).apply {
            text = "Loại da"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#95A192"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val hCount = TextView(requireContext()).apply {
            text = "SP tương thích"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#95A192"))
        }
        header.addView(hLabel); header.addView(hCount)
        binding.llSkinChart.addView(header)

        skinMap.entries.sortedByDescending { it.value }.forEachIndexed { idx, (label, count) ->
            val pct = count.toFloat() / total
            val emoji = skinEmojis[label] ?: "•"
            val color = barColors[idx % barColors.size]

            val rowWrap = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dpToPx(14) }
            }
            // Top row: label + count
            val topRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dpToPx(6) }
            }
            val labelTv = TextView(requireContext()).apply {
                text = "$emoji  $label"
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#3E4D44"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val pctTv = TextView(requireContext()).apply {
                text = "$count SP  (${"%.0f".format(pct * 100)}%)"
                textSize = 11f
                setTextColor(Color.parseColor(color))
                setTypeface(null, Typeface.BOLD)
            }
            topRow.addView(labelTv); topRow.addView(pctTv)

            // Bar track
            val track = android.widget.FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(8))
                background = android.graphics.drawable.GradientDrawable().also {
                    it.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    it.cornerRadius = dpToPx(4).toFloat()
                    it.setColor(Color.parseColor("#EEF0E8"))
                }
            }
            val fill = View(requireContext()).apply {
                val maxWidth = (resources.displayMetrics.widthPixels * 0.72f).toInt()
                val barWidth = (maxWidth * pct).toInt().coerceAtLeast(dpToPx(6))
                layoutParams = android.widget.FrameLayout.LayoutParams(barWidth, android.widget.FrameLayout.LayoutParams.MATCH_PARENT)
                background = android.graphics.drawable.GradientDrawable().also {
                    it.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    it.cornerRadius = dpToPx(4).toFloat()
                    it.setColor(Color.parseColor(color))
                }
            }
            track.addView(fill)
            rowWrap.addView(topRow)
            rowWrap.addView(track)
            binding.llSkinChart.addView(rowWrap)
        }
    }

    private fun buildFaqSection() {
        // Câu hỏi thực tế dựa trên ghi chú khách hàng (users.json) và sản phẩm thực (products.json)
        data class FaqItem(val tag: String, val tagColor: String, val question: String)
        val faqs = listOf(
            FaqItem("Da dầu mụn", "#4F6544",
                "Da dầu có mụn ẩn nên dùng Combo chăm sóc da mụn bí đao toàn diện hay Tinh chất bí đao 70ml đơn lẻ?"),
            FaqItem("Da nhạy cảm", "#677559",
                "Tinh chất bí đao có chứa Tràm trà và BHA 0.8%, da nhạy cảm dùng có bị kích ứng không?"),
            FaqItem("Da xỉn màu", "#59AE7B",
                "Serum nghệ Hưng Yên curcumin cao gấp 3 lần có để lại màu vàng trên da không?"),
            FaqItem("Quà tặng", "#95A192",
                "GiftBox \"Rootie đã có mặt tại Pháp\" có thể làm quà sinh nhật không? Gồm những sản phẩm gì?"),
            FaqItem("Chống nắng", "#E65100",
                "Sữa chống nắng bí đao Cocoon SPF có dùng được cho da khô hay chỉ phù hợp da dầu?"),
            FaqItem("Routine", "#0D47A1",
                "Nước sen Hậu Giang (toner) dùng bước nào trong routine sáng và tối?")
        )
        binding.llFaq.removeAllViews()
        faqs.forEach { faq ->
            val card = androidx.cardview.widget.CardView(requireContext()).apply {
                radius = dpToPx(14).toFloat()
                cardElevation = dpToPx(2).toFloat()
                setCardBackgroundColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dpToPx(10) }
            }
            val inner = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
            }
            val tagView = TextView(requireContext()).apply {
                text = faq.tag
                textSize = 10f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = android.graphics.drawable.GradientDrawable().also {
                    it.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    it.cornerRadius = dpToPx(6).toFloat()
                    it.setColor(Color.parseColor(faq.tagColor))
                }
                setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dpToPx(8) }
            }
            val questionView = TextView(requireContext()).apply {
                text = "💬  ${faq.question}"
                textSize = 13f
                setTextColor(Color.parseColor("#3E4D44"))
                setLineSpacing(0f, 1.4f)
                maxLines = 3
            }
            inner.addView(tagView)
            inner.addView(questionView)
            card.addView(inner)
            binding.llFaq.addView(card)
        }
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private fun parseProductsJson(): List<JSONObject> {
        return try {
            val jsonStr = requireContext().assets.open("products.json")
                .bufferedReader().use { it.readText() }
            val arr = JSONObject(jsonStr).getJSONArray("products")
            (0 until arr.length()).map { arr.getJSONObject(it) }
        } catch (e: Exception) { emptyList() }
    }

    private fun buildRankRow(rank: Int, name: String, subtitle: String, accentColor: String): View {
        // Màu nền của badge theo rank
        val badgeBgColor = when (rank) {
            1 -> "#FFF3CD"; 2 -> "#E5E8DA"; 3 -> "#F2F4EB"; else -> "#F5F5F5"
        }
        val rankEmoji = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#$rank" }

        // Divider trên cùng (trừ item đầu)
        val wrapper = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        if (rank > 1) {
            val divider = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).also { it.bottomMargin = dpToPx(12); it.topMargin = dpToPx(0) }
                setBackgroundColor(Color.parseColor("#F0F2EC"))
            }
            wrapper.addView(divider)
        }

        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(12) }
        }

        val rankBadge = TextView(requireContext()).apply {
            text = rankEmoji
            textSize = if (rank <= 3) 20f else 11f
            if (rank > 3) setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(accentColor))
            if (rank > 3) {
                background = android.graphics.drawable.GradientDrawable().also {
                    it.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    it.cornerRadius = dpToPx(8).toFloat()
                    it.setColor(Color.parseColor(badgeBgColor))
                }
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginEnd = dpToPx(12) }
        }

        val info = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val nameView = TextView(requireContext()).apply {
            text = if (name.length > 55) name.take(55) + "…" else name
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#3E4D44"))
            maxLines = 2
        }
        val subView = TextView(requireContext()).apply {
            text = subtitle
            textSize = 12f
            setTextColor(Color.parseColor(accentColor))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = dpToPx(3) }
        }
        info.addView(nameView)
        info.addView(subView)

        row.addView(rankBadge)
        row.addView(info)
        wrapper.addView(row)
        return wrapper
    }

    private fun buildInfoText(text: String, color: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.parseColor(color))
            setLineSpacing(0f, 1.4f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(8) }
        }
    }

    private fun formatShort(value: Long): String {
        return when {
            value >= 1_000_000_000 -> "${"%.1f".format(value / 1_000_000_000.0)}tỷ"
            value >= 1_000_000     -> "${"%.0f".format(value / 1_000_000.0)}tr"
            else                   -> vndFormat.format(value)
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
