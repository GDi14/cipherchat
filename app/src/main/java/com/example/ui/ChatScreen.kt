package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.ExpressiveBottomSurfaceShape
import com.example.ui.theme.ExpressiveTopSurfaceShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.local.MessageEntity
import com.example.repository.FriendRepository
import com.example.crypto.MessagePayloadCodec
import com.example.ui.components.ExpressiveAvatar
import com.example.ui.components.ExpressiveReceiptIcon
import com.example.ui.components.ExpressiveContainedLoadingIndicator
import com.example.ui.components.ExpressiveWaveform
import com.example.ui.viewer.FullscreenMediaViewerDialog
import com.example.ui.theme.PillShape
import com.example.ui.theme.ExpressiveIncomingBubbleShape
import com.example.ui.theme.ExpressiveOutgoingBubbleShape
import com.example.ui.theme.LocalExpressiveMotionEnabled
import com.vanniktech.blurhash.BlurHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private data class SendFlight(
    val messageId: String,
    val text: String,
    val startedAt: Long,
    val start: Rect,
    val end: Rect? = null,
    val endMeasured: Boolean = false,
    val scrollDistance: Float = 0f,
    val startMessageCount: Int = 0,
    val textWidth: Float = start.width,
    val bubbleHeight: Float = start.height
)

