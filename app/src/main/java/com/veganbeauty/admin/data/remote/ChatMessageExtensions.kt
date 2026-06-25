package com.veganbeauty.admin.data.remote

fun ChatMessage(
    id: String = "",
    senderId: String = "",
    senderName: String = "",
    senderAvatar: String = "",
    receiverId: String = "",
    receiverName: String = "",
    receiverAvatar: String = "",
    content: String = "",
    timestamp: Long = 0L,
    isRead: Boolean = false
): ChatMessage {
    val entity = ChatMessage()
    entity.id = id
    entity.senderId = senderId
    entity.senderName = senderName
    entity.senderAvatar = senderAvatar
    entity.receiverId = receiverId
    entity.receiverName = receiverName
    entity.receiverAvatar = receiverAvatar
    entity.content = content
    entity.timestamp = timestamp
    entity.isRead = isRead
    return entity
}
