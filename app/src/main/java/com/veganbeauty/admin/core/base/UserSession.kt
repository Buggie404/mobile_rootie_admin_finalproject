package com.veganbeauty.admin.core.base

import android.content.Context

object UserSession {
    private const val PREFS_NAME = "rootie_admin_prefs"
    private const val KEY_ROLE = "current_user_role"

    fun getRole(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ROLE, "admin") ?: "admin"
    }

    fun setRole(context: Context, role: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ROLE, role).apply()
    }
}
