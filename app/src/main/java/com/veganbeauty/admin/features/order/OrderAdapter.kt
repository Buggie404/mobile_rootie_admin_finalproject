package com.veganbeauty.admin.features.order

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.admin.R
import com.veganbeauty.admin.data.local.entities.OrderEntity
import com.veganbeauty.admin.databinding.OrderItemBinding
import java.text.NumberFormat
import java.util.Locale

class OrderAdapter(
    private var items: List<OrderEntity>,
    private var selectedOrderIds: Set<String> = emptySet(),
    private val onOrderSelectionToggled: (OrderEntity) -> Unit,
    private val onCancelClick: (OrderEntity) -> Unit,
    private val onApproveClick: (OrderEntity) -> Unit,
    private val onItemClick: (OrderEntity) -> Unit
) : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    class ViewHolder(val binding: OrderItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = OrderItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            // Bind Order Code
            txtOrderCode.text = item.orderId

            // Bind Customer Name
            txtCustomerName.text = if (item.shippingName.isNotBlank()) item.shippingName else "Khách hàng Rootie"

            // Bind Date/Time
            txtOrderTime.text = "${item.orderDate}, ${item.orderTime}"

            // Bind Items count
            val totalQty = item.items.sumOf { it.quantity }
            txtItemsCount.text = "$totalQty sản phẩm"

            // Bind Total price formatted
            txtOrderTotal.text = formatCurrency(item.totalAmount)

            // Bind Status Badge
            val statusClean = item.status.trim()
            txtOrderStatusBadge.text = statusClean
            setupStatusBadgeStyle(txtOrderStatusBadge, statusClean)

            // Bind Checkbox
            if (isSelectionAllowed) {
                imgSelectOrder.visibility = View.VISIBLE
                val isSelected = selectedOrderIds.contains(item.orderId)
                imgSelectOrder.setImageResource(
                    if (isSelected) R.drawable.ic_checkbox_checked
                    else R.drawable.ic_radio_primary_unchecked
                )
                imgSelectOrder.setOnClickListener {
                    onOrderSelectionToggled(item)
                }
            } else {
                imgSelectOrder.visibility = View.GONE
                imgSelectOrder.setOnClickListener(null)
            }

            // Setup buttons contextual texts and visibility based on status
            setupActionButtons(holder.binding, item)

            // Setup click listeners
            btnCancelOrder.setOnClickListener {
                onCancelClick(item)
            }

            btnApproveOrder.setOnClickListener {
                onApproveClick(item)
            }

            root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    private var isSelectionAllowed: Boolean = true

    fun updateData(newItems: List<OrderEntity>, newSelections: Set<String>, selectionAllowed: Boolean) {
        items = newItems
        selectedOrderIds = newSelections
        isSelectionAllowed = selectionAllowed
        notifyDataSetChanged()
    }

    private fun formatCurrency(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
        return "${formatter.format(amount)}đ"
    }

    private fun setupStatusBadgeStyle(badgeView: android.widget.TextView, status: String) {
        val normalizedStatus = status.lowercase()
        val textColor: Int
        val bgColor: Int

        when {
            normalizedStatus.contains("chờ") || normalizedStatus.contains("xử lý") -> {
                // Pending / Processing
                textColor = Color.parseColor("#4F6544")
                bgColor = Color.parseColor("#E5E8DA")
            }
            normalizedStatus.contains("chuẩn bị") -> {
                // Preparing
                textColor = Color.parseColor("#B0882E")
                bgColor = Color.parseColor("#FFF2D4")
            }
            normalizedStatus.contains("giao") -> {
                // Delivering
                textColor = Color.parseColor("#2B74B3")
                bgColor = Color.parseColor("#D4E9FF")
            }
            normalizedStatus.contains("hoàn") || normalizedStatus.contains("tất") || normalizedStatus.contains("thành") -> {
                // Completed
                textColor = Color.parseColor("#1B8756")
                bgColor = Color.parseColor("#D4FFE8")
            }
            normalizedStatus.contains("hủy") -> {
                // Cancelled
                textColor = Color.parseColor("#C92F2F")
                bgColor = Color.parseColor("#FFD4D4")
            }
            else -> {
                textColor = Color.parseColor("#677559")
                bgColor = Color.parseColor("#F2F3EC")
            }
        }

        badgeView.setTextColor(textColor)
        badgeView.backgroundTintList = ColorStateList.valueOf(bgColor)
    }

    private fun setupActionButtons(binding: OrderItemBinding, item: OrderEntity) {
        val normalizedStatus = item.status.trim().lowercase()

        when {
            normalizedStatus.contains("chờ xử lý") || normalizedStatus.contains("chờ xác nhận") -> {
                binding.layoutOrderActions.visibility = View.VISIBLE
                binding.btnCancelOrder.visibility = View.VISIBLE
                binding.btnCancelOrder.text = "Hủy đơn"
                binding.btnApproveOrder.visibility = View.VISIBLE
                binding.btnApproveOrder.text = "Xác nhận"
            }
            normalizedStatus.contains("đang xử lý") || normalizedStatus.contains("đang chuẩn bị") -> {
                binding.layoutOrderActions.visibility = View.VISIBLE
                binding.btnCancelOrder.visibility = View.VISIBLE
                binding.btnCancelOrder.text = "Hủy đơn"
                binding.btnApproveOrder.visibility = View.VISIBLE
                binding.btnApproveOrder.text = "Giao hàng"
            }
            normalizedStatus.contains("đang giao") -> {
                binding.layoutOrderActions.visibility = View.VISIBLE
                binding.btnCancelOrder.visibility = View.VISIBLE
                binding.btnCancelOrder.text = "Thất bại"
                binding.btnApproveOrder.visibility = View.VISIBLE
                binding.btnApproveOrder.text = "Hoàn tất"
            }
            else -> {
                // Completed / Cancelled -> hide action buttons
                binding.layoutOrderActions.visibility = View.GONE
            }
        }
    }
}
