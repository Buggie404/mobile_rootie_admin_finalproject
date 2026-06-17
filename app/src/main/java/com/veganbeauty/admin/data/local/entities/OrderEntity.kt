package com.veganbeauty.admin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

data class OrderItem(
    val productId: String,
    val productName: String,
    val productImage: String,
    val quantity: Int,
    val price: Long
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val orderId: String,
    val userId: String = "",
    val orderDate: String,
    val orderTime: String,
    val status: String,
    val totalAmount: Long,
    val items: List<OrderItem>,
    val shippingName: String = "",
    val shippingPhone: String = "",
    val shippingAddress: String = "",
    val shippingCost: Long = 0L,
    val voucherDiscount: Long = 0L,
    val paymentMethod: String = "",
    val expectedDeliveryTime: String? = null,
    val hasReview: Boolean = false,
    val reviewStars: Int = 0,
    val reviewText: String? = null,
    val reviewImage: String? = null,
    val isAnonymous: Boolean = false,
    val recommendToFriends: Boolean = false
)
