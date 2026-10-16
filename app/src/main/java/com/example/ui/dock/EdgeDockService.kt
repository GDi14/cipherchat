package com.example.ui.dock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.app.ServiceCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.SecurePreferencesManager
import com.example.ui.components.FluidSidebarDock
import com.example.ui.model.DockCategory
import com.example.ui.theme.MyApplicationTheme

class EdgeDockService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var preferences: SecurePreferencesManager
    private var lifecycleOwner: DockOverlayLifecycleOwner? = null
    private var overlay: ComposeView? = null
    private var foregroundStarted = false
    private var appVisible = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        preferences = SecurePreferencesManager(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                preferences.setEdgeDockEnabled(false)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_APP_VISIBLE -> appVisible = true
            ACTION_APP_HIDDEN -> appVisible = false
            null -> appVisible = false
        }

        if (!preferences.isEdgeDockEnabled()) {
            stopSelf()
            return START_NOT_STICKY
        }
        ensureForeground()
        if (appVisible || !Settings.canDrawOverlays(this)) removeOverlay() else showOverlay()
        return START_STICKY
    }

    private fun ensureForeground() {
        if (foregroundStarted) return
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_cypher)
            .setContentTitle("People dock is on")
            .setContentText("Swipe the edge dock to choose a conversation")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        foregroundStarted = true
    }

    private fun showOverlay() {
        if (overlay != null) return
        val composeView = ComposeView(this)
        val owner = DockOverlayLifecycleOwner()
        lifecycleOwner = owner
        composeView.setViewTreeLifecycleOwner(owner)
        composeView.setViewTreeSavedStateRegistryOwner(owner)
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        owner.registry.currentState = Lifecycle.State.RESUMED
        composeView.setContent {
            MyApplicationTheme(darkTheme = true) {
                OverlayDockContent(
                    onOpenConversation = { conversationId ->
                        val launch = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra(MainActivity.EXTRA_NOTIFICATION_CONVERSATION_ID, conversationId)
                        }
                        startActivity(launch)
                    }
                )
            }
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        // Increase dimensions to prevent spring animation clipping and allow gesture tracking outside the immediate dock bounds.
        // Dock is max 62x158. We use 86x200 to give padding for physics overshoots without blocking too much underlying app touch area.
        val width = (86 * resources.displayMetrics.density).toInt()
        val height = (200 * resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(
            width,
            height,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            // Offset y by 21dp (half of the 42dp height increase) to keep the dock exactly at the original 248dp visual position (248 - 21 = 227)
            y = (227 * resources.displayMetrics.density).toInt()
        }
        runCatching {
            windowManager.addView(composeView, params)
            overlay = composeView
        }.onFailure {
            owner.registry.currentState = Lifecycle.State.DESTROYED
            lifecycleOwner = null
            stopSelf()
        }
    }

    private fun removeOverlay() {
        val view = overlay ?: return
        overlay = null
        runCatching { windowManager.removeView(view) }
        lifecycleOwner?.registry?.currentState = Lifecycle.State.DESTROYED
        lifecycleOwner = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "People dock", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        removeOverlay()
        if (foregroundStarted) ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_APP_VISIBLE = "com.example.edge_dock.APP_VISIBLE"
        const val ACTION_APP_HIDDEN = "com.example.edge_dock.APP_HIDDEN"
        const val ACTION_STOP = "com.example.edge_dock.STOP"
        private const val CHANNEL_ID = "people_edge_dock"
        private const val NOTIFICATION_ID = 8412
    }
}

private class DockOverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    val registry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
    }

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
}

@Composable
private fun OverlayDockContent(onOpenConversation: (String) -> Unit) {
    val conversations by AppDatabase.getInstance(androidx.compose.ui.platform.LocalContext.current)
        .conversationDao().getAllConversations().collectAsState(initial = emptyList())
    val items = remember(conversations) {
        conversations.map { conversation ->
            DockCategory(
                id = conversation.id,
                shortLabel = conversation.title.orEmpty().trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fullTitle = conversation.title ?: if (conversation.type == "group") "Group" else "Chat"
            )
        }
    }
    var selectedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(items) {
        if (items.none { it.id == selectedId }) selectedId = items.firstOrNull()?.id
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        FluidSidebarDock(
            items = items,
            selectedCategoryId = selectedId,
            onCategorySelected = { selectedId = it.id },
            onCategoryActivated = { onOpenConversation(it.id) },
            modifier = Modifier
        )
    }
}
