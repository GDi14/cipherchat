package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.LocalMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationUserId = :conversationUserId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationUserId: String): Flow<List<LocalMessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): LocalMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: LocalMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<LocalMessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: String, status: String)

    @Query("UPDATE messages SET attachmentLocalPath = :path WHERE id = :id")
    suspend fun updateAttachmentLocalPath(id: String, path: String)

    @Query("UPDATE messages SET status = 'READ' WHERE conversationUserId = :conversationUserId AND isOutgoing = 0 AND status != 'READ'")
    suspend fun markIncomingMessagesAsRead(conversationUserId: String)

    @Query("SELECT * FROM messages WHERE isOutgoing = 1 AND status IN ('SENDING', 'SENT', 'DELIVERED')")
    suspend fun getPendingOutgoingMessages(): List<LocalMessageEntity>

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}
