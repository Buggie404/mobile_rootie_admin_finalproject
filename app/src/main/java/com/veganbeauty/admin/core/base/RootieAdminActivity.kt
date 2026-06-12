package com.veganbeauty.admin.core.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class RootieAdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        observeViewModel()
    }

    abstract fun setupUI()

    open fun observeViewModel() {
        // Option to observe common ViewModel livedata
    }
}
