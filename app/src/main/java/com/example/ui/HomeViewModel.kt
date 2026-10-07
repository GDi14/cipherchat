package com.example.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ConversationEntity
import com.example.data.local.FriendRequestEntity
import com.example.data.local.ProfileEntity
import com.example.data.remote.SupabaseClientProvider
import com.example.repository.AuthRepository
import com.example.repository.ChatRepository
import com.example.repository.FriendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val selectedTab: Int = 0, // 0: Chats, 1: Groups, 2: Calls
    val favoriteFriendIds: Set<String> = emptySet(),
    val voiceOfUserEnabled: Boolean = true,
    val searchTag: String = "",
    val searchResult: ProfileEntity? = null,
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val isSyncing: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val showSupabaseDialog: Boolean = false,
    val isSupabaseConfigured: Boolean = false,
    val supabaseUrlInput: String = "",
    val supabaseKeyInput: String = ""
)

class HomeViewModel(
    val authRepository: AuthRepository,
    val friendRepository: FriendRepository,
    val chatRepository: ChatRepository,
    val supabaseProvider: SupabaseClientProvider
) : ViewModel() {

    val conversations: StateFlow<List<ConversationEntity>> = chatRepository.getConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friends: StateFlow<List<ProfileEntity>> = friendRepository.getAcceptedFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingRequests: StateFlow<List<FriendRequestEntity>> = friendRepository.getPendingIncomingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRequests: StateFlow<List<FriendRequestEntity>> = friendRepository.getAllRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(
        HomeUiState(
            favoriteFriendIds = authRepository.getFavoriteFriendIds(),
            voiceOfUserEnabled = authRepository.isVoiceOfUserEnabled(),
            isSupabaseConfigured = supabaseProvider.isConfigured,
            supabaseUrlInput = supabaseProvider.currentUrl,
            supabaseKeyInput = supabaseProvider.currentAnonKey
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (supabaseProvider.isConfigured) {
                authRepository.syncProfileToSupabase()
            }
            sync()
            chatRepository.startRealtimeSync()
        }
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index, statusMessage = null, errorMessage = null)
    }

    fun toggleFavoriteFriend(friendId: String) {
        val updated = _uiState.value.favoriteFriendIds.toMutableSet().apply {
            if (!add(friendId)) remove(friendId)
        }.toSet()
        authRepository.saveFavoriteFriendIds(updated)
        _uiState.value = _uiState.value.copy(favoriteFriendIds = updated)
    }

    fun setVoiceOfUserEnabled(enabled: Boolean) {
        authRepository.setVoiceOfUserEnabled(enabled)
        chatRepository.setVoiceOfUserEnabled(enabled)
        _uiState.value = _uiState.value.copy(voiceOfUserEnabled = enabled)
    }

    fun onSearchTagChanged(tag: String) {
        _uiState.value = _uiState.value.copy(
            searchTag = tag,
            searchResult = null,
            searchError = null
        )
    }

    fun searchTag() {
        val tag = _uiState.value.searchTag.trim()
        if (tag.isBlank()) return
        _uiState.value = _uiState.value.copy(isSearching = true, searchError = null, searchResult = null)

        viewModelScope.launch {
            val result = friendRepository.searchUserByTag(tag)
            result.onSuccess { profile ->
                if (profile != null) {
                    _uiState.value = _uiState.value.copy(isSearching = false, searchResult = profile)
                } else {
                    _uiState.value = _uiState.value.copy(isSearching = false, searchError = "No user found with tag @$tag")
                }
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchError = err.localizedMessage ?: "Search failed"
                )
            }
        }
    }

    fun sendFriendRequest(target: ProfileEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val res = friendRepository.sendFriendRequest(target.id, target.displayName, target.tag)
            res.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchResult = null,
                    searchTag = "",
                    statusMessage = "Friend request sent to @${target.tag}!"
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    errorMessage = err.localizedMessage ?: "Failed to send request"
                )
            }
        }
    }

    fun acceptFriendRequest(request: FriendRequestEntity) {
        viewModelScope.launch {
            val res = friendRepository.acceptFriendRequest(request)
            res.onSuccess {
                _uiState.value = _uiState.value.copy(statusMessage = "Accepted friend request from @${request.senderTag}")
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(errorMessage = err.localizedMessage ?: "Failed to accept request")
            }
        }
    }

    fun declineFriendRequest(requestId: String) {
        viewModelScope.launch {
            friendRepository.declineFriendRequest(requestId)
        }
    }

    fun sync() {
        _uiState.value = _uiState.value.copy(isSyncing = true)
        viewModelScope.launch {
            friendRepository.syncFriendRequests()
            chatRepository.syncAllFromSupabase()
            _uiState.value = _uiState.value.copy(isSyncing = false)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearStatus() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
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
            supabaseKeyInput = supabaseProvider.currentAnonKey,
            statusMessage = "Supabase credentials saved. Publishing profile & syncing..."
        )
        viewModelScope.launch {
            if (supabaseProvider.isConfigured) {
                val syncRes = authRepository.syncProfileToSupabase()
                syncRes.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "Connected to Supabase! Your profile & public prekeys are published."
                    )
                }.onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Connected to Supabase, but profile sync failed: ${err.localizedMessage}. Did you run supabase_schema.sql?"
                    )
                }
            }
            sync()
            chatRepository.startRealtimeSync()
        }
    }

    fun updateProfile(displayName: String, tag: String, avatarUri: Uri?) {
        viewModelScope.launch {
            val res = authRepository.updateProfile(displayName, tag, avatarUri)
            res.onSuccess {
                _uiState.value = _uiState.value.copy(statusMessage = "Profile updated successfully!")
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(errorMessage = err.localizedMessage ?: "Profile update failed")
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val friendRepository: FriendRepository,
        private val chatRepository: ChatRepository,
        private val supabaseProvider: SupabaseClientProvider
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(authRepository, friendRepository, chatRepository, supabaseProvider) as T
        }
    }
}
