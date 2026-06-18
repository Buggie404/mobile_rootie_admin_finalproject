package com.veganbeauty.admin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val threadId: String,
    val customerName: String,
    val messageText: String,
    val sender: String, // "customer" or "admin"
    val timestamp: Long,
    val isRead: Boolean
)
