package com.veganbeauty.admin.features.home

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.veganbeauty.admin.MainActivity
import com.veganbeauty.admin.R
import com.veganbeauty.admin.features.product.list.ProductListFragment

object BottomNavHelper {

    private const val ACTIVE_COLOR = "#4F6544"
    private const val INACTIVE_COLOR = "#95A192"

    fun setup(
        activity: MainActivity,
        root: View,
        activeTabId: Int = R.id.nav_home,
        onTabSelected: (Int) -> Unit
    ) {
        val tabs = listOf(
            R.id.nav_home,
            R.id.nav_product,
            R.id.nav_customer,
            R.id.nav_order,
            R.id.nav_menu
        )

        val navRoot = activity.findViewById<View>(R.id.bottom_nav) ?: root

        tabs.forEach { viewId ->
            navRoot.findViewById<ViewGroup>(viewId)?.setOnClickListener {
                onTabSelected(viewId)
            }
        }

        highlightTab(navRoot, activeTabId)
    }

    fun highlightTab(root: View, activeTabId: Int) {
        val tabConfigs = listOf(
            Triple(R.id.nav_home, R.id.nav_home_pill, R.drawable.bg_nav_pill),
            Triple(R.id.nav_product, R.id.nav_product_pill, R.drawable.bg_nav_pill),
            Triple(R.id.nav_customer, R.id.nav_customer_pill, R.drawable.bg_nav_pill),
            Triple(R.id.nav_order, R.id.nav_order_pill, R.drawable.bg_nav_pill),
            Triple(R.id.nav_menu, R.id.nav_menu_pill, R.drawable.bg_nav_pill)
        )

        val iconDrawables = mapOf(
            R.id.nav_home to Pair(R.drawable.ic_home, R.drawable.ic_home_filled),
            R.id.nav_product to Pair(R.drawable.ic_bag, R.drawable.ic_bag_filled),
            R.id.nav_customer to Pair(R.drawable.ic_friends, R.drawable.ic_friends_filled),
            R.id.nav_order to Pair(R.drawable.ic_box, R.drawable.ic_box_filled),
            R.id.nav_menu to Pair(R.drawable.ic_grid, R.drawable.ic_grid)
        )

        tabConfigs.forEach { (tabId, pillId, bgRes) ->
            val tab = root.findViewById<ViewGroup>(tabId) ?: return@forEach
            val pill = tab.findViewById<ViewGroup>(pillId) ?: return@forEach
            val icon = pill.getChildAt(0) as? ImageView

            val isActive = tabId == activeTabId
            val (outlineRes, filledRes) = iconDrawables[tabId] ?: Pair(0, 0)

            if (isActive) {
                pill.setBackgroundResource(bgRes)
                if (filledRes != 0) {
                    icon?.setImageResource(filledRes)
                }
                icon?.setColorFilter(Color.parseColor(ACTIVE_COLOR))
            } else {
                pill.setBackgroundResource(android.R.color.transparent)
                if (outlineRes != 0) {
                    icon?.setImageResource(outlineRes)
                }
                icon?.setColorFilter(Color.parseColor(INACTIVE_COLOR))
            }
        }
    }
    fun navigate(activity: MainActivity, tabId: Int) {
        val target = when (tabId) {
            R.id.nav_home     -> HomeFragment()
            R.id.nav_product  -> ProductListFragment()
            R.id.nav_customer -> com.veganbeauty.admin.features.customer.CustomerAdminFragment()
            R.id.nav_order    -> com.veganbeauty.admin.features.order.list.OrderListFragment()
            R.id.nav_menu     -> com.veganbeauty.admin.features.profile.ProfileFragment()
            else              -> null
        } ?: return

        // Luôn cập nhật tab hiện tại và navigate
        activity.currentTabId = tabId
        activity.loadFragment(target)
        val bottomNav = activity.findViewById<View>(R.id.bottom_nav) ?: return
        highlightTab(bottomNav, tabId)
    }}
