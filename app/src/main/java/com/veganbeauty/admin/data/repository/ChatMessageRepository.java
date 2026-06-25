package com.veganbeauty.admin.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.veganbeauty.admin.data.local.RootieAdminDatabase;
import com.veganbeauty.admin.data.local.dao.ChatMessageDao;
import com.veganbeauty.admin.data.local.entities.ChatMessageEntity;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageRepository {
    private final ChatMessageDao chatMessageDao;

    public ChatMessageRepository(Context context) {
        this.chatMessageDao = RootieAdminDatabase.getDatabase(context).chatMessageDao();
    }

    public LiveData<List<ChatMessageEntity>> getAllMessagesLiveData() {
        return chatMessageDao.getAllMessages();
    }

    public LiveData<List<ChatMessageEntity>> getMessagesByThreadLiveData(String threadId) {
        return chatMessageDao.getMessagesByThread(threadId);
    }

    public void markThreadAsRead(String threadId) {
        chatMessageDao.markThreadAsRead(threadId);
    }

    public void sendMessage(String threadId, String customerName, String text, String sender) {
        ChatMessageEntity newMsg = new ChatMessageEntity();
        newMsg.setId("msg_" + System.currentTimeMillis());
        newMsg.setThreadId(threadId);
        newMsg.setCustomerName(customerName);
        newMsg.setMessageText(text);
        newMsg.setSender(sender);
        newMsg.setTimestamp(System.currentTimeMillis());
        newMsg.setRead(true);
        chatMessageDao.insertMessage(newMsg);
    }

    public void seedInitialChatsIfEmpty() {
        List<ChatMessageEntity> existing = chatMessageDao.getMessagesByThreadSync("thread_1");
        if (existing.isEmpty()) {
            long now = System.currentTimeMillis();
            List<ChatMessageEntity> initial = new ArrayList<>();
            
            ChatMessageEntity msg1 = new ChatMessageEntity();
            msg1.setId("msg_1");
            msg1.setThreadId("thread_1");
            msg1.setCustomerName("Nguyễn Thị Lan");
            msg1.setMessageText("Dạ shop tư vấn cho em sáp dưỡng ẩm sen Hậu Giang dùng cho da dầu được không ạ?");
            msg1.setSender("customer");
            msg1.setTimestamp(now - 3600000 * 2);
            msg1.setRead(false);
            initial.add(msg1);

            ChatMessageEntity msg2 = new ChatMessageEntity();
            msg2.setId("msg_2");
            msg2.setThreadId("thread_2");
            msg2.setCustomerName("Trần Minh Hoàng");
            msg2.setMessageText("Đơn hàng #9283 của mình khi nào giao tới Quận 7 vậy shop ơi?");
            msg2.setSender("customer");
            msg2.setTimestamp(now - 3600000 * 5);
            msg2.setRead(true);
            initial.add(msg2);

            ChatMessageEntity msg3 = new ChatMessageEntity();
            msg3.setId("msg_3");
            msg3.setThreadId("thread_2");
            msg3.setCustomerName("Trần Minh Hoàng");
            msg3.setMessageText("Dạ đơn hàng đang được shipper giao đi rồi ạ, dự kiến chiều nay sẽ tới.");
            msg3.setSender("admin");
            msg3.setTimestamp(now - 3600000 * 4);
            msg3.setRead(true);
            initial.add(msg3);

            ChatMessageEntity msg4 = new ChatMessageEntity();
            msg4.setId("msg_4");
            msg4.setThreadId("thread_3");
            msg4.setCustomerName("Lê Mỹ Duyên");
            msg4.setMessageText("Serum nghệ Hưng Yên dùng thích lắm ạ! Cảm ơn shop nhiều nha!");
            msg4.setSender("customer");
            msg4.setTimestamp(now - 3600000 * 24);
            msg4.setRead(true);
            initial.add(msg4);

            chatMessageDao.insertMessages(initial);
        }
    }
}
