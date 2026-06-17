package com.veganbeauty.admin.features.customer

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.admin.R
import com.veganbeauty.admin.data.local.entities.CustomerEntity
import com.veganbeauty.admin.databinding.ItemCustomerBinding

class CustomerAdapter(
    private var items: List<CustomerEntity>,
    private val onDetailClick: (CustomerEntity) -> Unit,
    private val onNoteClick: (CustomerEntity) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCustomerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            txtName.text = item.name
            txtSpendingInfo.text = "Chi tiêu: ${formatSpending(item.spending)} • Cuối: ${item.lastActive}"

            // Set tier badge styling
            badgeTier.text = item.tier
            when (item.tier.lowercase()) {
                "vip" -> {
                    badgeTier.setTextColor(Color.parseColor("#4F6544"))
                    badgeTier.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E5E8DA"))
                }
                "vàng" -> {
                    badgeTier.setTextColor(Color.parseColor("#FFFFFF"))
                    badgeTier.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#C69C2C"))
                }
                "bạc" -> {
                    badgeTier.setTextColor(Color.parseColor("#FFFFFF"))
                    badgeTier.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9E9E9E"))
                }
                else -> { // Thường / Normal
                    badgeTier.setTextColor(Color.parseColor("#616161"))
                    badgeTier.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F5F5F5"))
                }
            }

            // Load avatar
            if (!item.avatar.isNullOrEmpty()) {
                imgAvatar.load(item.avatar) {
                    crossfade(true)
                    placeholder(R.drawable.imv_avatar)
                    error(R.drawable.imv_avatar)
                }
            } else {
                imgAvatar.setImageResource(R.drawable.imv_avatar)
            }

            btnDetail.setOnClickListener { onDetailClick(item) }
            btnNote.setOnClickListener { onNoteClick(item) }
            root.setOnClickListener { onDetailClick(item) }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<CustomerEntity>) {
        items = newItems
        notifyDataSetChanged()
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
}
