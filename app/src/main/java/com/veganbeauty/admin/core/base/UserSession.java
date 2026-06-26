package com.veganbeauty.admin.core.base;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSession {
    private static final String PREFS_NAME = "rootie_admin_prefs";
    private static final String KEY_ROLE = "current_user_role";

    public static String getRole(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String role = prefs.getString(KEY_ROLE, "admin");
        return role != null ? role : "admin";
    }

    public static void setRole(Context context, String role) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ROLE, role).apply();
    }
}
