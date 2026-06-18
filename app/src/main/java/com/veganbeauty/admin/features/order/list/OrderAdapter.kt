package com.veganbeauty.admin.features.order.list

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
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
            binding.tvOrderId.text = order.orderId
            binding.tvOrderDateTime.text = "${order.orderDate} - ${order.orderTime}"
            binding.tvOrderStore.text = order.storeName

            // Format total price
            val vncLocale = Locale("vi", "VN")
            val nf = NumberFormat.getCurrencyInstance(vncLocale)
            binding.tvTotalPrice.text = nf.format(order.totalAmount)

            // Bind first product item details if exists
            val firstItem = order.items.firstOrNull()
            if (firstItem != null) {
                binding.tvProductName.text = firstItem.productName
                binding.tvProductQtyPrice.text = "Số lượng: ${firstItem.quantity}  x  ${nf.format(firstItem.price)}"
                
                if (firstItem.productImage.isNotEmpty()) {
                    val resourceId = binding.imgProduct.context.resources.getIdentifier(
                        firstItem.productImage,
                        "drawable",
                        binding.imgProduct.context.packageName
                    )
                    if (resourceId != 0) {
                        binding.imgProduct.load(resourceId) {
                            placeholder(R.drawable.nuoc_sen_hau_giang)
                            error(R.drawable.nuoc_sen_hau_giang)
                        }
                    } else {
                        binding.imgProduct.load(firstItem.productImage) {
                            placeholder(R.drawable.nuoc_sen_hau_giang)
                            error(R.drawable.nuoc_sen_hau_giang)
                        }
                    }
                } else {
                    binding.imgProduct.setImageResource(R.drawable.nuoc_sen_hau_giang)
                }
            }

            // More products indicator
            if (order.items.size > 1) {
                binding.tvMoreItems.visibility = View.VISIBLE
                binding.tvMoreItems.text = "+ ${order.items.size - 1} sản phẩm khác"
            } else {
                binding.tvMoreItems.visibility = View.GONE
            }

            // Status Badge setup
            setupStatusBadge(order.status)

            // Actions layout setup
            setupActions(order)
        }

        private fun setupStatusBadge(status: String) {
            binding.tvStatusBadge.text = status
            
            val (textColor, bgColor) = when (status) {
                "Chờ xử lý" -> Pair("#FF9F1C", "#FFEED6") // Orange
                "Đang xử lý" -> Pair("#3498DB", "#E8F4F8") // Blue
                "Đang giao" -> Pair("#9B59B6", "#F5EEF8") // Purple
                "Hoàn tất" -> Pair("#2ECC71", "#E8F8F5") // Green
                "Đã hủy" -> Pair("#E74C3C", "#FADBD8") // Red
                else -> Pair("#95A192", "#F2F4EB") // Muted
            }

            binding.tvStatusBadge.setTextColor(Color.parseColor(textColor))
            binding.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))
        }

        private fun setupActions(order: OrderEntity) {
            when (order.status) {
                "Chờ xử lý" -> {
                    binding.llActions.visibility = View.VISIBLE
                    binding.btnCancel.visibility = View.VISIBLE
                    binding.btnAction.visibility = View.VISIBLE
                    binding.btnAction.text = "Xác nhận"
                    binding.btnAction.setOnClickListener { onActionClick(order, "Đang xử lý") }
                    binding.btnCancel.setOnClickListener { onActionClick(order, "Đã hủy") }
                }
                "Đang xử lý" -> {
                    binding.llActions.visibility = View.VISIBLE
                    binding.btnCancel.visibility = View.VISIBLE
                    binding.btnAction.visibility = View.VISIBLE
                    binding.btnAction.text = "Giao hàng"
                    binding.btnAction.setOnClickListener { onActionClick(order, "Đang giao") }
                    binding.btnCancel.setOnClickListener { onActionClick(order, "Đã hủy") }
                }
                "Đang giao" -> {
                    binding.llActions.visibility = View.VISIBLE
                    binding.btnCancel.visibility = View.GONE
                    binding.btnAction.visibility = View.VISIBLE
                    binding.btnAction.text = "Hoàn tất"
                    binding.btnAction.setOnClickListener { onActionClick(order, "Hoàn tất") }
                }
                else -> {
                    // "Hoàn tất" or "Đã hủy"
                    binding.llActions.visibility = View.GONE
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
