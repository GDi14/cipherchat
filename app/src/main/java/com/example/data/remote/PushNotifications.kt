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
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
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
        } catch (error: Exception) {
            Log.e("PushTokenRegistrar", "Could not save FCM token; check device_tokens RLS and profile/Auth link", error)
            throw error
        }
    }
}

class CipherFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val securePrefs = SecurePreferencesManager(applicationContext)
        val supabaseProvider = SupabaseClientProvider(applicationContext, securePrefs)
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launchSafely {
            PushTokenRegistrar.register(applicationContext, securePrefs, supabaseProvider, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val securePrefs = SecurePreferencesManager(applicationContext)
        if (securePrefs.isApplicationForeground()) return
        val conversationId = message.data["conversation_id"] ?: return
        showNewMessageNotification(conversationId)
    }

    private fun showNewMessageNotification(conversationId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MESSAGES,
                    getString(R.string.notification_channel_messages),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

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
        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_title_new_message))
            .setContentText(getString(R.string.notification_body_new_message))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(this).notify(conversationId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_MESSAGES = "messages"
    }
}

private fun CoroutineScope.launchSafely(block: suspend () -> Unit) {
    launch {
        runCatching { block() }.onFailure {
            Log.e("PushTokenRegistrar", "FCM token refresh registration failed", it)
        }
    }
}
