package com.veganbeauty.admin.core.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.veganbeauty.admin.MainActivity
import com.veganbeauty.admin.features.home.HomeMessageFragment

abstract class RootieAdminFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
}
