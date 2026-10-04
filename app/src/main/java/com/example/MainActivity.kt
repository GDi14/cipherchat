package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.OnboardingScreen
import com.example.ui.OnboardingViewModel

import androidx.compose.animation.AnimatedContent
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.MyApplicationTheme

sealed interface AppScreen {
    val level: Int
    data object Onboarding : AppScreen { override val level: Int = 0 }
    data object Home : AppScreen { override val level: Int = 1 }
    data class Chat(val conversationId: String) : AppScreen { override val level: Int = 2 }
    data object NewGroup : AppScreen { override val level: Int = 2 }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember {
                        mutableStateOf<AppScreen>(
                            if (authRepository.isRegistered()) AppScreen.Home else AppScreen.Onboarding
                        )
                    }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (targetState.level >= initialState.level) {
                                ExpressiveMotion.ForwardTransition
                            } else {
                                ExpressiveMotion.BackwardTransition
                            }
                        },
                        label = "expressive_screen_transition"
                    ) { screen ->
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
                                    onBack = {
                                        currentScreen = AppScreen.Home
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
                        }
                    }
                }
            }
        }
    }
}
