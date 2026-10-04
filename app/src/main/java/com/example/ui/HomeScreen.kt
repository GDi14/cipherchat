package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ConversationEntity
import com.example.data.local.FriendRequestEntity
import com.example.data.local.ProfileEntity
import com.example.ui.components.ConnectedNavButton
import com.example.ui.components.ExpressiveAvatar
import com.example.ui.components.ExpressiveConnectedBottomBar
import com.example.ui.components.ExpressiveContainedLoadingIndicator
import com.example.ui.components.ExpressiveEmptyState
import com.example.ui.components.ExpressiveFabMenu
import com.example.ui.components.FabMenuItem
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.ShieldSuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenConversation: (String) -> Unit,
    onNavigateToNewGroup: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var fabMenuExpanded by remember { mutableStateOf(false) }
    var showCallDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ShieldChat",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AssistChip(
                            onClick = { },
                            label = { Text("BETA") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                },
                actions = {
                    // Profile / Settings Avatar Action matching layout
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.selectTab(2) }
                            .testTag("profile_avatar_action"),
                        contentAlignment = Alignment.Center
                    ) {
                        ExpressiveAvatar(
                            name = "User",
                            size = 36.dp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        },
        bottomBar = {
            val totalUnread = conversations.sumOf { it.unreadCount }
            val pendingCount = pendingRequests.size
            ExpressiveConnectedBottomBar(
                selectedIndex = uiState.selectedTab,
                onSelectIndex = { viewModel.selectTab(it) },
                items = listOf(
                    ConnectedNavButton(
                        label = "Chats",
                        icon = Icons.Default.Chat,
                        badgeCount = totalUnread,
                        testTag = "tab_chats"
                    ),
                    ConnectedNavButton(
                        label = "Friends",
                        icon = Icons.Default.PersonAdd,
                        badgeCount = pendingCount,
                        testTag = "tab_friends"
                    ),
                    ConnectedNavButton(
                        label = "Profile",
                        icon = Icons.Default.Person,
                        badgeCount = 0,
                        testTag = "tab_profile"
                    )
                )
            )
        },
        floatingActionButton = {
            if (uiState.selectedTab == 0 || uiState.selectedTab == 1) {
                ExpressiveFabMenu(
                    expanded = fabMenuExpanded,
                    onToggleExpanded = { fabMenuExpanded = !fabMenuExpanded },
                    onDismiss = { fabMenuExpanded = false },
                    items = listOf(
                        FabMenuItem(
                            id = "new_chat",
                            label = "New Chat",
                            icon = Icons.Default.Chat,
                            testTag = "fab_item_new_chat",
                            onClick = {
                                viewModel.selectTab(1)
                            }
                        ),
                        FabMenuItem(
                            id = "new_group",
                            label = "New Group",
                            icon = Icons.Default.GroupAdd,
                            testTag = "fab_item_new_group",
                            onClick = onNavigateToNewGroup
                        ),
                        FabMenuItem(
                            id = "new_call",
                            label = "New Call",
                            icon = Icons.Default.Call,
                            testTag = "fab_item_new_call",
                            onClick = {
                                showCallDialog = true
                            }
                        )
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Cloud Warning Banner in OutlinedCard
            if (!uiState.isSupabaseConfigured) {
                OutlinedCard(
                    onClick = { viewModel.showSupabaseDialog(true) },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    ListItem(
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        headlineContent = {
                            Text("Supabase Cloud Disconnected", fontWeight = FontWeight.Bold)
                        },
                        supportingContent = {
                            Text("Tap to configure URL and Anon Key to exchange E2EE messages.")
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = uiState.selectedTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(
                                initialOffsetX = { (it * 0.25f).toInt() },
                                animationSpec = tween(
                                    ExpressiveMotion.DurationMedium3,
                                    easing = ExpressiveMotion.EmphasizedDecelerateEasing
                                )
                            ) + fadeIn(tween(ExpressiveMotion.DurationMedium2))) togetherWith
                            (slideOutHorizontally(
                                targetOffsetX = { -(it * 0.2f).toInt() },
                                animationSpec = tween(
                                    ExpressiveMotion.DurationMedium3,
                                    easing = ExpressiveMotion.EmphasizedAccelerateEasing
                                )
                            ) + fadeOut(tween(ExpressiveMotion.DurationShort4)))
                        } else {
                            (slideInHorizontally(
                                initialOffsetX = { -(it * 0.25f).toInt() },
                                animationSpec = tween(
                                    ExpressiveMotion.DurationMedium3,
                                    easing = ExpressiveMotion.EmphasizedDecelerateEasing
                                )
                            ) + fadeIn(tween(ExpressiveMotion.DurationMedium2))) togetherWith
                            (slideOutHorizontally(
                                targetOffsetX = { (it * 0.2f).toInt() },
                                animationSpec = tween(
                                    ExpressiveMotion.DurationMedium3,
                                    easing = ExpressiveMotion.EmphasizedAccelerateEasing
                                )
                            ) + fadeOut(tween(ExpressiveMotion.DurationShort4)))
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "expressive_tab_transition"
                ) { tab ->
                    when (tab) {
                        0 -> M3ChatsTab(
                            conversations = conversations,
                            onOpenConversation = onOpenConversation,
                            onStartGroup = onNavigateToNewGroup,
                            isRefreshing = uiState.isSyncing,
                            onRefresh = { viewModel.sync() }
                        )
                        1 -> M3FriendsTab(
                            viewModel = viewModel,
                            friends = friends,
                            pendingRequests = pendingRequests,
                            onOpenConversationWithFriend = { friend ->
                                val existing = conversations.firstOrNull { it.otherUserId == friend.id }
                                if (existing != null) {
                                    onOpenConversation(existing.id)
                                } else {
                                    viewModel.sync()
                                }
                            }
                        )
                        2 -> M3ProfileTab(viewModel = viewModel)
                    }
                }
            }
        }
    }

    if (uiState.showSupabaseDialog) {
        M3SupabaseAlertDialog(
            currentUrl = uiState.supabaseUrlInput,
            currentKey = uiState.supabaseKeyInput,
            onDismiss = { viewModel.showSupabaseDialog(false) },
            onSave = { url, key -> viewModel.saveSupabaseConfig(url, key) }
        )
    }

    if (showCallDialog) {
        AlertDialog(
            onDismissRequest = { showCallDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Encrypted Voice & Video Call",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Calls are secured with pairwise end-to-end cryptographic keys. Select an accepted friend to initiate a call:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (friends.isEmpty()) {
                        Text(
                            text = "No accepted friends yet. Add friends using their @tag to make encrypted calls.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        friends.take(4).forEach { friend ->
                            OutlinedCard(
                                onClick = {
                                    showCallDialog = false
                                    val existing = conversations.firstOrNull { it.otherUserId == friend.id }
                                    if (existing != null) {
                                        onOpenConversation(existing.id)
                                    } else {
                                        viewModel.sync()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ListItem(
                                    leadingContent = {
                                        ExpressiveAvatar(name = friend.displayName, size = 36.dp)
                                    },
                                    headlineContent = {
                                        Text(friend.displayName, fontWeight = FontWeight.Bold)
                                    },
                                    supportingContent = {
                                        Text("@${friend.tag}")
                                    },
                                    trailingContent = {
                                        FilledTonalIconButton(
                                            onClick = {
                                                showCallDialog = false
                                                val existing = conversations.firstOrNull { it.otherUserId == friend.id }
                                                if (existing != null) {
                                                    onOpenConversation(existing.id)
                                                } else {
                                                    viewModel.sync()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = "Call")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCallDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// ==============================================================================
// 1. Official M3 Chats Tab (SearchBar, SegmentedButton, ListItems)
// ==============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3ChatsTab(
    conversations: List<ConversationEntity>,
    onOpenConversation: (String) -> Unit,
    onStartGroup: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filterLabels = listOf("All", "Direct", "Groups", "Unread")

    val filtered = remember(conversations, searchQuery, selectedFilterIndex) {
        conversations.filter { conv ->
            val matchesQuery = searchQuery.isBlank() ||
                    (conv.title?.contains(searchQuery, ignoreCase = true) == true) ||
                    (conv.otherUserTag?.contains(searchQuery, ignoreCase = true) == true) ||
                    (conv.lastMessageText?.contains(searchQuery, ignoreCase = true) == true)

            val matchesFilter = when (selectedFilterIndex) {
                1 -> conv.type == "direct"
                2 -> conv.type == "group"
                3 -> conv.unreadCount > 0
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = (pullToRefreshState.distanceFraction * 80.dp.toPx()).coerceAtLeast(0f)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing || pullToRefreshState.distanceFraction > 0f) {
                    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                    ContainedLoadingIndicator()
                }
            }
        }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                // M3 Search Field
                // M3 Expressive Search Bar (Pill shaped, prominent upper division)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("chat_search_input"),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search encrypted chats...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                // Official M3 SingleChoiceSegmentedButtonRow
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    filterLabels.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = filterLabels.size),
                            onClick = { selectedFilterIndex = index },
                            selected = selectedFilterIndex == index,
                            label = { Text(label) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (filtered.isEmpty()) {
                item {
                    ExpressiveEmptyState(
                        title = if (searchQuery.isNotBlank()) "No matching chats" else "No Conversations Yet",
                        description = if (searchQuery.isNotBlank()) "Check the spelling or clear the search query."
                        else "Add friends by their unique @tag or create an encrypted group to begin.",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        },
                        actionLabel = if (searchQuery.isBlank()) "Create Group" else null,
                        onAction = if (searchQuery.isBlank()) onStartGroup else null
                    )
                }
            } else {
                items(filtered, key = { it.id }) { conv ->
                    val isGroup = conv.type == "group"
                    val timeFormatted = remember(conv.lastMessageTimestamp) {
                        if (conv.lastMessageTimestamp == 0L) ""
                        else {
                            val date = Date(conv.lastMessageTimestamp)
                            val now = Date()
                            val diffHours = (now.time - date.time) / (1000 * 60 * 60)
                            if (diffHours < 24) {
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                            } else {
                                SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
                            }
                        }
                    }

                    // Official M3 ListItem
                    ListItem(
                        headlineContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = conv.title ?: "Encrypted Chat",
                                    fontWeight = if (conv.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isGroup) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                        Text(
                                            text = "GROUP",
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        },
                        supportingContent = {
                            Text(
                                text = conv.lastMessageText ?: "Encrypted message",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (conv.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (conv.unreadCount > 0) FontWeight.Medium else FontWeight.Normal
                            )
                        },
                        leadingContent = {
                            ExpressiveAvatar(name = conv.title ?: "Chat", size = 48.dp)
                        },
                        trailingContent = {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (timeFormatted.isNotBlank()) {
                                    Text(
                                        text = timeFormatted,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (conv.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                                if (conv.unreadCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text(
                                            text = conv.unreadCount.toString(),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (conv.unreadCount > 0) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenConversation(conv.id) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

// ==============================================================================
// 2. Official M3 Friends Tab (ElevatedCards, ListItems, Buttons)
// ==============================================================================

@Composable
fun M3FriendsTab(
    viewModel: HomeViewModel,
    friends: List<ProfileEntity>,
    pendingRequests: List<FriendRequestEntity>,
    onOpenConversationWithFriend: (ProfileEntity) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search Friend Card (M3 ElevatedCard)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Add Friend by @tag",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Both users establish a pairwise Double Ratchet session upon connection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = uiState.searchTag,
                    onValueChange = { viewModel.onSearchTagChanged(it) },
                    placeholder = { Text("e.g. bob") },
                    prefix = { Text("@", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        FilledTonalIconButton(
                            onClick = { viewModel.searchTag() },
                            modifier = Modifier.testTag("search_friend_button")
                        ) {
                            if (uiState.isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "Search Tag")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("friend_tag_input")
                )

                uiState.searchError?.let { err ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Search Result (M3 OutlinedCard)
                uiState.searchResult?.let { found ->
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            leadingContent = { ExpressiveAvatar(name = found.displayName, size = 44.dp) },
                            headlineContent = { Text(found.displayName, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("@${found.tag}", color = MaterialTheme.colorScheme.primary) },
                            trailingContent = {
                                Button(
                                    onClick = { viewModel.sendFriendRequest(found) },
                                    modifier = Modifier.testTag("add_friend_button")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add")
                                }
                            }
                        )
                    }
                }
            }
        }

        // Pending Friend Requests (M3 OutlinedCards & ListItems)
        if (pendingRequests.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Pending Requests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                        Text(pendingRequests.size.toString())
                    }
                }

                pendingRequests.forEach { req ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            leadingContent = { ExpressiveAvatar(name = req.senderName, size = 44.dp) },
                            headlineContent = { Text(req.senderName, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("@${req.senderTag}") },
                            trailingContent = {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { viewModel.acceptFriendRequest(req) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("accept_request_button")
                                    ) {
                                        Text("Accept")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.declineFriendRequest(req.id) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("decline_request_button")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Decline", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Friends List (M3 ListItems)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Friends (${friends.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (friends.isEmpty()) {
                ExpressiveEmptyState(
                    title = "No Friends Added",
                    description = "Search for a tag above to connect and start direct messaging.",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                )
            } else {
                friends.forEach { friend ->
                    OutlinedCard(
                        onClick = { onOpenConversationWithFriend(friend) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            leadingContent = {
                                ExpressiveAvatar(name = friend.displayName, avatarUrl = friend.avatarPath, size = 44.dp)
                            },
                            headlineContent = { Text(friend.displayName, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("@${friend.tag}", color = MaterialTheme.colorScheme.primary) },
                            trailingContent = {
                                FilledTonalButton(onClick = { onOpenConversationWithFriend(friend) }) {
                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Chat")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 3. Official M3 Profile Tab (ElevatedCards, ListItems, Buttons)
// ==============================================================================

@Composable
fun M3ProfileTab(viewModel: HomeViewModel) {
    val authRepo = viewModel.authRepository
    val supabaseProvider = viewModel.supabaseProvider
    val clipboardManager = LocalClipboardManager.current

    var displayName by remember { mutableStateOf(authRepo.getDisplayName()) }
    var tag by remember { mutableStateOf(authRepo.getTag()) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.updateProfile(displayName, tag, uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar Box with Photo Picker
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val avatarUri = authRepo.getAvatarPath()
            if (!avatarUri.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().clip(CircleShape)
                )
            } else {
                Text(
                    text = displayName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            FilledTonalIconButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Edit photo", modifier = Modifier.size(16.dp))
            }
        }

        // Profile Editing ElevatedCard
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Profile Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )

                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it.trim().removePrefix("@").lowercase() },
                    label = { Text("Unique Tag") },
                    prefix = { Text("@", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_tag_input")
                )

                Button(
                    onClick = { viewModel.updateProfile(displayName, tag, null) },
                    modifier = Modifier.fillMaxWidth().testTag("save_profile_button")
                ) {
                    Text("Save Changes")
                }
            }
        }

        // Security Hardware Details in OutlinedCard with M3 ListItems
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                ListItem(
                    leadingContent = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("Cryptographic Architecture", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("libsignal Double Ratchet + X3DH Key Exchange") }
                )
                HorizontalDivider()
                ListItem(
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null, tint = ShieldSuccessGreen) },
                    headlineContent = { Text("Key Storage Isolation", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Android KeyStore (Hardware Isolated)") }
                )
                HorizontalDivider()
                ListItem(
                    leadingContent = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("Device UUID", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("${authRepo.myDeviceId.take(16)}...", fontFamily = FontFamily.Monospace) },
                    trailingContent = {
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(authRepo.myDeviceId)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    }
                )
                HorizontalDivider()
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = if (supabaseProvider.isConfigured) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (supabaseProvider.isConfigured) ShieldSuccessGreen else MaterialTheme.colorScheme.error
                        )
                    },
                    headlineContent = { Text("Supabase Cloud Relay", fontWeight = FontWeight.Bold) },
                    supportingContent = {
                        Text(if (supabaseProvider.isConfigured) "Connected" else "Disconnected")
                    },
                    trailingContent = {
                        FilledTonalButton(
                            onClick = { viewModel.showSupabaseDialog(true) },
                            modifier = Modifier.testTag("open_supabase_config_button")
                        ) {
                            Text("Configure")
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContainedLoadingIndicator(
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 3.dp,
        modifier = modifier.size(48.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            LoadingIndicator(
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

