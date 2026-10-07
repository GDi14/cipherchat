package com.example.ui.viewer

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.media.MediaDownloader
import com.example.ui.components.ExpressiveVideoControls
import com.example.ui.components.ExpressiveVideoPlaybackToggle
import com.example.ui.components.ExpressiveVideoPlayerSurface
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun FullscreenMediaViewerDialog(
    filePath: String,
    isVideo: Boolean,
    onDismiss: () -> Unit,
    onReply: () -> Unit = {}
) {
    val context = LocalContext.current
    val file = remember(filePath) { File(filePath) }
    val videoPlayer = remember(filePath, isVideo) {
        if (isVideo) ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(file.toURI().toString()))
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        } else null
    }
    DisposableEffect(videoPlayer) {
        onDispose { videoPlayer?.release() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (!isVideo) {
                // Photo Viewer with Pinch-to-Zoom
                var scale by remember { mutableFloatStateOf(1f) }
                var offsetX by remember { mutableFloatStateOf(0f) }
                var offsetY by remember { mutableFloatStateOf(0f) }

                val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                    scale = (scale * zoomChange).coerceIn(1f, 5f)
                    offsetX += offsetChange.x
                    offsetY += offsetChange.y
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .transformable(state = state),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = file,
                        contentDescription = "Full photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                }
            } else {
                // Native video output stays in Media3; all playback controls use Compose M3.
                videoPlayer?.let { player ->
                    ExpressiveVideoPlayerSurface(player, Modifier.fillMaxSize())
                    ExpressiveVideoPlaybackToggle(player, Modifier.align(Alignment.Center).size(64.dp))
                }
            }

            // Close button (Top Left)
            FilledTonalIconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .testTag("media_viewer_close_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }

            // Download to Device button (Top Right)
            FilledTonalIconButton(
                onClick = {
                    val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
                    val displayName = "Cipher_${System.currentTimeMillis()}${if (isVideo) ".mp4" else ".jpg"}"
                    val result = MediaDownloader.saveToDownloads(context, file, displayName, mimeType)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Saved to Downloads/Cipher", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Download failed: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = "Download to Device")
            }

            if (isVideo) videoPlayer?.let { player ->
                ExpressiveVideoControls(
                    player = player,
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)
                )
            }

            FilledTonalIconButton(
                onClick = { onReply(); onDismiss() },
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Reply to media")
            }
        }
    }
}
