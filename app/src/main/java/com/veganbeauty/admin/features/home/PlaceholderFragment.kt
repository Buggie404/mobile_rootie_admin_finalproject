package com.veganbeauty.admin.features.home

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.veganbeauty.admin.core.base.RootieAdminFragment

class PlaceholderFragment : RootieAdminFragment() {

    companion object {
        fun newInstance(title: String): PlaceholderFragment {
            val fragment = PlaceholderFragment()
            val args = Bundle()
            args.putString("title", title)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val title = arguments?.getString("title") ?: "Placeholder Screen"
        val textView = TextView(context).apply {
            text = title
            textSize = 20f
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return textView
    }

    override fun setupUI(view: View) {
        // Simple placeholder
    }
}
