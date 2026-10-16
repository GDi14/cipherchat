package com.example.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.crypto.MediaEncryptor
import com.example.crypto.MediaPayload
import com.example.crypto.MessagePayload
import com.example.crypto.MessagePayloadCodec
import com.example.crypto.OneTimePrekeyItem
import com.example.crypto.PublicPrekeyBundle
import com.example.crypto.SignalProtocolManager
import com.example.crypto.TextEmphasisRange
import com.example.crypto.VoiceOfUserEmphasis
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.HiddenMessageEntity
import com.example.data.local.ProfileEntity
import com.example.data.local.SecurePreferencesManager
import com.example.data.remote.ConversationDto
import com.example.data.remote.ConversationMemberDto
import com.example.data.remote.GroupSenderKeyDto
import com.example.data.remote.MessageDto
import com.example.data.remote.MessageReceiptDto
import com.example.data.remote.ProfileDto
import com.example.data.remote.PrekeyDto
import com.example.data.remote.SupabaseClientProvider
import com.example.data.remote.TypingStatusDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.auth.auth
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.whispersystems.libsignal.SignalProtocolAddress
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class ChatRepository(
    private val context: Context,
    private val securePrefs: SecurePreferencesManager,
    private val signalManager: SignalProtocolManager,
    private val supabaseProvider: SupabaseClientProvider,
    private val database: AppDatabase
) {
    @Volatile private var activeConversationId: String? = null

    fun closeActiveConversation(conversationId: String) {
        if (activeConversationId == conversationId) activeConversationId = null
    }
    fun activateConversation(conversationId: String) {
        activeConversationId = conversationId
    }
    val myUserId: String get() = securePrefs.getOrCreateUserId()
    fun getMyDisplayName(): String = securePrefs.getDisplayName() ?: "Me"
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val encryptionMutex = Mutex()

    private fun messageTimestamp(createdAt: String?): Long {
        if (createdAt.isNullOrBlank() || createdAt.length < 19) return System.currentTimeMillis()
        val base = runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(createdAt.substring(0, 19))?.time
        }.getOrNull() ?: return System.currentTimeMillis()
        val fraction = createdAt.indexOf('.').takeIf { it >= 0 }?.let { dot ->
            createdAt.substring(dot + 1).takeWhile(Char::isDigit).take(3).padEnd(3, '0').toLongOrNull()
        } ?: 0L
        return base + fraction
    }

    // Typing status per conversation: conversationId -> set of userIds typing
    private val _typingUsers = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val typingUsers: StateFlow<Map<String, Set<String>>> = _typingUsers.asStateFlow()
    private val _voiceOfUserEnabled = MutableStateFlow(securePrefs.isVoiceOfUserEnabled())
    val voiceOfUserEnabled: StateFlow<Boolean> = _voiceOfUserEnabled.asStateFlow()
    private val _emphasisAnimationMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val emphasisAnimationMessageIds: StateFlow<Set<String>> = _emphasisAnimationMessageIds.asStateFlow()

    fun setVoiceOfUserEnabled(enabled: Boolean) {
        securePrefs.setVoiceOfUserEnabled(enabled)
        _voiceOfUserEnabled.value = enabled
    }

    fun completeEmphasisAnimation(messageId: String) {
        _emphasisAnimationMessageIds.value = _emphasisAnimationMessageIds.value - messageId
    }

    fun clearEmphasisAnimation(messageIds: Collection<String>) {
        _emphasisAnimationMessageIds.value = _emphasisAnimationMessageIds.value - messageIds.toSet()
    }

    private var realtimeChannelJob: Job? = null
    private val realtimeSyncLock = Any()

    fun getConversations(): Flow<List<ConversationEntity>> =
        database.conversationDao().getAllConversations()

    suspend fun peerFingerprint(peerUserId: String): String? {
        if (!supabaseProvider.isConfigured) return null
        val bundle = supabaseProvider.getClient().postgrest["prekeys"].select {
            filter { eq("user_id", peerUserId) }
        }.decodeList<PrekeyDto>().firstOrNull() ?: return null
        return signalManager.peerFingerprint(peerUserId, bundle.identityKey)
    }

    fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>> =
        database.messageDao().getLatestMessagesForConversation(convId, 300).map { it.asReversed() }

    suspend fun getOlderMessages(
        convId: String,
        beforeTimestamp: Long,
        beforeId: String,
        limit: Int
    ): List<MessageEntity> = withContext(Dispatchers.IO) {
        database.messageDao().getOlderMessages(convId, beforeTimestamp, beforeId, limit).asReversed()
    }

    suspend fun hideMessageLocally(messageId: String) = withContext(Dispatchers.IO) {
        database.messageDao().hideMessage(HiddenMessageEntity(messageId))
    }

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
                    ConversationDto(id = convId, type = "group", title = title, createdBy = myUserId)
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
        replyToMessage: MessageEntity? = null,
        messageId: String = UUID.randomUUID().toString(),
        timestamp: Long = System.currentTimeMillis()
    ): Result<MessageEntity> = withContext(Dispatchers.IO) {
        var optimisticMessageSaved = false
        try {
            val conv = database.conversationDao().getConversationById(conversationId)
                ?: return@withContext Result.failure(IllegalStateException("Conversation not found"))

            val textEmphasisRanges = if (_voiceOfUserEnabled.value) VoiceOfUserEmphasis.detect(text.trim()) else emptyList()
            val payload = MessagePayload(
                text = text.trim(),
                replyToId = replyToMessage?.id,
                replySnippet = replyToMessage?.let {
                    if (it.plaintextBody.isNotBlank()) it.plaintextBody.take(60) else "[Media]"
                },
                textEmphasisRanges = textEmphasisRanges
            )
            val payloadBytes = MessagePayloadCodec.encode(payload)

            // Persist a local sending bubble before encryption or network work so
            // the message list can render it immediately and keep it on failure.
            val optimisticMessage = MessageEntity(
                id = messageId,
                conversationId = conversationId,
                senderId = myUserId,
                senderName = securePrefs.getDisplayName() ?: "Me",
                recipientId = conv.otherUserId,
                plaintextBody = text.trim(),
                replyToId = replyToMessage?.id,
                replySnippet = payload.replySnippet,
                textEmphasisJson = MessagePayloadCodec.encodeEmphasisRanges(textEmphasisRanges),
                status = "sending",
                timestamp = timestamp
            )
            database.messageDao().insertOrUpdate(optimisticMessage)
            if (textEmphasisRanges.isNotEmpty()) _emphasisAnimationMessageIds.value += messageId
            database.conversationDao().updateLastMessage(conversationId, text.trim(), timestamp)
            optimisticMessageSaved = true

            val isDirect = conv.type == "direct"
            val recipientId = conv.otherUserId

            // Encrypt with libsignal
            val encResult = encryptionMutex.withLock {
                if (isDirect && recipientId != null) {
                    ensureDirectSession(recipientId)
                    signalManager.encryptDirectMessage(recipientId, payloadBytes)
                } else {
                    signalManager.encryptGroupMessage(conversationId, payloadBytes)
                }
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

            database.messageDao().updateOutgoingMessageState(
                id = messageId,
                recipientId = recipientId,
                messageType = encResult.type,
                status = remoteStatus
            )
            val entity = database.messageDao().getMessageById(messageId)
                ?: error("Optimistic message disappeared before send completed")

            Result.success(entity)
        } catch (e: Exception) {
            e.printStackTrace()
            if (optimisticMessageSaved) {
                runCatching { database.messageDao().updateStatus(messageId, "failed") }
            }
            _emphasisAnimationMessageIds.value -= messageId
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
        replyToMessage: MessageEntity? = null,
        messageId: String = UUID.randomUUID().toString()
    ): Result<MessageEntity> = withContext(Dispatchers.IO) {
        var optimisticMessageSaved = false
        try {
            val conv = database.conversationDao().getConversationById(conversationId)
                ?: return@withContext Result.failure(IllegalStateException("Conversation not found"))

            val timestamp = System.currentTimeMillis()
            val snippet = when {
                mimeType.startsWith("audio") -> "Voice Note (${durationSeconds}s)"
                mimeType.startsWith("video") -> "Video"
                else -> "Photo"
            }
            val replySnippet = replyToMessage?.let {
                if (it.plaintextBody.isNotBlank()) it.plaintextBody.take(60) else "[Media]"
            }
            val pendingEntity = MessageEntity(
                id = messageId,
                conversationId = conversationId,
                senderId = myUserId,
                senderName = securePrefs.getDisplayName() ?: "Me",
                recipientId = conv.otherUserId,
                plaintextBody = caption.trim(),
                replyToId = replyToMessage?.id,
                replySnippet = replySnippet,
                mediaType = mimeType,
                mediaDuration = durationSeconds,
                localDecryptedPath = file.absolutePath,
                status = "sending",
                timestamp = timestamp
            )
            database.messageDao().insertOrUpdate(pendingEntity)
            optimisticMessageSaved = true
            database.conversationDao().updateLastMessage(conversationId, "[$snippet]", timestamp)

            val fileBytes = file.readBytes()
            val encMedia = MediaEncryptor.encrypt(fileBytes)
            val fileName = "${UUID.randomUUID()}.${file.extension}.enc"
            val storagePath = "$conversationId/$myUserId/$fileName"

            // Upload encrypted ciphertext file to Supabase Storage
            if (supabaseProvider.isConfigured) {
                val client = supabaseProvider.getClient()
                val authUserId = client.auth.currentUserOrNull()?.id
                    ?: error("Your secure cloud session expired. Sign in again before sending media.")
                check(securePrefs.getSupabaseAuthUserId() == authUserId) {
                    "The signed-in account changed. Sign in again before sending media."
                }
                val linkedProfile = client.postgrest["profiles"].select {
                    filter { eq("auth_user_id", authUserId) }
                }.decodeList<ProfileDto>().firstOrNull()
                check(linkedProfile?.id == myUserId) {
                    "This device is not linked to the active chat profile. Sign in again to refresh the profile link."
                }
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

            val payload = MessagePayload(
                text = caption.trim(),
                replyToId = replyToMessage?.id,
                replySnippet = replySnippet,
                media = mediaPayload
            )
            val payloadBytes = MessagePayloadCodec.encode(payload)
            val isDirect = conv.type == "direct"
            val recipientId = conv.otherUserId

            val encResult = encryptionMutex.withLock {
                if (isDirect && recipientId != null) {
                    ensureDirectSession(recipientId)
                    signalManager.encryptDirectMessage(recipientId, payloadBytes)
                } else {
                    signalManager.encryptGroupMessage(conversationId, payloadBytes)
                }
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

            val displayLabel = if (caption.isNotBlank()) "$snippet · ${caption.trim()}" else "[$snippet]"

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
                timestamp = timestamp
            )
            database.messageDao().insertOrUpdate(entity)
            database.conversationDao().updateLastMessage(conversationId, displayLabel, entity.timestamp)

            Result.success(entity)
        } catch (e: Exception) {
            e.printStackTrace()
            if (optimisticMessageSaved) {
                runCatching { database.messageDao().updateStatus(messageId, "failed") }
            }
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
        activateConversation(conversationId)
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
        } catch (error: Exception) {
            Log.w("ChatSync", "Could not publish typing status", error)
        }
    }

    /**
     * Ensures an active libsignal session with a remote user via X3DH.
     */
    private suspend fun ensureDirectSession(recipientId: String, acceptIdentityChange: Boolean = false) {
        val address = org.whispersystems.libsignal.SignalProtocolAddress(recipientId, 1)
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

        if (signalManager.hasSessionForIdentity(recipientId, dto.identityKey)) return

        val previouslyTrustedIdentity = signalManager.store.getIdentity(address)
        val publishedIdentity = org.whispersystems.libsignal.IdentityKey(
            android.util.Base64.decode(dto.identityKey, android.util.Base64.NO_WRAP),
            0
        )
        if (previouslyTrustedIdentity != null && previouslyTrustedIdentity != publishedIdentity) {
            if (!acceptIdentityChange) {
                throw IllegalStateException(
                    "This contact's encryption key changed. Verify the safety number with them, then reset the secure session from their profile."
                )
            }
            signalManager.store.forgetIdentity(address)
        }
        // A session with no remembered peer identity is incomplete/corrupt; rebuild from the
        // currently published bundle. Keep the trusted identity unless the user explicitly reset.
        if (signalManager.store.containsSession(address)) signalManager.store.deleteSession(address)

        val oneTimePrekey = client.postgrest.rpc(
            "consume_one_time_prekey",
            mapOf("target_user_id" to recipientId)
        ).decodeList<OneTimePrekeyItem>().firstOrNull()
        val bundle = PublicPrekeyBundle(
            identityKey = dto.identityKey,
            registrationId = dto.registrationId,
            signedPrekeyId = dto.signedPrekeyId,
            signedPrekey = dto.signedPrekey,
            signedPrekeySignature = dto.signedPrekeySignature,
            oneTimePrekeys = listOfNotNull(oneTimePrekey)
        )

        signalManager.initializeSessionWithRecipient(recipientId, bundle)
    }

    /**
     * Explicitly resets a 1:1 secure session with a user and re-initializes it.
     */
    suspend fun resetSession(otherUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val address = SignalProtocolAddress(otherUserId, 1)
            signalManager.store.deleteSession(address)
            ensureDirectSession(otherUserId, acceptIdentityChange = true)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Subscribes to Supabase Realtime for messages, receipts, and typing changes.
     */
    fun startRealtimeSync() {
        if (!supabaseProvider.isConfigured) return
        // Do not bind the live channel to an anonymous startup session. On a fresh install
        // MainActivity is created before Google sign-in, so the channel must wait until the
        // Supabase Auth session exists; HomeViewModel calls this again after profile setup.
        if (supabaseProvider.getClient().auth.currentUserOrNull() == null) {
            Log.i("ChatSync", "Realtime startup deferred until Supabase Auth is signed in")
            return
        }
        synchronized(realtimeSyncLock) {
            // Foreground callbacks and ViewModel initialization can both request sync. Keep one
            // channel and its collectors alive instead of canceling/rejoining on every callback.
            if (realtimeChannelJob?.isActive == true) return

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

                // Start collecting before subscribing so the first database events cannot
                // arrive in the small gap between the channel joining and flow collection.
                channel.subscribe(blockUntilSubscribed = true)
                Log.i("ChatSync", "Supabase Realtime subscription joined")

                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("ChatSync", "Supabase Realtime subscription failed", e)
                }
            }
        }
    }

    /**
     * Decrypts and saves an incoming message.
     */
    private suspend fun processIncomingMessage(dto: MessageDto) {
        val msgId = dto.id ?: return
        if (dto.senderId == myUserId) return // skip our own messages

        // Retry messages with a prior decryption tombstone after keys/session state recover.
        val existingMessage = database.messageDao().getMessageById(msgId)
        if (existingMessage != null && existingMessage.status != "failed") return

        // A delivery receipt means this device fetched the encrypted message. Record it before
        // decryption so a local Signal/key problem does not leave the sender stuck at "sent".
        if (supabaseProvider.isConfigured) {
            runCatching {
                supabaseProvider.getClient().postgrest["message_receipts"].upsert(
                    MessageReceiptDto(messageId = msgId, userId = myUserId, status = "delivered")
                )
            }.onFailure { it.printStackTrace() }
        }

        try {
            // Decrypt libsignal payload
            val conv = database.conversationDao().getConversationById(dto.conversationId)
            val isDirect = conv?.type == "direct" || dto.recipientId == myUserId

            val decryptedBytes = try {
                if (isDirect) {
                    signalManager.decryptDirectMessage(dto.senderId, dto.messageType, dto.encryptedBody)
                } else {
                    signalManager.decryptGroupMessage(dto.conversationId, dto.senderId, dto.encryptedBody)
                }
            } catch (decryptionEx: Exception) {
                // A failed decrypt does not prove that the ratchet is stale. Deleting the
                // session here discards valid chain state and can make later messages unreadable.
                throw decryptionEx
            }

            val payload = MessagePayloadCodec.decode(decryptedBytes)

            // Lookup sender name
            val senderProfile = database.profileDao().getProfileById(dto.senderId)
            val senderName = senderProfile?.displayName ?: "Friend"
            val conversationIsVisible = activeConversationId == dto.conversationId

            val displaySnippet = when {
                payload.media != null -> when {
                    payload.media.mimeType.startsWith("audio") -> "Voice Note (${payload.media.durationSeconds}s)" + payload.text.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                    payload.media.mimeType.startsWith("video") -> "Video" + payload.text.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                    else -> "Photo" + payload.text.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
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
                textEmphasisJson = MessagePayloadCodec.encodeEmphasisRanges(payload.textEmphasisRanges),
                mediaPath = payload.media?.path,
                mediaType = payload.media?.mimeType,
                mediaKey = payload.media?.key,
                mediaIv = payload.media?.iv,
                mediaDuration = payload.media?.durationSeconds ?: 0,
                status = if (conversationIsVisible) "read" else "delivered",
                timestamp = messageTimestamp(dto.createdAt)
            )

            database.messageDao().insertOrUpdate(entity)
            if (conversationIsVisible && _voiceOfUserEnabled.value && payload.textEmphasisRanges.isNotEmpty()) {
                _emphasisAnimationMessageIds.value += msgId
            }
            database.conversationDao().updateLastMessage(dto.conversationId, displaySnippet, entity.timestamp)
            if (!conversationIsVisible) {
                database.conversationDao().incrementUnreadCount(dto.conversationId)
            } else {
                database.conversationDao().clearUnreadCount(dto.conversationId)
                database.messageDao().markAllIncomingAsRead(dto.conversationId, myUserId, "read")
            }

            // Auto download voice notes/media if needed
            if (payload.media != null) {
                scope.launch {
                    downloadAndDecryptMedia(entity)
                }
            }

            // A message arriving while its chat is visible is immediately read.
            if (conversationIsVisible && supabaseProvider.isConfigured) {
                runCatching {
                    supabaseProvider.getClient().postgrest["message_receipts"].upsert(
                        MessageReceiptDto(messageId = msgId, userId = myUserId, status = "read")
                    )
                }.onFailure { it.printStackTrace() }
            }
        } catch (e: Exception) {
            Log.e(
                "SignalDecrypt",
                "Could not decrypt incoming message type=${dto.messageType}; error=${e.javaClass.simpleName}: ${e.localizedMessage}",
                e
            )
            // Keep a retryable tombstone, but do not claim every failure is a ratchet mismatch.
            val entity = MessageEntity(
                id = msgId,
                conversationId = dto.conversationId,
                senderId = dto.senderId,
                plaintextBody = "[Unable to decrypt this message. Your security keys may have changed. Ask the sender to resend.]",
                messageType = dto.messageType,
                status = "failed",
                timestamp = messageTimestamp(dto.createdAt)
            )
            database.messageDao().insertOrUpdate(entity)
        }
    }

    /** Fetches one conversation on an FCM wake-up, falling back to full sync if uncached. */
    suspend fun syncConversationFromSupabase(conversationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!supabaseProvider.isConfigured) return@withContext Result.success(Unit)
        try {
            val conversation = database.conversationDao().getConversationById(conversationId)
                ?: return@withContext syncAllFromSupabase()
            val client = supabaseProvider.getClient()

            if (conversation.type == "group") {
                val senderKeys = client.postgrest["group_sender_keys"].select {
                    filter {
                        eq("conversation_id", conversationId)
                        eq("recipient_id", myUserId)
                    }
                }.decodeList<GroupSenderKeyDto>()
                for (senderKey in senderKeys) {
                    try {
                        val decrypted = signalManager.decryptDirectMessage(
                            senderKey.senderId,
                            org.whispersystems.libsignal.protocol.CiphertextMessage.WHISPER_TYPE,
                            senderKey.encryptedDistributionMessage
                        )
                        signalManager.processGroupDistributionMessage(
                            conversationId,
                            senderKey.senderId,
                            String(decrypted, Charsets.UTF_8)
                        )
                    } catch (_: Exception) {
                        // A previously consumed or unavailable group key is handled per message.
                    }
                }
            }

            val messages = client.postgrest["messages"].select {
                filter { eq("conversation_id", conversationId) }
            }.decodeList<MessageDto>()
            for (message in messages) processIncomingMessage(message)
            reconcileOutgoingReceipts(client, messages)
            Result.success(Unit)
        } catch (error: Exception) {
            error.printStackTrace()
            Result.failure(error)
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

                // Reconcile receipts even if the sender missed Realtime events while disconnected.
                reconcileOutgoingReceipts(client, messages)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun reconcileOutgoingReceipts(
        client: io.github.jan.supabase.SupabaseClient,
        messages: List<MessageDto>
    ) {
        val outgoingIds = messages.asSequence()
            .filter { it.senderId == myUserId }
            .mapNotNull { it.id }
            .toList()
        for (idBatch in outgoingIds.chunked(100)) {
            if (idBatch.isEmpty()) continue
            val idFilter = "(${idBatch.joinToString(",")})"
            val receipts = client.postgrest["message_receipts"].select {
                filter {
                    filter("message_id", FilterOperator.IN, idFilter)
                    neq("user_id", myUserId)
                }
            }.decodeList<MessageReceiptDto>()
            for ((messageId, messageReceipts) in receipts.groupBy { it.messageId }) {
                val status = if (messageReceipts.any { it.status == "read" }) "read" else "delivered"
                database.messageDao().updateStatus(messageId, status)
            }
        }
    }
}
