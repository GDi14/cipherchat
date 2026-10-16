package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.example.ui.theme.GoogleSansFlexDisplay
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.model.DockCategory
import com.example.ui.theme.DockInactiveText
import com.example.ui.theme.DockObsidian
import com.example.ui.theme.LocalExpressiveMotionEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Category color profiles matched directly to the video and screenshot:
 * - Number: Deep burnt copper
 * - Symbols: Radiant electric tangelo / neon orange
 * - Word: Warm terracotta / burnt sienna
 * - Text: Dark vermilion sunset
 */
private data class CategoryColorProfile(
    val top: Color,
    val bottom: Color,
    val specular: Color
)

private fun getCategoryColorProfile(category: DockCategory): CategoryColorProfile = CategoryColorProfile(
    top = Color(0xFFFF6200),
    bottom = Color(0xFFE03800),
    specular = Color(0xFFFFA070)
)

/**
 * Replicates the fluid interactive sidebar dock component:
 *
 * 1. ASYMMETRIC ORGANIC PEBBLE PILL:
 *    - Resting: Upright organic pebble squircle (32dp width x 41dp height) with
 *      asymmetric corner radii (18dp left, 13dp right), matching the reference screenshot.
 *    - Lettering: Upright bold capital 'A' paired with slanted italic lowercase 'a' ("Aa").
 *
 * 2. EXPANDED STRETCHED PILL & SINGLE-LINE LAYOUT:
 *    - When expanded, the pill stretches until it almost matches the width of the container
 *      (e.g. 46dp width within 50dp dock container, leaving just 2dp margin on each side).
 *    - The rotated text layout is mapped to the pill's full height (98dp+), strictly enforcing
 *      maxLines = 1, softWrap = false. It NEVER wraps into two rows.
 *    - When the text fits, it sits cleanly centered. If it exceeds available length,
 *      basicMarquee() automatically activates to scroll the text smoothly along the single line.
 *
 * 3. COMPACT 3-ITEM APERTURE:
 *    - Total dock height 158dp. Exactly 3 items visible: [above, center, below].
 *
 * 4. REAL-TIME GRADIENT COLOR MORPHING:
 *    - Gradient continuously interpolates in real time as the user drags.
 *    - Specular flash pulse on item boundary crossing.
 */
