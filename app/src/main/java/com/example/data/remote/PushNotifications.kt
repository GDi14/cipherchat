package com.example.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.SecurePreferencesManager
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.data.remote.DeviceTokenDto
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object PushTokenRegistrar {
    suspend fun register(
        context: Context,
        securePrefs: SecurePreferencesManager,
        supabaseProvider: SupabaseClientProvider,
        refreshedToken: String? = null
    ) = withContext(Dispatchers.IO) {
        if (!securePrefs.isRegistered()) {
            Log.d("PushTokenRegistrar", "Skipping token registration before profile setup")
            return@withContext
        }
        val client = supabaseProvider.getClient()
        val authUserId = client.auth.currentUserOrNull()?.id ?: run {
            Log.w("PushTokenRegistrar", "Skipping token registration: no Supabase Auth session")
            return@withContext
        }
        if (securePrefs.getSupabaseAuthUserId() != authUserId) {
            Log.w("PushTokenRegistrar", "Skipping token registration: local Auth identity does not match session")
            return@withContext
        }
        val token = refreshedToken ?: FirebaseMessaging.getInstance().token.await()
        try {
            client.postgrest["device_tokens"].upsert(
                DeviceTokenDto(
                    deviceId = securePrefs.getOrCreateDeviceId(),
                    userId = securePrefs.getOrCreateUserId(),
                    fcmToken = token
                )
            )
            Log.i("PushTokenRegistrar", "FCM token registered for the current profile")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e("PushTokenRegistrar", "Could not save FCM token; check device_tokens RLS and profile/Auth link", error)
            throw error
        }
    }
}

object MessageNotificationChannel {
    const val CHANNEL_MESSAGES = "messages"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MESSAGES,
                    context.getString(R.string.notification_channel_messages),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }
}

class CipherFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val securePrefs = SecurePreferencesManager(applicationContext)
        val supabaseProvider = SupabaseClientProvider(applicationContext, securePrefs)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PushTokenRegistrar.register(applicationContext, securePrefs, supabaseProvider, token)
            } catch (e: Exception) {
                Log.e("ChatSync", "Error registering push token", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val conversationId = message.data["conversation_id"]
        if (conversationId.isNullOrBlank()) {
            Log.w("ChatSync", "Ignoring FCM event without a conversation_id")
            return
        }
        Log.d("ChatSync", "FCM message received; queueing durable conversation catch-up")
        IncomingMessageSyncWorker.enqueue(applicationContext, conversationId)
        val senderName = message.data["sender_name"]
            ?.trim()
            ?.take(80)
            ?.ifBlank { null }
            ?: getString(R.string.notification_title_new_message)
        showNewMessageNotification(conversationId, senderName)
    }

    override fun onDeletedMessages() {
        Log.w("ChatSync", "FCM reported deleted messages; queueing a full catch-up")
        IncomingMessageSyncWorker.enqueue(applicationContext)
    }

    private fun showNewMessageNotification(conversationId: String, senderName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("ChatSync", "Push received, but notifications are disabled by Android permission")
            return
        }

        MessageNotificationChannel.ensureCreated(this)

        val openConversation = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_NOTIFICATION_CONVERSATION_ID, conversationId)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            conversationId.hashCode(),
            openConversation,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, MessageNotificationChannel.CHANNEL_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(senderName)
            .setContentText(getString(R.string.notification_body_new_message))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(this).notify(conversationId.hashCode(), notification)
    }
}

private fun CoroutineScope.launchSafely(block: suspend () -> Unit) {
    launch {
        runCatching { block() }.onFailure {
            Log.e("PushTokenRegistrar", "FCM token refresh registration failed", it)
        }
    }
}
