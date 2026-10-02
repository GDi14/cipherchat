package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.chatlist.ChatListScreen
import com.example.ui.chatlist.ChatListViewModel
import com.example.ui.components.ExpressiveLoadingIndicator
import com.example.ui.components.M3EmphasizedEasing
import com.example.ui.components.M3MotionDurationLong1
import com.example.ui.components.M3MotionDurationMedium2
import com.example.ui.conversation.ConversationScreen
import com.example.ui.conversation.ConversationViewModel
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.onboarding.OnboardingViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.CipherChatTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CipherChatApplication

        setContent {
            CipherChatTheme {
                // Request POST_NOTIFICATIONS on Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPermission) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                CipherChatNavApp(app)
            }
        }
    }
}

@Composable
fun CipherChatNavApp(app: CipherChatApplication) {
    var startRoute by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val profile = app.database.userProfileDao().getUserProfile()
        startRoute = if (profile != null) "chat_list" else "onboarding"
    }

    if (startRoute == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            ExpressiveLoadingIndicator(size = 56.dp)
        }
        return
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startRoute!!,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(M3MotionDurationLong1, easing = M3EmphasizedEasing)
            ) + fadeIn(animationSpec = tween(M3MotionDurationMedium2))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(M3MotionDurationLong1, easing = M3EmphasizedEasing)
            ) + fadeOut(animationSpec = tween(M3MotionDurationMedium2))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(M3MotionDurationLong1, easing = M3EmphasizedEasing)
            ) + fadeIn(animationSpec = tween(M3MotionDurationMedium2))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(M3MotionDurationLong1, easing = M3EmphasizedEasing)
            ) + fadeOut(animationSpec = tween(M3MotionDurationMedium2))
        }
    ) {
        composable("onboarding") {
            val vm: OnboardingViewModel = viewModel(
                factory = OnboardingViewModel.Factory(
                    userProfileDao = app.database.userProfileDao(),
                    signalProtocolManager = app.signalProtocolManager,
                    chatRepository = app.chatRepository
                )
            )
            OnboardingScreen(
                viewModel = vm,
                onOnboardingComplete = {
                    navController.navigate("chat_list") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("chat_list") {
            val vm: ChatListViewModel = viewModel(
                factory = ChatListViewModel.Factory(
                    contactDao = app.database.contactDao(),
                    userProfileDao = app.database.userProfileDao(),
                    chatRepository = app.chatRepository,
                    firestore = app.firestore
                )
            )
            ChatListScreen(
                viewModel = vm,
                onOpenChat = { friendUserId ->
                    navController.navigate("conversation/$friendUserId")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable(
            route = "conversation/{friendUserId}",
            arguments = listOf(
                navArgument("friendUserId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val friendUserId = backStackEntry.arguments?.getString("friendUserId") ?: ""
            val vm: ConversationViewModel = viewModel(
                factory = ConversationViewModel.Factory(
                    friendUserId = friendUserId,
                    messageDao = app.database.messageDao(),
                    contactDao = app.database.contactDao(),
                    chatRepository = app.chatRepository
                )
            )
            ConversationScreen(
                viewModel = vm,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("settings") {
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(
                    userProfileDao = app.database.userProfileDao(),
                    updateManager = app.updateManager
                )
            )
            SettingsScreen(
                viewModel = vm,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
