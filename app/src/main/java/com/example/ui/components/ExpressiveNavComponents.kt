package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExpressiveMotion
import com.example.ui.theme.LocalExpressiveMotionEnabled

// ==============================================================================
// Primary app destinations use navigation components. Button groups and
// segmented buttons are reserved for related actions and view filters.
// ==============================================================================

data class AppNavigationDestination(
    val label: String,
    val icon: ImageVector,
    val unselectedIcon: ImageVector = icon,
    val badgeCount: Int = 0,
    val testTag: String
)

@Composable
fun AppNavigationBar(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    items: List<AppNavigationDestination>,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        for ((index, item) in items.withIndex()) {
            val selected = selectedIndex == index
            val destinationIcon = if (selected) item.icon else item.unselectedIcon
            val icon: @Composable () -> Unit = {
                if (item.badgeCount > 0) {
                    BadgedBox(badge = {
                        Badge {
                            Text(if (item.badgeCount > 99) "99+" else item.badgeCount.toString())
                        }
                    }) {
                        Icon(destinationIcon, contentDescription = null)
                    }
                } else {
                    Icon(destinationIcon, contentDescription = null)
                }
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectIndex(index) },
                icon = icon,
                label = { Text(item.label, maxLines = 1) },
                alwaysShowLabel = true,
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}

@Composable
fun AppNavigationRail(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    items: List<AppNavigationDestination>,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        for ((index, item) in items.withIndex()) {
            val selected = selectedIndex == index
            val destinationIcon = if (selected) item.icon else item.unselectedIcon
            val icon: @Composable () -> Unit = {
                if (item.badgeCount > 0) {
                    BadgedBox(badge = {
                        Badge { Text(if (item.badgeCount > 99) "99+" else item.badgeCount.toString()) }
                    }) { Icon(destinationIcon, contentDescription = null) }
                } else Icon(destinationIcon, contentDescription = null)
            }
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectIndex(index) },
                icon = icon,
                label = { Text(item.label, maxLines = 1) },
                alwaysShowLabel = true,
                modifier = Modifier.testTag(item.testTag)
            )
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
    val motionEnabled = LocalExpressiveMotionEnabled.current
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
                enter = if (motionEnabled) fadeIn(tween(ExpressiveMotion.DurationShort4)) +
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                        ) + scaleIn(
                            initialScale = 0.8f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                        ) else EnterTransition.None,
                exit = if (motionEnabled) fadeOut(tween(ExpressiveMotion.DurationShort2)) +
                        slideOutVertically(
                            targetOffsetY = { it / 2 },
                            animationSpec = tween(ExpressiveMotion.DurationShort4, easing = ExpressiveMotion.EmphasizedAccelerateEasing)
                        ) + scaleOut(
                            targetScale = 0.8f,
                            animationSpec = tween(ExpressiveMotion.DurationShort4, easing = ExpressiveMotion.EmphasizedAccelerateEasing)
                        ) else ExitTransition.None
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (menuItem in items) {
                        Surface(
                            onClick = {
                                onToggleExpanded()
                                menuItem.onClick()
                            },
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
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
                    targetValue = if (motionEnabled && expanded) 135f else 0f,
                animationSpec = if (motionEnabled) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow) else snap(),
                label = "fab_rotation"
            )

            val fabContainerColor by animateColorAsState(
                targetValue = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.primaryContainer,
                animationSpec = if (motionEnabled) tween(ExpressiveMotion.DurationMedium2) else snap(),
                label = "fab_color"
            )

            val fabIconColor by animateColorAsState(
                targetValue = if (expanded) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onPrimaryContainer,
                animationSpec = if (motionEnabled) tween(ExpressiveMotion.DurationMedium2) else snap(),
                label = "fab_icon_color"
            )

            FloatingActionButton(
                onClick = onToggleExpanded,
                containerColor = fabContainerColor,
                contentColor = fabIconColor,
                shape = MaterialTheme.shapes.medium,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .size(56.dp)
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
