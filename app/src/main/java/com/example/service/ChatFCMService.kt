package com.example.service

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.crypto.MessagePayloadCodec
import com.example.crypto.SignalProtocolManager
import com.example.data.local.AppDatabase
import com.example.data.local.MessageEntity
import com.example.data.local.SecurePreferencesManager
import com.example.data.remote.DeviceTokenDto
import com.example.data.remote.MessageReceiptDto
import com.example.data.remote.SupabaseClientProvider
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ChatFCMService : FirebaseMessagingService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i("ChatFCMService", "Refreshed token: $token")
        val context = applicationContext
        val securePrefs = SecurePreferencesManager(context)
        
        if (!securePrefs.isRegistered()) return
        
        val myUserId = securePrefs.getOrCreateUserId()
        val myDeviceId = securePrefs.getOrCreateDeviceId()
        val supabaseProvider = SupabaseClientProvider(context, securePrefs)

        scope.launch {
            if (supabaseProvider.isConfigured) {
                try {
                    val client = supabaseProvider.getClient()
                    client.postgrest["device_tokens"].upsert(
                        DeviceTokenDto(
                            deviceId = myDeviceId,
                            userId = myUserId,
                            fcmToken = token
                        )
                    )
                    Log.i("ChatFCMService", "FCM token synced to Supabase")
                } catch (e: Exception) {
                    Log.e("ChatFCMService", "Failed to sync FCM token", e)
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.i("ChatFCMService", "Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isEmpty()) return

        val messageId = data["message_id"] ?: return
        val conversationId = data["conversation_id"] ?: return
        val senderId = data["sender_id"] ?: return
        val recipientId = data["recipient_id"]
        val messageTypeStr = data["message_type"] ?: return
        val encryptedBody = data["encrypted_body"] ?: return
        val timestampStr = data["created_at"]

        val messageType = messageTypeStr.toIntOrNull() ?: return
        val timestamp = timestampStr?.toLongOrNull() ?: System.currentTimeMillis()

        val context = applicationContext
        val securePrefs = SecurePreferencesManager(context)
        val database = AppDatabase.getDatabase(context)
        val signalManager = SignalProtocolManager(context, securePrefs)
        val supabaseProvider = SupabaseClientProvider(context, securePrefs)
        val myUserId = securePrefs.getOrCreateUserId()

        scope.launch {
            try {
                // Ensure we don't duplicate
                val existing = database.messageDao().getMessageById(messageId)
                if (existing != null && existing.status != "failed") return@launch
                
                // Write a delivered receipt to Supabase so sender gets ticks immediately
                if (supabaseProvider.isConfigured) {
                    runCatching {
                        supabaseProvider.getClient().postgrest["message_receipts"].upsert(
                            MessageReceiptDto(messageId = messageId, userId = myUserId, status = "delivered")
                        )
                    }
                }

                // Check conversation type
                val conv = database.conversationDao().getConversationById(conversationId)
                val isDirect = conv?.type == "direct" || recipientId == myUserId

                val decryptedBytes = try {
                    if (isDirect) {
                        signalManager.decryptDirectMessage(senderId, messageType, encryptedBody)
                    } else {
                        signalManager.decryptGroupMessage(conversationId, senderId, encryptedBody)
                    }
                } catch (e: Exception) {
                    Log.e("ChatFCMService", "FCM Decryption failed", e)
                    return@launch
                }

                val payload = MessagePayloadCodec.decode(decryptedBytes)
                
                // Look up sender name
                val senderProfile = database.profileDao().getProfileById(senderId)
                val senderName = senderProfile?.displayName ?: "Friend"

                val displaySnippet = when {
                    payload.media != null -> when {
                        payload.media.mimeType.startsWith("audio") -> "Voice Note"
                        payload.media.mimeType.startsWith("video") -> "Video"
                        else -> "Photo"
                    }
                    payload.text.isNotBlank() -> payload.text
                    else -> "New message"
                }

                val entity = MessageEntity(
                    id = messageId,
                    conversationId = conversationId,
                    senderId = senderId,
                    senderName = senderName,
                    recipientId = recipientId,
                    plaintextBody = payload.text,
                    messageType = messageType,
                    replyToId = payload.replyToId,
                    replySnippet = payload.replySnippet,
                    textEmphasisJson = MessagePayloadCodec.encodeEmphasisRanges(payload.textEmphasisRanges),
                    mediaPath = payload.media?.path,
                    mediaType = payload.media?.mimeType,
                    mediaKey = payload.media?.key,
                    mediaIv = payload.media?.iv,
                    mediaDuration = payload.media?.durationSeconds ?: 0,
                    status = "delivered",
                    timestamp = timestamp
                )

                database.messageDao().insertOrUpdate(entity)
                database.conversationDao().updateLastMessage(conversationId, displaySnippet, entity.timestamp)
                database.conversationDao().incrementUnreadCount(conversationId)

                showNotification(context, senderName, displaySnippet, conversationId)

            } catch (e: Exception) {
                Log.e("ChatFCMService", "Error processing push", e)
            }
        }
    }

    private fun showNotification(context: Context, title: String, text: String, conversationId: String) {
        val channelId = "chat_messages"
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Chat Messages", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("conversationId", conversationId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            // Note: Replace with an actual small icon drawable later
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(conversationId.hashCode(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
