package com.veganbeauty.admin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val avatar: String = "",
    val spending: Long = 0L,
    val tier: String = "Thường",
    val lastActive: String = "",
    val notes: String = "",
    val role: String = "customer",
    val birthday: String = "",
    val region: String = "",
    val joinYear: Int = 1,
    val orderCount: Int = 0,
    val recentPurchase: String = "",
    val spendingYear: Long = 0L,
    val spendingMonth: Long = 0L,
    val points: Int = 0
)
