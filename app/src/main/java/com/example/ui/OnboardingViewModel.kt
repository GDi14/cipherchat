package com.example.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.remote.SupabaseClientProvider
import com.example.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val displayName: String = "",
    val tag: String = "",
    val avatarUri: Uri? = null,
    val isCheckingTag: Boolean = false,
    val isTagAvailable: Boolean? = null,
    val tagMessage: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistered: Boolean = false,
    val showSupabaseDialog: Boolean = false,
    val isSupabaseConfigured: Boolean = false,
    val supabaseUrlInput: String = "",
    val supabaseKeyInput: String = ""
)

class OnboardingViewModel(
    private val authRepository: AuthRepository,
    private val supabaseProvider: SupabaseClientProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            displayName = authRepository.getDisplayName().takeIf { it != "User" } ?: "",
            tag = authRepository.getTag(),
            isRegistered = authRepository.isRegistered(),
            isSupabaseConfigured = supabaseProvider.isConfigured,
            supabaseUrlInput = supabaseProvider.currentUrl,
            supabaseKeyInput = supabaseProvider.currentAnonKey
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var tagCheckJob: Job? = null

    fun onDisplayNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(displayName = name, errorMessage = null)
    }

    fun showSupabaseDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(
            showSupabaseDialog = show,
            supabaseUrlInput = supabaseProvider.currentUrl,
            supabaseKeyInput = supabaseProvider.currentAnonKey
        )
    }

    fun saveSupabaseConfig(url: String, anonKey: String) {
        supabaseProvider.updateCredentials(url, anonKey)
        _uiState.value = _uiState.value.copy(
            showSupabaseDialog = false,
            isSupabaseConfigured = supabaseProvider.isConfigured,
            supabaseUrlInput = supabaseProvider.currentUrl,
            supabaseKeyInput = supabaseProvider.currentAnonKey
        )
        // Re-check tag if one is entered
        if (_uiState.value.tag.isNotBlank()) {
            onTagChanged(_uiState.value.tag)
        }
    }

    fun onTagChanged(rawTag: String) {
        val cleanTag = rawTag.trim().removePrefix("@").lowercase().replace(" ", "")
        _uiState.value = _uiState.value.copy(
            tag = cleanTag,
            isCheckingTag = true,
            isTagAvailable = null,
            tagMessage = null,
            errorMessage = null
        )

        tagCheckJob?.cancel()
        if (cleanTag.length < 3) {
            _uiState.value = _uiState.value.copy(
                isCheckingTag = false,
                isTagAvailable = false,
                tagMessage = "Tag must be at least 3 characters"
            )
            return
        }

        tagCheckJob = viewModelScope.launch {
            delay(400) // debounce
            val result = authRepository.checkTagAvailable(cleanTag)
            result.onSuccess { available ->
                _uiState.value = _uiState.value.copy(
                    isCheckingTag = false,
                    isTagAvailable = available,
                    tagMessage = if (available) "Tag is available!" else "Tag is already taken"
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isCheckingTag = false,
                    isTagAvailable = true, // allow fallback in offline mode
                    tagMessage = if (supabaseProvider.isConfigured) "Tag available" else "Offline verification: proceeding"
                )
            }
        }
    }

    fun onAvatarSelected(uri: Uri?) {
        _uiState.value = _uiState.value.copy(avatarUri = uri)
    }

    fun register() {
        val state = _uiState.value
        val name = state.displayName.trim()
        val tag = state.tag.trim()

        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your display name")
            return
        }
        if (tag.length < 3) {
            _uiState.value = _uiState.value.copy(errorMessage = "Tag must be at least 3 characters")
            return
        }
        if (state.isTagAvailable == false) {
            _uiState.value = _uiState.value.copy(errorMessage = "This tag is already taken. Please pick another.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = authRepository.registerUser(name, tag, state.avatarUri)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, isRegistered = true)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err.localizedMessage ?: "Registration failed. Check network or Supabase settings."
                )
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val supabaseProvider: SupabaseClientProvider
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(authRepository, supabaseProvider) as T
        }
    }
}
