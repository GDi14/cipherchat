package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ConversationEntity
import com.example.data.local.FriendRequestEntity
import com.example.data.local.ProfileEntity
import com.example.data.remote.ProfileAppearance
import com.example.repository.FriendRepository
import com.example.ui.theme.LocalExpressiveMotionEnabled
import com.example.ui.typography.ChatFontOption

private val ProfileCanvas = Color(0xFFC987FA)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun PublicProfilePage(
    userId: String,
    friendRepository: FriendRepository,
    homeViewModel: HomeViewModel,
    friends: List<ProfileEntity>,
    requests: List<FriendRequestEntity>,
    conversations: List<ConversationEntity>,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit
) {
    var profile by remember(userId) { mutableStateOf<ProfileEntity?>(null) }
    var profileAppearance by remember(userId) { mutableStateOf(ProfileAppearance()) }
    var isLoading by remember(userId) { mutableStateOf(true) }
    var reloadKey by remember(userId) { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val uiState by homeViewModel.uiState.collectAsState()
    val motionEnabled = LocalExpressiveMotionEnabled.current
    val isFriend = friends.any { it.id == userId } || profile?.isFriend == true
    val isOwnProfile = userId == friendRepository.myUserId
    val requestPending = requests.any {
        it.status == "pending" && it.senderId == friendRepository.myUserId && it.receiverId == userId
    }

    LaunchedEffect(userId, reloadKey, if (isOwnProfile) uiState.profileAppearance else null) {
        isLoading = true
        if (isOwnProfile) {
            val auth = homeViewModel.authRepository
            profileAppearance = uiState.profileAppearance
            profile = ProfileEntity(
                id = userId,
                displayName = auth.getDisplayName(),
                tag = auth.getTag(),
                avatarPath = auth.getAvatarPath(),
                isFriend = false
            )
            isLoading = false
            return@LaunchedEffect
        }
        val cachedProfile = friendRepository.getProfileById(userId)
        profile = cachedProfile
        friendRepository.getRemoteProfileById(userId)?.let { dto ->
            profileAppearance = ProfileAppearance(dto.profileBackground, dto.profileFont, dto.profileFontWidth,
                dto.profileFontGrade, dto.profileFontWeight, dto.profileFontOpticalSize, dto.profileBio)
            profile = ProfileEntity(
                id = dto.id,
                displayName = dto.displayName,
                tag = dto.tag,
                avatarPath = dto.avatarPath,
                isFriend = cachedProfile?.isFriend == true
            )
        }
        isLoading = false
    }
    LaunchedEffect(uiState.statusMessage, uiState.errorMessage) {
        uiState.statusMessage?.let { snackbar.showSnackbar(it); homeViewModel.clearStatus() }
        uiState.errorMessage?.let { snackbar.showSnackbar(it); homeViewModel.clearError() }
    }

    val canvasColor = remember(profileAppearance.background) { profileColor(profileAppearance.background, ProfileCanvas) }
    val inkColor = remember(canvasColor) { bestContrastInk(canvasColor) }
    val profileFont = remember(profileAppearance) {
        ChatFontOption.fromId(profileAppearance.fontId).getFontFamily(
            profileAppearance.weight, profileAppearance.width, grade = 0,
            opticalSize = 14f
        )
    }
    Scaffold(
        containerColor = canvasColor,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = inkColor)
                    }
                },
                title = {
            ProfileWeightText(
                "Profile",
                weight = profileAppearance.effectiveProfileWeight(),
                width = profileAppearance.width,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = profileFont),
                color = inkColor
            )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = canvasColor)
            )
        }
    ) { insets ->
        when {
            isLoading -> ProfileLoading(insets)
            profile == null -> ProfileUnavailable(insets, canvasColor, inkColor) { reloadKey++ }
            else -> {
                val user = profile!!
                BoxWithConstraints(Modifier.fillMaxSize().padding(insets)) {
                    val contentWidth = maxWidth.coerceAtMost(720.dp)
                    val photoHeight = (maxHeight * 0.54f).coerceIn(300.dp, 520.dp)
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier.width(contentWidth).padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ProfileDisplayName(user.displayName, contentWidth - 48.dp, profileAppearance, inkColor)
                            Spacer(Modifier.height(18.dp))
                            ProfilePortrait(user.displayName, user.avatarPath, photoHeight, canvasColor, inkColor, profileFont, profileAppearance)

                            if (profileAppearance.bio.isNotBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                                    shape = MaterialTheme.shapes.large,
                                    color = inkColor.copy(alpha = 0.08f)
                                ) {
                                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ProfileWeightText(
                                            "ABOUT",
                                            weight = profileAppearance.effectiveProfileWeight(),
                                            width = profileAppearance.width,
                                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = profileFont),
                                            color = inkColor
                                        )
                                        ProfileWeightText(
                                            profileAppearance.bio,
                                            weight = profileAppearance.effectiveProfileWeight(),
                                            width = profileAppearance.width,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontFamily = profileFont
                                            ),
                                            color = inkColor
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = true,
                                enter = if (motionEnabled) fadeIn() + slideInVertically(initialOffsetY = { it / 8 })
                                else EnterTransition.None
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 28.dp),
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = MaterialTheme.shapes.large,
                                            color = inkColor.copy(alpha = 0.08f)
                                        ) {
                                            ProfileDetail(
                                                title = "HANDLE",
                                                value = "@${user.tag}",
                                                modifier = Modifier.padding(16.dp), appearance = profileAppearance, ink = inkColor
                                            )
                                        }
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = MaterialTheme.shapes.large,
                                            color = inkColor.copy(alpha = 0.08f)
                                        ) {
                                            ProfileDetail(
                                                title = "CONNECTION",
                                                value = if (isFriend) "Friends" else "CypherChat",
                                                modifier = Modifier.padding(16.dp), appearance = profileAppearance, ink = inkColor
                                            )
                                        }
                                    }

                                    if (isFriend) {
                                        Button(
                                            onClick = {
                                                val existing = conversations.firstOrNull {
                                                    it.type == "direct" && it.otherUserId == user.id
                                                }
                                                if (existing != null) onOpenConversation(existing.id)
                                                else profile?.let { friend ->
                                                    homeViewModel.openConversationWithFriend(friend, onOpenConversation)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            shape = CircleShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = inkColor,
                                                contentColor = canvasColor
                                            )
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                                            Spacer(Modifier.width(10.dp))
                                            ProfileWeightText(
                                                "Message",
                                                weight = profileAppearance.effectiveProfileWeight(),
                                                width = profileAppearance.width,
                                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = profileFont),
                                                color = canvasColor
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = { if (!requestPending) homeViewModel.sendFriendRequest(user) },
                                            enabled = !requestPending && !uiState.isSearching,
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            shape = CircleShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = inkColor,
                                                contentColor = canvasColor
                                            )
                                        ) {
                                            Icon(
                                                if (requestPending) Icons.Default.Schedule else Icons.Default.PersonAdd,
                                                contentDescription = null
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            ProfileWeightText(
                                                if (requestPending) "Request sent" else "Add friend",
                                                weight = profileAppearance.effectiveProfileWeight(),
                                                width = profileAppearance.width,
                                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = profileFont),
                                                color = canvasColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalTextApi::class)
private fun ProfileDisplayName(name: String, availableWidth: Dp, appearance: ProfileAppearance, ink: Color) {
    val fontFamily = remember(appearance) {
        ChatFontOption.fromId(appearance.fontId).getFontFamily(
            appearance.weight, appearance.width, grade = 0, opticalSize = 14f
        )
    }
    ProfileWeightText(
        text = name,
        weight = appearance.effectiveProfileWeight(),
        modifier = Modifier.widthIn(max = availableWidth)
            .graphicsLayer { scaleX = appearance.width / 100f },
        style = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = fontFamily,
            letterSpacing = (-0.6).sp
        ),
        color = ink,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
@OptIn(ExperimentalTextApi::class)
private fun ProfilePortrait(
    name: String,
    avatarUrl: String?,
    height: Dp,
    canvas: Color,
    ink: Color,
    profileFont: androidx.compose.ui.text.font.FontFamily,
    appearance: ProfileAppearance
) {
    Box(
        Modifier.fillMaxWidth().height(height)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(ink)
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "$name profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    ink
                ),
                contentAlignment = Alignment.Center
            ) {
                ProfileWeightText(
                    name.firstOrNull()?.uppercase() ?: "?",
                    weight = appearance.effectiveProfileWeight(),
                    width = appearance.width,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = profileFont
                    ),
                    color = canvas
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalTextApi::class)
private fun ProfileDetail(title: String, value: String, modifier: Modifier = Modifier, appearance: ProfileAppearance, ink: Color) {
    val fontFamily = remember(appearance) {
        ChatFontOption.fromId(appearance.fontId).getFontFamily(
            appearance.weight, appearance.width, grade = 0, opticalSize = 14f
        )
    }
    Column(modifier) {
        ProfileWeightText(
            title,
            weight = appearance.effectiveProfileWeight(),
            width = appearance.width,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = fontFamily),
            color = ink
        )
        Spacer(Modifier.height(5.dp))
        ProfileWeightText(
            value,
            weight = appearance.effectiveProfileWeight(),
            width = appearance.width,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun profileColor(hex: String, fallback: Color): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(fallback)

private fun bestContrastInk(background: Color): Color {
    val luminance = background.luminance()
    val blackContrast = (luminance + 0.05f) / 0.05f
    val whiteContrast = 1.05f / (luminance + 0.05f)
    return if (blackContrast >= whiteContrast) Color(0xFF190626) else Color(0xFFFFF8FF)
}

private fun ProfileAppearance.effectiveProfileWeight(): Int {
    return weight.coerceIn(100, 1000)
}

@Composable
private fun ProfileLoading(insets: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ProfileUnavailable(insets: PaddingValues, canvas: Color, ink: Color, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Profile unavailable", style = MaterialTheme.typography.headlineSmall, color = ink)
            Text(
                "This profile could not be loaded.",
                style = MaterialTheme.typography.bodyMedium,
                color = ink
            )
            AssistChip(
                onClick = onRetry,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = ink, labelColor = canvas, leadingIconContentColor = canvas
                ),
                label = { Text("Try again") }, leadingIcon = {
                Icon(Icons.Default.Refresh, contentDescription = null)
            })
        }
    }
}
