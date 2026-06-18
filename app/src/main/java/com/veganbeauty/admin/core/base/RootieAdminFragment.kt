package com.veganbeauty.admin.core.base

import android.os.Bundle
import android.view.View
import android.widget.TextView
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

            // Find badge inside messageBtn
            val badgeView = messageBtn.findViewById<TextView>(com.veganbeauty.admin.R.id.message_badge)
                ?: messageBtn.findViewById<TextView>(com.veganbeauty.admin.R.id.home_header_message_badge)

            (activity as? MainActivity)?.let { mainAct ->
                badgeView?.let { badge ->
                    if (mainAct.lastUnreadCount > 0) {
                        badge.text = mainAct.lastUnreadCount.toString()
                        badge.visibility = View.VISIBLE
                    } else {
                        badge.visibility = View.GONE
                    }
                }
            }
        } else {
            messageBtn.visibility = View.GONE
        }
    }
}
