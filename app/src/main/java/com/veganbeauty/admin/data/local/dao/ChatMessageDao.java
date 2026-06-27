package com.veganbeauty.admin.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.veganbeauty.admin.data.local.entities.ChatMessageEntity;

import java.util.List;

@Dao
public interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    LiveData<List<ChatMessageEntity>> getAllMessages();

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    LiveData<List<ChatMessageEntity>> getMessagesByThread(String threadId);

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    List<ChatMessageEntity> getMessagesByThreadSync(String threadId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessages(List<ChatMessageEntity> messages);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(ChatMessageEntity message);

    @Query("UPDATE chat_messages SET isRead = 1 WHERE threadId = :threadId")
    void markThreadAsRead(String threadId);
}
