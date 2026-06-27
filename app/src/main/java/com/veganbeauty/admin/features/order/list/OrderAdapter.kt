package com.veganbeauty.admin.features.order.list

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.admin.R
import com.veganbeauty.admin.data.local.entities.OrderEntity
import com.veganbeauty.admin.databinding.OrderItemBinding
import java.text.NumberFormat
import java.util.Locale

class OrderAdapter(
    private val onActionClick: (OrderEntity, String) -> Unit
) : ListAdapter<OrderEntity, OrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = OrderItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(private val binding: OrderItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: OrderEntity) {
            binding.txtOrderCode.text = order.orderId
            binding.txtOrderTime.text = String.format(Locale.getDefault(), "%s - %s", order.orderDate, order.orderTime)
            binding.txtCustomerName.text = order.shippingName.ifBlank { "Khách hàng Rootie" }

            // Format total price
            val vncLocale = Locale.forLanguageTag("vi-VN")
            val nf = NumberFormat.getCurrencyInstance(vncLocale)
            binding.txtOrderTotal.text = nf.format(order.totalAmount)

            // Bind items count
            val totalQty = order.items.sumOf { it.quantity }
            binding.txtItemsCount.text = String.format(Locale.getDefault(), "%d sản phẩm", totalQty)

            // Status Badge setup
            setupStatusBadge(order.status)

            // Actions layout setup
            setupActions(order)
        }

        private fun setupStatusBadge(status: String) {
            binding.txtOrderStatusBadge.text = status
            
            val (textColor, bgColor) = when (status) {
                "Chờ xử lý", "Chờ xác nhận" -> Pair("#FF9F1C", "#FFEED6") // Orange
                "Đang xử lý", "Đang chuẩn bị" -> Pair("#3498DB", "#E8F4F8") // Blue
                "Đang giao" -> Pair("#9B59B6", "#F5EEF8") // Purple
                "Hoàn tất" -> Pair("#2ECC71", "#E8F8F5") // Green
                "Đã hủy" -> Pair("#E74C3C", "#FADBD8") // Red
                else -> Pair("#95A192", "#F2F4EB") // Muted
            }

            binding.txtOrderStatusBadge.setTextColor(Color.parseColor(textColor))
            binding.txtOrderStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))
        }

        private fun setupActions(order: OrderEntity) {
            val normalizedStatus = order.status.lowercase()
            if (normalizedStatus.contains("hoàn tất") || normalizedStatus.contains("đã hủy")) {
                binding.layoutOrderActions.visibility = View.GONE
                return
            }

            binding.layoutOrderActions.visibility = View.VISIBLE
            binding.btnCancelOrder.visibility = View.VISIBLE
            binding.btnApproveOrder.visibility = View.VISIBLE

            when {
                normalizedStatus.contains("chờ xử lý") || normalizedStatus.contains("chờ xác nhận") -> {
                    binding.btnApproveOrder.text = "Xác nhận"
                    binding.btnApproveOrder.setOnClickListener { onActionClick(order, "Đang chuẩn bị") }
                    binding.btnCancelOrder.setOnClickListener { onActionClick(order, "Đã hủy") }
                }
                normalizedStatus.contains("đang xử lý") || normalizedStatus.contains("đang chuẩn bị") -> {
                    binding.btnApproveOrder.text = "Giao hàng"
                    binding.btnApproveOrder.setOnClickListener { onActionClick(order, "Đang giao") }
                    binding.btnCancelOrder.setOnClickListener { onActionClick(order, "Đã hủy") }
                }
                normalizedStatus.contains("đang giao") -> {
                    binding.btnCancelOrder.visibility = View.GONE // Thường không hủy khi đang giao trong view này
                    binding.btnApproveOrder.text = "Hoàn tất"
                    binding.btnApproveOrder.setOnClickListener { onActionClick(order, "Hoàn tất") }
                }
            }
        }
    }
}

class OrderDiffCallback : DiffUtil.ItemCallback<OrderEntity>() {
    override fun areItemsTheSame(oldItem: OrderEntity, newItem: OrderEntity): Boolean {
        return oldItem.orderId == newItem.orderId
    }

    override fun areContentsTheSame(oldItem: OrderEntity, newItem: OrderEntity): Boolean {
        return oldItem == newItem
    }
}
