package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationUserId", "timestamp"]),
        Index(value = ["status"])
    ]
)
data class LocalMessageEntity(
    @PrimaryKey val id: String,
    val conversationUserId: String,
    val senderId: String,
    val recipientId: String,
    val plaintext: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SENDING", // "SENDING", "SENT", "DELIVERED", "READ", "FAILED"
    val hasAttachment: Boolean = false,
    val attachmentUrl: String? = null,
    val attachmentLocalPath: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentFileName: String? = null,
    val attachmentFileSize: Long = 0L,
    val isOutgoing: Boolean = true
)
