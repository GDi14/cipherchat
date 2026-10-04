package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.example.ui.components.ExpressiveEmptyState
import com.example.ui.components.tactileBounce

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupScreen(
    viewModel: NewGroupViewModel,
    onBack: () -> Unit,
    onGroupCreated: (groupId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        onBack()
    }

    LaunchedEffect(uiState.createdGroupId) {
        uiState.createdGroupId?.let { groupId ->
            onGroupCreated(groupId)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Text(
                        text = "New Encrypted Group",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    AssistChip(
                        onClick = { },
                        label = { Text("Sender Keys") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Group Title Field
            OutlinedTextField(
                value = uiState.groupTitle,
                onValueChange = { viewModel.onTitleChanged(it) },
                label = { Text("Group Name") },
                placeholder = { Text("Project Shield") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("group_name_input")
            )

            // Selected Members M3 InputChip Tray
            AnimatedVisibility(visible = uiState.selectedFriendIds.isNotEmpty()) {
                val selectedFriends = friends.filter { uiState.selectedFriendIds.contains(it.id) }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Selected Members (${selectedFriends.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(selectedFriends, key = { it.id }) { friend ->
                            InputChip(
                                selected = true,
                                onClick = { viewModel.toggleFriendSelection(friend.id) },
                                label = { Text(friend.displayName) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                avatar = {
                                    ExpressiveAvatar(name = friend.displayName, size = 24.dp)
                                }
                            )
                        }
                    }
                }
            }

            // Cryptographic Explanation OutlinedCard
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    headlineContent = {
                        Text("Pairwise Key Distribution", fontWeight = FontWeight.Bold)
                    },
                    supportingContent = {
                        Text("Group Sender Keys are distributed to each selected friend via 1:1 Double Ratchet sessions.")
                    }
                )
            }

            // Section Header
            Text(
                text = "Select Friends",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Friends Selection List using M3 OutlinedCard & ListItems
            if (friends.isEmpty()) {
                ExpressiveEmptyState(
                    title = "No Accepted Friends",
                    description = "You need accepted friends to create an encrypted group. Add friends in the Friends tab first.",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(friends, key = { it.id }) { friend ->
                        val isSelected = uiState.selectedFriendIds.contains(friend.id)
                        OutlinedCard(
                            onClick = { viewModel.toggleFriendSelection(friend.id) },
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .tactileBounce()
                                .testTag("friend_selection_${friend.id}")
                        ) {
                            ListItem(
                                leadingContent = {
                                    ExpressiveAvatar(
                                        name = friend.displayName,
                                        avatarUrl = friend.avatarPath,
                                        size = 40.dp
                                    )
                                },
                                headlineContent = { Text(friend.displayName, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text("@${friend.tag}", color = MaterialTheme.colorScheme.primary) },
                                trailingContent = {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { viewModel.toggleFriendSelection(friend.id) },
                                        modifier = Modifier.testTag("friend_checkbox_${friend.id}")
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Create Group Button
            Button(
                onClick = { viewModel.createGroup() },
                enabled = !uiState.isCreating && uiState.groupTitle.isNotBlank() && uiState.selectedFriendIds.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .tactileBounce()
                    .testTag("create_group_button")
            ) {
                if (uiState.isCreating) {
                    ExpressiveContainedLoadingIndicator(
                        text = "Creating Encrypted Group...",
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val count = uiState.selectedFriendIds.size
                        Text(
                            text = if (count > 0) "Create Encrypted Group ($count)" else "Create Encrypted Group",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
