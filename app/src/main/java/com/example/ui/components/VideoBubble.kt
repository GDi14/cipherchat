package com.example.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.Locale

@Composable
fun VideoBubble(
    videoUrlOrPath: String,
    isMe: Boolean,
    durationMs: Long = 0L,
    modifier: Modifier = Modifier
) {
    var showFullscreenPlayer by remember { mutableStateOf(false) }
    val containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHigh

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .size(width = 240.dp, height = 160.dp)
            .padding(vertical = 4.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Play video, ${formatDuration(durationMs)}"
            }
            .clickable { showFullscreenPlayer = true }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(32.dp))
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
            ) {
                Text(
                    text = formatDuration(durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }

    if (showFullscreenPlayer) {
        VideoPlayerDialog(videoUrlOrPath, onDismiss = { showFullscreenPlayer = false })
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerDialog(
    videoUrlOrPath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val player = remember(videoUrlOrPath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(toMediaUri(videoUrlOrPath)))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            ExpressiveVideoPlayerSurface(player, Modifier.fillMaxSize())

            FilledTonalIconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close video")
            }

            ExpressiveVideoPlaybackToggle(player, Modifier.align(Alignment.Center).size(64.dp))

            ExpressiveVideoControls(
                player = player,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp)
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ExpressiveVideoPlayerSurface(player: ExoPlayer, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                this.player = player
            }
        },
        update = { it.player = player },
        modifier = modifier
    )
}

@Composable
fun ExpressiveVideoControls(player: ExoPlayer, modifier: Modifier = Modifier) {
    var positionMs by remember(player) { mutableLongStateOf(0L) }
    var durationMs by remember(player) { mutableLongStateOf(0L) }
    var dragPosition by remember(player) { mutableFloatStateOf(0f) }
    var isSeeking by remember(player) { mutableStateOf(false) }

    LaunchedEffect(player) {
        while (isActive) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.takeIf { it > 0L } ?: 0L
            delay(300)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Slider(
                value = (if (isSeeking) dragPosition else if (durationMs > 0) positionMs.toFloat() / durationMs else 0f)
                    .coerceIn(0f, 1f),
                onValueChange = { dragPosition = it; isSeeking = true },
                onValueChangeFinished = {
                    player.seekTo((dragPosition * durationMs).toLong())
                    isSeeking = false
                },
                enabled = durationMs > 0,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.semantics { contentDescription = "Video playback position" }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0L)) },
                    enabled = durationMs > 0
                ) {
                    Icon(Icons.Default.Replay10, contentDescription = "Back 10 seconds")
                }
                Text(
                    "${formatDuration(if (isSeeking) (dragPosition * durationMs).toLong() else positionMs)} / ${formatDuration(durationMs)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = { player.seekTo((player.currentPosition + 10_000).coerceAtMost(durationMs)) },
                    enabled = durationMs > 0
                ) {
                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10 seconds")
                }
            }
        }
    }
}

@Composable
fun ExpressiveVideoPlaybackToggle(player: ExoPlayer, modifier: Modifier = Modifier) {
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    LaunchedEffect(player) {
        while (isActive) {
            isPlaying = player.isPlaying
            delay(300)
        }
    }
    FilledIconButton(
        onClick = {
            if (player.isPlaying) player.pause() else player.play()
            isPlaying = player.isPlaying
        },
        modifier = modifier.semantics {
            contentDescription = if (isPlaying) "Pause video" else "Play video"
        }
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
    }
}

private fun toMediaUri(pathOrUrl: String): Uri {
    val uri = Uri.parse(pathOrUrl)
    return if (uri.scheme.isNullOrBlank()) Uri.fromFile(File(pathOrUrl)) else uri
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
