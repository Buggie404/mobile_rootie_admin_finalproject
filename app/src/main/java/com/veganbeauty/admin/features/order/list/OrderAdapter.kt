package com.veganbeauty.admin.features.order.list

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
            binding.txtOrderTime.text = "${order.orderDate} - ${order.orderTime}"
            binding.txtCustomerName.text = order.shippingName.ifBlank { order.storeName }

            // Format total price
            val vncLocale = Locale.forLanguageTag("vi-VN")
            val nf = NumberFormat.getCurrencyInstance(vncLocale)
            binding.txtOrderTotal.text = nf.format(order.totalAmount)

            // Items count
            binding.txtItemsCount.text = "${order.items.size} sản phẩm"

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
                "Hoàn tất", "Hoàn thành" -> Pair("#2ECC71", "#E8F8F5") // Green
                "Đã hủy" -> Pair("#E74C3C", "#FADBD8") // Red
                else -> Pair("#95A192", "#F2F4EB") // Muted
            }

            binding.txtOrderStatusBadge.setTextColor(Color.parseColor(textColor))
            binding.txtOrderStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))
        }

        private fun setupActions(order: OrderEntity) {
            when (order.status) {
                "Chờ xử lý", "Chờ xác nhận" -> {
                    binding.layoutOrderActions.visibility = View.VISIBLE
                    binding.btnCancelOrder.visibility = View.VISIBLE
                    binding.btnApproveOrder.visibility = View.VISIBLE
                    binding.btnApproveOrder.text = "Xác nhận"
                    binding.btnApproveOrder.setOnClickListener { onActionClick(order, "Đang xử lý") }
                    binding.btnCancelOrder.setOnClickListener { onActionClick(order, "Đã hủy") }
                }
                "Đang xử lý", "Đang chuẩn bị" -> {
                    binding.layoutOrderActions.visibility = View.VISIBLE
                    binding.btnCancelOrder.visibility = View.VISIBLE
                    binding.btnApproveOrder.visibility = View.VISIBLE
                    binding.btnApproveOrder.text = "Giao hàng"
                    binding.btnApproveOrder.setOnClickListener { onActionClick(order, "Đang giao") }
                    binding.btnCancelOrder.setOnClickListener { onActionClick(order, "Đã hủy") }
                }
                "Đang giao" -> {
                    binding.layoutOrderActions.visibility = View.VISIBLE
                    binding.btnCancelOrder.visibility = View.GONE
                    binding.btnApproveOrder.visibility = View.VISIBLE
                    binding.btnApproveOrder.text = "Hoàn tất"
                    binding.btnApproveOrder.setOnClickListener { onActionClick(order, "Hoàn tất") }
                }
                else -> {
                    // "Hoàn tất" or "Đã hủy"
                    binding.layoutOrderActions.visibility = View.GONE
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
