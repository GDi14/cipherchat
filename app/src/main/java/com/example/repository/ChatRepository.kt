package com.example.repository

import android.content.Context
import android.net.Uri
import com.example.crypto.MediaEncryptor
import com.example.crypto.MediaPayload
import com.example.crypto.MessagePayload
import com.example.crypto.MessagePayloadCodec
import com.example.crypto.OneTimePrekeyItem
import com.example.crypto.PublicPrekeyBundle
import com.example.crypto.SignalProtocolManager
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.ProfileEntity
import com.example.data.local.SecurePreferencesManager
import com.example.data.remote.ConversationDto
import com.example.data.remote.ConversationMemberDto
import com.example.data.remote.GroupSenderKeyDto
import com.example.data.remote.MessageDto
import com.example.data.remote.MessageReceiptDto
import com.example.data.remote.PrekeyDto
import com.example.data.remote.SupabaseClientProvider
import com.example.data.remote.TypingStatusDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class ChatRepository(
    private val context: Context,
    private val securePrefs: SecurePreferencesManager,
    private val signalManager: SignalProtocolManager,
    private val supabaseProvider: SupabaseClientProvider,
    private val database: AppDatabase
) {
    val myUserId: String = securePrefs.getOrCreateUserId()
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // Typing status per conversation: conversationId -> set of userIds typing
    private val _typingUsers = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val typingUsers: StateFlow<Map<String, Set<String>>> = _typingUsers.asStateFlow()

    private var realtimeChannelJob: Job? = null

    fun getConversations(): Flow<List<ConversationEntity>> =
        database.conversationDao().getAllConversations()

    fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>> =
        database.messageDao().getMessagesForConversation(convId)

    suspend fun getConversation(convId: String): ConversationEntity? = withContext(Dispatchers.IO) {
        database.conversationDao().getConversationById(convId)
    }

    /**
     * Creates an encrypted group with selected accepted friends.
     */
    suspend fun createGroupConversation(title: String, memberIds: List<String>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val convId = UUID.randomUUID().toString()
            val allMembers = (memberIds + myUserId).distinct()

            if (supabaseProvider.isConfigured) {
                val client = supabaseProvider.getClient()
                // 1. Insert conversation
                client.postgrest["conversations"].insert(
                    ConversationDto(id = convId, type = "group", title = title)
                )

                // 2. Insert conversation members
                val memberDtos = allMembers.map {
                    ConversationMemberDto(conversationId = convId, userId = it)
                }
                client.postgrest["conversation_members"].insert(memberDtos)

                // 3. Create group SenderKeyDistributionMessage and distribute to other members
                val distributionMsgB64 = signalManager.createGroupDistributionMessage(convId)
                for (otherId in memberIds) {
                    if (otherId == myUserId) continue
                    // Ensure 1:1 session exists before sending distribution message
                    ensureDirectSession(otherId)
                    val encResult = signalManager.encryptDirectMessage(
                        otherId,
                        distributionMsgB64.toByteArray(Charsets.UTF_8)
                    )
                    client.postgrest["group_sender_keys"].upsert(
                        GroupSenderKeyDto(
                            conversationId = convId,
                            senderId = myUserId,
                            recipientId = otherId,
                            encryptedDistributionMessage = encResult.ciphertextBase64
                        )
                    )
                }
            }

            // Save group in Room
            database.conversationDao().insertOrUpdate(
                ConversationEntity(
                    id = convId,
                    type = "group",
                    title = title,
                    lastMessageText = "Group created with ${allMembers.size} members",
                    lastMessageTimestamp = System.currentTimeMillis(),
                    unreadCount = 0
                )
            )

            Result.success(convId)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Sends an E2EE text or reply message.
     */
    suspend fun sendMessage(
        conversationId: String,
        text: String,
        replyToMessage: MessageEntity? = null
    ): Result<MessageEntity> = withContext(Dispatchers.IO) {
        try {
            val conv = database.conversationDao().getConversationById(conversationId)
                ?: return@withContext Result.failure(IllegalStateException("Conversation not found"))

            val payload = MessagePayload(
                text = text.trim(),
                replyToId = replyToMessage?.id,
                replySnippet = replyToMessage?.let {
                    if (it.plaintextBody.isNotBlank()) it.plaintextBody.take(60) else "[Media]"
                }
            )
            val payloadBytes = MessagePayloadCodec.encode(payload)
            val messageId = UUID.randomUUID().toString()

            val isDirect = conv.type == "direct"
            val recipientId = conv.otherUserId

            // Encrypt with libsignal
            val encResult = if (isDirect && recipientId != null) {
                ensureDirectSession(recipientId)
                signalManager.encryptDirectMessage(recipientId, payloadBytes)
            } else {
                signalManager.encryptGroupMessage(conversationId, payloadBytes)
            }

            // Save to Supabase if connected
            var remoteStatus = "sent"
            if (supabaseProvider.isConfigured) {
                try {
                    val client = supabaseProvider.getClient()
                    val dto = MessageDto(
                        id = messageId,
                        conversationId = conversationId,
                        senderId = myUserId,
                        recipientId = recipientId,
                        messageType = encResult.type,
                        encryptedBody = encResult.ciphertextBase64,
                        replyToId = replyToMessage?.id,
                        status = "sent"
                    )
                    client.postgrest["messages"].insert(dto)
                } catch (e: Exception) {
                    e.printStackTrace()
                    remoteStatus = "failed"
                }
            }

            // Save to Room
            val entity = MessageEntity(
                id = messageId,
                conversationId = conversationId,
                senderId = myUserId,
                senderName = securePrefs.getDisplayName() ?: "Me",
                recipientId = recipientId,
                plaintextBody = text.trim(),
                messageType = encResult.type,
                replyToId = replyToMessage?.id,
                replySnippet = payload.replySnippet,
                status = remoteStatus,
                timestamp = System.currentTimeMillis()
            )
            database.messageDao().insertOrUpdate(entity)
            database.conversationDao().updateLastMessage(conversationId, text.trim(), entity.timestamp)

            Result.success(entity)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Sends an E2EE media message (photo, video, voice note).
     * The file is encrypted locally via AES-256-GCM before upload.
     * The AES key and IV are stored strictly inside the libsignal ciphertext payload.
     */
    suspend fun sendMediaMessage(
        conversationId: String,
        file: File,
        mimeType: String,
        caption: String = "",
        durationSeconds: Int = 0,
        replyToMessage: MessageEntity? = null
    ): Result<MessageEntity> = withContext(Dispatchers.IO) {
        try {
            val conv = database.conversationDao().getConversationById(conversationId)
                ?: return@withContext Result.failure(IllegalStateException("Conversation not found"))

            val fileBytes = file.readBytes()
            val encMedia = MediaEncryptor.encrypt(fileBytes)
            val fileName = "${UUID.randomUUID()}.${file.extension}.enc"
            val storagePath = "$conversationId/$myUserId/$fileName"

            // Upload encrypted ciphertext file to Supabase Storage
            if (supabaseProvider.isConfigured) {
                val client = supabaseProvider.getClient()
                val bucket = client.storage["chat-media"]
                bucket.upload(storagePath, encMedia.encryptedBytes) {
                    upsert = true
                }
            }

            val mediaPayload = MediaPayload(
                path = storagePath,
                mimeType = mimeType,
                key = encMedia.keyBase64,
                iv = encMedia.ivBase64,
                durationSeconds = durationSeconds,
                fileName = file.name
            )

            val snippet = when {
                mimeType.startsWith("audio") -> "Voice Note (${durationSeconds}s)"
                mimeType.startsWith("video") -> "Video"
                else -> "Photo"
            }

            val payload = MessagePayload(
                text = caption.trim(),
                replyToId = replyToMessage?.id,
                replySnippet = replyToMessage?.plaintextBody?.take(60),
                media = mediaPayload
            )
            val payloadBytes = MessagePayloadCodec.encode(payload)
            val messageId = UUID.randomUUID().toString()

            val isDirect = conv.type == "direct"
            val recipientId = conv.otherUserId

            val encResult = if (isDirect && recipientId != null) {
                ensureDirectSession(recipientId)
                signalManager.encryptDirectMessage(recipientId, payloadBytes)
            } else {
                signalManager.encryptGroupMessage(conversationId, payloadBytes)
            }

            // Save to Supabase
            if (supabaseProvider.isConfigured) {
                val client = supabaseProvider.getClient()
                val dto = MessageDto(
                    id = messageId,
                    conversationId = conversationId,
                    senderId = myUserId,
                    recipientId = recipientId,
                    messageType = encResult.type,
                    encryptedBody = encResult.ciphertextBase64,
                    replyToId = replyToMessage?.id,
                    status = "sent"
                )
                client.postgrest["messages"].insert(dto)
            }

            val displayLabel = if (caption.isNotBlank()) caption.trim() else "[$snippet]"

            val entity = MessageEntity(
                id = messageId,
                conversationId = conversationId,
                senderId = myUserId,
                senderName = securePrefs.getDisplayName() ?: "Me",
                recipientId = recipientId,
                plaintextBody = caption.trim(),
                messageType = encResult.type,
                replyToId = replyToMessage?.id,
                replySnippet = payload.replySnippet,
                mediaPath = storagePath,
                mediaType = mimeType,
                mediaKey = encMedia.keyBase64,
                mediaIv = encMedia.ivBase64,
                mediaDuration = durationSeconds,
                localDecryptedPath = file.absolutePath,
                status = "sent",
                timestamp = System.currentTimeMillis()
            )
            database.messageDao().insertOrUpdate(entity)
            database.conversationDao().updateLastMessage(conversationId, displayLabel, entity.timestamp)

            Result.success(entity)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Downloads and decrypts media for an incoming message.
     */
    suspend fun downloadAndDecryptMedia(message: MessageEntity): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (message.localDecryptedPath != null && File(message.localDecryptedPath).exists()) {
                return@withContext Result.success(message.localDecryptedPath)
            }

            val path = message.mediaPath ?: return@withContext Result.failure(IllegalStateException("No media path"))
            val key = message.mediaKey ?: return@withContext Result.failure(IllegalStateException("No media key"))
            val iv = message.mediaIv ?: return@withContext Result.failure(IllegalStateException("No media iv"))

            if (!supabaseProvider.isConfigured) {
                return@withContext Result.failure(IllegalStateException("Supabase not configured"))
            }

            val client = supabaseProvider.getClient()
            val bucket = client.storage["chat-media"]
            val encryptedBytes = bucket.downloadAuthenticated(path)

            val decryptedBytes = MediaEncryptor.decrypt(encryptedBytes, key, iv)
            val ext = when {
                message.mediaType?.startsWith("image") == true -> "jpg"
                message.mediaType?.startsWith("video") == true -> "mp4"
                message.mediaType?.startsWith("audio") == true -> "m4a"
                else -> "bin"
            }
            val localDir = File(context.cacheDir, "decrypted_media").apply { mkdirs() }
            val localFile = File(localDir, "dec_${message.id}.$ext")
            localFile.writeBytes(decryptedBytes)

            database.messageDao().updateLocalDecryptedPath(message.id, localFile.absolutePath)
            Result.success(localFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Marks messages as read in the conversation and notifies Supabase receipts.
     */
    suspend fun markConversationAsRead(conversationId: String) = withContext(Dispatchers.IO) {
        try {
            database.conversationDao().clearUnreadCount(conversationId)
            database.messageDao().markAllIncomingAsRead(conversationId, myUserId, "read")

            if (supabaseProvider.isConfigured) {
                val client = supabaseProvider.getClient()
                val messages = client.postgrest["messages"].select {
                    filter {
                        eq("conversation_id", conversationId)
                        neq("sender_id", myUserId)
                    }
                }.decodeList<MessageDto>()

                for (msg in messages) {
                    val msgId = msg.id ?: continue
                    client.postgrest["message_receipts"].upsert(
                        MessageReceiptDto(
                            messageId = msgId,
                            userId = myUserId,
                            status = "read"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Updates typing status for this user in conversation.
     */
    suspend fun setTyping(conversationId: String, isTyping: Boolean) = withContext(Dispatchers.IO) {
        try {
            if (supabaseProvider.isConfigured) {
                val client = supabaseProvider.getClient()
                client.postgrest["typing_status"].upsert(
                    TypingStatusDto(
                        conversationId = conversationId,
                        userId = myUserId,
                        isTyping = isTyping
                    )
                )
            }
        } catch (_: Exception) {}
    }

    /**
     * Ensures an active libsignal session with a remote user via X3DH.
     */
    private suspend fun ensureDirectSession(recipientId: String) {
        val address = org.whispersystems.libsignal.SignalProtocolAddress(recipientId, 1)
        if (signalManager.store.containsSession(address)) {
            return
        }

        if (!supabaseProvider.isConfigured) {
            throw IllegalStateException("Supabase is not configured. Connect both devices in Settings.")
        }

        val client = supabaseProvider.getClient()
        val prekeyDtos = try {
            client.postgrest["prekeys"].select {
                filter {
                    eq("user_id", recipientId)
                }
            }.decodeList<PrekeyDto>()
        } catch (e: Exception) {
            e.printStackTrace()
            throw IllegalStateException("Failed to query recipient keys from Supabase: ${e.localizedMessage}", e)
        }

        val dto = prekeyDtos.firstOrNull()
            ?: throw IllegalStateException(
                "Recipient hasn't uploaded their encryption keys yet. Have them open the app to sync their keys with Supabase."
            )

        val bundle = PublicPrekeyBundle(
            identityKey = dto.identityKey,
            registrationId = dto.registrationId,
            signedPrekeyId = dto.signedPrekeyId,
            signedPrekey = dto.signedPrekey,
            signedPrekeySignature = dto.signedPrekeySignature,
            oneTimePrekeys = dto.oneTimePrekeys
        )

        signalManager.initializeSessionWithRecipient(recipientId, bundle)
    }

    /**
     * Subscribes to Supabase Realtime for messages, receipts, and typing changes.
     */
    fun startRealtimeSync() {
        if (!supabaseProvider.isConfigured) return
        realtimeChannelJob?.cancel()

        realtimeChannelJob = scope.launch {
            try {
                val client = supabaseProvider.getClient()
                val channel = client.realtime.channel("shieldchat-live-sync")

                // Listen to postgres changes on messages
                val messageFlow = channel.postgresChangeFlow<PostgresAction>(
                    schema = "public"
                ) {
                    table = "messages"
                }

                // Listen to receipts
                val receiptFlow = channel.postgresChangeFlow<PostgresAction>(
                    schema = "public"
                ) {
                    table = "message_receipts"
                }

                // Listen to typing
                val typingFlow = channel.postgresChangeFlow<PostgresAction>(
                    schema = "public"
                ) {
                    table = "typing_status"
                }

                client.realtime.connect()
                channel.subscribe()

                // Launch collectors
                launch {
                    messageFlow.collect { action ->
                        if (action is PostgresAction.Insert) {
                            try {
                                val jsonStr = action.record.toString()
                                val dto = Json { ignoreUnknownKeys = true }.decodeFromString<MessageDto>(jsonStr)
                                processIncomingMessage(dto)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                launch {
                    receiptFlow.collect { action ->
                        if (action is PostgresAction.Insert || action is PostgresAction.Update) {
                            try {
                                val record = when (action) {
                                    is PostgresAction.Insert -> action.record
                                    is PostgresAction.Update -> action.record
                                    else -> null
                                }
                                if (record != null) {
                                    val receipt = Json { ignoreUnknownKeys = true }.decodeFromString<MessageReceiptDto>(record.toString())
                                    database.messageDao().updateStatus(receipt.messageId, receipt.status)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                launch {
                    typingFlow.collect { action ->
                        try {
                            val record = when (action) {
                                is PostgresAction.Insert -> action.record
                                is PostgresAction.Update -> action.record
                                else -> null
                            }
                            if (record != null) {
                                val typing = Json { ignoreUnknownKeys = true }.decodeFromString<TypingStatusDto>(record.toString())
                                if (typing.userId != myUserId) {
                                    val currentMap = _typingUsers.value.toMutableMap()
                                    val currentSet = (currentMap[typing.conversationId] ?: emptySet()).toMutableSet()
                                    if (typing.isTyping) {
                                        currentSet.add(typing.userId)
                                    } else {
                                        currentSet.remove(typing.userId)
                                    }
                                    currentMap[typing.conversationId] = currentSet
                                    _typingUsers.value = currentMap
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Decrypts and saves an incoming message.
     */
    private suspend fun processIncomingMessage(dto: MessageDto) {
        val msgId = dto.id ?: return
        if (dto.senderId == myUserId) return // skip our own messages

        // Check if message already in Room
        if (database.messageDao().getMessageById(msgId) != null) return

        try {
            // Decrypt libsignal payload
            val conv = database.conversationDao().getConversationById(dto.conversationId)
            val isDirect = conv?.type == "direct" || dto.recipientId == myUserId

            val decryptedBytes = if (isDirect) {
                signalManager.decryptDirectMessage(dto.senderId, dto.messageType, dto.encryptedBody)
            } else {
                signalManager.decryptGroupMessage(dto.conversationId, dto.senderId, dto.encryptedBody)
            }

            val payload = MessagePayloadCodec.decode(decryptedBytes)

            // Lookup sender name
            val senderProfile = database.profileDao().getProfileById(dto.senderId)
            val senderName = senderProfile?.displayName ?: "Friend"

            val displaySnippet = when {
                payload.media != null -> when {
                    payload.media.mimeType.startsWith("audio") -> "Voice Note (${payload.media.durationSeconds}s)"
                    payload.media.mimeType.startsWith("video") -> "Video"
                    else -> "Photo"
                }
                payload.text.isNotBlank() -> payload.text
                else -> "[Encrypted Message]"
            }

            val entity = MessageEntity(
                id = msgId,
                conversationId = dto.conversationId,
                senderId = dto.senderId,
                senderName = senderName,
                recipientId = dto.recipientId,
                plaintextBody = payload.text,
                messageType = dto.messageType,
                replyToId = payload.replyToId,
                replySnippet = payload.replySnippet,
                mediaPath = payload.media?.path,
                mediaType = payload.media?.mimeType,
                mediaKey = payload.media?.key,
                mediaIv = payload.media?.iv,
                mediaDuration = payload.media?.durationSeconds ?: 0,
                status = "delivered",
                timestamp = System.currentTimeMillis()
            )

            database.messageDao().insertOrUpdate(entity)
            database.conversationDao().updateLastMessage(dto.conversationId, displaySnippet, entity.timestamp)
            database.conversationDao().incrementUnreadCount(dto.conversationId)

            // Auto download voice notes/media if needed
            if (payload.media != null) {
                scope.launch {
                    downloadAndDecryptMedia(entity)
                }
            }

            // Send delivered receipt
            if (supabaseProvider.isConfigured) {
                val client = supabaseProvider.getClient()
                client.postgrest["message_receipts"].upsert(
                    MessageReceiptDto(
                        messageId = msgId,
                        userId = myUserId,
                        status = "delivered"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // In case of decryption failure, save tombstone with visible error state
            val entity = MessageEntity(
                id = msgId,
                conversationId = dto.conversationId,
                senderId = dto.senderId,
                plaintextBody = "[Decryption Failed: Ratchet Key Mismatch]",
                messageType = dto.messageType,
                status = "failed",
                timestamp = System.currentTimeMillis()
            )
            database.messageDao().insertOrUpdate(entity)
        }
    }

    /**
     * Initial sync of conversations and messages from Supabase.
     */
    suspend fun syncAllFromSupabase(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!supabaseProvider.isConfigured) return@withContext Result.success(Unit)
        try {
            val client = supabaseProvider.getClient()

            // 1. Fetch conversations where user is a member
            val memberships = client.postgrest["conversation_members"].select {
                filter {
                    eq("user_id", myUserId)
                }
            }.decodeList<ConversationMemberDto>()

            for (m in memberships) {
                val convs = client.postgrest["conversations"].select {
                    filter {
                        eq("id", m.conversationId)
                    }
                }.decodeList<ConversationDto>()

                val convDto = convs.firstOrNull() ?: continue
                val existing = database.conversationDao().getConversationById(convDto.id ?: "")

                if (existing == null) {
                    var title = convDto.title
                    var otherUserId: String? = null
                    var otherUserTag: String? = null

                    if (convDto.type == "direct") {
                        // Find other member
                        val otherMembers = client.postgrest["conversation_members"].select {
                            filter {
                                eq("conversation_id", convDto.id ?: "")
                                neq("user_id", myUserId)
                            }
                        }.decodeList<ConversationMemberDto>()
                        val otherMember = otherMembers.firstOrNull()
                        if (otherMember != null) {
                            otherUserId = otherMember.userId
                            val otherProfiles = client.postgrest["profiles"].select {
                                filter {
                                    eq("id", otherMember.userId)
                                }
                            }.decodeList<com.example.data.remote.ProfileDto>()
                            val p = otherProfiles.firstOrNull()
                            title = p?.displayName ?: "Friend"
                            otherUserTag = p?.tag
                        }
                    }

                    database.conversationDao().insertOrUpdate(
                        ConversationEntity(
                            id = convDto.id ?: UUID.randomUUID().toString(),
                            type = convDto.type,
                            title = title,
                            otherUserId = otherUserId,
                            otherUserTag = otherUserTag,
                            lastMessageText = "Conversation ready",
                            lastMessageTimestamp = System.currentTimeMillis(),
                            unreadCount = 0
                        )
                    )
                }

                // Fetch group sender keys if group
                if (convDto.type == "group") {
                    val senderKeys = client.postgrest["group_sender_keys"].select {
                        filter {
                            eq("conversation_id", convDto.id ?: "")
                            eq("recipient_id", myUserId)
                        }
                    }.decodeList<GroupSenderKeyDto>()

                    for (sk in senderKeys) {
                        try {
                            // Decrypt distribution message using 1:1 direct session
                            val decryptedDistBytes = signalManager.decryptDirectMessage(
                                sk.senderId,
                                org.whispersystems.libsignal.protocol.CiphertextMessage.WHISPER_TYPE,
                                sk.encryptedDistributionMessage
                            )
                            val distB64 = String(decryptedDistBytes, Charsets.UTF_8)
                            signalManager.processGroupDistributionMessage(convDto.id ?: "", sk.senderId, distB64)
                        } catch (_: Exception) {}
                    }
                }

                // Fetch messages for conversation
                val messages = client.postgrest["messages"].select {
                    filter {
                        eq("conversation_id", convDto.id ?: "")
                    }
                }.decodeList<MessageDto>()

                for (msg in messages) {
                    processIncomingMessage(msg)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
