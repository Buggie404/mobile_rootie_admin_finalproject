package com.veganbeauty.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.veganbeauty.admin.core.base.RootieAdminActivity
import com.veganbeauty.admin.data.local.SessionManager
import com.veganbeauty.admin.databinding.ActivityMainBinding
import com.veganbeauty.admin.features.auth.LoginActivity
import com.veganbeauty.admin.features.home.BottomNavHelper
import com.veganbeauty.admin.features.home.HomeFragment

class MainActivity : RootieAdminActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            super.onCreate(savedInstanceState)
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        super.onCreate(savedInstanceState)
        
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }
    var currentTabId: Int = R.id.nav_home

    override fun setupUI() {
        val sessionManager = SessionManager(this)
        val role = sessionManager.getRole() ?: "admin"

        val bottomNav = findViewById<View>(R.id.bottom_nav)
        if (bottomNav != null) {
            if (role == "staff" || role == "nhân viên") {
                bottomNav.findViewById<View>(R.id.nav_product)?.visibility = View.GONE
                bottomNav.findViewById<View>(R.id.nav_customer)?.visibility = View.GONE
            } else {
                bottomNav.findViewById<View>(R.id.nav_product)?.visibility = View.VISIBLE
                bottomNav.findViewById<View>(R.id.nav_customer)?.visibility = View.VISIBLE
            }

            BottomNavHelper.setup(
                activity = this,
                root = bottomNav
            ) { tabId ->
                // Luôn cho phép navigate về Home, dù đang ở Home hay không
                BottomNavHelper.navigate(this, tabId)
            }
            BottomNavHelper.highlightTab(bottomNav, currentTabId)
        }
    }

    /**
     * Được gọi bởi các fragment con khi tự điều hướng (không qua bottom nav)
     * để đồng bộ trạng thái highlight của bottom nav.
     */
    fun syncTab(tabId: Int) {
        currentTabId = tabId
        val bottomNav = findViewById<View>(R.id.bottom_nav) ?: return
        BottomNavHelper.highlightTab(bottomNav, tabId)
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.mainContainer.id, fragment)
            .commit()
    }
}
