package com.example.ui.conversation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseChatRepository
import com.example.data.local.dao.ContactDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.entity.LocalContactEntity
import com.example.data.local.entity.LocalMessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConversationViewModel(
    val friendUserId: String,
    private val messageDao: MessageDao,
    private val contactDao: ContactDao,
    private val chatRepository: FirebaseChatRepository
) : ViewModel() {

    val messages: StateFlow<List<LocalMessageEntity>> = messageDao.getMessagesForConversation(friendUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _friendContact = MutableStateFlow<LocalContactEntity?>(null)
    val friendContact: StateFlow<LocalContactEntity?> = _friendContact.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        chatRepository.activeChatUserId = friendUserId
        loadFriendInfo()
        markMessagesAsRead()
    }

    override fun onCleared() {
        super.onCleared()
        if (chatRepository.activeChatUserId == friendUserId) {
            chatRepository.activeChatUserId = null
        }
    }

    fun loadFriendInfo() {
        viewModelScope.launch {
            _friendContact.value = contactDao.getContactById(friendUserId)
        }
    }

    fun markMessagesAsRead() {
        viewModelScope.launch {
            chatRepository.markChatAsRead(friendUserId)
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    private val _replyingToMessage = MutableStateFlow<LocalMessageEntity?>(null)
    val replyingToMessage: StateFlow<LocalMessageEntity?> = _replyingToMessage.asStateFlow()

    fun setReplyingTo(message: LocalMessageEntity) {
        _replyingToMessage.value = message
    }

    fun cancelReply() {
        _replyingToMessage.value = null
    }

    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val replyTarget = _replyingToMessage.value
        val replySenderName = when {
            replyTarget == null -> null
            replyTarget.isOutgoing -> "You"
            else -> _friendContact.value?.displayName ?: "Friend"
        }

        viewModelScope.launch {
            _isSending.value = true
            _errorMessage.value = null

            val result = chatRepository.sendTextMessage(
                recipientUserId = friendUserId,
                text = trimmed,
                replyToMessageId = replyTarget?.id,
                replyToSenderName = replySenderName,
                replyToText = replyTarget?.plaintext
            )
            if (result.isSuccess) {
                _replyingToMessage.value = null
            } else {
                val err = result.exceptionOrNull()
                _errorMessage.value = err?.localizedMessage ?: "Failed to send message"
            }
            _isSending.value = false
        }
    }

    fun sendAttachment(
        uri: Uri,
        fileName: String,
        mimeType: String,
        caption: String = ""
    ) {
        viewModelScope.launch {
            _isSending.value = true
            _errorMessage.value = null

            val result = chatRepository.sendAttachmentMessage(
                recipientUserId = friendUserId,
                uri = uri,
                fileName = fileName,
                mimeType = mimeType,
                caption = caption
            )

            if (result.isFailure) {
                val err = result.exceptionOrNull()
                _errorMessage.value = err?.localizedMessage
                    ?: "File sharing isn't set up yet — enable Firebase Storage in the console"
            }
            _isSending.value = false
        }
    }

    class Factory(
        private val friendUserId: String,
        private val messageDao: MessageDao,
        private val contactDao: ContactDao,
        private val chatRepository: FirebaseChatRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConversationViewModel(friendUserId, messageDao, contactDao, chatRepository) as T
        }
    }
}
