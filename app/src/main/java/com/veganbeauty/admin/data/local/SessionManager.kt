package com.veganbeauty.admin.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("RootieAdminPrefs", Context.MODE_PRIVATE)

    fun saveSession(username: String, fullName: String, role: String, assignedStore: String, storeID: String = "") {
        prefs.edit().apply {
            putString("username", username)
            putString("full_name", fullName)
            putString("role", role)
            putString("assigned_store", assignedStore)
            putString("store_id", storeID)
            putBoolean("is_logged_in", true)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun getUsername(): String? {
        return prefs.getString("username", null)
    }

    fun getFullName(): String? {
        return prefs.getString("full_name", null)
    }

    fun getRole(): String? {
        return prefs.getString("role", null)
    }

    fun getAssignedStore(): String? {
        return prefs.getString("assigned_store", null)
    }

    fun getStoreID(): String? {
        return prefs.getString("store_id", null)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
