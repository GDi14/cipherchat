package com.example.data.remote

import com.example.crypto.OneTimePrekeyItem
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@JsonClass(generateAdapter = true)
@Serializable
data class ProfileDto(
    @Json(name = "id") val id: String,
    @Json(name = "device_id") @SerialName("device_id") val deviceId: String,
    @Json(name = "display_name") @SerialName("display_name") val displayName: String,
    @Json(name = "tag") val tag: String,
    @Json(name = "avatar_path") @SerialName("avatar_path") val avatarPath: String? = null,
    @Json(name = "created_at") @SerialName("created_at") val createdAt: String? = null,
    @Json(name = "updated_at") @SerialName("updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
@Serializable
data class PrekeyDto(
    @Json(name = "user_id") @SerialName("user_id") val userId: String,
    @Json(name = "identity_key") @SerialName("identity_key") val identityKey: String,
    @Json(name = "registration_id") @SerialName("registration_id") val registrationId: Int,
    @Json(name = "signed_prekey_id") @SerialName("signed_prekey_id") val signedPrekeyId: Int,
    @Json(name = "signed_prekey") @SerialName("signed_prekey") val signedPrekey: String,
    @Json(name = "signed_prekey_signature") @SerialName("signed_prekey_signature") val signedPrekeySignature: String,
    @Json(name = "one_time_prekeys") @SerialName("one_time_prekeys") val oneTimePrekeys: List<OneTimePrekeyItem> = emptyList(),
    @Json(name = "updated_at") @SerialName("updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
@Serializable
data class FriendRequestDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "sender_id") @SerialName("sender_id") val senderId: String,
    @Json(name = "receiver_id") @SerialName("receiver_id") val receiverId: String,
    @Json(name = "status") val status: String,
    @Json(name = "created_at") @SerialName("created_at") val createdAt: String? = null,
    @Json(name = "updated_at") @SerialName("updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
@Serializable
data class ConversationDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "type") val type: String, // "direct" or "group"
    @Json(name = "title") val title: String? = null,
    @Json(name = "created_at") @SerialName("created_at") val createdAt: String? = null,
    @Json(name = "updated_at") @SerialName("updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
@Serializable
data class ConversationMemberDto(
    @Json(name = "conversation_id") @SerialName("conversation_id") val conversationId: String,
    @Json(name = "user_id") @SerialName("user_id") val userId: String,
    @Json(name = "joined_at") @SerialName("joined_at") val joinedAt: String? = null
)

@JsonClass(generateAdapter = true)
@Serializable
data class MessageDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "conversation_id") @SerialName("conversation_id") val conversationId: String,
    @Json(name = "sender_id") @SerialName("sender_id") val senderId: String,
    @Json(name = "recipient_id") @SerialName("recipient_id") val recipientId: String? = null,
    @Json(name = "message_type") @SerialName("message_type") val messageType: Int = 3,
    @Json(name = "encrypted_body") @SerialName("encrypted_body") val encryptedBody: String,
    @Json(name = "reply_to_id") @SerialName("reply_to_id") val replyToId: String? = null,
    @Json(name = "status") val status: String = "sent",
    @Json(name = "created_at") @SerialName("created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
@Serializable
data class MessageReceiptDto(
    @Json(name = "message_id") @SerialName("message_id") val messageId: String,
    @Json(name = "user_id") @SerialName("user_id") val userId: String,
    @Json(name = "status") val status: String,
    @Json(name = "updated_at") @SerialName("updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
@Serializable
data class GroupSenderKeyDto(
    @Json(name = "conversation_id") @SerialName("conversation_id") val conversationId: String,
    @Json(name = "sender_id") @SerialName("sender_id") val senderId: String,
    @Json(name = "recipient_id") @SerialName("recipient_id") val recipientId: String,
    @Json(name = "encrypted_distribution_message") @SerialName("encrypted_distribution_message") val encryptedDistributionMessage: String,
    @Json(name = "iteration") val iteration: Int = 0,
    @Json(name = "updated_at") @SerialName("updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
@Serializable
data class TypingStatusDto(
    @Json(name = "conversation_id") @SerialName("conversation_id") val conversationId: String,
    @Json(name = "user_id") @SerialName("user_id") val userId: String,
    @Json(name = "is_typing") @SerialName("is_typing") val isTyping: Boolean = false,
    @Json(name = "updated_at") @SerialName("updated_at") val updatedAt: String? = null
)
