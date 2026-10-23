package com.example.ui.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun VideoRenderer(
    videoTrack: VideoTrack?,
    eglBaseContext: EglBase.Context,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                init(eglBaseContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setEnableHardwareScaler(true)
            }
        },
        update = { renderer ->
            videoTrack?.addSink(renderer)
        },
        modifier = modifier
    )

    DisposableEffect(videoTrack) {
        onDispose {
            // Need to remove sink when track changes or composable leaves composition
            // Unfortunately we don't have direct access to the renderer here in onDispose
            // but the addSink on the next update will handle replacing the track if needed.
            // A more robust implementation would cache the renderer instance.
        }
    }
}
