package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.sin

// Material 3 Official Motion Tokens & Easing Curves
val M3EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
val M3EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
val M3EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
val M3StandardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

const val M3MotionDurationShort1 = 50
const val M3MotionDurationShort2 = 100
const val M3MotionDurationMedium1 = 250
const val M3MotionDurationMedium2 = 300
const val M3MotionDurationLong1 = 450
const val M3MotionDurationLong2 = 500

// Expressive Motion Physics & Spring Specs (M3 Motion System)
val ExpressiveBouncySpring = spring<Float>(
    stiffness = Spring.StiffnessLow,
    dampingRatio = Spring.DampingRatioMediumBouncy
)

val ExpressiveFastBouncySpring = spring<Float>(
    stiffness = Spring.StiffnessMediumLow,
    dampingRatio = 0.65f
)

val ExpressiveGentleSpring = spring<Float>(
    stiffness = Spring.StiffnessMediumLow,
    dampingRatio = Spring.DampingRatioNoBouncy
)

/**
 * Expressive Scale-on-Touch Physics Modifier
 */
@Composable
fun Modifier.expressivePressScale(
    pressedScale: Float = 0.93f,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = ExpressiveBouncySpring,
        label = "expressive_press_scale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Expressive Physics Button
 */
@Composable
fun ExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = ExpressiveBouncySpring,
        label = "m3_expressive_btn_scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 6.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "m3_expressive_btn_elevation"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = colors.containerColor,
        contentColor = colors.contentColor,
        shadowElevation = elevation,
        tonalElevation = elevation,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Expressive Card with Spring Scale Feedback
 */
@Composable
fun ExpressiveCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = ExpressiveFastBouncySpring,
        label = "expressive_card_scale"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.large,
        colors = colors,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        content()
    }
}

/**
 * Expressive Morphing Loading Indicator
 */
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3_expressive_loading")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val morphFactor by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "morph"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = this.size.minDimension
            val cornerRadius = canvasSize * morphFactor
            drawRoundRect(
                color = color,
                size = Size(canvasSize, canvasSize),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = canvasSize * 0.12f)
            )
        }
    }
}

/**
 * Expressive Wavy Progress Bar
 */
@Composable
fun ExpressiveWavyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    waveAmplitude: Dp = 4.dp,
    waveFrequency: Float = 4f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "expressive_wavy_progress")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val ampPx = waveAmplitude.toPx()

        // Draw track
        drawLine(
            color = trackColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 6.dp.toPx()
        )

        // Draw animated wave progress line
        val progressWidth = width * progress.coerceIn(0f, 1f)
        if (progressWidth > 0f) {
            val path = Path()
            path.moveTo(0f, centerY)

            var x = 0f
            val step = 4f
            while (x <= progressWidth) {
                val waveOffset = sin((x / width * waveFrequency * 2 * Math.PI) + phase) * ampPx
                path.lineTo(x, (centerY + waveOffset).toFloat())
                x += step
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 6.dp.toPx())
            )
        }
    }
}

/**
 * Expressive User Avatar
 */
@Composable
fun ExpressiveAvatar(
    name: String,
    imageUrl: String? = null,
    isOnline: Boolean = false,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = name.trim().firstOrNull()?.uppercase() ?: "?"
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Online Status Badge
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .background(Color(0xFF10B981), CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

/**
 * Expressive Split Button
 */
@Composable
fun ExpressiveSplitButton(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryIcon: @Composable () -> Unit
) {
    val primaryInteraction = remember { MutableInteractionSource() }
    val secondaryInteraction = remember { MutableInteractionSource() }

    val primaryPressed by primaryInteraction.collectIsPressedAsState()
    val secondaryPressed by secondaryInteraction.collectIsPressedAsState()

    val primaryScale by animateFloatAsState(
        targetValue = if (primaryPressed) 0.94f else 1f,
        animationSpec = ExpressiveBouncySpring,
        label = "split_btn_primary_scale"
    )

    val secondaryScale by animateFloatAsState(
        targetValue = if (secondaryPressed) 0.86f else 1f,
        animationSpec = ExpressiveBouncySpring,
        label = "split_btn_secondary_scale"
    )

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primary)
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = primaryScale
                    scaleY = primaryScale
                }
                .clickable(
                    interactionSource = primaryInteraction,
                    indication = null,
                    onClick = onPrimaryClick
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = primaryText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight(0.6f)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(52.dp)
                .graphicsLayer {
                    scaleX = secondaryScale
                    scaleY = secondaryScale
                }
                .clickable(
                    interactionSource = secondaryInteraction,
                    indication = null,
                    onClick = onSecondaryClick
                ),
            contentAlignment = Alignment.Center
        ) {
            secondaryIcon()
        }
    }
}

/**
 * Expressive Segmented Button Group
 */
@Composable
fun ExpressiveButtonGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1f,
                    animationSpec = ExpressiveBouncySpring,
                    label = "group_btn_scale"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "group_btn_color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelect(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

/**
 * Expressive Floating Toolbar Container
 */
@Composable
fun ExpressiveFloatingToolbar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 8.dp,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/**
 * Expressive Security Shield Badge
 */
@Composable
fun ExpressiveSecurityBadge(
    modifier: Modifier = Modifier,
    text: String = "Signal E2EE Active"
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/**
 * Expressive Typing Indicator
 */
@Composable
fun ExpressiveTypingIndicator(
    friendName: String = "Friend",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "$friendName is typing",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer { alpha = dot1Alpha }
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer { alpha = dot2Alpha }
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer { alpha = dot3Alpha }
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}
