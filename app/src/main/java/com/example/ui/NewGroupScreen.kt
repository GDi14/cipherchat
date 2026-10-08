package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.ExpressiveAvatar
import com.example.ui.components.ExpressiveContainedLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupScreen(viewModel: NewGroupViewModel, onBack: () -> Unit, onGroupCreated: (String) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.createdGroupId) { state.createdGroupId?.let(onGroupCreated) }
    LaunchedEffect(state.errorMessage) { state.errorMessage?.let { snackbar.showSnackbar(it) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("MAKE ROOM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text("Create a group", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = { Icon(Icons.Default.Lock, contentDescription = "Encrypted group", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 18.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Button(
                    onClick = { viewModel.createGroup() },
                    enabled = !state.isCreating && state.groupTitle.isNotBlank() && state.selectedFriendIds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(56.dp).testTag("create_group_button")
                ) {
                    if (state.isCreating) ExpressiveContainedLoadingIndicator(Modifier.size(26.dp), size = 18.dp)
                    else {
                        Icon(Icons.Default.GroupAdd, null)
                        Spacer(Modifier.width(10.dp))
                        Text(if (state.selectedFriendIds.isEmpty()) "Choose members" else "Create group · ${state.selectedFriendIds.size}",
                            style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 760.dp).fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 22.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.Group, null, Modifier.size(34.dp))
                        Column {
                            Text("A private room for your people", style = MaterialTheme.typography.titleLarge)
                            Text("Messages stay protected with group sender keys.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = state.groupTitle,
                    onValueChange = viewModel::onTitleChanged,
                    label = { Text("Group name") },
                    placeholder = { Text("Weekend plans") },
                    leadingIcon = { Icon(Icons.Default.Group, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("group_name_input")
                )

                AnimatedVisibility(state.selectedFriendIds.isNotEmpty()) {
                    val selected = friends.filter { it.id in state.selectedFriendIds }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("MEMBERS · ${selected.size}", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                            items(selected, key = { it.id }) { friend ->
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.toggleFriendSelection(friend.id) },
                                    label = { Text(friend.displayName) },
                                    leadingIcon = { ExpressiveAvatar(name = friend.displayName, size = 24.dp) },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove ${friend.displayName}", Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose friends", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    Text("${friends.size} available", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (friends.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary)
                            Text("No accepted friends yet", style = MaterialTheme.typography.titleMedium)
                            Text("Add friends first, then return here to start a protected group.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(friends, key = { it.id }) { friend ->
                            val selected = friend.id in state.selectedFriendIds
                            Surface(
                                onClick = { viewModel.toggleFriendSelection(friend.id) },
                                shape = MaterialTheme.shapes.large,
                                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth().testTag("friend_selection_${friend.id}")
                            ) {
                                ListItem(
                                    leadingContent = { ExpressiveAvatar(friend.displayName, friend.avatarPath, size = 48.dp) },
                                    headlineContent = { Text(friend.displayName, style = MaterialTheme.typography.titleMedium) },
                                    supportingContent = { Text("@${friend.tag}") },
                                    trailingContent = {
                                        Checkbox(checked = selected, onCheckedChange = { viewModel.toggleFriendSelection(friend.id) },
                                            modifier = Modifier.testTag("friend_checkbox_${friend.id}"))
                                    },
                                    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
