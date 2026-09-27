package com.example.model

import org.json.JSONObject

sealed class DecryptedContent {
    data class Text(
        val text: String,
        val replyToMessageId: String? = null,
        val replyToSenderName: String? = null,
        val replyToText: String? = null
    ) : DecryptedContent()

    data class Attachment(
        val storagePath: String,
        val fileName: String,
        val mimeType: String,
        val fileSize: Long,
        val aesKey: String,
        val aesIv: String,
        val caption: String
    ) : DecryptedContent()

    fun toJsonString(): String {
        val json = JSONObject()
        when (this) {
            is Text -> {
                json.put("type", "text")
                json.put("text", text)
                if (replyToMessageId != null) json.put("replyToMessageId", replyToMessageId)
                if (replyToSenderName != null) json.put("replyToSenderName", replyToSenderName)
                if (replyToText != null) json.put("replyToText", replyToText)
            }
            is Attachment -> {
                json.put("type", "attachment")
                json.put("storagePath", storagePath)
                json.put("fileName", fileName)
                json.put("mimeType", mimeType)
                json.put("fileSize", fileSize)
                json.put("aesKey", aesKey)
                json.put("aesIv", aesIv)
                json.put("caption", caption)
            }
        }
        return json.toString()
    }

    companion object {
        fun fromJsonString(jsonStr: String): DecryptedContent {
            return try {
                val json = JSONObject(jsonStr)
                when (json.optString("type")) {
                    "attachment" -> Attachment(
                        storagePath = json.getString("storagePath"),
                        fileName = json.optString("fileName", "attachment"),
                        mimeType = json.optString("mimeType", "application/octet-stream"),
                        fileSize = json.optLong("fileSize", 0L),
                        aesKey = json.getString("aesKey"),
                        aesIv = json.getString("aesIv"),
                        caption = json.optString("caption", "")
                    )
                    else -> Text(
                        text = json.optString("text", jsonStr),
                        replyToMessageId = json.optString("replyToMessageId").ifEmpty { null },
                        replyToSenderName = json.optString("replyToSenderName").ifEmpty { null },
                        replyToText = json.optString("replyToText").ifEmpty { null }
                    )
                }
            } catch (e: Exception) {
                // If not JSON or legacy plaintext
                Text(jsonStr)
            }
        }
    }
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

data class ChatMessage(
    val id: String,
    val conversationUserId: String,
    val senderId: String,
    val recipientId: String,
    val plaintext: String,
    val timestamp: Long,
    val status: MessageStatus,
    val hasAttachment: Boolean,
    val attachmentUrl: String? = null,
    val attachmentLocalPath: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentFileName: String? = null,
    val attachmentFileSize: Long = 0L,
    val isOutgoing: Boolean
)

data class FriendContact(
    val userId: String,
    val displayName: String,
    val identityKey: String = "",
    val lastMessageText: String = "",
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L
)
