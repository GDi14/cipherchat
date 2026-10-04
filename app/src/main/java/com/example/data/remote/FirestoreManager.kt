package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.model.PreKeyBundleDto
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class FirestoreUser(
    val userId: String = "",
    val displayName: String = "",
    val isOnline: Boolean = true,
    val typingTo: String? = null,
    val updatedAt: Long = 0L
)

data class ContactRequestDto(
    val requestId: String = "",
    val senderUserId: String = "",
    val senderDisplayName: String = "",
    val recipientUserId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "pending"
)

data class RemoteMessage(
    val messageId: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    val ciphertext: String = "",
    val signalType: Int = 2,
    val timestamp: Long = 0L,
    val status: String = "sent",
    val replyToId: String? = null,
    val replySnippet: String? = null
)

class FirestoreManager(
    private val context: Context? = null
) {
    companion object {
        private const val TAG = "FirestoreManager"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_MESSAGES = "messages"
        private const val COLLECTION_CONTACT_REQUESTS = "contact_requests"
    }

    private fun ensureFirebaseApp() {
        if (context != null && FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:498267109913:android:67aa3c5ac2664f13f14888")
                .setProjectId("cipherchat-4a819")
                .setApiKey("AIzaSyCWHhXvpBPRt7_LDZc7EaqB8K04hjri4DY")
                .build()
            FirebaseApp.initializeApp(context, options)
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            ensureFirebaseApp()
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization handled safely", e)
            null
        }
    }

    private val auth: FirebaseAuth? by lazy {
        try {
            ensureFirebaseApp()
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth initialization notice", e)
            null
        }
    }

    suspend fun ensureAuthenticated() = withContext(Dispatchers.IO) {
        val a = auth ?: return@withContext
        try {
            if (a.currentUser == null) {
                a.signInAnonymously().await()
                Log.d(TAG, "Authenticated anonymously for Firestore session: ${a.currentUser?.uid}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Anonymous auth notice (proceeding with session): ${e.message}")
        }
    }

    suspend fun publishUserBundle(
        userId: String,
        displayName: String,
        registrationId: Int,
        identityKeyBase64: String,
        signedPreKeyId: Int,
        signedPreKeyPublicBase64: String,
        signedPreKeySignatureBase64: String,
        preKeys: Map<String, String>
    ) {
        val db = firestore ?: return
        try {
            ensureAuthenticated()
            val data = hashMapOf(
                "userId" to userId,
                "displayName" to displayName,
                "registrationId" to registrationId,
                "identityKey" to identityKeyBase64,
                "signedPreKeyId" to signedPreKeyId,
                "signedPreKey" to signedPreKeyPublicBase64,
                "signedPreKeySignature" to signedPreKeySignatureBase64,
                "preKeys" to preKeys,
                "isOnline" to true,
                "typingTo" to null,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_USERS).document(userId).set(data).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Notice publishing user bundle: ${e.message}")
        }
    }

    suspend fun fetchUserPreKeyBundle(userId: String): PreKeyBundleDto? {
        val db = firestore ?: return null
        return try {
            val doc = db.collection(COLLECTION_USERS).document(userId).get().await()
            if (!doc.exists()) return null

            val regId = (doc.getLong("registrationId") ?: 0L).toInt()
            val identityKey = doc.getString("identityKey").orEmpty()
            val signedPreKeyId = (doc.getLong("signedPreKeyId") ?: 0L).toInt()
            val signedPreKey = doc.getString("signedPreKey").orEmpty()
            val signedPreKeySig = doc.getString("signedPreKeySignature").orEmpty()

            // Safely read preKeys as Map<String, String> keyed by preKeyId string
            val rawPreKeys = doc.get("preKeys")
            val preKeyMap = mutableMapOf<String, String>()
            when (rawPreKeys) {
                is Map<*, *> -> {
                    for ((k, v) in rawPreKeys) {
                        if (k != null && v is String) {
                            preKeyMap[k.toString()] = v
                        }
                    }
                }
                is List<*> -> {
                    // Safe fallback for legacy list format without crashing
                    for (item in rawPreKeys) {
                        if (item is Map<*, *>) {
                            val id = item["keyId"]?.toString()
                            val key = item["publicKey"] as? String
                            if (id != null && key != null) {
                                preKeyMap[id] = key
                            }
                        }
                    }
                }
            }

            PreKeyBundleDto(
                registrationId = regId,
                identityKey = identityKey,
                signedPreKeyId = signedPreKeyId,
                signedPreKey = signedPreKey,
                signedPreKeySignature = signedPreKeySig,
                preKeys = preKeyMap
            )
        } catch (e: Exception) {
            Log.w(TAG, "Notice fetching user prekey bundle: ${e.message}")
            null
        }
    }

    fun observeGroupUsers(currentUserId: String): Flow<List<FirestoreUser>> {
        val db = firestore ?: return flowOf(emptyList())
        return callbackFlow {
            val subscription = db.collection(COLLECTION_USERS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            Log.w(TAG, "Group users permission restricted by rules")
                        } else {
                            Log.w(TAG, "Notice observing users: ${error.message}")
                        }
                        return@addSnapshotListener
                    }
                    val users = snapshot?.documents?.mapNotNull { doc ->
                        val id = doc.getString("userId") ?: doc.id
                        if (id == currentUserId) return@mapNotNull null
                        FirestoreUser(
                            userId = id,
                            displayName = doc.getString("displayName") ?: id,
                            isOnline = doc.getBoolean("isOnline") ?: false,
                            typingTo = doc.getString("typingTo"),
                            updatedAt = doc.getLong("updatedAt") ?: 0L
                        )
                    }.orEmpty()
                    trySend(users)
                }
            awaitClose { subscription.remove() }
        }
    }

    fun observeUserStatus(userId: String): Flow<Pair<Boolean, String?>> {
        val db = firestore ?: return flowOf(Pair(false, null))
        return callbackFlow {
            val subscription = db.collection(COLLECTION_USERS).document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val isOnline = snapshot.getBoolean("isOnline") ?: false
                    val typingTo = snapshot.getString("typingTo")
                    trySend(Pair(isOnline, typingTo))
                }
            awaitClose { subscription.remove() }
        }
    }

    suspend fun setTypingStatus(currentUserId: String, typingToRecipientId: String?) {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_USERS).document(currentUserId)
                .update("typingTo", typingToRecipientId)
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Notice updating typing status: ${e.message}")
        }
    }

    suspend fun setUserOnlineStatus(currentUserId: String, isOnline: Boolean) {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_USERS).document(currentUserId)
                .update(
                    mapOf(
                        "isOnline" to isOnline,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Notice updating online status: ${e.message}")
        }
    }

    suspend fun sendEncryptedMessage(
        messageId: String,
        senderId: String,
        recipientId: String,
        ciphertext: String,
        signalType: Int,
        timestamp: Long,
        replyToId: String?,
        replySnippet: String?
    ) {
        val db = firestore ?: return
        try {
            val data = hashMapOf(
                "messageId" to messageId,
                "senderId" to senderId,
                "recipientId" to recipientId,
                "ciphertext" to ciphertext,
                "signalType" to signalType,
                "timestamp" to timestamp,
                "status" to "sent",
                "replyToId" to replyToId,
                "replySnippet" to replySnippet
            )
            db.collection(COLLECTION_MESSAGES).document(messageId).set(data).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Notice sending remote message: ${e.message}")
        }
    }

    fun listenForIncomingMessages(
        currentUserId: String,
        onMessagesReceived: (List<RemoteMessage>) -> Unit
    ): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection(COLLECTION_MESSAGES)
                .whereEqualTo("recipientId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            Log.w(TAG, "Incoming messages permission restricted by Firestore rules")
                        } else {
                            Log.w(TAG, "Notice in incoming message listener: ${error.message}")
                        }
                        return@addSnapshotListener
                    }
                    val messages = snapshot?.documents?.mapNotNull { it.toRemoteMessage() }
                        .orEmpty()
                        .sortedBy { it.timestamp }
                    if (messages.isNotEmpty()) {
                        onMessagesReceived(messages)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Notice starting incoming listener: ${e.message}")
            null
        }
    }

    suspend fun markMessageDelivered(messageId: String) {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_MESSAGES).document(messageId)
                .update("status", "delivered")
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Notice marking delivered: ${e.message}")
        }
    }

    suspend fun markMessageRead(messageId: String) {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_MESSAGES).document(messageId)
                .update("status", "read")
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Notice marking read: ${e.message}")
        }
    }

    suspend fun updateDisplayName(userId: String, newDisplayName: String) {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_USERS).document(userId)
                .update("displayName", newDisplayName)
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Notice updating display name: ${e.message}")
        }
    }

    suspend fun searchUserById(userIdOrCode: String): FirestoreUser? {
        val db = firestore ?: return null
        return try {
            val doc = db.collection(COLLECTION_USERS).document(userIdOrCode.trim()).get().await()
            if (doc.exists()) {
                FirestoreUser(
                    userId = doc.id,
                    displayName = doc.getString("displayName") ?: doc.id,
                    isOnline = doc.getBoolean("isOnline") ?: false,
                    typingTo = doc.getString("typingTo"),
                    updatedAt = doc.getLong("updatedAt") ?: 0L
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice searching user by ID: ${e.message}")
            null
        }
    }

    suspend fun sendContactRequest(
        senderUserId: String,
        senderDisplayName: String,
        recipientUserId: String
    ): Boolean {
        val db = firestore ?: return false
        return try {
            val requestId = "${senderUserId}_to_${recipientUserId}"
            val data = hashMapOf(
                "requestId" to requestId,
                "senderUserId" to senderUserId,
                "senderDisplayName" to senderDisplayName,
                "recipientUserId" to recipientUserId,
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending"
            )
            db.collection(COLLECTION_CONTACT_REQUESTS).document(requestId).set(data).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Notice sending contact request: ${e.message}")
            false
        }
    }

    fun observePendingContactRequests(userId: String): Flow<List<ContactRequestDto>> {
        val db = firestore ?: return flowOf(emptyList())
        return callbackFlow {
            val subscription = db.collection(COLLECTION_CONTACT_REQUESTS)
                .whereEqualTo("recipientUserId", userId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val requests = snapshot.documents.mapNotNull { doc ->
                        ContactRequestDto(
                            requestId = doc.getString("requestId") ?: doc.id,
                            senderUserId = doc.getString("senderUserId").orEmpty(),
                            senderDisplayName = doc.getString("senderDisplayName").orEmpty(),
                            recipientUserId = doc.getString("recipientUserId").orEmpty(),
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            status = doc.getString("status") ?: "pending"
                        )
                    }
                    trySend(requests)
                }
            awaitClose { subscription.remove() }
        }
    }

    suspend fun acceptContactRequest(requestId: String) {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_CONTACT_REQUESTS).document(requestId)
                .update("status", "accepted")
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Notice accepting contact request: ${e.message}")
        }
    }

    suspend fun declineContactRequest(requestId: String) {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_CONTACT_REQUESTS).document(requestId)
                .update("status", "declined")
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Notice declining contact request: ${e.message}")
        }
    }

    private fun DocumentSnapshot.toRemoteMessage(): RemoteMessage? {
        val mid = getString("messageId") ?: id
        val sender = getString("senderId") ?: return null
        val recipient = getString("recipientId") ?: return null
        val ciphertext = getString("ciphertext") ?: return null
        val type = (getLong("signalType") ?: 2L).toInt()
        val ts = getLong("timestamp") ?: System.currentTimeMillis()
        val st = getString("status") ?: "sent"
        val replyId = getString("replyToId")
        val snippet = getString("replySnippet")

        return RemoteMessage(
            messageId = mid,
            senderId = sender,
            recipientId = recipient,
            ciphertext = ciphertext,
            signalType = type,
            timestamp = ts,
            status = st,
            replyToId = replyId,
            replySnippet = snippet
        )
    }
}
