package com.veganbeauty.admin

import android.os.Bundle
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

    override fun setupUI() {
        BottomNavHelper.setup(
            activity = this,
            root = binding.root,
            activeTabId = binding.bottomNav.navHome.id
        ) { tabId ->
            BottomNavHelper.navigate(this, tabId)
        }
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.mainContainer.id, fragment)
            .commit()
    }
}
