package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.AudioRecorderHelper
import com.example.ui.theme.EditorialTypography
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@Composable
fun VoiceRecorderBar(
    onSendVoiceMemo: (recordedFile: File, durationMs: Long) -> Unit,
    onCancelRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recorderHelper = remember { AudioRecorderHelper(context) }

    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recordingDurationMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        recordingFile = recorderHelper.startRecording()
        val startTime = System.currentTimeMillis()
        while (recorderHelper.isRecording()) {
            recordingDurationMs = System.currentTimeMillis() - startTime
            delay(100)
        }
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Static recording badge remains clear when system animations are disabled.
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Recording voice memo",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Recording Duration Text
                Text(
                    text = formatDuration(recordingDurationMs),
                    style = EditorialTypography.MonospacedNumeral,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cancel Button
                IconButton(
                    onClick = {
                        recorderHelper.stopRecording()
                        recordingFile?.delete()
                        onCancelRecording()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Cancel recording",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                // Send Voice Memo Button
                FilledIconButton(
                    onClick = {
                        val file = recorderHelper.stopRecording() ?: recordingFile
                        if (file != null && file.exists() && recordingDurationMs > 500) {
                            onSendVoiceMemo(file, recordingDurationMs)
                        } else {
                            file?.delete()
                            onCancelRecording()
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send voice memo"
                    )
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
