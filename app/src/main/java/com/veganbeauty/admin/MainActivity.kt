package com.veganbeauty.admin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.veganbeauty.admin.core.base.RootieAdminActivity
import com.veganbeauty.admin.databinding.ActivityMainBinding
import com.veganbeauty.admin.features.home.BottomNavHelper
import com.veganbeauty.admin.features.home.HomeFragment

class MainActivity : RootieAdminActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        super.onCreate(savedInstanceState)
        
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }

    private var currentTabId: Int = R.id.nav_home

    override fun setupUI() {
        val bottomNav = findViewById<View>(R.id.bottom_nav)
        if (bottomNav != null) {
            BottomNavHelper.setup(
                activity = this,
                root = bottomNav
            ) { tabId ->
                if (tabId != currentTabId) {
                    currentTabId = tabId
                    BottomNavHelper.navigate(this, tabId)
                }
            }
            BottomNavHelper.highlightTab(bottomNav, currentTabId)
        }
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.mainContainer.id, fragment)
            .commit()
    }
}
