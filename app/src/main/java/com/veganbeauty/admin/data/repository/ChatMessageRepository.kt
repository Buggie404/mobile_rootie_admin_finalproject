package com.veganbeauty.admin.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.veganbeauty.admin.data.local.RootieAdminDatabase
import com.veganbeauty.admin.data.local.entities.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatMessageRepository(context: Context) {

    private val chatMessageDao = RootieAdminDatabase.getDatabase(context).chatMessageDao()

    fun getAllMessagesLiveData(): LiveData<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()

    fun getMessagesByThreadLiveData(threadId: String): LiveData<List<ChatMessageEntity>> =
        chatMessageDao.getMessagesByThread(threadId)

    suspend fun markThreadAsRead(threadId: String) = withContext(Dispatchers.IO) {
        chatMessageDao.markThreadAsRead(threadId)
    }

    suspend fun sendMessage(threadId: String, customerName: String, text: String, sender: String) = withContext(Dispatchers.IO) {
        val newMsg = ChatMessageEntity(
            id = "msg_${System.currentTimeMillis()}",
            threadId = threadId,
            customerName = customerName,
            messageText = text,
            sender = sender,
            timestamp = System.currentTimeMillis(),
            isRead = true
        )
        chatMessageDao.insertMessage(newMsg)
    }

    suspend fun seedInitialChatsIfEmpty() = withContext(Dispatchers.IO) {
        val existing = chatMessageDao.getMessagesByThreadSync("thread_1")
        if (existing.isEmpty()) {
            val now = System.currentTimeMillis()
            val initial = listOf(
                ChatMessageEntity(
                    id = "msg_1",
                    threadId = "thread_1",
                    customerName = "Nguyễn Thị Lan",
                    messageText = "Dạ shop tư vấn cho em sáp dưỡng ẩm sen Hậu Giang dùng cho da dầu được không ạ?",
                    sender = "customer",
                    timestamp = now - 3600000 * 2, // 2 hours ago
                    isRead = false
                ),
                ChatMessageEntity(
                    id = "msg_2",
                    threadId = "thread_2",
                    customerName = "Trần Minh Hoàng",
                    messageText = "Đơn hàng #9283 của mình khi nào giao tới Quận 7 vậy shop ơi?",
                    sender = "customer",
                    timestamp = now - 3600000 * 5, // 5 hours ago
                    isRead = true
                ),
                ChatMessageEntity(
                    id = "msg_3",
                    threadId = "thread_2",
                    customerName = "Trần Minh Hoàng",
                    messageText = "Dạ đơn hàng đang được shipper giao đi rồi ạ, dự kiến chiều nay sẽ tới.",
                    sender = "admin",
                    timestamp = now - 3600000 * 4, // 4 hours ago
                    isRead = true
                ),
                ChatMessageEntity(
                    id = "msg_4",
                    threadId = "thread_3",
                    customerName = "Lê Mỹ Duyên",
                    messageText = "Serum nghệ Hưng Yên dùng thích lắm ạ! Cảm ơn shop nhiều nha!",
                    sender = "customer",
                    timestamp = now - 3600000 * 24, // 1 day ago
                    isRead = true
                )
            )
            chatMessageDao.insertMessages(initial)
        }
    }
}
