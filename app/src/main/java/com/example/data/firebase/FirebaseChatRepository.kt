package com.example.data.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.crypto.AttachmentCrypto
import com.example.data.crypto.EncryptedFileData
import com.example.data.local.dao.ContactDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.entity.LocalContactEntity
import com.example.data.local.entity.LocalMessageEntity
import com.example.data.signal.PublicPreKeyBundleData
import com.example.data.signal.SignalProtocolManager
import com.example.model.DecryptedContent
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed class StorageUploadResult {
    data class Success(val storagePath: String) : StorageUploadResult()
    data class Failure(val userFacingReason: String) : StorageUploadResult()
}

class FirebaseChatRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val messageDao: MessageDao,
    private val contactDao: ContactDao,
    private val userProfileDao: UserProfileDao,
    private val signalProtocolManager: SignalProtocolManager
) {
    private val tag = "FirebaseChatRepo"
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var incomingMessagesListener: ListenerRegistration? = null
    private var sentMessagesListener: ListenerRegistration? = null
    private var contactsListener: ListenerRegistration? = null

    // Track active chat to mark messages as read immediately
    var activeChatUserId: String? = null

    suspend fun publishUserIdentity(bundle: PublicPreKeyBundleData, fcmToken: String?) = withContext(Dispatchers.IO) {
        val userMap = hashMapOf<String, Any>(
            "userId" to bundle.userId,
            "displayName" to bundle.displayName,
            "registrationId" to bundle.registrationId,
            "identityKey" to bundle.identityKey,
            "signedPreKeyId" to bundle.signedPreKeyId,
            "signedPreKey" to bundle.signedPreKey,
            "signedPreKeySignature" to bundle.signedPreKeySignature,
            "preKeyId" to bundle.preKeyId,
            "preKey" to bundle.preKey,
            "isOnline" to true,
            "lastSeen" to System.currentTimeMillis()
        )
        if (!fcmToken.isNullOrEmpty()) {
            userMap["fcmToken"] = fcmToken
        }
        firestore.collection("users").document(bundle.userId)
            .set(userMap, SetOptions.merge())
            .await()
    }

    suspend fun updateFcmToken(userId: String, token: String) = withContext(Dispatchers.IO) {
        userProfileDao.updateFcmToken(userId, token)
        try {
            firestore.collection("users").document(userId)
                .update("fcmToken", token)
                .await()
        } catch (e: Exception) {
            Log.w(tag, "Failed to update FCM token on Firestore", e)
        }
    }

    suspend fun updatePresence(isOnline: Boolean) = withContext(Dispatchers.IO) {
        val profile = userProfileDao.getUserProfile() ?: return@withContext
        try {
            firestore.collection("users").document(profile.userId)
                .update(
                    mapOf(
                        "isOnline" to isOnline,
                        "lastSeen" to System.currentTimeMillis()
                    )
                ).await()
        } catch (e: Exception) {
            Log.w(tag, "Failed to update presence", e)
        }
    }

    suspend fun fetchRecipientBundle(recipientId: String): PublicPreKeyBundleData? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("users").document(recipientId).get().await()
            if (!doc.exists()) return@withContext null

            PublicPreKeyBundleData(
                userId = doc.getString("userId") ?: recipientId,
                displayName = doc.getString("displayName") ?: "Friend",
                registrationId = (doc.getLong("registrationId") ?: 0L).toInt(),
                identityKey = doc.getString("identityKey") ?: "",
                signedPreKeyId = (doc.getLong("signedPreKeyId") ?: 0L).toInt(),
                signedPreKey = doc.getString("signedPreKey") ?: "",
                signedPreKeySignature = doc.getString("signedPreKeySignature") ?: "",
                preKeyId = (doc.getLong("preKeyId") ?: 0L).toInt(),
                preKey = doc.getString("preKey") ?: ""
            )
        } catch (e: Exception) {
            Log.e(tag, "Error fetching recipient bundle for $recipientId", e)
            null
        }
    }

    fun startListening(currentUserId: String) {
        stopListening()

        try {
            // 1. Listen for incoming messages addressed to current user
            incomingMessagesListener = firestore.collection("messages")
                .whereEqualTo("recipientId", currentUserId)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w(tag, "Notice: Incoming messages listener: ${e.message}")
                        return@addSnapshotListener
                    }
                    snapshots?.let { snapshot ->
                        for (dc in snapshot.documentChanges) {
                            if (dc.type == DocumentChange.Type.ADDED || dc.type == DocumentChange.Type.MODIFIED) {
                                val data = dc.document.data
                                val messageId = dc.document.id
                                val senderId = data["senderId"] as? String ?: continue
                                val encryptedPayload = data["encryptedPayload"] as? String ?: continue
                                val messageType = (data["messageType"] as? Long)?.toInt() ?: 3
                                val timestamp = (data["timestamp"] as? Long) ?: System.currentTimeMillis()
                                val remoteStatus = data["status"] as? String ?: "sent"

                                repoScope.launch {
                                    handleIncomingMessage(
                                        messageId = messageId,
                                        senderId = senderId,
                                        recipientId = currentUserId,
                                        encryptedPayload = encryptedPayload,
                                        messageType = messageType,
                                        timestamp = timestamp,
                                        remoteStatus = remoteStatus
                                    )
                                }
                            }
                        }
                    }
                }

            // 2. Listen for status updates on sent messages (delivered, read)
            sentMessagesListener = firestore.collection("messages")
                .whereEqualTo("senderId", currentUserId)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w(tag, "Notice: Sent messages listener: ${e.message}")
                        return@addSnapshotListener
                    }
                    snapshots?.let { snapshot ->
                        for (dc in snapshot.documentChanges) {
                            val messageId = dc.document.id
                            val status = dc.document.getString("status") ?: continue
                            repoScope.launch {
                                messageDao.updateMessageStatus(messageId, status.uppercase())
                            }
                        }
                    }
                }

            // 3. Listen to users collection to discover friends and update presence
            contactsListener = firestore.collection("users")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w(tag, "Notice: Contacts listener: ${e.message}")
                        return@addSnapshotListener
                    }
                    snapshots?.let { snapshot ->
                        repoScope.launch {
                            val contacts = snapshot.documents.mapNotNull { doc ->
                                val uid = doc.getString("userId") ?: return@mapNotNull null
                                if (uid == currentUserId) return@mapNotNull null
                                val name = doc.getString("displayName") ?: "Friend"
                                val idKey = doc.getString("identityKey") ?: ""
                                val isOnline = doc.getBoolean("isOnline") ?: false
                                val lastSeen = doc.getLong("lastSeen") ?: 0L

                                val existing = contactDao.getContactById(uid)
                                LocalContactEntity(
                                    userId = uid,
                                    displayName = name,
                                    identityKey = idKey,
                                    lastMessageText = existing?.lastMessageText ?: "",
                                    lastMessageTimestamp = existing?.lastMessageTimestamp ?: 0L,
                                    unreadCount = existing?.unreadCount ?: 0,
                                    isOnline = isOnline,
                                    lastSeen = lastSeen
                                )
                            }
                            contactDao.insertOrUpdateContacts(contacts)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Error attaching Firestore listeners (ensure google-services.json is configured)", e)
        }
    }

    fun stopListening() {
        incomingMessagesListener?.remove()
        incomingMessagesListener = null
        sentMessagesListener?.remove()
        sentMessagesListener = null
        contactsListener?.remove()
        contactsListener = null
    }

    private suspend fun handleIncomingMessage(
        messageId: String,
        senderId: String,
        recipientId: String,
        encryptedPayload: String,
        messageType: Int,
        timestamp: Long,
        remoteStatus: String
    ) {
        val existing = messageDao.getMessageById(messageId)
        if (existing != null) {
            // Already decrypted and stored
            return
        }

        try {
            var senderIdentityKey = contactDao.getContactById(senderId)?.identityKey
            if (senderIdentityKey.isNullOrBlank()) {
                val bundle = fetchRecipientBundle(senderId)
                senderIdentityKey = bundle?.identityKey
            }

            // Decrypt with Signal Protocol / Standard EC
            val decryptedPlaintext = signalProtocolManager.decryptMessage(
                senderId = senderId,
                base64Ciphertext = encryptedPayload,
                messageType = messageType,
                senderIdentityKeyBase64 = senderIdentityKey
            )

            val content = DecryptedContent.fromJsonString(decryptedPlaintext)
            val isChatActive = (activeChatUserId == senderId)
            val initialStatus = if (isChatActive) "READ" else "DELIVERED"

            var previewText = ""
            var hasAttachment = false
            var attachmentPath: String? = null
            var fileName: String? = null
            var mimeType: String? = null
            var fileSize: Long = 0L

            when (content) {
                is DecryptedContent.Text -> {
                    previewText = content.text
                }
                is DecryptedContent.Attachment -> {
                    hasAttachment = true
                    previewText = if (content.caption.isNotBlank()) content.caption else "[File: ${content.fileName}]"
                    fileName = content.fileName
                    mimeType = content.mimeType
                    fileSize = content.fileSize

                    // Asynchronously download & decrypt the attachment
                    downloadAndDecryptAttachment(
                        messageId = messageId,
                        storagePath = content.storagePath,
                        fileName = content.fileName,
                        aesKey = content.aesKey,
                        aesIv = content.aesIv
                    )
                }
            }

            val textContent = content as? DecryptedContent.Text

            val entity = LocalMessageEntity(
                id = messageId,
                conversationUserId = senderId,
                senderId = senderId,
                recipientId = recipientId,
                plaintext = previewText,
                timestamp = timestamp,
                status = initialStatus,
                hasAttachment = hasAttachment,
                attachmentUrl = (content as? DecryptedContent.Attachment)?.storagePath,
                attachmentLocalPath = attachmentPath,
                attachmentMimeType = mimeType,
                attachmentFileName = fileName,
                attachmentFileSize = fileSize,
                isOutgoing = false,
                replyToMessageId = textContent?.replyToMessageId,
                replyToSenderName = textContent?.replyToSenderName,
                replyToText = textContent?.replyToText
            )

            messageDao.insertMessage(entity)

            // Update contact in local DB
            contactDao.updateLastMessage(senderId, previewText, timestamp)
            if (!isChatActive) {
                contactDao.incrementUnread(senderId)
            }

            // Update remote Firestore status to delivered or read
            val newRemoteStatus = if (isChatActive) "read" else "delivered"
            if (remoteStatus != newRemoteStatus) {
                firestore.collection("messages").document(messageId)
                    .update("status", newRemoteStatus)
                    .addOnFailureListener { Log.w(tag, "Failed to update message status to $newRemoteStatus") }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to decrypt message $messageId from $senderId", e)
        }
    }

    suspend fun markChatAsRead(friendUserId: String) = withContext(Dispatchers.IO) {
        contactDao.resetUnread(friendUserId)
        messageDao.markIncomingMessagesAsRead(friendUserId)
        val currentUserId = userProfileDao.getUserProfile()?.userId ?: return@withContext

        // Update unread messages on Firestore
        try {
            val unreadSnapshot = firestore.collection("messages")
                .whereEqualTo("recipientId", currentUserId)
                .whereEqualTo("senderId", friendUserId)
                .whereNotEqualTo("status", "read")
                .get()
                .await()

            for (doc in unreadSnapshot.documents) {
                doc.reference.update("status", "read")
            }
        } catch (e: Exception) {
            Log.w(tag, "Error updating read status on Firestore", e)
        }
    }

    suspend fun sendTextMessage(
        recipientUserId: String,
        text: String,
        replyToMessageId: String? = null,
        replyToSenderName: String? = null,
        replyToText: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userProfile = userProfileDao.getUserProfile()
                ?: return@withContext Result.failure(IllegalStateException("User not logged in"))

            val recipientBundle = fetchRecipientBundle(recipientUserId)
                ?: return@withContext Result.failure(IllegalStateException("Recipient not found or not yet registered on Firebase"))

            // Encrypt using Signal Protocol / Standard EC
            val payload = DecryptedContent.Text(
                text = text,
                replyToMessageId = replyToMessageId,
                replyToSenderName = replyToSenderName,
                replyToText = replyToText
            ).toJsonString()
            val encrypted = signalProtocolManager.encryptMessage(recipientBundle, payload)

            val messageId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            // Save in local DB immediately as SENDING
            val localEntity = LocalMessageEntity(
                id = messageId,
                conversationUserId = recipientUserId,
                senderId = userProfile.userId,
                recipientId = recipientUserId,
                plaintext = text,
                timestamp = now,
                status = "SENDING",
                hasAttachment = false,
                isOutgoing = true,
                replyToMessageId = replyToMessageId,
                replyToSenderName = replyToSenderName,
                replyToText = replyToText
            )
            messageDao.insertMessage(localEntity)
            contactDao.updateLastMessage(recipientUserId, text, now)

            // Write encrypted blob to Firestore
            val messageData = hashMapOf(
                "id" to messageId,
                "senderId" to userProfile.userId,
                "recipientId" to recipientUserId,
                "encryptedPayload" to encrypted.base64Ciphertext,
                "messageType" to encrypted.messageType,
                "timestamp" to now,
                "status" to "sent"
            )

            firestore.collection("messages").document(messageId)
                .set(messageData)
                .await()

            messageDao.updateMessageStatus(messageId, "SENT")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error sending message", e)
            Result.failure(e)
        }
    }

    suspend fun uploadEncryptedAttachment(
        uri: Uri,
        fileName: String
    ): Pair<StorageUploadResult, EncryptedFileData?> = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Pair(
                    StorageUploadResult.Failure("Unable to read selected file"),
                    null
                )

            // AES-256-GCM symmetric encryption on device before upload!
            val encryptedData = AttachmentCrypto.encryptBytes(bytes)
            val fileId = UUID.randomUUID().toString()
            val storagePath = "attachments/$fileId.bin"

            val storageRef = FirebaseStorage.getInstance().reference.child(storagePath)

            try {
                storageRef.putBytes(encryptedData.encryptedBytes).await()
                Pair(StorageUploadResult.Success(storagePath), encryptedData)
            } catch (storageEx: StorageException) {
                Log.e(tag, "Firebase Storage exception", storageEx)
                Pair(
                    StorageUploadResult.Failure("File sharing isn't set up yet — enable Firebase Storage in the console (Blaze plan required)"),
                    null
                )
            } catch (ex: Exception) {
                Log.e(tag, "Storage upload exception", ex)
                Pair(
                    StorageUploadResult.Failure("File sharing isn't set up yet — enable Firebase Storage in the console"),
                    null
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Attachment prep failure", e)
            Pair(StorageUploadResult.Failure(e.message ?: "Failed to process attachment"), null)
        }
    }

    suspend fun sendAttachmentMessage(
        recipientUserId: String,
        uri: Uri,
        fileName: String,
        mimeType: String,
        caption: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userProfile = userProfileDao.getUserProfile()
                ?: return@withContext Result.failure(IllegalStateException("User not logged in"))

            val (uploadResult, encryptedData) = uploadEncryptedAttachment(uri, fileName)
            val storagePath = when (uploadResult) {
                is StorageUploadResult.Success -> uploadResult.storagePath
                is StorageUploadResult.Failure -> {
                    return@withContext Result.failure(IllegalStateException(uploadResult.userFacingReason))
                }
            }

            if (encryptedData == null) {
                return@withContext Result.failure(IllegalStateException("Encryption failed"))
            }

            val recipientBundle = fetchRecipientBundle(recipientUserId)
                ?: return@withContext Result.failure(IllegalStateException("Recipient not found"))

            val attachmentContent = DecryptedContent.Attachment(
                storagePath = storagePath,
                fileName = fileName,
                mimeType = mimeType,
                fileSize = uriToSize(uri),
                aesKey = encryptedData.base64Key,
                aesIv = encryptedData.base64Iv,
                caption = caption
            )

            // Encrypt metadata & keys inside Signal message
            val encryptedSignalMessage = signalProtocolManager.encryptMessage(
                recipientBundle,
                attachmentContent.toJsonString()
            )

            val messageId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val displayText = if (caption.isNotBlank()) caption else "[File: $fileName]"

            // Save local copy of plain file in app's cache directory so sender can view immediately
            val localCacheFile = saveLocalAttachmentCopy(uri, fileName)

            val localEntity = LocalMessageEntity(
                id = messageId,
                conversationUserId = recipientUserId,
                senderId = userProfile.userId,
                recipientId = recipientUserId,
                plaintext = displayText,
                timestamp = now,
                status = "SENDING",
                hasAttachment = true,
                attachmentUrl = storagePath,
                attachmentLocalPath = localCacheFile?.absolutePath,
                attachmentMimeType = mimeType,
                attachmentFileName = fileName,
                attachmentFileSize = uriToSize(uri),
                isOutgoing = true
            )
            messageDao.insertMessage(localEntity)
            contactDao.updateLastMessage(recipientUserId, displayText, now)

            val messageData = hashMapOf(
                "id" to messageId,
                "senderId" to userProfile.userId,
                "recipientId" to recipientUserId,
                "encryptedPayload" to encryptedSignalMessage.base64Ciphertext,
                "messageType" to encryptedSignalMessage.messageType,
                "timestamp" to now,
                "status" to "sent"
            )

            firestore.collection("messages").document(messageId)
                .set(messageData)
                .await()

            messageDao.updateMessageStatus(messageId, "SENT")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error sending attachment", e)
            Result.failure(e)
        }
    }

    private fun downloadAndDecryptAttachment(
        messageId: String,
        storagePath: String,
        fileName: String,
        aesKey: String,
        aesIv: String
    ) {
        repoScope.launch {
            try {
                val storageRef = FirebaseStorage.getInstance().reference.child(storagePath)
                // Download up to 20MB
                val maxDownloadBytes: Long = 20 * 1024 * 1024
                val encryptedBytes = storageRef.getBytes(maxDownloadBytes).await()

                // Decrypt using AES-256-GCM key and IV received inside Signal message
                val decryptedBytes = AttachmentCrypto.decryptBytes(encryptedBytes, aesKey, aesIv)

                val dir = File(context.cacheDir, "attachments").apply { mkdirs() }
                val targetFile = File(dir, "${messageId}_$fileName")
                FileOutputStream(targetFile).use { it.write(decryptedBytes) }

                messageDao.updateAttachmentLocalPath(messageId, targetFile.absolutePath)
            } catch (e: Exception) {
                Log.w(tag, "Failed to download and decrypt attachment $storagePath", e)
            }
        }
    }

    private fun uriToSize(uri: Uri): Long {
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                it.length
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun saveLocalAttachmentCopy(uri: Uri, fileName: String): File? {
        return try {
            val dir = File(context.cacheDir, "attachments").apply { mkdirs() }
            val file = File(dir, "${System.currentTimeMillis()}_$fileName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }
}
