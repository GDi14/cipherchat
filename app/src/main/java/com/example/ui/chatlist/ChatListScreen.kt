package com.example.ui.chatlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.LocalContactEntity
import com.example.ui.components.ExpressiveAvatar
import com.example.ui.components.ExpressiveButtonGroup
import com.example.ui.components.ExpressiveCard
import com.example.ui.components.ExpressiveLoadingIndicator
import com.example.ui.components.ExpressiveSecurityBadge
import com.example.ui.components.ExpressiveSplitButton
import com.example.ui.components.expressivePressScale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel,
    onOpenChat: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val contacts by viewModel.contacts.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val discoveredUsers by viewModel.discoveredUsers.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val discoveryError by viewModel.discoveryError.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = Chats, 1 = Directory
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 1 && discoveredUsers.isEmpty()) {
            viewModel.loadDiscoveredFriends()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExpressiveAvatar(
                            name = userProfile?.displayName ?: "Me",
                            size = 40.dp,
                            isOnline = true
                        )
                        Column {
                            Text(
                                text = userProfile?.displayName ?: "CipherChat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            ExpressiveSecurityBadge(text = "Signal E2EE")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) {
                        selectedTabIndex = 1
                    } else {
                        viewModel.loadDiscoveredFriends()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.expressivePressScale()
            ) {
                Icon(
                    imageVector = if (selectedTabIndex == 0) Icons.Default.Add else Icons.Default.Refresh,
                    contentDescription = "Action"
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Expressive Button Group Tab Switcher
            ExpressiveButtonGroup(
                options = listOf("Recent Conversations", "Discover Friends"),
                selectedIndex = selectedTabIndex,
                onSelect = { selectedTabIndex = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search encrypted contacts or directory...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            if (selectedTabIndex == 0) {
                // Chats Tab
                val filteredContacts = contacts.filter {
                    it.displayName.contains(searchQuery, ignoreCase = true) ||
                            it.userId.contains(searchQuery, ignoreCase = true)
                }

                if (filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No conversations yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Discover friends in your circle to start end-to-end encrypted chats.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            ExpressiveSplitButton(
                                primaryText = "Discover Circle Directory",
                                onPrimaryClick = { selectedTabIndex = 1 },
                                onSecondaryClick = { selectedTabIndex = 1 }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredContacts, key = { it.userId }) { contact ->
                            ContactChatItem(
                                contact = contact,
                                onClick = { onOpenChat(contact.userId) }
                            )
                        }
                    }
                }
            } else {
                // Directory Discovery Tab
                if (isDiscovering) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ExpressiveLoadingIndicator(size = 56.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Scanning E2EE Directory...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else if (discoveryError != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = discoveryError ?: "Error loading directory",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    val filteredDiscovered = discoveredUsers.filter {
                        it.displayName.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredDiscovered.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No users found in directory.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredDiscovered, key = { it.userId }) { user ->
                                DiscoveredUserItem(
                                    user = user,
                                    onStartChat = {
                                        viewModel.startChatWithUser(user, onOpenChat)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactChatItem(
    contact: LocalContactEntity,
    onClick: () -> Unit
) {
    val formattedTime = remember(contact.lastMessageTimestamp) {
        if (contact.lastMessageTimestamp > 0L) {
            val date = Date(contact.lastMessageTimestamp)
            val now = Date()
            val diffMs = now.time - date.time
            if (diffMs < 24 * 3600 * 1000) {
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
            } else {
                SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            }
        } else ""
    }

    ExpressiveCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveAvatar(
                name = contact.displayName,
                isOnline = contact.isOnline,
                size = 52.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = contact.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (formattedTime.isNotBlank()) {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = contact.lastMessageText.ifBlank { "Signal Protocol E2EE Active" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (contact.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (contact.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (contact.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${contact.unreadCount}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveredUserItem(
    user: DiscoveredUser,
    onStartChat: () -> Unit
) {
    ExpressiveCard(
        onClick = onStartChat,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveAvatar(
                name = user.displayName,
                isOnline = user.isOnline,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (user.isOnline) "Online in circle" else "Offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (user.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            IconButton(
                onClick = onStartChat,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Start Chat",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
