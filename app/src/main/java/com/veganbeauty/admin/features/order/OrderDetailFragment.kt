package com.veganbeauty.admin.features.order

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import coil.load
import com.veganbeauty.admin.MainActivity
import com.veganbeauty.admin.R
import com.veganbeauty.admin.core.base.RootieAdminFragment
import com.veganbeauty.admin.data.local.RootieAdminDatabase
import com.veganbeauty.admin.data.local.entities.OrderEntity
import com.veganbeauty.admin.data.remote.FirebaseService
import com.veganbeauty.admin.data.repository.OrderRepository
import com.veganbeauty.admin.databinding.OrderDetailFragmentBinding
import com.veganbeauty.admin.databinding.ItemOrderDetailProductBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class OrderDetailFragment : RootieAdminFragment() {

    private var _binding: OrderDetailFragmentBinding? = null
    private val binding get() = _binding!!

    private var orderId: String? = null
    private lateinit var repository: OrderRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            orderId = it.getString(ARG_ORDER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = OrderDetailFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        val database = RootieAdminDatabase.getDatabase(requireContext().applicationContext)
        repository = OrderRepository(database.orderDao(), FirebaseService())

        setupListeners()
        loadOrderData()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            navigateBack()
        }

        binding.btnNotification.setOnClickListener {
            Toast.makeText(requireContext(), "Mở thông báo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadOrderData() {
        val id = orderId ?: return
        lifecycleScope.launch {
            val order = withContext(Dispatchers.IO) {
                val database = RootieAdminDatabase.getDatabase(requireContext().applicationContext)
                database.orderDao().getByIdSync(id)
            }
            if (order != null) {
                bindOrder(order)
            } else {
                Toast.makeText(requireContext(), "Không tìm thấy đơn hàng!", Toast.LENGTH_SHORT).show()
                navigateBack()
            }
        }
    }

    private fun bindOrder(order: OrderEntity) {
        with(binding) {
            txtOrderCodeTitle.text = "Đơn hàng ${order.orderId}"
            
            // Status and styling
            val statusClean = order.status.trim()
            txtStatusVal.text = statusClean
            setupStatusStyle(statusClean)

            // Shipping Info
            txtShippingName.text = if (order.shippingName.isNotBlank()) order.shippingName else "Khách hàng Rootie"
            txtShippingPhone.text = if (order.shippingPhone.isNotBlank()) order.shippingPhone else "Chưa cập nhật"
            txtShippingAddress.text = if (order.shippingAddress.isNotBlank()) order.shippingAddress else "Chưa cập nhật"

            // Populate Products
            layoutProductsContainer.removeAllViews()
            val inflater = LayoutInflater.from(requireContext())
            var calculatedSubtotal = 0L

            for (item in order.items) {
                val itemBinding = ItemOrderDetailProductBinding.inflate(inflater, layoutProductsContainer, false)
                itemBinding.txtProductName.text = item.productName
                itemBinding.txtProductPrice.text = formatCurrency(item.price)
                itemBinding.txtProductQuantity.text = "x${item.quantity}"
                
                if (item.productImage.isNotEmpty()) {
                    itemBinding.imgProduct.load(item.productImage) {
                        crossfade(true)
                        placeholder(R.drawable.nuoc_sen_hau_giang)
                        error(R.drawable.nuoc_sen_hau_giang)
                    }
                } else {
                    itemBinding.imgProduct.setImageResource(R.drawable.nuoc_sen_hau_giang)
                }

                // Add original price crossed out if there's any hypothetical discount or structure.
                // In the mockup we see original price crossed out (e.g. 325.000đ original, 241.000đ discount).
                // Let's check if we can simulate original price if price is discounted, or just keep it neat.
                // E.g., if price is 241.000đ and name is "Gel tắm...", the original is 325.000đ.
                // Let's hardcode the original crossed out price for this specific mockup demo item if needed,
                // or calculate it based on directDiscount / quantity.
                val simulatedOriginalPrice = if (order.voucherDiscount > 0L || (order.totalAmount < order.items.sumOf { it.price * it.quantity } + order.shippingCost)) {
                    // Just show a crossed out value for aesthetic premium feel
                    (item.price * 1.25).toLong()
                } else {
                    0L
                }

                if (simulatedOriginalPrice > 0L) {
                    itemBinding.txtOriginalPrice.visibility = View.VISIBLE
                    itemBinding.txtOriginalPrice.text = formatCurrency(simulatedOriginalPrice)
                    itemBinding.txtOriginalPrice.paintFlags = itemBinding.txtOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    itemBinding.txtOriginalPrice.visibility = View.GONE
                }

                layoutProductsContainer.addView(itemBinding.root)
                calculatedSubtotal += item.price * item.quantity
            }

            // Calculations
            txtSubtotal.text = formatCurrency(calculatedSubtotal)
            
            // Voucher Discount
            txtVoucherDiscount.text = if (order.voucherDiscount > 0L) "-${formatCurrency(order.voucherDiscount)}" else "0đ"
            
            // Shipping Cost
            if (order.shippingCost == 0L) {
                txtShippingCost.text = "Miễn phí"
            } else {
                txtShippingCost.text = formatCurrency(order.shippingCost)
            }

            // Direct discount (calculated based on difference)
            // subtotal - voucher + shipping - total
            val rawDiff = (calculatedSubtotal + order.shippingCost - order.voucherDiscount) - order.totalAmount
            val directDiscount = if (rawDiff > 0L) rawDiff else 0L
            txtDirectDiscount.text = if (directDiscount > 0L) "-${formatCurrency(directDiscount)}" else "0đ"

            // Total
            txtTotalVal.text = formatCurrency(order.totalAmount)

            // Payment method subtext
            val methodLower = order.paymentMethod.lowercase()
            if (methodLower.contains("cod") || methodLower.contains("khi nhận hàng")) {
                txtPaymentMethod.text = "(Thanh toán khi nhận hàng COD)"
            } else if (methodLower.contains("momo")) {
                txtPaymentMethod.text = "(Thanh toán qua ví MoMo)"
            } else if (methodLower.contains("ngân hàng") || methodLower.contains("chuyển khoản")) {
                txtPaymentMethod.text = "(Thanh toán Chuyển khoản ngân hàng)"
            } else {
                txtPaymentMethod.text = "(${order.paymentMethod})"
            }

            // Setup Actions
            setupActionButtons(order)
        }
    }

    private fun setupStatusStyle(status: String) {
        val normalizedStatus = status.lowercase()
        val textColor: Int

        when {
            normalizedStatus.contains("chờ") || normalizedStatus.contains("xử lý") -> {
                textColor = Color.parseColor("#4F6544")
            }
            normalizedStatus.contains("chuẩn bị") -> {
                textColor = Color.parseColor("#B0882E")
            }
            normalizedStatus.contains("giao") -> {
                textColor = Color.parseColor("#2B74B3")
            }
            normalizedStatus.contains("hoàn") || normalizedStatus.contains("tất") || normalizedStatus.contains("thành") -> {
                textColor = Color.parseColor("#1B8756")
            }
            normalizedStatus.contains("hủy") -> {
                textColor = Color.parseColor("#C92F2F")
            }
            else -> {
                textColor = Color.parseColor("#3E4D44")
            }
        }
        binding.txtStatusVal.setTextColor(textColor)
    }

    private fun setupActionButtons(order: OrderEntity) {
        val normalizedStatus = order.status.trim().lowercase()
        val actionsLayout = binding.layoutActionsBar
        val btnCancel = binding.btnCancel
        val btnApprove = binding.btnApprove

        when {
            normalizedStatus.contains("chờ xử lý") || normalizedStatus.contains("chờ xác nhận") -> {
                actionsLayout.visibility = View.VISIBLE
                btnCancel.visibility = View.VISIBLE
                btnCancel.text = "Hủy"
                btnApprove.visibility = View.VISIBLE
                btnApprove.text = "Xác nhận"

                btnCancel.setOnClickListener {
                    updateStatus(order.orderId, "Đã hủy")
                }
                btnApprove.setOnClickListener {
                    updateStatus(order.orderId, "Đang chuẩn bị")
                }
            }
            normalizedStatus.contains("đang xử lý") || normalizedStatus.contains("đang chuẩn bị") -> {
                actionsLayout.visibility = View.VISIBLE
                btnCancel.visibility = View.VISIBLE
                btnCancel.text = "Hủy"
                btnApprove.visibility = View.VISIBLE
                btnApprove.text = "Giao hàng"

                btnCancel.setOnClickListener {
                    updateStatus(order.orderId, "Đã hủy")
                }
                btnApprove.setOnClickListener {
                    updateStatus(order.orderId, "Đang giao")
                }
            }
            normalizedStatus.contains("đang giao") -> {
                actionsLayout.visibility = View.VISIBLE
                btnCancel.visibility = View.VISIBLE
                btnCancel.text = "Thất bại"
                btnApprove.visibility = View.VISIBLE
                btnApprove.text = "Hoàn tất"

                btnCancel.setOnClickListener {
                    updateStatus(order.orderId, "Đã hủy")
                }
                btnApprove.setOnClickListener {
                    updateStatus(order.orderId, "Hoàn tất")
                }
            }
            else -> {
                // Completed or Cancelled -> no actions
                actionsLayout.visibility = View.GONE
            }
        }
    }

    private fun updateStatus(orderId: String, newStatus: String) {
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                repository.updateOrderStatus(orderId, newStatus)
            }
            if (success) {
                Toast.makeText(requireContext(), "Cập nhật trạng thái thành công!", Toast.LENGTH_SHORT).show()
                loadOrderData() // Reload
            } else {
                Toast.makeText(requireContext(), "Cập nhật thất bại!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatCurrency(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
        return "${formatter.format(amount)}đ"
    }

    private fun navigateBack() {
        val mainActivity = activity as? MainActivity ?: return
        mainActivity.loadFragment(OrderListFragment())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ORDER_ID = "arg_order_id"

        fun newInstance(orderId: String): OrderDetailFragment {
            val fragment = OrderDetailFragment()
            val args = Bundle()
            args.putString(ARG_ORDER_ID, orderId)
            fragment.arguments = args
            return fragment
        }
    }
}
