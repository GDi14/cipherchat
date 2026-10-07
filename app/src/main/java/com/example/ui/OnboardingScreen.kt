package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import com.example.ui.components.ExpressiveContainedLoadingIndicator
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.components.ExpressiveAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, onRegistered: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.isRegistered) {
        if (state.isRegistered) onRegistered()
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { viewModel.onAvatarSelected(it) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("CIPHER", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                        Text("Create your profile", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showSupabaseDialog(true) },
                        modifier = Modifier.testTag("open_supabase_config_button")) {
                        Icon(Icons.Default.Security, contentDescription = "Cloud settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Lock, null, Modifier.size(16.dp))
                                Text("END-TO-END ENCRYPTED", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Text("Your space.\nYour people.", style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold)
                        Text("Set up a name and a unique tag so friends can find you securely.",
                            style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Card(
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth().testTag("avatar_selector")
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                            if (state.avatarUri != null) {
                                AsyncImage(model = state.avatarUri, contentDescription = "Selected profile photo",
                                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                            } else ExpressiveAvatar(name = state.displayName.ifBlank { "You" }, size = 72.dp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Add a profile photo", style = MaterialTheme.typography.titleMedium)
                            Text("Help friends recognize you", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = viewModel::onDisplayNameChanged,
                    label = { Text("Display name") },
                    supportingText = { Text("This is how you appear in conversations") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().testTag("display_name_input")
                )
                OutlinedTextField(
                    value = state.tag,
                    onValueChange = viewModel::onTagChanged,
                    label = { Text("Your unique tag") },
                    prefix = { Text("@", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                    supportingText = {
                        Text(state.tagMessage ?: "At least 3 characters · lowercase letters and numbers")
                    },
                    isError = state.isTagAvailable == false,
                    trailingIcon = {
                        if (state.isCheckingTag) ExpressiveContainedLoadingIndicator(Modifier.size(24.dp), size = 16.dp)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().testTag("tag_input")
                )

                state.errorMessage?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("onboarding_error"))
                }

                Button(
                    onClick = { focusManager.clearFocus(); viewModel.register() },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("register_button")
                ) {
                    if (state.isLoading) ExpressiveContainedLoadingIndicator(Modifier.size(26.dp), size = 18.dp)
                    else {
                        Icon(Icons.Default.Lock, null)
                        Spacer(Modifier.width(10.dp))
                        Text("Create secure profile", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(if (state.isSupabaseConfigured) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (state.isSupabaseConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(Modifier.weight(1f)) {
                            Text(if (state.isSupabaseConfigured) "Cloud relay connected" else "Cloud relay not configured",
                                style = MaterialTheme.typography.titleSmall)
                            Text("You can configure sync and friend discovery here.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { viewModel.showSupabaseDialog(true) }) { Text("Setup") }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (state.showSupabaseDialog) {
        M3SupabaseAlertDialog(
            currentUrl = state.supabaseUrlInput,
            currentKey = state.supabaseKeyInput,
            onDismiss = { viewModel.showSupabaseDialog(false) },
            onSave = viewModel::saveSupabaseConfig
        )
    }
}

@Composable
fun M3SupabaseAlertDialog(
    currentUrl: String,
    currentKey: String,
    onDismiss: () -> Unit,
    onSave: (url: String, key: String) -> Unit
) {
    var url by remember(currentUrl) { mutableStateOf(currentUrl) }
    var key by remember(currentKey) { mutableStateOf(currentKey) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Security, contentDescription = null) },
        title = { Text("Cloud relay") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Connect Supabase to exchange friend requests and sync encrypted messages.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Project URL") },
                    singleLine = true, modifier = Modifier.fillMaxWidth().testTag("supabase_url_input"))
                OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("Anon key") },
                    singleLine = true, modifier = Modifier.fillMaxWidth().testTag("supabase_key_input"))
            }
        },
        confirmButton = {
            Button(onClick = { onSave(url.trim(), key.trim()) }, modifier = Modifier.testTag("save_supabase_config_button")) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
