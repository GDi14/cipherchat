package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.LocalContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY lastMessageTimestamp DESC, displayName ASC")
    fun getAllContacts(): Flow<List<LocalContactEntity>>

    @Query("SELECT * FROM contacts WHERE userId = :userId LIMIT 1")
    suspend fun getContactById(userId: String): LocalContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateContact(contact: LocalContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateContacts(contacts: List<LocalContactEntity>)

    @Query("UPDATE contacts SET isOnline = :isOnline, lastSeen = :lastSeen WHERE userId = :userId")
    suspend fun updatePresence(userId: String, isOnline: Boolean, lastSeen: Long)

    @Query("UPDATE contacts SET lastMessageText = :lastText, lastMessageTimestamp = :timestamp WHERE userId = :userId")
    suspend fun updateLastMessage(userId: String, lastText: String, timestamp: Long)

    @Query("UPDATE contacts SET unreadCount = unreadCount + 1 WHERE userId = :userId")
    suspend fun incrementUnread(userId: String)

    @Query("UPDATE contacts SET unreadCount = 0 WHERE userId = :userId")
    suspend fun resetUnread(userId: String)

    @Query("DELETE FROM contacts")
    suspend fun clearContacts()
}
