package com.example

import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crypto.SignalProtocolManager
import com.example.data.local.AppDatabase
import com.example.data.local.SecurePreferencesManager
import com.example.data.remote.SupabaseClientProvider
import com.example.repository.AuthRepository
import com.example.repository.ChatRepository
import com.example.repository.FriendRepository
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.HomeScreen
import com.example.ui.HomeViewModel
import com.example.ui.NewGroupScreen
import com.example.ui.NewGroupViewModel
import com.example.ui.M3AppSettingsPage
import com.example.ui.M3FriendsTab
import com.example.ui.M3NotificationsPage
import com.example.ui.M3ProfileTab
import com.example.ui.OnboardingScreen
import com.example.ui.OnboardingViewModel
import com.example.ui.PublicProfilePage

import androidx.compose.animation.AnimatedContent
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.ExpressivePagesTheme
import com.example.ui.theme.LocalExpressiveMotionEnabled
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.rememberSystemMotionEnabled

sealed interface AppScreen {
    val level: Int
    data object Onboarding : AppScreen { override val level: Int = 0 }
    data object Home : AppScreen { override val level: Int = 1 }
    data class Chat(val conversationId: String) : AppScreen { override val level: Int = 2 }
    data object NewGroup : AppScreen { override val level: Int = 2 }
    data object AddFriends : AppScreen { override val level: Int = 2 }
    data object Notifications : AppScreen { override val level: Int = 2 }
    data object AppSettings : AppScreen { override val level: Int = 3 }
    data object ProfileSettings : AppScreen { override val level: Int = 3 }
    data class UserProfile(val userId: String) : AppScreen { override val level: Int = 3 }
}

private val AppScreenSaver = Saver<AppScreen, String>(
    save = { screen ->
        when (screen) {
            AppScreen.Onboarding -> "onboarding"
            AppScreen.Home -> "home"
            is AppScreen.Chat -> "chat:${screen.conversationId}"
            AppScreen.NewGroup -> "new_group"
            AppScreen.AddFriends -> "friends"
            AppScreen.Notifications -> "notifications"
            AppScreen.AppSettings -> "app_settings"
            AppScreen.ProfileSettings -> "profile_settings"
            is AppScreen.UserProfile -> "user_profile:${screen.userId}"
        }
    },
    restore = { saved ->
        when {
            saved == "onboarding" -> AppScreen.Onboarding
            saved.startsWith("chat:") -> AppScreen.Chat(saved.removePrefix("chat:"))
            saved == "new_group" -> AppScreen.NewGroup
            saved == "friends" -> AppScreen.AddFriends
            saved == "notifications" -> AppScreen.Notifications
            saved == "app_settings" -> AppScreen.AppSettings
            saved == "profile_settings" -> AppScreen.ProfileSettings
            saved.startsWith("user_profile:") -> AppScreen.UserProfile(saved.removePrefix("user_profile:"))
            else -> AppScreen.Home
        }
    }
)

class MainActivity : ComponentActivity() {
    private var notificationConversationRequest by mutableStateOf<String?>(null)

    companion object {
        const val EXTRA_NOTIFICATION_CONVERSATION_ID = "notification_conversation_id"
    }

    override fun onStart() {
        super.onStart()
        SecurePreferencesManager(applicationContext).setApplicationForeground(true)
    }

