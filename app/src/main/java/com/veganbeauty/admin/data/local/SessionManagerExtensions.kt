package com.veganbeauty.admin.data.local

fun SessionManager.saveSession(
    username: String,
    fullName: String,
    role: String,
    assignedStore: String,
    storeID: String = ""
) {
    this.saveSession(username, fullName, role, assignedStore, storeID)
}
