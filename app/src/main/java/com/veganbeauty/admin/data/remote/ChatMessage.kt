package com.veganbeauty.admin.data.remote

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverAvatar: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    var isRead: Boolean = false
)
