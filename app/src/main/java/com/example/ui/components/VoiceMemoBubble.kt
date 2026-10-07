package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.media.AudioPlayerHelper
import com.example.ui.theme.ExpressiveIncomingBubbleShape
import com.example.ui.theme.ExpressiveOutgoingBubbleShape
import java.util.Locale

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

    val bubbleShape = if (isMe) ExpressiveOutgoingBubbleShape else ExpressiveIncomingBubbleShape

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
                    modifier = Modifier.size(48.dp)
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
                    ExpressiveWaveform(currentProgress, isThisMemoPlaying,
                        Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 4.dp))
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
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = "Voice memo playback position"
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatDuration(memoPositionMs)} / ${formatDuration(resolvedDurationMs)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                            color = contentColor.copy(alpha = 0.8f),
                        )

                        // Speed multiplier toggle button
                        FilledTonalButton(
                            onClick = {
                                val nextSpeed = when (speedMultiplier) {
                                    1.0f -> 1.5f
                                    1.5f -> 2.0f
                                    else -> 1.0f
                                }
                                audioPlayerHelper.setPlaybackSpeed(nextSpeed)
                            },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Playback speed"
                                    stateDescription = "${speedMultiplier} times"
                                }
                        ) {
                            Text(
                                text = "${speedMultiplier}×",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
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
