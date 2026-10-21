package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.DisposableEffect
import androidx.core.content.ContextCompat
import com.example.data.local.SecurePreferencesManager
import com.example.ui.dock.EdgeDockService
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ConversationEntity
import com.example.data.local.FriendRequestEntity
import com.example.data.local.ProfileEntity
import com.example.data.remote.ProfileAppearance
import com.example.utils.AppUpdater
import com.example.ui.typography.ChatFontOption
import kotlinx.coroutines.launch
import com.example.ui.components.AppNavigationDestination
import com.example.ui.components.ExpressiveAvatar
import com.example.ui.components.FluidSidebarDock
import com.example.ui.model.DockCategory
import com.example.ui.components.ExpressiveEmptyState
import com.example.ui.components.ExpressiveFabMenu
import com.example.ui.components.ExpressiveContainedLoadingIndicator
import com.example.ui.components.FabMenuItem
import com.example.ui.theme.ExpressiveTopSurfaceShape
import com.example.ui.theme.ExpressiveCardShape
import com.example.ui.theme.ExpressiveButtonShape
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.GoogleSansFlexHeadline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.ExperimentalTextApi
import com.example.R
import com.example.ui.theme.ExpressiveShapesList
import com.example.ui.theme.ExpressiveStarShape
import com.example.ui.theme.getRandomExpressiveShape
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, onOpenConversation: (String) -> Unit, onNavigateToNewGroup: () -> Unit,
               onOpenAddFriends: () -> Unit, onOpenNotifications: () -> Unit,
               onOpenAppSettings: () -> Unit, onOpenProfileSettings: () -> Unit,
               onOpenProfile: (String) -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val requests by viewModel.pendingRequests.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var fabOpen by remember { mutableStateOf(false) }
    var settingsMenuExpanded by remember { mutableStateOf(false) }
    var pageSearchQuery by remember { mutableStateOf("") }
    var homeHeaderHeightPx by remember { mutableIntStateOf(0) }
    var yourCircleCenterYInRootPx by remember { mutableIntStateOf(0) }
    var chatSurfaceExpansionPx by remember { mutableStateOf(0f) }
    var chatListHasOverflow by remember { mutableStateOf(false) }
    val chatListState = rememberLazyListState()
    val statusBarHeightPx = WindowInsets.statusBars.getTop(LocalDensity.current).toFloat()
    val chatSurfaceExpansionLimitPx = (homeHeaderHeightPx - statusBarHeightPx).coerceAtLeast(0f)
    val homeScrollConnection = remember(
        state.selectedTab, chatSurfaceExpansionLimitPx, chatListState, chatListHasOverflow
    ) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (state.selectedTab != 0) return Offset.Zero

                val atTop = chatListState.firstVisibleItemIndex == 0 &&
                    chatListState.firstVisibleItemScrollOffset == 0
                val currentExpansion = chatSurfaceExpansionPx
                val delta = when {
                    available.y < 0f && chatListHasOverflow && chatListState.canScrollForward &&
                        currentExpansion < chatSurfaceExpansionLimitPx ->
                        -minOf(-available.y, chatSurfaceExpansionLimitPx - currentExpansion)
                    available.y > 0f && atTop && currentExpansion > 0f ->
                        minOf(available.y, currentExpansion)
                    else -> 0f
                }
                if (delta == 0f) return Offset.Zero
                chatSurfaceExpansionPx = (currentExpansion - delta)
                    .coerceIn(0f, chatSurfaceExpansionLimitPx)
                return Offset(0f, delta)
            }
        }
    }
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
    val navigationDockItems = remember(destinations) {
        destinations.map { destination ->
            DockCategory(
                id = "tab_${destination.label.lowercase()}",
                shortLabel = destination.label.take(1),
                fullTitle = destination.label,
                icon = destination.icon
            )
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }
    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { snackbar.showSnackbar(it); viewModel.clearStatus() }
    }
    LaunchedEffect(state.selectedTab) { pageSearchQuery = "" }
    LaunchedEffect(state.selectedTab) {
        if (state.selectedTab != 0) {
            chatSurfaceExpansionPx = 0f
            chatListHasOverflow = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(homeScrollConnection),
            containerColor = MaterialTheme.colorScheme.surface,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .onSizeChanged { homeHeaderHeightPx = it.height }
                        .graphicsLayer { translationY = if (state.selectedTab == 0) -chatSurfaceExpansionPx else 0f }
                ) {
                    Column(Modifier.widthIn(max = 1040.dp).fillMaxWidth().padding(horizontal = 20.dp)) {
                        Row(
                            Modifier.fillMaxWidth().height(88.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AsyncImage(
                                    model = R.drawable.cipher_chat_logo,
                                    contentDescription = "CypherChat logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                )
                                Text(
                                    when (state.selectedTab) { 1 -> "Your groups"; 2 -> "Calls"; else -> "Messages" },
                                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = GoogleSansFlexHeadline),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = onOpenNotifications, modifier = Modifier.testTag("notifications_action")) {
                                    BadgedBox(badge = { if (requests.isNotEmpty()) Badge { Text("${requests.size}") } }) {
                                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                                    }
                                }
                                Box {
                                    IconButton(onClick = { settingsMenuExpanded = true }, modifier = Modifier.testTag("settings_action")) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                                    }
                                    DropdownMenu(
                                        expanded = settingsMenuExpanded,
                                        onDismissRequest = { settingsMenuExpanded = false },
                                        shape = MaterialTheme.shapes.extraLarge,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("App") },
                                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                            onClick = { settingsMenuExpanded = false; onOpenAppSettings() },
                                            modifier = Modifier.testTag("settings_menu_app")
                                        )
                                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                        DropdownMenuItem(
                                            text = { Text("Profile") },
                                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                            onClick = { settingsMenuExpanded = false; onOpenProfileSettings() },
                                            modifier = Modifier.testTag("settings_menu_profile")
                                        )
                                    }
                                }
                            }
                            Box(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                                TextField(
                                    value = pageSearchQuery,
                                    onValueChange = { pageSearchQuery = it },
                                    placeholder = { Text(when (state.selectedTab) {
                                        1 -> "Search groups"
                                        2 -> "Search calls"
                                        else -> "Search people or messages"
                                    }) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    singleLine = true,
                                    shape = CircleShape,
                                    colors = TextFieldDefaults.colors(
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("chat_search_input")
                                )
                            }
                        }
                    }
                },
                bottomBar = {},
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
                    Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        AnimatedContent(
                            targetState = state.selectedTab,
                            transitionSpec = {
                                val direction = if (targetState > initialState) 1 else -1
                                (slideInHorizontally(
                                    animationSpec = tween(ExpressiveMotion.DurationMedium1, easing = ExpressiveMotion.EmphasizedDecelerateEasing),
                                    initialOffsetX = { width -> direction * width / 5 }
                                ) + fadeIn(animationSpec = tween(ExpressiveMotion.DurationShort4))) togetherWith
                                    (slideOutHorizontally(
                                        animationSpec = tween(ExpressiveMotion.DurationShort4, easing = ExpressiveMotion.EmphasizedAccelerateEasing),
                                        targetOffsetX = { width -> -direction * width / 6 }
                                    ) + fadeOut(animationSpec = tween(ExpressiveMotion.DurationShort3)))
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
                                    homeListState = chatListState,
                                    sheetExpansionPx = chatSurfaceExpansionPx,
                                    onChatListOverflowChange = { chatListHasOverflow = it },
                                    onYourCircleCenterMeasured = { yourCircleCenterYInRootPx = it },
                                    onOpenConversationWithFriend = { friend ->
                                        viewModel.openConversationWithFriend(friend, onOpenConversation)
                                    })
                            }
                        }
                    }
                }
            }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 1040.dp).fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                val dockHalfHeightPx = with(LocalDensity.current) { 73.dp.roundToPx() }
                val selectedDestination = navigationDockItems[state.selectedTab.coerceIn(navigationDockItems.indices)]
                FluidSidebarDock(
                    items = navigationDockItems,
                    selectedCategoryId = selectedDestination.id,
                    onCategorySelected = { item ->
                        val index = navigationDockItems.indexOfFirst { it.id == item.id }
                        if (index >= 0) viewModel.selectTab(index)
                    },
                    onCategoryActivated = { item ->
                        val index = navigationDockItems.indexOfFirst { it.id == item.id }
                        if (index >= 0) viewModel.selectTab(index)
                    },
                    modifier = Modifier
                        .offset {
                            IntOffset(x = 0, y = if (yourCircleCenterYInRootPx == 0) 0 else yourCircleCenterYInRootPx - dockHalfHeightPx)
                        }
                        .testTag("navigation_sidebar_dock")
                )
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
               homeListState: LazyListState? = null, sheetExpansionPx: Float = 0f,
               onChatListOverflowChange: (Boolean) -> Unit = {},
               onYourCircleCenterMeasured: (Int) -> Unit = {},
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
    val listState = homeListState ?: rememberLazyListState()
    LaunchedEffect(listState, visible) {
        val conversationKeys = visible.map { it.id }.toSet()
        snapshotFlow {
            val atTop = listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset == 0
            if (!atTop || listState.layoutInfo.totalItemsCount == 0) {
                null
            } else {
                val layoutInfo = listState.layoutInfo
                val visibleConversationItems = layoutInfo.visibleItemsInfo.filter { it.key in conversationKeys }
                val allConversationsFit = visibleConversationItems.size == conversationKeys.size &&
                    visibleConversationItems.all {
                        it.offset >= layoutInfo.viewportStartOffset &&
                            it.offset + it.size <= layoutInfo.viewportEndOffset
                    }
                conversationKeys.isNotEmpty() && !allConversationsFit
            }
        }.filterNotNull().distinctUntilChanged().collect(onChatListOverflowChange)
    }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val expansion = with(LocalDensity.current) { sheetExpansionPx.toDp() }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(maxHeight + expansion)
                    .align(Alignment.BottomCenter),
                shape = ExpressiveTopSurfaceShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .widthIn(max = 1040.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 104.dp)
                ) {
            if (showSearchField) {
                item {
                    OutlinedTextField(
                        value = query, onValueChange = updateQuery,
                        placeholder = { Text("Search people or messages") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true, shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(start = horizontalMargin, end = horizontalMargin, bottom = 16.dp).testTag("chat_search_input")
                    )
                }
            }
            if (scopeKey == "chats") {
                item(key = "your_circle_card") {
                    AnimatedVisibility(
                        visible = filter == 0 && query.isBlank(),
                        enter = expandVertically(
                            expandFrom = Alignment.Top,
                            animationSpec = tween(ExpressiveMotion.DurationMedium2, easing = ExpressiveMotion.EmphasizedDecelerateEasing)
                        ) + fadeIn(animationSpec = tween(ExpressiveMotion.DurationShort4)),
                        exit = fadeOut(animationSpec = tween(ExpressiveMotion.DurationShort2)) + shrinkVertically(
                            shrinkTowards = Alignment.Top,
                            animationSpec = tween(ExpressiveMotion.DurationMedium1, easing = ExpressiveMotion.EmphasizedAccelerateEasing)
                        )
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalMargin)
                                .onGloballyPositioned { coordinates ->
                                    onYourCircleCenterMeasured(
                                        coordinates.positionInRoot().y.roundToInt() + coordinates.size.height / 2
                                    )
                                }
                        ) {
                            Column(Modifier.padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 12.dp)) {
                                Text("Your Circle", style = MaterialTheme.typography.titleLarge)
                                val favoriteFriends = friends.filter { it.id in favoriteFriendIds }
                                if (favoriteFriends.isEmpty()) {
                                    Text(
                                        "Favorite friends to keep them close here.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                    onAddFriends?.let {
                                        FilledTonalButton(onClick = it, modifier = Modifier.padding(top = 12.dp)) {
                                            Icon(Icons.Default.PersonAdd, null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Add friends")
                                        }
                                    }
                                } else {
                                    Spacer(Modifier.height(10.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        onAddFriends?.let {
                                            item {
                                                    Surface(
                                                        onClick = it,
                                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                                        color = Color.Transparent,
                                                        modifier = Modifier.width(92.dp)
                                                    ) {
                                                        Column(
                                                            Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Surface(
                                                                shape = ExpressiveStarShape,
                                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                                modifier = Modifier.size(80.dp)
                                                            ) {
                                                                Icon(Icons.Default.PersonAdd, contentDescription = "Add friends", modifier = Modifier.padding(24.dp))
                                                            }
                                                            Text("Add", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                                        }
                                                    }
                                            }
                                        }
                                        itemsIndexed(favoriteFriends, key = { _, it -> it.id }) { index, friend ->
                                            val conversation = conversations.firstOrNull { it.otherUserId == friend.id && it.type == "direct" }
                                            val assignedShape = ExpressiveShapesList[(index + 1) % ExpressiveShapesList.size]
                                            Surface(
                                                onClick = {
                                                    conversation?.let { onOpenConversation(it.id) }
                                                        ?: onOpenConversationWithFriend(friend)
                                                },
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                                color = Color.Transparent,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.width(104.dp).testTag("circle_friend_${friend.id}")
                                            ) {
                                                Column(
                                                    Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    ExpressiveAvatar(friend.displayName, friend.avatarPath, 80.dp,
                                                        shape = assignedShape, border = null)
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
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp, start = horizontalMargin, end = horizontalMargin), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (filter == 0) "Recent messages" else "Your conversations", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = horizontalMargin),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    itemsIndexed(filters) { index, label ->
                        FilterChip(
                            selected = filter == index,
                            onClick = { filter = index },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(label)
                                    if (label == "Unread") {
                                        val totalUnread = conversations.count { it.unreadCount > 0 }
                                        if (totalUnread > 0) {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                            ) { Text("$totalUnread") }
                                        }
                                    }
                                }
                            },
                            shape = CircleShape
                        )
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
                    onAction = if (query.isBlank()) onStartGroup else null,
                    modifier = Modifier.padding(horizontal = horizontalMargin)
                )
            }
            } else {
                itemsIndexed(visible, key = { _, it -> it.id }) { index, conversation ->
                    val conversationAvatarPath = conversation.otherUserId?.let { otherUserId ->
                        friends.firstOrNull { it.id == otherUserId }?.avatarPath
                    }
                    val time = remember(conversation.lastMessageTimestamp) {
                        if (conversation.lastMessageTimestamp == 0L) "" else {
                            val date = Date(conversation.lastMessageTimestamp)
                            val hours = (System.currentTimeMillis() - date.time) / 3_600_000
                            SimpleDateFormat(if (hours < 24) "HH:mm" else "MMM d", Locale.getDefault()).format(date)
                        }
                    }
                    
                    val shape = when {
                        visible.size == 1 -> androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        index == visible.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                        else -> androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    }
                    
                    val isUnread = conversation.unreadCount > 0
                    
                    Surface(
                        onClick = { onOpenConversation(conversation.id) },
                        shape = shape,
                        color = if (isUnread) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().padding(start = horizontalMargin, end = horizontalMargin, bottom = 2.dp)
                    ) {
                        ListItem(
                            leadingContent = {
                                ExpressiveAvatar(conversation.title ?: "Chat", avatarUrl = conversationAvatarPath, size = 56.dp,
                                    modifier = Modifier.clickable(enabled = conversation.type == "direct" && conversation.otherUserId != null) {
                                        conversation.otherUserId?.let(onOpenProfile)
                                    })
                            },
                            headlineContent = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(conversation.title ?: "Encrypted conversation", style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
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
                                    Text(preview, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal, color = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
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
                                            tint = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = normalizedPreview,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    if (time.isNotBlank()) Text(time, style = MaterialTheme.typography.labelSmall,
                                        color = if (isUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal)
                                    if (isUnread) {
                                        Box(
                                            modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
                            leadingContent = { ExpressiveAvatar(request.senderName, request.avatarPath, size = 48.dp) },
                            headlineContent = { Text(request.senderName) }, supportingContent = { Text("@${request.senderTag}") },
                            trailingContent = {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(onClick = { viewModel.acceptFriendRequest(request) }, modifier = Modifier.testTag("accept_request_button")) { Text("Accept") }
                                    OutlinedButton(onClick = { viewModel.declineFriendRequest(request.id) }, modifier = Modifier.testTag("decline_request_button")) { Text("Decline") }
                                }
                            }, colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
                        }, colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
                ListItem(leadingContent = { ExpressiveAvatar(request.senderName, request.avatarPath, size = 48.dp) },
                    headlineContent = { Text("${request.senderName} sent a friend request") },
                    supportingContent = { Text("@${request.senderTag}") },
                    trailingContent = { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { viewModel.acceptFriendRequest(request) }) { Text("Accept") }
                        TextButton(onClick = { viewModel.declineFriendRequest(request.id) }) { Text("Decline") }
                    } }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
            }
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
    val dockPreferences = remember(context) { SecurePreferencesManager(context) }
    var edgeDockEnabled by remember { mutableStateOf(dockPreferences.isEdgeDockEnabled()) }
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val granted = Settings.canDrawOverlays(context)
        edgeDockEnabled = granted && dockPreferences.isEdgeDockEnabled()
        if (!granted) dockPreferences.setEdgeDockEnabled(false)
        if (granted && edgeDockEnabled) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, EdgeDockService::class.java).setAction(EdgeDockService.ACTION_APP_VISIBLE)
            )
        }
    }
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
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("App settings") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { pagePadding ->
    Box(Modifier.fillMaxSize().padding(pagePadding), contentAlignment = Alignment.TopCenter) {
    Column(Modifier.widthIn(max = 900.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Privacy and sync", style = MaterialTheme.typography.titleLarge)
        ListItem(leadingContent = { Icon(Icons.Default.Security, null) }, headlineContent = { Text("End-to-end encryption") },
            supportingContent = { Text("Messages are encrypted on this device") },
            modifier = Modifier.fillMaxWidth().clip(ExpressiveCardShape),
            trailingContent = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lock, contentDescription = "Encrypted", modifier = Modifier.size(16.dp))
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow))
        Text("Notifications", style = MaterialTheme.typography.titleLarge)
        ListItem(
            leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
            headlineContent = { Text(if (notificationsAllowed) "Message alerts enabled" else "Message alerts disabled") },
            supportingContent = { Text("Notifications use generic text so encrypted message content stays private.") },
            modifier = Modifier.fillMaxWidth().clip(ExpressiveCardShape).clickable {
                if (!notificationsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Notification settings") }
        )
        Text("Quick access", style = MaterialTheme.typography.titleLarge)
        ListItem(
            leadingContent = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            headlineContent = { Text("People dock") },
            supportingContent = { Text("Keep the swipe dock available over other apps. Android shows an ongoing notification while it is on.") },
            modifier = Modifier.fillMaxWidth().clip(ExpressiveCardShape),
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            trailingContent = {
                Switch(
                    checked = edgeDockEnabled,
                    thumbContent = if (edgeDockEnabled) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                    } else null,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            dockPreferences.setEdgeDockEnabled(false)
                            edgeDockEnabled = false
                            context.stopService(Intent(context, EdgeDockService::class.java))
                        } else if (Settings.canDrawOverlays(context)) {
                            dockPreferences.setEdgeDockEnabled(true)
                            edgeDockEnabled = true
                            ContextCompat.startForegroundService(
                                context,
                                Intent(context, EdgeDockService::class.java).setAction(EdgeDockService.ACTION_APP_VISIBLE)
                            )
                        } else {
                            dockPreferences.setEdgeDockEnabled(true)
                            overlayPermissionLauncher.launch(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            )
                        }
                    }
                )
            }
        )
        Text("Application updates", style = MaterialTheme.typography.titleLarge)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.Update, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("App updates", style = MaterialTheme.typography.titleMedium)
                            Text(updateMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
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
                        FilledTonalButton(
                            shape = ExpressiveButtonShape,
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
                                shape = ExpressiveButtonShape,
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
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)),
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                trailingContent = { FilledTonalButton(shape = ExpressiveButtonShape, onClick = { viewModel.showSupabaseDialog(true) }) { Text("Configure") } })
        }
        Text("Appearance and motion", style = MaterialTheme.typography.titleLarge)
        ListItem(
            leadingContent = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            headlineContent = { Text("Voice of User") },
            supportingContent = { Text("Subtly emphasizes expressive words after sending. Message text stays unchanged.") },
            modifier = Modifier.fillMaxWidth().clip(ExpressiveCardShape),
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            trailingContent = {
                Switch(
                    checked = state.voiceOfUserEnabled,
                    thumbContent = if (state.voiceOfUserEnabled) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                    } else null,
                    onCheckedChange = viewModel::setVoiceOfUserEnabled
                )
            }
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

@OptIn(ExperimentalTextApi::class)
@Composable
private fun ProfileAppearanceSettings(
    appearance: ProfileAppearance,
    previewName: String,
    previewTag: String,
    previewAvatarPath: String?,
    saveMessage: String?,
    saveError: String?,
    onAppearanceChange: (ProfileAppearance) -> Unit,
    onSave: () -> Unit
) {
    val palettes = listOf(
        "Lavender" to "#C987FA", "Violet" to "#8139A0", "Rose" to "#F2A7C7",
        "Sky" to "#9BDCF2", "Mint" to "#9BF0D0", "Space" to "#1B1D22"
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Live profile preview", style = MaterialTheme.typography.titleMedium)
            Text("Choose how your profile looks to other people.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = profilePreviewColor(appearance.background)
            ) {
                val previewInk = profilePreviewInk(appearance.background)
                val previewFont = ChatFontOption.fromId(appearance.fontId).getFontFamily(
                    appearance.weight, appearance.width, grade = 0, opticalSize = 14f
                )
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExpressiveAvatar(previewName, previewAvatarPath, size = 60.dp)
                    ProfileWeightText(
                        previewName.ifBlank { "Your name" },
                        weight = profilePreviewWeight(appearance),
                        modifier = Modifier.graphicsLayer { scaleX = appearance.width / 100f },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = previewFont,
                            letterSpacing = (-0.6).sp
                        ),
                        color = previewInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ProfileWeightText(
                        "@${previewTag.ifBlank { "yourtag" }}",
                        weight = profilePreviewWeight(appearance),
                        width = appearance.width,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = previewFont),
                        color = previewInk
                    )
                    ProfileWeightText(
                        appearance.bio.ifBlank { "Your bio will appear here" },
                        weight = profilePreviewWeight(appearance),
                        width = appearance.width,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = previewFont),
                        color = previewInk,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text("Profile background", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                palettes.forEach { (name, color) ->
                    FilterChip(
                        selected = appearance.background.equals(color, ignoreCase = true),
                        onClick = { onAppearanceChange(appearance.copy(background = color)) },
                        label = { Text(name) }
                    )
                }
            }
            Text("Typography", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChatFontOption.entries.forEach { option ->
                    FilterChip(
                        selected = appearance.fontId == option.id,
                        onClick = { onAppearanceChange(appearance.copy(fontId = option.id)) },
                        label = { Text(option.displayName) }
                    )
                }
            }
            OutlinedTextField(
                value = appearance.bio,
                onValueChange = { onAppearanceChange(appearance.copy(bio = it.take(300))) },
                label = { Text("Bio") },
                placeholder = { Text("A little about you") },
                supportingText = { Text("${appearance.bio.length}/300") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
            ProfileAxisSlider("Width", appearance.width, 25f..151f, "${appearance.width.toInt()}") {
                onAppearanceChange(appearance.copy(width = it))
            }
            ProfileAxisSlider("Weight", appearance.weight.toFloat(), 100f..1000f, appearance.weight.toString()) {
                onAppearanceChange(appearance.copy(weight = it.toInt()))
            }
            Text(
                "Changes are saved on this device as you adjust them. Save to publish them to your profile. Your Supabase schema must include the profile appearance and bio columns.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            saveMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            saveError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Save and share profile") }
    }
}

@Composable
private fun ProfileAxisSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}

private fun profilePreviewWeight(appearance: ProfileAppearance): Int {
    return appearance.weight.coerceIn(100, 1000)
}

private fun profilePreviewColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFFC987FA))

private fun profilePreviewInk(hex: String): Color {
    val luminance = profilePreviewColor(hex).luminance()
    val blackContrast = (luminance + 0.05f) / 0.05f
    val whiteContrast = 1.05f / (luminance + 0.05f)
    return if (blackContrast >= whiteContrast) Color(0xFF190626) else Color(0xFFFFF8FF)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3ProfileTab(viewModel: HomeViewModel, onBack: (() -> Unit)? = null) {
    val state by viewModel.uiState.collectAsState()
    val auth = viewModel.authRepository
    val cloud = viewModel.supabaseProvider
    val clipboard = LocalClipboardManager.current
    var name by remember { mutableStateOf(auth.getDisplayName()) }
    var tag by remember { mutableStateOf(auth.getTag()) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.updateProfile(name, tag, uri)
    }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                navigationIcon = {
                    onBack?.let { back ->
                        IconButton(onClick = back, modifier = Modifier.testTag("back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                title = { Text("Profile and customisation") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { viewModel.updateProfile(name, tag, null) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("save_profile_button")
                ) {
                    Text("Save profile")
                }
            }
        }
    ) { pagePadding ->
        Box(Modifier.fillMaxSize().padding(pagePadding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 900.dp).fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Profile Details
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Profile details", style = MaterialTheme.typography.titleLarge)
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Surface(
                                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    modifier = Modifier.size(96.dp).testTag("profile_avatar_picker"),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    val avatar = auth.getAvatarPath()
                                    if (!avatar.isNullOrBlank()) AsyncImage(
                                        model = avatar,
                                        contentDescription = "Change profile photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                    else ExpressiveAvatar(name, size = 96.dp)
                                }
                                SmallFloatingActionButton(
                                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    shape = CircleShape,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp).offset(x = 4.dp, y = 4.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit photo", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Display name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                        )
                        OutlinedTextField(
                            value = tag,
                            onValueChange = { tag = it.trim().removePrefix("@").lowercase() },
                            label = { Text("Unique tag") },
                            prefix = { Text("@") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("profile_tag_input")
                        )
                        OutlinedTextField(
                            value = state.profileAppearance.bio,
                            onValueChange = { viewModel.updateProfileAppearance(state.profileAppearance.copy(bio = it.take(300))) },
                            label = { Text("Bio / Status") },
                            singleLine = false,
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth().testTag("profile_bio_input")
                        )
                        state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
                        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                }

                // Customisation
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Customisation", style = MaterialTheme.typography.titleLarge)
                        ProfileAppearanceSettings(
                            appearance = state.profileAppearance,
                            previewName = name,
                            previewTag = tag,
                            previewAvatarPath = auth.getAvatarPath(),
                            saveMessage = state.statusMessage,
                            saveError = state.errorMessage,
                            onAppearanceChange = viewModel::updateProfileAppearance,
                            onSave = viewModel::publishProfileAppearance
                        )
                    }
                }

                // Security & devices
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(vertical = 20.dp)) {
                        Text("Security & devices", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp))
                        ListItem(
                            leadingContent = { Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary) },
                            headlineContent = { Text("End-to-end encryption") },
                            supportingContent = { Text("libsignal Double Ratchet + X3DH") },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        ListItem(
                            leadingContent = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                            headlineContent = { Text("Private key storage") },
                            supportingContent = { Text("Android hardware-backed KeyStore") },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        ListItem(
                            leadingContent = { Icon(Icons.Default.VpnKey, null, tint = MaterialTheme.colorScheme.primary) },
                            headlineContent = { Text("Device identifier") },
                            supportingContent = { Text("${auth.myDeviceId.take(16)}…", fontFamily = FontFamily.Monospace) },
                            trailingContent = {
                                IconButton(onClick = { clipboard.setText(AnnotatedString(auth.myDeviceId)) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy device identifier")
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        ListItem(
                            leadingContent = {
                                Icon(
                                    if (cloud.isConfigured) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    null,
                                    tint = if (cloud.isConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            },
                            headlineContent = { Text("Cloud relay") },
                            supportingContent = { Text(if (cloud.isConfigured) "Connected" else "Not configured") },
                            trailingContent = {
                                FilledTonalButton(
                                    onClick = { viewModel.showSupabaseDialog(true) },
                                    modifier = Modifier.testTag("open_supabase_config_button")
                                ) { Text("Configure") }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
                Spacer(Modifier.height(48.dp)) // padding for bottom bar
            }
        }
    }
}
