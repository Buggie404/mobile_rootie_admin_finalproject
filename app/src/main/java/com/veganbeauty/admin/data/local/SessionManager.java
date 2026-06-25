package com.veganbeauty.admin.data.local;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getSharedPreferences("RootieAdminPrefs", Context.MODE_PRIVATE);
    }

    public void saveSession(String username, String fullName, String role, String assignedStore, String storeID) {
        prefs.edit()
                .putString("username", username)
                .putString("full_name", fullName)
                .putString("role", role)
                .putString("assigned_store", assignedStore)
                .putString("store_id", storeID)
                .putBoolean("is_logged_in", true)
                .apply();
    }

    public void saveSession(String username, String fullName, String role, String assignedStore) {
        saveSession(username, fullName, role, assignedStore, "");
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean("is_logged_in", false);
    }

    public String getUsername() {
        return prefs.getString("username", null);
    }

    public String getFullName() {
        return prefs.getString("full_name", null);
    }

    public String getRole() {
        return prefs.getString("role", null);
    }

    public String getAssignedStore() {
        return prefs.getString("assigned_store", null);
    }

    public String getStoreID() {
        return prefs.getString("store_id", null);
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
