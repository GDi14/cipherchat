package com.example

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.example.data.firebase.FirebaseChatRepository
import com.example.data.local.AppDatabase
import com.example.data.signal.SignalProtocolManager
import com.example.data.updates.UpdateManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CipherChatApplication : Application(), Application.ActivityLifecycleCallbacks {

    val database by lazy { AppDatabase.getInstance(this) }
    val signalProtocolManager by lazy {
        SignalProtocolManager(
            signalStoreDao = database.signalStoreDao(),
            userProfileDao = database.userProfileDao()
        )
    }

    val firestore: FirebaseFirestore by lazy {
        ensureFirebaseInitialized()
        FirebaseFirestore.getInstance()
    }

    val chatRepository by lazy {
        FirebaseChatRepository(
            context = this,
            firestore = firestore,
            messageDao = database.messageDao(),
            contactDao = database.contactDao(),
            userProfileDao = database.userProfileDao(),
            signalProtocolManager = signalProtocolManager
        )
    }

    val updateManager by lazy {
        UpdateManager(this)
    }

    private val applicationScope = CoroutineScope(Dispatchers.IO)
    private var resumedActivities = 0

    override fun onCreate() {
        super.onCreate()
        ensureFirebaseInitialized()
        createNotificationChannels()
        registerActivityLifecycleCallbacks(this)

        // Safe startup sync
        applicationScope.launch {
            try {
                val profile = database.userProfileDao().getUserProfile()
                if (profile != null) {
                    chatRepository.startListening(profile.userId)
                    chatRepository.updatePresence(true)
                }
            } catch (e: Exception) {
                Log.w("CipherApp", "Background sync deferred: ${e.message}")
            }
        }
    }

    fun ensureFirebaseInitialized() {
        try {
            if (FirebaseApp.getApps(this).isNotEmpty()) {
                return
            }
            // Attempt standard initialization with google-services.json
            val defaultApp = FirebaseApp.initializeApp(this)
            if (defaultApp != null) {
                return
            }
        } catch (e: Exception) {
            Log.w("CipherApp", "Standard Firebase initialization note: ${e.message}")
        }

        // Resilient fallback initialization so the app NEVER crashes if google-services.json is pending
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val fallbackOptions = FirebaseOptions.Builder()
                    .setApplicationId(packageName)
                    .setProjectId("cipherchat-demo")
                    .setApiKey("AIzaSyFakeKeyForPreviewInitOnly1234567")
                    .build()
                FirebaseApp.initializeApp(this, fallbackOptions)
                Log.i("CipherApp", "Initialized fallback FirebaseApp (pending google-services.json)")
            }
        } catch (e: Exception) {
            Log.e("CipherApp", "Could not initialize fallback FirebaseApp", e)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        resumedActivities++
        if (resumedActivities == 1) {
            applicationScope.launch {
                try {
                    chatRepository.updatePresence(true)
                } catch (e: Exception) {
                    // Suppress if offline or pending credentials
                }
            }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        resumedActivities = (resumedActivities - 1).coerceAtLeast(0)
        if (resumedActivities == 0) {
            applicationScope.launch {
                try {
                    chatRepository.updatePresence(false)
                } catch (e: Exception) {
                    // Suppress if offline or pending credentials
                }
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "cipher_messages_channel",
                "Encrypted Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for private end-to-end encrypted messages"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
