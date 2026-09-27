package com.example.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseChatRepository
import com.example.data.local.dao.ContactDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.entity.LocalContactEntity
import com.example.data.local.entity.LocalUserProfileEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class DiscoveredUser(
    val userId: String,
    val displayName: String,
    val isOnline: Boolean = false
)

class ChatListViewModel(
    private val contactDao: ContactDao,
    private val userProfileDao: UserProfileDao,
    private val chatRepository: FirebaseChatRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    val contacts: StateFlow<List<LocalContactEntity>> = contactDao.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<LocalUserProfileEntity?> = userProfileDao.getUserProfileFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _discoveredUsers = MutableStateFlow<List<DiscoveredUser>>(emptyList())
    val discoveredUsers: StateFlow<List<DiscoveredUser>> = _discoveredUsers.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _discoveryError = MutableStateFlow<String?>(null)
    val discoveryError: StateFlow<String?> = _discoveryError.asStateFlow()

    fun loadDiscoveredFriends() {
        val currentUid = userProfile.value?.userId ?: return
        viewModelScope.launch {
            _isDiscovering.value = true
            _discoveryError.value = null
            try {
                val snapshot = firestore.collection("users").get().await()
                val list = snapshot.documents.mapNotNull { doc ->
                    val uid = doc.getString("userId") ?: return@mapNotNull null
                    if (uid == currentUid) return@mapNotNull null
                    val name = doc.getString("displayName") ?: "Friend"
                    val isOnline = doc.getBoolean("isOnline") ?: false
                    DiscoveredUser(uid, name, isOnline)
                }
                _discoveredUsers.value = list
            } catch (e: Exception) {
                _discoveryError.value = "Failed to load directory: ${e.localizedMessage}"
            } finally {
                _isDiscovering.value = false
            }
        }
    }

    fun startChatWithUser(user: DiscoveredUser, onNavToChat: (String) -> Unit) {
        viewModelScope.launch {
            // Save or update in local contact DB
            val existing = contactDao.getContactById(user.userId)
            if (existing == null) {
                contactDao.insertOrUpdateContact(
                    LocalContactEntity(
                        userId = user.userId,
                        displayName = user.displayName,
                        isOnline = user.isOnline
                    )
                )
            }
            onNavToChat(user.userId)
        }
    }

    class Factory(
        private val contactDao: ContactDao,
        private val userProfileDao: UserProfileDao,
        private val chatRepository: FirebaseChatRepository,
        private val firestore: FirebaseFirestore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatListViewModel(contactDao, userProfileDao, chatRepository, firestore) as T
        }
    }
}
