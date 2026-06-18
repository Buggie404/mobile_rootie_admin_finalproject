package com.veganbeauty.admin.features.booking.list

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.admin.data.local.entities.BookingEntity
import com.veganbeauty.admin.databinding.BookingItemBinding

class BookingAdapter(
    private val isAdmin: Boolean,
    private val onActionClick: (BookingEntity, String) -> Unit,
    private val onCancelClick: (BookingEntity) -> Unit
) : ListAdapter<BookingEntity, BookingAdapter.BookingViewHolder>(BookingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = BookingItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookingViewHolder(private val binding: BookingItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: BookingEntity) {
            binding.tvBookingService.text = booking.serviceName
            binding.tvBookingTime.text = booking.time
            
            // Date format: e.g. "Thứ 2, ngày 20 Tháng 5"
            val day = if (booking.dayOfWeek.isNotEmpty()) "${booking.dayOfWeek}, " else ""
            val monthClean = booking.monthDisplay.replace("\n", " ")
            binding.tvBookingDate.text = "${day}ngày ${booking.dateDisplay} $monthClean"
            binding.tvBookingStore.text = "${booking.storeName} - ${booking.storeAddress}"
            
            binding.tvCustomerName.text = booking.userName
            binding.tvCustomerPhoneEmail.text = "SĐT: ${booking.userPhone} | Email: ${booking.userEmail}"

            if (booking.note.isNotEmpty()) {
                binding.tvBookingNote.visibility = View.VISIBLE
                binding.tvBookingNote.text = "Ghi chú: ${booking.note}"
            } else {
                binding.tvBookingNote.visibility = View.GONE
            }

            // Cancel Reason setup
            val isCancelled = booking.status.equals("Đã huỷ", ignoreCase = true) || 
                              booking.status.equals("Đã hủy", ignoreCase = true) || 
                              booking.status.equals("cancelled", ignoreCase = true)
            
            if (isCancelled && booking.cancelReason.isNotEmpty()) {
                binding.llCancelReason.visibility = View.VISIBLE
                binding.tvCancelReason.text = booking.cancelReason
            } else {
                binding.llCancelReason.visibility = View.GONE
            }

            // Setup Badge Status
            setupStatusBadge(booking.status)

            // Setup Actions layout
            setupActions(booking)
        }

        private fun setupStatusBadge(status: String) {
            val isPending = status.equals("Chờ xác nhận", ignoreCase = true) || status.equals("pending", ignoreCase = true)
            val isUpcoming = status.equals("Sắp diễn ra", ignoreCase = true) || 
                             status.equals("confirmed", ignoreCase = true) || 
                             status.equals("upcoming", ignoreCase = true)
            val isCompleted = status.equals("Đã hoàn thành", ignoreCase = true) || status.equals("completed", ignoreCase = true)
            val isCancelled = status.equals("Đã huỷ", ignoreCase = true) || 
                              status.equals("Đã hủy", ignoreCase = true) || 
                              status.equals("cancelled", ignoreCase = true)

            val (badgeText, textColor, bgColor) = when {
                isPending -> Triple("Chờ xác nhận", "#FF9F1C", "#FFEED6") // Orange
                isUpcoming -> Triple("Sắp diễn ra", "#3498DB", "#E8F4F8") // Blue
                isCompleted -> Triple("Hoàn tất", "#2ECC71", "#E8F8F5") // Green
                isCancelled -> Triple("Đã hủy", "#E74C3C", "#FADBD8") // Red
                else -> Triple(status, "#95A192", "#F2F4EB") // Muted
            }

            binding.tvStatusBadge.text = badgeText
            binding.tvStatusBadge.setTextColor(Color.parseColor(textColor))
            binding.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))
        }

        private fun setupActions(booking: BookingEntity) {
            if (isAdmin) {
                binding.llActions.visibility = View.GONE
                return
            }
            val isPending = booking.status.equals("Chờ xác nhận", ignoreCase = true) || booking.status.equals("pending", ignoreCase = true)
            val isUpcoming = booking.status.equals("Sắp diễn ra", ignoreCase = true) || 
                             booking.status.equals("confirmed", ignoreCase = true) || 
                             booking.status.equals("upcoming", ignoreCase = true)

            when {
                isPending -> {
                    binding.llActions.visibility = View.VISIBLE
                    binding.btnCancel.visibility = View.VISIBLE
                    binding.btnAction.visibility = View.VISIBLE
                    binding.btnAction.text = "Xác nhận"
                    binding.btnAction.setOnClickListener { onActionClick(booking, "Sắp diễn ra") }
                    binding.btnCancel.setOnClickListener { onCancelClick(booking) }
                }
                isUpcoming -> {
                    binding.llActions.visibility = View.VISIBLE
                    binding.btnCancel.visibility = View.VISIBLE
                    binding.btnAction.visibility = View.VISIBLE
                    binding.btnAction.text = "Hoàn tất"
                    binding.btnAction.setOnClickListener { onActionClick(booking, "Đã hoàn thành") }
                    binding.btnCancel.setOnClickListener { onCancelClick(booking) }
                }
                else -> {
                    // Completed or Cancelled
                    binding.llActions.visibility = View.GONE
                }
            }
        }
    }
}

class BookingDiffCallback : DiffUtil.ItemCallback<BookingEntity>() {
    override fun areItemsTheSame(oldItem: BookingEntity, newItem: BookingEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: BookingEntity, newItem: BookingEntity): Boolean {
        return oldItem == newItem
    }
}