@Composable
fun FluidSidebarDock(
    items: List<DockCategory>,
    selectedCategoryId: String?,
    onCategorySelected: (DockCategory) -> Unit,
    onCategoryActivated: (DockCategory) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val itemCount = items.size
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // Compact item spacing (38dp between item centers)
    val itemSpacingDp = 38.dp
    val itemSpacingPx = with(density) { itemSpacingDp.toPx() }

    // Proportions:
    // Resting: 32dp width, 41dp height (upright pebble squircle)
    // Expanded: 46dp width (almost full container width of 50dp!), 98dp height (ample single-line room)
    val restingPillWidthDp = 32.dp
    val restingPillHeightDp = 41.dp
    val expandedPillWidthDp = 46.dp
    val expandedPillHeightDp = 98.dp

    // Selected index in base list (0..3)
    val selectedIndex = items.indexOfFirst { it.id == selectedCategoryId }.coerceAtLeast(0)

    // Unbounded continuous scroll offset supporting infinite cycling
    val scrollOffset = remember { Animatable(selectedIndex.toFloat()) }

    var isPressed by remember { mutableStateOf(false) }
    var lastHapticIndex by remember { mutableIntStateOf(selectedIndex) }

    // Flash intensity animatable (1f -> peak flash, 0f -> resting state)
    val flashAnim = remember { Animatable(0f) }
    val iconProgress = remember { Animatable(1f) }
    val motionEnabled = LocalExpressiveMotionEnabled.current

    // Ensure icon layer starts fully visible and formed immediately.
    LaunchedEffect(motionEnabled) {
        iconProgress.snapTo(1f)
    }

    // Trigger flash when category changes
    LaunchedEffect(selectedCategoryId) {
        flashAnim.snapTo(1f)
        flashAnim.animateTo(0f, animationSpec = tween(240, easing = FastOutSlowInEasing))
    }

    // Synchronize scrollOffset when selectedCategory changes externally
    LaunchedEffect(selectedCategoryId) {
        if (!isPressed) {
            val currentInt = scrollOffset.value.roundToInt()
            val currentMod = ((currentInt % itemCount) + itemCount) % itemCount
            var diff = selectedIndex - currentMod
            if (diff > itemCount / 2) diff -= itemCount
            if (diff < -itemCount / 2) diff += itemCount
            val closestVirtualTarget = (currentInt + diff).toFloat()

            if (abs(scrollOffset.value - closestVirtualTarget) > 0.05f) {
                scrollOffset.animateTo(
                    targetValue = closestVirtualTarget,
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    // Dynamic expansion animations (responds immediately on touch down)
    val dragProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 460f),
        label = "dragProgress"
    )

    val currentPillWidth by animateDpAsState(
        targetValue = if (isPressed) expandedPillWidthDp else restingPillWidthDp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 460f),
        label = "pillWidth"
    )

    val currentPillHeight by animateDpAsState(
        targetValue = if (isPressed) expandedPillHeightDp else restingPillHeightDp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 460f),
        label = "pillHeight"
    )

    // Dynamic Corner Radii:
    // Resting: Asymmetric squircle pebble (18dp left, 13dp right)
    // Expanded: Fully rounded capsule (23dp all corners)
    val cornerRadiusLeft by animateDpAsState(
        targetValue = if (isPressed) 23.dp else 18.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 450f),
        label = "cornerLeft"
    )
    val cornerRadiusRight by animateDpAsState(
        targetValue = if (isPressed) 23.dp else 13.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 450f),
        label = "cornerRight"
    )

    // Dock dimensions:
    // Container width expands to 50dp, so the 46dp expanded pill almost fills it entirely!
    val dockBaseWidth = 44.dp
    val dockExpandedWidth = 50.dp
    val currentDockWidth by animateDpAsState(
        targetValue = if (isPressed) dockExpandedWidth else dockBaseWidth,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
        label = "dockWidth"
    )

    val dockBaseHeight = 146.dp
    val dockExpandedHeight = 158.dp
    val currentDockHeight by animateDpAsState(
        targetValue = if (isPressed) dockExpandedHeight else dockBaseHeight,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
        label = "dockHeight"
    )

    // Current category at center
    val currentVirtualIndex = scrollOffset.value.roundToInt()
    val currentActualIndex = ((currentVirtualIndex % itemCount) + itemCount) % itemCount
    val activeCategory = items[currentActualIndex]

    // Real-time continuous color interpolation based on scroll position
    val offsetVal = scrollOffset.value
    val floorIdx = floor(offsetVal).toInt()
    val ceilIdx = ceil(offsetVal).toInt()
    val interpFraction = (offsetVal - floorIdx).coerceIn(0f, 1f)

    val fromActual = ((floorIdx % itemCount) + itemCount) % itemCount
    val toActual = ((ceilIdx % itemCount) + itemCount) % itemCount

    val fromProfile = getCategoryColorProfile(items[fromActual])
    val toProfile = getCategoryColorProfile(items[toActual])

    val continuousTop = lerp(fromProfile.top, toProfile.top, interpFraction)
    val continuousBottom = lerp(fromProfile.bottom, toProfile.bottom, interpFraction)

    // Flash wave enhancement on switch
    val flashVal = flashAnim.value
    val activeProfile = getCategoryColorProfile(activeCategory)
    val dynamicTopColor = lerp(continuousTop, activeProfile.specular, flashVal * 0.45f)
    val dynamicBottomColor = lerp(continuousBottom, continuousTop, flashVal * 0.25f)

    val currentPillShape = RoundedCornerShape(
        topStart = cornerRadiusLeft,
        bottomStart = cornerRadiusLeft,
        topEnd = cornerRadiusRight,
        bottomEnd = cornerRadiusRight
    )

    Box(
        modifier = modifier
            .testTag("fluid_sidebar_dock")
            .width(62.dp)
            .height(currentDockHeight),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Organic dark dock background with concave Bezier fillets.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dock_background_canvas")
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val dockW = currentDockWidth.toPx()
            val right = canvasWidth
            val left = right - dockW

            val filletRadius = 20.dp.toPx()
            val cornerRadius = 20.dp.toPx()

            val topY = 8.dp.toPx()
            val bottomY = canvasHeight - 8.dp.toPx()

            val path = Path().apply {
                moveTo(right, topY - filletRadius)
                cubicTo(
                    right - filletRadius * 0.45f, topY - filletRadius * 0.1f,
                    left, topY - cornerRadius * 0.2f,
                    left, topY + cornerRadius
                )
                lineTo(left, bottomY - cornerRadius)
                cubicTo(
                    left, bottomY + cornerRadius * 0.2f,
                    right - filletRadius * 0.45f, bottomY + filletRadius * 0.1f,
                    right, bottomY + filletRadius
                )
                lineTo(right, topY - filletRadius)
                close()
            }

            drawPath(path = path, color = DockObsidian)
        }

        // 2. Interactive Gesture Area
        Box(
            modifier = Modifier
                .width(currentDockWidth)
                .height(currentDockHeight)
                .align(Alignment.CenterEnd)
                .pointerInput(items) {
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        isPressed = true
                        coroutineScope.launch {
                            flashAnim.snapTo(0.6f)
                            flashAnim.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
                        }
                        val startY = down.position.y
                        var lastY = startY
                        var hasDragged = false

                        while (true) {
                            // Observe and consume drags in Initial pass so scrollable parents
                            // (LazyColumn and pull-to-refresh) never receive the dock gesture.
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (!change.pressed) {
                                break
                            }

                            val currentY = change.position.y
                            val dy = currentY - lastY
                            if (abs(currentY - startY) > 3.dp.toPx() || hasDragged) {
                                hasDragged = true
                                change.consume()
                                // Match the dock's tap behavior: moving down selects the
                                // category displayed below the center (Chats -> Groups).
                                val delta = dy / itemSpacingPx
                                coroutineScope.launch {
                                    scrollOffset.snapTo(scrollOffset.value + delta)
                                }

                                val nearest = scrollOffset.value.roundToInt()
                                val nearestActual = ((nearest % itemCount) + itemCount) % itemCount
                                if (nearestActual != lastHapticIndex) {
                                    lastHapticIndex = nearestActual
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    coroutineScope.launch {
                                        flashAnim.snapTo(1f)
                                        flashAnim.animateTo(0f, tween(240, easing = FastOutSlowInEasing))
                                    }
                                    onCategorySelected(items[nearestActual])
                                }
                                lastY = currentY
                            }
                        }

                        isPressed = false
                        if (!hasDragged) {
                            // Tap detection
                            val dockCenterY = size.height / 2f
                            val deltaFromCenterPx = startY - dockCenterY
                            val stepOffset = (deltaFromCenterPx / itemSpacingPx).roundToInt()

                            if (stepOffset != 0) {
                                val target = (scrollOffset.value + stepOffset).roundToInt()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                coroutineScope.launch {
                                    flashAnim.snapTo(1f)
                                    flashAnim.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
                                    scrollOffset.animateTo(
                                        target.toFloat(),
                                        spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                                val finalActual = ((target % itemCount) + itemCount) % itemCount
                                onCategorySelected(items[finalActual])
                            } else {
                                val target = scrollOffset.value.roundToInt()
                                coroutineScope.launch {
                                    flashAnim.snapTo(0.8f)
                                    flashAnim.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
                                    scrollOffset.animateTo(
                                        target.toFloat(),
                                        spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                                onCategoryActivated(items[((target % itemCount) + itemCount) % itemCount])
                            }
                        } else {
                            val target = scrollOffset.value.roundToInt()
                            val finalActual = ((target % itemCount) + itemCount) % itemCount
                            coroutineScope.launch {
                                flashAnim.snapTo(0.7f)
                                flashAnim.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
                                scrollOffset.animateTo(
                                    target.toFloat(),
                                    spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                            onCategorySelected(items[finalActual])
                        }
                    }
                }
        ) {
            val dockCenterYPx = with(density) { currentDockHeight.toPx() / 2f }
            val pillHalfHeightPx = with(density) { currentPillHeight.toPx() / 2f }
            val dockHalfHeightPx = with(density) { currentDockHeight.toPx() / 2f }

            // 3. Strictly 3 Items in Viewport: [-1, 0, +1] relative to center
            val visibleWindow = (currentVirtualIndex - 1)..(currentVirtualIndex + 1)

            for (virtualIndex in visibleWindow) {
                val actualIndex = ((virtualIndex % itemCount) + itemCount) % itemCount
                val item = items[actualIndex]

                val itemCenterYPx = dockCenterYPx + (virtualIndex - scrollOffset.value) * itemSpacingPx
                val distFromCenterPx = abs(itemCenterYPx - dockCenterYPx)

                if (distFromCenterPx > dockHalfHeightPx) continue

                val edgeFadeDistancePx = with(density) { 20.dp.toPx() }
                val distFromDockEdgePx = dockHalfHeightPx - distFromCenterPx
                val edgeAlpha = (distFromDockEdgePx / edgeFadeDistancePx).coerceIn(0f, 1f)

                val coverageAlpha = when {
                    !isPressed -> {
                        if (distFromCenterPx < with(density) { 14.dp.toPx() }) 0f else 1f
                    }
                    else -> {
                        if (distFromCenterPx < pillHalfHeightPx) {
                            0f
                        } else {
                            val clearance = distFromCenterPx - pillHalfHeightPx
                            (clearance / with(density) { 12.dp.toPx() }).coerceIn(0f, 1f)
                        }
                    }
                }

                val finalAlpha = edgeAlpha * coverageAlpha

                // Dynamic luminance heating towards pure white as item approaches center
                val proximity = (1f - distFromCenterPx / (itemSpacingPx * 1.2f)).coerceIn(0f, 1f)
                val itemDynamicColor = lerp(DockInactiveText, Color.White, proximity)

                Box(
                    modifier = Modifier
                        .size(width = currentDockWidth, height = itemSpacingDp)
                        .offset {
                            IntOffset(
                                x = 0,
                                y = (itemCenterYPx - itemSpacingPx / 2f).roundToInt()
                            )
                        }
                        .graphicsLayer {
                            val progress = iconProgress.value
                            alpha = finalAlpha * progress.coerceIn(0f, 1f)
                        }
                        .testTag("dock_item_${item.id}_$virtualIndex"),
                    contentAlignment = Alignment.Center
                ) {
                    DockItemContent(
                        category = item,
                        color = itemDynamicColor
                    )
                }
            }

            // 4. ACTIVE PILL: FIXED AT VERTICAL CENTER
            // Stretches until it almost hits the container width (46dp inside 50dp container)
            val contentScale = 1f + 0.06f * flashVal

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = with(density) { ((currentDockWidth - currentPillWidth) / 2f).toPx() }.roundToInt(),
                            y = (dockCenterYPx - pillHalfHeightPx).roundToInt()
                        )
                    }
                    .size(width = currentPillWidth, height = currentPillHeight)
                    .graphicsLayer {
                        val progress = iconProgress.value
                        alpha = progress.coerceIn(0f, 1f)
                    }
                    .clip(currentPillShape)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(dynamicTopColor, dynamicBottomColor)
                        )
                    )
                    .testTag("active_orange_pill"),
                contentAlignment = Alignment.Center
            ) {
                // Specular sheen sweep during flash
                if (flashVal > 0.05f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = flashVal * 0.38f }
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White,
                                        activeProfile.specular.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (dragProgress > 0.35f) {
                    // Pressed / Dragging:
                    // Using requiredSize so the Text container takes the full unconstrained pill height (98dp)!
                    // Text is measured: only if textWidth exceeds available length does marquee activate!
                    AnimatedContent(
                            targetState = activeCategory.fullTitle,
                        transitionSpec = {
                            fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                        },
                        label = "rotatedLabel"
                    ) { title ->
                        val textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.4.sp,
                            textAlign = TextAlign.Center
                        )
                        val measured = remember(title) {
                            textMeasurer.measure(
                                text = title,
                                style = textStyle,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        val textWidthDp = with(density) { measured.size.width.toDp() }
                        val availableLengthDp = currentPillHeight - 12.dp
                        val isOverflowing = textWidthDp > availableLengthDp

                        val marqueeMod = if (isOverflowing) {
                            Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                initialDelayMillis = 1000,
                                velocity = 28.dp
                            )
                        } else {
                            Modifier
                        }

                        Box(
                            modifier = Modifier
                                .wrapContentSize(align = Alignment.Center, unbounded = true)
                                .requiredSize(width = currentPillHeight, height = currentPillWidth)
                                .graphicsLayer {
                                    rotationZ = -90f
                                    alpha = ((dragProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
                                    scaleX = contentScale
                                    scaleY = contentScale
                                }
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = textStyle,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier
                                    .then(marqueeMod)
                                    .testTag("rotated_title_text")
                            )
                        }
                    }
                } else {
                    // Resting: Asymmetric pebble content
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = (1f - dragProgress * 2.5f).coerceIn(0f, 1f)
                                scaleX = contentScale
                                scaleY = contentScale
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        DockItemContent(
                            category = activeCategory,
                            color = Color.White
                        )
                    }
                }
                }
            }
        }
    }
}

/**
 * Compact item content matching reference screenshot exactly:
 * - Number: "12"
 * - Symbols: 6-pointed bold Asterisk "*"
 * - Word: Upright capital 'A' + slanted italic 'a' ("Aa")
 * - Text: 4 horizontal paragraph bars with tapering widths
 */
@Composable
private fun DockItemContent(
    category: DockCategory,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (category.icon != null) {
        Icon(category.icon, contentDescription = category.fullTitle, tint = color, modifier = modifier.size(22.dp))
    } else {
        Text(
            text = category.shortLabel,
            color = color,
            fontSize = 20.sp,
            fontFamily = GoogleSansFlexDisplay,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = modifier
        )
    }
}
