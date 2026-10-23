package com.example.ui.model

import androidx.compose.runtime.Immutable
import com.example.crypto.MessagePayloadCodec
import com.example.crypto.TextEmphasisRange
import com.example.data.local.MessageEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MessageContentType {
    TEXT_OUTGOING,
    TEXT_INCOMING,
    VOICE_NOTE,
    IMAGE,
    VIDEO
}

@Immutable
data class MessageUiModel(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String?,
    val recipientId: String?,
    val plaintextBody: String,
    val messageType: Int,
    val replyToId: String?,
    val replySnippet: String?,
    val mediaPath: String?,
    val mediaType: String?,
    val mediaKey: String?,
    val mediaIv: String?,
    val mediaDuration: Int,
    val localDecryptedPath: String?,
    val blurHash: String?,
    val textEmphasisRanges: List<TextEmphasisRange>,
    val status: String,
    val syncStatus: String,
    val timestamp: Long,
    val formattedTime: String,
    val isFromMe: Boolean,
    val contentType: MessageContentType,
    val originalEntity: MessageEntity
)

fun MessageEntity.toUiModel(myUserId: String): MessageUiModel {
    val isFromMe = (senderId == myUserId)
    val parsedEmphasis = MessagePayloadCodec.decodeEmphasisRanges(textEmphasisJson)
    val timeStr = runCatching {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    }.getOrDefault("")

    val type = when {
        mediaType?.startsWith("audio") == true -> MessageContentType.VOICE_NOTE
        mediaType?.startsWith("video") == true -> MessageContentType.VIDEO
        mediaType?.startsWith("image") == true -> MessageContentType.IMAGE
        isFromMe -> MessageContentType.TEXT_OUTGOING
        else -> MessageContentType.TEXT_INCOMING
    }

    return MessageUiModel(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        senderName = senderName,
        recipientId = recipientId,
        plaintextBody = plaintextBody,
        messageType = messageType,
        replyToId = replyToId,
        replySnippet = replySnippet,
        mediaPath = mediaPath,
        mediaType = mediaType,
        mediaKey = mediaKey,
        mediaIv = mediaIv,
        mediaDuration = mediaDuration,
        localDecryptedPath = localDecryptedPath,
        blurHash = blurHash,
        textEmphasisRanges = parsedEmphasis,
        status = status,
        syncStatus = syncStatus,
        timestamp = timestamp,
        formattedTime = timeStr,
        isFromMe = isFromMe,
        contentType = type,
        originalEntity = this
    )
}
