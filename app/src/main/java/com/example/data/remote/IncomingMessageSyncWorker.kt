package com.example.data.remote

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import com.example.crypto.SignalProtocolManager
import com.example.data.local.AppDatabase
import com.example.data.local.SecurePreferencesManager
import com.example.repository.ChatRepository
import java.util.concurrent.TimeUnit

/** Durable catch-up for high-priority FCM events after the service callback returns. */
class IncomingMessageSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val securePrefs = SecurePreferencesManager(applicationContext)
            if (!securePrefs.isRegistered()) return Result.success()

            val provider = SupabaseClientProvider(applicationContext, securePrefs)
            val client = provider.getClient()
            val authStatus = withTimeoutOrNull(AUTH_RESTORE_TIMEOUT_MS) {
                client.auth.sessionStatus.first { it != SessionStatus.Initializing }
            }
            val authUserId = client.auth.currentUserOrNull()?.id
            if (authStatus == null || authUserId == null || authUserId != securePrefs.getSupabaseAuthUserId()) {
                Log.w(TAG, "Background catch-up has no matching Supabase Auth session; will retry")
                return Result.retry()
            }

            val repository = ChatRepository(
                context = applicationContext,
                securePrefs = securePrefs,
                signalManager = SignalProtocolManager(applicationContext, securePrefs),
                supabaseProvider = provider,
                database = AppDatabase.getInstance(applicationContext)
            )
            val conversationId = inputData.getString(KEY_CONVERSATION_ID)
            val syncResult = if (conversationId.isNullOrBlank()) {
                repository.syncAllFromSupabase()
            } else {
                repository.syncConversationFromSupabase(conversationId)
            }
            syncResult.fold(
                onSuccess = { Result.success() },
                onFailure = { error ->
                    Log.w(TAG, "FCM-triggered catch-up failed; scheduling retry", error)
                    Result.retry()
                }
            )
        } catch (error: Exception) {
            Log.e(TAG, "Could not run FCM-triggered catch-up", error)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "IncomingMessageSync"
        private const val UNIQUE_WORK_NAME = "incoming-message-catch-up"
        private const val AUTH_RESTORE_TIMEOUT_MS = 8_000L

        private const val KEY_CONVERSATION_ID = "conversation_id"

        fun enqueue(context: Context, conversationId: String? = null) {
            val input = conversationId?.let { workDataOf(KEY_CONVERSATION_ID to it) } ?: Data.EMPTY
            val request = OneTimeWorkRequestBuilder<IncomingMessageSyncWorker>()
                .setInputData(input)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
        }
    }
}
