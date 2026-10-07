package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE type = 'direct' AND otherUserId = :otherUserId LIMIT 1")
    suspend fun getDirectConversation(otherUserId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: ConversationEntity)

    @Query("UPDATE conversations SET lastMessageText = :text, lastMessageTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastMessage(id: String, text: String, timestamp: Long)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :id")
    suspend fun clearUnreadCount(id: String)

    @Query("UPDATE conversations SET unreadCount = unreadCount + 1 WHERE id = :id")
    suspend fun incrementUnreadCount(id: String)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("""
        UPDATE messages SET status = CASE
            WHEN status = 'read' OR :status = 'read' THEN 'read'
            WHEN status = 'delivered' OR :status = 'delivered' THEN 'delivered'
            WHEN status = 'failed' AND :status IN ('sending', 'sent') THEN 'failed'
            ELSE :status
        END WHERE id = :id
    """)
    suspend fun updateStatus(id: String, status: String)

    @Query("""
        UPDATE messages SET
            recipientId = :recipientId,
            messageType = :messageType,
            status = CASE
                WHEN status = 'read' OR :status = 'read' THEN 'read'
                WHEN status = 'delivered' OR :status = 'delivered' THEN 'delivered'
                WHEN status = 'failed' AND :status IN ('sending', 'sent') THEN 'failed'
                ELSE :status
            END
        WHERE id = :id
    """)
    suspend fun updateOutgoingMessageState(
        id: String,
        recipientId: String?,
        messageType: Int,
        status: String
    )

    @Query("UPDATE messages SET status = :status WHERE conversationId = :conversationId AND senderId != :myUserId AND status != 'read'")
    suspend fun markAllIncomingAsRead(conversationId: String, myUserId: String, status: String = "read")

    @Query("UPDATE messages SET localDecryptedPath = :path WHERE id = :id")
    suspend fun updateLocalDecryptedPath(id: String, path: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)
}

@Dao
interface FriendRequestDao {
    @Query("SELECT * FROM friend_requests ORDER BY timestamp DESC")
    fun getAllRequests(): Flow<List<FriendRequestEntity>>

    @Query("SELECT * FROM friend_requests WHERE isIncoming = 1 AND status = 'pending' ORDER BY timestamp DESC")
    fun getPendingIncomingRequests(): Flow<List<FriendRequestEntity>>

    @Query("SELECT * FROM friend_requests WHERE id = :id LIMIT 1")
    suspend fun getRequestById(id: String): FriendRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(request: FriendRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(requests: List<FriendRequestEntity>)

    @Query("UPDATE friend_requests SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM friend_requests WHERE id = :id")
    suspend fun deleteRequest(id: String)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE isFriend = 1")
    fun getFriends(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE lower(tag) = lower(:tag) LIMIT 1")
    suspend fun getProfileByTag(tag: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: ProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<ProfileEntity>)

    @Query("UPDATE profiles SET isFriend = :isFriend WHERE id = :id")
    suspend fun setFriendStatus(id: String, isFriend: Boolean)
}

@Dao
interface SignalStoreDao {
    @Query("SELECT * FROM signal_sessions WHERE addressName = :addressName AND deviceId = :deviceId LIMIT 1")
    suspend fun getSession(addressName: String, deviceId: Int): SignalSessionEntity?

    @Query("SELECT * FROM signal_sessions WHERE addressName = :addressName")
    suspend fun getSubDeviceSessions(addressName: String): List<SignalSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun storeSession(session: SignalSessionEntity)

    @Query("DELETE FROM signal_sessions WHERE addressName = :addressName AND deviceId = :deviceId")
    suspend fun deleteSession(addressName: String, deviceId: Int)

    @Query("SELECT * FROM signal_prekeys WHERE keyId = :keyId LIMIT 1")
    suspend fun getPreKey(keyId: Int): SignalPreKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun storePreKey(preKey: SignalPreKeyEntity)

    @Query("DELETE FROM signal_prekeys WHERE keyId = :keyId")
    suspend fun deletePreKey(keyId: Int)

    @Query("SELECT * FROM signal_signed_prekeys WHERE keyId = :keyId LIMIT 1")
    suspend fun getSignedPreKey(keyId: Int): SignalSignedPreKeyEntity?

    @Query("SELECT * FROM signal_signed_prekeys")
    suspend fun getAllSignedPreKeys(): List<SignalSignedPreKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun storeSignedPreKey(signedPreKey: SignalSignedPreKeyEntity)

    @Query("SELECT * FROM signal_sender_keys WHERE groupId = :groupId AND senderAddress = :senderAddress AND deviceId = :deviceId LIMIT 1")
    suspend fun getSenderKey(groupId: String, senderAddress: String, deviceId: Int): SignalSenderKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun storeSenderKey(senderKey: SignalSenderKeyEntity)
}
