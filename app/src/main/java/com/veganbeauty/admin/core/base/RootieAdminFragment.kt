package com.veganbeauty.admin.core.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.veganbeauty.admin.MainActivity
import com.veganbeauty.admin.features.home.HomeMessageFragment
import com.veganbeauty.admin.core.utils.KeyboardUtils

abstract class RootieAdminFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        KeyboardUtils.setupKeyboardAutoHiding(view, activity)
        setupUI(view)
        observeViewModel()
    }

    abstract fun setupUI(view: View)

    open fun observeViewModel() {
        // Option to observe common ViewModel data
    }

    fun setupHeaderMessageButton(messageBtn: View?) {
        if (messageBtn == null) return
        val context = requireContext()
        val isAdmin = UserSession.getRole(context) == "admin"
        if (isAdmin) {
            messageBtn.visibility = View.VISIBLE
            messageBtn.setOnClickListener {
                val mainActivity = activity as? MainActivity
                mainActivity?.loadFragment(HomeMessageFragment())
            }
        } else {
            messageBtn.visibility = View.GONE
        }
    }

    fun setupHeaderNotification(notificationBtn: View?, notificationBadge: android.widget.TextView?) {
        if (notificationBtn == null) return
        
        notificationBtn.setOnClickListener { view ->
            val mainActivity = activity as? MainActivity
            if (mainActivity != null) {
                val popup = androidx.appcompat.widget.PopupMenu(view.context, view)
                val bookingsCount = mainActivity.pendingBookingsCount.value ?: 0
                val ordersCount = mainActivity.pendingOrdersCount.value ?: 0
                val chatsCount = mainActivity.unreadChatsCount.value ?: 0
                
                popup.menu.add(0, 1, 0, "Lịch hẹn chờ xác nhận ($bookingsCount)")
                popup.menu.add(0, 2, 0, "Đơn hàng chờ xử lý ($ordersCount)")
                popup.menu.add(0, 3, 0, "Tin nhắn chưa đọc ($chatsCount)")
                
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        1 -> {
                            mainActivity.loadFragment(com.veganbeauty.admin.features.booking.list.BookingListFragment())
                            true
                        }
                        2 -> {
                            mainActivity.loadFragment(com.veganbeauty.admin.features.order.list.OrderListFragment())
                            true
                        }
                        3 -> {
                            mainActivity.loadFragment(com.veganbeauty.admin.features.home.HomeMessageFragment())
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
        
        val mainActivity = activity as? MainActivity
        if (mainActivity != null && notificationBadge != null) {
            mainActivity.totalNotificationCount.observe(viewLifecycleOwner) { count ->
                if (count > 0) {
                    notificationBadge.visibility = View.VISIBLE
                    notificationBadge.text = count.toString()
                } else {
                    notificationBadge.visibility = View.GONE
                }
            }
        }
    }
}
