package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class User(
    val userId: String = "",
    val tag: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val avatarShape: String = "flower", // "circle", "flower", "squircle", "clover"
    val chatFont: String = "roboto_flex",
    val fontAxes: String = "",
    val bubbleColor: String = "#FF5C9D",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

data class TagLookup(
    val tag: String = "",
    val userId: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

data class PreKeyBundleDto(
    val userId: String = "",
    val registrationId: Int = 0,
    val identityKey: String = "",
    val signedPreKeyId: Int = 0,
    val signedPreKey: String = "",
    val signedPreKeySignature: String = "",
    val preKeys: Map<String, String> = emptyMap(), // Stored as Map<String, String> keyed by preKeyId string to prevent index out of bounds crashes
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

typealias PublicKeys = PreKeyBundleDto

data class PreKeyRecordDto(
    val id: Int = 0,
    val key: String = ""
)

data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val fromTag: String = "",
    val fromDisplayName: String = "",
    val fromPhotoUrl: String = "",
    val status: String = "pending", // "pending", "accepted", "declined"
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

data class Friendship(
    val id: String = "",
    val user1Id: String = "",
    val user2Id: String = "",
    val users: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

data class Conversation(
    val id: String = "",
    val type: String = "direct", // "direct", "group"
    val name: String = "",
    val memberIds: List<String> = emptyList(),
    val lastMessageSnippet: String = "",
    @ServerTimestamp
    val lastMessageTimestamp: Timestamp? = null,
    val typingUsers: List<String> = emptyList(),
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

data class EncryptedMessage(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderTag: String = "",
    val senderDisplayName: String = "",
    val senderChatFont: String = "roboto_flex",
    val senderBubbleColor: String = "#FF5C9D",
    val recipientId: String = "",
    val memberIds: List<String> = emptyList(),
    val encryptedBody: String = "", // Base64 Signal ciphertext
    val messageType: Int = 1, // 1: Text, 2: Photo, 3: Voice, 4: Video
    val mediaUrl: String = "", // Cloudinary URL to encrypted media blob
    val mediaEncryptionKey: String = "", // Base64 encrypted AES-256-GCM key
    val mediaDuration: Int = 0, // In seconds (for voice messages)
    val replyToMessageId: String = "",
    val replyToSnippet: String = "",
    val replyToSenderName: String = "",
    val status: String = "sent", // "sent", "delivered", "read"
    val readBy: List<String> = emptyList(),
    val deletedForUsers: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Timestamp? = null
)
