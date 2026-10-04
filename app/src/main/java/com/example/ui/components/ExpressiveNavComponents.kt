package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExpressiveMotion

// ==============================================================================
// 1. Material 3 Expressive Connected Button Group (Bottom Navigation Bar)
// Spec: https://m3.material.io/components/button-groups/overview
// Connected variant: Joined buttons sharing borders in a unified container,
// with 2dp inner padding, corner-radius morphing upon selection.
// ==============================================================================

data class ConnectedNavButton(
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
    val testTag: String
)

@Composable
fun ExpressiveConnectedBottomBar(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    items: List<ConnectedNavButton>,
    modifier: Modifier = Modifier
) {
    // Stock Material 3 Expressive Connected Button Group (zero outer bar background)
    // Centers the joined buttons seamlessly above the bottom navigation insets
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp, top = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = (index == selectedIndex)
                val isFirst = (index == 0)
                val isLast = (index == items.size - 1)

                // Stock M3 Expressive Connected Shapes:
                // - Unselected edge buttons have outer 24dp rounded corners and 4dp inner corners
                // - Unselected middle buttons have flat 4dp corners
                // - Selected button morphs into a complete pill (24dp on all 4 corners)
                val startRadius = if (isSelected || isFirst) 24.dp else 4.dp
                val endRadius = if (isSelected || isLast) 24.dp else 4.dp
                val buttonShape = RoundedCornerShape(
                    topStart = startRadius,
                    bottomStart = startRadius,
                    topEnd = endRadius,
                    bottomEnd = endRadius
                )

                // Animated fill color: primaryContainer when selected, surfaceContainerHighest when unselected
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                    animationSpec = tween(ExpressiveMotion.DurationMedium2, easing = ExpressiveMotion.EmphasizedDecelerateEasing),
                    label = "stock_connected_container"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    animationSpec = tween(ExpressiveMotion.DurationMedium2, easing = ExpressiveMotion.EmphasizedDecelerateEasing),
                    label = "stock_connected_content"
                )

                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    animationSpec = tween(ExpressiveMotion.DurationMedium2, easing = ExpressiveMotion.EmphasizedDecelerateEasing),
                    label = "stock_connected_border"
                )

                Surface(
                    onClick = { onSelectIndex(index) },
                    shape = buttonShape,
                    color = containerColor,
                    contentColor = contentColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag(item.testTag)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(
                                start = if (isSelected) 24.dp else 18.dp,
                                end = if (isSelected) 26.dp else 20.dp
                            )
                    ) {
                        if (item.badgeCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(item.badgeCount.toString())
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 2. Material 3 Expressive FAB Menu
// Spec: https://m3.material.io/components/fab-menu/overview
// Expands from a primary FAB into a speed-dial / menu with 2-6 connected action items.
// ==============================================================================

data class FabMenuItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
fun ExpressiveFabMenu(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onDismiss: () -> Unit,
    items: List<FabMenuItem>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Scrim backdrop when expanded
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 8.dp, end = 4.dp)
        ) {
            // Menu Items List (Staggered Animation from bottom up)
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(ExpressiveMotion.DurationShort4)) +
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(ExpressiveMotion.DurationMedium3, easing = ExpressiveMotion.EmphasizedDecelerateEasing)
                        ) + scaleIn(
                            initialScale = 0.8f,
                            animationSpec = tween(ExpressiveMotion.DurationMedium3, easing = ExpressiveMotion.EmphasizedDecelerateEasing)
                        ),
                exit = fadeOut(tween(ExpressiveMotion.DurationShort2)) +
                        slideOutVertically(
                            targetOffsetY = { it / 2 },
                            animationSpec = tween(ExpressiveMotion.DurationShort4, easing = ExpressiveMotion.EmphasizedAccelerateEasing)
                        ) + scaleOut(
                            targetScale = 0.8f,
                            animationSpec = tween(ExpressiveMotion.DurationShort4, easing = ExpressiveMotion.EmphasizedAccelerateEasing)
                        )
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items.forEach { menuItem ->
                        ElevatedCard(
                            onClick = {
                                onToggleExpanded()
                                menuItem.onClick()
                            },
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag(menuItem.testTag)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = menuItem.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = menuItem.icon,
                                        contentDescription = menuItem.label,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Main Toggle FAB
            val rotationAngle by animateFloatAsState(
                targetValue = if (expanded) 135f else 0f,
                animationSpec = tween(ExpressiveMotion.DurationMedium3, easing = ExpressiveMotion.EmphasizedDecelerateEasing),
                label = "fab_rotation"
            )

            val fabContainerColor by animateColorAsState(
                targetValue = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.primary,
                animationSpec = tween(ExpressiveMotion.DurationMedium2),
                label = "fab_color"
            )

            val fabIconColor by animateColorAsState(
                targetValue = if (expanded) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onPrimary,
                animationSpec = tween(ExpressiveMotion.DurationMedium2),
                label = "fab_icon_color"
            )

            FloatingActionButton(
                onClick = onToggleExpanded,
                containerColor = fabContainerColor,
                contentColor = fabIconColor,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .size(60.dp)
                    .testTag("fab_menu_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (expanded) "Close FAB menu" else "Open FAB menu",
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(rotationAngle)
                )
            }
        }
    }
}
