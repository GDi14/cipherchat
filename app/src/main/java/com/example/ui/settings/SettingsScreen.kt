package com.example.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.updates.UpdateCheckState
import com.example.ui.components.ExpressiveAvatar
import com.example.ui.components.ExpressiveLoadingIndicator
import com.example.ui.components.ExpressiveSecurityBadge
import com.example.ui.components.ExpressiveSplitButton
import com.example.ui.components.expressivePressScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val repoInput by viewModel.repoInput.collectAsState()
    val needsInstallPermission by viewModel.needsInstallPermission.collectAsState()

    val cloudName by viewModel.cloudName.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val apiSecret by viewModel.apiSecret.collectAsState()

    var cloudNameState by remember { mutableStateOf("") }
    var apiKeyState by remember { mutableStateOf("") }
    var apiSecretState by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadCloudinaryCredentials(context)
    }

    LaunchedEffect(cloudName, apiKey, apiSecret) {
        cloudNameState = cloudName
        apiKeyState = apiKey
        apiSecretState = apiSecret
    }

    if (needsInstallPermission) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissInstallPermissionDialog() },
            title = { Text("Permission Required") },
            text = { Text("To install OTA app updates directly, please allow unknown app installation permissions in system settings.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissInstallPermissionDialog() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Text(
                        text = "Settings & Security",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpressiveAvatar(
                        name = userProfile?.displayName ?: "User",
                        size = 56.dp,
                        isOnline = true
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile?.displayName ?: "Anonymous User",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "ID: ${userProfile?.userId ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ExpressiveSecurityBadge(text = "Signal Identity Verified")
                    }
                }
            }

            // Cloudinary Credentials Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Attachment Cloud Hosting",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Configure Cloudinary credentials for hosting encrypted media & voice memos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = cloudNameState,
                        onValueChange = { cloudNameState = it },
                        label = { Text("Cloud Name") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = apiKeyState,
                        onValueChange = { apiKeyState = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = apiSecretState,
                        onValueChange = { apiSecretState = it },
                        label = { Text("API Secret") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.saveCloudinaryCredentials(
                                context,
                                cloudNameState,
                                apiKeyState,
                                apiSecretState
                            )
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .align(Alignment.End)
                            .expressivePressScale()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Credentials")
                    }
                }
            }

            // GitHub OTA Updates Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "OTA Updates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Current Version: ${viewModel.getAppVersion()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = repoInput,
                        onValueChange = { viewModel.updateRepoInput(it) },
                        label = { Text("GitHub Repository (owner/repo)") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    when (val state = updateState) {
                        is UpdateCheckState.Checking -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ExpressiveLoadingIndicator(size = 32.dp)
                                Text("Checking GitHub releases...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        is UpdateCheckState.UpdateAvailable -> {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "New Release: ${state.release.tagName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = state.release.notes.ifBlank { "Bug fixes and security updates." },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.downloadAndInstall(state.release) },
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Download & Install")
                                    }
                                }
                            }
                        }
                        is UpdateCheckState.UpToDate -> {
                            Text(
                                text = "You are on the latest version!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        is UpdateCheckState.Error -> {
                            Text(
                                text = "Check failed: ${state.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {}
                    }

                    ExpressiveSplitButton(
                        primaryText = "Check for Updates",
                        onPrimaryClick = { viewModel.checkForUpdates() },
                        onSecondaryClick = { viewModel.checkForUpdates() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Security Architecture Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Security Specifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "• Signal Protocol Double Ratchet E2EE\n• Room Encrypted Local SQLite Database\n• One-Time PreKey Identity Publishing\n• Firebase Cloud Firestore Messaging Pipeline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
