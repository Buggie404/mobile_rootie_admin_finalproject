package com.veganbeauty.admin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admins")
data class AdminEntity(
    @PrimaryKey val username: String,
    val password: String = "123456",
    val fullName: String,
    val role: String, // "business" or "nhân viên"
    val storeID: String = "", // foreign key to stores
    val storeName: String = "",
    val storeAddress: String = ""
)
