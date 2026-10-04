package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ProfileEntity
import com.example.repository.ChatRepository
import com.example.repository.FriendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NewGroupUiState(
    val groupTitle: String = "",
    val selectedFriendIds: Set<String> = emptySet(),
    val isCreating: Boolean = false,
    val createdGroupId: String? = null,
    val errorMessage: String? = null
)

class NewGroupViewModel(
    private val friendRepository: FriendRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    val friends: StateFlow<List<ProfileEntity>> = friendRepository.getAcceptedFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(NewGroupUiState())
    val uiState: StateFlow<NewGroupUiState> = _uiState.asStateFlow()

    fun onTitleChanged(title: String) {
        _uiState.value = _uiState.value.copy(groupTitle = title, errorMessage = null)
    }

    fun toggleFriendSelection(friendId: String) {
        val current = _uiState.value.selectedFriendIds.toMutableSet()
        if (current.contains(friendId)) {
            current.remove(friendId)
        } else {
            current.add(friendId)
        }
        _uiState.value = _uiState.value.copy(selectedFriendIds = current, errorMessage = null)
    }

    fun createGroup() {
        val title = _uiState.value.groupTitle.trim()
        val members = _uiState.value.selectedFriendIds.toList()

        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a group name")
            return
        }
        if (members.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select at least one friend for the group")
            return
        }

        _uiState.value = _uiState.value.copy(isCreating = true, errorMessage = null)
        viewModelScope.launch {
            val result = chatRepository.createGroupConversation(title, members)
            result.onSuccess { convId ->
                _uiState.value = _uiState.value.copy(isCreating = false, createdGroupId = convId)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    errorMessage = err.localizedMessage ?: "Failed to create group"
                )
            }
        }
    }

    class Factory(
        private val friendRepository: FriendRepository,
        private val chatRepository: ChatRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NewGroupViewModel(friendRepository, chatRepository) as T
        }
    }
}
