package com.veganbeauty.admin.features.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.admin.R
import com.veganbeauty.admin.databinding.ItemRecentActivityBinding

data class RecentActivity(
    val title: String,
    val orderId: String,
    val timeAgo: String,
    val price: String,
    val status: String,
    val imageRes: Int? = null,
    val imageUrl: String? = null
)

class RecentActivityAdapter(
    private val items: List<RecentActivity>,
    private val onItemClick: (RecentActivity) -> Unit
) : RecyclerView.Adapter<RecentActivityAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemRecentActivityBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentActivityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            txtTitle.text = item.title
            txtSubtitle.text = "Đơn hàng #${item.orderId} • ${item.timeAgo}"
            txtPrice.text = item.price
            txtStatus.text = item.status

            if (item.imageRes != null) {
                imvProduct.load(item.imageRes) {
                    crossfade(true)
                    placeholder(R.color.gray_light)
                    error(R.color.gray_light)
                }
            } else if (!item.imageUrl.isNullOrEmpty()) {
                imvProduct.load(item.imageUrl) {
                    crossfade(true)
                    placeholder(R.color.gray_light)
                    error(R.color.gray_light)
                }
            } else {
                imvProduct.setImageResource(R.color.gray_light)
            }

            root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun getItemCount(): Int = items.size
}
