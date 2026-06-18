package com.veganbeauty.admin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val userEmail: String = "",
    val serviceName: String,
    val dateDisplay: String,
    val monthDisplay: String = "",
    val dayOfWeek: String = "",
    val time: String,
    val duration: String = "",
    val storeName: String,
    val storeAddress: String,
    val storePhone: String = "",
    val storeImage: String = "",
    val storeID: String = "", // foreign key to stores
    val note: String = "",
    val status: String,
    val createdAt: String = "",
    val consultantName: String = "",
    val cancelReason: String = ""
)