    override fun onStop() {
        SecurePreferencesManager(applicationContext).setApplicationForeground(false)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationConversationRequest = intent?.getStringExtra(EXTRA_NOTIFICATION_CONVERSATION_ID)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        // 1. Core singletons
        val securePrefs = SecurePreferencesManager(applicationContext)
        val database = AppDatabase.getInstance(applicationContext)
        val supabaseProvider = SupabaseClientProvider(applicationContext, securePrefs)
        val signalManager = SignalProtocolManager(applicationContext, securePrefs)

        val authRepository = AuthRepository(
            context = applicationContext,
            securePrefs = securePrefs,
            signalManager = signalManager,
            supabaseProvider = supabaseProvider,
            database = database
        )
        val friendRepository = FriendRepository(
            securePrefs = securePrefs,
            supabaseProvider = supabaseProvider,
            database = database
        )
        val chatRepository = ChatRepository(
            context = applicationContext,
            securePrefs = securePrefs,
            signalManager = signalManager,
            supabaseProvider = supabaseProvider,
            database = database
        )

        setContent {
            val systemMotionEnabled = rememberSystemMotionEnabled()
            CompositionLocalProvider(LocalExpressiveMotionEnabled provides systemMotionEnabled) {
                MyApplicationTheme(darkTheme = true) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                    var currentScreen by rememberSaveable(stateSaver = AppScreenSaver) {
                        mutableStateOf<AppScreen>(
                            notificationConversationRequest?.takeIf { authRepository.isRegistered() }
                                ?.let { AppScreen.Chat(it) }
                                ?: if (authRepository.isRegistered()) AppScreen.Home else AppScreen.Onboarding
                        )
                    }
                    LaunchedEffect(notificationConversationRequest) {
                        val conversationId = notificationConversationRequest ?: return@LaunchedEffect
                        if (authRepository.isRegistered()) currentScreen = AppScreen.Chat(conversationId)
                        notificationConversationRequest = null
                    }
                    var profileReturnScreen by rememberSaveable(stateSaver = AppScreenSaver) {
                        mutableStateOf<AppScreen>(AppScreen.Home)
                    }

                    BackHandler(
                        enabled = currentScreen != AppScreen.Home && currentScreen != AppScreen.Onboarding
                    ) {
                        currentScreen = when (currentScreen) {
                            AppScreen.AppSettings, AppScreen.ProfileSettings -> AppScreen.Home
                            is AppScreen.UserProfile -> profileReturnScreen
                            else -> AppScreen.Home
                        }
                    }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (!systemMotionEnabled) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else if (targetState.level >= initialState.level) {
                                ExpressiveMotion.ForwardTransition
                            } else {
                                ExpressiveMotion.BackwardTransition
                            }
                        },
                        label = "expressive_screen_transition"
                    ) { screen ->
                        val renderScreen: @Composable () -> Unit = {
                        when (screen) {
                            is AppScreen.Onboarding -> {
                                val onboardingViewModel: OnboardingViewModel = viewModel(
                                    factory = OnboardingViewModel.Factory(authRepository, supabaseProvider)
                                )
                                OnboardingScreen(
                                    viewModel = onboardingViewModel,
                                    onRegistered = {
                                        currentScreen = AppScreen.Home
                                    }
                                )
                            }

                            is AppScreen.Home -> {
                                val homeViewModel: HomeViewModel = viewModel(
                                    factory = HomeViewModel.Factory(
                                        authRepository,
                                        friendRepository,
                                        chatRepository,
                                        supabaseProvider
                                    )
                                )
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    onOpenConversation = { convId ->
                                        currentScreen = AppScreen.Chat(convId)
                                    },
                                    onNavigateToNewGroup = {
                                        currentScreen = AppScreen.NewGroup
                                    },
                                    onOpenAddFriends = { currentScreen = AppScreen.AddFriends },
                                    onOpenNotifications = { currentScreen = AppScreen.Notifications },
                                    onOpenAppSettings = { currentScreen = AppScreen.AppSettings },
                                    onOpenProfileSettings = { currentScreen = AppScreen.ProfileSettings },
                                    onOpenProfile = { userId ->
                                        profileReturnScreen = AppScreen.Home
                                        currentScreen = AppScreen.UserProfile(userId)
                                    }
                                )
                            }

                            is AppScreen.Chat -> {
                                val chatViewModel: ChatViewModel = viewModel(
                                    key = screen.conversationId,
                                    factory = ChatViewModel.Factory(
                                        applicationContext,
                                        screen.conversationId,
                                        chatRepository
                                    )
                                )
                                ChatScreen(
                                    viewModel = chatViewModel,
                                    friendRepository = friendRepository,
                                    onBack = {
                                        currentScreen = AppScreen.Home
                                    },
                                    onOpenProfile = { userId ->
                                        profileReturnScreen = screen
                                        currentScreen = AppScreen.UserProfile(userId)
                                    }
                                )
                            }

                            is AppScreen.NewGroup -> {
                                val newGroupViewModel: NewGroupViewModel = viewModel(
                                    factory = NewGroupViewModel.Factory(
                                        friendRepository,
                                        chatRepository
                                    )
                                )
                                NewGroupScreen(
                                    viewModel = newGroupViewModel,
                                    onBack = {
                                        currentScreen = AppScreen.Home
                                    },
                                    onGroupCreated = { newGroupId ->
                                        currentScreen = AppScreen.Chat(newGroupId)
                                    }
                                )
                            }
                            is AppScreen.AddFriends -> {
                                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(
                                    authRepository, friendRepository, chatRepository, supabaseProvider))
                                val friends by homeViewModel.friends.collectAsState()
                                val requests by homeViewModel.pendingRequests.collectAsState()
                                val conversations by homeViewModel.conversations.collectAsState()
                                M3FriendsTab(homeViewModel, friends, requests, onBack = { currentScreen = AppScreen.Home },
                                    onOpenProfile = { userId ->
                                        profileReturnScreen = AppScreen.AddFriends
                                        currentScreen = AppScreen.UserProfile(userId)
                                    }) { friend ->
                                    val existing = conversations.firstOrNull { it.otherUserId == friend.id }
                                    if (existing != null) currentScreen = AppScreen.Chat(existing.id)
                                    else homeViewModel.openConversationWithFriend(friend) { currentScreen = AppScreen.Chat(it) }
                                }
                            }
                            is AppScreen.Notifications -> {
                                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(
                                    authRepository, friendRepository, chatRepository, supabaseProvider))
                                val requests by homeViewModel.pendingRequests.collectAsState()
                                M3NotificationsPage(homeViewModel, requests) { currentScreen = AppScreen.Home }
                            }
                            is AppScreen.AppSettings -> {
                                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(
                                    authRepository, friendRepository, chatRepository, supabaseProvider))
                                M3AppSettingsPage(homeViewModel) { currentScreen = AppScreen.Home }
                            }
                            is AppScreen.ProfileSettings -> {
                                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(
                                    authRepository, friendRepository, chatRepository, supabaseProvider))
                                M3ProfileTab(homeViewModel, onBack = { currentScreen = AppScreen.Home })
                            }
                            is AppScreen.UserProfile -> {
                                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(
                                    authRepository, friendRepository, chatRepository, supabaseProvider))
                                PublicProfilePage(
                                    userId = screen.userId,
                                    friendRepository = friendRepository,
                                    homeViewModel = homeViewModel,
                                    friends = homeViewModel.friends.collectAsState().value,
                                    requests = homeViewModel.allRequests.collectAsState().value,
                                    conversations = homeViewModel.conversations.collectAsState().value,
                                    onBack = { currentScreen = profileReturnScreen },
                                    onOpenConversation = { currentScreen = AppScreen.Chat(it) }
                                )
                            }
                        }
                        }

                        if (screen is AppScreen.Chat) {
                            renderScreen()
                        } else {
                            ExpressivePagesTheme(content = renderScreen)
                        }
                    }
                }
            }
        }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationConversationRequest = intent.getStringExtra(EXTRA_NOTIFICATION_CONVERSATION_ID)
    }
}
