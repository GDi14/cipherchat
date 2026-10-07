package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["lastMessageTimestamp"])]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val type: String, // "direct" or "group"
    val title: String?,
    val otherUserId: String? = null,
    val otherUserTag: String? = null,
    val lastMessageText: String? = null,
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Int = 0
)

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId", "timestamp"]),
        Index(value = ["replyToId"])
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String? = null,
    val recipientId: String? = null,
    val plaintextBody: String = "",
    val messageType: Int = 3,
    val replyToId: String? = null,
    val replySnippet: String? = null,
    val mediaPath: String? = null,
    val mediaType: String? = null,
    val mediaKey: String? = null,
    val mediaIv: String? = null,
    val mediaDuration: Int = 0,
    val localDecryptedPath: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "'[]'") val textEmphasisJson: String = "[]",
    val status: String = "sent", // sending, sent, delivered, read, failed
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "friend_requests",
    indices = [
        Index(value = ["senderId", "receiverId"], unique = true),
        Index(value = ["status"])
    ]
)
data class FriendRequestEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val receiverId: String,
    val senderName: String,
    val senderTag: String,
    val status: String, // pending, accepted, declined
    val isIncoming: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "profiles",
    indices = [Index(value = ["tag"], unique = true)]
)
data class ProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val tag: String,
    val avatarPath: String? = null,
    val isFriend: Boolean = false
)

@Entity(tableName = "signal_sessions", primaryKeys = ["addressName", "deviceId"])
data class SignalSessionEntity(
    val addressName: String,
    val deviceId: Int,
    val recordBytes: ByteArray
)

@Entity(tableName = "signal_prekeys")
data class SignalPreKeyEntity(
    @PrimaryKey
    val keyId: Int,
    val recordBytes: ByteArray
)

@Entity(tableName = "signal_signed_prekeys")
data class SignalSignedPreKeyEntity(
    @PrimaryKey
    val keyId: Int,
    val recordBytes: ByteArray
)

@Entity(tableName = "signal_sender_keys", primaryKeys = ["groupId", "senderAddress", "deviceId"])
data class SignalSenderKeyEntity(
    val groupId: String,
    val senderAddress: String,
    val deviceId: Int,
    val recordBytes: ByteArray
)
