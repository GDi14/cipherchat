package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.ContextCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ConversationEntity
import com.example.data.local.FriendRequestEntity
import com.example.data.local.ProfileEntity
import com.example.utils.AppUpdater
import kotlinx.coroutines.launch
import com.example.ui.components.AppNavigationBar
import com.example.ui.components.AppNavigationDestination
import com.example.ui.components.AppNavigationRail
import com.example.ui.components.ExpressiveAvatar
import com.example.ui.components.ExpressiveEmptyState
import com.example.ui.components.ExpressiveFabMenu
import com.example.ui.components.ExpressiveContainedLoadingIndicator
import com.example.ui.components.FabMenuItem
import com.example.ui.theme.ExpressiveTopSurfaceShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, onOpenConversation: (String) -> Unit, onNavigateToNewGroup: () -> Unit,
               onOpenAddFriends: () -> Unit, onOpenNotifications: () -> Unit,
               onOpenSettings: () -> Unit, onOpenProfile: (String) -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val requests by viewModel.pendingRequests.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var fabOpen by remember { mutableStateOf(false) }
    var pageSearchQuery by remember { mutableStateOf("") }
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val unreadCount = remember(conversations) { conversations.sumOf { it.unreadCount } }
    val directConversations = remember(conversations) { conversations.filter { it.type == "direct" } }
    val groupConversations = remember(conversations) { conversations.filter { it.type == "group" } }
    val destinations = remember(unreadCount) {
        listOf(
            AppNavigationDestination("Chats", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat, unreadCount, "tab_chats"),
            AppNavigationDestination("Groups", Icons.Default.GroupAdd, Icons.Outlined.GroupAdd, 0, "tab_groups"),
            AppNavigationDestination("Calls", Icons.Default.Call, Icons.Outlined.Call, 0, "tab_calls")
        )
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }
    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { snackbar.showSnackbar(it); viewModel.clearStatus() }
    }
    LaunchedEffect(state.selectedTab) { pageSearchQuery = "" }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Column(Modifier.widthIn(max = 1040.dp).fillMaxWidth().padding(horizontal = 20.dp)) {
                    Row(
                        Modifier.fillMaxWidth().height(88.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("CIPHER CHAT  ·  PRIVATE BY DESIGN", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                            Text(
                                when (state.selectedTab) { 1 -> "Your groups"; 2 -> "Calls"; else -> "Messages" },
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onOpenNotifications, modifier = Modifier.testTag("notifications_action")) {
                            androidx.compose.material3.BadgedBox(badge = { if (requests.isNotEmpty()) Badge { Text("${requests.size}") } }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            }
                        }
                        IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("settings_action")) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                    Box(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                        OutlinedTextField(
                            value = pageSearchQuery,
                            onValueChange = { pageSearchQuery = it },
                            placeholder = { Text(when (state.selectedTab) {
                                1 -> "Search groups"
                                2 -> "Search calls"
                                else -> "Search people or messages"
                            }) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth().height(58.dp).testTag("chat_search_input")
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (!wide) AppNavigationBar(state.selectedTab, viewModel::selectTab, destinations)
        },
        floatingActionButton = {
            ExpressiveFabMenu(
                    expanded = fabOpen,
                    onToggleExpanded = { fabOpen = !fabOpen },
                    onDismiss = { fabOpen = false },
                    items = listOf(
                        FabMenuItem("new_group", "Create a group", Icons.Default.GroupAdd, "fab_item_new_group", onNavigateToNewGroup),
                        FabMenuItem("add_friend", "Add friends", Icons.Default.PersonAdd, "fab_item_add_friends", onOpenAddFriends)
                    )
                )
        }
    ) { insets ->
        Row(Modifier.fillMaxSize().padding(insets)) {
            if (wide) AppNavigationRail(state.selectedTab, viewModel::selectTab, destinations)
            Box(Modifier.weight(1f).fillMaxSize().widthIn(max = 1040.dp), contentAlignment = Alignment.TopCenter) {
                AnimatedContent(
                    targetState = state.selectedTab,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(
                            animationSpec = tween(240, easing = FastOutSlowInEasing),
                            initialOffsetX = { width -> direction * width / 5 }
                        ) + fadeIn(animationSpec = tween(180))) togetherWith
                            (slideOutHorizontally(
                                animationSpec = tween(200, easing = FastOutSlowInEasing),
                                targetOffsetX = { width -> -direction * width / 6 }
                            ) + fadeOut(animationSpec = tween(140)))
                    },
                    label = "home_destination"
                ) { tab ->
                    when (tab) {
                        1 -> M3ChatsTab(groupConversations, onOpenConversation, onNavigateToNewGroup,
                            state.isSyncing, viewModel::sync, scopeKey = "groups", searchQuery = pageSearchQuery,
                            onSearchQueryChange = { pageSearchQuery = it }, showSearchField = false, onOpenProfile = onOpenProfile)
                        2 -> M3CallsTab(searchQuery = pageSearchQuery)
                        else -> M3ChatsTab(directConversations, onOpenConversation, onNavigateToNewGroup,
                            state.isSyncing, viewModel::sync, friends, onOpenAddFriends,
                            state.favoriteFriendIds, scopeKey = "chats", searchQuery = pageSearchQuery,
                            onSearchQueryChange = { pageSearchQuery = it }, showSearchField = false, onOpenProfile = onOpenProfile,
                            onOpenConversationWithFriend = { friend ->
                                viewModel.openConversationWithFriend(friend, onOpenConversation)
                            })
                    }
                }
            }
        }
    }

    if (state.showSupabaseDialog) {
        M3SupabaseAlertDialog(state.supabaseUrlInput, state.supabaseKeyInput,
            { viewModel.showSupabaseDialog(false) }, viewModel::saveSupabaseConfig)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3ChatsTab(conversations: List<ConversationEntity>, onOpenConversation: (String) -> Unit,
               onStartGroup: () -> Unit, isRefreshing: Boolean, onRefresh: () -> Unit,
               friends: List<ProfileEntity> = emptyList(), onAddFriends: (() -> Unit)? = null,
               favoriteFriendIds: Set<String> = emptySet(), scopeKey: String = "chats",
               searchQuery: String? = null, onSearchQueryChange: ((String) -> Unit)? = null,
               showSearchField: Boolean = true, onOpenProfile: (String) -> Unit = {},
               onOpenConversationWithFriend: (ProfileEntity) -> Unit = {}) {
    var localQuery by remember(scopeKey) { mutableStateOf("") }
    val query = searchQuery ?: localQuery
    val updateQuery: (String) -> Unit = onSearchQueryChange ?: { localQuery = it }
    var filter by remember(scopeKey) { mutableIntStateOf(0) }
    val filters = listOf("All", "Read", "Unread")
    val horizontalMargin = if (LocalConfiguration.current.screenWidthDp >= 600) 24.dp else 16.dp
    val visible = remember(conversations, query, filter) {
        conversations.filter { conversation ->
            val matchesText = query.isBlank() || listOfNotNull(conversation.title, conversation.otherUserTag,
                conversation.lastMessageText).any { it.contains(query, true) }
            matchesText && when (filter) {
                1 -> conversation.unreadCount == 0
                2 -> conversation.unreadCount > 0
                else -> true
            }
        }
    }
    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = ExpressiveTopSurfaceShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = Modifier.widthIn(max = 980.dp).fillMaxSize(),
                    contentPadding = PaddingValues(start = horizontalMargin, end = horizontalMargin, top = 20.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
            if (showSearchField) {
                item {
                    OutlinedTextField(
                        value = query, onValueChange = updateQuery,
                        placeholder = { Text("Search people or messages") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true, shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("chat_search_input")
                    )
                }
            }
            if (scopeKey == "chats") {
                item(key = "your_circle_card") {
                    AnimatedVisibility(
                        visible = filter == 0 && query.isBlank(),
                        enter = expandVertically(
                            expandFrom = Alignment.Top,
                            animationSpec = tween(320, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(180)),
                        exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(
                            shrinkTowards = Alignment.Top,
                            animationSpec = tween(240, easing = FastOutSlowInEasing)
                        )
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(start = 22.dp, end = 18.dp, top = 22.dp, bottom = 18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Your Circle", style = MaterialTheme.typography.titleLarge)
                                    }
                                    onAddFriends?.let {
                                        androidx.compose.material3.FilledTonalIconButton(onClick = it) {
                                            Icon(Icons.Default.PersonAdd, contentDescription = "Add friends")
                                        }
                                    }
                                }
                                val favoriteFriends = friends.filter { it.id in favoriteFriendIds }
                                if (favoriteFriends.isEmpty()) {
                                    Text(
                                        "Favorite friends to keep them close here.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                } else {
                                    Spacer(Modifier.height(14.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(favoriteFriends, key = { it.id }) { friend ->
                                            val conversation = conversations.firstOrNull { it.otherUserId == friend.id && it.type == "direct" }
                                            Surface(
                                                onClick = {
                                                    conversation?.let { onOpenConversation(it.id) }
                                                        ?: onOpenConversationWithFriend(friend)
                                                },
                                                shape = CircleShape,
                                                color = Color.Transparent,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.width(96.dp).testTag("circle_friend_${friend.id}")
                                            ) {
                                                Column(
                                                    Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    ExpressiveAvatar(friend.displayName, friend.avatarPath, 56.dp,
                                                        modifier = Modifier.clickable { onOpenProfile(friend.id) })
                                                    Text(friend.displayName, style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (filter == 0) "Recent messages" else "Your conversations", style = MaterialTheme.typography.titleLarge)
                    }
                    if (visible.isNotEmpty()) Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer) { Text("${visible.size}") }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(filters) { index, label ->
                        FilterChip(selected = filter == index, onClick = { filter = index }, label = { Text(label) },
                            shape = CircleShape)
                    }
                }
            }
            if (visible.isEmpty()) {
                item {
                    ExpressiveEmptyState(
                        title = if (query.isNotBlank()) "No conversations found" else "Start with someone you trust",
                        description = if (query.isNotBlank()) "Try another name or clear your search."
                        else "Add a friend or create an encrypted group to begin chatting.",
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp)) },
                        actionLabel = if (query.isBlank()) "Create a group" else null,
                        onAction = if (query.isBlank()) onStartGroup else null
                    )
                }
            } else {
                items(visible, key = { it.id }) { conversation ->
                    val time = remember(conversation.lastMessageTimestamp) {
                        if (conversation.lastMessageTimestamp == 0L) "" else {
                            val date = Date(conversation.lastMessageTimestamp)
                            val hours = (System.currentTimeMillis() - date.time) / 3_600_000
                            SimpleDateFormat(if (hours < 24) "HH:mm" else "MMM d", Locale.getDefault()).format(date)
                        }
                    }
                    Surface(
                        onClick = { onOpenConversation(conversation.id) },
                        shape = MaterialTheme.shapes.extraLarge,
                        color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            leadingContent = {
                                ExpressiveAvatar(conversation.title ?: "Chat", size = 56.dp,
                                    modifier = Modifier.clickable(enabled = conversation.type == "direct" && conversation.otherUserId != null) {
                                        conversation.otherUserId?.let(onOpenProfile)
                                    })
                            },
                            headlineContent = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(conversation.title ?: "Encrypted conversation", style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                }
                            },
                            supportingContent = {
                                val preview = conversation.lastMessageText ?: "Messages are end-to-end encrypted"
                                val normalizedPreview = preview.removeSurrounding("[", "]").trim()
                                val previewType = when {
                                    normalizedPreview.startsWith("voice note", ignoreCase = true) || normalizedPreview.startsWith("audio", ignoreCase = true) -> "audio"
                                    normalizedPreview.startsWith("video", ignoreCase = true) -> "video"
                                    normalizedPreview.startsWith("photo", ignoreCase = true) || normalizedPreview.startsWith("image", ignoreCase = true) -> "image"
                                    else -> null
                                }
                                if (previewType == null) {
                                    Text(preview, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (previewType) {
                                                "audio" -> Icons.Default.Mic
                                                "video" -> Icons.Default.Videocam
                                                else -> Icons.Default.Image
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = normalizedPreview,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    if (time.isNotBlank()) Text(time, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (conversation.unreadCount > 0) Badge { Text("${conversation.unreadCount}") }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )
                    }
                }
            }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3FriendsTab(viewModel: HomeViewModel, friends: List<ProfileEntity>, pendingRequests: List<FriendRequestEntity>,
                 onBack: (() -> Unit)? = null, onOpenProfile: (String) -> Unit = {},
                 onOpenConversationWithFriend: (ProfileEntity) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            onBack?.let { back ->
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = back, modifier = Modifier.testTag("back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = { Text("Friends") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        }
    ) { pagePadding ->
    Box(Modifier.fillMaxSize().padding(pagePadding), contentAlignment = Alignment.TopCenter) {
    Column(Modifier.widthIn(max = 900.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("YOUR PEOPLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text("Find your people", style = MaterialTheme.typography.headlineSmall)
            Text("Connect by tag and keep every conversation private.", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add a friend", style = MaterialTheme.typography.titleLarge)
                Text("Enter someone's unique tag to find them.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = state.searchTag, onValueChange = viewModel::onSearchTagChanged,
                    label = { Text("Friend tag") }, prefix = { Text("@", color = MaterialTheme.colorScheme.primary) },
                    singleLine = true, modifier = Modifier.fillMaxWidth().testTag("friend_tag_input"),
                    trailingIcon = {
                        FilledTonalIconButton(onClick = { viewModel.searchTag() }, modifier = Modifier.testTag("search_friend_button")) {
                            if (state.isSearching) ExpressiveContainedLoadingIndicator(Modifier.size(24.dp), size = 16.dp)
                            else Icon(Icons.Default.Search, contentDescription = "Search tag")
                        }
                    }
                )
                state.searchError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                state.searchResult?.let { result ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            leadingContent = { ExpressiveAvatar(result.displayName, result.avatarPath, 48.dp,
                                modifier = Modifier.clickable { onOpenProfile(result.id) }) },
                            headlineContent = { Text(result.displayName) }, supportingContent = { Text("@${result.tag}") },
                            trailingContent = { Button(onClick = { viewModel.sendFriendRequest(result) }, modifier = Modifier.testTag("add_friend_button")) {
                                Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(6.dp)); Text("Add")
                            } }
                        )
                    }
                }
            }
        }
        if (pendingRequests.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Friend requests · ${pendingRequests.size}", style = MaterialTheme.typography.titleLarge)
                pendingRequests.forEach { request ->
                    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            leadingContent = { ExpressiveAvatar(request.senderName, size = 48.dp) },
                            headlineContent = { Text(request.senderName) }, supportingContent = { Text("@${request.senderTag}") },
                            trailingContent = {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(onClick = { viewModel.acceptFriendRequest(request) }, modifier = Modifier.testTag("accept_request_button")) { Text("Accept") }
                                    OutlinedButton(onClick = { viewModel.declineFriendRequest(request.id) }, modifier = Modifier.testTag("decline_request_button")) { Text("Decline") }
                                }
                            }, colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Friends", style = MaterialTheme.typography.titleLarge)
            if (friends.isEmpty()) {
                ExpressiveEmptyState("No friends yet", "Search for a tag above to connect and start a private conversation.",
                    icon = { Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) })
            } else friends.forEach { friend ->
                Surface(onClick = { onOpenConversationWithFriend(friend) }, shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        leadingContent = { ExpressiveAvatar(friend.displayName, friend.avatarPath, 52.dp,
                            modifier = Modifier.clickable { onOpenProfile(friend.id) }) },
                        headlineContent = { Text(friend.displayName, style = MaterialTheme.typography.titleMedium) },
                        supportingContent = { Text("@${friend.tag}") },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.toggleFavoriteFriend(friend.id) },
                                    modifier = Modifier.testTag("favorite_friend_${friend.id}")
                                ) {
                                    val isFavorite = friend.id in state.favoriteFriendIds
                                    Icon(
                                        if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = if (isFavorite) "Remove ${friend.displayName} from Your Circle"
                                        else "Add ${friend.displayName} to Your Circle",
                                        tint = if (isFavorite) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalIconButton(onClick = { onOpenConversationWithFriend(friend) }) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message ${friend.displayName}")
                                }
                            }
                        }, colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            }
        }
        if (!state.isSupabaseConfigured) {
            OutlinedCard(onClick = { viewModel.showSupabaseDialog(true) }, modifier = Modifier.fillMaxWidth()) {
                ListItem(leadingContent = { Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.error) },
                    headlineContent = { Text("Connect cloud relay") }, supportingContent = { Text("Configure sync to discover friends across devices.") })
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3CallsTab(searchQuery: String = "") {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = ExpressiveTopSurfaceShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("RECENT ACTIVITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text("Call history", style = MaterialTheme.typography.headlineSmall)
            ExpressiveEmptyState(
                if (searchQuery.isBlank()) "No calls yet" else "No matching calls",
                if (searchQuery.isBlank()) "Your recent calls will appear here." else "Try another search.",
                icon = { Icon(Icons.Default.Call, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3NotificationsPage(viewModel: HomeViewModel, requests: List<FriendRequestEntity>, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Notifications") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { pagePadding ->
    Box(Modifier.fillMaxSize().padding(pagePadding), contentAlignment = Alignment.TopCenter) {
    Column(Modifier.widthIn(max = 900.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("YOUR INBOX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text("Notifications", style = MaterialTheme.typography.headlineSmall)
        if (requests.isEmpty()) ExpressiveEmptyState("You're all caught up", "New friend requests will appear here.",
            icon = { Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary) })
        requests.forEach { request ->
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()) {
                ListItem(leadingContent = { ExpressiveAvatar(request.senderName, size = 48.dp) },
                    headlineContent = { Text("${request.senderName} sent a friend request") },
                    supportingContent = { Text("@${request.senderTag}") },
                    trailingContent = { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { viewModel.acceptFriendRequest(request) }) { Text("Accept") }
                        TextButton(onClick = { viewModel.declineFriendRequest(request.id) }) { Text("Decline") }
                    } }, colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent))
            }
        }
    }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3SettingsPage(
    onBack: () -> Unit,
    onOpenProfileSettings: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { pagePadding ->
        Box(Modifier.fillMaxSize().padding(pagePadding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 900.dp).fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("MAKE IT YOURS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text("Settings", style = MaterialTheme.typography.headlineSmall)
                Text("Account", style = MaterialTheme.typography.titleLarge)
                Surface(
                    onClick = onOpenProfileSettings,
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().testTag("open_profile_settings")
                ) {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        headlineContent = { Text("Profile settings") },
                        supportingContent = { Text("Update your name, tag, and profile photo") },
                        trailingContent = { Text("Open", color = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }

                Text("Preferences", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                Surface(
                    onClick = onOpenAppSettings,
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().testTag("open_app_settings")
                ) {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        headlineContent = { Text("App settings") },
                        supportingContent = { Text("Privacy, sync, and appearance options") },
                        trailingContent = { Text("Open", color = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3AppSettingsPage(viewModel: HomeViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var notificationsAllowed by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notificationsAllowed = it }
    val scope = rememberCoroutineScope()
    var updateMessage by remember { mutableStateOf("Check GitHub for the latest signed alpha APK.") }
    var release by remember { mutableStateOf<AppUpdater.ReleaseInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var showReleaseNotes by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("App settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { pagePadding ->
    Box(Modifier.fillMaxSize().padding(pagePadding), contentAlignment = Alignment.TopCenter) {
    Column(Modifier.widthIn(max = 900.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("PREFERENCES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text("App settings", style = MaterialTheme.typography.headlineSmall)
        Text("Privacy and sync", style = MaterialTheme.typography.titleLarge)
        ListItem(leadingContent = { Icon(Icons.Default.Security, null) }, headlineContent = { Text("End-to-end encryption") },
            supportingContent = { Text("Messages are encrypted on this device") })
        Text("Notifications", style = MaterialTheme.typography.titleLarge)
        ListItem(
            leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
            headlineContent = { Text(if (notificationsAllowed) "Message alerts enabled" else "Message alerts disabled") },
            supportingContent = { Text("Notifications use generic text so encrypted message content stays private.") },
            trailingContent = {
                FilledTonalButton(
                    enabled = !notificationsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                    onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                ) { Text(if (notificationsAllowed) "Enabled" else "Enable") }
            }
        )
        Text("Application updates", style = MaterialTheme.typography.titleLarge)
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(updateMessage, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                release?.let { latest ->
                    TextButton(onClick = { showReleaseNotes = true }) {
                        Text("View release notes · ${latest.tagName}")
                    }
                }
                if (isDownloadingUpdate) {
                    LinearProgressIndicator(progress = { downloadProgress / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("Downloading update · $downloadProgress%", style = MaterialTheme.typography.labelLarge)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        enabled = !isCheckingUpdate && !isDownloadingUpdate,
                        onClick = {
                            isCheckingUpdate = true
                            updateMessage = "Checking for updates…"
                            scope.launch {
                                val found = AppUpdater.checkForUpdate(context)
                                isCheckingUpdate = false
                                release = found
                                updateMessage = when {
                                    found == null -> "Couldn't find a release APK. Check your connection and try again."
                                    AppUpdater.isNewerRelease(found.tagName, AppUpdater.getCurrentVersionName(context)) ->
                                        "Version ${found.tagName} is available. Review the release notes before installing."
                                    else -> "You're up to date · ${AppUpdater.getCurrentVersionName(context)}"
                                }
                            }
                        }
                    ) {
                        if (isCheckingUpdate) ExpressiveContainedLoadingIndicator(Modifier.size(20.dp), size = 16.dp)
                        else Text("Check for updates")
                    }
                    val available = release?.takeIf {
                        AppUpdater.isNewerRelease(it.tagName, AppUpdater.getCurrentVersionName(context))
                    }
                    available?.let { latest ->
                        FilledTonalButton(
                            enabled = !isDownloadingUpdate,
                            onClick = {
                                isDownloadingUpdate = true
                                downloadProgress = 0
                                scope.launch {
                                    AppUpdater.downloadAndInstall(
                                        context = context,
                                        apkUrl = latest.apkUrl,
                                        onProgress = { downloadProgress = it },
                                        onError = { message ->
                                            isDownloadingUpdate = false
                                            updateMessage = "Update download failed: $message"
                                        }
                                    )
                                    if (isDownloadingUpdate) {
                                        isDownloadingUpdate = false
                                        updateMessage = "Android's package installer was opened. Follow its prompts to finish updating."
                                    }
                                }
                            }
                        ) { Text("Install ${latest.tagName}") }
                    }
                }
            }
        }
        ListItem(leadingContent = { Icon(Icons.Default.CloudDone, null) }, headlineContent = { Text("Cloud relay") },
            supportingContent = { Text(if (viewModel.supabaseProvider.isConfigured) "Connected" else "Not configured") },
            trailingContent = { FilledTonalButton(onClick = { viewModel.showSupabaseDialog(true) }) { Text("Configure") } })
        Text("Appearance and motion", style = MaterialTheme.typography.titleLarge)
        ListItem(
            leadingContent = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            headlineContent = { Text("Voice of User") },
            supportingContent = { Text("Subtly emphasizes expressive words after sending. Message text stays unchanged.") },
            trailingContent = { Switch(checked = state.voiceOfUserEnabled, onCheckedChange = viewModel::setVoiceOfUserEnabled) }
        )
    }
    }
    }
    if (state.showSupabaseDialog) M3SupabaseAlertDialog(state.supabaseUrlInput, state.supabaseKeyInput,
        { viewModel.showSupabaseDialog(false) }, viewModel::saveSupabaseConfig)
    if (showReleaseNotes) {
        val latest = release
        if (latest != null) {
            AlertDialog(
                onDismissRequest = { showReleaseNotes = false },
                title = { Text("Release ${latest.tagName}") },
                text = {
                    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                        Text(latest.releaseNotes.ifBlank { "No release notes were provided." })
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showReleaseNotes = false }) { Text("Done") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3ProfileTab(viewModel: HomeViewModel, onBack: (() -> Unit)? = null) {
    val auth = viewModel.authRepository
    val cloud = viewModel.supabaseProvider
    val clipboard = LocalClipboardManager.current
    var name by remember { mutableStateOf(auth.getDisplayName()) }
    var tag by remember { mutableStateOf(auth.getTag()) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.updateProfile(name, tag, uri)
    }
    Scaffold(
        topBar = {
            onBack?.let { back ->
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = back, modifier = Modifier.testTag("back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = { Text("Your profile") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        }
    ) { pagePadding ->
    Box(Modifier.fillMaxSize().padding(pagePadding), contentAlignment = Alignment.TopCenter) {
    Column(Modifier.widthIn(max = 900.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("ACCOUNT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text("Your profile", style = MaterialTheme.typography.headlineSmall)
        }
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.size(92.dp).clip(CircleShape).testTag("profile_avatar_picker")) {
                    val avatar = auth.getAvatarPath()
                    if (!avatar.isNullOrBlank()) AsyncImage(avatar, contentDescription = "Change profile photo",
                        modifier = Modifier.fillMaxSize().clip(CircleShape))
                    else ExpressiveAvatar(name, size = 76.dp)
                }
                Text(name, style = MaterialTheme.typography.headlineSmall)
                Text("@${auth.getTag()}", style = MaterialTheme.typography.bodyLarge)
                Text("Your identity is protected on this device", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Card(shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Profile details", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(name, { name = it }, label = { Text("Display name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input"))
                OutlinedTextField(tag, { tag = it.trim().removePrefix("@").lowercase() }, label = { Text("Unique tag") },
                    prefix = { Text("@") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("profile_tag_input"))
                Button(onClick = { viewModel.updateProfile(name, tag, null) }, modifier = Modifier.fillMaxWidth().testTag("save_profile_button")) {
                    Text("Save profile")
                }
            }
        }
        Card(shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Security & devices", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(20.dp))
                ListItem(leadingContent = { Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("End-to-end encryption") }, supportingContent = { Text("libsignal Double Ratchet + X3DH") },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent))
                ListItem(leadingContent = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("Private key storage") }, supportingContent = { Text("Android hardware-backed KeyStore") },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent))
                ListItem(leadingContent = { Icon(Icons.Default.VpnKey, null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("Device identifier") }, supportingContent = { Text("${auth.myDeviceId.take(16)}…", fontFamily = FontFamily.Monospace) },
                    trailingContent = { IconButton(onClick = { clipboard.setText(AnnotatedString(auth.myDeviceId)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy device identifier")
                    } }, colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent))
                ListItem(leadingContent = { Icon(if (cloud.isConfigured) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    null, tint = if (cloud.isConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) },
                    headlineContent = { Text("Cloud relay") },
                    supportingContent = { Text(if (cloud.isConfigured) "Connected" else "Not configured") },
                    trailingContent = { FilledTonalButton(onClick = { viewModel.showSupabaseDialog(true) },
                        modifier = Modifier.testTag("open_supabase_config_button")) { Text("Configure") } },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent))
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    }
    }
}
