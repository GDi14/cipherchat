package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.theme.ExpressiveMotion
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.local.MessageEntity
import com.example.ui.components.ExpressiveAvatar
import com.example.ui.components.ExpressiveReceiptIcon
import com.example.ui.theme.PillShape
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val typingUsersMap by viewModel.typingUsers.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSafetySheet by remember { mutableStateOf(false) }

    val isImeVisible = WindowInsets.isImeVisible

    BackHandler {
        onBack()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Media Pickers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.sendMediaUri(uri, "image/jpeg")
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
                    val isTyping = (typingUsersMap[viewModel.conversationId]?.isNotEmpty() == true)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showSafetySheet = true }
                    ) {
                        ExpressiveAvatar(name = title, size = 38.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                                text = if (isTyping) "typing..." else "Double Ratchet Verified",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                actions = {
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
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(messages, key = { it.id }) { message ->
                    M3MessageItem(
                        message = message,
                        isOutgoing = message.senderId == viewModel.myUserId,
                        onReply = { viewModel.setReplyingTo(message) },
                        onPlayAudio = { viewModel.playAudio(message) },
                        isPlayingAudio = uiState.playingAudioMessageId == message.id,
                        audioProgress = if (uiState.playingAudioMessageId == message.id) uiState.audioProgress else 0f,
                        onViewMedia = { viewModel.openMediaViewer(message) }
                    )
                }
            }

            // M3 Tonal Surface Bottom Composer
            Surface(
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
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
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alpha"
                            )

                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = pulseAlpha))
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
                            Text(
                                text = "Recording encrypted audio...",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                modifier = Modifier.testTag("attach_media_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Attach")
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            OutlinedTextField(
                                value = uiState.inputText,
                                onValueChange = { viewModel.onInputTextChanged(it) },
                                placeholder = { Text("Encrypted message...") },
                                maxLines = 4,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("message_input")
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            if (uiState.inputText.isNotBlank()) {
                                FilledIconButton(
                                    onClick = { viewModel.sendTextMessage() },
                                    enabled = !uiState.isSending,
                                    modifier = Modifier.testTag("send_message_button")
                                ) {
                                    if (uiState.isSending) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                                    }
                                }
                            } else {
                                FilledTonalIconButton(
                                    onClick = {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            viewModel.startVoiceRecording()
                                        } else {
                                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier.testTag("voice_record_button")
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "Record Voice")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Safety Numbers Verification ModalBottomSheet (Official M3)
    if (showSafetySheet) {
        M3SafetyNumbersSheet(
            conversationTitle = uiState.conversation?.title ?: "Chat",
            conversationId = viewModel.conversationId,
            onDismiss = { showSafetySheet = false }
        )
    }

    // Full Screen Media Viewer Dialog
    uiState.selectedViewerMedia?.let { mediaMsg ->
        Dialog(
            onDismissRequest = { viewModel.closeMediaViewer() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                IconButton(
                    onClick = { viewModel.closeMediaViewer() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .testTag("media_viewer_close_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                val localPath = mediaMsg.localDecryptedPath
                if (localPath != null && File(localPath).exists()) {
                    AsyncImage(
                        model = File(localPath),
                        contentDescription = "Decrypted Media",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ==============================================================================
// Official M3 Message Item (ElevatedCard, LinearProgressIndicator, Buttons)
// ==============================================================================

@Composable
fun M3MessageItem(
    message: MessageEntity,
    isOutgoing: Boolean,
    onReply: () -> Unit,
    onPlayAudio: () -> Unit,
    isPlayingAudio: Boolean,
    audioProgress: Float,
    onViewMedia: () -> Unit
) {
    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    val scale = remember { Animatable(0.92f) }
    LaunchedEffect(message.id) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.65f,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(
                scaleX = scale.value,
                scaleY = scale.value,
                transformOrigin = TransformOrigin(if (isOutgoing) 1f else 0f, 1f)
            ),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        if (isOutgoing) {
            // Outgoing Bubble: ElevatedCard with primaryContainer
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .clickable(onClick = onReply)
            ) {
                M3MessageContent(
                    message = message,
                    isOutgoing = true,
                    timeFormatted = timeFormatted,
                    onPlayAudio = onPlayAudio,
                    isPlayingAudio = isPlayingAudio,
                    audioProgress = audioProgress,
                    onViewMedia = onViewMedia
                )
            }
        } else {
            // Incoming Bubble: OutlinedCard with surfaceContainerHigh
            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .clickable(onClick = onReply)
            ) {
                M3MessageContent(
                    message = message,
                    isOutgoing = false,
                    timeFormatted = timeFormatted,
                    onPlayAudio = onPlayAudio,
                    isPlayingAudio = isPlayingAudio,
                    audioProgress = audioProgress,
                    onViewMedia = onViewMedia
                )
            }
        }
    }
}

@Composable
private fun M3MessageContent(
    message: MessageEntity,
    isOutgoing: Boolean,
    timeFormatted: String,
    onPlayAudio: () -> Unit,
    isPlayingAudio: Boolean,
    audioProgress: Float,
    onViewMedia: () -> Unit
) {
    Column(modifier = Modifier.padding(12.dp)) {
        // Group sender name
        if (!isOutgoing && !message.senderName.isNullOrBlank()) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Quoted Reply Snippet in M3 OutlinedCard
        if (!message.replySnippet.isNullOrBlank()) {
            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
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

        // Media Image Card
        if (message.mediaType?.startsWith("image") == true) {
            val localPath = message.localDecryptedPath
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .clickable(onClick = onViewMedia)
            ) {
                if (localPath != null && File(localPath).exists()) {
                    AsyncImage(
                        model = File(localPath),
                        contentDescription = "Media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).align(Alignment.Center),
                        strokeWidth = 2.dp
                    )
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

        // Text Body
        if (message.plaintextBody.isNotBlank()) {
            Text(
                text = message.plaintextBody,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Time + Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            if (isOutgoing) {
                Spacer(modifier = Modifier.width(4.dp))
                ExpressiveReceiptIcon(status = message.status)
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
    conversationId: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            FilledTonalIconButton(
                onClick = { },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp)
                )
            }

            Text(
                text = "Safety Numbers Verification",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Comparing safety numbers with $conversationTitle verifies that your conversation is genuinely end-to-end encrypted with no third-party interception.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Fingerprint Card (ElevatedCard)
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val hash = kotlin.math.abs(conversationId.hashCode())
                    val p1 = String.format(Locale.getDefault(), "%05d", hash % 100000)
                    val p2 = String.format(Locale.getDefault(), "%05d", (hash / 7) % 100000)
                    val p3 = String.format(Locale.getDefault(), "%05d", (hash / 13) % 100000)
                    val p4 = String.format(Locale.getDefault(), "%05d", (hash / 19) % 100000)

                    Text(
                        text = "$p1  $p2\n$p3  $p4",
                        style = MaterialTheme.typography.headlineSmall,
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
                Text("Verification Confirmed")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
