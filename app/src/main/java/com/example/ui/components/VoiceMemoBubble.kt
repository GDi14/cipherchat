package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.AudioPlayerHelper
import com.example.ui.theme.EditorialTypography
import com.example.ui.theme.ExperimentalMaterial3ExpressiveApi
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VoiceMemoBubble(
    audioUrlOrPath: String,
    isMe: Boolean,
    audioPlayerHelper: AudioPlayerHelper,
    durationMs: Long = 0L,
    modifier: Modifier = Modifier
) {
    val isPlaying by audioPlayerHelper.isPlaying.collectAsState()
    val activePath by audioPlayerHelper.currentPlayingPath.collectAsState()
    val isThisMemoPlaying = isPlaying && activePath == audioUrlOrPath
    val positionMs by audioPlayerHelper.positionMs.collectAsState()
    val playerDurationMs by audioPlayerHelper.durationMs.collectAsState()
    val speedMultiplier by audioPlayerHelper.playbackSpeed.collectAsState()
    val isActiveMemo = activePath == audioUrlOrPath
    val memoPositionMs = if (isActiveMemo) positionMs else 0L
    val resolvedDurationMs = if (isActiveMemo) {
        playerDurationMs.takeIf { it > 0L } ?: durationMs
    } else durationMs
    val currentProgress = if (resolvedDurationMs > 0L) {
        (memoPositionMs.toFloat() / resolvedDurationMs).coerceIn(0f, 1f)
    } else 0f

    DisposableEffect(audioUrlOrPath) {
        onDispose {
            if (audioPlayerHelper.currentPlayingPath.value == audioUrlOrPath) {
                audioPlayerHelper.stopAudio()
            }
        }
    }

    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
    }

    val containerColor = if (isMe) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = if (isMe) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = bubbleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .widthIn(min = 220.dp, max = 290.dp)
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Play / Pause Toggle Button
                FilledIconButton(
                    onClick = {
                        if (isThisMemoPlaying) {
                            audioPlayerHelper.pauseAudio()
                        } else {
                            if (activePath == audioUrlOrPath) {
                                audioPlayerHelper.resumeAudio()
                            } else {
                                audioPlayerHelper.playAudio(audioUrlOrPath)
                            }
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = if (isThisMemoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isThisMemoPlaying) "Pause voice memo" else "Play voice memo"
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Audio Progress / Waveform Indicator
                    Slider(
                        value = currentProgress,
                        onValueChange = { fraction ->
                            if (resolvedDurationMs > 0L) {
                                audioPlayerHelper.seekTo((fraction * resolvedDurationMs).toLong())
                            }
                        },
                        enabled = resolvedDurationMs > 0L,
                        colors = SliderDefaults.colors(
                            thumbColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            activeTrackColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            inactiveTrackColor = contentColor.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatDuration(memoPositionMs)} / ${formatDuration(resolvedDurationMs)}",
                            style = EditorialTypography.MonospacedNumeral,
                            color = contentColor.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )

                        // Speed multiplier toggle button
                        Surface(
                            onClick = {
                                val nextSpeed = when (speedMultiplier) {
                                    1.0f -> 1.5f
                                    1.5f -> 2.0f
                                    else -> 1.0f
                                }
                                audioPlayerHelper.setPlaybackSpeed(nextSpeed)
                            },
                            shape = MaterialTheme.shapes.large,
                            color = contentColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${speedMultiplier}x",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