private fun Rect.relativeTo(origin: Offset) = Rect(
    left = left - origin.x,
    top = top - origin.y,
    right = right - origin.x,
    bottom = bottom - origin.y
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    friendRepository: FriendRepository,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit = {},
    onStartCall: (String) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(viewModel, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.markRead()
                Lifecycle.Event.ON_STOP -> viewModel.onConversationHidden()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onConversationHidden()
        }
    }

    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val uiState by viewModel.uiState.collectAsState()
    val directUserId = uiState.conversation?.takeIf { it.type == "direct" }?.otherUserId
    var headerAvatarPath by remember(directUserId) { mutableStateOf<String?>(null) }
    LaunchedEffect(directUserId) {
        val targetId = directUserId ?: return@LaunchedEffect
        val profile = friendRepository.getProfileWithCachePolicy(targetId, forceRefresh = false)
        headerAvatarPath = profile?.avatarPath
    }
    val messages = viewModel.messages.collectAsLazyPagingItems()
    val pendingMessages by viewModel.pendingMessages.collectAsState()
    
    val loadedMessageIds = remember(messages.itemSnapshotList.items) {
        messages.itemSnapshotList.items.map { it.id }.toSet()
    }
    
    // Sort descending because reverseLayout=true means first item in LazyColumn is drawn at the bottom
    val visiblePending = remember(pendingMessages, loadedMessageIds) {
        pendingMessages.filterNot { it.id in loadedMessageIds }.sortedByDescending { it.timestamp }
    }
    
    val totalMessageCount = messages.itemCount + visiblePending.size
    
    val loadedMessages = messages.itemSnapshotList.items
    val groupSenderIds = remember(loadedMessages, uiState.conversation?.type) {
        if (uiState.conversation?.type == "group") loadedMessages.map { it.senderId }.distinct() else emptyList()
    }
    var groupSenderAvatarPaths by remember(viewModel.conversationId) { mutableStateOf<Map<String, String?>>(emptyMap()) }
    LaunchedEffect(groupSenderIds) {
        val missingIds = groupSenderIds.filterNot { it in groupSenderAvatarPaths }
        if (missingIds.isNotEmpty()) {
            val localMap = missingIds.associateWith { id ->
                friendRepository.getProfileWithCachePolicy(id, forceRefresh = false)?.avatarPath
            }
            groupSenderAvatarPaths = groupSenderAvatarPaths + localMap
        }
    }
    val typingUsersMap by viewModel.typingUsers.collectAsState()
    val voiceOfUserEnabled by viewModel.voiceOfUserEnabled.collectAsState()
    val emphasisAnimationMessageIds by viewModel.emphasisAnimationMessageIds.collectAsState()
    val isSomeoneTyping = typingUsersMap[viewModel.conversationId]?.isNotEmpty() == true
    val initialScrollIndex = remember(viewModel.conversationId, uiState.initialUnreadCount) {
        if (uiState.initialUnreadCount > 0) uiState.initialUnreadCount else 0
    }
    val listState = remember(viewModel.conversationId) {
        LazyListState(
            firstVisibleItemIndex = initialScrollIndex,
            firstVisibleItemScrollOffset = 0
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val screenScope = rememberCoroutineScope()

    var showSafetySheet by remember { mutableStateOf(false) }
    var currentPeerFingerprint by remember { mutableStateOf<String?>(null) }
    val safetyPeerId = uiState.conversation?.takeIf { it.type == "direct" }?.otherUserId
    LaunchedEffect(showSafetySheet, safetyPeerId) {
        currentPeerFingerprint = if (showSafetySheet && safetyPeerId != null) {
            runCatching { viewModel.peerFingerprint(safetyPeerId) }.getOrNull()
        } else null
    }
    var composerBounds by remember { mutableStateOf<Rect?>(null) }
    var sendFlight by remember { mutableStateOf<SendFlight?>(null) }
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    var composerHeightPx by remember { mutableStateOf(with(density) { 88.dp.roundToPx() }) }
    var initialMessagePositioned by remember(viewModel.conversationId) { mutableStateOf(false) }
    var previousMessageCount by remember(viewModel.conversationId) { mutableStateOf(0) }
    val sendFlightProgress = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    val messageTextStyle = MaterialTheme.typography.bodyLarge
    val hapticFeedback = LocalHapticFeedback.current
    val motionEnabled = LocalExpressiveMotionEnabled.current

    val isImeVisible = WindowInsets.isImeVisible

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(totalMessageCount, uiState.sendingMediaUri) {
        if (sendFlight != null) return@LaunchedEffect

        if (!initialMessagePositioned) {
            if (totalMessageCount > 0) {
                if (uiState.initialUnreadCount <= 0) {
                    listState.scrollToItem(0)
                }
                initialMessagePositioned = true
            }
            return@LaunchedEffect
        }

        if (totalMessageCount > 0) {
            val firstVisible = listState.firstVisibleItemIndex
            // When no unread messages are available (initialUnreadCount <= 0), or if near the bottom / sending media: stick to the bottom
            if (uiState.initialUnreadCount <= 0 || firstVisible <= 3 || uiState.sendingMediaUri != null) {
                listState.animateScrollToItem(0)
            }
        }
    }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible && totalMessageCount > 0) {
            withFrameNanos { }
            val targetIndex = 0
            listState.animateScrollToItem(targetIndex)
        }
    }

    // Media Pickers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            viewModel.sendMediaUri(uri, mimeType)
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoiceRecording()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                shape = RectangleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)
            ) {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    val title = uiState.conversation?.title ?: "Encrypted Chat"
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val directUserId = uiState.conversation?.takeIf { it.type == "direct" }?.otherUserId
                        ExpressiveAvatar(name = title, avatarUrl = headerAvatarPath, size = 44.dp,
                            modifier = Modifier.clickable(enabled = directUserId != null) {
                                directUserId?.let(onOpenProfile)
                            })
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable(enabled = directUserId != null) {
                                        directUserId?.let(onOpenProfile)
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "Encrypted · safety numbers",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                actions = {
                    val otherUserId = uiState.conversation?.takeIf { it.type == "direct" }?.otherUserId
                    if (otherUserId != null) {
                        IconButton(
                            onClick = { onStartCall(otherUserId) },
                            modifier = Modifier.testTag("chat_audio_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Audio Call",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { onStartCall(otherUserId) },
                            modifier = Modifier.testTag("chat_video_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Video Call",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    AssistChip(
                        onClick = { showSafetySheet = true },
                        label = { Text("Safety") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .onGloballyPositioned {
                overlayOrigin = it.positionInRoot()
            }
        ) {
            val unreadCount = uiState.initialUnreadCount
            val hasUnreadDivider = unreadCount > 0
            
            LazyColumn(
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .widthIn(max = 960.dp)
                    .fillMaxSize()
                    .padding(bottom = with(density) { composerHeightPx.toDp() })
                    .align(Alignment.Center)
                    .graphicsLayer {
                        alpha = if (initialMessagePositioned || totalMessageCount == 0) 1f else 0f
                    }
            ) {
                items(visiblePending, key = { it.id }) { message ->
                    val isInSendFlight = sendFlight?.let { flight ->
                        message.id == flight.messageId
                    } == true
                    M3MessageItem(
                        modifier = Modifier,
                        message = message,
                        animateEmphasis = message.id in emphasisAnimationMessageIds,
                        voiceOfUserEnabled = voiceOfUserEnabled,
                        onEmphasisAnimationComplete = viewModel::completeEmphasisAnimation,
                        isOutgoing = message.senderId == viewModel.myUserId,
                        showSenderBadge = uiState.conversation?.type == "group",
                        senderLabel = message.senderName ?: if (message.senderId == viewModel.myUserId) {
                            viewModel.myDisplayName
                        } else {
                            "Friend"
                        },
                        senderAvatarPath = groupSenderAvatarPaths[message.senderId],
                        onReply = { viewModel.setReplyingTo(message) },
                        onDelete = { viewModel.hideMessage(message) },
                        onPlayAudio = { viewModel.playAudio(message) },
                        isPlayingAudio = uiState.playingAudioMessageId == message.id,
                        audioProgress = if (uiState.playingAudioMessageId == message.id) uiState.audioProgress else 0f,
                        onViewMedia = { viewModel.openMediaViewer(message) },
                        hideOutgoingBubble = isInSendFlight,
                        onBounds = { bounds ->
                            val flight = sendFlight
                            if (flight != null && message.id == flight.messageId
                            ) {
                                val target = bounds.relativeTo(overlayOrigin)
                                val desiredBottom = flight.start.top - with(density) { 8.dp.toPx() }
                                sendFlight = flight.copy(
                                    end = target,
                                    endMeasured = true,
                                    scrollDistance = target.bottom - desiredBottom
                                )
                            }
                        }
                    )
                }
                
                items(
                    count = messages.itemCount,
                    key = messages.itemKey { it.id },
                    contentType = messages.itemContentType { it.contentType.name }
                ) { index ->
                    val messageUi = messages[index]
                    val message = messageUi?.originalEntity
                    
                    if (hasUnreadDivider && index == unreadCount) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = PillShape
                            ) {
                                Text(
                                    text = "$unreadCount Unread Messages",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (message != null) {
                    val isInSendFlight = sendFlight?.let { flight ->
                        message.id == flight.messageId
                    } == true
                    M3MessageItem(
                        // Keep already-visible rows stationary when the database emits
                        // a received message; animating every row caused a refresh-like
                        // shift and amplified work in long conversations.
                        modifier = Modifier,
                        message = message,
                        animateEmphasis = message.id in emphasisAnimationMessageIds,
                        voiceOfUserEnabled = voiceOfUserEnabled,
                        onEmphasisAnimationComplete = viewModel::completeEmphasisAnimation,
                        isOutgoing = message.senderId == viewModel.myUserId,
                        showSenderBadge = uiState.conversation?.type == "group",
                        senderLabel = message.senderName ?: if (message.senderId == viewModel.myUserId) {
                            viewModel.myDisplayName
                        } else {
                            "Friend"
                        },
                        senderAvatarPath = groupSenderAvatarPaths[message.senderId],
                        onReply = { viewModel.setReplyingTo(message) },
                        onDelete = { viewModel.hideMessage(message) },
                        onPlayAudio = { viewModel.playAudio(message) },
                        isPlayingAudio = uiState.playingAudioMessageId == message.id,
                        audioProgress = if (uiState.playingAudioMessageId == message.id) uiState.audioProgress else 0f,
                        onViewMedia = { viewModel.openMediaViewer(message) },
                        hideOutgoingBubble = isInSendFlight,
                        onBounds = { bounds ->
                            val flight = sendFlight
                            if (flight != null && message.id == flight.messageId
                            ) {
                                val target = bounds.relativeTo(overlayOrigin)
                                val desiredBottom = flight.start.top - with(density) { 8.dp.toPx() }
                                sendFlight = flight.copy(
                                    end = target,
                                    endMeasured = true,
                                    scrollDistance = target.bottom - desiredBottom
                                )
                            }
                        }
                    )
                    }
                }
                item(key = "typing_indicator") {
                    AnimatedVisibility(
                        visible = isSomeoneTyping,
                        enter = fadeIn(animationSpec = tween(ExpressiveMotion.DurationShort4, easing = ExpressiveMotion.EmphasizedEasing)) +
                            expandVertically(
                                expandFrom = Alignment.Top,
                                animationSpec = tween(ExpressiveMotion.DurationShort4, easing = ExpressiveMotion.EmphasizedDecelerateEasing)
                            ),
                        exit = fadeOut(animationSpec = tween(ExpressiveMotion.DurationShort3, easing = ExpressiveMotion.EmphasizedAccelerateEasing)) +
                            shrinkVertically(
                                shrinkTowards = Alignment.Top,
                                animationSpec = tween(ExpressiveMotion.DurationShort3, easing = ExpressiveMotion.EmphasizedAccelerateEasing)
                            )
                    ) {
                        TypingIndicatorBubble(modifier = Modifier.fillMaxWidth())
                    }
                }
                uiState.sendingMediaUri?.let { uri ->
                    item(key = "sending_media_preview") {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            val isPhoto = uiState.sendingMediaType?.startsWith("image") == true
                            Box(Modifier.size(width = 220.dp, height = 150.dp).clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                                if (isPhoto) AsyncImage(model = uri, contentDescription = "Sending photo", contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize())
                                else Text("Sending video…", Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (isPhoto) ExpressiveContainedLoadingIndicator(
                                    Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)).padding(8.dp), size = 20.dp)
                            }
                        }
                    }
                }
            }

            // M3 Tonal Surface Bottom Composer
            Surface(
                shape = RectangleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 960.dp)
                    .fillMaxWidth()
                    .onSizeChanged { composerHeightPx = it.height }
                    .then(
                        if (!isImeVisible) Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        else Modifier
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Quoted Message Banner
                    AnimatedVisibility(
                        visible = uiState.replyingTo != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        uiState.replyingTo?.let { replyMsg ->
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Replying to ${replyMsg.senderName ?: "Friend"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (replyMsg.plaintextBody.isNotBlank()) replyMsg.plaintextBody else "[Media]",
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { viewModel.setReplyingTo(null) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                                    }
                                }
                            }
                        }
                    }

                    // Input Controls Row
                    if (uiState.isRecordingVoice) {
                        // Voice Recording State with M3 Components
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            val minutes = uiState.voiceDurationSeconds / 60
                            val seconds = uiState.voiceDurationSeconds % 60
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            ExpressiveWaveform(0f, true, Modifier.weight(1f).height(34.dp))
                            IconButton(onClick = { viewModel.cancelVoiceRecording() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                            FilledIconButton(
                                onClick = { viewModel.stopAndSendVoiceRecording() },
                                modifier = Modifier.testTag("send_voice_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Voice")
                            }
                        }
                    } else {
                        // Standard Composer Row with M3 OutlinedTextField & FilledIconButton
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .testTag("attach_media_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Attach media")
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(PillShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, PillShape)
                                        .onGloballyPositioned {
                                            composerBounds = it.boundsInRoot().relativeTo(overlayOrigin)
                                        }
                                ) {
                                    OutlinedTextField(
                                        value = uiState.inputText,
                                        onValueChange = { viewModel.onInputTextChanged(it) },
                                        placeholder = { Text("Message") },
                                        maxLines = 4,
                                        shape = PillShape,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            disabledBorderColor = Color.Transparent,
                                            errorBorderColor = Color.Transparent,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            errorContainerColor = Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(end = 48.dp)
                                            .testTag("message_input")
                                    )
                                    IconButton(
                                        onClick = {
                                            if (uiState.inputText.isNotBlank()) {
                                                val startMessageCount = totalMessageCount
                                                viewModel.sendTextMessage { provisionalMessage ->
                                                    composerBounds?.let { inputBounds ->
                                                    val text = provisionalMessage.plaintextBody
                                                    val minBubbleWidth = with(density) { 56.dp.toPx() }
                                                    val maxBubbleWidth = with(density) { 310.dp.toPx() }
                                                    val horizontalPadding = with(density) { 24.dp.toPx() }
                                                    val maxTextWidth = (minOf(inputBounds.width, maxBubbleWidth) - horizontalPadding)
                                                        .roundToInt().coerceAtLeast(1)
                                                    val textLayout = textMeasurer.measure(
                                                        text = text,
                                                        style = messageTextStyle,
                                                        constraints = Constraints(maxWidth = maxTextWidth)
                                                    )
                                                    val estimatedWidth = (textLayout.size.width + horizontalPadding)
                                                        .coerceIn(minBubbleWidth, minOf(inputBounds.width, maxBubbleWidth))
                                                    // Includes the text, timestamp row, gap, and bubble padding.
                                                    val estimatedHeight = textLayout.size.height + with(density) { 36.dp.toPx() }
                                                    val endBottom = inputBounds.top - with(density) { 8.dp.toPx() }
                                                    val estimatedEnd = Rect(
                                                        left = inputBounds.right - estimatedWidth,
                                                        top = endBottom - estimatedHeight,
                                                        right = inputBounds.right,
                                                        bottom = endBottom
                                                    )

                                                    val layoutInfo = listState.layoutInfo
                                                    val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()
                                                    val itemSpacingPx = with(density) { 8.dp.toPx() }
                                                    val distanceToExistingBottom = if (firstVisible == null) {
                                                        0f
                                                    } else {
                                                        val averageExtent = layoutInfo.visibleItemsInfo
                                                            .map { it.size + itemSpacingPx }
                                                            .average()
                                                            .toFloat()
                                                        val itemsBefore = firstVisible.index
                                                        val visibleGap = firstVisible.offset.toFloat()
                                                        (itemsBefore * averageExtent + visibleGap).coerceAtLeast(0f)
                                                    }

                                                    sendFlight = SendFlight(
                                                        messageId = provisionalMessage.id,
                                                        text = text,
                                                        startedAt = provisionalMessage.timestamp,
                                                        start = inputBounds,
                                                        end = estimatedEnd,
                                                        scrollDistance = distanceToExistingBottom + estimatedHeight + itemSpacingPx,
                                                        startMessageCount = startMessageCount,
                                                        textWidth = textLayout.size.width.toFloat(),
                                                        bubbleHeight = estimatedHeight
                                                    )
                                                    }
                                                }
                                            } else {
                                                val hasPerm = ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.RECORD_AUDIO
                                                ) == PackageManager.PERMISSION_GRANTED
                                                if (hasPerm) {
                                                    viewModel.startVoiceRecording()
                                                } else {
                                                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                }
                                            }
                                        },
                                        enabled = !uiState.isSending,
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = if (uiState.inputText.isNotBlank()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant,
                                            contentColor = if (uiState.inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 4.dp)
                                            .testTag(
                                                if (uiState.inputText.isNotBlank()) "send_message_button" else "voice_record_button"
                                            )
                                    ) {
                                        if (uiState.isSending) {
                                            ExpressiveContainedLoadingIndicator(Modifier.size(26.dp),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer, size = 17.dp)
                                        } else if (uiState.inputText.isNotBlank()) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Send")
                                        } else {
                                            Icon(Icons.Default.Mic, contentDescription = "Record voice message")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        val flight = sendFlight
        LaunchedEffect(flight?.startedAt) {
            val startedAt = flight?.startedAt ?: return@LaunchedEffect
            sendFlightProgress.snapTo(0f)
            // The provisional row's estimated scroll distance was captured at
            // tap time, so the bubble and list can start moving on this frame.
            val scrollDistance = flight.scrollDistance.coerceAtLeast(0f)
            // A single eased progress drives both the bubble and the list so they
            // decelerate together into the final resting position.
            val flightMotion = tween<Float>(durationMillis = ExpressiveMotion.DurationLong1, easing = ExpressiveMotion.EmphasizedEasing)
            coroutineScope {
                val scrollMotion = launch {
                    if (scrollDistance > 1f) {
                        var appliedScroll = 0f
                        snapshotFlow { sendFlightProgress.value.coerceIn(0f, 1f) }
                            .onEach { progress ->
                                // Give the list a quick initial lift, then let it
                                // coast into the bottom anchor as the bubble lands.
                                val remainingProgress = 1f - progress
                                val scrollProgress = 1f -
                                    (remainingProgress * remainingProgress * remainingProgress)
                                val requestedScroll = scrollProgress * scrollDistance
                                val remainingScroll = requestedScroll - appliedScroll
                                if (kotlin.math.abs(remainingScroll) > 0.1f) {
                                    // In reverseLayout=true, scrolling towards the newly inserted
                                    // item 0 requires a negative scroll delta.
                                    appliedScroll += abs(listState.scrollBy(-remainingScroll))
                                }
                            }
                            .first { it >= 1f }
                    }
                }
                val bubbleMotion = launch {
                    if (motionEnabled) {
                        sendFlightProgress.animateTo(1f, flightMotion)
                    } else {
                        sendFlightProgress.snapTo(1f)
                    }
                }
                bubbleMotion.join()
                scrollMotion.join()
            }
            // Wait for the real row to finish layout only after the visible
            // send motion, before swapping the overlay proxy for that row.
            val messageInserted = withTimeoutOrNull(800L) {
                snapshotFlow { totalMessageCount }
                    .first { it > flight.startMessageCount }
                true
            } == true
            if (messageInserted) withFrameNanos { }
            // Allow the list's final layout to settle before swapping the proxy
            // for the real message bubble.
            withFrameNanos { }
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            // Keep the idle progress at the source. Otherwise the next flight's
            // first composed frame can draw its proxy at the previous endpoint.
            sendFlightProgress.snapTo(0f)
            sendFlight = null
        }
        if (flight != null) {
            val rawProgress = sendFlightProgress.value
            val progress = rawProgress.coerceIn(0f, 1f)
            val start = flight.start
            val end = flight.end ?: start
            val width = start.width + (end.width - start.width) * progress
            val height = start.height + (end.height - start.height) * progress
            val textWidth = flight.textWidth.coerceAtLeast(1f)
            val targetTextWidth = (end.width - with(density) { 24.dp.toPx() }).coerceAtLeast(1f)
            // The source is the typed text's measured width, not the full-width
            // composer. Limit correction to a subtle squeeze if bubble measurement
            // differs from the measured message layout.
            val targetTextScale = (targetTextWidth / textWidth).coerceIn(0.92f, 1f)
            val textScale = 1f + (targetTextScale - 1f) * progress
            // Stop translation at the destination. Use the spring's overshoot as
            // an impact impulse instead of letting the bubble drift past and return.
            val x = start.left + (end.left - start.left) * progress
            val y = start.top + (end.top - start.top) * progress
            val midFlightStretch = 1f + 0.054f * kotlin.math.sin(progress * Math.PI).toFloat()
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = if (progress < 0.16f) PillShape else ExpressiveOutgoingBubbleShape,
                modifier = Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .width(with(density) { width.toDp() })
                    .height(with(density) { height.toDp() })
                    .graphicsLayer {
                        scaleX = 1f / kotlin.math.sqrt(midFlightStretch)
                        scaleY = midFlightStretch
                        transformOrigin = TransformOrigin(1f, 1f)
                    }
            ) {
                Box(Modifier.fillMaxSize()) {
                    Text(
                        flight.text,
                        maxLines = Int.MAX_VALUE,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 12.dp, top = 8.dp)
                            .width(with(density) { textWidth.toDp() })
                            .graphicsLayer {
                                scaleX = textScale
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                    )
                    if (progress < 0.18f) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                        )
                    }
                }
            }
        }
    }
    }

    // Safety Numbers Verification ModalBottomSheet (Official M3)
    if (showSafetySheet) {
        val directPeerId = uiState.conversation?.takeIf { it.type == "direct" }?.otherUserId
        M3SafetyNumbersSheet(
            conversationTitle = uiState.conversation?.title ?: "Chat",
            fingerprint = currentPeerFingerprint,
            onResetSession = directPeerId?.let { peerId ->
                {
                    viewModel.resetSessionWithPeer(peerId) { success, message ->
                        screenScope.launch {
                            snackbarHostState.showSnackbar(
                                if (success) message else "Could not reset secure session: $message"
                            )
                        }
                    }
                }
            },
            onDismiss = { showSafetySheet = false }
        )
    }

    uiState.selectedViewerMedia?.let { mediaMsg ->
        mediaMsg.localDecryptedPath?.let { path ->
            FullscreenMediaViewerDialog(
                filePath = path,
                isVideo = mediaMsg.mediaType?.startsWith("video") == true,
                onDismiss = { viewModel.closeMediaViewer() },
                onReply = { viewModel.setReplyingTo(mediaMsg) }
            )
        }
    }
}

// ==============================================================================
// M3 message item with tonal surfaces, progress indicators, and buttons.
// ==============================================================================

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun M3MessageItem(
    modifier: Modifier = Modifier,
    message: MessageEntity,
    animateEmphasis: Boolean = false,
    voiceOfUserEnabled: Boolean = true,
    onEmphasisAnimationComplete: (String) -> Unit = {},
    isOutgoing: Boolean,
    showSenderBadge: Boolean = false,
    senderLabel: String? = message.senderName,
    senderAvatarPath: String? = null,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onPlayAudio: () -> Unit,
    isPlayingAudio: Boolean,
    audioProgress: Float,
    onViewMedia: () -> Unit,
    hideOutgoingBubble: Boolean = false,
    onBounds: ((Rect) -> Unit)? = null
) {
    var showDeleteConfirmation by remember(message.id) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val maxDrag = 150f

    val messageInteraction = Modifier
        .pointerInput(message.id) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    scope.launch {
                        if (offsetX.value > 100f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onReply()
                        }
                        offsetX.animateTo(0f)
                    }
                },
                onDragCancel = {
                    scope.launch { offsetX.animateTo(0f) }
                },
                onHorizontalDrag = { change, dragAmount ->
                    if (dragAmount > 0 || offsetX.value > 0) {
                        change.consume()
                        scope.launch {
                            val newOffset = (offsetX.value + dragAmount).coerceIn(0f, maxDrag)
                            offsetX.snapTo(newOffset)
                        }
                    }
                }
            )
        }
        .graphicsLayer {
            translationX = offsetX.value
        }
        .combinedClickable(
            onClick = onReply,
            onLongClick = { showDeleteConfirmation = true }
        )
    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (hideOutgoingBubble) 0f else 1f },
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        if (showSenderBadge && !senderLabel.isNullOrBlank()) {
            Row(
                modifier = Modifier.padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveAvatar(senderLabel, senderAvatarPath, size = 24.dp)
                Text(senderLabel, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (isOutgoing) {
            // Message bubbles use M3 container roles without added elevation.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = ExpressiveOutgoingBubbleShape,
                    modifier = Modifier
                        .widthIn(max = 310.dp)
                        .then(if (onBounds != null) Modifier.onGloballyPositioned { onBounds(it.boundsInRoot()) } else Modifier)
                        .then(messageInteraction)
                ) {
                    M3MessageContent(
                        message = message,
                        animateEmphasis = animateEmphasis,
                        voiceOfUserEnabled = voiceOfUserEnabled,
                        onEmphasisAnimationComplete = onEmphasisAnimationComplete,
                        isOutgoing = true,
                        timeFormatted = timeFormatted,
                        onPlayAudio = onPlayAudio,
                        isPlayingAudio = isPlayingAudio,
                        audioProgress = audioProgress,
                        onViewMedia = onViewMedia,
                        showSenderName = !showSenderBadge
                    )
                }
            }
        } else {
            // Incoming messages use a distinct tonal surface from sent messages.
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = ExpressiveIncomingBubbleShape,
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .then(messageInteraction)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ExpressiveIncomingBubbleShape)
            ) {
                M3MessageContent(
                    message = message,
                    animateEmphasis = animateEmphasis,
                    voiceOfUserEnabled = voiceOfUserEnabled,
                    onEmphasisAnimationComplete = onEmphasisAnimationComplete,
                    isOutgoing = false,
                    timeFormatted = timeFormatted,
                    onPlayAudio = onPlayAudio,
                    isPlayingAudio = isPlayingAudio,
                    audioProgress = audioProgress,
                    onViewMedia = onViewMedia,
                    showSenderName = !showSenderBadge
                )
            }
        }
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete message from this device?") },
            text = { Text("This hides the message for you. Other participants can still see it.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TypingIndicatorBubble(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = PillShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveContainedLoadingIndicator(size = 16.dp)
                Spacer(Modifier.width(8.dp))
                Text("Typing…", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun M3MessageContent(
    message: MessageEntity,
    animateEmphasis: Boolean,
    voiceOfUserEnabled: Boolean,
    onEmphasisAnimationComplete: (String) -> Unit,
    isOutgoing: Boolean,
    timeFormatted: String,
    onPlayAudio: () -> Unit,
    isPlayingAudio: Boolean,
    audioProgress: Float,
    onViewMedia: () -> Unit,
    showSenderName: Boolean
) {
    val motionEnabled = LocalExpressiveMotionEnabled.current
    val ranges = remember(message.textEmphasisJson, message.plaintextBody, voiceOfUserEnabled) {
        if (!voiceOfUserEnabled) emptyList() else MessagePayloadCodec.decodeEmphasisRanges(message.textEmphasisJson)
            .filter { it.start >= 0 && it.end <= message.plaintextBody.length && it.start < it.end }
    }
    val emphasisProgress = remember(message.id, animateEmphasis) { Animatable(if (animateEmphasis) 0f else 1f) }
    LaunchedEffect(message.id, animateEmphasis, voiceOfUserEnabled, motionEnabled, ranges) {
        if (!voiceOfUserEnabled || ranges.isEmpty()) {
            emphasisProgress.snapTo(1f)
            if (animateEmphasis) onEmphasisAnimationComplete(message.id)
        } else if (animateEmphasis && motionEnabled) {
            emphasisProgress.snapTo(0f)
            delay(500)
            emphasisProgress.animateTo(1f, tween(ExpressiveMotion.DurationMedium2, easing = ExpressiveMotion.EmphasizedDecelerateEasing))
            onEmphasisAnimationComplete(message.id)
        } else {
            emphasisProgress.snapTo(1f)
            if (animateEmphasis) onEmphasisAnimationComplete(message.id)
        }
    }
    val emphasizedText = remember(message.plaintextBody, ranges, emphasisProgress.value) {
        buildAnnotatedString {
            append(message.plaintextBody)
            val weight = FontWeight(400 + (200f * emphasisProgress.value).toInt())
            val style = SpanStyle(
                fontWeight = weight,
                fontStyle = if (emphasisProgress.value >= 0.98f) FontStyle.Italic else FontStyle.Normal
            )
            ranges.forEach { range -> addStyle(style, range.start, range.end) }
        }
    }
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        // Group sender name
        if (showSenderName && !isOutgoing && !message.senderName.isNullOrBlank()) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Quoted Reply Snippet in M3 OutlinedCard
        if (!message.replySnippet.isNullOrBlank()) {
            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .align(Alignment.End)
                    .widthIn(max = 280.dp)
                    .padding(bottom = 6.dp)
            ) {
                Text(
                    text = message.replySnippet,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Call Blob Card
        if (message.mediaType == "call" || message.plaintextBody.startsWith("[Call]")) {
            val callText = message.plaintextBody.removePrefix("[Call]").trim()
            val isMissed = callText.contains("Missed", ignoreCase = true) || callText.contains("declined", ignoreCase = true)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isMissed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isMissed) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isMissed) Icons.Default.CallEnd else Icons.Default.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = callText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (message.mediaType?.startsWith("image") == true) {
            val localPath = message.localDecryptedPath
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable(onClick = onViewMedia)
            ) {
                if (localPath != null && File(localPath).exists()) {
                    AsyncImage(
                        model = File(localPath),
                        contentDescription = "Media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else if (!message.blurHash.isNullOrBlank()) {
                    val blurHashBitmap = remember(message.blurHash) {
                        try {
                            BlurHash.decode(message.blurHash, 30, 20)?.asImageBitmap()
                        } catch (e: Exception) { null }
                    }
                    if (blurHashBitmap != null) {
                        Image(
                            bitmap = blurHashBitmap,
                            contentDescription = "Media loading",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        ExpressiveContainedLoadingIndicator(Modifier.align(Alignment.Center), size = 24.dp)
                    }
                } else {
                    ExpressiveContainedLoadingIndicator(Modifier.align(Alignment.Center), size = 24.dp)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Audio Player using M3 FilledTonalIconButton & LinearProgressIndicator
        if (message.mediaType?.startsWith("audio") == true) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                FilledTonalIconButton(onClick = onPlayAudio) {
                    Icon(
                        imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play Audio"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    LinearProgressIndicator(
                        progress = { audioProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    val duration = message.mediaDuration
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d", duration / 60, duration % 60),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Text Body (Only for normal text messages)
        if (message.plaintextBody.isNotBlank() && message.mediaType != "call" && !message.plaintextBody.startsWith("[Call]")) {
            Text(
                text = emphasizedText,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (message.mediaType != "call" && !message.plaintextBody.startsWith("[Call]")) {
            Spacer(modifier = Modifier.height(4.dp))

            // Time + Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isOutgoing) {
                    Spacer(modifier = Modifier.width(4.dp))
                    ExpressiveReceiptIcon(status = message.status)
                }
            }
        }
    }
}

// ==============================================================================
// Official M3 ModalBottomSheet for Safety Numbers
// ==============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3SafetyNumbersSheet(
    conversationTitle: String,
    fingerprint: String?,
    onResetSession: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showResetConfirmation by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Text(
                text = "Safety Numbers Verification",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Compare this pairwise device-key fingerprint with $conversationTitle over a trusted channel. If either device was reinstalled, the fingerprint may have changed.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Fingerprint details sit on a tonal surface without a drop shadow.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = fingerprint ?: "Fingerprint available after a secure session is established",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Close")
            }

            if (onResetSession != null) {
                OutlinedButton(
                    onClick = { showResetConfirmation = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Reset secure session")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Trust the contact's current key?") },
            text = {
                Text("Only continue after comparing the fingerprint with the contact over a trusted channel. Messages encrypted before their key changed may remain unreadable.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirmation = false
                    onResetSession?.invoke()
                }) { Text("Trust key and reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) { Text("Cancel") }
            }
        )
    }
}
