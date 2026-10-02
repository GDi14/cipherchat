package com.example.ui.conversation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.entity.LocalMessageEntity
import com.example.ui.components.ExpressiveAvatar
import com.example.ui.components.ExpressiveSecurityBadge
import com.example.ui.components.ExpressiveTypingIndicator
import com.example.ui.components.ExpressiveWavyProgressIndicator
import com.example.ui.components.expressivePressScale
import com.example.ui.conversation.audio.AudioPlaybackState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val friendContact by viewModel.friendContact.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val replyingToMessage by viewModel.replyingToMessage.collectAsState()

    val friendIsTyping by viewModel.friendIsTyping.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDuration by viewModel.recordingDurationSeconds.collectAsState()
    val playbackState by viewModel.audioPlaybackState.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Start listening to friend's typing status
    LaunchedEffect(viewModel.friendUserId) {
        viewModel.startListeningToTyping(viewModel.friendUserId)
    }

    // Attachment Picker Launcher
    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment ?: "attachment"
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            viewModel.sendAttachment(uri, fileName, mimeType)
        }
    }

    val sortedMessages = remember(messages) {
        messages.sortedBy { it.timestamp }
    }

    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    // Auto-scroll when messages update, typing status changes, or keyboard opens
    LaunchedEffect(sortedMessages.size, friendIsTyping, imeBottomPadding) {
        if (sortedMessages.isNotEmpty()) {
            listState.animateScrollToItem(sortedMessages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // Shifts the entire layout upwards when soft keyboard opens!
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExpressiveAvatar(
                            name = friendContact?.displayName ?: "Friend",
                            isOnline = friendContact?.isOnline ?: false,
                            size = 40.dp
                        )
                        Column {
                            Text(
                                text = friendContact?.displayName ?: "Encrypted Chat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        friendIsTyping -> "typing..."
                                        friendContact?.isOnline == true -> "Online • E2EE"
                                        else -> "Signal E2EE"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (friendIsTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (friendIsTyping) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                actions = {
                    ExpressiveSecurityBadge(text = "Double Ratchet")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Error Banner
            errorMessage?.let { err ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.dismissError() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedMessages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        playbackState = playbackState,
                        onPlayVoiceMemo = {
                            viewModel.playOrPauseVoiceMemo(context, msg.id, msg.attachmentLocalPath)
                        },
                        onLongClick = {
                            viewModel.setReplyingTo(msg)
                        }
                    )
                }
            }

            // Friend Typing Indicator Banner
            AnimatedVisibility(
                visible = friendIsTyping,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    ExpressiveTypingIndicator(
                        friendName = friendContact?.displayName ?: "Friend"
                    )
                }
            }

            // Reply Preview Banner
            AnimatedVisibility(
                visible = replyingToMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                replyingToMessage?.let { replyMsg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Replying to ${if (replyMsg.isOutgoing) "You" else friendContact?.displayName ?: "Friend"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = replyMsg.plaintext.ifBlank { "[Attachment]" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { viewModel.cancelReply() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Reply",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Input Bar / Recording Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 6.dp
            ) {
                if (isRecording) {
                    // Voice Recorder Active Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color.Red, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val minutes = recordingDuration / 60
                            val seconds = recordingDuration % 60
                            Text(
                                text = String.format("Recording Voice Memo %d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { viewModel.cancelVoiceRecording() },
                                modifier = Modifier.background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    CircleShape
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Recording",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }

                            IconButton(
                                onClick = { viewModel.stopAndSendVoiceRecording(context) },
                                modifier = Modifier.background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Send Recording",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                } else {
                    // Normal Message Input Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { attachmentLauncher.launch("*/*") },
                            modifier = Modifier.expressivePressScale()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach File",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { newText ->
                                textInput = newText
                                viewModel.onUserTyping(viewModel.friendUserId, newText)
                            },
                            placeholder = { Text("Type encrypted message...") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            shape = MaterialTheme.shapes.large,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )

                        if (textInput.isBlank()) {
                            IconButton(
                                onClick = { viewModel.startVoiceRecording(context) },
                                modifier = Modifier.expressivePressScale()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Memo",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val msg = textInput
                                    textInput = ""
                                    viewModel.onUserTyping(viewModel.friendUserId, "")
                                    viewModel.sendText(msg)
                                },
                                modifier = Modifier
                                    .expressivePressScale()
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: LocalMessageEntity,
    playbackState: AudioPlaybackState,
    onPlayVoiceMemo: () -> Unit,
    onLongClick: () -> Unit
) {
    val isOutgoing = message.isOutgoing
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    val bubbleColor = if (isOutgoing) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val textColor = if (isOutgoing) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            modifier = Modifier
                .padding(
                    start = if (isOutgoing) 48.dp else 0.dp,
                    end = if (isOutgoing) 0.dp else 48.dp
                )
                .clickable { onLongClick() },
            shape = bubbleShape,
            color = bubbleColor,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Reply Quote Section
                if (!message.replyToText.isNullOrBlank()) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = message.replyToSenderName ?: "Quote",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = message.replyToText,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Attachments Rendering
                if (message.hasAttachment) {
                    val mime = message.attachmentMimeType ?: ""
                    when {
                        mime.startsWith("image/") -> {
                            val imgUrl = message.attachmentLocalPath ?: message.attachmentUrl
                            if (!imgUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = "Attachment Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        mime.startsWith("audio/") -> {
                            // Voice Memo Playback Bubble
                            val isPlaying = playbackState.activeMessageId == message.id && playbackState.isPlaying
                            val progress = if (playbackState.activeMessageId == message.id && playbackState.durationMs > 0) {
                                playbackState.currentPositionMs.toFloat() / playbackState.durationMs
                            } else 0f

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onPlayVoiceMemo,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    ExpressiveWavyProgressIndicator(
                                        progress = progress,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = message.attachmentFileName ?: "Voice Memo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                        else -> {
                            // Generic File Attachment
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message.attachmentFileName ?: "Encrypted File",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                // Plaintext Content
                if (message.plaintext.isNotBlank()) {
                    Text(
                        text = message.plaintext,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Time and Status
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f)
                    )

                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val statusIcon = when (message.status) {
                            "READ" -> Icons.Default.DoneAll
                            "DELIVERED" -> Icons.Default.DoneAll
                            "SENT" -> Icons.Default.Check
                            else -> Icons.Default.Check
                        }
                        val tintColor = if (message.status == "READ") {
                            Color(0xFF3B82F6)
                        } else {
                            textColor.copy(alpha = 0.7f)
                        }
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = message.status,
                            modifier = Modifier.size(14.dp),
                            tint = tintColor
                        )
                    }
                }
            }
        }
    }
}
