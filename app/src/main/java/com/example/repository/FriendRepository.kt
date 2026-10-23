package com.example.repository

import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.FriendRequestEntity
import com.example.data.local.ProfileEntity
import com.example.data.local.SecurePreferencesManager
import com.example.data.remote.ConversationDto
import com.example.data.remote.ConversationMemberDto
import com.example.data.remote.FriendRequestDto
import com.example.data.remote.ProfileDto
import com.example.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class FriendRepository(
    private val securePrefs: SecurePreferencesManager,
    private val supabaseProvider: SupabaseClientProvider,
    private val database: AppDatabase
) {
    val myUserId: String get() = securePrefs.getOrCreateUserId()

    fun getAcceptedFriends(): Flow<List<ProfileEntity>> = database.profileDao().getFriends()
    fun getPendingIncomingRequests(): Flow<List<FriendRequestEntity>> =
        database.friendRequestDao().getPendingIncomingRequests()
    fun getAllRequests(): Flow<List<FriendRequestEntity>> =
        database.friendRequestDao().getAllRequests()

    /** Returns the existing direct chat or creates one for an accepted friend. */
    suspend fun getOrCreateDirectConversation(friend: ProfileEntity): Result<String> = withContext(Dispatchers.IO) {
        try {
            require(friend.id != myUserId) { "You can't start a chat with your own profile." }
            database.conversationDao().getDirectConversation(friend.id)?.let {
                return@withContext Result.success(it.id)
            }

            if (supabaseProvider.isConfigured) {
                val client = supabaseProvider.getClient()
                val myMemberships = client.postgrest["conversation_members"].select {
                    filter { eq("user_id", myUserId) }
                }.decodeList<ConversationMemberDto>().mapTo(HashSet()) { it.conversationId }
                val friendMemberships = client.postgrest["conversation_members"].select {
                    filter { eq("user_id", friend.id) }
                }.decodeList<ConversationMemberDto>().mapTo(HashSet()) { it.conversationId }
                for (existingId in myMemberships intersect friendMemberships) {
                    val remote = client.postgrest["conversations"].select {
                        filter { eq("id", existingId) }
                    }.decodeList<ConversationDto>().firstOrNull() ?: continue
                    if (remote.type != "direct") continue
                    database.conversationDao().insertOrUpdate(
                        ConversationEntity(
                            id = existingId,
                            type = "direct",
                            title = friend.displayName,
                            otherUserId = friend.id,
                            otherUserTag = friend.tag,
                            lastMessageText = "Messages are end-to-end encrypted",
                            lastMessageTimestamp = System.currentTimeMillis()
                        )
                    )
                    return@withContext Result.success(existingId)
                }
            }

            val conversationId = UUID.randomUUID().toString()
            if (supabaseProvider.isConfigured) {
                val client = supabaseProvider.getClient()
                client.postgrest["conversations"].insert(
                    ConversationDto(id = conversationId, type = "direct", createdBy = myUserId)
                )
                client.postgrest["conversation_members"].insert(
                    listOf(
                        ConversationMemberDto(conversationId = conversationId, userId = myUserId),
                        ConversationMemberDto(conversationId = conversationId, userId = friend.id)
                    )
                )
            }

            database.conversationDao().insertOrUpdate(
                ConversationEntity(
                    id = conversationId,
                    type = "direct",
                    title = friend.displayName,
                    otherUserId = friend.id,
                    otherUserTag = friend.tag,
                    lastMessageText = "Messages are end-to-end encrypted",
                    lastMessageTimestamp = System.currentTimeMillis()
                )
            )
            Result.success(conversationId)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun getProfileById(userId: String): ProfileEntity? = withContext(Dispatchers.IO) {
        database.profileDao().getProfileById(userId)?.let { return@withContext it }
        if (!supabaseProvider.isConfigured) return@withContext null
        runCatching {
            val profile = supabaseProvider.getClient().postgrest["profiles"].select {
                filter { eq("id", userId) }
            }.decodeList<ProfileDto>().firstOrNull() ?: return@runCatching null
            ProfileEntity(
                id = profile.id,
                displayName = profile.displayName,
                tag = profile.tag,
                avatarPath = profile.avatarPath,
                isFriend = false
            ).also { database.profileDao().insertOrUpdate(it) }
        }.getOrNull()
    }

    suspend fun getRemoteProfileById(userId: String): ProfileDto? = withContext(Dispatchers.IO) {
        if (!supabaseProvider.isConfigured) return@withContext null
        runCatching {
            supabaseProvider.getClient().postgrest["profiles"].select {
                filter { eq("id", userId) }
            }.decodeList<ProfileDto>().firstOrNull()
        }.getOrNull()
    }

    /**
     * Search user by exact tag.
     */
    suspend fun searchUserByTag(rawTag: String): Result<ProfileEntity?> = withContext(Dispatchers.IO) {
        try {
            val cleanTag = rawTag.trim().removePrefix("@").lowercase()
            if (cleanTag.isBlank()) return@withContext Result.success(null)

            if (!supabaseProvider.isConfigured) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Supabase is not configured! Tap the Settings (gear) icon in the top right to enter your Supabase Project URL and Anon Key."
                    )
                )
            }

            val client = supabaseProvider.getClient()
            val list = client.postgrest["profiles"].select {
                filter {
                    eq("tag", cleanTag)
                }
            }.decodeList<ProfileDto>()

            if (list.isEmpty()) {
                // Try case-insensitive lookup
                val ilikeList = try {
                    client.postgrest["profiles"].select {
                        filter {
                            ilike("tag", cleanTag)
                        }
                    }.decodeList<ProfileDto>()
                } catch (e: Exception) {
                    emptyList()
                }

                if (ilikeList.isNotEmpty()) {
                    val match = ilikeList.firstOrNull { it.id != myUserId }
                    if (match != null) {
                        val profileEntity = ProfileEntity(
                            id = match.id,
                            displayName = match.displayName,
                            tag = match.tag,
                            avatarPath = match.avatarPath,
                            isFriend = false
                        )
                        database.profileDao().insertOrUpdate(profileEntity)
                        return@withContext Result.success(profileEntity)
                    }
                }

                return@withContext Result.success(null)
            }

            // Check if user accidentally searched their own tag
            val selfMatch = list.firstOrNull { it.id == myUserId }
            if (selfMatch != null && list.size == 1) {
                return@withContext Result.failure(
                    IllegalArgumentException("You entered your own tag (@$cleanTag). Please enter your friend's tag.")
                )
            }

            val match = list.firstOrNull { it.id != myUserId }
            if (match != null) {
                val profileEntity = ProfileEntity(
                    id = match.id,
                    displayName = match.displayName,
                    tag = match.tag,
                    avatarPath = match.avatarPath,
                    isFriend = false
                )
                // Cache found profile
                database.profileDao().insertOrUpdate(profileEntity)
                Result.success(profileEntity)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val hint = if (e.message?.contains("profiles") == true || e.message?.contains("42P01") == true || e.message?.contains("PGRST204") == true) {
                " (The 'profiles' table was not found in Supabase. Please execute the provided supabase_schema.sql script in your Supabase SQL editor.)"
            } else {
                ""
            }
            Result.failure(Exception("${e.localizedMessage ?: "Network or query error"}$hint"))
        }
    }

    /**
     * Sends a friend request to a target user.
     */
    suspend fun sendFriendRequest(targetUserId: String, targetName: String, targetTag: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (targetUserId == myUserId) {
                return@withContext Result.failure(IllegalArgumentException("Cannot send friend request to yourself."))
            }

            val requestId = UUID.randomUUID().toString()
            if (supabaseProvider.isConfigured) {
                val client = supabaseProvider.getClient()
                val dto = FriendRequestDto(
                    id = requestId,
                    senderId = myUserId,
                    receiverId = targetUserId,
                    status = "pending"
                )
                client.postgrest["friend_requests"].upsert(dto)
            }

            // Save outgoing request locally
            database.friendRequestDao().insertOrUpdate(
                FriendRequestEntity(
                    id = requestId,
                    senderId = myUserId,
                    receiverId = targetUserId,
                    senderName = targetName,
                    senderTag = targetTag,
                    status = "pending",
                    isIncoming = false,
                    avatarPath = database.profileDao().getProfileById(targetUserId)?.avatarPath,
                    timestamp = System.currentTimeMillis()
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Accepts a friend request, marking the user as friend and unlocking direct messaging.
     */
    suspend fun acceptFriendRequest(request: FriendRequestEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (supabaseProvider.isConfigured) {
                val client = supabaseProvider.getClient()
                client.postgrest["friend_requests"].update({
                    set("status", "accepted")
                }) {
                    filter {
                        eq("id", request.id)
                    }
                }
            }

            // Update local request status
            database.friendRequestDao().updateStatus(request.id, "accepted")

            // Determine friend ID and profile
            val friendId = if (request.isIncoming) request.senderId else request.receiverId
            database.profileDao().setFriendStatus(friendId, true)

            // Unlock and initialize DM conversation in Room
            val existingConv = database.conversationDao().getDirectConversation(friendId)
            if (existingConv == null) {
                val convId = UUID.randomUUID().toString()
                if (supabaseProvider.isConfigured) {
                    val client = supabaseProvider.getClient()
                    client.postgrest["conversations"].insert(
                        ConversationDto(id = convId, type = "direct", createdBy = myUserId)
                    )
                    client.postgrest["conversation_members"].insert(
                        listOf(
                            ConversationMemberDto(conversationId = convId, userId = myUserId),
                            ConversationMemberDto(conversationId = convId, userId = friendId)
                        )
                    )
                }

                database.conversationDao().insertOrUpdate(
                    ConversationEntity(
                        id = convId,
                        type = "direct",
                        title = request.senderName,
                        otherUserId = friendId,
                        otherUserTag = request.senderTag,
                        avatarPath = request.avatarPath,
                        lastMessageText = "Friend request accepted. Direct messaging unlocked!",
                        lastMessageTimestamp = System.currentTimeMillis(),
                        unreadCount = 0
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Declines a friend request.
     */
    suspend fun declineFriendRequest(requestId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (supabaseProvider.isConfigured) {
                val client = supabaseProvider.getClient()
                client.postgrest["friend_requests"].update({
                    set("status", "declined")
                }) {
                    filter {
                        eq("id", requestId)
                    }
                }
            }
            database.friendRequestDao().updateStatus(requestId, "declined")
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Syncs friend requests from Supabase.
     */
    suspend fun syncFriendRequests(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!supabaseProvider.isConfigured) return@withContext Result.success(Unit)
        try {
            val client = supabaseProvider.getClient()

            // Fetch incoming requests
            val incoming = client.postgrest["friend_requests"].select {
                filter {
                    eq("receiver_id", myUserId)
                }
            }.decodeList<FriendRequestDto>()

            for (req in incoming) {
                val reqId = req.id ?: continue
                // Lookup sender profile
                val senderProfiles = client.postgrest["profiles"].select {
                    filter {
                        eq("id", req.senderId)
                    }
                }.decodeList<ProfileDto>()
                val sender = senderProfiles.firstOrNull()
                val senderName = sender?.displayName ?: "Unknown"
                val senderTag = sender?.tag ?: ""

                database.friendRequestDao().insertOrUpdate(
                    FriendRequestEntity(
                        id = reqId,
                        senderId = req.senderId,
                        receiverId = req.receiverId,
                        senderName = senderName,
                        senderTag = senderTag,
                        status = req.status,
                        isIncoming = true,
                        avatarPath = sender?.avatarPath,
                        timestamp = System.currentTimeMillis()
                    )
                )

                if (req.status == "accepted") {
                    database.profileDao().insertOrUpdate(
                        ProfileEntity(
                            id = req.senderId,
                            displayName = senderName,
                            tag = senderTag,
                            avatarPath = sender?.avatarPath,
                            isFriend = true
                        )
                    )
                    database.conversationDao().getDirectConversation(req.senderId)?.let { conv ->
                        if (conv.avatarPath != sender?.avatarPath) {
                            database.conversationDao().insertOrUpdate(conv.copy(avatarPath = sender?.avatarPath))
                        }
                    }
                }
            }

            // Fetch outgoing requests
            val outgoing = client.postgrest["friend_requests"].select {
                filter {
                    eq("sender_id", myUserId)
                }
            }.decodeList<FriendRequestDto>()

            for (req in outgoing) {
                val reqId = req.id ?: continue
                val receiverProfiles = client.postgrest["profiles"].select {
                    filter {
                        eq("id", req.receiverId)
                    }
                }.decodeList<ProfileDto>()
                val receiver = receiverProfiles.firstOrNull()
                val receiverName = receiver?.displayName ?: "User"
                val receiverTag = receiver?.tag ?: ""

                database.friendRequestDao().insertOrUpdate(
                    FriendRequestEntity(
                        id = reqId,
                        senderId = req.senderId,
                        receiverId = req.receiverId,
                        senderName = receiverName,
                        senderTag = receiverTag,
                        status = req.status,
                        isIncoming = false,
                        avatarPath = receiver?.avatarPath,
                        timestamp = System.currentTimeMillis()
                    )
                )

                if (req.status == "accepted") {
                    database.profileDao().insertOrUpdate(
                        ProfileEntity(
                            id = req.receiverId,
                            displayName = receiverName,
                            tag = receiverTag,
                            avatarPath = receiver?.avatarPath,
                            isFriend = true
                        )
                    )
                    database.conversationDao().getDirectConversation(req.receiverId)?.let { conv ->
                        if (conv.avatarPath != receiver?.avatarPath) {
                            database.conversationDao().insertOrUpdate(conv.copy(avatarPath = receiver?.avatarPath))
                        }
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
