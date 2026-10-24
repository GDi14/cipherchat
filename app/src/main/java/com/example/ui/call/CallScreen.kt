package com.example.ui.call

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.ExpressiveAvatar
import com.example.ui.theme.ExpressiveButtonShape
import com.example.ui.theme.ExpressiveCardShape
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.ExpressiveStarShape
import com.example.ui.theme.ExpressiveTopSurfaceShape
import com.example.ui.theme.GoogleSansFlexHeadline
import com.example.ui.theme.LocalExpressiveMotionEnabled
import org.webrtc.EglBase
import org.webrtc.PeerConnection
import org.webrtc.VideoTrack

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CallScreen(
    viewModel: CallViewModel,
    onHangUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val callSessionState by viewModel.callSessionState.collectAsState()
    val localVideoTrack = viewModel.localVideoTrack
    val remoteVideoTrack by viewModel.remoteVideoTrack.collectAsState()
    val eglBaseContext = viewModel.getEglBaseContext()

    val isMicMuted by viewModel.isMicMuted.collectAsState()
    val isVideoEnabled by viewModel.isVideoEnabled.collectAsState()

    var areControlsVisible by remember { mutableStateOf(true) }
    val motionEnabled = LocalExpressiveMotionEnabled.current

    // Pulsing animation for calling / ringing state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_ring")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LaunchedEffect(callSessionState) {
        if (callSessionState is CallViewModel.CallSessionState.Ended) {
            onHangUp()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .pointerInput(Unit) {
                detectTapGestures {
                    areControlsVisible = !areControlsVisible
                }
            }
    ) {
        // Remote Video (Full Screen when connected)
        if (remoteVideoTrack != null && connectionState == PeerConnection.PeerConnectionState.CONNECTED) {
            VideoRenderer(
                videoTrack = remoteVideoTrack,
                eglBaseContext = eglBaseContext,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Material 3 Expressive Calling / Ringing UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Pulsing Expressive Avatar Container
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                                alpha = pulseAlpha
                            }
                            .clip(ExpressiveStarShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    Surface(
                        shape = ExpressiveStarShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(130.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            ExpressiveAvatar(
                                name = viewModel.remoteUserName,
                                avatarUrl = null,
                                size = 110.dp,
                                shape = ExpressiveStarShape
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = viewModel.remoteUserName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = GoogleSansFlexHeadline,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status Pill
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = when (callSessionState) {
                                is CallViewModel.CallSessionState.Ringing -> "Ringing..."
                                is CallViewModel.CallSessionState.Connecting -> "Connecting WebRTC..."
                                is CallViewModel.CallSessionState.Ended -> "Call Ended"
                                else -> "Connecting..."
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Local Video (PiP) - Draggable and always visible regardless of controls
        var pipOffsetX by remember { mutableFloatStateOf(0f) }
        var pipOffsetY by remember { mutableFloatStateOf(0f) }

        if (localVideoTrack != null && isVideoEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset { IntOffset(pipOffsetX.roundToInt(), pipOffsetY.roundToInt()) }
                        .size(width = 110.dp, height = 150.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                pipOffsetX += dragAmount.x
                                pipOffsetY += dragAmount.y
                            }
                        },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 8.dp
                ) {
                    VideoRenderer(
                        videoTrack = localVideoTrack,
                        eglBaseContext = eglBaseContext,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Top Overlay Bar (caller details when in video call)
        if (remoteVideoTrack != null && connectionState == PeerConnection.PeerConnectionState.CONNECTED) {
            AnimatedVisibility(
                visible = areControlsVisible,
                enter = if (!motionEnabled) EnterTransition.None else slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(ExpressiveMotion.DurationMedium2, easing = ExpressiveMotion.EmphasizedDecelerateEasing)
                ) + fadeIn(animationSpec = tween(ExpressiveMotion.DurationMedium1)),
                exit = if (!motionEnabled) ExitTransition.None else slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(ExpressiveMotion.DurationShort4, easing = ExpressiveMotion.EmphasizedAccelerateEasing)
                ) + fadeOut(animationSpec = tween(ExpressiveMotion.DurationShort3)),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExpressiveAvatar(
                            name = viewModel.remoteUserName,
                            avatarUrl = null,
                            size = 36.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.remoteUserName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "End-to-End Encrypted",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Bottom Expressive Control Bar
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = if (!motionEnabled) EnterTransition.None else slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(ExpressiveMotion.DurationMedium2, easing = ExpressiveMotion.EmphasizedDecelerateEasing)
            ) + fadeIn(animationSpec = tween(ExpressiveMotion.DurationMedium1)),
            exit = if (!motionEnabled) ExitTransition.None else slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(ExpressiveMotion.DurationShort4, easing = ExpressiveMotion.EmphasizedAccelerateEasing)
            ) + fadeOut(animationSpec = tween(ExpressiveMotion.DurationShort3)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ExpressiveTopSurfaceShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .padding(vertical = 24.dp, horizontal = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mic Mute Toggle
                    FilledTonalIconButton(
                        onClick = { viewModel.toggleMic() },
                        shape = ExpressiveButtonShape,
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isMicMuted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (isMicMuted) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Toggle Mic"
                        )
                    }

                    // End Call Button
                    Button(
                        onClick = { viewModel.endCall("User ended call") },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Hang up",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Video Enable Toggle
                    FilledTonalIconButton(
                        onClick = { viewModel.toggleVideo() },
                        shape = ExpressiveButtonShape,
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (!isVideoEnabled) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (!isVideoEnabled) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = if (!isVideoEnabled) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            contentDescription = "Toggle Video"
                        )
                    }
                }
            }
        }
    }
}
