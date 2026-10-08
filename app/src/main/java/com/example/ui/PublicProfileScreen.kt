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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ConversationEntity
import com.example.data.local.FriendRequestEntity
import com.example.data.local.ProfileEntity
import com.example.repository.FriendRepository
import com.example.ui.theme.GoogleSansFlexDisplay
import com.example.ui.theme.GoogleSansFlexHeadline
import com.example.ui.theme.LocalExpressiveMotionEnabled

private val ProfileCanvas = Color(0xFFC987FA)
private val ProfileInk = Color(0xFF190626)

@OptIn(ExperimentalMaterial3Api::class)
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
    var isLoading by remember(userId) { mutableStateOf(true) }
    var reloadKey by remember(userId) { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val uiState by homeViewModel.uiState.collectAsState()
    val motionEnabled = LocalExpressiveMotionEnabled.current
    val isFriend = friends.any { it.id == userId } || profile?.isFriend == true
    val requestPending = requests.any {
        it.status == "pending" && it.senderId == friendRepository.myUserId && it.receiverId == userId
    }

    LaunchedEffect(userId, reloadKey) {
        isLoading = true
        profile = friendRepository.getProfileById(userId)
        isLoading = false
    }
    LaunchedEffect(uiState.statusMessage, uiState.errorMessage) {
        uiState.statusMessage?.let { snackbar.showSnackbar(it); homeViewModel.clearStatus() }
        uiState.errorMessage?.let { snackbar.showSnackbar(it); homeViewModel.clearError() }
    }

    Scaffold(
        containerColor = ProfileCanvas,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ProfileInk)
                    }
                },
                title = {
                    Text(
                        profile?.let { "@${it.tag}" } ?: "PROFILE",
                        style = MaterialTheme.typography.titleMedium,
                        color = ProfileInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ProfileCanvas)
            )
        }
    ) { insets ->
        when {
            isLoading -> ProfileLoading(insets)
            profile == null -> ProfileUnavailable(insets) { reloadKey++ }
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
                            ProfileDisplayName(user.displayName, contentWidth - 48.dp)
                            Spacer(Modifier.height(18.dp))
                            ProfilePortrait(user.displayName, user.avatarPath, photoHeight)

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
                                        ProfileDetail(
                                            title = "HANDLE",
                                            value = "@${user.tag}",
                                            modifier = Modifier.weight(1f)
                                        )
                                        ProfileDetail(
                                            title = "CONNECTION",
                                            value = if (isFriend) "Friends" else "Cipher Chat",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Text(
                                        "A little closer, one conversation at a time.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = ProfileInk.copy(alpha = 0.78f)
                                    )

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
                                                containerColor = ProfileInk,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                                            Spacer(Modifier.width(10.dp))
                                            Text("Message")
                                        }
                                    } else {
                                        Button(
                                            onClick = { if (!requestPending) homeViewModel.sendFriendRequest(user) },
                                            enabled = !requestPending && !uiState.isSearching,
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            shape = CircleShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ProfileInk,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(
                                                if (requestPending) Icons.Default.Schedule else Icons.Default.PersonAdd,
                                                contentDescription = null
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(if (requestPending) "Request sent" else "Add friend")
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
private fun ProfileDisplayName(name: String, availableWidth: Dp) {
    val title = name.uppercase()
    val glyphSize = ((availableWidth.value - 12f) / (title.length.coerceAtLeast(1) * 0.68f))
        .coerceIn(28f, 96f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        title.forEachIndexed { index, glyph ->
            Text(
                text = glyph.toString(),
                modifier = Modifier.rotate(
                    when {
                        glyph.isWhitespace() -> 0f
                        index % 3 == 0 -> -4f
                        index % 3 == 1 -> 2.5f
                        else -> -1.5f
                    }
                ),
                style = TextStyle(
                    fontFamily = if (index % 3 == 0) GoogleSansFlexHeadline else GoogleSansFlexDisplay,
                    fontWeight = if (index % 3 == 0) FontWeight.Bold else FontWeight.ExtraBold,
                    fontSize = glyphSize.sp,
                    lineHeight = (glyphSize * 1.04f).sp,
                    letterSpacing = (-3).sp
                ),
                color = ProfileInk,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProfilePortrait(name: String, avatarUrl: String?, height: Dp) {
    Box(
        Modifier.fillMaxWidth().height(height)
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.tertiary, ProfileInk)))
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
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primary, ProfileInk)
                    )
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.60f to Color.Transparent,
                    1f to ProfileInk.copy(alpha = 0.52f)
                )
            )
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
            shape = CircleShape,
            color = ProfileCanvas.copy(alpha = 0.93f),
            contentColor = ProfileInk
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("CIPHER CHAT", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ProfileDetail(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = ProfileInk.copy(alpha = 0.62f)
        )
        Spacer(Modifier.height(5.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = ProfileInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProfileLoading(insets: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ProfileUnavailable(insets: PaddingValues, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Profile unavailable", style = MaterialTheme.typography.headlineSmall, color = ProfileInk)
            Text(
                "This profile could not be loaded.",
                style = MaterialTheme.typography.bodyMedium,
                color = ProfileInk.copy(alpha = 0.7f)
            )
            AssistChip(onClick = onRetry, label = { Text("Try again") }, leadingIcon = {
                Icon(Icons.Default.Refresh, contentDescription = null)
            })
        }
    }
}
